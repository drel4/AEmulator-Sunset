/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.ui

import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.aemu.R
import app.aemu.core.GuestImage
import app.aemu.core.VmArchiveStorage
import app.aemu.core.VmSettings
import app.aemu.core.VmArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun VmExportSection(img: GuestImage, settings: VmSettings, busy: Boolean, setBusy: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var includeData by remember { mutableStateOf(false) }
    var includeSystem by remember { mutableStateOf(true) }
    var includeConfig by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf<GuestImage?>(null) }
    var pendingParts by remember { mutableStateOf(VmArchive.Selection()) }
    var message by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val snapshot = pending
        pending = null
        if (uri != null && snapshot != null) {
            setBusy(true)
            message = ctx.getString(R.string.vm_export_working)
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { VmArchiveStorage.export(ctx, snapshot, pendingParts, uri) {} }
                        .onFailure { runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) } }
                }
                setBusy(false)
                message = ctx.getString(if (result.isSuccess) R.string.vm_export_done else R.string.vm_export_failed)
            }
        }
    }
    TextButton(enabled = !busy && pending == null, modifier = Modifier.fillMaxWidth(), onClick = { confirm = true }) {
        Text(stringResource(R.string.vm_export))
    }
    message?.let { Text(it) }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.vm_export)) },
        text = { Column {
            Text(stringResource(R.string.vm_export_info))
            Toggle(stringResource(if (app.aemu.core.BootPartitionImport.present(app.aemu.core.VmPaths(ctx, img.id).dir))
                R.string.vm_export_system_boot else R.string.vm_export_system), stringResource(R.string.vm_export_system_info), includeSystem) { includeSystem = it }
            Toggle(stringResource(R.string.vm_export_config), stringResource(R.string.vm_export_config_info), includeConfig) { includeConfig = it }
            Toggle(stringResource(R.string.vm_export_data), stringResource(R.string.vm_export_data_info), includeData) { includeData = it }
        } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { TextButton(enabled = includeSystem || includeConfig || includeData, onClick = {
            confirm = false
            pending = (app.aemu.core.ImageStore.get(ctx, img.id) ?: img).copy(settings = settings)
            pendingParts = VmArchive.Selection(includeSystem, includeConfig, includeData)
            pick.launch(img.name.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").take(80).ifEmpty { "VM" } + ".aessvm")
        }) { Text(stringResource(R.string.vm_export)) } },
    )
}
