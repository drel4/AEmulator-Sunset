package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class CameraProtocolTest {
    private fun request(op: Int, camera: Int) = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        .put("CAM1".toByteArray()).putInt(op).putInt(camera).putInt(if (op == 2) 85 else 0).array()
    @Test fun validatesRequestsAndMetadata() {
        assertEquals(CameraProtocol.Request(1, 1), CameraProtocol.request(request(1, 1)))
        assertNull(CameraProtocol.request(request(0, 1)))
        assertNull(CameraProtocol.request(request(3, 0)))
        assertNull(CameraProtocol.request(request(1, -1)))
        assertNull(CameraProtocol.request(request(1, 0).copyOf(11)))
        assertNull(CameraProtocol.request(request(1, 0).apply { this[0] = 0 }))
        val info = ByteBuffer.wrap(CameraProtocol.information(listOf(CameraProtocol.Info(0, 90), CameraProtocol.Info(1, 270))))
            .order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(24, info.capacity()); assertEquals(2, info.getInt(4))
        assertEquals(0, info.getInt(8)); assertEquals(90, info.getInt(12))
        assertEquals(1, info.getInt(16)); assertEquals(270, info.getInt(20))
    }
    @Test fun serializesFrameHeader() {
        val b = ByteBuffer.wrap(CameraProtocol.header("FRM1", CameraProtocol.NV21_BYTES, 123456789012L)).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(16, b.capacity()); assertEquals(460800, b.getInt(4)); assertEquals(123456789012L, b.getLong(8))
    }
    @Test fun rejectsInvalidQualityAndUnusedFields() {
        assertNull(CameraProtocol.request(request(2, 0).apply { ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(12, 0) }))
        assertNull(CameraProtocol.request(request(2, 0).apply { ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(12, 101) }))
        assertNull(CameraProtocol.request(request(1, 0).apply { this[12] = 1 }))
        assertEquals(CameraProtocol.Request(2, 1, 85), CameraProtocol.request(request(2, 1)))
        assertEquals(8, CameraProtocol.information(emptyList()).size)
    }
    @Test(expected = IllegalArgumentException::class) fun boundsJpegPayload() {
        CameraProtocol.header("JPG1", CameraProtocol.MAX_JPEG_BYTES + 1)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidOrientation() {
        CameraProtocol.information(listOf(CameraProtocol.Info(0, 45)))
    }
}
