/* AEmulator Sunset addition, 2026-10-07. GPL-3.0. */
package app.aemu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.RomCardNote

@Composable
internal fun RomCardNoteView(text: String) {
    val note = RomCardNote.normalize(text)
    if (note.isEmpty()) return
    var expanded by remember(note) { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(
            onClickLabel = stringResource(R.string.vm_card_note_open)) { expanded = true }) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.vm_rom_note), style = MaterialTheme.typography.labelMedium)
            Text(note, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false },
        title = { Text(stringResource(R.string.vm_rom_note)) },
        text = { Text(note, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.close)) } })
}
