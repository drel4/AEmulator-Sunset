/* AEmulator Sunset addition, 2026-10-07: service-owned imports. GPL-3.0. */
package app.aemu.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.catalog.RomDownload
import app.aemu.catalog.RomDownloadException
import app.aemu.catalog.RomDownloadRequest
import app.aemu.core.ImageStore
import app.aemu.ui.ImportState
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** One app-process task, independent of Activity/ViewModel lifetime. Never auto-restarts after death. */
internal object RomImportTask {
    val state = MutableStateFlow(ImportState())
    private data class Input(val uri: Uri?, val title: String, val request: RomDownloadRequest?)
    private var pending: Input? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var importer: Importer? = null
    @Volatile private var cancelled = false
    private var serial = 0L

    @Synchronized fun enqueue(ctx: Context, uri: Uri?, title: String, request: RomDownloadRequest?) {
        if (state.value.active) return
        cancelled = false
        pending = Input(uri, title, request)
        state.value = ImportState(active = true, file = title, step = ctx.getString(R.string.import_running), downloading = request != null, taskId = ++serial)
        try { ctx.startForegroundService(Intent(ctx, RomImportService::class.java)) }
        catch (error: Exception) { failStartup(error) }
    }

    @Synchronized fun failStartup(error: Exception) {
        pending = null
        state.value = state.value.copy(active = false, error = error.message ?: error.toString())
    }

    @Synchronized fun begin(ctx: Context) {
        val input = pending ?: return
        pending = null
        scope.launch { run(AppPrefs.wrap(ctx.applicationContext), input) }
    }

    fun cancel() { cancelled = true; importer?.cancelled = true }
    fun cancel(taskId: Long) { val current = state.value; if (TransferProgress.canCancel(current.active, current.taskId, taskId)) cancel() }
    fun dismiss() { if (!state.value.active) state.value = ImportState() }

    private fun run(ctx: Context, input: Input) {
        var downloaded: File? = null
        var terminal: ImportState? = null
        val log = ArrayList<String>()
        try {
            val imp = Importer(ctx,
                onProgress = { step, p -> state.value = state.value.copy(step = step, progress = p) },
                log = { line -> log.add(line); state.value = state.value.copy(log = log.takeLast(50)) })
            importer = imp
            var name = input.title
            val source = if (input.request != null) {
                downloaded = RomDownload.download(input.request, File(ctx.cacheDir, "rom-downloads"), { cancelled }) { count, total ->
                    val size = android.text.format.Formatter.formatShortFileSize(ctx, count)
                    val step = if (total > 0) ctx.getString(R.string.rom_downloading_total, size,
                        android.text.format.Formatter.formatShortFileSize(ctx, total)) else ctx.getString(R.string.rom_downloading, size)
                    state.value = state.value.copy(step = step, downloadedBytes = count, totalBytes = total,
                        progress = if (total > 0) count.toFloat() / total else -1f)
                }
                name = downloaded!!.name
                Uri.fromFile(downloaded)
            } else requireNotNull(input.uri)
            if (cancelled) imp.cancelled = true
            state.value = state.value.copy(step = ctx.getString(R.string.import_running), progress = -1f, downloading = false)
            val raw = imp.import(source, name)
            val defaults = AppPrefs.defaults(ctx)
            val image = if (name.endsWith(".aessvm", true)) raw else raw.copy(
                name = input.request?.title?.trim()?.take(128)?.takeIf { it.isNotEmpty() } ?: raw.name,
                settings = raw.settings.copy(gpu = defaults.gpu, jit = defaults.jit, netProxy = defaults.netProxy,
                    showNavBar = defaults.showNavBar, navButtons = defaults.navButtons, trackball = defaults.trackball,
                    trackballDpad = defaults.trackballDpad, trackballStepDp = defaults.trackballStepDp, keepScreenOn = defaults.keepScreenOn))
            if (image != raw) ImageStore.save(ctx, image)
            terminal = state.value.copy(active = false, done = image, step = ctx.getString(R.string.import_done), progress = 1f)
        } catch (error: Throwable) {
            val message = if (cancelled) ctx.getString(R.string.rom_cancelled) else if (error is RomDownloadException) ctx.getString(when (error.reason) {
                RomDownloadException.Reason.WEB_PAGE -> R.string.rom_web_page
                RomDownloadException.Reason.SPACE -> R.string.rom_no_space
                RomDownloadException.Reason.INCOMPLETE -> R.string.rom_incomplete
                RomDownloadException.Reason.CHECKSUM -> R.string.rom_checksum
                RomDownloadException.Reason.CANCELLED -> R.string.rom_cancelled
            }) else error.message ?: error.toString()
            terminal = state.value.copy(active = false, error = message, browserUrl = input.request?.url)
        } finally {
            downloaded?.delete()
            importer = null
            terminal?.let { state.value = it } // Only admit a new task after all cleanup is complete.
        }
    }
}
