package app.aemu.core

import android.content.Context
import android.system.Os
import org.json.JSONObject
import java.io.File

/** Расположение файлов одного образа внутри песочницы приложения. */
class VmPaths(val ctx: Context, val id: String) {
    val dir: File = File(ImageStore.imagesDir(ctx), id)
    /** дерево гостя: сюда смотрит qemu через -L */
    val root: File = File(dir, "root")
    /** рабочий каталог: сокеты хоста, журналы, копии свойств */
    val bin: File = File(dir, "run")
    val meta: File = File(dir, "image.json")
    val props: File get() = File(root, "dev/__properties__")
    val propsTemplate: File get() = File(dir, "props.base")
    val fb: File get() = File(root, "dev/graphics/fb0")
    val binderSock: File get() = File(bin, "binder.sock")
    val inputSock: File get() = File(bin, "input.sock")
    val frameSock: File get() = File(bin, "frame.sock")
    val glSock: File get() = File(root, "dev/socket/gl")
    val creds: File get() = File(bin, "creds")
    val owners: File get() = File(root, "dhd.owners")
    val snapshots: File get() = File(dir, "snapshots")
    fun socket(name: String) = File(root, "dev/socket/$name")
    fun log(name: String) = File(bin, "$name.log")

    fun nativeBin(name: String): File = File(ctx.applicationInfo.nativeLibraryDir, name)
}

object ImageStore {
    fun imagesDir(ctx: Context): File = File(ctx.filesDir, "images").apply { mkdirs() }

    fun list(ctx: Context): List<GuestImage> =
        imagesDir(ctx).listFiles()?.mapNotNull { d ->
            val f = File(d, "image.json")
            if (!f.isFile) null else runCatching { GuestImage.fromJson(JSONObject(f.readText())) }.getOrNull()
        }?.sortedByDescending { it.createdAt } ?: emptyList()

    fun get(ctx: Context, id: String): GuestImage? {
        val f = VmPaths(ctx, id).meta
        return if (f.isFile) runCatching { GuestImage.fromJson(JSONObject(f.readText())) }.getOrNull() else null
    }

    fun save(ctx: Context, img: GuestImage) {
        val p = VmPaths(ctx, img.id)
        p.dir.mkdirs()
        val tmp = File(p.dir, "image.json.tmp")
        tmp.writeText(img.toJson().toString(2))
        tmp.renameTo(p.meta)
    }

    fun newId(ctx: Context): String {
        val abc = "abcdefghijkmnpqrstuvwxyz23456789"
        while (true) {
            val id = (1..6).map { abc.random() }.joinToString("")
            if (!File(imagesDir(ctx), id).exists()) return id
        }
    }

    /** Удаляет каталог образа, не проходя по символьным ссылкам наружу. */
    fun delete(ctx: Context, id: String) {
        wipe(VmPaths(ctx, id).dir)
    }

    /**
     * New container of [src]'s firmware: a full own copy of the root including /system (flashing in one container
     * never touches another), a fresh /data (factory reset: apps, settings and accounts start empty; dalvik-cache is kept
     * so the first boot is quick) or a full copy of it, its own memory-card folder and settings.
     */
    fun clone(ctx: Context, src: GuestImage, name: String, copyData: Boolean, progress: (String) -> Unit): GuestImage {
        val from = VmPaths(ctx, src.id)
        val need = du(from.root) + du(File(from.root, "system").canonicalFile).takeIf { File(from.root, "system").canonicalPath != File(from.root, "system").path } .let { it ?: 0 }
        val free = imagesDir(ctx).usableSpace
        if (need + (200L shl 20) > free) error("not enough space: need ${need shr 20} MB, free ${free shr 20} MB")
        val id = newId(ctx)
        val to = VmPaths(ctx, id)
        try {
            to.root.mkdirs()
            val skipData = setOf("data", "system", "user", "property", "anr", "tombstones", "backup", "misc/wifi", "misc/keystore",
                "local/tmp", "app", "app-private", "app-asec")
            fun copy(a: File, b: File, rel: String) {
                val st = runCatching { Os.lstat(a.path) }.getOrNull() ?: return
                when {
                    android.system.OsConstants.S_ISLNK(st.st_mode) -> runCatching { Os.symlink(Os.readlink(a.path), b.path) }
                    android.system.OsConstants.S_ISDIR(st.st_mode) -> {
                        b.mkdirs()
                        for (c in a.list().orEmpty()) {
                            val r = if (rel.isEmpty()) c else "$rel/$c"
                            if (rel.isEmpty() && (c == "dev" || c == "proc")) continue
                            if (rel.isEmpty() && c == "system") progress("system")
                            if (!copyData && r.startsWith("data/") && r.removePrefix("data/") in skipData) { File(b, c).mkdirs(); continue }
                            if (rel.isEmpty() && c == "data") progress("data")
                            copy(File(a, c), File(b, c), r)
                        }
                    }
                    android.system.OsConstants.S_ISREG(st.st_mode) -> {
                        a.inputStream().use { i -> b.outputStream().use { i.copyTo(it, 1 shl 16) } }
                        runCatching { Os.chmod(b.path, st.st_mode and 0xfff) }
                    }
                }
            }
            progress("root")
            // an older container's /system may be a link to its original: copy what it points to
            val sys = File(from.root, "system").canonicalFile
            copy(from.root, to.root, "")
            if (runCatching { android.system.OsConstants.S_ISLNK(Os.lstat(File(to.root, "system").path).st_mode) }.getOrDefault(false)) {
                File(to.root, "system").delete(); copy(sys, File(to.root, "system"), "system")
            }
            File(to.root, "dev").mkdirs()
            // ownership table is keyed by inode: the fresh copy gets its own on first boot
            if (!copyData) { File(to.root, "dhd.owners").delete(); File(to.root, "dhd.owners.seeded").delete() }
            listOf("props.base").forEach { n -> File(from.dir, n).takeIf { it.isFile }?.copyTo(File(to.dir, n), true) }
            val img = src.copy(id = id, name = name, baseId = src.id, createdAt = System.currentTimeMillis(),
                lastBootMs = 0, bootCount = 0, sizeBytes = du(to.dir))
            save(ctx, img)
            return img
        } catch (t: Throwable) {
            wipe(to.dir)
            throw t
        }
    }

    fun wipe(f: File) {
        val isLink = runCatching { android.system.OsConstants.S_ISLNK(Os.lstat(f.path).st_mode) }.getOrDefault(false)
        if (!isLink && f.isDirectory) { f.setWritable(true, true); f.listFiles()?.forEach { wipe(it) } }
        f.delete()
    }

    fun du(f: File): Long {
        var n = 0L
        val st = ArrayDeque<File>().apply { add(f) }
        while (st.isNotEmpty()) {
            val x = st.removeLast()
            val isLink = runCatching { android.system.OsConstants.S_ISLNK(Os.lstat(x.path).st_mode) }.getOrDefault(false)
            if (isLink) continue
            if (x.isDirectory) x.listFiles()?.forEach { st.add(it) } else n += x.length()
        }
        return n
    }
}
