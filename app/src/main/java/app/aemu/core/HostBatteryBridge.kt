/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import java.io.File

/** Independent of VM window visibility. Binder calls never run on the UI or boot thread. */
internal class HostBatteryBridge(private val ctx: Context, private val root: File,
    private val api: Int, defaultEnabled: Boolean, private val ready: () -> Boolean, private val generation: () -> Int,
    private val run: (String) -> Unit, private val log: (String) -> Unit) {
    @Volatile var following = defaultEnabled
        private set
    @Volatile var latest: HostBatteryState? = null
        private set
    @Volatile private var active = false
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var registered = false
    private var lastWritten: HostBatteryState? = null
    private var lastApplied: Pair<HostBatteryState, Int>? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { read(intent) }
    }
    private fun read(intent: Intent?) {
        if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
        latest = HostBatteryState.from(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1), intent.getIntExtra(BatteryManager.EXTRA_STATUS, 1),
            intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
    }
    fun start() {
        if (active) return
        val sticky = runCatching {
            ContextCompat.registerReceiver(ctx, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        }.getOrElse { log("battery: host receiver unavailable: ${it.message}"); return }
        read(sticky)
        registered = true
        val initial = if (following) latest else HostBatteryState.from(100, 100, 5, 1)
        initial?.let { runCatching { it.writeSysfs(root); lastWritten = it }.onFailure { log("battery: ${it.message}") } }
        active = true
        worker = HandlerThread("host-battery").also { it.start() }
        handler = Handler(worker!!.looper)
        handler!!.post(tick)
        log("battery: host following=${following}, initial=${latest?.percent}")
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            if (following) latest?.let { value ->
                runCatching {
                    if (lastWritten != value) { value.writeSysfs(root); lastWritten = value }
                    // Gingerbread/ICS lack the dumpsys override; retain their sysfs path.
                    // Reapply after changes/restarts, not five binder calls on every poll.
                    val key = value to generation()
                    if (active && following && api >= 17 && ready() && lastApplied != key) {
                        run(value.command(api)); lastApplied = key
                    }
                }.onFailure { log("battery: ${it.message}") }
            }
            if (active) handler?.postDelayed(this, 15_000)
        }
    }
    fun applyManual(percent: Int, charging: Boolean) {
        following = false
        val value = HostBatteryState.from(percent, 100, if (charging) 2 else 3, if (charging) 1 else 0)!!
        handler?.post {
            if (active) runCatching {
                value.writeSysfs(root); lastWritten = value
                if (api >= 17 && ready()) run(value.command(api))
            }.onFailure { log("battery: ${it.message}") }
        }
    }
    fun reset(defaultEnabled: Boolean) {
        following = defaultEnabled
        handler?.post {
            if (active && !following) runCatching { HostBatteryState.from(100, 100, 5, 1)!!.writeSysfs(root) }
            if (active && ready() && api >= 17) runCatching { run("dumpsys battery reset") }
            if (active) { lastWritten = null; lastApplied = null; handler?.removeCallbacks(tick); handler?.post(tick) }
        }
    }
    fun stop() {
        active = false
        if (registered) { runCatching { ctx.unregisterReceiver(receiver) }; registered = false }
        handler?.removeCallbacksAndMessages(null)
        worker?.quitSafely()
        // GuestRunner's command timeout plus reader join are bounded to six seconds.
        worker?.join(7_000)
        worker = null; handler = null
    }
}
