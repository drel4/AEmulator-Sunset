package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class DisplayGeometryTest {
    @Test fun keepsControlsAtPhysicalBottomForAllTurns() {
        assertEquals(listOf(DisplayGeometry.Edge.BOTTOM, DisplayGeometry.Edge.RIGHT, DisplayGeometry.Edge.TOP, DisplayGeometry.Edge.LEFT),
            (0..3).map(DisplayGeometry::edge))
    }
    @Test fun reservesOnlyTheCurrentControlEdge() {
        val bottom = DisplayGeometry.fit(100, 200, 100, 200, 20, 0, false)
        assertEquals(180, bottom.height)
        val right = DisplayGeometry.fit(200, 100, 200, 100, 20, 1, false)
        assertEquals(180, right.width)
        val left = DisplayGeometry.fit(200, 100, 200, 100, 20, 3, false)
        assertEquals(20, left.left)
        val top = DisplayGeometry.fit(100, 200, 100, 200, 20, 2, false)
        assertEquals(20, top.top)
    }
    @Test fun hostResolutionFillsBehindControls() {
        assertEquals(DisplayGeometry.Rect(100, 200, 0, 0), DisplayGeometry.fit(100, 200, 100, 200, 20, 0, true))
        assertEquals(DisplayGeometry.Rect(200, 100, 0, 0), DisplayGeometry.fit(200, 100, 200, 100, 20, 1, true))
    }
    @Test fun convertsCurrentResolutionToNaturalDimensions() {
        assertEquals(1080 to 2400, DisplayGeometry.natural(2400, 1080, 1))
        assertEquals(1080 to 2400, DisplayGeometry.natural(1080, 2400, 0))
    }
}
