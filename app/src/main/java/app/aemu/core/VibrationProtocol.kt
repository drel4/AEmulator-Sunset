package app.aemu.core

/** Small fixed-frame protocol shared with the ARM legacy vibrator shim. */
object VibrationProtocol {
    const val FRAME_SIZE = 12
    const val MAX_PULSE_MS = 60_000L
    data class Command(val op: Int, val durationMs: Long)

    fun decode(frame: ByteArray): Command? {
        if (frame.size != FRAME_SIZE || frame[0] != 86.toByte() || frame[1] != 73.toByte() ||
            frame[2] != 66.toByte() || frame[3] != 49.toByte()) return null
        fun unsigned(offset: Int): Long = (0..3).fold(0L) { value, i ->
            value or ((frame[offset + i].toLong() and 255) shl (8 * i))
        }
        val op = unsigned(4)
        val duration = unsigned(8)
        if (op !in 0L..2L || (op != 1L && duration != 0L)) return null
        return Command(op.toInt(), duration.coerceAtMost(MAX_PULSE_MS))
    }
    fun reply(status: Int) = ByteArray(4) { (status ushr (it * 8)).toByte() }
}

/** Serialize requests with stop, so queued clients cannot vibrate a stopped VM. */
class VibrationController(
    private val enabled: () -> Boolean,
    private val supported: () -> Boolean,
    private val pulse: (Long) -> Unit,
    private val cancel: () -> Unit,
) {
    private var active = false
    @Synchronized fun start() { active = true }
    @Synchronized fun stop() { active = false; runCatching { cancel() } }
    @Synchronized fun request(frame: ByteArray): Int {
        val command = VibrationProtocol.decode(frame) ?: return -22
        if (!active) return if (command.op == 0) 0 else -19
        return try {
            val available = enabled() && supported()
            when (command.op) {
                0 -> if (available) 1 else 0
                1 -> { if (command.durationMs == 0L || !available) cancel() else pulse(command.durationMs); 0 }
                else -> { cancel(); 0 }
            }
        } catch (_: Exception) { -5 }
    }
}
