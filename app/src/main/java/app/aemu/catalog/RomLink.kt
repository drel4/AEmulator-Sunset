/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.catalog

import java.net.HttpURLConnection
import java.net.URL

/** Sharing pages need a browser (cookies, scripts, passwords); never pretend they are archives. */
object RomLink {
    fun browserOnly(url: String): Boolean {
        val parsed = runCatching { URL(url) }.getOrNull() ?: return true
        val host = parsed.host.lowercase()
        fun domain(name: String) = host == name || host.endsWith(".$name")
        return domain("drive.google.com") || domain("docs.google.com") || domain("mega.nz") ||
            domain("mega.io") || domain("mega.co.nz") || domain("4shared.com") ||
            domain("disk.yandex.ru") || domain("disk.yandex.com") || domain("disk.yandex.com.tr") ||
            domain("yadi.sk") || domain("1drv.ms") || domain("onedrive.live.com") ||
            domain("androidfilehost.com") || domain("sourceforge.net") && !host.endsWith(".dl.sourceforge.net") ||
            domain("mediafire.com") && !host.startsWith("download") ||
            domain("dropbox.com") && parsed.query.orEmpty().split('&').none { it == "dl=1" || it == "raw=1" }
    }

    /** Small streaming probe for otherwise unknown links, with the same redirect safety as downloads. */
    fun isDirect(url: String): Boolean {
        if (browserOnly(url)) return false
        var current = CatalogUrls.valid(url) ?: return false
        return runCatching {
            repeat(6) {
                val connection = URL(current).openConnection() as HttpURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 5000; connection.readTimeout = 5000
                    connection.setRequestProperty("Range", "bytes=0-511")
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    val code = connection.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        val location = connection.getHeaderField("Location") ?: return false
                        val next = CatalogUrls.valid(URL(URL(current), location).toExternalForm()) ?: return false
                        if (current.startsWith("https:", true) && !next.startsWith("https:", true)) return false
                        if (browserOnly(next)) return false
                        current = next
                    } else {
                        if (code != 200 && code != 206) return false
                        val type = connection.contentType.orEmpty().lowercase()
                        if (type.contains("html") || type.contains("json") || type.contains("xml")) return false
                        connection.inputStream.use { input ->
                            val head = ByteArray(512)
                            val n = input.read(head)
                            if (n <= 0) return false
                            val prefix = String(head, 0, n, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\r', '\n', '\t')
                            if (prefix.startsWith("<")) return false
                            return !type.startsWith("text/") || connection.getHeaderField("Content-Disposition").orEmpty().contains("attachment", true)
                        }
                    }
                } finally { connection.disconnect() }
            }
            false
        }.getOrDefault(false)
    }
}
