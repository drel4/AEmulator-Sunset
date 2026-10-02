/* Modified for AEmulator Sunset, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class VmDataResetTest {
    private fun fixture(action: (File, File) -> Unit) {
        val images = Files.createTempDirectory("permanent-reset").toFile()
        val root = File(images, "a9dyxj/root").apply { mkdirs() }
        try { action(images, root) } finally { images.deleteRecursively() }
    }
    @Test fun permanentlyDeletesDataCacheRuntimeWithoutBackup() = fixture { images, root ->
        for (name in listOf("data", "cache", "system")) {
            File(root, name).mkdir(); File(root, "$name/keep").writeText(name)
        }
        File(root, "dhd.owners.seeded").writeText("old")
        File(root.parentFile, "run").mkdir(); File(root.parentFile, "run/state").writeText("old")
        File(root.parentFile, "image.json").writeText("settings")
        File(root, "dhd.owners").writeText("data keep 1000 1000\nsystem keep 0 0\n")
        VmDataReset.reset(images, "a9dyxj") { if (it.path.contains("${File.separator}data${File.separator}")) "data keep" else "other key" }
        assertFalse(File(root, "data").exists()); assertFalse(File(root, "cache").exists())
        assertFalse(File(root.parentFile, "run").exists())
        assertEquals("system", File(root, "system/keep").readText())
        assertEquals("settings", File(root.parentFile, "image.json").readText())
        assertEquals("system keep 0 0\n", File(root, "dhd.owners").readText())
        assertEquals(setOf("root", "image.json"), root.parentFile!!.list()!!.toSet())
        VmDataReset.reset(images, "a9dyxj") { "other key" }
    }
    @Test fun neverFollowsDataOrNestedSymlinks() = fixture { images, root ->
        val outside = Files.createTempDirectory("reset-link-target").toFile()
        try {
            File(outside, "keep").writeText("safe")
            Files.createSymbolicLink(File(root, "data").toPath(), outside.toPath())
            File(root, "cache").mkdir()
            Files.createSymbolicLink(File(root, "cache/link").toPath(), outside.toPath())
            VmDataReset.reset(images, "a9dyxj") { "other key" }
            assertEquals("safe", File(outside, "keep").readText())
            assertFalse(Files.exists(File(root, "data").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS))
        } finally { outside.deleteRecursively() }
    }
    @Test fun unsafeIdAndLinkedRootAreRejectedBeforeDeletion() = fixture { images, root ->
        File(root, "data").mkdir(); File(root, "data/keep").writeText("safe")
        assertTrue(runCatching { VmDataReset.reset(images, "../out") { "x y" } }.isFailure)
        assertEquals("safe", File(root, "data/keep").readText())
        val linked = File(images, "b9dyxj").apply { mkdir() }
        Files.createSymbolicLink(File(linked, "root").toPath(), root.toPath())
        assertTrue(runCatching { VmDataReset.reset(images, "b9dyxj") { "x y" } }.isFailure)
        assertEquals("safe", File(root, "data/keep").readText())
        Files.delete(File(linked, "root").toPath())
    }
    @Test fun storageLeaseStillExcludesCompetingMutators() = fixture { images, _ ->
        VmStorageLease(images).use { owner ->
            owner.acquire()
            VmStorageLease(images).use { assertTrue(runCatching { it.acquire() }.isFailure) }
        }
        VmStorageLease(images).use { it.acquire() }
    }
}
