/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.importer

/** Compression signatures take precedence over misleading download suffixes (e.g. .tgz.tar). */
internal object FirmwareContainers {
    enum class Compression { GZIP, XZ, BZIP2 }
    fun compression(h: ByteArray): Compression? = when {
        h.size >= 2 && h[0] == 0x1f.toByte() && h[1] == 0x8b.toByte() -> Compression.GZIP
        h.size >= 6 && h.copyOfRange(0, 6).contentEquals(byteArrayOf(0xfd.toByte(), 0x37, 0x7a, 0x58, 0x5a, 0)) -> Compression.XZ
        h.size >= 3 && h[0] == 'B'.code.toByte() && h[1] == 'Z'.code.toByte() && h[2] == 'h'.code.toByte() -> Compression.BZIP2
        else -> null
    }
    fun nested(name: String): Boolean {
        val base = name.substringAfterLast('/').lowercase()
        if (base.startsWith("modem") || base.startsWith("cp_") || base.contains("csc")) return false
        return listOf(".zip", ".7z", ".tar", ".tar.md5", ".tgz", ".gz", ".xz", ".bz2", ".md5").any(base::endsWith)
    }
    fun checkDepth(depth: Int) { require(depth <= 8) { "Firmware archives are nested too deeply" } }
    fun destination(root: java.io.File, relative: String): java.io.File {
        require(!relative.startsWith('/') && !relative.contains('\\') && !relative.contains('\u0000') &&
            relative.split('/').none { it == ".." }) { "Unsafe firmware path" }
        val file = java.io.File(root, relative)
        require(file.canonicalPath == root.canonicalPath || file.canonicalPath.startsWith(root.canonicalPath + java.io.File.separator)) { "Firmware path escapes guest root" }
        return file
    }
    fun checkLink(relative: String, target: String) {
        require(target.isNotEmpty() && !target.contains('\\') && !target.contains('\u0000')) { "Unsafe firmware symlink" }
        val parent = relative.substringBeforeLast('/', "")
        val path = if (target.startsWith('/')) target.trimStart('/') else if (parent.isEmpty()) target else "$parent/$target"
        require(!java.nio.file.Paths.get(path).normalize().startsWith("..")) { "Firmware symlink escapes guest root" }
    }
}
