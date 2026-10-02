/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class LowPowerBootTest {
    @Test fun presetOnlyChangesTemporaryRefreshInputAndHostAwake() {
        val saved = VmSettings(ramMb = 2048, camera = true)
        val session = LowPowerBoot.apply(saved)
        assertEquals(saved.copy(fbHz = 30, touchHz = 30, keepScreenOn = false), session)
        assertEquals(60, saved.fbHz)
        assertTrue(saved.keepScreenOn)
        assertEquals(saved, saved.copy()) // Stored image retains this original object.
    }
    @Test fun presetNeverRaisesLowerRates() {
        val saved = VmSettings(fbHz = 15, touchHz = 20, keepScreenOn = false)
        assertEquals(saved, LowPowerBoot.apply(saved))
        assertEquals(1, LowPowerBoot.apply(saved.copy(fbHz = 0)).fbHz)
    }
}
