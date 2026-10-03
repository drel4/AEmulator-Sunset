package app.aemu.core

import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.*

class VmArchiveTest {
    private fun fixture(run: (File, File) -> Unit) {
        val base = Files.createTempDirectory("aessvm-test").toFile()
        try {
            val source = File(base, "source").apply { mkdir() }
            File(source, "root/system/bin").mkdirs()
            File(source, "root/system/bin/sh").writeText("shell")
            File(source, "root/data/private").apply { parentFile.mkdirs(); writeText("secret") }
            File(source, "root/dev/socket").mkdirs()
            File(source, "root/dev/socket/host").writeText("not portable")
            File(source, "root/dhd.owners").writeText("old inode keys")
            File(source, "boot.img").writeText("boot")
            File(source, "props.base").writeText("properties")
            File(source, "root/init.rc").writeText("init")
            run(source, File(base, "destination").apply { mkdir() })
        } finally { base.deleteRecursively() }
    }
    private fun export(source: File, data: Boolean): ByteArray = ByteArrayOutputStream().also { out ->
        VmArchive.export(source, "settings", data, out, { VmArchive.Metadata(0x1ed, 10001, 10002) })
    }.toByteArray()
    private fun restore(bytes: ByteArray, dest: File): String = VmArchive.restore(ByteArrayInputStream(bytes), dest, { _, _ -> }, { _, _, _ -> })

    @Test fun roundTripIncludesSettingsBootSystemAndOptionalData() = fixture { src, dest ->
        assertEquals("settings", restore(export(src, true), dest))
        assertEquals("shell", File(dest, "root/system/bin/sh").readText())
        assertEquals("secret", File(dest, "root/data/private").readText())
        assertEquals("boot", File(dest, "boot.img").readText())
        assertEquals("init", File(dest, "root/init.rc").readText())
        assertFalse(File(dest, "root/dev").exists())
        assertFalse(File(dest, "root/dhd.owners").exists())
    }
    @Test fun excludesDataByDefaultChoice() = fixture { src, dest ->
        restore(export(src, false), dest)
        assertFalse(File(dest, "root/data").exists())
    }
    @Test fun retainsGuestSymlinksButNotHostLinks() = fixture { src, dest ->
        Files.createSymbolicLink(File(src, "root/system/bin/tool").toPath(), java.nio.file.Paths.get("sh"))
        Files.createSymbolicLink(File(src, "root/system/bin/host").toPath(), src.parentFile.toPath())
        restore(export(src, false), dest)
        assertEquals("sh", Files.readSymbolicLink(File(dest, "root/system/bin/tool").toPath()).toString())
        assertFalse(Files.exists(File(dest, "root/system/bin/host").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS))
    }
    @Test fun ownershipAndModesArePortable() = fixture { src, dest ->
        val owners = mutableListOf<Pair<Long, Long>>()
        val modes = mutableListOf<Int>()
        VmArchive.restore(ByteArrayInputStream(export(src, true)), dest, { _, mode -> modes += mode }, { _, uid, gid -> owners += uid to gid })
        assertTrue(owners.contains(10001L to 10002L))
        assertTrue(modes.isNotEmpty())
    }
    @Test fun supportsMissingOriginalBootImage() = fixture { src, dest ->
        File(src, "boot.img").delete()
        restore(export(src, false), dest)
        assertFalse(File(dest, "boot.img").exists())
        assertTrue(File(dest, "root/init.rc").isFile)
    }
    private fun badArchive(name: String, link: String? = null): ByteArray = ByteArrayOutputStream().also { out ->
        TarArchiveOutputStream(GZIPOutputStream(out)).use { tar ->
            tar.putArchiveEntry(TarArchiveEntry("aessvm.version").apply { size = 2 }); tar.write("1\n".toByteArray()); tar.closeArchiveEntry()
            val entry = if (link == null) TarArchiveEntry(name) else TarArchiveEntry(name, TarConstants.LF_SYMLINK).apply { linkName = link }
            tar.putArchiveEntry(entry); tar.closeArchiveEntry()
        }
    }.toByteArray()
    @Test fun rejectsTraversal() = fixture { _, dest ->
        assertThrows(IllegalArgumentException::class.java) { restore(badArchive("root/../../escape"), dest) }
        assertFalse(File(dest.parentFile, "escape").exists())
    }
    @Test fun rejectsEscapingLinks() = fixture { _, dest ->
        assertThrows(IllegalArgumentException::class.java) { restore(badArchive("root/system/link", "../../../escape"), dest) }
    }
    @Test fun rejectsRuntimeFiles() = fixture { _, dest ->
        assertThrows(IllegalArgumentException::class.java) { restore(badArchive("root/dev/socket"), dest) }
    }
    @Test fun materializesSharedSystem() = fixture { src, dest ->
        val original = File(src, "root/system")
        val shared = File(src.parentFile, "shared-system")
        assertTrue(original.renameTo(shared))
        Files.createSymbolicLink(original.toPath(), shared.toPath())
        Files.createSymbolicLink(File(shared, "bin/tool").toPath(), java.nio.file.Paths.get("sh"))
        restore(export(src, false), dest)
        assertFalse(Files.isSymbolicLink(File(dest, "root/system").toPath()))
        assertEquals("shell", File(dest, "root/system/bin/tool").readText())
    }
    @Test fun rejectsTruncatedArchive() = fixture { src, dest ->
        val bytes = export(src, true)
        assertThrows(Exception::class.java) { restore(bytes.copyOf(bytes.size / 2), dest) }
    }
    @Test fun rejectsDuplicatePaths() = fixture { _, dest ->
        val bytes = ByteArrayOutputStream().also { out ->
            TarArchiveOutputStream(GZIPOutputStream(out)).use { tar ->
                tar.putArchiveEntry(TarArchiveEntry("aessvm.version").apply { size = 2 }); tar.write("1\n".toByteArray()); tar.closeArchiveEntry()
                repeat(2) { tar.putArchiveEntry(TarArchiveEntry("root/system/")); tar.closeArchiveEntry() }
            }
        }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { restore(bytes, dest) }
    }
    @Test fun rejectsUnsupportedVersion() = fixture { _, dest ->
        val bytes = ByteArrayOutputStream().also { out ->
            TarArchiveOutputStream(GZIPOutputStream(out)).use { tar ->
                tar.putArchiveEntry(TarArchiveEntry("aessvm.version").apply { size = 2 }); tar.write("2\n".toByteArray()); tar.closeArchiveEntry()
            }
        }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { restore(bytes, dest) }
    }
}
