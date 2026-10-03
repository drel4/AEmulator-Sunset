/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

/** Stationary gravity in natural-device axes; guest Android decides whether to rotate. */
internal object ManualMotion {
    fun gravity(turns: Int): FloatArray = when (Math.floorMod(turns, 4)) {
        0 -> floatArrayOf(0f, 9.80665f, 0f)
        1 -> floatArrayOf(9.80665f, 0f, 0f)
        2 -> floatArrayOf(0f, -9.80665f, 0f)
        else -> floatArrayOf(-9.80665f, 0f, 0f)
    }
}
