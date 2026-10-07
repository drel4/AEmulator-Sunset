/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.core

/** Persistent author advice is independent of analyzer warnings and one-time boot notes. */
internal object RomCardNote {
    const val MAX_LENGTH = 2000
    private val LEGACY_CM_WARNINGS = setOf(
        "Experimental CM11 guest; not firmware for a physical device.",
        "Experimental CM11 build with first-boot IME fix. Needs testing.",
    )
    fun normalize(text: String): String = OneTimeVmNote.normalize(text).take(MAX_LENGTH)
    fun warnings(model: String, api: Int, warnings: List<String>): List<String> =
        if (model == "AEmulator Sunset CM11" && api == 19) warnings.filterNot { it in LEGACY_CM_WARNINGS }
        else warnings

    fun exportImage(image: GuestImage, settings: VmSettings, cardNote: String, bootNote: String): GuestImage =
        image.copy(settings = settings, romCardNote = normalize(cardNote), oneTimeNote = OneTimeVmNote.normalize(bootNote))
}
