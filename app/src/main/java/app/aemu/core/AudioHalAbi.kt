package app.aemu.core

/** Recognizes the verified stock ZR Thumb callback layout, not merely a brand. */
object AudioHalAbi {
    private val timestampPrefix = byteArrayOf(
        0x13, 0xb5.toByte(), 0x0c, 0x46, // push; mov r4,r1
        0x80.toByte(), 0x69,            // ldr r0,[r0,#0x18] (stream)
        0x83.toByte(), 0x6e,            // ldr r3,[r0,#0x68] (presentation)
        0x13, 0xb9.toByte(),            // cbnz r3
        0x6f, 0xf0.toByte(), 0x25, 0x00 // missing callback -> -ENOSYS
    )

    fun usesDirectTrackTail(elf: ByteArray): Boolean {
        if (elf.size < 52 || elf[0] != 0x7f.toByte() || elf[1] != 'E'.code.toByte() ||
            elf[2] != 'L'.code.toByte() || elf[3] != 'F'.code.toByte() ||
            elf[4] != 1.toByte() || elf[5] != 1.toByte() ||
            elf[18] != 40.toByte() || elf[19] != 0.toByte()) return false
        if (!String(elf, Charsets.ISO_8859_1).contains("AudioStreamOutSink")) return false
        return (0..elf.size - timestampPrefix.size).any { start ->
            timestampPrefix.indices.all { elf[start + it] == timestampPrefix[it] }
        }
    }
}
