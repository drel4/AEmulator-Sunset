package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class LegacySimIoTest {
    private fun request(command: Int, file: Int = 0x2fe2, p1: Int = 0, p2: Int = 0, p3: Int = 10,
                        path: String? = "3F00"): ByteBuffer {
        val b = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(command).putInt(file)
        if (path == null) b.putInt(-1) else {
            b.putInt(path.length)
            path.forEach { b.putChar(it) }; b.putChar('\u0000')
            while (b.position() % 4 != 0) b.put(0)
        }
        b.putInt(p1).putInt(p2).putInt(p3)
        b.flip()
        return b
    }
    @Test fun readsIdentityAndTransparentHeader() {
        val header = LegacySimIo.handle(request(0xc0, p3 = 15))
        assertEquals(0x90, header.sw1)
        assertEquals(30, header.hex!!.length)
        assertEquals("000A", header.hex.substring(4, 8))
        assertEquals("04", header.hex.substring(12, 14))
        val reply = LegacySimIo.handle(request(0xb0))
        val decoded = reply.hex!!.chunked(2).joinToString("") { "${it[1]}${it[0]}" }
        assertEquals(LegacySimIo.ICCID, decoded)
        assertEquals(reply, LegacySimIo.handle(request(0xb0, path = null)))
    }
    @Test fun supportsOffsetsWithoutMutatingCaller() {
        val b = request(0xb0, p2 = 8, p3 = 2)
        assertEquals("0040", LegacySimIo.handle(b).hex)
        assertEquals(0, b.position())
    }
    @Test fun rejectsInvalidOrUnsupportedCommands() {
        assertEquals(0x82, LegacySimIo.handle(request(0xb0, file = 0x6f3a)).sw2)
        assertEquals(0x82, LegacySimIo.handle(request(0xb0, path = "3F007F20")).sw2)
        assertEquals(0x86, LegacySimIo.handle(request(0xb0, p2 = 9, p3 = 2)).sw2)
        assertEquals(0x6d, LegacySimIo.handle(request(0xd6)).sw1)
        assertEquals(0x86, LegacySimIo.handle(ByteBuffer.allocate(0)).sw2)
        assertEquals(0x86, LegacySimIo.handle(request(0xc0, p3 = 14)).sw2)
    }
}
