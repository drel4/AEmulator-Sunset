package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class AudioHalAbiTest {
    private fun fixture(): ByteArray = ByteArray(256).apply {
        byteArrayOf(0x7f, 69, 76, 70, 1, 1).copyInto(this)
        this[18] = 40
        "AudioStreamOutSink".toByteArray().copyInto(this, 64)
        byteArrayOf(0x13, 0xb5.toByte(), 0x0c, 0x46, 0x80.toByte(), 0x69,
            0x83.toByte(), 0x6e, 0x13, 0xb9.toByte(), 0x6f, 0xf0.toByte(), 0x25, 0x00).copyInto(this, 128)
    }
    @Test fun recognizesVerifiedThumbLayout() {
        assertTrue(AudioHalAbi.usesDirectTrackTail(fixture()))
        assertFalse(AudioHalAbi.usesDirectTrackTail(fixture().apply { this[134] = 0x03 })) // AOSP slot 0x60
        assertFalse(AudioHalAbi.usesDirectTrackTail(fixture().apply { this[64] = 0 }))
    }
    @Test fun rejectsWrongArchitectureAndTruncation() {
        assertFalse(AudioHalAbi.usesDirectTrackTail(byteArrayOf()))
        assertFalse(AudioHalAbi.usesDirectTrackTail(fixture().copyOf(135)))
        for (offset in listOf(0, 4, 5, 18, 19)) {
            assertFalse(AudioHalAbi.usesDirectTrackTail(fixture().apply { this[offset] = 2 }))
        }
    }
}
