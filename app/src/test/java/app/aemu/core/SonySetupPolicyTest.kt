package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class SonySetupPolicyTest {
    @Test fun restoresOnlyOldSonyDisable() {
        val xml = "<package-restrictions><pkg enabled='2' name='com.sonyericsson.setupwizard'>" +
            "<disabled-components><item name='keep.Me'/></disabled-components></pkg>" +
            "<pkg name='other' enabled='2'/></package-restrictions>"
        val restored = SonySetupPolicy.restorePackage(xml)
        assertTrue(restored.contains("enabled=\"0\""))
        assertTrue(restored.contains("<item name='keep.Me'/>"))
        assertTrue(restored.contains("<pkg name='other' enabled='2'/>"))
        assertEquals(restored, SonySetupPolicy.restorePackage(restored))
        assertEquals(xml.replace("enabled='2'", "enabled='3'"),
            SonySetupPolicy.restorePackage(xml.replace("enabled='2'", "enabled='3'")))
    }
    @Test fun recognizesActualWizardCompletionPreference() {
        assertTrue(SonySetupPolicy.completed("<map><boolean value='true' name='setup_wizard_has_run'/></map>"))
        assertFalse(SonySetupPolicy.completed("<map><boolean name='setup_wizard_has_run' value='false'/></map>"))
        assertFalse(SonySetupPolicy.completed("<map><boolean name='unrelated' value='true'/></map>"))
        assertFalse(SonySetupPolicy.completed(""))
    }
}
