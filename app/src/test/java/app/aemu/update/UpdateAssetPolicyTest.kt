package app.aemu.update

import org.junit.Assert.*
import org.junit.Test

class UpdateAssetPolicyTest {
    private val standard = "AEmulator-Sunset-0.0.0.3-sunset.20-app.aemu.apk"
    private val clone = "AEmulator-Sunset-0.0.0.3-sunset.20-app.aemu.clone.apk"

    @Test fun choosesCloneRegardlessOfAssetOrder() {
        assertEquals(clone, listOf(standard, clone).firstOrNull { UpdateAssetPolicy.matches(it, "app.aemu.clone") })
        assertEquals(clone, listOf(clone, standard).firstOrNull { UpdateAssetPolicy.matches(it, "app.aemu.clone") })
    }
    @Test fun choosesStandardAndNeverClone() {
        assertTrue(UpdateAssetPolicy.matches(standard, "app.aemu"))
        assertFalse(UpdateAssetPolicy.matches(clone, "app.aemu"))
    }
    @Test fun missingVariantDoesNotFallBackToOtherApks() {
        assertNull(listOf(standard, "unknown.apk", "$clone.asc").firstOrNull { UpdateAssetPolicy.matches(it, "app.aemu.clone") })
        assertFalse(UpdateAssetPolicy.matches(clone, "other.app"))
    }
    @Test fun matchingIsCaseInsensitiveButRequiresExactPackageSuffix() {
        assertTrue(UpdateAssetPolicy.matches(clone.uppercase(), "app.aemu.clone"))
        assertFalse(UpdateAssetPolicy.matches("$clone.zip", "app.aemu.clone"))
        assertFalse(UpdateAssetPolicy.matches("prefix-app.aemu.clone.fake.apk", "app.aemu.clone"))
    }
}
