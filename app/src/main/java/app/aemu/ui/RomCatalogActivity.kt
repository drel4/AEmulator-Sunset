package app.aemu.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.catalog.*
import kotlinx.coroutines.CancellationException

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
    LaunchedEffect(source, revision) {
        loading = true
        error = null
        try { catalog = CatalogLoader.load(source) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: e.javaClass.simpleName }
        finally { loading = false }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.catalog_title)) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            IconButton(onClick = { draft = source; edit = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.catalog_source)) }
            IconButton(onClick = { revision++ }, enabled = !loading) { Icon(Icons.Rounded.Refresh, stringResource(R.string.catalog_refresh)) }
        })
    }) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad),
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
                if (list.motd.isNotEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(list.motd, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    Text(stringResource(R.string.catalog_reported), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { openBrowser(ctx, source) }) { Text(stringResource(R.string.catalog_view_source)) }
                }
                if (list.warningLines.isNotEmpty()) item {
                    Text(stringResource(R.string.catalog_warnings, list.warningLines.take(10).joinToString(", ") +
                        if (list.warningLines.size > 10) "…" else ""),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (list.sections.isEmpty()) item { Text(stringResource(R.string.catalog_no_roms)) }
                list.sections.forEachIndexed { sectionIndex, section ->
                    item(key = "section-$sectionIndex") {
                        Column {
                            Text(section.name.ifEmpty { stringResource(R.string.catalog_roms) }, style = MaterialTheme.typography.titleLarge)
                            if (section.comment.isNotEmpty()) Text(section.comment, style = MaterialTheme.typography.bodyMedium)
                            section.source?.let { url -> TextButton(onClick = { openBrowser(ctx, url) }) {
                                Text(stringResource(R.string.catalog_section_source))
                            } }
                            if (section.roms.isEmpty()) Text(stringResource(R.string.catalog_no_roms),
                                Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    section.roms.forEachIndexed { romIndex, rom ->
                        item(key = "rom-$sectionIndex-$romIndex") { CatalogRomCard(rom) }
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
                source = next
                revision++
                edit = false
            }) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { edit = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun CatalogRomCard(rom: CatalogRom) {
    val ctx = LocalContext.current
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(rom.device, style = MaterialTheme.typography.titleMedium)
            Text(listOf("Android ${rom.android}", rom.skin).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(status), color = statusColor, style = MaterialTheme.typography.labelLarge)
            if (rom.comment.isNotEmpty()) Text(rom.comment, style = MaterialTheme.typography.bodyMedium)
            rom.url?.let { url -> TextButton(onClick = { openBrowser(ctx, url) }) { Text(stringResource(R.string.catalog_download)) } }
                ?: Text(stringResource(R.string.catalog_no_url), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
