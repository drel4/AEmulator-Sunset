package app.aemu.core

import org.junit.Test
import org.junit.Assert.*

class OneTimeVmNoteTest {
    @Test fun needsExplicitBootReport() {
        assertFalse(OneTimeVmNote().ready(100_000, true, true))
    }
    @Test fun waitsTenSecondsAfterReport() {
        val note = OneTimeVmNote(); note.reported(2000)
        assertFalse(note.ready(11_999, true, true))
        assertTrue(note.ready(12_000, true, true))
    }
    @Test fun duplicateReportsDoNotRestartTimer() {
        val note = OneTimeVmNote(); note.reported(2000); note.reported(9000)
        assertTrue(note.ready(12_000, true, true))
    }
    @Test fun stoppedFailedAndUnhealthyGuestsCannotConsume() {
        val note = OneTimeVmNote(); note.reported(0)
        assertFalse(note.ready(10_000, false, true))
        assertFalse(note.ready(10_000, true, false))
    }
    @Test fun crashRequiresNewReportAndFreshInterval() {
        val note = OneTimeVmNote(); note.reported(0); note.interrupted()
        assertFalse(note.ready(50_000, true, true))
        note.reported(50_000)
        assertFalse(note.ready(59_999, true, true))
        assertTrue(note.ready(60_000, true, true))
    }
    @Test fun resetAndBackwardsClockDoNotConsume() {
        val note = OneTimeVmNote(); note.reported(20_000)
        assertFalse(note.ready(19_000, true, true))
        note.interrupted(); assertFalse(note.ready(60_000, true, true))
    }
    @Test fun boundedPlainText() {
        assertEquals("hello\nworld", OneTimeVmNote.normalize(" \u0000hello\nworld "))
        assertEquals("", OneTimeVmNote.normalize(" \t\n"))
        assertEquals(2000, OneTimeVmNote.normalize("x".repeat(4000)).length)
    }
}
