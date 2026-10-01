package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NavControlsTest {
    @Test fun buttonOrderAndStableIds() {
        assertEquals(listOf(NavButton.MENU, NavButton.BACK, NavButton.HOME),
            NavControls.parse("menu,unknown,back,menu, home"))
        assertEquals("menu,back,home", NavControls.encode(NavControls.parse("menu,back,home")))
        assertTrue(NavControls.parse("").isEmpty())
        assertEquals(4, NavControls.parse(NavControls.DEFAULT_BUTTONS).size)
    }

    @Test fun trackballPreservesFineMotionAndSigns() {
        val real = TrackballMotion(18f)
        assertEquals(0 to 0, real.move(1.5f, -1.5f, false))
        assertEquals(1 to -1, real.move(1.5f, -1.5f, false))
        assertEquals(6 to -6, real.move(18f, -18f, false))
        assertEquals(0 to 0, real.move(Float.NaN, 1f, false))
        val dpad = TrackballMotion(18f)
        assertEquals(0 to 0, dpad.move(9f, 9f, true))
        assertEquals(1 to 1, dpad.move(9f, 9f, true))
    }

    @Test fun relativeFrameIsArm32EvdevNotTouchOrArrowKeys() {
        val bytes = TrackballEvents.frame(-6, 3, nanos = 1_234_567_000L)
        assertEquals(48, bytes.size)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun event(type: Int, code: Int, value: Int) {
            assertEquals(1, b.int); assertEquals(234567, b.int)
            assertEquals(type, b.short.toInt()); assertEquals(code, b.short.toInt()); assertEquals(value, b.int)
        }
        event(2, 0, -6); event(2, 1, 3); event(0, 0, 0)
        val click = ByteBuffer.wrap(TrackballEvents.frame(button = true, nanos = 0)).order(ByteOrder.LITTLE_ENDIAN)
        click.position(8)
        assertEquals(1, click.short.toInt()); assertEquals(272, click.short.toInt()); assertEquals(1, click.int)
        assertTrue(TrackballEvents.frame(nanos = 0).isEmpty())
    }
}
