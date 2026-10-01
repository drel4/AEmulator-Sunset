package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** First camera bridge: HAL1, fixed 640x480 NV21 preview and JPEG snapshots. */
object CameraProtocol {
    const val WIDTH = 640
    const val HEIGHT = 480
    const val NV21_BYTES = WIDTH * HEIGHT * 3 / 2
    const val REQUEST_BYTES = 16
    const val MAX_JPEG_BYTES = 2 * 1024 * 1024
    data class Request(val operation: Int, val camera: Int, val quality: Int = 0)
    data class Info(val facing: Int, val orientation: Int)
    fun request(bytes: ByteArray): Request? {
        if (bytes.size != REQUEST_BYTES || !bytes.copyOfRange(0, 4).contentEquals("CAM1".toByteArray())) return null
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val operation = b.getInt(4); val camera = b.getInt(8)
        val quality = b.getInt(12)
        if (operation !in 0..2 || camera !in 0..1 || (operation == 0 && camera != 0)) return null
        if (if (operation == 2) quality !in 1..100 else quality != 0) return null
        return Request(operation, camera, quality)
    }
    fun information(cameras: List<Info>): ByteArray {
        require(cameras.size <= 2 && cameras.all { it.facing in 0..1 && it.orientation in listOf(0, 90, 180, 270) })
        return ByteBuffer.allocate(8 + cameras.size * 8).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("INF1".toByteArray()); putInt(cameras.size)
            cameras.forEach { putInt(it.facing); putInt(it.orientation) }
        }.array()
    }
    fun header(magic: String, value: Int, timestamp: Long = 0): ByteArray {
        require(magic in listOf("FRM1", "JPG1", "ERR1"))
        require(when (magic) { "FRM1" -> value == NV21_BYTES; "JPG1" -> value in 1..MAX_JPEG_BYTES; else -> value < 0 })
        return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(magic.toByteArray()); putInt(value); putLong(timestamp)
        }.array()
    }
}
