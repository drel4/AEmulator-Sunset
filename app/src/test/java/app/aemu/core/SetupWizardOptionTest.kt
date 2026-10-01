package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class SetupWizardOptionTest {
    @Test fun optionIsExplicit() {
        assertFalse(VmSettings().skipSetupWizard)
        assertTrue(VmSettings().copy(skipSetupWizard = true).skipSetupWizard)
    }
    @Test fun originalSonyMigrationStillRunsWithoutOptIn() {
        assertTrue(SetupWizardOption.restoreSonyFlow(false, false))
        assertFalse(SetupWizardOption.restoreSonyFlow(true, false))
    }
    @Test fun optOutDoesNotUndoCompletedSetupViaLegacyMigration() {
        assertFalse(SetupWizardOption.restoreSonyFlow(false, true))
        assertFalse(SetupWizardOption.restoreSonyFlow(true, true))
    }
    @Test fun restorationRunsWithOptionOffWhenStateExists() {
        assertFalse(SetupWizardOption.runHelper(false, false))
        assertTrue(SetupWizardOption.runHelper(false, true))
        assertTrue(SetupWizardOption.runHelper(true, false))
        assertTrue(SetupWizardOption.runHelper(true, true))
    }
}
