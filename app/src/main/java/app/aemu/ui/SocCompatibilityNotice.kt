/* AEmulator Sunset addition, 2026-10-04. GPL-3.0; see LICENSE. */
package app.aemu.ui

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.core.SocCompatibility

@Composable
internal fun SocCompatibilityNotice() {
    val ctx = LocalContext.current
    val prefs = remember { AppPrefs.prefs(ctx) }
    val chip = remember { if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "" }
    val hardware = remember { Build.HARDWARE }
    val unverified = remember { SocCompatibility.family(chip, hardware) == SocCompatibility.Family.UNVERIFIED }
    var show by remember { mutableStateOf(!prefs.getBoolean("soc_notice_checked", false) && unverified) }
    LaunchedEffect(Unit) {
        if (!unverified) prefs.edit().putBoolean("soc_notice_checked", true).apply()
    }
    fun close() { prefs.edit().putBoolean("soc_notice_checked", true).apply(); show = false }
    if (show) AlertDialog(
        onDismissRequest = ::close,
        title = { Text(stringResource(R.string.soc_warning_title)) },
        text = { Text(stringResource(R.string.soc_warning,
            listOf(chip, hardware).filter { it.isNotBlank() && it != Build.UNKNOWN }.distinct().joinToString(" / ")
                .ifBlank { ctx.getString(R.string.soc_unknown) })) },
        confirmButton = { TextButton(onClick = ::close) { Text(stringResource(R.string.soc_warning_ok)) } },
    )
}
