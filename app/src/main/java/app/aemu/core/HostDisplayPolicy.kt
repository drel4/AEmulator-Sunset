/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

/** Host window policy never transforms the guest framebuffer or its input axes. */
internal object HostDisplayPolicy {
    fun rotateWindow(smallestWidthDp: Int) = smallestWidthDp >= 600
    fun controlsTurns(tablet: Boolean, rotateNavbar: Boolean, windowTurns: Int) =
        if (tablet && !rotateNavbar) Math.floorMod(windowTurns, 4) else 0
    fun controlIconRotation(tablet: Boolean, rotateNavbar: Boolean, physicalTurns: Int, windowTurns: Int) =
        if (tablet) if (rotateNavbar) 0f else -Math.floorMod(windowTurns, 4) * 90f
        else iconRotation(physicalTurns, windowTurns)
    fun iconRotation(physicalTurns: Int, windowTurns: Int): Float =
        -Math.floorMod(physicalTurns - windowTurns, 4) * 90f
    fun iconTurns(angle: Int, previous: Int): Int {
        if (angle !in 0..359) return previous
        val candidate = ((angle + 45) / 90) % 4
        val center = candidate * 90
        val distance = minOf(Math.floorMod(angle - center, 360), Math.floorMod(center - angle, 360))
        return if (distance <= 30) candidate else previous // hysteresis around diagonal positions
    }
}
