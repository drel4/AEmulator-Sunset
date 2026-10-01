package app.aemu.catalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CatalogLoader {
    private const val MAX_BYTES = 1024 * 1024
    suspend fun load(source: String): RomCatalog = withContext(Dispatchers.IO) {
        var url = CatalogUrls.valid(source) ?: error("Invalid catalog URL")
        repeat(6) { hop ->
            coroutineContext.ensureActive()
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "text/plain")
                connection.setRequestProperty("User-Agent", "AEmulator-Sunset-RomCatalog")
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    require(hop < 5) { "Too many redirects" }
                    val location = connection.getHeaderField("Location") ?: error("Redirect without location")
                    val next = CatalogUrls.valid(URL(URL(url), location).toExternalForm()) ?: error("Unsafe redirect")
                    require(!url.startsWith("https:", true) || next.startsWith("https:", true)) { "HTTPS downgrade rejected" }
                    url = next
                } else {
                    require(code == 200) { "HTTP $code" }
                    require(connection.contentLengthLong <= MAX_BYTES) { "Catalog exceeds 1 MiB" }
                    val bytes = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            require(bytes.size() + n <= MAX_BYTES) { "Catalog exceeds 1 MiB" }
                            bytes.write(buffer, 0, n)
                        }
                    }
                    val text = bytes.toString("UTF-8")
                    require(!text.trimStart().startsWith("<")) { "Server returned HTML, not rom.list" }
                    return@withContext RomCatalogParser.parse(text)
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
}
