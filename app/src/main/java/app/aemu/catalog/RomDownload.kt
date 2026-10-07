/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.catalog

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.security.MessageDigest

data class RomDownloadRequest(val title: String, val url: String, val sha256: String? = null)

object StarterRom {
    val request = RomDownloadRequest("AEmulator Sunset CM11",
        "https://dumpster.ralsei.tech/drel/AESSRomCatalog/Android442forAESS.aessvm",
        "7254606fea3d4ffe61aef80c0da20c64146ee2bee5f5084d9b9d0f9f8253ee10")
}

class RomDownloadException(val reason: Reason) : IOException(reason.name) {
    enum class Reason { WEB_PAGE, SPACE, INCOMPLETE, CHECKSUM, CANCELLED }
}

/** Streaming, bounded HTTP downloads. The caller owns successful temporary files. */
object RomDownload {
    private const val MAX_BYTES = 16L * 1024 * 1024 * 1024
    private const val RESERVE = 64L * 1024 * 1024
    fun fileName(url: String, disposition: String?): String {
        val encoded = disposition?.let { Regex("(?i)filename\\*=UTF-8''([^;]+)").find(it)?.groupValues?.get(1) }
        val plain = disposition?.let { Regex("(?i)filename=\"([^\"]+)\"|filename=([^;]+)").find(it) }
        val name = encoded?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?: plain?.let { it.groupValues[1].ifEmpty { it.groupValues[2] }.trim() }
            ?: URL(url).path.substringAfterLast('/')
        return name.replace('\\', '/').substringAfterLast('/').replace(Regex("[^\\p{L}\\p{N}._-]"), "_")
            .takeLast(160).ifBlank { "firmware" }
    }
    fun download(request: RomDownloadRequest, directory: File, cancelled: () -> Boolean,
                 progress: (Long, Long) -> Unit): File {
        directory.mkdirs()
        var url = CatalogUrls.valid(request.url) ?: throw IOException("Invalid ROM URL")
        var output: File? = null
        fun checkCancel() { if (cancelled()) throw RomDownloadException(RomDownloadException.Reason.CANCELLED) }
        try {
            repeat(6) { hop ->
                checkCancel()
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    connection.setRequestProperty("User-Agent", "AEmulator-Sunset-RomDownload")
                    val code = connection.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        require(hop < 5) { "Too many redirects" }
                        val location = connection.getHeaderField("Location") ?: throw IOException("Redirect without location")
                        val next = CatalogUrls.valid(URL(URL(url), location).toExternalForm()) ?: throw IOException("Unsafe redirect")
                        require(!url.startsWith("https:", true) || next.startsWith("https:", true)) { "HTTPS downgrade rejected" }
                        url = next
                    } else {
                        if (code != 200) throw IOException("HTTP $code")
                        val length = connection.contentLengthLong
                        if (length > MAX_BYTES || length > directory.usableSpace - RESERVE)
                            throw RomDownloadException(RomDownloadException.Reason.SPACE)
                        if (connection.contentType.orEmpty().contains("html", true))
                            throw RomDownloadException(RomDownloadException.Reason.WEB_PAGE)
                        val name = fileName(url, connection.getHeaderField("Content-Disposition"))
                        // Retain the original extension for the importer's VM-archive dispatch.
                        val file = File.createTempFile("rom-", "-$name", directory)
                        output = file
                        val digest = MessageDigest.getInstance("SHA-256")
                        var count = 0L
                        connection.inputStream.buffered(65536).use { input ->
                            input.mark(512)
                            val head = ByteArray(512)
                            val n = input.read(head)
                            input.reset()
                            if (n > 0 && String(head, 0, n, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\r', '\n', '\t').startsWith("<"))
                                throw RomDownloadException(RomDownloadException.Reason.WEB_PAGE)
                            file.outputStream().buffered(65536).use { sink ->
                                val buffer = ByteArray(65536)
                                while (true) {
                                    checkCancel()
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    count += read
                                    if (count > MAX_BYTES || directory.usableSpace < read + RESERVE)
                                        throw RomDownloadException(RomDownloadException.Reason.SPACE)
                                    digest.update(buffer, 0, read)
                                    sink.write(buffer, 0, read)
                                    progress(count, length)
                                }
                            }
                        }
                        checkCancel()
                        if (count == 0L || length >= 0 && count != length)
                            throw RomDownloadException(RomDownloadException.Reason.INCOMPLETE)
                        val sha = digest.digest().joinToString("") { "%02x".format(it) }
                        if (request.sha256 != null && !sha.equals(request.sha256, true))
                            throw RomDownloadException(RomDownloadException.Reason.CHECKSUM)
                        return file
                    }
                } finally { connection.disconnect() }
            }
            throw IOException("Too many redirects")
        } catch (error: Throwable) { output?.delete(); throw error }
    }
}
