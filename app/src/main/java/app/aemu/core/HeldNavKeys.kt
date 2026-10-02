/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/** UI-thread key ownership: a cancelled/disposed/backgrounded button cannot stay held. */
internal class HeldNavKeys(private val send: (Int, Boolean) -> Unit) {
    private val held = mutableSetOf<Int>()
    fun down(code: Int) { if (held.add(code)) send(code, true) }
    fun up(code: Int) { if (held.remove(code)) send(code, false) }
    fun releaseAll() { held.toList().forEach(::up) }
}
