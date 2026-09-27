package app.aemu.core

import android.net.LocalSocket
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.DataInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

/**
 * Fake modem behind /dev/socket/rild — the same idea as the SDK emulator's reference RIL.
 *
 * Telephony sees a ready SIM, a registered UMTS network and one data connection (rmnet0). That is what
 * makes ConnectivityService report "mobile: CONNECTED": the guest's sockets already reach the internet
 * through the phone (qemu user mode), but apps check the active network first and refuse to go online.
 * The operator is taken from the firmware's own apns-conf.xml so the framework finds an APN for it.
 *
 * Response layouts follow the guest's RIL.java: RIL v6 parcels for 4.x, the string lists of 2.3.
 * Extra trailing fields are harmless — Parcel reads past the end as zero/null.
 */
class RilStub(
    private val paths: VmPaths,
    private val log: (String) -> Unit,
    private val imei: String = VmSettings.DEFAULT_IMEI,
    private val api: Int = 19,
) {
    @Volatile var answered = 0L
        private set

    private val server = UnixServer(paths.socket("rild"), "rild") { c -> serveOne(c) }
    private val operator: Pair<String, String> by lazy { pickOperator() }   // mcc to mnc

    fun serve() = server.start(log)
    fun stop() = server.stop()

    private fun serveOne(c: LocalSocket) {
        val o = c.outputStream
        if (api >= 14) unsol(o, UNSOL_RIL_CONNECTED, ints(RIL_VERSION))
        unsol(o, UNSOL_RADIO_STATE_CHANGED, le(0))
        log("radio: telephony connected, operator ${operator.first}${operator.second}")
        val din = DataInputStream(c.inputStream)
        while (true) {
            val len = try { din.readInt() } catch (e: Exception) { break }
            if (len <= 0 || len > 1 shl 20) break
            val body = ByteArray(len)
            din.readFully(body)
            if (len < 8) continue
            val b = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
            val request = b.getInt()
            val serial = b.getInt()
            val (err, payload) = runCatching { handle(request) }.getOrElse { 0 to ByteArray(0) }
            val resp = ByteArrayOutputStream()
            resp.write(le(0)); resp.write(le(serial)); resp.write(le(err)); resp.write(payload)
            frame(o, resp.toByteArray())
            answered++
            if (request == RIL_REQUEST_RADIO_POWER) afterPower(o, b)
        }
    }

    private fun handle(req: Int): Pair<Int, ByteArray> {
        val (mcc, mnc) = operator
        return when (req) {
            RIL_REQUEST_GET_SIM_STATUS -> 0 to simStatus()
            RIL_REQUEST_GET_CURRENT_CALLS -> 0 to le(0)
            RIL_REQUEST_GET_IMSI -> 0 to str(mcc + mnc + "0123456789".take(15 - mcc.length - mnc.length))
            RIL_REQUEST_SIGNAL_STRENGTH -> 0 to ints(20, 99, -1, -1, -1, -1, -1, 99, -1, -1, -1, -1, -1)
            RIL_REQUEST_VOICE_REGISTRATION_STATE -> 0 to strs("1", "0001", "00000001", "3")
            RIL_REQUEST_DATA_REGISTRATION_STATE -> 0 to strs("1", "0001", "00000001", "3", "0", "1")
            RIL_REQUEST_OPERATOR -> 0 to strs(OPERATOR_NAME, OPERATOR_NAME, mcc + mnc)
            RIL_REQUEST_SETUP_DATA_CALL -> 0 to dataCall(single = true)
            RIL_REQUEST_DATA_CALL_LIST -> 0 to dataCall(single = false)
            RIL_REQUEST_SIM_IO -> 0 to (le(0x6A) + le(0x82) + str(null))   // "file not found": no SIM records
            RIL_REQUEST_GET_IMEI -> 0 to str(imei)
            RIL_REQUEST_GET_IMEISV -> 0 to str(IMEISV)
            RIL_REQUEST_BASEBAND_VERSION -> 0 to str(BASEBAND)
            RIL_REQUEST_DEVICE_IDENTITY -> 0 to strs(imei, IMEISV, "", "")
            RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE -> 0 to ints(0)
            RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE -> 0 to ints(0)
            RIL_REQUEST_GET_NEIGHBORING_CELL_IDS -> 0 to le(0)
            RIL_REQUEST_VOICE_RADIO_TECH -> 0 to ints(3)
            RIL_REQUEST_GET_SMSC_ADDRESS -> 0 to str("+10000000000")
            RIL_REQUEST_QUERY_FACILITY_LOCK -> 0 to ints(0)
            RIL_REQUEST_SEND_SMS, RIL_REQUEST_SEND_SMS_EXPECT_MORE -> 0 to (le(1) + str(null) + le(-1))
            RIL_REQUEST_DIAL -> E_GENERIC_FAILURE to ByteArray(0)
            // unknown request: one element that parses both as int[] {0} and String[] {""} —
            // an empty payload crashes com.android.phone (Samsung reads index 0 of the result)
            else -> 0 to (le(1) + le(0) + le(0))
        }
    }

    private fun afterPower(o: OutputStream, args: ByteBuffer) {
        val on = runCatching { args.position(12); args.getInt() != 0 }.getOrDefault(true)
        // before RIL v7 (4.2) "on with a ready SIM" was its own radio state
        unsol(o, UNSOL_RADIO_STATE_CHANGED, le(if (!on) 0 else if (api >= 17) 10 else 4))
        if (on) {
            unsol(o, UNSOL_RESPONSE_SIM_STATUS_CHANGED, ByteArray(0))
            unsol(o, UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED, ByteArray(0))
        }
    }

    private fun simStatus(): ByteArray {
        val b = ByteArrayOutputStream()
        b.write(le(1)); b.write(le(0))                    // card present, universal PIN unknown
        b.write(le(0)); b.write(le(-1))                   // GSM/UMTS app 0, no CDMA app
        if (api >= 16) b.write(le(-1))                    // no IMS app (field added in 4.1)
        b.write(le(1))                                    // one application
        b.write(le(if (api >= 14) 2 else 1))              // USIM (SIM on 2.3)
        b.write(le(5))                                    // APPSTATE_READY
        b.write(le(0))                                    // perso substate
        b.write(str(null)); b.write(str(null))            // aid, label
        b.write(le(0)); b.write(le(0)); b.write(le(0))    // pin1 replaced, pin1, pin2
        return b.toByteArray()
    }

    private fun dataCall(single: Boolean): ByteArray {
        if (api < 14) {   // 2.3: cid, interface, address
            return if (single) strs("1", IFACE, "10.0.2.15") else (le(1) + le(1) + le(2) + str("IP") + str("10.0.2.15"))
        }
        val b = ByteArrayOutputStream()
        b.write(le(RIL_VERSION)); b.write(le(1))
        b.write(le(0)); b.write(le(-1))                   // status OK, no retry
        b.write(le(1)); b.write(le(2))                    // cid, active + link up
        b.write(str("IP")); b.write(str(IFACE)); b.write(str("10.0.2.15/24"))
        b.write(str("8.8.8.8 1.1.1.1")); b.write(str("10.0.2.2"))
        return b.toByteArray()
    }

    /** First generic ("default") APN of the firmware; 310/260 as a last resort. */
    private fun pickOperator(): Pair<String, String> = runCatching {
        val xml = File(paths.root, "system/etc/apns-conf.xml").readText()
        for (m in Regex("<apn\\b([^>]*)>", RegexOption.DOT_MATCHES_ALL).findAll(xml)) {
            val a = m.groupValues[1]
            fun attr(n: String) = Regex("\\b$n\\s*=\\s*\"([^\"]*)\"").find(a)?.groupValues?.get(1)
            val mcc = attr("mcc") ?: continue
            val mnc = attr("mnc") ?: continue
            val type = attr("type")
            if (attr("mvno_type") != null) continue
            if (type == null || type.contains("default") || type == "*") return@runCatching mcc to mnc
        }
        null
    }.getOrNull() ?: ("310" to "260")

    private fun unsol(o: OutputStream, code: Int, data: ByteArray) = runCatching { frame(o, le(1) + le(code) + data) }

    @Synchronized
    private fun frame(out: OutputStream, payload: ByteArray) {
        out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.size).array())
        out.write(payload)
        out.flush()
    }

    private fun le(v: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    private fun ints(vararg v: Int) = le(v.size) + v.fold(ByteArray(0)) { acc, x -> acc + le(x) }
    private fun strs(vararg v: String) = le(v.size) + v.fold(ByteArray(0)) { acc, x -> acc + str(x) }

    /** Parcel.writeString: length (-1 for null), UTF-16 with a terminator, padded to 4 bytes */
    private fun str(s: String?): ByteArray {
        if (s == null) return le(-1)
        val chars = (s.length + 1) * 2
        val b = ByteBuffer.allocate(4 + (chars + 3) / 4 * 4).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(s.length)
        s.forEach { b.putChar(it) }
        return b.array()
    }

    companion object {
        private const val RIL_VERSION = 6
        private const val IFACE = "rmnet0"
        private const val OPERATOR_NAME = "AEmulator"
        private const val UNSOL_RADIO_STATE_CHANGED = 1000
        private const val UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED = 1002
        private const val UNSOL_RESPONSE_SIM_STATUS_CHANGED = 1019
        private const val UNSOL_RIL_CONNECTED = 1034
        private const val E_GENERIC_FAILURE = 2
        private const val RIL_REQUEST_GET_SIM_STATUS = 1
        private const val RIL_REQUEST_GET_CURRENT_CALLS = 9
        private const val RIL_REQUEST_DIAL = 10
        private const val RIL_REQUEST_GET_IMSI = 11
        private const val RIL_REQUEST_SIGNAL_STRENGTH = 19
        private const val RIL_REQUEST_VOICE_REGISTRATION_STATE = 20
        private const val RIL_REQUEST_DATA_REGISTRATION_STATE = 21
        private const val RIL_REQUEST_OPERATOR = 22
        private const val RIL_REQUEST_RADIO_POWER = 23
        private const val RIL_REQUEST_SEND_SMS = 25
        private const val RIL_REQUEST_SEND_SMS_EXPECT_MORE = 26
        private const val RIL_REQUEST_SETUP_DATA_CALL = 27
        private const val RIL_REQUEST_SIM_IO = 28
        private const val RIL_REQUEST_GET_IMEI = 38
        private const val RIL_REQUEST_GET_IMEISV = 39
        private const val RIL_REQUEST_QUERY_FACILITY_LOCK = 42
        private const val RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE = 45
        private const val RIL_REQUEST_BASEBAND_VERSION = 51
        private const val RIL_REQUEST_DATA_CALL_LIST = 57
        private const val RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE = 74
        private const val RIL_REQUEST_GET_NEIGHBORING_CELL_IDS = 75
        private const val RIL_REQUEST_DEVICE_IDENTITY = 98
        private const val RIL_REQUEST_GET_SMSC_ADDRESS = 100
        private const val RIL_REQUEST_VOICE_RADIO_TECH = 108
        private const val IMEISV = "01"
        private const val BASEBAND = "AEmulator"
    }
}

