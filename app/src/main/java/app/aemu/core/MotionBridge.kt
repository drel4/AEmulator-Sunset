/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import java.io.DataInputStream
import java.io.File

/** Real natural-device axes when enabled; explicit synthetic gravity for manual rotation otherwise. */
internal class MotionBridge(ctx: Context, paths: VmPaths, private val enabled: () -> Boolean,
    private val hostSensors: () -> Boolean, private val log: (String) -> Unit) : SensorEventListener {
    private val manager = ctx.getSystemService(SensorManager::class.java)
    private val sensors = MotionProtocol.types.map { manager?.getDefaultSensor(it) }
    private val hostAvailable = sensors.mapIndexed { i, s -> if (s != null) 1 shl i else 0 }.fold(0, Int::or)
    private val available get() = if (hostSensors()) hostAvailable else 1
    private var manualTurns = 0
    private val latest = arrayOfNulls<MotionProtocol.Sample>(3)
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var started = false
    private var visible = false
    private var desired = 0
    private var registered = 0
    private val server = UnixServer(File(paths.root, "dev/aemu_sensors"), "motion") { client ->
        client.soTimeout = 500
        val bytes = ByteArray(MotionProtocol.REQUEST_SIZE)
        DataInputStream(client.inputStream).readFully(bytes)
        val request = MotionProtocol.request(bytes) ?: return@UnixServer
        val reply = synchronized(this) {
            if (request.info) MotionProtocol.info(if (enabled() && started) available else 0)
            else {
                desired = request.mask and available
                handler?.post { updateRegistration() }
                MotionProtocol.samples(if (!visible || !started || !enabled()) emptyList() else if (!hostSensors())
                    if (desired and 1 != 0) listOf(MotionProtocol.Sample(1, android.os.SystemClock.elapsedRealtimeNanos(), ManualMotion.gravity(manualTurns), 3)) else emptyList()
                else latest.mapIndexedNotNull { i, sample -> sample?.takeIf { desired and (1 shl i) != 0 } })
            }
        }
        client.outputStream.write(reply); client.outputStream.flush()
    }
    @Synchronized fun serve() {
        if (started || !enabled()) return
        worker = HandlerThread("motion-sensors").also { it.start() }
        handler = Handler(worker!!.looper)
        started = true
        if (!server.start(log)) { stop(); return }
        log("motion: ${if (hostSensors()) "host" else "manual gravity"} bridge ready, sensor mask=$available, capped at 50 Hz")
    }
    @Synchronized fun simulateRotation(): Boolean {
        if (!started || !enabled() || hostSensors()) return false
        manualTurns = (manualTurns + 1) % 4
        log("motion: simulated phone rotation=$manualTurns (guest auto-rotate policy applies)")
        return true
    }
    @Synchronized fun visible(value: Boolean) {
        visible = value
        if (!value) latest.fill(null)
        handler?.post { updateRegistration() }
    }
    @Synchronized private fun updateRegistration() {
        val target = if (started && visible && enabled() && hostSensors()) desired else 0
        if (target == registered) return
        manager?.unregisterListener(this)
        latest.fill(null)
        registered = 0
        sensors.forEachIndexed { i, sensor ->
            if (sensor != null && target and (1 shl i) != 0 &&
                manager?.registerListener(this, sensor, 20_000, handler) == true) registered = registered or (1 shl i)
        }
    }
    @Synchronized override fun onSensorChanged(event: SensorEvent) {
        val i = MotionProtocol.types.indexOf(event.sensor.type)
        if (!started || !visible || i < 0 || registered and (1 shl i) == 0 || event.timestamp <= 0 ||
            event.values.size < 3 || event.values.take(3).any { !it.isFinite() }) return
        latest[i] = MotionProtocol.Sample(event.sensor.type, event.timestamp, event.values.copyOf(3), event.accuracy)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    @Synchronized fun stop() {
        started = false; desired = 0; registered = 0
        manager?.unregisterListener(this)
        latest.fill(null)
        server.stop()
        worker?.quitSafely(); worker = null; handler = null
    }
}
