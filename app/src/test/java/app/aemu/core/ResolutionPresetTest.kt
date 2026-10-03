package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class ResolutionPresetTest {
    @Test fun hostEnablesExistingModeWithoutOverwritingManualValues() {
        val manual = VmSettings(width = 600, height = 1000, density = 280)
        assertEquals(manual.copy(hostResolution = true), ResolutionPreset.HOST.apply(manual))
    }
    @Test fun fixedPresetLeavesHostModeAndSetsDensity() {
        assertEquals(VmSettings(width = 720, height = 1280, density = 320),
            ResolutionPreset.HD.apply(VmSettings(hostResolution = true)))
    }
    @Test fun onlyHostIsSelectedWhenEnabled() {
        val settings = VmSettings(width = 480, height = 800, hostResolution = true)
        assertEquals(listOf(ResolutionPreset.HOST), ResolutionPreset.entries.filter { it.selected(settings) })
        assertEquals(listOf(ResolutionPreset.WVGA), ResolutionPreset.entries.filter { it.selected(settings.copy(hostResolution = false)) })
    }
}
