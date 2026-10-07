package app.aemu.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class ExtendedEnvironmentTest {
    private fun fixture(test: (File) -> Unit) {
        val root = Files.createTempDirectory("extended-env").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
    @Test fun defaultAndUnknownProfilesStaySunset() {
        assertEquals(UniversalProfile.SUNSET, VmSettings().universalProfile)
        assertEquals(UniversalProfile.SUNSET, UniversalProfile.byId("senses"))
        assertEquals(UniversalProfile.EXTENDED, UniversalProfile.byId("sunset_extended"))
    }
    @Test fun extendedNeverAppliesToDefaultOrGingerbread() {
        assertFalse(ExtendedEnvironment.active(Engine.KK, UniversalProfile.SUNSET))
        assertFalse(ExtendedEnvironment.active(Engine.GB, UniversalProfile.EXTENDED))
        assertTrue(ExtendedEnvironment.active(Engine.KK, UniversalProfile.EXTENDED))
    }
    @Test fun legacySelectionAndOldSettingsArePreserved() {
        val image = GuestImage(id = "test", name = "Test", release = "2.3", api = 10,
            brand = "", model = "", skin = "AOSP", engine = Engine.KK, bootclasspath = "",
            exports = emptyMap(), dirs = emptyList(), services = emptyList(), sdcardPath = "/sdcard",
            settings = VmSettings(), createdAt = 0)
        assertEquals(Engine.KK, image.effective().engine)
        assertEquals(Engine.GB, image.copy(settings = VmSettings(legacyEngine = true,
            universalProfile = UniversalProfile.EXTENDED)).effective().engine)
        assertEquals(Engine.KK, image.copy(api = 19, settings = VmSettings(legacyEngine = true)).effective().engine)
    }
    @Test fun bootMediaKeepsItsOwnEnvironment() = fixture { root ->
        File(root, "system/bin").mkdirs()
        File(root, "system/bin/bootanimation").writeText("elf")
        File(root, "init.rc").writeText("service bootanim /system/bin/bootanimation\n setenv LANG boot\n disabled\n oneshot\n")
        assertEquals(mapOf("LANG" to "boot"), BootMediaServices.resolve(root, "bootanim")!!.environment)
    }
    @Test fun searchPreservesOrderAndAddsOnlyExistingDirectories() = fixture { root ->
        File(root, "system/lib").mkdirs(); File(root, "system/vendor/lib").mkdirs()
        assertEquals("/custom/lib:/system/lib:/system/vendor/lib",
            ExtendedEnvironment.searchPath(root, "/custom/lib:/system/lib", ExtendedEnvironment.libraryDirs))
    }
    @Test fun preservesEmptySearchComponents() = fixture { root ->
        File(root, "system/lib").mkdirs()
        assertEquals(":/custom::/system/lib", ExtendedEnvironment.searchPath(root, ":/custom:", ExtendedEnvironment.libraryDirs))
    }
    @Test fun externalSymlinkIsNotAdded() = fixture { root ->
        val outside = Files.createTempDirectory("outside-env")
        try {
            File(root, "system").mkdirs()
            Files.createSymbolicLink(File(root, "system/lib").toPath(), outside)
            assertEquals("/custom", ExtendedEnvironment.searchPath(root, "/custom", ExtendedEnvironment.libraryDirs))
        } finally { Files.delete(outside) }
    }
    @Test fun quotesEscapesAndEmptyValues() {
        assertEquals("LANG" to "en US", ExtendedEnvironment.setenv("setenv LANG \"en US\""))
        assertEquals("LANG" to "en US", ExtendedEnvironment.setenv("setenv LANG en\\ US"))
        assertEquals("EMPTY" to "", ExtendedEnvironment.setenv("setenv EMPTY \"\""))
        assertEquals("X" to "a\\nb", ExtendedEnvironment.setenv("setenv X \"a\\nb\""))
        assertEquals("X" to "value", ExtendedEnvironment.setenv("setenv X value # comment"))
        assertNull(ExtendedEnvironment.setenv("setenv X a\\nb"))
    }
    @Test fun malformedAndUnresolvedValuesAreIgnored() {
        for (line in listOf("setenv X \"broken", "setenv X tail\\", "setenv X a b", "setenv 1BAD x",
                "setenv X \${ro.foo}", "setenv X")) assertNull(line, ExtendedEnvironment.setenv(line))
    }
    @Test fun firmwareCannotOverrideHostBridgesOrLoader() {
        for (key in listOf("DHD_BINDER", "DHD_ENV_LD_PRELOAD", "QEMU_STRACE", "AEMU_GL_RETARGET",
                "ANDROID_SOCKET_zygote", "ANDROID_PROPERTY_WORKSPACE", "LD_PRELOAD", "LD_AUDIT", "LD_DEBUG",
                "BOOTCLASSPATH", "ANDROID_ROOT", "ANDROID_DATA", "ASHMEM_SHIM_DIR"))
            assertFalse(key, ExtendedEnvironment.allowed(key, "x"))
        assertFalse(ExtendedEnvironment.allowed("LANG", "x\u0000y"))
        assertFalse(ExtendedEnvironment.allowed("LANG", "x".repeat(8193)))
    }
    @Test fun serviceValuesOnlyUseGuestTransport() = fixture { root ->
        File(root, "system/lib").mkdirs()
        val env = ExtendedEnvironment.service(root, mapOf("LANG" to "ru_RU", "LD_LIBRARY_PATH" to "/custom/lib",
            "LD_PRELOAD" to "/evil.so", "DHD_BINDER" to "evil"))
        assertEquals(mapOf("DHD_ENV_LANG" to "ru_RU", "DHD_ENV_LD_LIBRARY_PATH" to "/custom/lib:/system/lib"), env)
    }
    @Test fun setenvStaysLocalToServiceAndLastValueWins() = fixture { root ->
        val rc = File(root, "init.rc").apply { writeText("""
            service zygote /system/bin/app_process --zygote
                setenv LANG old
                setenv LANG "en US"
                setenv LD_PRELOAD /evil.so
            service media /system/bin/mediaserver
                setenv LANG other
            on boot
                setenv LANG global
        """.trimIndent()) }
        val parsed = InitPlan.parse(listOf(rc))
        assertEquals(mapOf("LANG" to "en US"), parsed.services.getValue("zygote").environment)
        assertEquals(mapOf("LANG" to "other"), parsed.services.getValue("media").environment)
        assertTrue(parsed.exports.isEmpty())
    }
    @Test fun plannedCoreAndOptionalServicesCarryEnvironment() = fixture { root ->
        File(root, "system/bin").mkdirs()
        for (bin in listOf("app_process", "customd")) File(root, "system/bin/$bin").writeText("elf")
        val rc = File(root, "init.rc").apply { writeText("service zygote /system/bin/app_process --zygote\n setenv LANG core\nservice custom /system/bin/customd\n setenv LANG optional\n") }
        val plan = InitPlan.plan(InitPlan.parse(listOf(rc)), 19, root)
        assertEquals(mapOf("LANG" to "core"), plan.first { it.name == "zygote" }.environment)
        assertEquals(mapOf("LANG" to "optional"), plan.first { it.name == "custom" }.environment)
    }
}
