/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

import java.io.File

/** Optional firmware boot media only. Never auto-restart or run charging services. */
internal object BootMediaServices {
    private val names = setOf("bootanim", "bootanimation", "samsungani", "playsound")
    fun rcFiles(root: File): List<File> = listOf(root, File(root, "system/etc/init"))
        .flatMap { it.listFiles()?.filter { f -> f.isFile && f.name.endsWith(".rc") } ?: emptyList() }
        .filterNot { it.name.startsWith("init.charging") || it.name.startsWith("init.recovery") || it.name.startsWith("lpm") }
        .sortedBy { it.name }
    fun resolve(root: File, name: String): GuestService? {
        if (name !in names) return null
        val rc = InitPlan.parse(rcFiles(root))
        val aliases = if (name in setOf("bootanim", "bootanimation")) listOf("bootanim", "samsungani", "bootanimation") else listOf(name)
        val service = aliases.firstNotNullOfOrNull { rc.services[it] }
        val argv = service?.argv ?: when (name) {
            "playsound" -> listOf("/system/bin/playsound")
            "samsungani" -> listOf("/system/bin/samsungani")
            else -> listOf(if (File(root, "system/bin/samsungani").isFile) "/system/bin/samsungani" else "/system/bin/bootanimation")
        }
        if (argv.isEmpty() || !argv[0].startsWith('/')) return null
        val binary = File(root, argv[0].trimStart('/'))
        if (!binary.isFile || !binary.canonicalPath.startsWith(root.canonicalPath + File.separator)) return null
        return GuestService(if (name == "playsound") name else "bootanim", argv,
            service?.sockets?.toMap() ?: emptyMap(), optional = true)
    }
    fun triggers(root: File, property: String, value: String): List<Pair<Boolean, String>> {
        // Deliberately not a general init trigger interpreter.
        if (property != "service.bootanim.exit") return emptyList()
        val result = ArrayList<Pair<Boolean, String>>()
        for (file in rcFiles(root)) {
            var matching = false
            for (raw in file.readLines()) {
                val tokens = raw.substringBefore('#').trim().split(Regex("\\s+"))
                if (tokens[0] == "on") matching = tokens.size == 2 && tokens[1] == "property:$property=$value"
                else if (tokens[0] == "service") matching = false
                else if (matching && tokens.size == 2 && tokens[0] in setOf("start", "stop") && tokens[1] in names)
                    result += (tokens[0] == "start") to tokens[1]
            }
        }
        return result.distinct()
    }
}
