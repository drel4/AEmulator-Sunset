/* AEmulator Sunset, 2026-10-07. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File

/** Host-safe, opt-in translation of firmware service environment into guest variables. */
object ExtendedEnvironment {
    fun active(engine: Engine, profile: UniversalProfile) = engine == Engine.KK && profile == UniversalProfile.EXTENDED
    private val name = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val protected = setOf("LD_PRELOAD", "LD_AUDIT", "BOOTCLASSPATH", "ANDROID_PROPERTY_WORKSPACE",
        "ANDROID_ROOT", "ANDROID_DATA", "ASHMEM_SHIM_DIR")

    fun allowed(key: String, value: String): Boolean = name.matches(key) &&
        key !in protected && listOf("DHD_", "QEMU_", "AEMU_", "ANDROID_SOCKET_", "LD_")
            .none { key.startsWith(it) && key != "LD_LIBRARY_PATH" } &&
        value.length <= 8192 && value.none { it.code < 32 || it.code == 127 } &&
        '$' !in value // Unresolved init property expansions must not be passed literally.

    /** Keep existing order (including empty components); append only guest-contained directories. */
    fun searchPath(root: File, declared: String, candidates: List<String>): String {
        val base = root.canonicalFile.toPath()
        val seen = declared.split(':').toMutableSet()
        val additions = candidates.filter { path ->
            val dir = File(root, path.removePrefix("/"))
            runCatching { dir.isDirectory && dir.canonicalFile.toPath().startsWith(base) && seen.add(path) }
                .getOrDefault(false)
        }
        return if (additions.isEmpty()) declared else (listOf(declared) + additions).joinToString(":")
    }

    val commandDirs = listOf("/system/bin", "/system/xbin", "/vendor/bin", "/sbin")
    val libraryDirs = listOf("/system/lib", "/vendor/lib", "/system/vendor/lib")

    fun service(root: File, values: Map<String, String>): Map<String, String> = buildMap {
        for ((key, value) in values) if (allowed(key, value)) {
            val resolved = when (key) {
                "PATH" -> searchPath(root, value, commandDirs)
                "LD_LIBRARY_PATH" -> searchPath(root, value, libraryDirs)
                else -> value
            }
            put("DHD_ENV_$key", resolved)
        }
    }

    /** Init uses double quotes and backslash escaping, not shell evaluation. Malformed lines are ignored. */
    fun setenv(line: String): Pair<String, String>? {
        val words = ArrayList<String>()
        val word = StringBuilder()
        var quoted = false
        var escaped = false
        var started = false
        for (c in line) {
            if (quoted) {
                if (c == '"') quoted = false else word.append(c)
                continue
            }
            if (escaped) {
                word.append(when (c) { 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> c })
                escaped = false; started = true; continue
            }
            if (c == '#' && !started) break
            when {
                c == '\\' -> { escaped = true; started = true }
                c == '"' -> { quoted = !quoted; started = true }
                c.isWhitespace() && !quoted -> if (started) {
                    words.add(word.toString()); word.setLength(0); started = false
                }
                else -> { word.append(c); started = true }
            }
        }
        if (escaped || quoted) return null
        if (started) words.add(word.toString())
        return if (words.size == 3 && words[0] == "setenv" && allowed(words[1], words[2]))
            words[1] to words[2] else null
    }
}
