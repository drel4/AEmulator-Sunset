package app.aemu.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class BootMediaServicesTest {
    private fun fixture(run: (File) -> Unit) {
        val root = Files.createTempDirectory("boot-media").toFile()
        try {
            File(root, "system/bin").mkdirs()
            File(root, "system/bin/samsungani").writeText("elf")
            File(root, "system/bin/playsound").writeText("elf")
            File(root, "init.rc").writeText("service bootanim /system/bin/samsungani --boot\n    disabled\n    oneshot\nservice playsound /system/bin/playsound /system/media/PowerOn.ogg\n    disabled\n    oneshot\non property:service.bootanim.exit=0\n    start playsound\n    start dangerous\non property:service.bootanim.exit=1\n    stop playsound\n")
            run(root)
        } finally { root.deleteRecursively() }
    }
    @Test fun preservesFirmwareArgumentsAndNeverRestartsMedia() = fixture { root ->
        val animation = BootMediaServices.resolve(root, "bootanim")!!
        assertEquals(listOf("/system/bin/samsungani", "--boot"), animation.argv)
        assertFalse(animation.restart); assertTrue(animation.optional)
        assertEquals("/system/media/PowerOn.ogg", BootMediaServices.resolve(root, "playsound")!!.argv.last())
    }
    @Test fun allowsOnlyBootMediaTriggers() = fixture { root ->
        assertEquals(listOf(true to "playsound"), BootMediaServices.triggers(root, "service.bootanim.exit", "0"))
        assertEquals(listOf(false to "playsound"), BootMediaServices.triggers(root, "service.bootanim.exit", "1"))
        assertTrue(BootMediaServices.triggers(root, "sys.boot_completed", "1").isEmpty())
    }
    @Test fun absentMediaDoesNotCreateBrokenServices() = fixture { root ->
        File(root, "system/bin/playsound").delete()
        assertNull(BootMediaServices.resolve(root, "playsound"))
        assertNull(BootMediaServices.resolve(root, "charger"))
    }
}
