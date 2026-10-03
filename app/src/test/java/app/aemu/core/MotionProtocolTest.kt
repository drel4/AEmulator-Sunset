package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import org.junit.Assert.*
import org.junit.Test

class MotionProtocolTest {
    @Test fun validatesRequests() {
        val b = ByteBuffer.allocate(12).order(LITTLE_ENDIAN).put("SNS1".toByteArray()).putInt(1).putInt(5).array()
        assertEquals(MotionProtocol.Request(false, 5), MotionProtocol.request(b))
        assertNull(MotionProtocol.request(b.copyOf(11)))
        b[8] = 8; assertNull(MotionProtocol.request(b))
        b[0] = 0; assertNull(MotionProtocol.request(b))
    }
    @Test fun infoOnlyReportsSupportedBits() {
        val b = ByteBuffer.wrap(MotionProtocol.info(5)).order(LITTLE_ENDIAN)
        assertEquals(5, b.getInt(4))
    }
    @Test fun emitsSIValuesAndNanosecondTimestamps() {
        val sample = MotionProtocol.Sample(1, 123456789L, floatArrayOf(1f, 2f, 9.81f), 3)
        val b = ByteBuffer.wrap(MotionProtocol.samples(listOf(sample))).order(LITTLE_ENDIAN)
        assertEquals(36, b.capacity()); assertEquals(1, b.getInt(4)); assertEquals(1, b.getInt(8))
        assertEquals(123456789L, b.getLong(12)); assertEquals(9.81f, b.getFloat(28), 0f); assertEquals(3, b.getInt(32))
    }
    @Test fun rejectsInvalidSamples() {
        assertThrows(IllegalArgumentException::class.java) { MotionProtocol.samples(listOf(MotionProtocol.Sample(1, 1, floatArrayOf(Float.NaN, 0f, 0f), 0))) }
        assertThrows(IllegalArgumentException::class.java) { MotionProtocol.samples(listOf(MotionProtocol.Sample(7, 1, floatArrayOf(0f, 0f, 0f), 0))) }
    }
}
