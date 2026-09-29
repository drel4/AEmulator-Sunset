package app.aemu.core

import android.os.Environment
import java.io.File

/**
 * Internal storage/aemulator/firmware: firmware files dropped there show up in the library for one-tap import.
 * Same folder as "AEmulator" (the shared storage is case-insensitive), next to the systems' memory cards.
 */
object FirmwareFolder {
    private val EXT = Regex("""(?i).*\.(zip|7z|tar|md5|tgz|gz|xz|bz2|tbz2?|img|win|ext4)$""")

    val dir: File get() = File(Environment.getExternalStorageDirectory(), "aemulator/firmware")

    /** Firmware files, newest first; empty when there is no all-files access. */
    fun list(): List<File> = runCatching {
        if (!Environment.isExternalStorageManager()) return emptyList()
        dir.mkdirs()
        dir.listFiles().orEmpty().filter { it.isFile && it.length() > 1_000_000 && EXT.matches(it.name) }
            .sortedByDescending { it.lastModified() }
    }.getOrDefault(emptyList())
}
