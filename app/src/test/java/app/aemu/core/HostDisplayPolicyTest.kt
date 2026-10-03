package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class HostDisplayPolicyTest {
    @Test fun tabletNavbarUsesWindowRotationNotMotionSensor() {
        assertEquals(0, HostDisplayPolicy.controlsTurns(true, true, 1))
        assertEquals(1, HostDisplayPolicy.controlsTurns(true, false, 1))
        assertEquals(0, HostDisplayPolicy.controlsTurns(false, false, 1))
        assertEquals(0f, HostDisplayPolicy.controlIconRotation(true, true, 1, 0), 0f)
        assertEquals(0f, HostDisplayPolicy.controlIconRotation(true, false, 1, 0), 0f)
        assertEquals(-90f, HostDisplayPolicy.controlIconRotation(true, false, 0, 1), 0f)
    }
    @Test fun tabletsCanRotateButPhonesRemainFixed() {
        assertFalse(HostDisplayPolicy.rotateWindow(0))
        assertFalse(HostDisplayPolicy.rotateWindow(599))
        assertTrue(HostDisplayPolicy.rotateWindow(600))
        assertTrue(HostDisplayPolicy.rotateWindow(800))
    }
    @Test fun rotatingWindowDoesNotRotateIconsTwice() {
        for (turn in 0..3) assertEquals(0f, HostDisplayPolicy.iconRotation(turn, turn), 0f)
        assertEquals(-90f, HostDisplayPolicy.iconRotation(1, 0), 0f)
        assertEquals(-270f, HostDisplayPolicy.iconRotation(0, 1), 0f)
    }
    @Test fun iconsFollowAllFourPhysicalOrientations() {
        for (turn in 0..3) assertEquals(turn, HostDisplayPolicy.iconTurns(turn * 90, 0))
    }
    @Test fun diagonalHysteresisKeepsPreviousIcons() {
        assertEquals(0, HostDisplayPolicy.iconTurns(45, 0))
        assertEquals(1, HostDisplayPolicy.iconTurns(45, 1))
        assertEquals(1, HostDisplayPolicy.iconTurns(60, 0))
    }
    @Test fun unknownAndWraparoundAreSafe() {
        assertEquals(2, HostDisplayPolicy.iconTurns(-1, 2))
        assertEquals(0, HostDisplayPolicy.iconTurns(359, 3))
    }
}
