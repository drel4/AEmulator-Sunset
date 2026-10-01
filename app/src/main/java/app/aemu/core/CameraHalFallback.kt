package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Old libhardware ignores ro.hardware.camera; preserve/restore its default slot. */
object CameraHalFallback {
    fun configure(root: File, source: File, enabled: Boolean): Boolean {
        val destination = File(root, "system/lib/hw/camera.default.so")
        val marker = File(root, "system/.aemu-camera-default-host")
        val backup = File(root, "system/.aemu-parked/system#lib#hw#camera.default.so.host-camera")
        fun present(file: File) = Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)
        fun digest(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        fun owned() = !present(destination) || (destination.isFile && marker.readText() == digest(destination))
        fun matches() = destination.isFile && source.isFile &&
            destination.length() == source.length() && destination.readBytes().contentEquals(source.readBytes())
        if (!enabled) {
            if (!marker.isFile) return false
            check(owned()) { "camera fallback changed externally; refusing to overwrite it" }
            Files.deleteIfExists(destination.toPath())
            if (present(backup)) Files.move(backup.toPath(), destination.toPath())
            Files.delete(marker.toPath())
            return true
        }
        check(source.isFile) { "host camera HAL missing" }
        destination.parentFile!!.mkdirs()
        if (!marker.isFile) {
            check(!present(backup)) { "unmarked camera fallback backup exists; refusing to overwrite it" }
            if (present(destination)) {
                backup.parentFile!!.mkdirs()
                Files.move(destination.toPath(), backup.toPath())
            }
            marker.writeText("pending")
        } else check(owned()) { "camera fallback changed externally; refusing to overwrite it" }
        if (matches()) return false
        // The source is an emulator-owned HAL, never a stock APK/ODEX.
        val temporary = File(destination.parentFile, "camera.default.so.aemu-new")
        Files.copy(source.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING)
        temporary.setReadable(true, false); temporary.setExecutable(true, false)
        Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        marker.writeText(digest(destination))
        return true
    }
}
