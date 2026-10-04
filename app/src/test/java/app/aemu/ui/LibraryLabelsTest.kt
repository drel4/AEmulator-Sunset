package app.aemu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLabelsTest {
    @Test fun abbreviatesOnlyKnownCyanogenModLabels() {
        assertEquals("CM11", LibraryLabels.compactSkin("CyanogenMod 11"))
        assertEquals("CM7.2", LibraryLabels.compactSkin("CyanogenMod 7.2"))
        assertEquals("CM", LibraryLabels.compactSkin("CyanogenMod"))
    }
    @Test fun preservesOtherRomIdentities() {
        for (skin in listOf("TouchWiz", "Xperia UI", "AOSP", "LineageOS", "CyanogenMod custom"))
            assertEquals(skin, LibraryLabels.compactSkin(skin))
    }
}
