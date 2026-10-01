package app.aemu.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraHalFallbackTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun fixture(): Pair<File, File> {
        val root = temporary.newFolder()
        val source = File(root, "system/lib/hw/camera.aemu_host.so")
        source.parentFile!!.mkdirs(); source.writeText("host camera")
        return root to source
    }
    @Test fun installsRefreshesAndRemovesOwnedFallback() {
        val (root, source) = fixture()
        val destination = File(source.parentFile, "camera.default.so")
        assertTrue(CameraHalFallback.configure(root, source, true))
        assertEquals("host camera", destination.readText())
        assertFalse(CameraHalFallback.configure(root, source, true))
        source.writeText("updated host camera")
        assertTrue(CameraHalFallback.configure(root, source, true))
        assertEquals(source.readText(), destination.readText())
        assertTrue(CameraHalFallback.configure(root, source, false))
        assertFalse(destination.exists())
        assertFalse(CameraHalFallback.configure(root, source, false))
    }
    @Test fun preservesAndRestoresExistingDefault() {
        val (root, source) = fixture()
        val destination = File(source.parentFile, "camera.default.so")
        destination.writeText("original ROM HAL")
        CameraHalFallback.configure(root, source, true)
        CameraHalFallback.configure(root, source, false)
        assertEquals("original ROM HAL", destination.readText())
    }
    @Test fun preservesAndRestoresDefaultSymlink() {
        val (root, source) = fixture()
        val stock = File(source.parentFile, "camera.stock.so").apply { writeText("stock") }
        val destination = File(source.parentFile, "camera.default.so")
        Files.createSymbolicLink(destination.toPath(), stock.toPath().fileName)
        CameraHalFallback.configure(root, source, true)
        CameraHalFallback.configure(root, source, false)
        assertEquals(stock.toPath().fileName, Files.readSymbolicLink(destination.toPath()))
        assertEquals("stock", destination.readText())
    }
    @Test(expected = IllegalStateException::class) fun doesNotOverwriteExternalChangesOnDisable() {
        val (root, source) = fixture()
        CameraHalFallback.configure(root, source, true)
        File(source.parentFile, "camera.default.so").writeText("user changed this")
        CameraHalFallback.configure(root, source, false)
    }
}
