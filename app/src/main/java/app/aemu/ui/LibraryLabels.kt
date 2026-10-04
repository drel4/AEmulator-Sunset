/* AEmulator Sunset addition, 2026-10-04. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

/** Compact card label only; persisted ROM identity stays unchanged. */
internal object LibraryLabels {
    fun compactSkin(skin: String): String {
        val match = Regex("^CyanogenMod(?:\\s+([0-9]+(?:\\.[0-9]+)*))?$").matchEntire(skin.trim())
        return if (match != null) "CM${match.groupValues[1]}" else skin
    }
}
