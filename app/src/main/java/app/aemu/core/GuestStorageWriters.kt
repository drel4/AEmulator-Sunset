/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import android.content.Context
import android.os.Process
import android.system.Os
import java.io.File

/** A killed :vm can leave native children alive. Check only our UID;
 * unreadable same-UID process information fails closed. Caller holds storage lease. */
internal object GuestStorageWriters {
    fun requireIdle(ctx: Context) {
        val proc = File("/proc").listFiles() ?: error("Cannot inspect guest processes")
        for (entry in proc) {
            val pid = entry.name.toIntOrNull() ?: continue
            if (pid == Process.myPid()) continue
            val uid = try { Os.stat(entry.path).st_uid } catch (t: Exception) {
                if (!entry.exists()) continue else throw t
            }
            if (uid != Process.myUid()) continue
            val command = try { File(entry, "cmdline").readBytes().toString(Charsets.ISO_8859_1).replace('\u0000', ' ') }
                catch (t: Exception) { if (!entry.exists()) continue else throw t }
            check(!GuestWriterPolicy.matches(command, ctx.packageName, File(ctx.filesDir, "images").path)) {
                "Guest helper still running; shut down the VM first"
            }
        }
    }
}
