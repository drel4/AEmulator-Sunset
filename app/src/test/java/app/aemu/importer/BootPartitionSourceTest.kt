package app.aemu.importer

import app.aemu.core.BootPartitionImport
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.tar.*
import org.junit.Assert.*
import org.junit.Test

class BootPartitionSourceTest {
    @Test fun rejectsEscapingRamdiskLinksBeforeAnyFilesAreWritten() {
        val link = BootImage.CpioEntry("sbin/tool", 0xa1ff, "../../../../outside".toByteArray())
        assertThrows(IllegalArgumentException::class.java) { BootPartitionImport.validate(listOf(link)) }
        val safe = link.copy(data = "/system/bin/tool".toByteArray())
        assertEquals(listOf(safe), BootPartitionImport.validate(listOf(safe)))
    }
    private val boot = "ANDROID!".toByteArray() + ByteArray(2048)
    private fun fixture(run: (File) -> Unit) {
        val dir = Files.createTempDirectory("boot-source-test").toFile()
        try { run(dir) } finally { dir.deleteRecursively() }
    }
    @Test fun twrpWinIsDetectedAsRawBootBeforeItsExtension() = fixture { dir ->
        assertArrayEquals(boot, BootPartitionSource.read(ByteArrayInputStream(boot), "boot.emmc.win", dir))
    }
    @Test fun compressedTwrpBootIsRecognized() = fixture { dir ->
        val gzip = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(boot) } }.toByteArray()
        assertArrayEquals(boot, BootPartitionSource.read(ByteArrayInputStream(gzip), "boot.emmc.win.gz", dir))
    }
    @Test fun zipSelectsBootWithoutExtractingOtherPartitions() = fixture { dir ->
        val zip = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use {
            it.putNextEntry(ZipEntry("system/file")); it.write("system".toByteArray()); it.closeEntry()
            it.putNextEntry(ZipEntry("folder/boot.img")); it.write(boot); it.closeEntry()
        } }.toByteArray()
        assertArrayEquals(boot, BootPartitionSource.read(ByteArrayInputStream(zip), "rom.zip", dir))
        assertTrue(dir.list()!!.isEmpty())
    }
    @Test fun odinTarIsRecognized() = fixture { dir ->
        val tar = ByteArrayOutputStream().also { out -> TarArchiveOutputStream(out).use {
            it.putArchiveEntry(TarArchiveEntry("boot.img").apply { size = boot.size.toLong() }); it.write(boot); it.closeArchiveEntry()
        } }.toByteArray()
        assertArrayEquals(boot, BootPartitionSource.read(ByteArrayInputStream(tar), "AP.tar.md5", dir))
    }
    @Test fun ramdiskRejectsTraversalAndDoesNotImportSystemOrData() {
        fun entry(name: String) = BootImage.CpioEntry(name, 0x81a4, byteArrayOf(1))
        assertThrows(IllegalArgumentException::class.java) { BootPartitionImport.validate(listOf(entry("../../escape"))) }
        assertEquals(listOf("init.rc"), BootPartitionImport.validate(listOf(entry("init.rc"), entry("system/file"), entry("data/file"))).map { it.name })
        assertThrows(IllegalArgumentException::class.java) { BootPartitionImport.validate(listOf(entry("init.rc"), entry("init.rc"))) }
    }
    @Test fun existingRamdiskCountsAsBootEvenWithoutOriginalImage() = fixture { dir ->
        assertFalse(BootPartitionImport.present(dir))
        File(dir, "root").mkdir()
        File(dir, "root/init.rc").writeText("init")
        assertTrue(BootPartitionImport.present(dir))
    }
}
