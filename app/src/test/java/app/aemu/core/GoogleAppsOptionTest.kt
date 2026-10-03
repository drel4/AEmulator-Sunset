package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class GoogleAppsOptionTest {
    @Test fun defaultIsOffAndIndependentOfSetupAndSensors() {
        assertFalse(VmSettings().disableGoogleApps)
        val settings = VmSettings().copy(disableGoogleApps = true)
        assertTrue(settings.disableGoogleApps)
        assertFalse(settings.skipSetupWizard)
        assertFalse(settings.motionSensors)
    }
    @Test fun optOutRestoresOnlyWhenTrackingStateExists() {
        assertFalse(GoogleAppsOption.runHelper(19, false, false, false, false))
        assertTrue(GoogleAppsOption.runHelper(19, false, true, false, false))
        assertTrue(GoogleAppsOption.runHelper(19, true, false, false, false))
    }
    @Test fun guestVersionBoundsAreExplicit() {
        assertTrue(GoogleAppsOption.runHelper(9, true, false, false, false))
        assertTrue(GoogleAppsOption.runHelper(25, true, false, false, false))
        assertFalse(GoogleAppsOption.runHelper(8, true, false, false, false))
        assertFalse(GoogleAppsOption.runHelper(26, true, false, false, false))
    }
    @Test fun neverStartsInRecoveryOrChargingMode() {
        assertFalse(GoogleAppsOption.runHelper(19, true, true, true, false))
        assertFalse(GoogleAppsOption.runHelper(19, true, true, false, true))
    }
}
