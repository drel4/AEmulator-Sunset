/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN

internal object MotionProtocol {
    val types = intArrayOf(1, 2, 4) // acceleration, magnetic field, angular velocity
    const val REQUEST_SIZE = 12
    data class Request(val info: Boolean, val mask: Int)
    data class Sample(val type: Int, val timestamp: Long, val values: FloatArray, val accuracy: Int)
    fun request(bytes: ByteArray): Request? {
        if (bytes.size != REQUEST_SIZE || !bytes.copyOfRange(0, 4).contentEquals("SNS1".toByteArray())) return null
        val b = ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN)
        val op = b.getInt(4); val mask = b.getInt(8)
        if (op !in 0..1 || mask and 7.inv() != 0) return null
        return Request(op == 0, mask)
    }
    fun info(mask: Int): ByteArray = ByteBuffer.allocate(8).order(LITTLE_ENDIAN)
        .put("SNI1".toByteArray()).putInt(mask and 7).array()
    fun samples(samples: List<Sample>): ByteArray {
        require(samples.size <= 3)
        val b = ByteBuffer.allocate(8 + samples.size * 28).order(LITTLE_ENDIAN)
            .put("SNE1".toByteArray()).putInt(samples.size)
        for (s in samples) {
            require(s.type in types && s.timestamp > 0 && s.values.size >= 3 && s.values.take(3).all { it.isFinite() })
            b.putInt(s.type).putLong(s.timestamp)
            s.values.take(3).forEach { b.putFloat(it) }
            b.putInt(s.accuracy.coerceIn(0, 3))
        }
        return b.array()
    }
}
