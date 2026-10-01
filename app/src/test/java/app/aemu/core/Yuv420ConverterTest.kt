package app.aemu.core

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class Yuv420ConverterTest {
    private fun plane(bytes: ByteArray, row: Int, pixel: Int = 1) = Yuv420Converter.Plane(ByteBuffer.wrap(bytes), row, pixel)
    @Test fun honorsPaddingAndInterleavedChroma() {
        val y = plane(byteArrayOf(1, 2, 3, 4, 99, 5, 6, 7, 8, 99), 5)
        val u = plane(byteArrayOf(20, 99, 21), 4, 2)
        val v = plane(byteArrayOf(30, 99, 31), 4, 2)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 30, 20, 31, 21),
            Yuv420Converter.nv21(4, 2, listOf(y, u, v), width = 4, height = 2))
    }
    @Test fun honorsBufferPositionsWithoutChangingThem() {
        val p = plane(byteArrayOf(99, 99, 1, 2, 3, 4), 2)
        p.buffer.position(2)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 9, 8), Yuv420Converter.nv21(2, 2,
            listOf(p, plane(byteArrayOf(8), 1), plane(byteArrayOf(9), 1)), width = 2, height = 2))
        assertEquals(2, p.buffer.position())
    }
    @Test fun cropsToTargetAspectAndScales() {
        val y = plane(ByteArray(16) { it.toByte() }, 8)
        val u = plane(ByteArray(4) { 20 }, 4)
        val v = plane(ByteArray(4) { 30 }, 4)
        assertArrayEquals(byteArrayOf(2, 3, 10, 11, 30, 20),
            Yuv420Converter.nv21(8, 2, listOf(y, u, v), width = 2, height = 2))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsTruncatedPlane() {
        Yuv420Converter.nv21(2, 2, listOf(plane(byteArrayOf(1), 2), plane(byteArrayOf(2), 1), plane(byteArrayOf(3), 1)), width = 2, height = 2)
    }
}
