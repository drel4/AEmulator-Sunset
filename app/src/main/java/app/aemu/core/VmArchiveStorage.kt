/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.core

import android.content.Context
import android.net.Uri
import android.system.Os
import java.io.File
import java.util.Locale
import org.json.JSONObject

internal object VmArchiveStorage {
    fun export(ctx: Context, img: GuestImage, selection: VmArchive.Selection, uri: Uri, progress: (String) -> Unit) {
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
            val profile = img.copy(baseId = "").toJson().put("aessvmIncludesData", selection.data)
                .put("aessvmIncludesConfig", selection.config)
                .put("aessvmRomFingerprint", fingerprint(paths))
            if (!selection.config) profile.remove("settings")
            val json = profile.toString(2)
            (ctx.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open export file")).use { out ->
                VmArchive.export(paths.dir, json, selection, out, { file ->
                    val st = Os.lstat(file.path)
                    val key = String.format(Locale.ROOT, "%016x %016x", st.st_dev, st.st_ino)
                    val pair = owners[key] ?: (0L to 0L)
                    VmArchive.Metadata(st.st_mode, pair.first, pair.second)
                }, progress)
            }
        }
    }

    data class SettingsImport(val image: GuestImage, val fingerprint: String) {
        fun differs(ctx: Context, target: GuestImage): Boolean =
            image.brand != target.brand || image.model != target.model || image.api != target.api ||
            image.release != target.release || image.skin != target.skin ||
            fingerprint.isBlank() || fingerprint != VmArchiveStorage.fingerprint(VmPaths(ctx, target.id))
    }

    private fun fingerprint(paths: VmPaths): String = runCatching {
        PropArea.parseProps(File(paths.root, "system/build.prop").readText())["ro.build.fingerprint"].orEmpty()
    }.getOrDefault("")

    fun readSettings(ctx: Context, uri: Uri): SettingsImport {
        val profile = (ctx.contentResolver.openInputStream(uri) ?: error("Cannot open archive")).use { VmArchive.profile(it) }
        require(profile.selection.config) { "Archive does not include settings" }
        val json = JSONObject(profile.json)
        require(json.has("settings")) { "Archive does not include settings" }
        return SettingsImport(GuestImage.fromJson(json), json.optString("aessvmRomFingerprint"))
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
