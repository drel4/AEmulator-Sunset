/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants

/** Versioned streaming archive: no staging copy, host paths, sockets or inode ownership keys. */
internal object VmArchive {
    data class Selection(val system: Boolean = true, val config: Boolean = true, val data: Boolean = false) {
        val valid get() = system || config || data
        fun encode() = "${if (system) 1 else 0}${if (config) 1 else 0}${if (data) 1 else 0}\n"
        companion object {
            fun decode(text: String): Selection {
                require(text.length == 4 && text.last() == '\n' && text.take(3).all { it == '0' || it == '1' })
                return Selection(text[0] == '1', text[1] == '1', text[2] == '1').also { require(it.valid) }
            }
        }
    }
    data class Profile(val json: String, val selection: Selection)
    data class Metadata(val mode: Int, val uid: Long = 0, val gid: Long = 0)
    private val runtime = setOf("dev", "proc", "sys", "cache", "dhd.owners", "dhd.owners.seeded")

    fun export(dir: File, imageJson: String, includeData: Boolean, output: OutputStream,
               metadata: (File) -> Metadata, progress: (String) -> Unit = {}) =
        export(dir, imageJson, Selection(data = includeData), output, metadata, progress)

    fun export(dir: File, imageJson: String, selection: Selection, output: OutputStream,
               metadata: (File) -> Metadata, progress: (String) -> Unit = {}) {
        require(selection.valid) { "Select at least one export component" }
        val root = File(dir, "root")
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        if (selection.system) require(File(root, "system").isDirectory) { "System partition missing" }
        val system = File(root, "system").canonicalFile
        TarArchiveOutputStream(GZIPOutputStream(output.buffered(65536))).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            fun text(name: String, value: String) {
                val bytes = value.toByteArray(Charsets.UTF_8)
                tar.putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
                tar.write(bytes); tar.closeArchiveEntry()
            }
            text("aessvm.version", "2\n")
            text("image.json", imageJson)
            text("aessvm.parts", selection.encode())
            fun add(file: File, name: String) {
                val attrs = Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS)
                if (!attrs.isRegularFile && !attrs.isDirectory && !attrs.isSymbolicLink) return
                var link: String? = null
                if (attrs.isSymbolicLink) {
                    val target = Files.readSymbolicLink(file.toPath())
                    val resolved = file.toPath().parent.resolve(target).normalize()
                    // Host-engine links are rebuilt by TreeFixer, never exported.
                    val boundary = if (name.startsWith("root/system/")) system.toPath() else root.toPath()
                    if (!resolved.startsWith(boundary) || !file.canonicalFile.toPath().startsWith(boundary)) return
                    link = file.toPath().parent.relativize(resolved).toString()
                }
                val stat = metadata(file)
                val entry = if (link != null) TarArchiveEntry(name, TarConstants.LF_SYMLINK).apply { linkName = link }
                    else TarArchiveEntry(if (attrs.isDirectory) "$name/" else name)
                entry.mode = stat.mode and 0xfff
                entry.setUserId(stat.uid); entry.setGroupId(stat.gid)
                if (attrs.isRegularFile) entry.size = attrs.size()
                tar.putArchiveEntry(entry)
                if (attrs.isRegularFile) file.inputStream().use { it.copyTo(tar, 65536) }
                tar.closeArchiveEntry()
                progress(name)
                if (attrs.isDirectory) for (child in (file.listFiles() ?: error("Cannot read $name")).sortedBy { it.name })
                    add(child, "$name/${child.name}")
            }
            // Old containers may share /system: materialize it, not the container's host link.
            if (selection.system) add(system, "root/system")
            for (file in (root.listFiles() ?: error("Cannot read root")).sortedBy { it.name }) {
                if (file.name == "system" || file.name in runtime ||
                    (file.name == "data" && !selection.data) || (file.name != "data" && !selection.system)) continue
                add(file, "root/${file.name}")
            }
            for (name in listOf("props.base", "boot.img")) {
                val file = File(dir, name)
                if (selection.system && file.isFile && !Files.isSymbolicLink(file.toPath())) add(file, name)
            }
        }
    }

    /** Destination must be new. Links are created only after all file bytes are written. */
    fun restore(input: InputStream, dir: File, mode: (File, Int) -> Unit,
                owner: (File, Long, Long) -> Unit, progress: (String) -> Unit = {}, requireSystem: Boolean = true): String {
        require(dir.isDirectory && dir.list()?.isEmpty() == true)
        val seen = HashSet<String>()
        val links = ArrayList<Triple<File, String, Metadata>>()
        var image: String? = null
        var selection: Selection? = null
        TarArchiveInputStream(GZIPInputStream(input.buffered(65536))).use { tar ->
            val first = tar.nextTarEntry ?: error("Empty VM archive")
            val version = readVersion(first, tar)
            while (true) {
                val entry = tar.nextTarEntry ?: break
                val name = entry.name.trimEnd('/')
                require(name.isNotEmpty() && !name.startsWith('/') && !name.contains('\\') &&
                    name.split('/').none { it == "." || it == ".." || it.isEmpty() }) { "Unsafe archive path" }
                require(seen.add(name) && seen.size <= 1_000_000) { "Duplicate or excessive archive entries" }
                require(name in setOf("image.json", "aessvm.parts", "props.base", "boot.img", "root") || name.startsWith("root/"))
                if (!name.startsWith("root")) require(entry.isFile) { "Invalid metadata entry" }
                require(name.split('/').getOrNull(1) !in runtime) { "Runtime files in archive" }
                val file = File(dir, name)
                require(entry.isFile || entry.isDirectory || entry.isSymbolicLink) { "Unsupported archive entry" }
                if (name == "aessvm.parts") {
                    require(version == 2 && entry.isFile && entry.size == 4L)
                    selection = Selection.decode(tar.readBytes().toString(Charsets.UTF_8))
                    if (requireSystem) require(selection!!.system) { "Archive has no system; use Import settings for configuration archives" }
                    continue
                }
                if (version == 2 && name != "image.json") {
                    val parts = selection ?: error("Missing archive component selection")
                    if (name == "root/data" || name.startsWith("root/data/")) require(parts.data)
                    else if (name != "root") require(parts.system)
                }
                if (entry.isSymbolicLink) {
                    require(name.startsWith("root/") && entry.linkName.isNotEmpty() && !entry.linkName.contains('\\'))
                    val target = java.nio.file.Paths.get(entry.linkName)
                    require(!target.isAbsolute && file.toPath().parent.resolve(target).normalize().startsWith(File(dir, "root").toPath())) { "Link escapes guest" }
                    links += Triple(file, entry.linkName, Metadata(entry.mode, entry.longUserId, entry.longGroupId))
                } else {
                    require(file.parentFile.isDirectory || file.parentFile.mkdirs())
                    if (entry.isDirectory) require(file.isDirectory || file.mkdir())
                    else if (name == "image.json") {
                        require(entry.size in 1..1_048_576)
                        image = tar.readBytes().toString(Charsets.UTF_8)
                    } else {
                        require(entry.size >= 0 && entry.size < dir.usableSpace) { "Not enough space to restore VM" }
                        file.outputStream().buffered(65536).use { tar.copyTo(it, 65536) }
                    }
                    if (file.exists()) {
                        mode(file, entry.mode or if (entry.isDirectory) 0x1c0 else 0x180)
                        owner(file, entry.longUserId, entry.longGroupId)
                    }
                }
                progress(name)
            }
            require(version == 1 || selection != null) { "Missing archive component selection" }
        }
        require(image != null && (!requireSystem || File(dir, "root/system").isDirectory)) { "Incomplete VM archive" }
        for ((file, target, stat) in links) {
            var parent = file.parentFile
            while (parent != dir) {
                require(!Files.isSymbolicLink(parent.toPath())) { "Link parent is a symlink" }
                parent = parent.parentFile ?: error("Invalid link parent")
            }
            require(!Files.exists(file.toPath(), NOFOLLOW_LINKS)) { "Link conflicts with archive directory" }
            require(file.parentFile.isDirectory || file.parentFile.mkdirs())
            Files.createSymbolicLink(file.toPath(), java.nio.file.Paths.get(target))
            owner(file, stat.uid, stat.gid)
        }
        for ((file) in links) require(file.canonicalFile.toPath().startsWith(File(dir, "root").toPath())) { "Link chain escapes guest" }
        return image!!
    }

    /** Read bounded leading metadata only; importing settings never extracts partitions. */
    fun profile(input: InputStream): Profile = TarArchiveInputStream(GZIPInputStream(input.buffered(65536))).use { tar ->
        val version = readVersion(tar.nextTarEntry ?: error("Empty archive"), tar)
        val entry = tar.nextTarEntry ?: error("Missing VM profile")
        require(entry.name == "image.json" && entry.isFile && entry.size in 1..1_048_576)
        val json = tar.readBytes().toString(Charsets.UTF_8)
        val parts = if (version == 1) Selection() else {
            val next = tar.nextTarEntry ?: error("Missing component selection")
            require(next.name == "aessvm.parts" && next.isFile && next.size == 4L)
            Selection.decode(tar.readBytes().toString(Charsets.UTF_8))
        }
        Profile(json, parts)
    }
    private fun readVersion(entry: TarArchiveEntry, tar: TarArchiveInputStream): Int {
        require(entry.name == "aessvm.version" && entry.isFile && entry.size == 2L)
        return when (tar.readBytes().toString(Charsets.UTF_8)) {
            "1\n" -> 1
            "2\n" -> 2
            else -> throw IllegalArgumentException("Unsupported VM archive")
        }
    }
}
