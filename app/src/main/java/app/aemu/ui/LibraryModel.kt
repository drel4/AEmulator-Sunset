/* Modified for AEmulator Sunset, 2026-10-03: preserve restored VM settings.
 * GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aemu.core.GuestImage
import app.aemu.core.ImageStore
import app.aemu.core.VmSettings
import app.aemu.importer.RomImportTask
import app.aemu.catalog.RomDownloadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportState(
    val active: Boolean = false,
    val file: String = "",
    val step: String = "",
    val progress: Float = -1f,
    val log: List<String> = emptyList(),
    val error: String? = null,
    val done: GuestImage? = null,
    val browserUrl: String? = null,
    val downloading: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = -1,
    val taskId: Long = 0,
)

class LibraryModel(app: Application) : AndroidViewModel(app) {
    private val _images = MutableStateFlow<List<GuestImage>>(emptyList())
    val images: StateFlow<List<GuestImage>> = _images
    private val _import = RomImportTask.state
    val import: StateFlow<ImportState> = _import
    val loaded = MutableStateFlow(false)
    val downloadRequest = MutableStateFlow<RomDownloadRequest?>(null)

    fun requestDownload(request: RomDownloadRequest) { if (!_import.value.active) downloadRequest.value = request }
    fun download(request: RomDownloadRequest) { startImport(null, request.title, request) }

    init {
        refresh()
        viewModelScope.launch { _import.collect { if (!it.active) refresh() } }
    }

    fun refresh() {
        viewModelScope.launch {
            _images.value = withContext(Dispatchers.IO) { ImageStore.list(getApplication()) }
            loaded.value = true
        }
    }

    fun import(uri: Uri) {
        if (_import.value.active) return
        val ctx = getApplication<Application>()
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "firmware"
        startImport(uri, name, null)
    }

    private fun startImport(uri: Uri?, displayName: String, request: RomDownloadRequest?) {
        RomImportTask.enqueue(app.aemu.AppPrefs.wrap(getApplication<Application>()), uri, displayName, request)
    }

    fun clone(src: GuestImage, name: String, copyData: Boolean) {
        if (_import.value.active) return
        val ctx = getApplication<Application>()
        _import.value = ImportState(active = true, file = name, step = "Copying")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val img = ImageStore.clone(ctx, src, name, copyData) { s -> _import.value = _import.value.copy(step = "Copying $s") }
                _import.value = _import.value.copy(active = false, done = img, step = "Done", progress = 1f)
            } catch (t: Throwable) {
                _import.value = _import.value.copy(active = false, error = t.message ?: t.toString())
            } finally { refresh() }
        }
    }

    fun cancelImport() = RomImportTask.cancel()
    fun dismissImport() = RomImportTask.dismiss()

    fun delete(img: GuestImage) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageStore.delete(getApplication(), img.id)
            refresh()
        }
    }

    fun rename(img: GuestImage, name: String) = save(img.copy(name = name))

    fun updateSettings(img: GuestImage, s: VmSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = ImageStore.get(getApplication(), img.id) ?: return@launch
            ImageStore.save(getApplication(), current.copy(settings = s))
            refresh()
        }
    }

    private fun save(img: GuestImage) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageStore.save(getApplication(), img)
            refresh()
        }
    }
}
