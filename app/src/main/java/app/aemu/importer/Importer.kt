/* Modified for AEmulator Sunset, 2026-10-03: VM archive restore and boot retention.
 * GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.importer

import android.content.Context
import android.net.Uri
import android.system.Os
import app.aemu.core.GuestImage
import app.aemu.core.ImageStore
import app.aemu.core.TreeFixer
import app.aemu.core.VmPaths
import app.aemu.core.RecoveryImage
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.brotli.dec.BrotliInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

/**
 * Импорт прошивки в дерево гостя. Понимает (в том числе вложенные друг в друга):
 *  - ZIP для CWM/TWRP (MIUI, CyanogenMod, большинство прошивок 2.x–4.x): system/… + boot.img + updater-script
 *  - ZIP/TGZ factory-образов Google, прошивки с system.img внутри
 *  - TAR / TAR.MD5 (Samsung Odin, TouchWiz), TWRP-бэкапы (.win), tar.gz/xz/bz2
 *  - system.img: ext2/3/4, в том числе sparse (и system.img_sparsechunk.* Motorola)
 *  - system.new.dat(.br) + system.transfer.list (OTA 5.x–6.x)
 *  - уже готовое дерево rootfs в tar.gz (например, из стендов HTC)
 */
class Importer(
    private val ctx: Context,
    private val onProgress: (String, Float) -> Unit,
    private val log: (String) -> Unit,
) {
    private lateinit var paths: VmPaths
    private lateinit var root: File
    private val tmp = File(ctx.cacheDir, "import").apply { mkdirs() }
    private var ramdisk: List<BootImage.CpioEntry>? = null
    private var recovery: ByteArray? = null
    private val symlinks = ArrayList<Pair<String, String>>() // (цель, путь ссылки в госте)
    private val perms = ArrayList<Triple<String, Int, Boolean>>() // (путь, режим, рекурсивно-файлы)
    private var files = 0
    private var bytes = 0L
    private var gotSystem = false
    @Volatile var cancelled = false

    fun import(uri: Uri, name: String): GuestImage {
        if (name.endsWith(".aessvm", true)) {
            return app.aemu.core.VmArchiveStorage.restore(ctx, uri) { path ->
                if (cancelled) throw IOException("cancelled")
                onProgress("Restoring $path", -1f)
            }.also { onProgress("Done", 1f) }
        }
        val id = ImageStore.newId(ctx)
        paths = VmPaths(ctx, id)
        root = paths.root
        root.mkdirs()
        try {
            onProgress("Opening $name", 0f)
            val pfd = ctx.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("file failed to open")
            pfd.use {
                val ch = FileInputStream(pfd.fileDescriptor).channel
                val seekable = runCatching { ch.position(0); ch.size() > 0 }.getOrDefault(false)
                if (seekable) handle(ChannelSource(ch), ch, name, 0)
                else {
                    val t = spill(FileInputStream(pfd.fileDescriptor), "whole")
                    FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c -> handle(ChannelSource(c), c, name, 0) }
                    t.delete()
                }
            }
            // build.prop is optional: some dumps/ports lack it, Analyzer infers the version from the tree
            if (!File(root, "system/framework").isDirectory && !File(root, "system/build.prop").isFile)
                throw IOException("no Android system partition found in file")
            finishTree()
            recovery?.let { runCatching { RecoveryImage.install(paths, it, log) } }
            onProgress("Analyzing firmware", 0.97f)
            val img = Analyzer(ctx, paths, ramdisk).analyze(id, name)
            TreeFixer(ctx, paths, img, log).sanitize()
            ImageStore.save(ctx, img.copy(sizeBytes = ImageStore.du(paths.dir)))
            onProgress("Done", 1f)
            log("import: $files files, ${bytes shr 20} MB")
            return ImageStore.get(ctx, id)!!
        } catch (t: Throwable) {
            ImageStore.delete(ctx, id)
            throw t
        } finally {
            tmp.listFiles()?.forEach { it.delete() }
        }
    }

    // ------------------------------------------------------------------ разбор контейнеров

    private fun handle(src: RandomSource, ch: FileChannel?, name: String, depth: Int) {
        FirmwareContainers.checkDepth(depth)
        if (cancelled) throw IOException("cancelled")
        val head = ByteBuffer.allocate(1100)
        src.read(0, head); head.flip()
        val h = ByteArray(head.remaining()).also { head.get(it) }
        if (h.size < 2) throw IOException("empty or truncated firmware file")
        when {
            FirmwareContainers.compression(h) != null -> {
                val input = when (FirmwareContainers.compression(h)!!) {
                    FirmwareContainers.Compression.GZIP -> GZIPInputStream(streamOf(src), 1 shl 16)
                    FirmwareContainers.Compression.XZ -> XZInputStream(streamOf(src))
                    FirmwareContainers.Compression.BZIP2 -> BZip2CompressorInputStream(streamOf(src))
                }
                val buffered = input.buffered(65536)
                buffered.mark(512)
                val innerHead = ByteArray(512)
                var read = 0
                while (read < innerHead.size) {
                    val n = buffered.read(innerHead, read, innerHead.size - read)
                    if (n < 0) break
                    read += n
                }
                buffered.reset()
                if (isTar(innerHead.copyOf(read))) {
                    buffered.use { importTarStream(it, name, depth + 1) }
                    return
                }
                val t = buffered.use { spill(it, "uncompressed") }
                try {
                    FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c ->
                        // The inner payload is identified by content, not the outer .tar suffix.
                        handle(ChannelSource(c), c, name.substringBeforeLast('.', name), depth + 1)
                    }
                } finally { t.delete() }
            }
            h.size > 4 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte() && h[2].toInt() == 3 && h[3].toInt() == 4 -> {
                if (ch != null) importZip(ch, name, depth) else throw IOException("zip without random access")
            }
            h.size > 6 && h[0] == '7'.code.toByte() && h[1] == 'z'.code.toByte() && h[2] == 0xBC.toByte() && h[3] == 0xAF.toByte() -> {
                if (ch != null) importSevenZ(ch, name, depth) else throw IOException("7z without random access")
            }
            SparseSource.probe(src) -> importImage(SparseSource(listOf(src)), "system")
            Ext4Reader.probe(src) -> importImage(src, "system")
            Yaffs2Reader.probe(src) -> importImage(src, "system")
            isTar(h) || name.endsWith(".tar", true) || name.endsWith(".md5", true) || name.endsWith(".win", true) ->
                importTarStream(streamOf(src), name, depth)
            h.size >= 8 && String(h, 0, 8, Charsets.ISO_8859_1) == "ANDROID!" -> takeBoot(readAllFrom(src))
            else -> throw IOException("unknown file format \"$name\"")
        }
    }

    private fun isTar(h: ByteArray) = h.size > 262 && String(h, 257, 5, Charsets.ISO_8859_1) == "ustar"

    private fun streamOf(src: RandomSource): InputStream = object : InputStream() {
        var pos = 0L
        override fun read(): Int { val b = ByteArray(1); return if (read(b, 0, 1) <= 0) -1 else b[0].toInt() and 0xff }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (pos >= src.size) return -1
            val k = minOf(len.toLong(), src.size - pos).toInt()
            src.read(pos, ByteBuffer.wrap(b, off, k))
            pos += k
            return k
        }
    }.let { BufferedInputStream(it, 1 shl 20) }

    private fun readAllFrom(src: RandomSource): ByteArray {
        if (src.size > 64L * 1024 * 1024) throw IOException("Boot image exceeds 64 MiB")
        val b = ByteBuffer.allocate(src.size.toInt()); src.read(0, b); return b.array()
    }

    private var zipChannel: FileChannel? = null
    /** partition images spilled out of a nested zip, imported only after that zip's temp file is gone (disk peak) */
    private var deferred: MutableList<Pair<File, String>>? = null

    private fun importZip(ch: FileChannel, name: String, depth: Int) {
        val zip = ZipFile.builder().setSeekableByteChannel(ch).get()
        val prevCh = zipChannel
        zipChannel = ch
        try { importZipEntries(zip, name, depth) } finally { zipChannel = prevCh }
    }

    private fun importZipEntries(zip: ZipFile, name: String, depth: Int) {
        val entries = zip.entries.toList()
        val total = entries.sumOf { maxOf(0L, it.size) }.coerceAtLeast(1)
        var done = 0L
        // сначала — сценарий установки (ссылки и права)
        entries.firstOrNull { it.name.endsWith("META-INF/com/google/android/updater-script") }?.let { e ->
            zip.getInputStream(e).use { parseUpdaterScript(String(it.readBytes())) }
        }
        // прошивка, упакованная вместе с папкой (Имя/META-INF/…, Имя/system/…): папку-обёртку снимаем
        val wrap = entries.firstOrNull { it.name.endsWith("META-INF/com/google/android/updater-script") }
            ?.name?.substringBefore("META-INF/")?.takeIf { it.isNotEmpty() && it.count { c -> c == '/' } == 1 } ?: ""
        if (wrap.isNotEmpty()) log("archive with wrapper folder \"${wrap.trimEnd('/')}\"")
        val names = entries.map { it.name.removePrefix(wrap) }.toSet()
        val datBr = entries.firstOrNull { it.name.matches(Regex("(.*/)?system\\.new\\.dat(\\.br)?")) }
        val sparseChunks = entries.filter { it.name.matches(Regex("(.*/)?system\\.img_sparsechunk\\.\\d+")) }.sortedBy { it.name.substringAfterLast('.').toInt() }
        for (e in entries) {
            if (cancelled) throw IOException("cancelled")
            val n = e.name.replace('\\', '/').removePrefix(wrap)
            val base = n.substringAfterLast('/')
            when {
                e.isDirectory -> {}
                base.equals("system.yaffs2.img", true) -> withEntrySource(zip, e) { importImage(it, "system") }
                base.endsWith(".yaffs2.img", true) -> {} // CWM user-data/cache backups are not firmware.
                n.contains("__MACOSX/") || base.startsWith("._") -> {}
                n.startsWith("system/") -> {
                    zip.getInputStream(e).use { writeFile(n, it, e.unixMode.takeIf { m -> m != 0 }) }
                    gotSystem = true
                }
                base.equals("boot.img", true) -> zip.getInputStream(e).use { takeBoot(readBoot(it)) }
                base.equals("recovery.img", true) && e.size < 64_000_000 -> zip.getInputStream(e).use { recovery = it.readBytes() }
                base.matches(Regex("(?i)system(\\.ext4)?\\.img(\\.ext4)?|system_image\\.img|system\\.raw\\.img|factoryfs\\.img")) ->
                    withEntrySource(zip, e) { importImage(if (SparseSource.probe(it)) SparseSource(listOf(it)) else it, "system") }
                base.matches(Regex("(?i)vendor(\\.ext4)?\\.img")) ->
                    withEntrySource(zip, e) { runCatching { importImage(if (SparseSource.probe(it)) SparseSource(listOf(it)) else it, "vendor") } }
                base.lowercase().endsWith(".zip") && FirmwareContainers.nested(base) -> {
                    handleNestedZip(spillEntry(zip, e), base, depth)
                }
                FirmwareContainers.nested(base)
                    // Odin: AP/PDA/CODE — система, BL/KERNEL/HOME — ядро с рамдиском; модем и CSC не нужны
                    && !base.startsWith("MODEM") && !base.startsWith("CP_") && !base.contains("CSC") -> {
                    val t = spillEntry(zip, e)
                    try { FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c -> handle(ChannelSource(c), c, base, depth + 1) } }
                    finally { t.delete() }
                }
            }
            done += maxOf(0L, e.size)
            onProgress("Extracting: $base", 0.9f * done / total)
        }
        if (datBr != null && !gotSystem) {
            val listName = datBr.name.substringBeforeLast("system.new.dat") + "system.transfer.list"
            val listE = entries.firstOrNull { it.name == listName } ?: throw IOException("system.transfer.list missing")
            val list = zip.getInputStream(listE).use { String(it.readBytes()) }
            onProgress("Building system.img from OTA", 0.5f)
            val raw = File(tmp, "system.raw")
            zip.getInputStream(datBr).use { s ->
                val data = if (datBr.name.endsWith(".br")) BrotliInputStream(BufferedInputStream(s, 1 shl 20)) else s
                TransferList.build(list, data, raw)
            }
            FileChannel.open(raw.toPath(), StandardOpenOption.READ).use { c -> importImage(ChannelSource(c), "system") }
            raw.delete()
        }
        if (sparseChunks.isNotEmpty() && !gotSystem) {
            val parts = sparseChunks.map { spillEntry(zip, it) }
            val chans = parts.map { FileChannel.open(it.toPath(), StandardOpenOption.READ) }
            try { importImage(SparseSource(chans.map { ChannelSource(it) }), "system") } finally { chans.forEach { it.close() }; parts.forEach { it.delete() } }
        }
        if (!names.any { it.startsWith("system/") } && !gotSystem) log("no system found in archive $name")
    }

    /** 7z: entries are read in order (solid archives cannot seek); interesting ones are unpacked like their zip twins */
    private fun importSevenZ(ch: FileChannel, name: String, depth: Int) {
        SevenZFile.builder().setSeekableByteChannel(ch).get().use { z ->
            val nested = ArrayList<Pair<File, String>>()
            while (true) {
                if (cancelled) throw IOException("cancelled")
                val e = z.nextEntry ?: break
                if (e.isDirectory) continue
                val n = e.name.replace('\\', '/')
                val base = n.substringAfterLast('/')
                val stream = z.getInputStream(e)
                when {
                    base.equals("system.yaffs2.img", true) -> nested.add(spill(stream, base) to "system")
                    base.endsWith(".yaffs2.img", true) -> {}
                    n.contains("__MACOSX/") || base.startsWith("._") -> {}
                    n.startsWith("system/") -> { writeFile(n, stream, null); gotSystem = true }
                    base.equals("boot.img", true) -> takeBoot(readBoot(stream))
                    base.equals("recovery.img", true) && e.size < 64_000_000 -> recovery = stream.readBytes()
                    base.matches(Regex("(?i)system(\\.ext4)?\\.img(\\.ext4)?|system_image\\.img|system\\.raw\\.img|factoryfs\\.img")) ->
                        nested.add(spill(stream, base) to "system")
                    FirmwareContainers.nested(base) &&
                        !base.startsWith("MODEM") && !base.startsWith("CP_") && !base.contains("CSC") ->
                        nested.add(spill(stream, base) to "archive")
                }
                onProgress("Extracting: $base", 0.5f)
            }
            for ((f, kind) in nested) {
                try {
                    FileChannel.open(f.toPath(), StandardOpenOption.READ).use { c ->
                        val src = ChannelSource(c)
                        if (kind == "system") importImage(if (SparseSource.probe(src)) SparseSource(listOf(src)) else src, "system")
                        else handle(src, c, f.name, depth + 1)
                    }
                } finally { f.delete() }
            }
            if (!gotSystem && nested.isEmpty()) log("no system found in archive $name")
        }
    }

    private fun withEntrySource(zip: ZipFile, e: ZipArchiveEntry, block: (RandomSource) -> Unit) {
        // несжатую запись читаем прямо из архива по смещению, сжатую — через временный файл
        val ch = zipChannel
        if (e.method == ZipArchiveEntry.STORED && ch != null) {
            runCatching { zip.getRawInputStream(e).close() } // вычисляет dataOffset
            if (e.dataOffset > 0) { block(ChannelSource(ch, e.dataOffset, e.size)); return }
        }
        val t = spillEntry(zip, e)
        deferred?.let { d -> d.add(t to e.name); return }
        FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c -> block(ChannelSource(c)) }
        t.delete()
    }

    /** a nested zip: its images are copied out first, the zip deleted, then the images unpacked */
    private fun handleNestedZip(t: File, base: String, depth: Int) {
        val outer = deferred
        val mine = ArrayList<Pair<File, String>>()
        deferred = mine
        try {
            FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c -> handle(ChannelSource(c), c, base, depth + 1) }
        } finally { deferred = outer; t.delete() }
        try {
            for ((f, entry) in mine) {
                val part = if (entry.substringAfterLast('/').lowercase().startsWith("vendor")) "vendor" else "system"
                FileChannel.open(f.toPath(), StandardOpenOption.READ).use { c ->
                    val src = ChannelSource(c)
                    val img = if (SparseSource.probe(src)) SparseSource(listOf(src)) else src
                    if (part == "vendor") runCatching { importImage(img, part) } else importImage(img, part)
                }
                f.delete()
            }
        } finally { mine.forEach { it.first.delete() } }
    }

    private fun spillEntry(zip: ZipFile, e: ZipArchiveEntry): File = zip.getInputStream(e).use { spill(it, e.name.substringAfterLast('/')) }

    private fun spill(i: InputStream, name: String): File {
        val suffix = name.replace('\\', '/').substringAfterLast('/').replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(120)
        val t = File.createTempFile("payload-", "-$suffix", tmp)
        try {
            t.outputStream().use { o ->
                val buffer = ByteArray(1 shl 20)
                var count = 0L
                while (true) {
                    if (cancelled) throw IOException("cancelled")
                    val n = i.read(buffer)
                    if (n < 0) break
                    count += n
                    if (count > 16L * 1024 * 1024 * 1024 || tmp.usableSpace < n + 32L * 1024 * 1024)
                        throw IOException("Not enough space to unpack firmware")
                    o.write(buffer, 0, n)
                }
            }
            return t
        } catch (toss: Throwable) { t.delete(); throw toss }
    }

    private fun importTarStream(s: InputStream, name: String, depth: Int) {
        FirmwareContainers.checkDepth(depth)
        val tar = TarArchiveInputStream(s, "UTF-8")
        var fullRoot: Boolean? = null
        while (true) {
            if (cancelled) throw IOException("cancelled")
            val e: TarArchiveEntry = tar.nextEntry ?: break
            var n = e.name.removePrefix("./").trimStart('/')
            if (n.isEmpty()) continue
            val base = n.substringAfterLast('/')
            // дерево rootfs целиком (system/, data/, dev/, …)
            if (fullRoot == null && (n == "system" || n.startsWith("system/") || n.startsWith("init.rc") || n.startsWith("default.prop"))) fullRoot = true
            when {
                base.equals("system.yaffs2.img", true) && e.isFile -> {
                    val t = spill(tar.nonClosing(), base)
                    try { FileChannel.open(t.toPath(), StandardOpenOption.READ).use { importImage(ChannelSource(it), "system") } }
                    finally { t.delete() }
                }
                base.endsWith(".yaffs2.img", true) -> {}
                base.matches(Regex("(?i)system(\\.ext4)?\\.img(\\.ext4)?(\\.lz4)?|factoryfs\\.img|system\\.img\\.ext4")) && e.isFile -> {
                    val t = spill(maybeLz4(tar, base), base)
                    FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c ->
                        val src = ChannelSource(c)
                        importImage(if (SparseSource.probe(src)) SparseSource(listOf(src)) else src, "system")
                    }
                    t.delete()
                }
                base.matches(Regex("(?i)(boot\\.img|zImage|kernel)(\\.lz4)?")) && e.isFile -> takeBoot(readBoot(maybeLz4(tar, base)))
                base.matches(Regex("(?i)recovery\\.img(\\.lz4)?")) && e.isFile && e.size < 64_000_000 -> recovery = maybeLz4(tar, base).readBytes()
                // factory-образы Google: tgz → image-*.zip → system.img/boot.img; Samsung: zip внутри tar
                base.lowercase().endsWith(".zip") && e.isFile && FirmwareContainers.nested(base) -> {
                    handleNestedZip(spill(tar.nonClosing(), base), base, depth)
                }
                FirmwareContainers.nested(base) && e.isFile -> {
                    val t = spill(tar.nonClosing(), base)
                    try { FileChannel.open(t.toPath(), StandardOpenOption.READ).use { c -> handle(ChannelSource(c), c, base, depth + 1) } }
                    finally { t.delete() }
                }
                n.matches(Regex("^(system|data|dev|sbin|vendor|etc)(/.*)?$")) || n.matches(Regex("^[^/]+\\.rc$")) || n == "default.prop" || n.startsWith("dhd.") -> {
                    // файлы корня (рамдиск, dhd.*) берём, только если архив — целое дерево rootfs
                    if (fullRoot != true && !n.startsWith("system") && !(n.startsWith("dhd.") || n.endsWith(".rc") || n == "default.prop" || n.startsWith("sbin"))) continue
                    when {
                        e.isDirectory -> FirmwareContainers.destination(root, n).mkdirs()
                        e.isSymbolicLink -> symlinks.add(e.linkName to "/$n")
                        e.isLink -> hardlink(n, e.linkName)
                        e.isFile -> {
                            writeFile(n, tar.nonClosing(), e.mode)
                            if (n.startsWith("system/")) gotSystem = true
                        }
                    }
                }
                // TWRP system.ext4.win: пути без префикса system/
                name.contains("system", true) && name.endsWith(".win", true) -> {
                    val p = "system/$n"
                    when {
                        e.isDirectory -> FirmwareContainers.destination(root, p).mkdirs()
                        e.isSymbolicLink -> symlinks.add(e.linkName to "/$p")
                        e.isFile -> { writeFile(p, tar.nonClosing(), e.mode); gotSystem = true }
                    }
                }
            }
            if (files % 200 == 0) onProgress("Extracting: $base", -1f)
        }
    }

    private fun InputStream.nonClosing(): InputStream = object : java.io.FilterInputStream(this) { override fun close() {} }

    private fun maybeLz4(s: InputStream, name: String): InputStream =
        if (name.endsWith(".lz4", true)) org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream(s.nonClosing()) else s.nonClosing()

    private fun hardlink(n: String, target: String) {
        val src = FirmwareContainers.destination(root, target.removePrefix("./").trimStart('/'))
        val dst = FirmwareContainers.destination(root, n)
        if (src.isFile) { dst.parentFile?.mkdirs(); src.copyTo(dst, overwrite = true) }
    }

    // ------------------------------------------------------------------ образы ФС

    private fun importImage(src: RandomSource, mount: String) {
        if (Yaffs2Reader.probe(src)) { importYaffs2(src, mount); return }
        val fs = Ext4Reader(src)
        onProgress("Reading image $mount (ext4, block ${fs.blockSize})", -1f)
        var n = 0
        fs.walk { path, node ->
            if (cancelled) throw IOException("cancelled")
            val rel = "$mount/$path"
            val f = FirmwareContainers.destination(root, rel)
            when {
                node.isDir -> f.mkdirs()
                node.isLink -> symlinks.add(fs.linkTarget(node) to "/$rel")
                node.isFile -> {
                    f.parentFile?.mkdirs()
                    f.outputStream().buffered(1 shl 20).use { fs.copy(node, it) }
                    applyMode(f, node.perm)
                    files++; bytes += node.size
                }
            }
            if (++n % 150 == 0) onProgress("$mount: ${path.substringAfterLast('/')}", -1f)
        }
        if (mount == "system") gotSystem = true
        log("image $mount: $n objects")
    }

    private fun importYaffs2(src: RandomSource, mount: String) {
        onProgress("Reading image $mount (YAFFS2)", -1f)
        val fs = Yaffs2Reader(src) { if (cancelled) throw IOException("cancelled") }
        var n = 0
        fs.walk { path, node ->
            val rel = "$mount/$path"
            val f = FirmwareContainers.destination(root, rel)
            if (!f.canonicalPath.startsWith(root.canonicalPath + File.separator)) throw IOException("unsafe YAFFS2 path")
            when (node.type) {
                3 -> { if (!f.isDirectory && !f.mkdirs()) throw IOException("cannot create $rel") }
                2 -> {
                    val target = if (node.alias.startsWith("/")) node.alias.trimStart('/')
                        else rel.substringBeforeLast('/') + "/" + node.alias
                    val normalized = java.nio.file.Paths.get(target).normalize().toString()
                    if (node.alias.isEmpty() || normalized == ".." || normalized.startsWith("../"))
                        throw IOException("YAFFS2 symlink escapes guest root")
                    symlinks.add(node.alias to "/$rel")
                }
                1, 4 -> {
                    f.parentFile?.mkdirs()
                    // Links are only created by finishTree, after all file bytes.
                    if (isLink(f)) throw IOException("YAFFS2 destination is a symlink")
                    f.outputStream().buffered(1 shl 20).use { fs.copy(node, it) }
                    applyMode(f, fs.fileMode(node) and 0xfff)
                    files++; bytes += f.length()
                }
                5 -> {} // Guest /dev is created by the VM; never mknod on the host.
            }
            if (++n % 150 == 0) onProgress("$mount: ${path.substringAfterLast('/')}", -1f)
        }
        if (mount == "system") gotSystem = true
        log("YAFFS2 $mount: $n objects, page ${fs.geometry.pageSize}+${fs.geometry.spareSize}")
    }

    private fun takeBoot(data: ByteArray) {
        if (ramdisk != null) return
        val rd = runCatching { BootImage.ramdisk(data) }.getOrNull()
        if (rd.isNullOrEmpty()) { log("boot: ramdisk not recognized"); return }
        ramdisk = rd
        File(paths.dir, "boot.img").writeBytes(data)
        log("boot: ramdisk, ${rd.size} files")
    }

    private fun readBoot(input: InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(65536)
        while (true) {
            if (cancelled) throw IOException("cancelled")
            val n = input.read(buffer)
            if (n < 0) break
            if (output.size().toLong() + n > 64L * 1024 * 1024) throw IOException("Boot image exceeds 64 MiB")
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }

    // ------------------------------------------------------------------ файлы, ссылки, права

    private fun writeFile(rel: String, i: InputStream, mode: Int?) {
        val f = FirmwareContainers.destination(root, rel)
        f.parentFile?.mkdirs()
        if (runCatching { android.system.OsConstants.S_ISLNK(Os.lstat(f.path).st_mode) }.getOrDefault(false)) f.delete()
        f.outputStream().buffered(1 shl 20).use { o ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                if (cancelled) throw IOException("cancelled")
                val n = i.read(buffer)
                if (n < 0) break
                bytes += n
                if (bytes > 16L * 1024 * 1024 * 1024 || root.usableSpace < n + 32L * 1024 * 1024)
                    throw IOException("Not enough space to unpack firmware")
                o.write(buffer, 0, n)
            }
        }
        if (mode != null) applyMode(f, mode and 0xfff)
        files++
    }

    private fun applyMode(f: File, perm: Int) {
        if (perm and 0x49 != 0) f.setExecutable(true, false)
        f.setReadable(true, false)
    }

    private fun parseUpdaterScript(s: String) {
        val text = s.replace(Regex("#[^\n]*"), "")
        for (m in Regex("symlink\\s*\\(([^;]*?)\\)\\s*;", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            val args = Regex("\"([^\"]*)\"").findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()
            if (args.size >= 2) for (link in args.drop(1)) symlinks.add(args[0] to link)
        }
        for (m in Regex("set_perm_recursive\\s*\\(([^;]*?)\\)\\s*;", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            val a = m.groupValues[1].split(',').map { it.trim().trim('"') }
            if (a.size >= 5) a[3].toIntOrNull(8)?.let { perms.add(Triple(a[4], it, true)) }
        }
        for (m in Regex("set_perm\\s*\\(([^;]*?)\\)\\s*;", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            val a = m.groupValues[1].split(',').map { it.trim().trim('"') }
            if (a.size >= 4) a[2].toIntOrNull(8)?.let { perms.add(Triple(a[3], it, false)) }
        }
        for (m in Regex("set_metadata(_recursive)?\\s*\\(([^;]*?)\\)\\s*;", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            val a = m.groupValues[2].split(',').map { it.trim().trim('"') }
            if (a.isEmpty()) continue
            val key = if (m.groupValues[1].isNotEmpty()) "fmode" else "mode"
            val i = a.indexOf(key)
            if (i > 0 && i + 1 < a.size) a[i + 1].toIntOrNull(8)?.let { perms.add(Triple(a[0], it, m.groupValues[1].isNotEmpty())) }
        }
        log("updater-script: ${symlinks.size} symlinks, ${perms.size} perms")
    }

    /** Ссылки (с переводом абсолютных целей в относительные), права, служебные ссылки корня. */
    private fun finishTree() {
        // рамдиск: init*.rc, default.prop, sbin/, file_contexts…
        ramdisk?.forEach { e ->
            val type = e.mode and 0xF000
            val n = e.name
            if (n.isEmpty() || n == "." || n.startsWith("system/") || n.startsWith("data/") || n.startsWith("dev/") || n.startsWith("proc") || n.startsWith("sys/")) return@forEach
            val f = FirmwareContainers.destination(root, n)
            when (type) {
                0x4000 -> f.mkdirs()
                0xA000 -> symlinks.add(String(e.data) to "/$n")
                0x8000 -> {
                    if (n == "init" || n.startsWith("sbin/ueventd") || n.startsWith("sbin/adbd")) return@forEach
                    f.parentFile?.mkdirs(); f.writeBytes(e.data)
                    applyMode(f, e.mode and 0xfff)
                }
            }
        }
        var made = 0
        for ((target, link) in symlinks) {
            val rel = link.trimStart('/')
            val f = FirmwareContainers.destination(root, rel)
            FirmwareContainers.checkLink(rel, target)
            f.parentFile?.mkdirs()
            val t = relTarget(link, target)
            runCatching {
                if (f.exists() || isLink(f)) { if (f.isDirectory && !isLink(f)) return@runCatching; f.delete() }
                Os.symlink(t, f.absolutePath); made++
            }
        }
        for ((p, mode, rec) in perms) {
            val f = FirmwareContainers.destination(root, p.trimStart('/'))
            if (rec) f.walkTopDown().filter { it.isFile }.forEach { applyMode(it, mode) } else if (f.isFile) applyMode(f, mode)
        }
        // исполняемые — всё в bin/xbin/sbin
        for (d in listOf("system/bin", "system/xbin", "sbin", "vendor/bin", "system/vendor/bin")) {
            File(root, d).listFiles()?.forEach { if (it.isFile) it.setExecutable(true, false) }
        }
        standardLinks()
        fun rootLink(name: String, target: String) {
            val f = File(root, name)
            if (!f.exists() && !isLink(f) && File(root, target).exists()) runCatching { Os.symlink(target, f.absolutePath) }
        }
        rootLink("vendor", "system/vendor")
        rootLink("etc", "system/etc")
        rootLink("bin", "system/bin")
        runCatching { File(root, "system/etc/mtab").let { if (!it.exists() && !isLink(it)) Os.symlink("../../proc/mounts", it.absolutePath) } }
        log("symlinks created: $made")
    }

    private fun isLink(f: File) = runCatching { android.system.OsConstants.S_ISLNK(Os.lstat(f.absolutePath).st_mode) }.getOrDefault(false)

    /** Абсолютную цель ссылки гостя делаем относительной — иначе ядро телефона уйдёт за пределы дерева. */
    /**
     * Архивы без updater-script и без ссылок в самом архиве (multirom, выгрузки system/ из TWRP) не
     * содержат sh → mksh и ссылок на апплеты toolbox — без них не стартует ни одна служба init.
     * Создаём их сами, если их нет: апплет берём, только если его имя есть в самом toolbox.
     */
    private fun standardLinks() {
        val bin = File(root, "system/bin")
        fun missing(n: String) = File(bin, n).let { !it.exists() && !isLink(it) }
        var made = 0
        if (missing("sh")) {
            val sh = listOf("mksh", "ash").firstOrNull { File(bin, it).isFile }
            if (sh != null && runCatching { Os.symlink(sh, File(bin, "sh").absolutePath) }.isSuccess) made++
        }
        val toolbox = File(bin, "toolbox")
        if (toolbox.isFile) {
            val text = runCatching { String(toolbox.readBytes(), Charsets.ISO_8859_1) }.getOrDefault("")
            for (a in TOOLBOX_APPLETS) {
                if (!missing(a)) continue
                if (!text.contains("\u0000$a\u0000")) continue
                if (runCatching { Os.symlink("toolbox", File(bin, a).absolutePath) }.isSuccess) made++
            }
        }
        if (made > 0) log("sh/toolbox symlinks created: $made (missing from archive)")
    }

    private fun relTarget(link: String, target: String): String {
        if (!target.startsWith("/")) return target
        val from = link.trimStart('/').split('/').dropLast(1)
        val to = target.trimStart('/').split('/').filter { it.isNotEmpty() }
        var i = 0
        while (i < from.size && i < to.size && from[i] == to[i]) i++
        val ups = List(from.size - i) { ".." }
        return (ups + to.drop(i)).joinToString("/").ifEmpty { "." }
    }

    companion object {
        /** Апплеты toolbox Android 2.3–6.0 (ссылка создаётся, только если апплет есть в бинарнике). */
        private val TOOLBOX_APPLETS = listOf(
            "cat", "chcon", "chmod", "chown", "clear", "cmp", "cp", "date", "dd", "df", "dmesg", "du", "getenforce",
            "getevent", "getprop", "getsebool", "grep", "hd", "id", "ifconfig", "iftop", "insmod", "ioctl", "ionice",
            "kill", "ln", "load_policy", "log", "ls", "lsmod", "lsof", "md5", "mkdir", "mkswap", "mount", "mv",
            "nandread", "netstat", "newfs_msdos", "nohup", "notify", "printenv", "ps", "readlink", "renice",
            "restorecon", "rm", "rmdir", "rmmod", "route", "runcon", "schedtop", "sendevent", "setenforce",
            "setprop", "setsebool", "sleep", "smd", "start", "stop", "swapoff", "swapon", "sync", "top", "touch",
            "umount", "uptime", "vmstat", "watchprops", "wipe", "chroot", "sh",
        ).filter { it != "sh" }
    }
}
