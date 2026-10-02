/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HeldNavKeysTest {
    @Test fun downIsImmediateAndUpIsNotSentUntilRelease() {
        val events = mutableListOf<Pair<Int, Boolean>>()
        val keys = HeldNavKeys { code, down -> events += code to down }
        keys.down(102)
        assertEquals(listOf(102 to true), events)
        keys.up(102)
        assertEquals(listOf(102 to true, 102 to false), events)
    }
    @Test fun repeatedDownAndLateReleaseAreIdempotent() {
        val events = mutableListOf<Pair<Int, Boolean>>()
        val keys = HeldNavKeys { code, down -> events += code to down }
        keys.down(102); keys.down(102); keys.releaseAll(); keys.up(102); keys.releaseAll()
        assertEquals(listOf(102 to true, 102 to false), events)
    }
    @Test fun backgroundReleasesEveryHeldButtonAndAllowsAnotherPress() {
        val events = mutableListOf<Pair<Int, Boolean>>()
        val keys = HeldNavKeys { code, down -> events += code to down }
        keys.down(102); keys.down(115); keys.releaseAll(); keys.down(102); keys.up(102)
        assertEquals(listOf(102 to true, 115 to true, 102 to false, 115 to false, 102 to true, 102 to false), events)
    }
}
