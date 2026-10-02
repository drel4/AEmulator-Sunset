/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

internal object VmUiPolicy {
    enum class BackAction { CLOSE_LOG, OPEN_MENU, GUEST_BACK }
    fun back(logVisible: Boolean, hideMenu: Boolean) = when {
        logVisible -> BackAction.CLOSE_LOG
        hideMenu -> BackAction.OPEN_MENU
        else -> BackAction.GUEST_BACK
    }
    fun active(state: String) = state in setOf("PREPARING", "BOOTING", "RUNNING")
}
