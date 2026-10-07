/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.importer

internal object TransferProgress {
    fun percent(progress: Float): Int? = if (progress.isFinite() && progress >= 0f)
        (progress.coerceAtMost(1f) * 100).toInt() else null
    fun canCancel(active: Boolean, currentTask: Long, requestedTask: Long): Boolean = active && currentTask == requestedTask
}
