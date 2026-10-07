/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.ui

import androidx.compose.material3.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.catalog.StarterRom

@Composable
internal fun RomImportWelcome(model: LibraryModel, otherNoticeVisible: Boolean) {
    val ctx = LocalContext.current
    val prefs = remember { AppPrefs.prefs(ctx) }
    val loaded by model.loaded.collectAsState()
    val images by model.images.collectAsState()
    val state by model.import.collectAsState()
    val request by model.downloadRequest.collectAsState()
    var welcome by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loaded, images.isNotEmpty()) {
        if (loaded && !prefs.getBoolean("starter_rom_offered", false)) {
            // Existing libraries should not receive a first-use prompt after upgrading.
            if (images.isNotEmpty()) { prefs.edit().putBoolean("starter_rom_offered", true).apply(); welcome = false }
            else welcome = true
        }
    }
    fun dismissWelcome() { prefs.edit().putBoolean("starter_rom_offered", true).apply(); welcome = false }
    if (!otherNoticeVisible && !state.active && request == null && welcome) AlertDialog(
        onDismissRequest = ::dismissWelcome,
        title = { Text(stringResource(R.string.starter_title)) },
        text = { Text(stringResource(R.string.starter_text), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { dismissWelcome(); model.download(StarterRom.request) }) {
            Text(stringResource(R.string.rom_download_import))
        } },
        dismissButton = { Column {
            TextButton(onClick = { dismissWelcome(); openBrowser(ctx, StarterRom.request.url) }) { Text(stringResource(R.string.rom_open_browser)) }
            TextButton(onClick = ::dismissWelcome) { Text(stringResource(R.string.starter_skip)) }
        } })
    if (!otherNoticeVisible && !state.active) request?.let { rom -> AlertDialog(
        onDismissRequest = { model.downloadRequest.value = null },
        title = { Text(stringResource(R.string.rom_import_title, rom.title)) },
        text = { Text(stringResource(R.string.rom_import_consent, rom.url), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = {
            model.downloadRequest.value = null
            dismissWelcome()
            model.download(rom)
        }) { Text(stringResource(R.string.rom_download_import)) } },
        dismissButton = { Column {
            TextButton(onClick = { model.downloadRequest.value = null; openBrowser(ctx, rom.url) }) { Text(stringResource(R.string.rom_open_browser)) }
            TextButton(onClick = { model.downloadRequest.value = null }) { Text(stringResource(R.string.cancel)) }
        } }) }
}
