package app.aemu.core

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Low memory killer вместо ядерного. Без него фоновые процессы гостя не умирают никогда: на Xiaomi 15
 * у S3 4.3 висело 35 app_process по 75–300 МБ, телефон уходил в своп, а `pm` отвечал по две минуты.
 *
 * Важность процессов (oom_adj, шкала −17…15) сообщает ActivityManager гостя:
 *  - до 4.3 — записью в /proc/<pid>/oom_adj; хост её не пускает (SELinux), прослойка гостя
 *    (native/guestshim) перенаправляет запись в FIFO /dev/aemu_oom строкой «\n<pid> a <adj>»;
 *  - 4.4+ — пакетами в сокет /dev/socket/lmkd (int big-endian: 1 = PROCPRIO pid uid adj, 2 = PROCREMOVE pid).
 * Когда памяти мало, убиваем самый неважный и самый толстый процесс, как это делал бы lowmemorykiller.
 */
/** [ramMb] > 0: guest RAM budget; background apps are killed when the guest's total RSS exceeds it. */
class GuestLmk(private val root: File, private val lowRam: Boolean, private val log: (String) -> Unit, private val ramMb: Int = 0) {
    private val adj = ConcurrentHashMap<Int, Int>()
    @Volatile private var running = false
    private var server: LocalServerSocket? = null
    private var bound: LocalSocket? = null
    private val fifo = File(root, "dev/aemu_oom")
    @Volatile var kills = 0
        private set

    fun start() {
        running = true
        runCatching {
            fifo.delete(); fifo.parentFile?.mkdirs()
            Os.mkfifo(fifo.absolutePath, "666".toInt(8))
            Os.chmod(fifo.absolutePath, "666".toInt(8))
            Thread({ readFifo() }, "lmk-fifo").apply { isDaemon = true; start() }
        }.onFailure { log("lmk: FIFO not created: $it") }
        runCatching { serveLmkd() }.onFailure { log("lmk: lmkd socket failed: $it") }
        Thread({ loop() }, "lmk").apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }; runCatching { bound?.close() }
        // разбудить читателя FIFO
        runCatching { File(fifo.absolutePath).appendText("\n") }
        adj.clear()
    }

    private fun readFifo() {
        // O_RDWR: открытие не ждёт писателя и чтение не получает EOF, когда писатели уходят
        val fd = Os.open(fifo.absolutePath, OsConstants.O_RDWR or OsConstants.O_CLOEXEC, 0)
        val buf = ByteArray(4096)
        val line = StringBuilder()
        FileInputStream(fd).use { input ->
            while (running) {
                val n = try { input.read(buf) } catch (_: Exception) { break }
                if (n <= 0) break
                for (i in 0 until n) {
                    val c = buf[i].toInt().toChar()
                    if (c == '\n') { parseLine(line.toString()); line.setLength(0) } else if (line.length < 64) line.append(c)
                }
                // значение приходит отдельным write() без «\n»: строку закрывает «\n» следующей записи
            }
        }
    }

    private fun parseLine(s: String) {
        val p = s.trim().split(' ').filter { it.isNotEmpty() }
        if (p.size < 3) return
        val pid = p[0].toIntOrNull() ?: return
        val v = p[2].toIntOrNull() ?: return
        adj[pid] = if (p[1] == "s") scoreToAdj(v) else v
    }

    private fun scoreToAdj(score: Int) = if (score >= 1000) 15 else score * 17 / 1000

    private fun serveLmkd() {
        val sock = File(root, "dev/socket/lmkd")
        sock.parentFile?.mkdirs(); sock.delete()
        val ls = LocalSocket(LocalSocket.SOCKET_SEQPACKET)
        ls.bind(LocalSocketAddress(sock.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM))
        runCatching { Os.chmod(sock.absolutePath, "666".toInt(8)) }
        bound = ls
        val s = LocalServerSocket(ls.fileDescriptor)
        server = s
        Thread({
            while (running) {
                val c = try { s.accept() } catch (_: Exception) { break }
                Thread({ lmkdClient(c) }, "lmkd-client").apply { isDaemon = true; start() }
            }
        }, "lmkd-accept").apply { isDaemon = true; start() }
    }

    private fun lmkdClient(c: LocalSocket) {
        val buf = ByteArray(256)
        try {
            val input = c.inputStream
            while (running) {
                val n = input.read(buf)
                if (n <= 0) break
                val bb = java.nio.ByteBuffer.wrap(buf, 0, n)
                if (n < 8) continue
                when (bb.int) {
                    1 -> if (n >= 16) { val pid = bb.int; bb.int; adj[pid] = bb.int }
                    2 -> adj.remove(bb.int)
                }
            }
        } catch (_: Exception) {
        } finally { runCatching { c.close() } }
    }

    // ---------------------------------------------------------------- убийца

    private fun memAvailableMb(): Long = runCatching {
        File("/proc/meminfo").useLines { l -> l.first { it.startsWith("MemAvailable:") } }
            .split(Regex("\\s+"))[1].toLong() / 1024
    }.getOrDefault(Long.MAX_VALUE)

    private fun rssMb(pid: Int): Long = runCatching {
        File("/proc/$pid/statm").readText().split(' ')[1].toLong() * 4 / 1024
    }.getOrDefault(-1)

    private fun loop() {
        // RSS процессов гостя не складываем: страницы зиготы общие и считались бы в каждом процессе.
        // Мерило — свободная память телефона; с lowRam держим запас больше и фона меньше.
        val minHost = if (lowRam) 2500L else MIN_HOST_MB
        val maxCached = if (ramMb > 0) (ramMb / 128).coerceIn(2, MAX_CACHED) else if (lowRam) 4 else MAX_CACHED
        while (running) {
            Thread.sleep(2000)
            adj.keys.removeAll { !File("/proc/$it").exists() }
            val cached = adj.filter { it.value >= CACHED }.keys
            if (cached.isEmpty()) continue
            val avail = memAvailableMb()
            // RSS overcounts shared zygote pages, so the budget check is approximate (upper bound)
            val overBudget = ramMb > 0 && adj.keys.sumOf { rssMb(it).coerceAtLeast(0) } > ramMb * 2
            if (avail >= minHost && cached.size <= maxCached && !overBudget) continue
            val sizes = cached.associateWith { rssMb(it) }
            // сначала самый неважный, среди равных — самый большой
            val victim = cached.maxWithOrNull(compareBy<Int> { adj[it] ?: 0 }.thenBy { sizes[it] ?: 0 }) ?: continue
            val a = adj[victim]
            runCatching { Os.kill(victim, OsConstants.SIGKILL) }
            adj.remove(victim)
            kills++
            log("memory: killed background process $victim (adj $a, ${sizes[victim]} MB; cached ${cached.size}, host free $avail MB)")
            Thread.sleep(3000)
        }
    }

    companion object {
        /** HIDDEN_APP_MIN_ADJ (≤4.3) = CACHED_APP_MIN_ADJ (4.4+) = 9 */
        const val CACHED = 9
        const val MAX_CACHED = 10
        const val MIN_HOST_MB = 1500L
    }
}
