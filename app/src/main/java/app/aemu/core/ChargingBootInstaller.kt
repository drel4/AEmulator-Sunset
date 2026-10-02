/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import app.aemu.importer.BootImage
import java.io.File
import java.nio.file.Files

/** Restore charging files omitted by old importers. Caller holds storage lease.
 * No partitions, firmware framework or existing init rules are replaced. */
internal object ChargingBootInstaller {
    fun install(root: File, bytes: ByteArray): Int {
        val entries = BootImage.ramdisk(bytes) ?: error("Unrecognized boot image")
        require(!Files.isSymbolicLink(root.toPath()) && root.isDirectory)
        val files = entries.filter { e ->
            val name = e.name.removePrefix("./")
            e.mode and 0xf000 == 0x8000 && allowed(name)
        }
        require(files.any { it.name.removePrefix("./") in listOf("charger", "sbin/charger", "sbin/healthd") }) {
            "No charging executable in this boot image"
        }
        var count = 0
        for (entry in files) {
            val name = entry.name.removePrefix("./")
            val file = File(root, name)
            require(file.canonicalPath.startsWith(root.canonicalPath + File.separator))
            var parent = file.parentFile
            while (parent != null && parent != root) {
                require(!Files.isSymbolicLink(parent.toPath())) { "Unsafe charging-file path" }
                parent = parent.parentFile
            }
            require(!Files.isSymbolicLink(file.toPath())) { "Unsafe charging-file path" }
            // Only missing files: preserve existing healthd compatibility work and ROM assets.
            if (file.exists()) continue
            file.parentFile!!.mkdirs()
            file.writeBytes(entry.data)
            file.setReadable(true, false)
            if (entry.mode and 0x49 != 0) file.setExecutable(true, false)
            count++
        }
        return count
    }
    fun allowed(name: String): Boolean {
        if (name.startsWith('/') || name.split('/').any { it.isEmpty() || it == "." || it == ".." }) return false
        return name in setOf("charger", "sbin/charger", "sbin/healthd", "init.charging.rc", "lpm.rc") ||
            name.startsWith("res/images/charger/") || name.startsWith("res/images/battery")
    }
}
