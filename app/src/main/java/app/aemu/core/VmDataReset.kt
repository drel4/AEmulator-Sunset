/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.UUID

/** Same-filesystem moves, never a recursive deletion. Old data remains recoverable.
 * Caller must hold VmStorageLease. Imported symlinked roots are rejected. */
internal object VmDataReset {
    fun reset(images: File, id: String, move: (File, File) -> Unit = { from, to ->
        Files.move(from.toPath(), to.toPath()); Unit
    }): File {
        require(id.matches(Regex("[a-z0-9]{6}")))
        val dir = File(images, id)
        val root = File(dir, "root")
        for (f in listOf(images, dir, root)) {
            require(!Files.isSymbolicLink(f.toPath()) && f.isDirectory) { "Unsafe image path" }
        }
        val backup = File(dir, "data-reset-${UUID.randomUUID()}")
        check(backup.mkdir()) { "Cannot create reset backup" }
        val moved = mutableListOf<Pair<File, File>>()
        try {
            // Ownership entries are inode-based; preserve system entries but reseed new data.
            val targets = listOf("data", "cache", "dhd.owners.seeded")
            for (name in targets) {
                val source = File(root, name)
                if (Files.exists(source.toPath(), NOFOLLOW_LINKS)) {
                    val dest = File(backup, name)
                    move(source, dest)
                    moved += source to dest
                }
            }
            // Runtime setup markers/sockets must not leak into the fresh data tree.
            val run = File(dir, "run")
            if (Files.exists(run.toPath(), NOFOLLOW_LINKS)) {
                val dest = File(backup, "run")
                move(run, dest)
                moved += run to dest
            }
            return backup
        } catch (t: Throwable) {
            moved.asReversed().forEach { (source, dest) ->
                try { Files.move(dest.toPath(), source.toPath()) } catch (rollback: Throwable) { t.addSuppressed(rollback) }
            }
            backup.delete() // Only succeeds if empty; never deletes retained data.
            throw t
        }
    }
}
