/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

/** Only icon artwork follows physical orientation; never transform guest display or input. */
internal object HostDisplayPolicy {
    fun iconTurns(angle: Int, previous: Int): Int {
        if (angle !in 0..359) return previous
        val candidate = ((angle + 45) / 90) % 4
        val center = candidate * 90
        val distance = minOf(Math.floorMod(angle - center, 360), Math.floorMod(center - angle, 360))
        return if (distance <= 30) candidate else previous // hysteresis around diagonal positions
    }
}
