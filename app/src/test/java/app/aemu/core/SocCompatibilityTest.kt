package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class SocCompatibilityTest {
    @Test fun recognizesNamedFamiliesAndQualcommPartNumbers() {
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("SM8450", ""))
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("Snapdragon 8 Gen 3", ""))
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("", "qcom"))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("MediaTek Dimensity 9200", ""))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("MT6985", ""))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("", "mt6833"))
        assertEquals(SocCompatibility.Family.TENSOR, SocCompatibility.family("Google Tensor G4", ""))
        assertEquals(SocCompatibility.Family.TENSOR, SocCompatibility.family("", "gs101"))
    }
    @Test fun unknownOrOtherMediaTekIsNotSilentlyWhitelisted() {
        for (model in listOf("", "unknown", "Exynos 2400", "Unisoc T610", "Helio G99", "MT6789", "MT9999"))
            assertEquals(SocCompatibility.Family.UNVERIFIED, SocCompatibility.family(model, ""))
    }
}
