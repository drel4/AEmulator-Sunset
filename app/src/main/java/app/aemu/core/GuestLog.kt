package app.aemu.core

import java.io.File
import java.io.RandomAccessFile
import java.io.ByteArrayOutputStream
import java.io.Writer

/**
 * Журнал гостя. Гостевой liblog пишет в /dev/log/main записи вида
 *   [приоритет:1 байт][тег]\0[сообщение]\0
 * (заголовок добавило бы ядро, но драйвера logger нет — файл обычный).
 */
object GuestLog {
    data class Rec(val prio: Char, val tag: String, val msg: String)

    /** Export persisted records, not the log panel's 300-record preview. Buffers
     * have no timestamps, so keep them in separate labelled sections. */
    fun writeExport(root: File, run: File, out: Writer) {
        for (buffer in listOf("main", "system", "radio")) {
            out.write("\n=== guest $buffer ===\n")
            val f = File(root, "dev/log/$buffer")
            if (!f.isFile) continue
            var remaining = f.length()
            f.inputStream().buffered().use { input ->
                fun next(): Int = if (remaining-- > 0) input.read() else -1
                fun field(limit: Int): String? {
                    val bytes = ByteArrayOutputStream()
                    while (true) {
                        val c = next()
                        if (c == 0) return bytes.toString("UTF-8")
                        if (c < 0) return null
                        if (bytes.size() >= limit) return null
                        bytes.write(c)
                    }
                }
                while (remaining > 0) {
                    val p = next()
                    if (p !in 2..8) continue
                    val tag = field(48) ?: continue
                    if (tag.isEmpty() || tag.any { it < ' ' }) continue
                    val msg = field(64 * 1024) ?: continue
                    out.write("${"??VDIWEFS"[p]} $tag: $msg\n")
                }
            }
        }
        // Fatal assertions can be written to stderr instead of Android's main buffer.
        for (name in listOf("zygote", "binderd", "servicemanager", "surfaceflinger", "bootanim", "installd", "netd", "mediaserver")) {
            val f = File(run, "$name.log")
            if (!f.isFile) continue
            out.write("\n=== service $name stdout/stderr ===\n")
            f.bufferedReader().use { it.copyTo(out) }
        }
    }

    fun tail(root: File, bytes: Int = 256 * 1024, buffer: String = "main"): List<Rec> {
        val f = File(root, "dev/log/$buffer")
        if (!f.isFile || f.length() == 0L) return emptyList()
        val data = runCatching {
            RandomAccessFile(f, "r").use { r ->
                val n = minOf(r.length(), bytes.toLong()).toInt()
                r.seek(r.length() - n)
                ByteArray(n).also { r.readFully(it) }
            }
        }.getOrNull() ?: return emptyList()
        return parse(data)
    }

    fun parse(data: ByteArray): List<Rec> {
        val out = ArrayList<Rec>()
        var i = 0
        while (i < data.size - 2) {
            val p = data[i].toInt() and 0xff
            val end = indexOf(data, i + 1)
            if (end < 0) break
            if (p in 2..8 && end - i - 1 in 1..48) {
                val tag = String(data, i + 1, end - i - 1, Charsets.UTF_8)
                if (tag.none { it < ' ' }) {
                    val j = end + 1
                    var end2 = indexOf(data, j)
                    if (end2 < 0) end2 = data.size
                    out.add(Rec("??VDIWEFS"[p], tag, String(data, j, end2 - j, Charsets.UTF_8)))
                    i = end2 + 1
                    continue
                }
            }
            i++
        }
        return out
    }

    fun contains(root: File, vararg needles: String): Boolean {
        val f = File(root, "dev/log/main")
        if (!f.isFile) return false
        val t = runCatching { String(f.readBytes(), Charsets.ISO_8859_1) }.getOrDefault("")
        return needles.any { t.contains(it) }
    }

    fun clear(root: File) {
        for (n in listOf("main", "system", "radio", "events")) {
            val f = File(root, "dev/log/$n")
            if (f.isFile) runCatching { RandomAccessFile(f, "rw").use { it.setLength(0) } }
        }
    }

    private fun indexOf(a: ByteArray, from: Int): Int {
        for (k in from until a.size) if (a[k] == 0.toByte()) return k
        return -1
    }
}
