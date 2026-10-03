/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/** Separate from UI status: even failed/stopping guests may still have writers.
 * A VM retains this lease until process death; reset takes the same lease. */
internal class VmStorageLease(directory: File) : AutoCloseable {
    companion object {
        private var vmLease: VmStorageLease? = null
        /** Used only in :vm. Retained across Activity recreation and released by the kernel. */
        @Synchronized fun forVm(directory: File): VmStorageLease =
            vmLease ?: VmStorageLease(directory).also { vmLease = it }
    }
    private val file = File(directory, "vm-storage.lock")
    private var handle: RandomAccessFile? = null
    private var lock: FileLock? = null
    @Synchronized fun acquire() {
        if (lock?.isValid == true) return
        file.parentFile!!.mkdirs()
        val opened = RandomAccessFile(file, "rw")
        try {
            lock = opened.channel.tryLock() ?: error("Shut down all VMs before modifying or exporting VM files")
            handle = opened
        } catch (t: Throwable) { opened.close(); throw t }
    }
    @Synchronized override fun close() {
        try { lock?.release() } finally { lock = null; handle?.close(); handle = null }
    }
}
