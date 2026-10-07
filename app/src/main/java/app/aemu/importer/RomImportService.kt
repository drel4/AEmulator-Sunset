/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.importer

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.ui.ImportState
import app.aemu.ui.MainActivity
import kotlinx.coroutines.*

/** User-started dataSync foreground work: download and local import, not a guest VM service. */
class RomImportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var localized: Context
    private var taskId = 0L
    private var lastStartId = 0
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        localized = AppPrefs.wrap(this)
        taskId = RomImportTask.state.value.taskId
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, localized.getString(R.string.rom_notification_channel), NotificationManager.IMPORTANCE_LOW))
        try {
            val initial = notification(RomImportTask.state.value)
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(ID, initial)
            wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RomImport").apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L)
            }
        } catch (error: Exception) {
            RomImportTask.failStartup(error)
            stopSelf()
            return
        }
        scope.launch {
            var previous: ImportState? = null
            while (isActive) {
                val state = RomImportTask.state.value
                if (state != previous && state.file.isNotEmpty()) {
                    runCatching { manager.notify(ID, notification(state)) }
                    previous = state
                }
                if (!state.active) {
                    stopForeground(if (state.file.isEmpty()) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
                    stopSelfResult(lastStartId)
                    break
                }
                delay(500) // Bound notification updates even on fast downloads/extraction.
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (intent?.action == CANCEL) {
            RomImportTask.cancel(intent.getLongExtra("taskId", -1))
            if (!RomImportTask.state.value.active) stopSelf()
        } else if (RomImportTask.state.value.active) {
            taskId = RomImportTask.state.value.taskId
            RomImportTask.begin(this)
        }
        else stopSelf()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) { RomImportTask.cancel(taskId); stopSelf() }

    override fun onDestroy() {
        RomImportTask.cancel(taskId)
        scope.cancel()
        runCatching { wakeLock?.let { if (it.isHeld) it.release() } }
        super.onDestroy()
    }

    private fun notification(state: ImportState): Notification {
        val open = PendingIntent.getActivity(this, 20, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = localized.getString(when {
            state.error != null -> R.string.import_failed
            state.done != null -> R.string.import_done
            state.downloading -> R.string.rom_notification_downloading
            else -> R.string.import_running
        })
        val builder = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat_vm)
            .setContentTitle("$title · ${state.file.take(160)}").setContentText((state.error ?: state.step).take(1024))
            .setContentIntent(open).setOngoing(state.active).setAutoCancel(!state.active).setOnlyAlertOnce(true)
        if (state.active) {
            val percent = TransferProgress.percent(state.progress)
            builder.setProgress(100, percent ?: 0, percent == null)
            percent?.let { builder.setSubText("$it%") }
            val cancel = PendingIntent.getService(this, 21, Intent(this, RomImportService::class.java).setAction(CANCEL)
                .setData(android.net.Uri.parse("aemu-import-cancel:${state.taskId}")).putExtra("taskId", state.taskId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, localized.getString(R.string.cancel), cancel).build())
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL = "rom-import"
        private const val ID = 20
        private const val CANCEL = "app.aemu.CANCEL_ROM_IMPORT"
    }
}
