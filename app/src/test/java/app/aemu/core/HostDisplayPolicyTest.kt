package app.aemu.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HostDisplayPolicyTest {
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
