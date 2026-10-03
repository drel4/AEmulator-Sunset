/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/** Measure the original short-edge strip, then rotate its placement, not its width. */
@Composable
internal fun NaturalControls(turns: Int, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { children, constraints ->
        val sideways = turns % 2 != 0
        val span = if (sideways) constraints.maxHeight else constraints.maxWidth
        val limit = if (sideways) constraints.maxWidth else constraints.maxHeight
        val child = children.single().measure(Constraints(minWidth = span, maxWidth = span, maxHeight = limit))
        val w = if (sideways) child.height else child.width
        val h = if (sideways) child.width else child.height
        layout(w, h) {
            child.placeWithLayer((w - child.width) / 2, (h - child.height) / 2) { rotationZ = -turns * 90f }
        }
    }
}
