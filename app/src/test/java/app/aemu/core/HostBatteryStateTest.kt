package app.aemu.core

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class HostBatteryStateTest {
    @Test fun hostBatteryDefaultsOnAndCanBeDisabled() {
        assertTrue(VmSettings().hostBattery)
        assertTrue(VmSettings.fromJson(null).hostBattery)
        assertFalse(VmSettings().copy(hostBattery = false).hostBattery)
    }
    @Test fun percentageUsesScaleAndClampsWithoutOverflow() {
        assertEquals(50, HostBatteryState.from(100, 200, 3, 0)!!.percent)
        assertEquals(100, HostBatteryState.from(Int.MAX_VALUE, 1, 3, 0)!!.percent)
        assertEquals(0, HostBatteryState.from(0, 100, 3, 0)!!.percent)
    }
    @Test fun missingBatteryReadingDoesNotInventFullCharge() {
        assertNull(HostBatteryState.from(-1, 100, 3, 0))
        assertNull(HostBatteryState.from(50, 0, 3, 0))
        assertNull(HostBatteryState.from(50, -1, 3, 0))
    }
    @Test fun unpluggedReportsDischargingAndClearsAllChargers() {
        val state = HostBatteryState.from(43, 100, 3, 0)!!
        assertEquals("Discharging", state.statusText)
        val command = state.command(19)
        assertTrue(command.contains("set level 43")); assertTrue(command.contains("set status 3"))
        for (charger in listOf("ac", "usb", "wireless")) assertTrue(command.contains("set $charger 0"))
    }
    @Test fun wirelessUsesAcOnOlderFrameworks() {
        val state = HostBatteryState.from(95, 100, 2, 4)!!
        assertTrue(state.command(17).contains("set ac 1"))
        assertFalse(state.command(17).contains("wireless"))
        assertTrue(state.command(19).contains("set wireless 1"))
        assertTrue(state.command(19).contains("set ac 0"))
    }
    @Test fun statusesAndPlugTypesRemainDistinct() {
        val full = HostBatteryState.from(100, 100, 5, 2)!!
        assertEquals("Full", full.statusText); assertTrue(full.usb); assertFalse(full.ac)
        assertEquals("Not charging", HostBatteryState.from(80, 100, 4, 1)!!.statusText)
        assertEquals(1, HostBatteryState.from(80, 100, 999, 0)!!.status)
    }
    @Test fun virtualSysfsTracksLevelAndUnplugging() {
        val root = Files.createTempDirectory("battery-root").toFile()
        try {
            HostBatteryState.from(62, 100, 2, 1)!!.writeSysfs(root)
            assertEquals("62\n", root.resolve("sys/class/power_supply/battery/capacity").readText())
            HostBatteryState.from(61, 100, 3, 0)!!.writeSysfs(root)
            assertEquals("Discharging\n", root.resolve("sys/class/power_supply/battery/status").readText())
            assertEquals("0\n", root.resolve("sys/class/power_supply/ac/online").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun virtualSysfsRejectsLinksOutsideVm() {
        val root = Files.createTempDirectory("battery-root").toFile()
        val outside = Files.createTempDirectory("battery-outside").toFile()
        try {
            val power = root.resolve("sys/class/power_supply")
            power.mkdirs()
            Files.createSymbolicLink(power.resolve("battery").toPath(), outside.toPath())
            assertThrows(IllegalStateException::class.java) { HostBatteryState.from(12, 100, 3, 0)!!.writeSysfs(root) }
            assertTrue(outside.list()!!.isEmpty())
            Files.delete(power.resolve("battery").toPath())
        } finally { root.deleteRecursively(); outside.deleteRecursively() }
    }
}
