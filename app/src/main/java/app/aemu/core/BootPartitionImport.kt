/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.system.Os
import app.aemu.importer.Analyzer
import app.aemu.importer.BootImage
import app.aemu.importer.BootPartitionSource
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS

internal object BootPartitionImport {
    fun present(dir: File) = File(dir, "boot.img").isFile || File(dir, "root/init.rc").isFile

    fun install(ctx: Context, id: String, uri: Uri) {
        VmStorageLease(ctx.filesDir).use { lease ->
            lease.acquire()
            GuestStorageWriters.requireIdle(ctx)
            val paths = VmPaths(ctx, id)
            val old = ImageStore.get(ctx, id) ?: error("VM no longer exists")
            require(!present(paths.dir)) { "VM already has a boot partition" }
            require(!Files.exists(File(paths.dir, "boot.img").toPath(), NOFOLLOW_LINKS) &&
                !Files.exists(File(paths.root, "init.rc").toPath(), NOFOLLOW_LINKS)) { "Conflicting boot path" }
            require(!Files.isSymbolicLink(paths.propsTemplate.toPath())) { "Unsafe property template" }
            for (dir in listOf(paths.dir, paths.root)) require(dir.isDirectory && !Files.isSymbolicLink(dir.toPath()))
            val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: "boot.img"
            val bytes = (ctx.contentResolver.openInputStream(uri) ?: error("Cannot open boot image")).use {
                BootPartitionSource.read(it, name, ctx.cacheDir)
            }
            val entries = BootImage.ramdisk(bytes) ?: error("No supported boot ramdisk found")
            require(entries.any { it.name == "init.rc" && it.mode and 0xf000 == 0x8000 && it.data.isNotEmpty() }) { "Boot ramdisk has no init.rc" }
            val selected = validate(entries)
            val created = ArrayList<File>()
            val props = paths.propsTemplate.takeIf { it.isFile }?.readBytes()
            try {
                // Files before links; no existing file is overwritten, including compatibility files.
                for (entry in selected.sortedBy { if (it.mode and 0xf000 == 0xa000) 1 else 0 }) {
                    val file = File(paths.root, entry.name)
                    var parent = file.parentFile ?: error("Invalid ramdisk path")
                    val missing = ArrayList<File>()
                    while (parent != paths.root) {
                        require(parent.toPath().normalize().startsWith(paths.root.toPath()))
                        require(!Files.isSymbolicLink(parent.toPath())) { "Ramdisk path traverses an existing link" }
                        if (!parent.exists()) missing += parent else require(parent.isDirectory)
                        parent = parent.parentFile ?: error("Invalid ramdisk parent")
                    }
                    if (Files.exists(file.toPath(), NOFOLLOW_LINKS)) continue
                    for (dir in missing.asReversed()) { check(dir.mkdir()); created += dir }
                    when (entry.mode and 0xf000) {
                        0x4000 -> { check(file.mkdir()); created += file; Os.chmod(file.path, (entry.mode and 0xfff) or 0x1c0) }
                        0x8000 -> { created += file; file.writeBytes(entry.data); Os.chmod(file.path, (entry.mode and 0xfff) or 0x180) }
                        0xa000 -> {
                            val raw = entry.data.toString(Charsets.UTF_8)
                            val target = if (raw.startsWith('/')) File(paths.root, raw.drop(1)).toPath()
                                else file.toPath().parent.resolve(raw)
                            require(target.normalize().startsWith(paths.root.toPath())) { "Ramdisk link escapes VM" }
                            Files.createSymbolicLink(file.toPath(), file.toPath().parent.relativize(target.normalize()))
                            created += file
                        }
                    }
                }
                require(File(paths.root, "init.rc").isFile)
                val analyzed = Analyzer(ctx, paths, entries).analyze(id, old.name, persistBuildProp = false)
                File(paths.dir, "boot.img").also { created += it }.writeBytes(bytes)
                val refreshed = analyzed.copy(name = old.name, settings = old.settings, baseId = old.baseId,
                    createdAt = old.createdAt, lastBootMs = old.lastBootMs, bootCount = old.bootCount,
                    sizeBytes = ImageStore.du(paths.dir))
                val record = android.util.AtomicFile(paths.meta)
                val output = record.startWrite()
                try {
                    output.write(refreshed.toJson().toString(2).toByteArray(Charsets.UTF_8))
                    record.finishWrite(output)
                } catch (t: Throwable) { record.failWrite(output); throw t }
            } catch (t: Throwable) {
                for (file in created.asReversed()) Files.deleteIfExists(file.toPath())
                if (props == null) Files.deleteIfExists(paths.propsTemplate.toPath()) else paths.propsTemplate.writeBytes(props)
                throw t
            }
        }
    }

    internal fun validate(entries: List<BootImage.CpioEntry>): List<BootImage.CpioEntry> {
        val names = HashSet<String>()
        return entries.filter { e ->
            val name = e.name.trimEnd('/')
            if (name.isEmpty() || name == ".") return@filter false
            require(!name.startsWith('/') && !name.contains('\\') && name.split('/').none { it == ".." || it == "." || it.isEmpty() }) { "Unsafe ramdisk path" }
            require(names.add(name)) { "Duplicate ramdisk path" }
            if (e.mode and 0xf000 == 0xa000) {
                val raw = e.data.toString(Charsets.UTF_8)
                require(raw.isNotEmpty() && !raw.contains('\u0000')) { "Invalid ramdisk link" }
                val anchor = java.nio.file.Paths.get("/guest")
                val target = if (raw.startsWith('/')) anchor.resolve(raw.drop(1)) else anchor.resolve(name).parent.resolve(raw)
                require(target.normalize().startsWith(anchor)) { "Ramdisk link escapes VM" }
            }
            val top = name.substringBefore('/')
            top !in setOf("system", "data", "cache", "dev", "proc", "sys") &&
                name !in setOf("init", "ueventd", "sbin/adbd", "sbin/ueventd", "dhd.owners", "dhd.owners.seeded") &&
                e.mode and 0xf000 in setOf(0x4000, 0x8000, 0xa000)
        }
    }
}
