package app.aemu.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.StringWriter

class GuestLogTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun exportRetainsEarlyRecordsAndOtherBuffersAndServiceStderr() {
        val root = temp.newFolder("root")
        val run = temp.newFolder("run")
        val logs = File(root, "dev/log").apply { mkdirs() }
        File(logs, "main").outputStream().use { out ->
            repeat(400) { i -> out.write("\u0004Boot\u0000record $i\u0000".toByteArray()) }
        }
        File(logs, "system").writeBytes("\u0007Assert\u0000fatal reason\u0000".toByteArray())
        File(logs, "radio").writeBytes("\u0006Radio\u0000radio unavailable\u0000".toByteArray())
        File(run, "zygote.log").writeText("assertion on stderr\n")
        val out = StringWriter()
        GuestLog.writeExport(root, run, out)
        val text = out.toString()
        assertTrue(text.contains("I Boot: record 0\n"))
        assertTrue(text.contains("I Boot: record 399\n"))
        assertTrue(text.contains("=== guest system ===\nF Assert: fatal reason\n"))
        assertTrue(text.contains("E Radio: radio unavailable\n"))
        assertTrue(text.contains("=== service zygote stdout/stderr ===\nassertion on stderr\n"))
    }

    @Test fun exportResynchronizesAndIgnoresAnIncompleteTrailingRecord() {
        val root = temp.newFolder("root")
        val run = temp.newFolder("run")
        val logs = File(root, "dev/log").apply { mkdirs() }
        File(logs, "main").writeBytes("junk\u0005Tag\u0000valid message\u0000\u0007Abort\u0000unfinished".toByteArray())
        val out = StringWriter()
        GuestLog.writeExport(root, run, out)
        assertTrue(out.toString().contains("W Tag: valid message\n"))
        assertFalse(out.toString().contains("unfinished"))
    }
}
