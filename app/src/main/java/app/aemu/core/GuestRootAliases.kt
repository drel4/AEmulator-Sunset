/* Sunset addition, 2026-10-07. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Paths

/** Stock init's /etc -> /system/etc, expressed relative to the guest tree.
 * Keep vendor directories/custom links. Never resolve an absolute guest link
 * against the host's /system. Must run before guest services are started.
 */
internal object GuestRootAliases {
    fun ensureEtc(root: File): Boolean {
        if (!File(root, "system/etc").isDirectory) return false
        val alias = File(root, "etc").toPath()
        if (Files.exists(alias, NOFOLLOW_LINKS)) {
            if (!Files.isSymbolicLink(alias) || Files.readSymbolicLink(alias).toString() != "/system/etc") return false
            Files.delete(alias) // only this exact known init symlink; never its target
        }
        Files.createSymbolicLink(alias, Paths.get("system/etc"))
        return true
    }
}