/**
 * Заглушка vold: MountService видит одну «вставленную» карту по пути из прошивки.
 * Сама карта — каталог на хосте, qemu подменяет гостевой путь (DHD_SDCARD).
 */
class VoldStub(
    paths: VmPaths,
    private val mountPoint: String,
    private val log: (String) -> Unit,
    sockName: String = "vold",
    /** остальные тома прошивки (съёмные карты, USB): сообщаем о них как о «нет носителя» */
    private val others: List<String> = emptyList(),
) {
    @Volatile var state = MOUNTED
    private val label = mountPoint.substringAfterLast('/').ifEmpty { "sdcard" }
    private val clients = Collections.synchronizedList(ArrayList<LocalSocket>())

    private val server = UnixServer(paths.socket(sockName), sockName) { c -> serveOne(c) }

    fun serve() = server.start(log)
    fun stop() { server.stop(); clients.clear() }

    private fun serveOne(c: LocalSocket) {
        clients.add(c)
        try {
            val out = c.outputStream
            val ins = c.inputStream
            val buf = ByteArray(4096)
            val acc = StringBuilder()
            while (true) {
                val n = try { ins.read(buf) } catch (e: Exception) { -1 }
                if (n <= 0) break
                for (i in 0 until n) {
                    val b = buf[i].toInt() and 0xff
                    if (b == 0) {
                        val cmd = acc.toString(); acc.setLength(0)
                        if (cmd.isNotEmpty()) answer(cmd, out)
                    } else acc.append(b.toChar())
                }
            }
        } finally {
            clients.remove(c)
        }
    }

    private fun answer(cmd: String, out: OutputStream) {
        val parts = cmd.trim().split(Regex("\\s+"))
        val seq = parts.getOrNull(0)?.toIntOrNull()
        val args = if (seq != null) parts.drop(1) else parts
        val head = args.getOrElse(0) { "" }
        val sub = args.getOrElse(1) { "" }
        var after: (() -> Unit)? = null
        val reply: List<String> = when {
            // с томами из storage_list (4.x) сообщаем только о них: основное хранилище там эмулируемое,
            // а незнакомый путь MountService Samsung считает ошибкой и обрывает список
            head == "volume" && sub == "list" -> (if (others.isEmpty()) listOf("110 $label $mountPoint $state")
                else others.mapIndexed { i, v -> "110 ${v.substringAfterLast('/')} $v ${if (i == 0 && v == mountPoint) state else 0}" }) +
                "200 Volumes listed."
            head == "volume" && sub == "mount" -> { state = MOUNTED; after = { announce(IDLE, MOUNTED) }; listOf("200 Volume mounted.") }
            head == "volume" && sub == "unmount" -> { state = IDLE; after = { announce(MOUNTED, IDLE) }; listOf("200 Volume unmounted.") }
            head == "volume" && sub == "format" -> listOf("200 Volume formatted.")
            head == "share" && sub == "status" -> listOf("210 ${args.getOrElse(2) { "ums" }} unavailable")
            head == "share" || head == "unshare" -> listOf("200 Share operation succeeded.")
            head == "storage" && sub == "users" -> listOf("200 Storage user list.")
            head == "asec" && sub == "list" -> listOf("200 Asec listed.")
            head == "asec" && sub == "path" -> listOf("211 /mnt/asec/${args.getOrElse(2) { "" }}")
            head == "obb" && sub == "list" -> listOf("200 Obb listed.")
            head == "cryptfs" -> listOf("200 0 0")
            else -> listOf("200 ok")
        }
        send(out, reply.map { withSeq(it, seq) })
        after?.invoke()
    }

    private fun withSeq(line: String, seq: Int?): String {
        if (seq == null) return line
        val sp = line.indexOf(' ')
        if (sp <= 0) return line
        val code = line.substring(0, sp).toIntOrNull() ?: return line
        if (code in 600..699) return line
        return "$code $seq${line.substring(sp)}"
    }

    fun announce(from: Int, to: Int) {
        val msg = "605 Volume $label $mountPoint state changed from $from (${name(from)}) to $to (${name(to)})"
        val list = synchronized(clients) { ArrayList(clients) }
        for (c in list) runCatching { send(c.outputStream, listOf(msg)) }
    }

    fun rescan() {
        state = MOUNTED
        announce(IDLE, MOUNTED)
    }

    private fun name(s: Int) = when (s) {
        0 -> "No-Media"; 1 -> "Idle-Unmounted"; 2 -> "Pending"; 3 -> "Checking"; 4 -> "Mounted"
        5 -> "Unmounting"; 6 -> "Formatting"; 7 -> "Shared-Unmounted"; else -> "Unknown"
    }

    @Synchronized
    private fun send(out: OutputStream, msgs: List<String>) {
        val b = ByteArrayOutputStream()
        for (s in msgs) { b.write(s.toByteArray()); b.write(0) }
        out.write(b.toByteArray())
        out.flush()
    }

    companion object {
        const val IDLE = 1
        const val MOUNTED = 4
    }
}

