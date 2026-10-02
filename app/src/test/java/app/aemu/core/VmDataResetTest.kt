/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class VmDataResetTest {
    @Test fun partialFailureRollsMovedDataBack() {
        val images = Files.createTempDirectory("reset-rollback").toFile()
        try {
            val root = File(images, "a9dyxj/root").apply { mkdirs() }
            File(root, "data").mkdir(); File(root, "data/keep").writeText("account")
            File(root, "cache").mkdir(); File(root, "cache/keep").writeText("cache")
            var calls = 0
            val result = runCatching { VmDataReset.reset(images, "a9dyxj") { from, to ->
                if (++calls == 2) error("simulated I/O failure")
                Files.move(from.toPath(), to.toPath()); Unit
            } }
            assertTrue(result.isFailure)
            assertEquals("account", File(root, "data/keep").readText())
            assertEquals("cache", File(root, "cache/keep").readText())
            assertEquals(listOf("root"), root.parentFile.list()!!.toList())
        } finally { images.deleteRecursively() }
    }
    @Test fun processDeathReleasesStorageLease() {
        val directory = Files.createTempDirectory("reset-process-lease").toFile()
        val java = File(System.getProperty("java.home"), "bin/java")
        val classpath = listOf(VmStorageLease::class.java, StorageLeaseProbe::class.java, kotlin.Unit::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).path }.distinct().joinToString(File.pathSeparator)
        val child = ProcessBuilder(java.path, "-cp", classpath, StorageLeaseProbe::class.java.name, directory.path)
            .redirectErrorStream(true).start()
        try {
            assertEquals("ready", child.inputStream.bufferedReader().readLine())
            VmStorageLease(directory).use { assertTrue(runCatching { it.acquire() }.isFailure) }
            child.destroyForcibly()
            assertTrue(child.waitFor(10, TimeUnit.SECONDS))
            VmStorageLease(directory).use { it.acquire() }
        } finally { child.destroyForcibly(); child.waitFor(10, TimeUnit.SECONDS); directory.deleteRecursively() }
    }
    @Test fun resetRetainsFirmwareSettingsAndRecoverableData() {
        val images = Files.createTempDirectory("reset-test").toFile()
        try {
            val dir = File(images, "a9dyxj").apply { mkdir() }
            val root = File(dir, "root").apply { mkdir() }
            for (name in listOf("data", "cache", "system")) {
                File(root, name).mkdir(); File(root, "$name/file").writeText(name)
            }
            File(root, "dhd.owners").writeText("system owners")
            File(root, "dhd.owners.seeded").writeText("stamp")
            File(dir, "image.json").writeText("settings")
            File(dir, "run").mkdir(); File(dir, "run/sony-setup-flow-v1").writeText("old")
            val backup = VmDataReset.reset(images, "a9dyxj")
            assertEquals("data", File(backup, "data/file").readText())
            assertEquals("cache", File(backup, "cache/file").readText())
            assertFalse(File(root, "data").exists())
            assertFalse(File(root, "dhd.owners.seeded").exists())
            assertEquals("system owners", File(root, "dhd.owners").readText())
            assertEquals("system", File(root, "system/file").readText())
            assertEquals("settings", File(dir, "image.json").readText())
            assertEquals("old", File(backup, "run/sony-setup-flow-v1").readText())
            assertNotEquals(backup, VmDataReset.reset(images, "a9dyxj"))
            assertTrue(File(backup, "data/file").isFile)
        } finally { images.deleteRecursively() }
    }
    @Test fun dataSymlinkIsMovedWithoutTouchingItsTarget() {
        val images = Files.createTempDirectory("reset-links").toFile()
        val outside = Files.createTempDirectory("reset-outside").toFile()
        try {
            File(outside, "keep").writeText("safe")
            val root = File(images, "a9dyxj/root").apply { mkdirs() }
            Files.createSymbolicLink(File(root, "data").toPath(), outside.toPath())
            val backup = VmDataReset.reset(images, "a9dyxj")
            assertTrue(Files.isSymbolicLink(File(backup, "data").toPath()))
            assertEquals("safe", File(outside, "keep").readText())
            Files.delete(File(backup, "data").toPath())
        } finally { images.deleteRecursively(); outside.deleteRecursively() }
    }
    @Test fun rejectsTraversalAndSymlinkedRoot() {
        val images = Files.createTempDirectory("reset-root").toFile()
        val outside = Files.createTempDirectory("reset-root-outside").toFile()
        try {
            val dir = File(images, "a9dyxj").apply { mkdir() }
            Files.createSymbolicLink(File(dir, "root").toPath(), outside.toPath())
            for (id in listOf("a9dyxj", "../out", "", "a9dyxj/../")) {
                assertTrue(runCatching { VmDataReset.reset(images, id) }.isFailure)
            }
            assertEquals(listOf("root"), dir.list()!!.toList())
            Files.delete(File(dir, "root").toPath())
        } finally { images.deleteRecursively(); outside.deleteRecursively() }
    }
    @Test fun storageLeaseBlocksResetAndBootCompetitors() {
        val directory = Files.createTempDirectory("reset-lease").toFile()
        try {
            VmStorageLease(directory).use { running ->
                running.acquire(); running.acquire()
                VmStorageLease(directory).use { competitor ->
                    assertTrue(runCatching { competitor.acquire() }.isFailure)
                    assertTrue(runCatching { competitor.acquire() }.isFailure)
                }
            }
            VmStorageLease(directory).use { it.acquire() }
        } finally { directory.deleteRecursively() }
    }
}

object StorageLeaseProbe {
    @JvmStatic fun main(args: Array<String>) {
        VmStorageLease(File(args.single())).use {
            it.acquire(); println("ready"); Thread.sleep(60_000)
        }
    }
}
