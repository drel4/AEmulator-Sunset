package app.aemu.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.catalog.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RomCatalogActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppPrefs.wrap(base))
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { CatalogScreen(::finish) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var source by rememberSaveable { mutableStateOf(AppPrefs.catalogUrl(ctx)) }
    var revision by remember { mutableIntStateOf(0) }
    var catalog by remember { mutableStateOf<RomCatalog?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(source) }
    var selectedSection by rememberSaveable(source) { mutableIntStateOf(-1) }
    val section = catalog?.sections?.getOrNull(selectedSection)
    BackHandler(enabled = selectedSection >= 0) { selectedSection = -1 }
    LaunchedEffect(source, revision) {
        loading = true
        error = null
        try {
            val loaded = CatalogLoader.load(source)
            catalog = loaded
            if (selectedSection !in loaded.sections.indices) selectedSection = -1
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: e.javaClass.simpleName }
        finally { loading = false }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(section?.name?.ifEmpty { stringResource(R.string.catalog_roms) }
            ?: stringResource(R.string.catalog_title)) }, navigationIcon = {
            IconButton(onClick = { if (selectedSection >= 0) selectedSection = -1 else onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(
                    if (selectedSection >= 0) R.string.catalog_back_sections else R.string.back))
            }
        }, actions = {
            IconButton(onClick = { draft = source; edit = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.catalog_source)) }
            IconButton(onClick = { selectedSection = -1; revision++ }, enabled = !loading) { Icon(Icons.Rounded.Refresh, stringResource(R.string.catalog_refresh)) }
        })
    }) { pad ->
        key(source, selectedSection) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), state = rememberLazyListState(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { problem -> item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.catalog_load_failed, problem), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { revision++ }, enabled = !loading) { Text(stringResource(R.string.catalog_retry)) }
                    }
                }
            } }
            catalog?.let { list ->
                if (selectedSection < 0 && list.motd.isNotEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(list.motd, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (selectedSection < 0) item {
                    Text(stringResource(R.string.catalog_reported), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (list.warningLines.isNotEmpty()) item {
                    Text(stringResource(R.string.catalog_warnings, list.warningLines.take(10).joinToString(", ") +
                        if (list.warningLines.size > 10) "…" else ""),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (list.sections.isEmpty()) item { Text(stringResource(R.string.catalog_no_roms)) }
                if (section == null) list.sections.forEachIndexed { sectionIndex, entry ->
                    item(key = "section-$sectionIndex") {
                        Card(Modifier.fillMaxWidth()) {
                            ListItem(modifier = Modifier.clickable { selectedSection = sectionIndex },
                                headlineContent = { Text(entry.name.ifEmpty { stringResource(R.string.catalog_roms) },
                                    style = MaterialTheme.typography.titleMedium) },
                                supportingContent = {
                                    Column {
                                        Text(if (entry.roms.isEmpty()) stringResource(R.string.catalog_no_roms)
                                            else pluralStringResource(R.plurals.catalog_rom_count, entry.roms.size, entry.roms.size))
                                        if (entry.comment.isNotEmpty()) Text(entry.comment, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) })
                        }
                    }
                } else {
                    item(key = "section-info") {
                        Column {
                            if (section.comment.isNotEmpty()) Text(section.comment, style = MaterialTheme.typography.bodyMedium)
                            section.source?.let { url -> TextButton(onClick = { openBrowser(ctx, url) }) {
                                Text(stringResource(R.string.catalog_section_source))
                            } }
                            if (section.roms.isEmpty()) Text(stringResource(R.string.catalog_no_roms),
                                Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    section.roms.forEachIndexed { romIndex, rom ->
                        item(key = "rom-$romIndex") { CatalogRomCard(rom) }
                    }
                }
            }
            }
        }
    }
    if (edit) AlertDialog(onDismissRequest = { edit = false }, title = { Text(stringResource(R.string.catalog_source)) },
        text = { Column {
            OutlinedTextField(draft, { draft = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.catalog_url)) }, isError = CatalogUrls.valid(draft) == null,
                supportingText = { Text(stringResource(R.string.catalog_url_hint)) })
            TextButton(onClick = { draft = CatalogUrls.DEFAULT }) { Text(stringResource(R.string.catalog_reset_source)) }
        } }, confirmButton = {
            TextButton(enabled = CatalogUrls.valid(draft) != null, onClick = {
                val next = CatalogUrls.valid(draft) ?: return@TextButton
                AppPrefs.setCatalogUrl(ctx, next)
                if (next != source) catalog = null
                selectedSection = -1
                source = next
                revision++
                edit = false
            }) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { edit = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun CatalogRomCard(rom: CatalogRom) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember(rom.url) { mutableStateOf(false) }
    val status = when (rom.status) {
        0 -> R.string.catalog_status_no_boot
        1 -> R.string.catalog_status_issues
        2 -> R.string.catalog_status_works
        else -> R.string.catalog_status_unknown
    }
    val statusColor = when (rom.status) {
        0 -> MaterialTheme.colorScheme.error
        1 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(rom.device, style = MaterialTheme.typography.titleMedium)
                Text(listOf("Android ${rom.android}", rom.skin).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(status), color = statusColor, style = MaterialTheme.typography.labelLarge)
                if (rom.comment.isNotEmpty()) Text(rom.comment, style = MaterialTheme.typography.bodyMedium)
                if (rom.url == null) Text(stringResource(R.string.catalog_no_url), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rom.url?.let { url -> IconButton(enabled = !checking, onClick = {
                checking = true
                scope.launch {
                    try {
                        val direct = withContext(Dispatchers.IO) { RomLink.isDirect(url) }
                        if (!direct) openBrowser(ctx, url)
                        else {
                            ctx.startActivity(android.content.Intent(ctx, MainActivity::class.java)
                                .setAction(ACTION_DOWNLOAD_ROM).putExtra("url", url).putExtra("title", rom.device)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
                            (ctx as? android.app.Activity)?.finish()
                        }
                    } finally { checking = false }
                }
            }) {
                if (checking) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Download, stringResource(R.string.catalog_download_rom, rom.device))
            } }
        }
    }
}
