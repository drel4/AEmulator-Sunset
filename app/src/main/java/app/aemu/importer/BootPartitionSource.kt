/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.importer

import java.io.*
import java.nio.file.Files
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.CompressorStreamFactory

/** Content-first detection: TWRP boot.emmc.win is usually a raw boot image, not tar. */
internal object BootPartitionSource {
    private const val MAX_BOOT = 64 * 1024 * 1024
    fun read(input: InputStream, name: String, scratch: File, depth: Int = 0): ByteArray {
        require(depth <= 4) { "Too many nested archives" }
        val src = input.buffered(65536)
        src.mark(512)
        val head = ByteArray(512)
        var count = 0
        while (count < head.size) { val n = src.read(head, count, head.size - count); if (n < 0) break; count += n }
        src.reset()
        fun magic(offset: Int, value: String) = count >= offset + value.length &&
            String(head, offset, value.length, Charsets.ISO_8859_1) == value
        val lower = name.lowercase()
        fun candidate(path: String): Boolean {
            val base = path.substringAfterLast('/').lowercase()
            return base.matches(Regex("(boot(\\.img|\\.(emmc|mtd)\\.win)?|zimage|kernel)(\\.(gz|xz|bz2|lz4))?"))
        }
        if (magic(0, "ANDROID!")) return bounded(src)
        if (magic(0, "PK\u0003\u0004")) {
            ZipArchiveInputStream(src).use { zip ->
                while (true) {
                    val e = zip.nextZipEntry ?: break
                    if (!e.isDirectory && candidate(e.name)) return read(zip, e.name, scratch, depth + 1)
                }
            }
            error("No boot partition in ZIP archive")
        }
        if (magic(0, "7z\u00bc\u00af\u0027\u001c")) {
            val temp = Files.createTempFile(scratch.toPath(), "boot-archive-", ".7z").toFile()
            try {
                temp.outputStream().use { out ->
                    val buf = ByteArray(65536)
                    var total = 0L
                    val limit = minOf(8L shl 30, scratch.usableSpace - (256L shl 20))
                    while (true) { val n = src.read(buf); if (n < 0) break; total += n; require(total <= limit) { "Not enough space for archive" }; out.write(buf, 0, n) }
                }
                SevenZFile.builder().setFile(temp).setMaxMemoryLimitKiB(65536).get().use { seven ->
                    while (true) {
                        val e = seven.nextEntry ?: break
                        if (!e.isDirectory && candidate(e.name)) return seven.getInputStream(e).use { read(it, e.name, scratch, depth + 1) }
                    }
                }
                error("No boot partition in 7z archive")
            } finally { temp.delete() }
        }
        // Legacy LZ4 ramdisks/images; other compression is detected by Commons Compress.
        if (magic(0, "\u0002\u0021\u004c\u0018")) return Lz4Legacy.decode(bounded(src)).also { require(it.size <= MAX_BOOT) }
        val compressed = runCatching { CompressorStreamFactory.detect(src) }.getOrNull()
        if (compressed != null) {
            CompressorStreamFactory(true, 65536).createCompressorInputStream(compressed, src).use {
                return read(it, lower.substringBeforeLast('.', lower), scratch, depth + 1)
            }
        }
        if (magic(257, "ustar") || lower.endsWith(".tar") || lower.endsWith(".tar.md5") || lower.endsWith(".win")) {
            TarArchiveInputStream(src).use { tar ->
                while (true) {
                    val e = tar.nextTarEntry ?: break
                    if (e.isFile && candidate(e.name)) return read(tar, e.name, scratch, depth + 1)
                }
            }
            error("No boot partition in TAR/backup archive (encrypted backups are unsupported)")
        }
        // Samsung kernels have no single boot header; BootImage validates their initramfs later.
        return bounded(src)
    }
    private fun bounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(65536)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            require(out.size().toLong() + n <= MAX_BOOT) { "Boot image exceeds 64 MiB" }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
