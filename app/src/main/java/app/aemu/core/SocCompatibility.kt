/* AEmulator Sunset addition, 2026-10-04. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.util.Locale

/** Advisory family recognition, not a claim that every chip/ROM in a family boots. */
internal object SocCompatibility {
    enum class Family { SNAPDRAGON, DIMENSITY, TENSOR, UNVERIFIED }
    // Conservative, finite mobile platform allowlist; never accept every MediaTek chip.
    private val dimensityPlatforms = setOf("6833", "6835", "6853", "6855", "6858", "6873", "6877", "6878",
        "6879", "6881", "6883", "6885", "6886", "6889", "6891", "6893", "6895", "6896", "6897", "6899",
        "6983", "6985", "6989", "6991", "6993")
    fun family(model: String, hardware: String): Family {
        val chip = "$model $hardware".lowercase(Locale.ROOT)
        return when {
            Regex("\\bsnapdragon\\b|\\b(sm|sdm|msm|apq)\\d{4}\\w*\\b|\\bqcom\\b").containsMatchIn(chip) -> Family.SNAPDRAGON
            Regex("\\bdimensity\\b").containsMatchIn(chip) ||
                Regex("\\bmt(\\d{4})[a-z0-9]*\\b").findAll(chip).any { it.groupValues[1] in dimensityPlatforms } -> Family.DIMENSITY
            Regex("\\btensor\\b|\\bgs(101|201)\\b").containsMatchIn(chip) -> Family.TENSOR
            else -> Family.UNVERIFIED
        }
    }
}
