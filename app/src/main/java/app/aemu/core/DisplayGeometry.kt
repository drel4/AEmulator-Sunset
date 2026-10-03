/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

/** Angles are display quarter-turns; bottom remains the device's natural bottom. */
internal object DisplayGeometry {
    enum class Edge { BOTTOM, RIGHT, TOP, LEFT }
    fun edge(turns: Int) = Edge.entries[Math.floorMod(turns, 4)]
    data class Rect(val width: Int, val height: Int, val left: Int, val top: Int)
    fun fit(width: Int, height: Int, guestWidth: Int, guestHeight: Int, controls: Int, turns: Int, overlay: Boolean): Rect {
        val amount = if (overlay) 0 else controls.coerceAtLeast(0)
        val edge = edge(turns)
        val aw = (width - if (edge == Edge.LEFT || edge == Edge.RIGHT) amount else 0).coerceAtLeast(1)
        val ah = (height - if (edge == Edge.TOP || edge == Edge.BOTTOM) amount else 0).coerceAtLeast(1)
        val scale = minOf(aw.toFloat() / guestWidth, ah.toFloat() / guestHeight)
        val w = (guestWidth * scale).toInt().coerceAtLeast(1)
        val h = (guestHeight * scale).toInt().coerceAtLeast(1)
        return Rect(w, h, (aw - w) / 2 + if (edge == Edge.LEFT) amount else 0,
            (ah - h) / 2 + if (edge == Edge.TOP) amount else 0)
    }
    fun natural(width: Int, height: Int, turns: Int): Pair<Int, Int> = if (turns % 2 == 0) width to height else height to width
}
