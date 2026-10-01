package app.aemu.ui

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.core.ExperimentalUnlock

@Composable
fun experimentalFeaturesEnabled(): Boolean {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(AppPrefs.experimental(ctx)) }
    DisposableEffect(ctx) {
        val prefs = AppPrefs.prefs(ctx)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "experimental") enabled = AppPrefs.experimental(ctx)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

@Composable
fun SunsetTitle(style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified) {
    val ctx = LocalContext.current
    Text("AEmulator Sunset", style = style, color = color, modifier = Modifier.clickable {
        if (AppPrefs.registerExperimentalTap(ctx)) {
            Toast.makeText(ctx, R.string.experimental_activated, Toast.LENGTH_LONG).show()
            openBrowser(ctx, ExperimentalUnlock.VIDEO)
        }
    })
}
