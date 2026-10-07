/* AEmulator Sunset, 2026-10-07. GPL-3.0; see LICENSE. */
package app.aemu.core

/** Compatibility policy, not a different native translator binary. */
enum class UniversalProfile(val id: String) {
    SUNSET("sunset"), EXTENDED("sunset_extended");

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: SUNSET
    }
}
