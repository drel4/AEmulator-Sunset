/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

internal object GoogleAppsOption {
    fun runHelper(api: Int, disabled: Boolean, hasState: Boolean, recovery: Boolean, charging: Boolean) =
        api in 9..25 && !recovery && !charging && (disabled || hasState)
}
