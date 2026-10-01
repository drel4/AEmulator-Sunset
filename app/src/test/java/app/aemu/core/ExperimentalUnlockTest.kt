package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class ExperimentalUnlockTest {
    @Test fun activatesOnlyOnTap42() {
        var taps = 0
        for (i in 1..42) {
            val next = ExperimentalUnlock.next(taps, false)
            assertEquals(i, next)
            assertEquals(i == 42, ExperimentalUnlock.activates(taps, next, false))
            taps = next
        }
        assertFalse(ExperimentalUnlock.activates(taps, ExperimentalUnlock.next(taps, false), false))
    }
    @Test fun alreadyUnlockedDoesNotTriggerVideoAgain() {
        assertEquals(0, ExperimentalUnlock.next(0, true))
        assertFalse(ExperimentalUnlock.activates(41, 42, true))
    }
}
