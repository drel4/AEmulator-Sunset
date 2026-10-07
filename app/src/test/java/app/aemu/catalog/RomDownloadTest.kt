package app.aemu.catalog

import java.net.ServerSocket
import java.net.InetAddress
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class RomDownloadTest {
    private val payload = byteArrayOf(0x1f, 0x8b.toByte(), 1, 2, 3)
    private fun fixture(block: (String, java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("rom-download-test").toFile()
        val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        data class Response(val type: String, val data: ByteArray, val length: Long)
        val responses = mutableMapOf<String, Response>()
        fun response(path: String, type: String, data: ByteArray, length: Long = data.size.toLong()) {
            responses[path] = Response(type, data, length)
        }
        response("/rom.aessvm", "application/octet-stream", payload)
        response("/html", "text/html", "<html>login</html>".toByteArray())
        response("/hidden-html", "application/octet-stream", "  <html>login</html>".toByteArray())
        response("/short", "application/octet-stream", payload, 100)
        response("/oversize", "application/octet-stream", payload, 20L * 1024 * 1024 * 1024)
        val worker = Thread {
            while (!server.isClosed) runCatching {
                server.accept().use { socket ->
                    socket.soTimeout = 2000
                    val reader = socket.getInputStream().bufferedReader()
                    val path = reader.readLine().split(' ')[1]
                    while (!reader.readLine().isNullOrEmpty()) { /* request headers */ }
                    val output = socket.getOutputStream()
                    val location = when (path) { "/redirect" -> "/rom.aessvm"; "/loop" -> "/loop"; else -> null }
                    if (location != null) output.write("HTTP/1.1 302 Found\r\nLocation: $location\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    else {
                        val reply = responses.getValue(path)
                        output.write("HTTP/1.1 200 OK\r\nContent-Type: ${reply.type}\r\nContent-Length: ${reply.length}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(reply.data)
                    }
                    output.flush()
                }
            }
        }.apply { isDaemon = true; start() }
        try { block("http://127.0.0.1:${server.localPort}", directory) }
        finally { server.close(); worker.join(3000); directory.deleteRecursively() }
    }
    @Test fun downloadPreservesArchiveSuffixAndVerifiesChecksum() = fixture { base, dir ->
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
        var bytes = 0L
        val file = RomDownload.download(RomDownloadRequest("CM", "$base/rom.aessvm", sha), dir, { false }) { n, _ -> bytes = n }
        assertTrue(file.name.endsWith(".aessvm")); assertArrayEquals(payload, file.readBytes()); assertEquals(5L, bytes)
    }
    @Test fun redirectsUseFinalFileName() = fixture { base, dir ->
        val file = RomDownload.download(RomDownloadRequest("CM", "$base/redirect"), dir, { false }) { _, _ -> }
        assertTrue(file.name.endsWith(".aessvm"))
    }
    @Test fun htmlIsNotImportedAndLeavesNoTemporaryFiles() = fixture { base, dir ->
        for (path in listOf("/html", "/hidden-html")) {
            val error = assertThrows(RomDownloadException::class.java) { RomDownload.download(RomDownloadRequest("ROM", base + path), dir, { false }) { _, _ -> } }
            assertEquals(RomDownloadException.Reason.WEB_PAGE, error.reason)
            assertTrue(dir.list()!!.isEmpty())
        }
    }
    @Test fun wrongChecksumRemovesDownload() = fixture { base, dir ->
        val error = assertThrows(RomDownloadException::class.java) { RomDownload.download(RomDownloadRequest("ROM", "$base/rom.aessvm", "0".repeat(64)), dir, { false }) { _, _ -> } }
        assertEquals(RomDownloadException.Reason.CHECKSUM, error.reason); assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun cancellationRemovesPartialDownload() = fixture { base, dir ->
        var cancel = false
        val error = assertThrows(RomDownloadException::class.java) { RomDownload.download(RomDownloadRequest("ROM", "$base/rom.aessvm"), dir, { cancel }) { _, _ -> cancel = true } }
        assertEquals(RomDownloadException.Reason.CANCELLED, error.reason); assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun oversizeIsRejectedBeforeWriting() = fixture { base, dir ->
        assertThrows(RomDownloadException::class.java) { RomDownload.download(RomDownloadRequest("ROM", "$base/oversize"), dir, { false }) { _, _ -> } }
        assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun redirectLoopsAreBounded() = fixture { base, dir ->
        assertThrows(IllegalArgumentException::class.java) { RomDownload.download(RomDownloadRequest("ROM", "$base/loop"), dir, { false }) { _, _ -> } }
        assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun incompleteResponseIsRejectedAndCleaned() = fixture { base, dir ->
        assertThrows(java.io.IOException::class.java) { RomDownload.download(RomDownloadRequest("ROM", "$base/short"), dir, { false }) { _, _ -> } }
        assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun fileNamesArePortableAndCannotEscapeDownloadDirectory() {
        assertEquals("rom.aessvm", RomDownload.fileName("https://example.org/download", "attachment; filename=\"../../rom.aessvm\""))
        assertEquals("rom.aessvm", RomDownload.fileName("https://example.org/download", "attachment; filename*=UTF-8''rom.aessvm"))
        assertEquals("factory.tgz.tar", RomDownload.fileName("https://example.org/factory.tgz.tar?token=secret", null))
    }
    @Test fun shareHostsGoToBrowserWithoutNetworking() {
        for (url in listOf("https://drive.google.com/file/d/id/view", "https://mega.nz/file/id#key",
            "https://www.mediafire.com/file/id/rom.zip/file", "https://disk.yandex.ru/d/id",
            "https://www.dropbox.com/s/id/rom.zip?dl=0", "https://1drv.ms/u/id")) {
            assertTrue(url, RomLink.browserOnly(url)); assertFalse(url, RomLink.isDirect(url))
        }
    }
    @Test fun hostMatchingCannotBeSpoofedBySuffixOrQuery() {
        assertFalse(RomLink.browserOnly("https://mega.nz.example.org/rom.zip"))
        assertFalse(RomLink.browserOnly("https://example.org/rom.zip?next=drive.google.com"))
        assertFalse(RomLink.browserOnly("https://download123.mediafire.com/id/rom.zip"))
        assertFalse(RomLink.browserOnly("https://www.dropbox.com/s/id/rom.zip?dl=1"))
    }
    @Test fun genericWebPagesAndDisguisedHtmlOpenBrowser() = fixture { base, _ ->
        assertFalse(RomLink.isDirect("$base/html"))
        assertFalse(RomLink.isDirect("$base/hidden-html"))
    }
    @Test fun binaryAndExtensionlessRedirectsStayInApp() = fixture { base, _ ->
        assertTrue(RomLink.isDirect("$base/rom.aessvm"))
        assertTrue(RomLink.isDirect("$base/redirect"))
    }
    @Test fun routingRejectsRedirectLoops() = fixture { base, _ ->
        assertFalse(RomLink.isDirect("$base/loop"))
    }
}
