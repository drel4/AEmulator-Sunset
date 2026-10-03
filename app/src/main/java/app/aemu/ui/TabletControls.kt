/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Explicit side layouts keep native trackball touch coordinates valid; no rotated AndroidView. */
@Composable
internal fun ControlStrip(turns: Int, modifier: Modifier, trackball: @Composable () -> Unit, buttons: @Composable () -> Unit) {
    when (Math.floorMod(turns, 4)) {
        1 -> Row(modifier, verticalAlignment = Alignment.CenterVertically) { trackball(); buttons() }
        3 -> Row(modifier, verticalAlignment = Alignment.CenterVertically) { buttons(); trackball() }
        2 -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { buttons(); trackball() }
        else -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { trackball(); buttons() }
    }
}

@Composable
internal fun ControlButtons(sideways: Boolean, content: @Composable () -> Unit) {
    if (sideways) Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).width(52.dp),
        verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(52.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { content() }
}
