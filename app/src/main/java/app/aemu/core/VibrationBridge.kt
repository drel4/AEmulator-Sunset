package app.aemu.core

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.DataInputStream
import java.io.File

/** Guest patterns stay in VibratorService; each on/off reaches the host motor. */
class VibrationBridge(ctx: Context, paths: VmPaths, enabled: () -> Boolean, private val log: (String) -> Unit) {
    private val vibrator = ctx.getSystemService(Vibrator::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val controller = VibrationController(enabled,
        { vibrator?.hasVibrator() == true },
        { duration -> vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE), attributes) },
        { vibrator?.cancel() })
    private val server = UnixServer(File(paths.root, "dev/aemu_vibrator"), "vibration") { client ->
        client.soTimeout = 500
        val frame = ByteArray(VibrationProtocol.FRAME_SIZE)
        DataInputStream(client.inputStream).readFully(frame)
        client.outputStream.write(VibrationProtocol.reply(controller.request(frame)))
        client.outputStream.flush()
    }
    fun serve(): Boolean {
        controller.start()
        val ok = server.start(log)
        if (!ok) controller.stop()
        log("vibration: bridge ${if (ok) "ready" else "unavailable"}; host motor ${runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)}")
        return ok
    }
    fun stop() { controller.stop(); server.stop() }
}
