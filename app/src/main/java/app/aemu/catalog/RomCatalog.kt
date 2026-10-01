package app.aemu.catalog

import java.net.URI

data class CatalogRom(val device: String, val android: String, val skin: String,
    val status: Int, val comment: String, val url: String?)
data class CatalogSection(val name: String, val comment: String, val source: String?, val roms: List<CatalogRom>)
data class RomCatalog(val motd: String, val sections: List<CatalogSection>, val warningLines: List<Int>)

object CatalogUrls {
    const val DEFAULT = "https://dumpster.ralsei.tech/drel/AESSRomCatalog/rom.list"
    fun valid(value: String): String? = runCatching {
        val s = value.trim()
        require(s.length in 1..8192)
        val uri = URI(s)
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true))
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null)
        s
    }.getOrNull()
}

/** Semicolon-separated records, with the final URL allowed to contain semicolons. */
object RomCatalogParser {
    fun parse(text: String): RomCatalog {
        data class Section(val name: String, val comment: String, val source: String?, val roms: MutableList<CatalogRom>)
        val sections = mutableListOf<Section>()
        val motd = mutableListOf<String>()
        val warnings = linkedSetOf<Int>()
        var started = false
        text.removePrefix("\uFEFF").lineSequence().forEachIndexed { index, raw ->
            require(index < 10_000) { "Catalog exceeds 10000 lines" }
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            if (line.startsWith('#')) {
                if (!started) motd += line.removePrefix("#").trim()
                return@forEachIndexed
            }
            started = true
            fun url(value: String): String? {
                val result = CatalogUrls.valid(value)
                if (value.isNotBlank() && result == null) warnings += index + 1
                return result
            }
            if (line.startsWith('[')) {
                val f = line.split(';', limit = 3).map(String::trim)
                if (f.size != 3 || !f[0].endsWith(']') || f[0].length < 3) {
                    warnings += index + 1
                    return@forEachIndexed
                }
                sections += Section(f[0].substring(1, f[0].length - 1), f[1], url(f[2]), mutableListOf())
            } else {
                val f = line.split(';', limit = 6).map(String::trim)
                if (f.size != 6 || f[0].isEmpty()) {
                    warnings += index + 1
                    return@forEachIndexed
                }
                val status = f[3].toIntOrNull()?.takeIf { it in -1..2 } ?: run { warnings += index + 1; -1 }
                if (sections.isEmpty()) sections += Section("", "", null, mutableListOf())
                sections.last().roms += CatalogRom(f[0], f[1], f[2], status, f[4], url(f[5]))
            }
        }
        return RomCatalog(motd.joinToString("\n"), sections.map { CatalogSection(it.name, it.comment, it.source, it.roms.toList()) }, warnings.toList())
    }
}
