/* Modified for AEmulator Sunset through 2026-10-03: rounded highlights, icon rotation,
 * held guest keys. GPL-3.0; original attribution is preserved in NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import app.aemu.core.NavButton
import app.aemu.core.TrackballMotion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon

@Composable
internal fun HoloNavButton(button: NavButton, label: String, onClick: () -> Unit, onDown: () -> Unit, onUp: () -> Unit, iconRotation: Float = 0f, original: Boolean = false) {
    val hostView = androidx.compose.ui.platform.LocalView.current
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    var pressed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { up() } }
    Box(Modifier.size(48.dp).background(if (pressed) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        RoundedCornerShape(8.dp)).semantics {
        contentDescription = label
        role = Role.Button
        // Accessibility activation remains one tap, without duplicating physical touches.
        onClick { hostView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); onClick(); true }
    }.onKeyEvent { event ->
        val native = event.nativeKeyEvent
        if (native.keyCode !in listOf(android.view.KeyEvent.KEYCODE_ENTER,
                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER, android.view.KeyEvent.KEYCODE_SPACE,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER)) false
        else when (native.action) {
            android.view.KeyEvent.ACTION_DOWN -> {
                if (native.repeatCount == 0) { pressed = true; hostView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); down() }
                true
            }
            android.view.KeyEvent.ACTION_UP -> { pressed = false; up(); true }
            else -> false
        }
    }.onFocusChanged { if (!it.hasFocus && pressed) { pressed = false; up() } }
        .focusable().pointerInput(button) {
        detectTapGestures(onPress = {
            val release = up
            pressed = true
            hostView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            down()
            try { tryAwaitRelease() } finally { pressed = false; release() }
        })
    }, contentAlignment = Alignment.Center) {
        if (original) Icon(when (button) {
            NavButton.BACK -> Icons.AutoMirrored.Rounded.ArrowBack
            NavButton.HOME -> Icons.Rounded.Circle
            NavButton.RECENTS -> Icons.Rounded.CropSquare
            NavButton.MENU -> Icons.Rounded.Menu
            NavButton.SEARCH -> Icons.Rounded.Search
            NavButton.POWER -> Icons.Rounded.PowerSettingsNew
            NavButton.VOLUME_DOWN -> Icons.Rounded.VolumeDown
            NavButton.VOLUME_UP -> Icons.Rounded.VolumeUp
            NavButton.UP -> Icons.Rounded.KeyboardArrowUp
            NavButton.DOWN -> Icons.Rounded.KeyboardArrowDown
            NavButton.LEFT -> Icons.Rounded.KeyboardArrowLeft
            NavButton.RIGHT -> Icons.Rounded.KeyboardArrowRight
            NavButton.CENTER -> Icons.Rounded.RadioButtonChecked
        }, null, Modifier.size(24.dp).graphicsLayer { rotationZ = iconRotation })
        else Canvas(Modifier.size(30.dp).graphicsLayer { rotationZ = iconRotation }) {
            val s = size.width / 32f
            fun p(x: Float, y: Float) = Offset(x * s, y * s)
            val stroke = Stroke(2f * s, cap = StrokeCap.Square, join = StrokeJoin.Miter)
            fun line(x: Float, y: Float, x1: Float, y1: Float) = drawLine(Color.White, p(x, y), p(x1, y1), 2f * s)
            fun outline(vararg points: Pair<Float, Float>, close: Boolean = false) {
                val path = Path().apply {
                    moveTo(points[0].first * s, points[0].second * s)
                    points.drop(1).forEach { lineTo(it.first * s, it.second * s) }
                    if (close) close()
                }
                drawPath(path, Color.White, style = stroke)
            }
            when (button) {
                NavButton.BACK -> {
                    // Holo's thin bent arrow, not the filled Material "undo" arrow.
                    outline(13f to 9f, 6f to 16f, 13f to 23f)
                    outline(6f to 16f, 24f to 16f, 24f to 23f)
                }
                NavButton.HOME -> outline(6f to 16f, 16f to 8f, 26f to 16f, 26f to 25f, 6f to 25f, close = true)
                NavButton.RECENTS -> {
                    outline(10f to 10f, 10f to 6f, 26f to 6f, 26f to 21f, 22f to 21f)
                    outline(6f to 11f, 21f to 11f, 21f to 26f, 6f to 26f, close = true)
                }
                NavButton.MENU -> for (y in listOf(7f, 14f, 21f)) {
                    drawRect(Color.White, p(14f, y), androidx.compose.ui.geometry.Size(4f * s, 4f * s))
                }
                NavButton.SEARCH -> { drawCircle(Color.White, 7f * s, p(13f, 13f), style = stroke); line(18f, 18f, 26f, 26f) }
                NavButton.POWER -> { drawArc(Color.White, -55f, 290f, false, p(6f, 6f), androidx.compose.ui.geometry.Size(20f * s, 20f * s), style = stroke); line(16f, 3f, 16f, 15f) }
                NavButton.VOLUME_DOWN, NavButton.VOLUME_UP -> {
                    outline(5f to 13f, 10f to 13f, 16f to 8f, 16f to 24f, 10f to 19f, 5f to 19f, close = true)
                    line(21f, 16f, 29f, 16f)
                    if (button == NavButton.VOLUME_UP) line(25f, 12f, 25f, 20f)
                }
                NavButton.UP -> outline(8f to 21f, 16f to 12f, 24f to 21f)
                NavButton.DOWN -> outline(8f to 11f, 16f to 20f, 24f to 11f)
                NavButton.LEFT -> outline(21f to 8f, 12f to 16f, 21f to 24f)
                NavButton.RIGHT -> outline(11f to 8f, 20f to 16f, 11f to 24f)
                NavButton.CENTER -> drawCircle(Color.White, 7f * s, p(16f, 16f), style = stroke)
            }
        }
    }
}

/** Single-finger rolling surface; cancelling/multitouch never generates a click.
 * It lives above the nav/inset area and consumes its own stream, not guest touch. */
internal class TrackballView(context: Context) : View(context) {
    var motion = TrackballMotion(18f)
    var dpad = false
    var onRoll: (Int, Int) -> Unit = { _, _ -> }
    var onSelect: () -> Unit = {}
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var pointer = -1
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var rolling = false
    init { isClickable = true; isFocusable = true }
    override fun onDraw(canvas: AndroidCanvas) {
        val radius = minOf(width, height) / 2f - 5f * resources.displayMetrics.density
        paint.color = if (isPressed) 0xff555555.toInt() else 0xff222222.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        paint.color = 0xffdddddd.toInt(); paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * resources.displayMetrics.density
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
    override fun performClick(): Boolean { super.performClick(); onSelect(); return true }
    private fun roll(x: Float, y: Float) {
        if (!rolling && kotlin.math.hypot(x - startX, y - startY) > slop) rolling = true
        if (rolling) {
            val density = resources.displayMetrics.density
            val (dx, dy) = motion.move((x - lastX) / density, (y - lastY) / density, dpad)
            if (dx != 0 || dy != 0) onRoll(dx, dy)
            lastX = x; lastY = y
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                pointer = event.getPointerId(0)
                startX = event.x; startY = event.y; lastX = event.x; lastY = event.y
                rolling = false; isPressed = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pointer)
                if (index < 0) return true
                for (h in 0 until event.historySize) roll(event.getHistoricalX(index, h), event.getHistoricalY(index, h))
                roll(event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_UP -> {
                if (pointer >= 0) roll(event.x, event.y)
                if (pointer >= 0 && !rolling) performClick()
                pointer = -1; isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                pointer = -1; rolling = true; isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        invalidate()
        return true
    }
}
