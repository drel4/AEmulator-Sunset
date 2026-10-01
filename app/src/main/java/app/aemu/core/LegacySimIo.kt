package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Minimal, read-only identity file for the existing synthetic SIM. No real SIM access. */
object LegacySimIo {
    const val ICCID = "89000000000000000004"
    data class Reply(val sw1: Int, val sw2: Int, val hex: String? = null)
    private val notFound = Reply(0x6a, 0x82)
    private val badArguments = Reply(0x6a, 0x86)

    fun handle(args: ByteBuffer): Reply = try {
        val b = args.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val command = b.int
        val file = b.int
        val length = b.int
        val path = if (length == -1) null else {
            require(length in 0..128)
            val byteCount = (length + 1) * 2
            val padded = (byteCount + 3) and -4
            require(b.remaining() >= padded + 12)
            val start = b.position()
            val value = String(CharArray(length) { b.char })
            require(b.char == '\u0000')
            b.position(start + padded)
            value
        }
        val p1 = b.int; val p2 = b.int; val p3 = b.int
        if (file != 0x2fe2 || (path != null && !path.equals("3F00", true))) notFound
        else if (p1 !in 0..127 || p2 !in 0..255 || p3 !in 0..255) badArguments
        else when (command) {
            0xc0 -> { // GET_RESPONSE: legacy transparent-EF header read by IccFileHandler
                if (p1 != 0 || p2 != 0 || p3 != 15) badArguments
                else Reply(0x90, 0, "0000000A2FE2040000000001020000")
            }
            0xb0 -> { // READ_BINARY; nibble-swapped decimal ICCID
                val data = ICCID.chunked(2).joinToString("") { "${it[1]}${it[0]}" }
                val offset = (p1 shl 8) or p2
                if (p3 == 0 || offset + p3 > 10) badArguments
                else Reply(0x90, 0, data.substring(offset * 2, (offset + p3) * 2))
            }
            else -> Reply(0x6d, 0) // updates/authentication/record reads are not emulated
        }
    } catch (_: Exception) { badArguments }
}
