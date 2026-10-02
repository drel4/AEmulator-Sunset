/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File

/** Offline charging is an alternative init class, not a normal Android preset. */
internal object ChargingBoot {
    /** Existing QEMU does not redirect faccessat(/sys). Adjust a private runtime
     * copy's sysfs constant, never the imported charger executable. */
    fun runtimeService(root: File, service: GuestService): GuestService {
        val source = File(root, service.argv.first().trimStart('/'))
        require(source.length() <= 16 * 1024 * 1024) { "Charging executable too large" }
        val bytes = source.readBytes()
        val from = "/sys/class/power_supply".toByteArray()
        val to = "sys/class/power_supply".toByteArray() + 0
        var changed = false
        if (bytes.size >= 4 && bytes[0] == 0x7f.toByte() && String(bytes, 1, 3) == "ELF") {
            for (i in 0..(bytes.size - from.size).coerceAtLeast(-1)) {
                if (from.indices.all { bytes[i + it] == from[it] }) {
                    to.copyInto(bytes, i); changed = true
                }
            }
        }
        if (!changed) return service
        val directory = File(root, "dev/aemu-charging")
        require(!java.nio.file.Files.isSymbolicLink(directory.toPath()))
        directory.mkdirs()
        val copy = File(directory, source.name)
        require(!java.nio.file.Files.isSymbolicLink(copy.toPath()))
        copy.writeBytes(bytes); copy.setReadable(true, false); copy.setExecutable(true, false)
        return service.copy(argv = listOf("/dev/aemu-charging/${source.name}") + service.argv.drop(1))
    }
    val properties = mapOf("ro.bootmode" to "charger", "ro.boot.bootmode" to "charger",
        "ro.boot.mode" to "charger", "ro.boot.charge" to "1", "sys.boot_from_charger_mode" to "1")
    fun services(root: File, api: Int = 25): List<GuestService> {
        val dirs = listOf(root, File(root, "system/etc/init"))
        val files = dirs.flatMap { it.listFiles()?.filter { f -> f.isFile && f.name.endsWith(".rc") } ?: emptyList() }
        val rc = InitPlan.parse(files, properties, includeCharging = true)
        val names = setOf("charger", "healthd-charger", "playlpm", "lpm", "charging")
        val selected = rc.services.values.filter { (it.cls == "charger" || it.name in names) &&
            it.name !in setOf("zygote", "surfaceflinger", "bootanim", "samsungani") &&
            it.argv.isNotEmpty() && it.argv.first().startsWith("/") && File(root, it.argv.first().trimStart('/')).isFile }
        if (selected.any { it.name != "healthd-charger" || "-n" !in it.argv })
            return selected.map { GuestService(it.name, it.argv, it.sockets.toMap()) }
        for (path in listOf("/charger", "/sbin/charger", "/system/bin/charger", "/system/bin/playlpm", "/system/bin/charging"))
            if (File(root, path.trimStart('/')).isFile) return listOf(GuestService("charger", listOf(path)))
        if (api >= 21 && File(root, "sbin/healthd").isFile) return listOf(GuestService("charger", listOf("/sbin/healthd", "-c")))
        error("This ROM has no usable offline-charging service. Import its boot image as well as /system.")
    }
}
