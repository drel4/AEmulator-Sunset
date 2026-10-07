package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Test
import org.junit.Assert.*

class GuestRootAliasesTest {
    private fun fixture(test: (File) -> Unit) {
        val root = Files.createTempDirectory("aess-root-alias").toFile()
        try {
            File(root, "system/etc").mkdirs()
            File(root, "system/etc/media_codecs.xml").writeText("<MediaCodecs/>")
            test(root)
        } finally { root.deleteRecursively() }
    }
    @Test fun missingAliasExposesExistingCodecConfig() = fixture { root ->
        assertTrue(GuestRootAliases.ensureEtc(root))
        assertEquals("system/etc", Files.readSymbolicLink(File(root, "etc").toPath()).toString())
        assertEquals("<MediaCodecs/>", File(root, "etc/media_codecs.xml").readText())
        assertFalse(GuestRootAliases.ensureEtc(root))
    }
    @Test fun preservesVendorDirectory() = fixture { root ->
        File(root, "etc").mkdir()
        File(root, "etc/vendor.conf").writeText("keep")
        assertFalse(GuestRootAliases.ensureEtc(root))
        assertEquals("keep", File(root, "etc/vendor.conf").readText())
    }
    @Test fun preservesCustomLink() = fixture { root ->
        Files.createSymbolicLink(File(root, "etc").toPath(), Paths.get("vendor/etc"))
        assertFalse(GuestRootAliases.ensureEtc(root))
        assertEquals("vendor/etc", Files.readSymbolicLink(File(root, "etc").toPath()).toString())
    }
    @Test fun repairsKnownAbsoluteGuestLink() = fixture { root ->
        Files.createSymbolicLink(File(root, "etc").toPath(), Paths.get("/system/etc"))
        assertTrue(GuestRootAliases.ensureEtc(root))
        assertEquals("<MediaCodecs/>", File(root, "etc/media_codecs.xml").readText())
    }
    @Test fun missingSystemConfigDoesNotCreateDanglingAlias() = fixture { root ->
        File(root, "system/etc").deleteRecursively()
        assertFalse(GuestRootAliases.ensureEtc(root))
        assertFalse(Files.isSymbolicLink(File(root, "etc").toPath()))
    }
}
