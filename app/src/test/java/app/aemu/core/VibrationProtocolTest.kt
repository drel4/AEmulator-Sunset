package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class VibrationProtocolTest {
    private fun frame(op: Int, duration: Long = 0) = ByteArray(12).apply {
        "VIB1".toByteArray().copyInto(this)
        VibrationProtocol.reply(op).copyInto(this, 4)
        VibrationProtocol.reply(duration.toInt()).copyInto(this, 8)
    }
    @Test fun validatesAndClampsUnsignedFrames() {
        assertEquals(123L, VibrationProtocol.decode(frame(1, 123))!!.durationMs)
        assertEquals(60_000L, VibrationProtocol.decode(frame(1, 0xffffffffL))!!.durationMs)
        assertNull(VibrationProtocol.decode(frame(3)))
        assertNull(VibrationProtocol.decode(frame(-1)))
        assertNull(VibrationProtocol.decode(frame(0, 1)))
        assertNull(VibrationProtocol.decode(frame(2, 1)))
        assertNull(VibrationProtocol.decode(frame(1).copyOf(11)))
        assertNull(VibrationProtocol.decode(frame(1).apply { this[0] = 0 }))
        assertArrayEquals(byteArrayOf(-22, -1, -1, -1), VibrationProtocol.reply(-22))
    }
    @Test fun ordersPulsesCancellationAndStop() {
        val events = mutableListOf<Long>()
        val controller = VibrationController({ true }, { true }, { events.add(it) }, { events.add(0) })
        assertEquals(0, controller.request(frame(0)))
        assertEquals(-19, controller.request(frame(1, 100)))
        controller.start()
        assertEquals(1, controller.request(frame(0)))
        assertEquals(0, controller.request(frame(1, 100)))
        assertEquals(0, controller.request(frame(1, 999999)))
        assertEquals(0, controller.request(frame(2)))
        assertEquals(0, controller.request(frame(1, 0)))
        controller.stop()
        assertEquals(-19, controller.request(frame(1, 100)))
        assertEquals(listOf(100L, 60000L, 0L, 0L, 0L), events)
    }
    @Test fun disabledOrMissingMotorNeverPulses() {
        for (flags in listOf(false to true, true to false)) {
            val controller = VibrationController({ flags.first }, { flags.second }, { fail("Unexpected pulse") }, {})
            controller.start()
            assertEquals(0, controller.request(frame(0)))
            assertEquals(0, controller.request(frame(1, 100)))
            assertEquals(0, controller.request(frame(2)))
        }
    }
    @Test fun rejectsMalformedInputAndReportsHostErrors() {
        val controller = VibrationController({ true }, { true }, { throw SecurityException() }, { throw SecurityException() })
        controller.start()
        assertEquals(-22, controller.request(frame(3)))
        assertEquals(-5, controller.request(frame(1, 100)))
        assertEquals(-5, controller.request(frame(2)))
        controller.stop() // cancellation failure must not prevent shutdown
        assertEquals(0, controller.request(frame(0)))
    }
}
