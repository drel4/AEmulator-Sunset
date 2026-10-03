/* AEmulator Sunset addition, 2026-10-03. GPL-3.0; see LICENSE. */
package app.aemu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.aemu.R
import app.aemu.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun VmImportSection(img: GuestImage, busy: Boolean, setBusy: (Boolean) -> Unit, apply: (VmSettings) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bootPresent by remember { mutableStateOf(BootPartitionImport.present(VmPaths(ctx, img.id).dir)) }
    var warning by remember { mutableStateOf<VmArchiveStorage.SettingsImport?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    fun accept(source: VmArchiveStorage.SettingsImport) {
        apply(source.image.settings)
        message = ctx.getString(R.string.vm_import_settings_done)
    }
    val pickSettings = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        if (uri != null) {
            setBusy(true)
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching {
                    val source = VmArchiveStorage.readSettings(ctx, uri)
                    source to source.differs(ctx, ImageStore.get(ctx, img.id) ?: img)
                } }
                setBusy(false)
                result.onSuccess { (source, differs) -> if (differs) warning = source else accept(source) }
                    .onFailure { message = ctx.getString(R.string.vm_import_failed, it.message.orEmpty()) }
            }
        }
    }
    val pickBoot = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        if (uri != null) {
            setBusy(true)
            message = ctx.getString(R.string.vm_import_boot_working)
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { BootPartitionImport.install(ctx, img.id, uri) } }
                bootPresent = BootPartitionImport.present(VmPaths(ctx, img.id).dir)
                setBusy(false)
                message = result.fold({ ctx.getString(R.string.vm_import_boot_done) },
                    { ctx.getString(R.string.vm_import_failed, it.message.orEmpty()) })
            }
        }
    }
    TextButton(enabled = !busy && !picking, modifier = Modifier.fillMaxWidth(), onClick = {
        picking = true; pickSettings.launch(arrayOf("*/*"))
    }) { Text(stringResource(R.string.vm_import_settings)) }
    if (!bootPresent) TextButton(enabled = !busy && !picking, modifier = Modifier.fillMaxWidth(), onClick = {
        picking = true; pickBoot.launch(arrayOf("*/*"))
    }) { Text(stringResource(R.string.vm_import_boot)) }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    warning?.let { source -> AlertDialog(
        onDismissRequest = { warning = null },
        title = { Text(stringResource(R.string.vm_import_settings_warning_title)) },
        text = { Text(stringResource(R.string.vm_import_settings_warning,
            "${source.image.brand} ${source.image.model} / ${source.image.release} / ${source.image.skin}",
            "${img.brand} ${img.model} / ${img.release} / ${img.skin}")) },
        dismissButton = { TextButton(onClick = { warning = null }) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { TextButton(onClick = { warning = null; accept(source) }) { Text(stringResource(R.string.vm_import_settings)) } },
    ) }
}
