/* Modified for AEmulator Sunset, 2026-10-02: permanent reset without backups.
 * GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Caller holds storage lease and checks for orphan writers. Never follows links.
 * Failure may leave partially erased data; no backup or rollback is created. */
internal object VmDataReset {
    fun reset(images: File, id: String, inodeKey: (File) -> String) {
        require(id.matches(Regex("[a-z0-9]{6}")))
        val dir = File(images, id)
        val root = File(dir, "root")
        for (f in listOf(images, dir, root))
            require(!Files.isSymbolicLink(f.toPath()) && f.isDirectory) { "Unsafe image path" }
        val targets = listOf(File(root, "data"), File(root, "cache"),
            File(root, "dhd.owners.seeded"), File(dir, "run"))
        val removedKeys = HashSet<String>()
        for (target in targets) if (Files.exists(target.toPath(), NOFOLLOW_LINKS)) {
            Files.walkFileTree(target.toPath(), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(p: Path, a: BasicFileAttributes) = CONTINUE.also { removedKeys += inodeKey(p.toFile()) }
                override fun visitFile(p: Path, a: BasicFileAttributes) = CONTINUE.also { removedKeys += inodeKey(p.toFile()) }
            })
        }
        // Prune before unlinking: recycled data inodes must not inherit stale UIDs.
        val owners = File(root, "dhd.owners")
        require(!Files.isSymbolicLink(owners.toPath())) { "Unsafe ownership table" }
        if (owners.isFile) {
            val temp = Files.createTempFile(root.toPath(), "owners-reset-", ".tmp")
            try {
                Files.newBufferedWriter(temp).use { out -> owners.forEachLine { line ->
                    val key = line.trim().split(Regex("\\s+")).take(2).joinToString(" ")
                    if (key !in removedKeys) { out.write(line); out.newLine() }
                } }
                Files.move(temp, owners.toPath(), REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temp) }
        }
        for (target in targets) if (Files.exists(target.toPath(), NOFOLLOW_LINKS)) {
            Files.walkFileTree(target.toPath(), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(p: Path, a: BasicFileAttributes) = CONTINUE.also { p.toFile().setWritable(true, false) }
                override fun visitFile(p: Path, a: BasicFileAttributes) = CONTINUE.also {
                    if (!a.isSymbolicLink) p.toFile().setWritable(true, false)
                    Files.delete(p)
                }
                override fun postVisitDirectory(p: Path, failure: java.io.IOException?) = CONTINUE.also {
                    if (failure != null) throw failure
                    Files.delete(p)
                }
            })
        }
    }
}
