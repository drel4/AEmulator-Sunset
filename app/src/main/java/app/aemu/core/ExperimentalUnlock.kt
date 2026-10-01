package app.aemu.core

object ExperimentalUnlock {
    const val TAPS = 42
    const val VIDEO = "https://www.youtube.com/watch?v=LVHBcsiZcyI"
    fun next(taps: Int, enabled: Boolean): Int = if (enabled) taps else (taps + 1).coerceIn(0, TAPS)
    fun activates(before: Int, after: Int, enabled: Boolean) = !enabled && before < TAPS && after == TAPS
}
