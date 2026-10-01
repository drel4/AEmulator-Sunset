package app.aemu.core

import java.nio.ByteBuffer

/** Respect plane origins/row/pixel strides; center-crop then nearest-neighbor scale to NV21. */
object Yuv420Converter {
    data class Plane(val buffer: ByteBuffer, val rowStride: Int, val pixelStride: Int)
    fun nv21(sourceWidth: Int, sourceHeight: Int, planes: List<Plane>,
             cropLeft: Int = 0, cropTop: Int = 0, cropWidth: Int = sourceWidth, cropHeight: Int = sourceHeight,
             width: Int = CameraProtocol.WIDTH, height: Int = CameraProtocol.HEIGHT): ByteArray {
        require(sourceWidth in 2..8192 && sourceHeight in 2..8192 && planes.size == 3)
        require(width in 2..1920 && height in 2..1080 && width % 2 == 0 && height % 2 == 0)
        require(cropLeft >= 0 && cropTop >= 0 && cropWidth >= 2 && cropHeight >= 2 &&
            cropLeft.toLong() + cropWidth <= sourceWidth && cropTop.toLong() + cropHeight <= sourceHeight)
        require(cropLeft % 2 == 0 && cropTop % 2 == 0)
        require(planes.all { it.rowStride > 0 && it.pixelStride > 0 })
        var cw = cropWidth; var ch = cropHeight
        if (cw.toLong() * height > ch.toLong() * width) cw = ch * width / height else ch = cw * height / width
        cw = cw and -2; ch = ch and -2
        require(cw >= 2 && ch >= 2)
        val left = (cropLeft + (cropWidth - cw) / 2) and -2
        val top = (cropTop + (cropHeight - ch) / 2) and -2
        fun sample(p: Plane, x: Int, y: Int): Byte {
            val index = p.buffer.position().toLong() + y.toLong() * p.rowStride + x.toLong() * p.pixelStride
            require(index in p.buffer.position().toLong() until p.buffer.limit().toLong())
            return p.buffer.get(index.toInt())
        }
        val out = ByteArray(width * height * 3 / 2)
        var index = 0
        for (y in 0 until height) for (x in 0 until width) {
            out[index++] = sample(planes[0], left + x * cw / width, top + y * ch / height)
        }
        for (y in 0 until height / 2) for (x in 0 until width / 2) {
            val sx = (left + x * 2 * cw / width) / 2
            val sy = (top + y * 2 * ch / height) / 2
            out[index++] = sample(planes[2], sx, sy)
            out[index++] = sample(planes[1], sx, sy)
        }
        return out
    }
}