/** «Звонок кадра»: GL-сервер сообщает о новом кадре — будим отрисовку. */
class FrameBell(paths: VmPaths, private val log: (String) -> Unit, private val onFrame: () -> Unit) {
    @Volatile var rings = 0L
        private set
    private val server = UnixServer(paths.frameSock, "frame") { c ->
        val ins = c.inputStream
        val b = ByteArray(64)
        while (true) {
            val n = try { ins.read(b) } catch (e: Exception) { -1 }
            if (n <= 0) break
            rings += n
            onFrame()
        }
    }
    fun serve() = server.start(log)
    fun stop() = server.stop()
}

/**
 * /dev/log/events — канал (FIFO), который хост держит открытым и вычитывает.
 * Обычным файлом его делать нельзя: часть служб (MIUI Whetstone и др.) читает журнал событий как
 * устройство logger — на обычном файле чтение сразу упирается в конец, и служба крутит цикл
 * «read error», забивая процессор и журнал (миллионы строк за минуту). Из канала читатель ждёт данных.
 * Сами события эмулятору не нужны — выбрасываем.
 */
class EventsSink(paths: VmPaths, private val log: (String) -> Unit) {
    private val fifo = File(paths.root, "dev/log/events")
    @Volatile private var raf: java.io.RandomAccessFile? = null

    fun start() {
        runCatching {
            val mode = runCatching { android.system.Os.stat(fifo.absolutePath).st_mode and android.system.OsConstants.S_IFMT }.getOrDefault(0)
            if (mode != android.system.OsConstants.S_IFIFO) {
                fifo.parentFile?.mkdirs(); fifo.delete()
                android.system.Os.mkfifo(fifo.absolutePath, "666".toInt(8))
            }
            // O_RDWR: открытие не ждёт второй стороны, а писатели гостя никогда не упираются в «нет читателя»
            val r = java.io.RandomAccessFile(fifo, "rw").also { raf = it }
            Thread({
                val buf = ByteArray(1 shl 16)
                while (true) { if (runCatching { r.read(buf) }.getOrDefault(-1) < 0) break }
            }, "aemu-events").apply { isDaemon = true; start() }
        }.onFailure { log("event log: channel failed: ${it.message}") }
    }

    fun stop() { runCatching { raf?.close() }; raf = null }
}
