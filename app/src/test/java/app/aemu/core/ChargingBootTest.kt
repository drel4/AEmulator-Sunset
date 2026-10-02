/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE. */
package app.aemu.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class ChargingBootTest {
    @Test fun realSonyBootRepairWhenFixtureProvided() = fixture { root ->
        val path = System.getenv("AEMU_CHARGING_BOOT") ?: return@fixture
        val bytes = File(path).readBytes()
        val entries = app.aemu.importer.BootImage.ramdisk(bytes)!!
        val original = entries.first { it.name == "charger" }.data
        assertTrue(ChargingBootInstaller.install(root, bytes) > 0)
        assertArrayEquals(original, File(root, "charger").readBytes())
        assertEquals("/charger", ChargingBoot.services(root, 19).first().argv.first())
        assertEquals(0, ChargingBootInstaller.install(root, bytes))
    }
    private fun fixture(action: (File) -> Unit) {
        val root = Files.createTempDirectory("charging-plan").toFile()
        try { action(root) } finally { root.deleteRecursively() }
    }
    private fun executable(root: File, path: String) { File(root, path).apply { parentFile!!.mkdirs(); writeText("fixture") } }
    @Test fun sonyChargingClassDoesNotStartNormalAndroid() = fixture { root ->
        executable(root, "charger"); executable(root, "sbin/healthd"); executable(root, "system/bin/app_process")
        File(root, "init.rc").writeText("service zygote /system/bin/app_process --zygote\n class main\nservice healthd-charger /sbin/healthd -n\n class charger\n")
        File(root, "init.qcom.rc").writeText("service charger /charger\n class charger\n")
        assertEquals(setOf("charger", "healthd-charger"), ChargingBoot.services(root).map { it.name }.toSet())
        assertEquals("charger", ChargingBoot.properties["ro.bootmode"])
    }
    @Test fun samsungLpmRcIsIncludedOnlyForCharging() = fixture { root ->
        executable(root, "system/bin/playlpm")
        File(root, "lpm.rc").writeText("service playlpm /system/bin/playlpm\n class charger\n")
        assertTrue(InitPlan.parse(listOf(File(root, "lpm.rc"))).services.isEmpty())
        assertEquals("playlpm", ChargingBoot.services(root).single().name)
    }
    @Test fun missingChargerFailsInsteadOfFallingBackToZygote() = fixture { root ->
        executable(root, "system/bin/app_process")
        assertTrue(runCatching { ChargingBoot.services(root) }.isFailure)
    }
    @Test fun healthdFallbackSelectsChargingMode() = fixture { root ->
        executable(root, "sbin/healthd")
        assertEquals(listOf("/sbin/healthd", "-c"), ChargingBoot.services(root).single().argv)
    }
    @Test fun oldSonyImportWithOnlyMonitorIsNotMistakenForChargingUi() = fixture { root ->
        executable(root, "sbin/healthd")
        File(root, "init.rc").writeText("service healthd-charger /sbin/healthd -n\n class charger\n")
        assertTrue(runCatching { ChargingBoot.services(root, 19) }.isFailure)
    }
    @Test fun installerRejectsTraversalAndNonChargingFiles() {
        for (name in listOf("../charger", "/charger", "res/images/charger/../../outside", "system/build.prop", "init.rc"))
            assertFalse(ChargingBootInstaller.allowed(name))
        for (name in listOf("charger", "sbin/healthd", "res/images/charger/battery_0.png"))
            assertTrue(ChargingBootInstaller.allowed(name))
    }
    @Test fun sysfsAdaptationOnlyChangesPrivateCopy() = fixture { root ->
        val source = File(root, "charger")
        val bytes = byteArrayOf(0x7f, 69, 76, 70) + "/sys/class/power_supply".toByteArray() + byteArrayOf(0)
        source.writeBytes(bytes)
        val runtime = ChargingBoot.runtimeService(root, GuestService("charger", listOf("/charger")))
        assertArrayEquals(bytes, source.readBytes())
        assertEquals("/dev/aemu-charging/charger", runtime.argv.first())
        assertTrue(File(root, runtime.argv.first().trimStart('/')).readText().contains("sys/class/power_supply"))
    }
}
