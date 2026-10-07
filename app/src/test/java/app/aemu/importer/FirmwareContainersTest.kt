package app.aemu.importer

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

class FirmwareContainersTest {
    @Test fun gzipSignatureWinsEvenWhenCalledTgzTar() {
        val compressed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(ByteArray(1024)) } }.toByteArray()
        assertEquals(FirmwareContainers.Compression.GZIP, FirmwareContainers.compression(compressed))
        assertTrue(FirmwareContainers.nested("nexus/factory.tgz.tar"))
    }
    @Test fun tarWrapperRecognizesNestedTgzWithoutSizeThreshold() {
        val compressed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(ByteArray(1024)) } }.toByteArray()
        val wrapper = ByteArrayOutputStream().also { out -> TarArchiveOutputStream(out).use { tar ->
            tar.putArchiveEntry(TarArchiveEntry("nexus/factory.tgz").apply { size = compressed.size.toLong() })
            tar.write(compressed); tar.closeArchiveEntry()
        } }.toByteArray()
        TarArchiveInputStream(wrapper.inputStream()).use { tar ->
            val entry = tar.nextEntry!!
            assertTrue(FirmwareContainers.nested(entry.name))
            assertEquals(FirmwareContainers.Compression.GZIP, FirmwareContainers.compression(tar.readBytes()))
        }
    }
    @Test fun factoryImageZipAndOdinRemainNestedCandidates() {
        listOf("image-occam.zip", "AP_system.tar.md5", "rootfs.tar.gz", "system.tar.xz", "system.tar.bz2").forEach { assertTrue(it, FirmwareContainers.nested(it)) }
        listOf("MODEM.tar.md5", "CP_radio.tar", "CSC_settings.tar", "readme.txt").forEach { assertFalse(it, FirmwareContainers.nested(it)) }
    }
    @Test fun signaturesAreLengthChecked() {
        assertNull(FirmwareContainers.compression(byteArrayOf('B'.code.toByte(), 'Z'.code.toByte())))
        assertEquals(FirmwareContainers.Compression.BZIP2, FirmwareContainers.compression("BZh9".toByteArray()))
        assertEquals(FirmwareContainers.Compression.XZ, FirmwareContainers.compression(byteArrayOf(0xfd.toByte(), 0x37, 0x7a, 0x58, 0x5a, 0)))
    }
    @Test fun archiveRecursionIsBounded() {
        FirmwareContainers.checkDepth(8)
        assertThrows(IllegalArgumentException::class.java) { FirmwareContainers.checkDepth(9) }
    }
    @Test fun archivePathsCannotEscapeOrUseSiblingPrefix() {
        val root = java.nio.file.Files.createTempDirectory("firmware-path-test").toFile()
        try {
            assertEquals(java.io.File(root, "system/build.prop"), FirmwareContainers.destination(root, "system/build.prop"))
            for (path in listOf("system/../../outside", "../${root.name}-other/file", "/tmp/file", "system\\file"))
                assertThrows(IllegalArgumentException::class.java) { FirmwareContainers.destination(root, path) }
        } finally { root.deleteRecursively() }
    }
    @Test fun symlinksStayInsideGuestRoot() {
        FirmwareContainers.checkLink("etc", "/system/etc")
        FirmwareContainers.checkLink("system/etc/config", "../../data/config")
        assertThrows(IllegalArgumentException::class.java) { FirmwareContainers.checkLink("etc", "../outside") }
        assertThrows(IllegalArgumentException::class.java) { FirmwareContainers.checkLink("system/link", "../../outside") }
    }
}
