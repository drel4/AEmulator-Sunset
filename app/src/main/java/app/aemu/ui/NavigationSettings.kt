package app.aemu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.NavButton
import app.aemu.core.NavControls
import app.aemu.core.VmSettings

internal fun NavButton.labelRes(): Int = when (this) {
    NavButton.BACK -> R.string.back
    NavButton.HOME -> R.string.home
    NavButton.RECENTS -> R.string.recents
    NavButton.MENU -> R.string.menu
    NavButton.SEARCH -> R.string.nav_search
    NavButton.POWER -> R.string.nav_power
    NavButton.VOLUME_DOWN -> R.string.nav_volume_down
    NavButton.VOLUME_UP -> R.string.nav_volume_up
    NavButton.UP -> R.string.nav_up
    NavButton.DOWN -> R.string.nav_down
    NavButton.LEFT -> R.string.nav_left
    NavButton.RIGHT -> R.string.nav_right
    NavButton.CENTER -> R.string.nav_center
}

@Composable
internal fun NavigationSettings(s: VmSettings, set: (VmSettings) -> Unit) {
    val buttons = NavControls.parse(s.navButtons)
    if (s.showNavBar) {
        Text(stringResource(R.string.nav_buttons), modifier = Modifier.padding(horizontal = 16.dp))
        NavButton.entries.forEach { button ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(button in buttons, { on ->
                    set(s.copy(navButtons = NavControls.encode(if (on) buttons + button else buttons - button)))
                })
                Text(stringResource(button.labelRes()), modifier = Modifier.weight(1f))
            }
        }
        Text(stringResource(R.string.nav_order), modifier = Modifier.padding(horizontal = 16.dp))
        buttons.forEachIndexed { index, button ->
            fun move(to: Int) {
                val order = buttons.toMutableList()
                order.removeAt(index); order.add(to, button)
                set(s.copy(navButtons = NavControls.encode(order)))
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}. ${stringResource(button.labelRes())}", Modifier.weight(1f))
                TextButton({ move(index - 1) }, enabled = index > 0) { Text(stringResource(R.string.nav_earlier)) }
                TextButton({ move(index + 1) }, enabled = index < buttons.lastIndex) { Text(stringResource(R.string.nav_later)) }
            }
        }
        TextButton({ set(s.copy(navButtons = NavControls.DEFAULT_BUTTONS)) }) { Text(stringResource(R.string.nav_reset)) }
    }
    Toggle(stringResource(R.string.nav_trackball), stringResource(R.string.nav_trackball_sub), s.trackball) { set(s.copy(trackball = it)) }
    if (s.trackball) {
        Toggle(stringResource(R.string.nav_dpad_mode), stringResource(R.string.nav_dpad_sub), s.trackballDpad) { set(s.copy(trackballDpad = it)) }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.nav_sensitivity, s.trackballStepDp))
            Slider(s.trackballStepDp.toFloat(), { set(s.copy(trackballStepDp = it.toInt().coerceIn(4, 48))) }, valueRange = 4f..48f)
        }
    }
}
