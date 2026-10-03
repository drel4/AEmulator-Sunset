package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class ManualMotionTest {
    @Test fun eachPhoneOrientationHasOneGravityAxis() {
        val expected = listOf(floatArrayOf(0f, 9.80665f, 0f), floatArrayOf(9.80665f, 0f, 0f),
            floatArrayOf(0f, -9.80665f, 0f), floatArrayOf(-9.80665f, 0f, 0f))
        for (turn in 0..3) assertArrayEquals(expected[turn], ManualMotion.gravity(turn), 0.00001f)
        assertArrayEquals(expected[0], ManualMotion.gravity(4), 0f)
        assertArrayEquals(expected[3], ManualMotion.gravity(-1), 0f)
    }
}
