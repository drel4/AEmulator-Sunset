/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

internal object LowPowerBoot {
    /** Do not disable hardware acceleration/JIT or reduce memory: that can cost
     * more CPU or break ROMs. Only cap refresh/input; never raise existing caps. */
    fun apply(saved: VmSettings) = saved.copy(
        fbHz = saved.fbHz.coerceIn(1, 30),
        touchHz = saved.touchHz.coerceIn(1, 30),
        keepScreenOn = false,
    )
}
