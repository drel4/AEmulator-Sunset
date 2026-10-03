/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.core

import android.content.Context
import android.net.Uri
import android.system.Os
import java.io.File
import java.util.Locale
import org.json.JSONObject

internal object VmArchiveStorage {
    fun export(ctx: Context, img: GuestImage, data: Boolean, uri: Uri, progress: (String) -> Unit) {
        VmStorageLease(ctx.filesDir).use { lease ->
            lease.acquire()
            GuestStorageWriters.requireIdle(ctx)
            val paths = VmPaths(ctx, img.id)
            val owners = HashMap<String, Pair<Long, Long>>()
            if (paths.owners.isFile) paths.owners.forEachLine { line ->
                val fields = line.trim().split(Regex("\\s+"))
                if (fields.size == 4) runCatching {
                    owners[fields.take(2).joinToString(" ")] = fields[2].toLong(16) to fields[3].toLong(16)
                }
            }
            val json = img.copy(baseId = "").toJson().put("aessvmIncludesData", data).toString(2)
            (ctx.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open export file")).use { out ->
                VmArchive.export(paths.dir, json, data, out, { file ->
                    val st = Os.lstat(file.path)
                    val key = String.format(Locale.ROOT, "%016x %016x", st.st_dev, st.st_ino)
                    val pair = owners[key] ?: (0L to 0L)
                    VmArchive.Metadata(st.st_mode, pair.first, pair.second)
                }, progress)
            }
        }
    }

    fun restore(ctx: Context, uri: Uri, progress: (String) -> Unit): GuestImage {
        VmStorageLease(ctx.filesDir).use { lease ->
            lease.acquire()
            GuestStorageWriters.requireIdle(ctx)
            val id = ImageStore.newId(ctx)
            val paths = VmPaths(ctx, id)
            check(paths.dir.mkdir())
            try {
                val rows = StringBuilder()
                val json = (ctx.contentResolver.openInputStream(uri) ?: error("Cannot open VM archive")).use { input ->
                    VmArchive.restore(input, paths.dir, { file, mode -> Os.chmod(file.path, mode and 0xfff) }, { file, uid, gid ->
                        val st = Os.lstat(file.path)
                        rows.append(String.format(Locale.ROOT, "%016x %016x %08x %08x\n", st.st_dev, st.st_ino, uid, gid))
                    }, progress)
                }
                paths.owners.writeText(rows.toString())
                val img = GuestImage.fromJson(JSONObject(json)).copy(id = id, baseId = "",
                    createdAt = System.currentTimeMillis(), lastBootMs = 0, bootCount = 0,
                    sizeBytes = ImageStore.du(paths.dir))
                ImageStore.save(ctx, img)
                return img
            } catch (t: Throwable) { ImageStore.delete(ctx, id); throw t }
        }
    }
}
