package app.aemu.core

import android.system.Os
import app.aemu.importer.BootImage
import java.io.File

/**
 * Recovery mode: the ramdisk of a recovery.img (stock, TWRP, OrangeFox, CWM…) unpacked into <image>/recovery.
 * The VM runs its /sbin/recovery under qemu with that folder as "/", sharing the system's framebuffer and touch.
 */
object RecoveryImage {
    fun dir(paths: VmPaths) = File(paths.dir, "recovery")
    fun installed(paths: VmPaths) = File(dir(paths), "sbin/recovery").isFile
    fun fb(paths: VmPaths) = File(dir(paths), "dev/graphics/fb0")

    /** Unpacks a recovery (or boot) image; false when it holds no /sbin/recovery. */
    fun install(paths: VmPaths, image: ByteArray, log: (String) -> Unit): Boolean {
        val rd = runCatching { BootImage.ramdisk(image) }.getOrNull()
        if (rd.isNullOrEmpty() || rd.none { it.name == "sbin/recovery" }) { log("recovery: image has no /sbin/recovery"); return false }
        val out = dir(paths)
        out.deleteRecursively()
        out.mkdirs()
        val base = out.canonicalPath
        for (e in rd) {
            val n = e.name.trimStart('/')
            if (n.isEmpty() || n == ".") continue
            val f = File(out, n)
            if (!f.canonicalPath.startsWith(base)) continue
            when (e.mode and 0xF000) {
                0x4000 -> f.mkdirs()
                0xA000 -> { f.parentFile?.mkdirs(); runCatching { Os.symlink(String(e.data), f.path) } }
                0x8000 -> {
                    f.parentFile?.mkdirs(); f.writeBytes(e.data)
                    if (e.mode and 0x49 != 0) f.setExecutable(true, false)
                    f.setReadable(true, false)
                }
            }
        }
        val name = runCatching { File(out, "default.prop").readText() }.getOrDefault("")
            .let { Regex("twrp", RegexOption.IGNORE_CASE).containsMatchIn(it) }.let { if (it) "TWRP" else "" }
        log("recovery: installed ${rd.size} files ${name}".trim())
        return true
    }

    /** Runtime pieces the ramdisk lacks: device nodes qemu emulates, mount points, the shared framebuffer. */
    fun prepare(paths: VmPaths, sdcard: File?) {
        val r = dir(paths)
        for (d in listOf("dev/graphics", "dev/input", "tmp", "cache", "data", "system", "sdcard", "proc", "sys")) File(r, d).mkdirs()
        // no tty0: minui gives up on graphics when it opens one but KDSETMODE fails; without it, it skips the step
        File(r, "dev/tty0").delete()
        // its own framebuffer (qemu only emulates an fb0 inside the guest root; links out of it are not followed
        // or not allowed), sized like the system's; the screen view shows it in recovery mode
        val fb = fb(paths)
        val size = paths.fb.length().takeIf { it > 0 } ?: 0L
        runCatching {
            if (java.nio.file.Files.isSymbolicLink(fb.toPath())) fb.delete()
            if (size > 0 && fb.length() != size) java.io.RandomAccessFile(fb, "rw").use { it.setLength(size) }
        }
        runCatching { File(r, "dhd.fbgeom").writeText(File(paths.root, "dhd.fbgeom").readText()) }
        // the system's partitions and memory card, for recoveries that browse or flash files
        for ((name, target) in listOf("system" to File(paths.root, "system"), "data" to File(paths.root, "data"))) {
            val link = File(r, name)
            if (link.isDirectory && link.list().isNullOrEmpty()) { link.delete(); runCatching { Os.symlink(target.absolutePath, link.path) } }
        }
        if (sdcard != null) {
            val link = File(r, "sdcard")
            if (link.isDirectory && link.list().isNullOrEmpty()) { link.delete(); runCatching { Os.symlink(sdcard.absolutePath, link.path) } }
        }
    }
}
