package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class RomCardNoteTest {
    private fun image() = GuestImage(id = "test", name = "Renamed VM", release = "4.4.2", api = 19,
        brand = "Aemulator", model = "AEmulator Sunset CM11", skin = "CyanogenMod 11", engine = Engine.KK,
        bootclasspath = "", exports = emptyMap(), dirs = emptyList(), services = emptyList(),
        sdcardPath = "/sdcard", settings = VmSettings(), createdAt = 0)

    @Test fun notesDefaultToEmptyForOldProfiles() {
        assertEquals("", image().romCardNote)
    }
    @Test fun noteIsBoundedPlainTextWithMultilineInstructions() {
        assertEquals("Disable JIT\nUse software graphics", RomCardNote.normalize("\u0000 Disable JIT\nUse software graphics\r "))
        assertEquals("", RomCardNote.normalize("\t\n "))
        assertEquals(2000, RomCardNote.normalize("a".repeat(3000)).length)
    }
    @Test fun removesOnlyExactBuiltinCmWarningAndKeepsDiagnostics() {
        val warnings = listOf("Experimental CM11 guest; not firmware for a physical device.",
            "Experimental CM11 build with first-boot IME fix. Needs testing.", "No boot.img found")
        assertEquals(listOf("No boot.img found"), image().copy(warnings = warnings).cardWarnings)
    }
    @Test fun otherRomsAndSimilarWarningsAreNotHidden() {
        val warning = "Experimental CM11 guest; not firmware for a physical device."
        assertEquals(listOf(warning), RomCardNote.warnings("Sony Xperia ZR", 19, listOf(warning)))
        assertEquals(listOf(warning), RomCardNote.warnings("AEmulator Sunset CM11", 18, listOf(warning)))
        assertEquals(listOf("Custom CM11 warning"), RomCardNote.warnings("AEmulator Sunset CM11", 19, listOf("Custom CM11 warning")))
    }
    @Test fun exportedAuthorNoteIsNotTreatedAsBuiltinWarning() {
        val note = "Experimental CM11 guest; not firmware for a physical device."
        assertEquals(note, RomCardNote.exportImage(image(), VmSettings(), note, "").romCardNote)
    }
    @Test fun exportKeepsNotesSeparateAndDoesNotModifySourceVm() {
        val source = image().copy(romCardNote = "Old note", oneTimeNote = "Old boot note")
        val exported = RomCardNote.exportImage(source, VmSettings(jit = false), " Disable JIT ", "First boot")
        assertEquals("Disable JIT", exported.romCardNote)
        assertEquals("First boot", exported.oneTimeNote)
        assertFalse(exported.settings.jit)
        assertEquals("Old note", source.romCardNote)
        assertEquals("Old boot note", source.oneTimeNote)
    }
    @Test fun consumingOneTimeNoteDoesNotConsumeCardNote() {
        val image = image().copy(romCardNote = "GPU bridge off", oneTimeNote = "Wait for first boot")
        assertEquals("GPU bridge off", image.copy(oneTimeNote = "", bootCount = 1).romCardNote)
    }
    @Test fun blankExportNoteClearsOnlyExportedAuthorAdvice() {
        val source = image().copy(romCardNote = "Old note", warnings = listOf("Missing boot image"))
        val exported = RomCardNote.exportImage(source, VmSettings(), " ", "")
        assertEquals("", exported.romCardNote)
        assertEquals(listOf("Missing boot image"), exported.cardWarnings)
    }
}
