/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal interface ProcessDeathWatch {
    val alive: Boolean
    fun register(onDeath: () -> Unit)
    fun unregister()
}

/** Never launch into the old :vm process; cancellation always removes the watcher. */
internal suspend fun awaitVmProcessExit(watch: ProcessDeathWatch) {
    if (!watch.alive) return
    val completed = AtomicBoolean(false)
    try {
        suspendCancellableCoroutine<Unit> { continuation ->
            val complete = { if (completed.compareAndSet(false, true)) continuation.resume(Unit) }
            watch.register { complete(); Unit }
            if (!watch.alive) complete()
        }
    } finally { watch.unregister() }
}
