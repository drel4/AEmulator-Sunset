/* Modified for AEmulator Sunset through 2026-10-03: natural-edge controls, cutout and
 * compatibility and camera integration. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import app.aemu.R
import java.io.File
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import app.aemu.core.NavButton
import app.aemu.core.NavControls
import app.aemu.core.HeldNavKeys
import app.aemu.core.VmUiPolicy
import app.aemu.core.TrackballMotion
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.core.Engine
import app.aemu.core.GuestLog
import app.aemu.core.GuestVm
import app.aemu.core.ImageStore
import app.aemu.core.InputService
import kotlinx.coroutines.delay
import kotlin.concurrent.thread
import app.aemu.core.DisplayGeometry
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.rounded.ScreenRotation

/** Процесс :vm держит ровно одну машину. */
object VmHost {
    @Volatile var vm: GuestVm? = null
}

class VmActivity : ComponentActivity() {
    private lateinit var vm: GuestVm
    private val endingVm = java.util.concurrent.atomic.AtomicBoolean(false)
    private val processLifetime = android.os.Binder()
    private val heldNavKeys = HeldNavKeys { code, down -> if (::vm.isInitialized) vm.input.key(code, down) }
    private lateinit var surfaceView: SurfaceView
    private lateinit var guest: GuestScreen
    private lateinit var box: FrameLayout
    private var state by mutableStateOf(GuestVm.State.STOPPED)
    private val logLines = mutableStateListOf<String>()
    private var controlsHeightPx = 0
    private var iconTurns by mutableStateOf(0)
    private var windowTurns by mutableStateOf(0)
    private var tablet = false
    private val controlsTurns get() = app.aemu.core.HostDisplayPolicy.controlsTurns(tablet, hostUi.tabletNavbarRotation, windowTurns)
    private var hostUi by mutableStateOf(app.aemu.AppPrefs.HostUiOptions())
    private var vmRoot: FrameLayout? = null
    private var cutoutInsets = androidx.core.graphics.Insets.NONE
    private var orientationListener: android.view.OrientationEventListener? = null
    private var updateGuestLayout: (() -> Unit)? = null
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (::vm.isInitialized) {
            vm.log("camera: host permission ${if (granted) "granted" else "denied"}")
            if (!granted) toast(getString(R.string.camera_permission_denied))
            bootIfStopped()
        }
    }

    private fun bootIfStopped() {
        if (vm.state == GuestVm.State.STOPPED || vm.state == GuestVm.State.FAILED) {
            thread(name = "vm-boot") { vm.boot() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val point = android.graphics.Point()
        windowManager.defaultDisplay.getRealSize(point)
        val natural = DisplayGeometry.natural(point.x, point.y, windowManager.defaultDisplay.rotation)
        // Tablets may rotate the host container without rotating/resizing the guest framebuffer.
        // Phones retain the fixed natural window. Guest Android owns its display orientation.
        tablet = app.aemu.core.HostDisplayPolicy.rotateWindow(resources.configuration.smallestScreenWidthDp)
        requestedOrientation = if (tablet) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else if (natural.first > natural.second)
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        windowTurns = windowManager.defaultDisplay.rotation
        hostUi = app.aemu.AppPrefs.hostUiOptions(this)
        orientationListener = object : android.view.OrientationEventListener(this, android.hardware.SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(angle: Int) {
                if (angle != ORIENTATION_UNKNOWN) iconTurns = app.aemu.core.HostDisplayPolicy.iconTurns(angle, iconTurns)
                windowTurns = windowManager.defaultDisplay.rotation
            }
        }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (android.os.Build.VERSION.SDK_INT >= 30)
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        val id = intent.getStringExtra(EXTRA_ID)
        val cur = VmHost.vm
        val stored = (if (id != null) ImageStore.get(this, id) else null) ?: cur?.img
        if (stored == null) { finish(); return }
        try { app.aemu.core.VmStorageLease.forVm(filesDir).acquire() }
        catch (_: Exception) { toast(getString(R.string.vm_storage_busy)); finish(); return }
        val img = if (cur?.img?.id == stored.id) stored else app.aemu.importer.Analyzer.refresh(this, stored).effective()
        if (cur != null && ((cur.img.id != img.id && cur.state != GuestVm.State.STOPPED) || cur.state == GuestVm.State.FAILED)) {
            // в процессе уже живёт другая машина — её надо сначала остановить
            cur.stop()
            restartProcess(img.id, intent.getBooleanExtra(EXTRA_RECOVERY, false), intent.getBooleanExtra(EXTRA_LOW_POWER, false))
            return
        }
        val resolved = if (img.settings.hostResolution && img.engine != Engine.GB) {
            val (width, height) = natural
            img.copy(settings = img.settings.copy(width = width and -2, height = height and -2,
                density = resources.displayMetrics.densityDpi))
        } else img
        vm = if (cur != null && cur.img.id == img.id) cur
            else GuestVm(applicationContext, resolved, intent.getBooleanExtra(EXTRA_LOW_POWER, false)).also { it.recoveryMode = intent.getBooleanExtra(EXTRA_RECOVERY, false); VmHost.vm = it }
        vm.onPower = { reboot, reason -> runOnUiThread { if (reboot) rebootVm(reason == "recovery") else stopVm() } }
        val s = vm.settings
        if (s.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val (gw, gh) = if (vm.engine == Engine.GB) 480 to 800 else s.width to s.height
        val root = FrameLayout(this).apply { setBackgroundColor(0xff000000.toInt()) }
        vmRoot = root
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            cutoutInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            applyCutoutBarrier()
            insets
        }
        box = FrameLayout(this)
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) { vm.surface = h.surface }
            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) { vm.surface = h.surface }
            override fun surfaceDestroyed(h: SurfaceHolder) { vm.surface = null }
        })
        guest = GuestScreen(this, gw, gh).apply {
            input = vm.input
            fb = if (vm.recoveryMode) app.aemu.core.RecoveryImage.fb(vm.paths) else vm.paths.fb
            if (vm.recoveryMode || vm.lowPowerBoot) pages = 2
        }
        // 5.0+ renders through the standalone glserverd into fb0 (see GuestVm.glUp), shown like the software path
        val useBridge = vm.engine == Engine.KK && s.gpu && !vm.recoveryMode && !vm.lowPowerBoot && vm.img.api < 21
        surfaceView.visibility = if (useBridge) View.VISIBLE else View.GONE
        guest.passthrough = useBridge
        box.addView(surfaceView, FrameLayout.LayoutParams(-1, -1))
        box.addView(guest, FrameLayout.LayoutParams(-1, -1))
        root.addView(box, FrameLayout.LayoutParams(0, 0))
        val overlay = ComposeView(this).apply { setContent { AemuTheme(forceDark = true) { Overlay() } } }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // экран гостя вписываем с сохранением пропорций над панелью кнопок
        updateGuestLayout = layout@{
            if (root.width <= 0 || root.height <= 0) return@layout
            val fit = DisplayGeometry.fit(root.width - root.paddingLeft - root.paddingRight,
                root.height - root.paddingTop - root.paddingBottom, gw, gh, controlsHeightPx, controlsTurns, s.hostResolution)
            val w = fit.width
            val h = fit.height
            val lp = box.layoutParams as FrameLayout.LayoutParams
            val left = fit.left
            val top = fit.top
            box.rotation = 0f
            if (lp.width != w || lp.height != h || lp.topMargin != top || lp.leftMargin != left) {
                lp.width = w; lp.height = h
                lp.gravity = Gravity.LEFT or Gravity.TOP
                lp.leftMargin = left; lp.topMargin = top
                box.layoutParams = lp
            }
        }
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateGuestLayout?.invoke() }

        vm.onFrame = { guest.rings++; guest.poke() }
        state = vm.state
        logLines.addAll(vm.lines().takeLast(200))
        vm.onState { st -> runOnUiThread { state = st } }
        vm.onLog { line -> runOnUiThread { logLines.add(line); if (logLines.size > 400) logLines.removeRange(0, logLines.size - 400) } }
        guest.start()
        loadMenuOffset()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (VmUiPolicy.back(showLog, vm.settings.hideMenuButton)) {
                    VmUiPolicy.BackAction.CLOSE_LOG -> { showLog = false; menuOpen = false }
                    VmUiPolicy.BackAction.OPEN_MENU -> menuOpen = true
                    VmUiPolicy.BackAction.GUEST_BACK -> vm.input.press(InputService.KEY_BACK)
                }
            }
        })

        registerShell()
        if (s.camera && vm.cameraSupported &&
            checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            (vm.state == GuestVm.State.STOPPED || vm.state == GuestVm.State.FAILED)) {
            cameraPermission.launch(android.Manifest.permission.CAMERA)
        } else bootIfStopped()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!::vm.isInitialized) return
        val id = intent.getStringExtra(EXTRA_ID) ?: vm.img.id
        val charging = intent.getBooleanExtra(EXTRA_LOW_POWER, false)
        val recovery = intent.getBooleanExtra(EXTRA_RECOVERY, false)
        if (id != vm.img.id || (charging && !vm.lowPowerBoot) || (recovery && !vm.recoveryMode)) {
            if (!endingVm.compareAndSet(false, true)) return
            heldNavKeys.releaseAll()
            thread { vm.stop(); VmHost.vm = null; runOnUiThread { restartProcess(id, recovery, charging) } }
        }
    }

    override fun onResume() {
        super.onResume()
        windowTurns = windowManager.defaultDisplay.rotation
        hostUi = app.aemu.AppPrefs.hostUiOptions(this)
        applyCutoutBarrier()
        orientationListener?.enable()
        if (::vm.isInitialized) { vm.cameraVisible(true); vm.motionVisible(true) }
    }

    override fun onPause() {
        orientationListener?.disable()
        heldNavKeys.releaseAll()
        if (::vm.isInitialized) { vm.cameraVisible(false); vm.motionVisible(false) }
        super.onPause()
    }

    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        super.onConfigurationChanged(config)
        windowTurns = windowManager.defaultDisplay.rotation
        heldNavKeys.releaseAll()
        updateGuestLayout?.invoke()
    }

    private fun rotateScreen() {
        if (!vm.simulateRotation()) toast(getString(R.string.m_rotate_failed))
        else toast(getString(R.string.m_rotate_simulated))
    }

    private fun applyCutoutBarrier() {
        val root = vmRoot ?: return
        val inset = if (hostUi.cutoutBarrier) cutoutInsets else androidx.core.graphics.Insets.NONE
        root.setPadding(inset.left, inset.top, inset.right, inset.bottom)
        updateGuestLayout?.invoke()
    }

    /** Отладка: adb shell am broadcast -a app.aemu.SHELL --es cmd "dumpsys power" → run/shell.out */
    private var shellRx: android.content.BroadcastReceiver? = null
    private fun registerShell() {
        if (!app.aemu.BuildConfig.DEBUG) return
        val rx = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                // --es keys "116d 115 116u": raw guest key codes, d/u = down/up only, bare = press
                i.getStringExtra("keys")?.let { ks ->
                    thread {
                        for (k in ks.split(' ').filter { it.isNotBlank() }) {
                            val code = k.trimEnd('d', 'u').toIntOrNull() ?: continue
                            when { k.endsWith("d") -> vm.input.key(code, true); k.endsWith("u") -> vm.input.key(code, false)
                                else -> { vm.input.key(code, true); Thread.sleep(80); vm.input.key(code, false) } }
                            Thread.sleep(150)
                        }
                    }
                    return
                }
                if (i.hasExtra("adb")) { if (i.getBooleanExtra("adb", false)) vm.adb.start() else vm.adb.stop(); return }
                val cmd = i.getStringExtra("cmd") ?: return
                // своё имя файла на каждую команду: медленная предыдущая команда не затрёт ответ
                val name = i.getStringExtra("out")?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]+")) } ?: "shell.out"
                thread {
                    val out = runCatching { vm.guestShell(cmd, 120_000) }.getOrElse { it.toString() }
                    val tmp = java.io.File(vm.paths.bin, "$name.tmp")
                    tmp.writeText(out + "\n<<done>>\n"); tmp.renameTo(java.io.File(vm.paths.bin, name))
                }
            }
        }
        shellRx = rx
        val f = android.content.IntentFilter("app.aemu.SHELL")
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_EXPORTED) else registerReceiver(rx, f)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val code = map(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) vm.input.key(code, true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val code = map(keyCode) ?: return super.onKeyUp(keyCode, event)
        vm.input.key(code, false)
        return true
    }

    private fun map(k: Int): Int? = when (k) {
        KeyEvent.KEYCODE_VOLUME_UP -> InputService.KEY_VOLUMEUP
        KeyEvent.KEYCODE_VOLUME_DOWN -> InputService.KEY_VOLUMEDOWN
        KeyEvent.KEYCODE_MENU -> InputService.KEY_MENU
        KeyEvent.KEYCODE_SEARCH -> InputService.KEY_SEARCH
        else -> null
    }

    override fun onDestroy() {
        orientationListener?.disable()
        vmRoot = null
        heldNavKeys.releaseAll()
        if (::guest.isInitialized) guest.stop()
        shellRx?.let { runCatching { unregisterReceiver(it) } }
        super.onDestroy()
    }

    private fun stopVm() {
        if (!endingVm.compareAndSet(false, true)) return
        thread {
            vm.stop()
            VmHost.vm = null
            runOnUiThread { finishAndRemoveTask() }
            Thread.sleep(300)
            // GL-мост нельзя поднять повторно в том же процессе — процесс :vm уходит вместе с машиной
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private enum class Dlg { NONE, SMS, CALL, BATTERY, ADB, DEVICE }

    // position of the floating menu button, kept across launches
    private val uiPrefs by lazy { getSharedPreferences("vm_ui", MODE_PRIVATE) }
    private var menuOffset by mutableStateOf(Offset.Zero)
    private var menuOpen by mutableStateOf(false)
    private var showLog by mutableStateOf(false)
    private fun loadMenuOffset() { menuOffset = Offset(uiPrefs.getFloat("menu_x", 0f), uiPrefs.getFloat("menu_y", 0f)) }
    private fun saveMenuOffset() { uiPrefs.edit().putFloat("menu_x", menuOffset.x).putFloat("menu_y", menuOffset.y).apply() }

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    private fun guestAsync(cmd: String, done: ((String) -> Unit)? = null) {
        thread { val out = runCatching { vm.guestShell(cmd, 60_000) }.getOrElse { it.toString() }; done?.invoke(out) }
    }

    /** Quote for the guest's /system/bin/sh. */
    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    private fun pasteToGuest() {
        val text = getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isEmpty()) { toast(getString(R.string.m_clip_empty)); return }
        // `input text` treats spaces as separators; %s is its escape for a space
        guestAsync("input text " + q(text.replace(" ", "%s")))
    }

    private fun screenshot() {
        val name = "aemu-" + java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date()) + ".png"
        val dir = vm.img.sdcardPath.trimEnd('/') + "/Pictures"
        guestAsync("mkdir -p $dir; screencap -p $dir/$name && echo OK") { out ->
            toast(if (out.contains("OK")) getString(R.string.m_screenshot_saved, "$dir/$name") else getString(R.string.m_failed))
        }
    }

    // APK picked on the host: copied into the shared card folder, then installed by the guest's package manager
    private val pickApk = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val dir = app.aemu.core.Sdcard.hostDir(vm.paths)
        if (dir == null) { toast(getString(R.string.m_failed)); return@registerForActivityResult }
        toast(getString(R.string.m_install_apk_start))
        thread {
            val ok = runCatching {
                contentResolver.openInputStream(uri)!!.use { i -> java.io.File(dir, "aemu-install.apk").outputStream().use { o -> i.copyTo(o) } }
            }.isSuccess
            if (!ok) { toast(getString(R.string.m_failed)); return@thread }
            val out = runCatching { vm.guestShell("pm install -r " + vm.img.sdcardPath.trimEnd('/') + "/aemu-install.apk", 300_000) }.getOrElse { it.toString() }
            toast(out.trim().lines().lastOrNull { it.isNotBlank() }?.take(200) ?: getString(R.string.m_failed))
        }
    }

    private fun sendSms(from: String, body: String) {
        // no live modem: the message is written straight into the SMS provider inbox
        val cmd = "content insert --uri content://sms/inbox --bind address:s:${q(from)} --bind body:s:${q(body)} " +
            "--bind read:i:0 --bind date:l:${System.currentTimeMillis()}"
        guestAsync(cmd) { out -> toast(if (out.isBlank()) getString(R.string.m_sms_sent) else out.trim().take(200)) }
    }

    @Composable
    private fun SmsDialog(onDone: () -> Unit) {
        var from by remember { mutableStateOf("+10000000000") }
        var body by remember { mutableStateOf("Hello from AEmulator") }
        AlertDialog(onDismissRequest = onDone,
            title = { Text(stringResource(R.string.m_sms)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(from, { from = it }, label = { Text(stringResource(R.string.m_sms_from)) }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text(stringResource(R.string.m_sms_body)) })
                }
            },
            confirmButton = { Button(onClick = { sendSms(from, body); onDone() }) { Text(stringResource(R.string.m_send)) } },
            dismissButton = { OutlinedButton(onClick = onDone) { Text(stringResource(R.string.close)) } })
    }

    @Composable
    private fun DeviceDialog(onDone: () -> Unit) {
        var airplane by remember { mutableStateOf(false) }
        var awake by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = onDone,
            title = { Text(stringResource(R.string.m_device)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.m_airplane), Modifier.weight(1f))
                        Switch(airplane, { v ->
                            airplane = v
                            guestAsync("settings put global airplane_mode_on ${if (v) 1 else 0}; " +
                                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $v")
                        })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.m_stay_awake), Modifier.weight(1f))
                        Switch(awake, { v ->
                            awake = v
                            guestAsync("settings put global stay_on_while_plugged_in ${if (v) 7 else 0}; svc power stayon ${if (v) "true" else "false"}")
                        })
                    }
                }
            },
            confirmButton = { Button(onClick = onDone) { Text(stringResource(R.string.close)) } })
    }

    @Composable
    private fun AdbDialog(onDone: () -> Unit) {
        var on by remember { mutableStateOf(vm.adb.running) }
        AlertDialog(onDismissRequest = onDone,
            title = { Text(stringResource(R.string.m_adb)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.m_adb_enable), Modifier.weight(1f))
                        Switch(on, { v -> if (v) vm.adb.start() else vm.adb.stop(); on = vm.adb.running })
                    }
                    if (on) {
                        val ips = app.aemu.core.AdbServer.addresses()
                        for (ip in ips.ifEmpty { listOf("127.0.0.1") })
                            Text("adb connect $ip:${vm.adb.port}", style = MaterialTheme.typography.bodyLarge,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Text(stringResource(R.string.m_adb_hint), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = onDone) { Text(stringResource(R.string.close)) } })
    }

    @Composable
    private fun CallDialog(onDone: () -> Unit) {
        var from by remember { mutableStateOf("+10000000000") }
        AlertDialog(onDismissRequest = onDone,
            title = { Text(stringResource(R.string.m_call)) },
            text = { OutlinedTextField(from, { from = it }, label = { Text(stringResource(R.string.m_sms_from)) }, singleLine = true) },
            confirmButton = { Button(onClick = {
                if (!vm.ril.ring(from.trim())) toast(getString(R.string.m_failed))
                onDone()
            }) { Text(stringResource(R.string.m_call_ring)) } },
            dismissButton = { OutlinedButton(onClick = onDone) { Text(stringResource(R.string.close)) } })
    }

    @Composable
    private fun BatteryDialog(onDone: () -> Unit) {
        var level by remember { mutableStateOf(vm.hostBatteryLevel.toFloat()) }
        var charging by remember { mutableStateOf(vm.hostBatteryCharging) }
        AlertDialog(onDismissRequest = onDone,
            title = { Text(stringResource(R.string.m_battery)) },
            text = {
                Column {
                    Text("${level.toInt()}%")
                    Slider(level, { level = it }, valueRange = 0f..100f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.m_charging), Modifier.weight(1f)); Switch(charging, { charging = it })
                    }
                }
            },
            confirmButton = { Button(onClick = {
                vm.setManualBattery(level.toInt(), charging)
                onDone()
            }) { Text(stringResource(R.string.m_apply)) } },
            dismissButton = { OutlinedButton(onClick = { vm.resetBattery(); onDone() }) { Text(stringResource(R.string.m_reset)) } })
    }

    private fun rebootVm(recovery: Boolean = false) {
        if (!endingVm.compareAndSet(false, true)) return
        heldNavKeys.releaseAll()
        val id = vm.img.id
        thread {
            vm.stop()
            VmHost.vm = null
            // the GL bridge cannot come up twice in one process, so a reboot restarts the :vm process
            runOnUiThread { restartProcess(id, recovery) }
        }
    }

    private fun restartProcess(id: String, recovery: Boolean = false, lowPower: Boolean = false) {
        // Hand off while still foreground, then wait for Binder death in the main process.
        // Removing the whole task here could also remove the new restart activity.
        try { VmRestartActivity.handoff(this, id, recovery, processLifetime, lowPower) }
        catch (_: Exception) { endingVm.set(false); toast(getString(R.string.vm_restart_failed)); return }
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // ------------------------------------------------------------------ интерфейс поверх экрана

    @Composable
    private fun Overlay() {
        var menu by ::menuOpen
        var dialog by remember { mutableStateOf(Dlg.NONE) }
        val running = state == GuestVm.State.RUNNING
        var seconds by remember { mutableStateOf(0L) }
        var frames by remember { mutableStateOf(0L) }
        var romNote by remember { mutableStateOf(vm.pendingNote) }
        LaunchedEffect(vm) { while (true) { romNote = vm.pendingNote; delay(500) } }
        LaunchedEffect(Unit) { while (true) { if (vm.recoveryMode) guest.format = vm.recoveryFormat; seconds = vm.bootSeconds(); frames = if (vm.glInApp) dev.lk.m7sense.GlBridge.frames() else guest.rings; delay(500) } }
        // как только гость начал рисовать — карточку убираем, остаётся маленький индикатор
        val drawing = frames > 30
        Box(Modifier.fillMaxSize()) {
            if (romNote.isNotBlank() && !vm.recoveryMode && !vm.lowPowerBoot && !showLog && !menu) {
                Surface(shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 360.dp).heightIn(max = 200.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Text(stringResource(R.string.vm_rom_note), style = MaterialTheme.typography.titleSmall)
                        Text(romNote, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            // карточка загрузки
            AnimatedVisibility(
                visible = state != GuestVm.State.RUNNING && !showLog && !(drawing && state == GuestVm.State.BOOTING),
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) { BootCard(seconds) }

            if (drawing && state == GuestVm.State.BOOTING && !showLog) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vm_booting_short, seconds.toInt()), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // журнал
            AnimatedVisibility(visible = showLog, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                LogPanel(onClose = { showLog = false })
            }

            // top menu button (hidden while the log is open so it does not cover the log toolbar); drag to move it
            if (!showLog || vm.settings.hideMenuButton) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                .offset { IntOffset(menuOffset.x.roundToInt(), menuOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = { saveMenuOffset() }) { ch, d -> ch.consume(); menuOffset += d }
                }) {
                if (!vm.settings.hideMenuButton) {
                    FilledTonalIconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                } else {
                    // Keep a stable, non-interactive dropdown anchor when the button is hidden.
                    Spacer(Modifier.size(1.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.log)) }, leadingIcon = { Icon(Icons.Rounded.Terminal, null) },
                        onClick = { menu = false; showLog = true })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.vol_up)) }, leadingIcon = { Icon(Icons.Rounded.VolumeUp, null) },
                        onClick = { vm.input.press(InputService.KEY_VOLUMEUP) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.vol_down)) }, leadingIcon = { Icon(Icons.Rounded.VolumeDown, null) },
                        onClick = { vm.input.press(InputService.KEY_VOLUMEDOWN) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.power)) }, leadingIcon = { Icon(Icons.Rounded.PowerSettingsNew, null) },
                        onClick = { menu = false; vm.input.press(InputService.KEY_POWER) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_power_menu)) }, leadingIcon = { Icon(Icons.Rounded.PowerSettingsNew, null) },
                        onClick = { menu = false; vm.input.press(InputService.KEY_POWER, 1500) })
                    if (!vm.settings.motionSensors) DropdownMenuItem(text = { Text(stringResource(R.string.m_rotate_screen)) }, leadingIcon = { Icon(Icons.Rounded.ScreenRotation, null) },
                        enabled = running && vm.img.api in 9..25 && !vm.recoveryMode && !vm.lowPowerBoot,
                        onClick = { menu = false; rotateScreen() })
                    // long-press Menu makes 2.x–4.x call InputMethodManager.toggleSoftInput: the firmware's own keyboard
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_keyboard)) }, leadingIcon = { Icon(Icons.Rounded.Keyboard, null) },
                        enabled = running, onClick = { menu = false; vm.input.press(InputService.KEY_MENU, 1000) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_paste)) }, leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                        enabled = running, onClick = { menu = false; pasteToGuest() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_sms)) }, leadingIcon = { Icon(Icons.Rounded.Sms, null) },
                        enabled = running, onClick = { menu = false; dialog = Dlg.SMS })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_call)) }, leadingIcon = { Icon(Icons.Rounded.Call, null) },
                        enabled = running, onClick = { menu = false; dialog = Dlg.CALL })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_battery)) }, leadingIcon = { Icon(Icons.Rounded.BatteryStd, null) },
                        enabled = running && vm.img.api >= 19, onClick = { menu = false; dialog = Dlg.BATTERY })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_install_apk)) }, leadingIcon = { Icon(Icons.Rounded.InstallMobile, null) },
                        enabled = running, onClick = { menu = false; pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_device)) }, leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                        enabled = running && vm.img.api >= 17, onClick = { menu = false; dialog = Dlg.DEVICE })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_adb)) }, leadingIcon = { Icon(Icons.Rounded.Lan, null) },
                        enabled = running, onClick = { menu = false; dialog = Dlg.ADB })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_screenshot)) }, leadingIcon = { Icon(Icons.Rounded.Screenshot, null) },
                        enabled = running, onClick = { menu = false; screenshot() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_settings)) }, leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                        enabled = running, onClick = { menu = false; guestAsync("am start -a android.settings.SETTINGS") })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_minimize)) }, leadingIcon = { Icon(Icons.Rounded.Minimize, null) },
                        onClick = {
                            menu = false
                            heldNavKeys.releaseAll()
                            startActivity(Intent(this@VmActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        })
                    DropdownMenuItem(text = { Text(stringResource(R.string.m_reboot)) }, leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) },
                        onClick = { menu = false; rebootVm(vm.recoveryMode) })
                    DropdownMenuItem(text = { Text(stringResource(if (vm.recoveryMode) R.string.m_reboot_system else R.string.m_reboot_recovery)) },
                        leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) },
                        enabled = vm.recoveryMode || app.aemu.core.RecoveryImage.installed(vm.paths),
                        onClick = { menu = false; rebootVm(!vm.recoveryMode) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shutdown)) }, leadingIcon = { Icon(Icons.Rounded.Close, null) },
                        onClick = { menu = false; stopVm() })
                }
            }

            when (dialog) {
                Dlg.SMS -> SmsDialog { dialog = Dlg.NONE }
                Dlg.CALL -> CallDialog { dialog = Dlg.NONE }
                Dlg.BATTERY -> BatteryDialog { dialog = Dlg.NONE }
                Dlg.ADB -> AdbDialog { dialog = Dlg.NONE }
                Dlg.DEVICE -> DeviceDialog { dialog = Dlg.NONE }
                Dlg.NONE -> {}
            }

            val buttons = NavControls.parse(vm.settings.navButtons).filter {
                it != NavButton.RECENTS || vm.img.api >= 11
            }
            val showButtons = vm.settings.showNavBar && buttons.isNotEmpty()
            val stripTurns = controlsTurns
            val sideways = stripTurns % 2 != 0
            val stripAlignment = when (DisplayGeometry.edge(stripTurns)) {
                DisplayGeometry.Edge.RIGHT -> Alignment.CenterEnd
                DisplayGeometry.Edge.LEFT -> Alignment.CenterStart
                DisplayGeometry.Edge.TOP -> Alignment.TopCenter
                DisplayGeometry.Edge.BOTTOM -> Alignment.BottomCenter
            }
            DisposableEffect(showButtons, vm.settings.trackball, showLog, stripTurns) {
                heldNavKeys.releaseAll()
                box.post { updateGuestLayout?.invoke() }
                if ((!showButtons && !vm.settings.trackball) || showLog) {
                    controlsHeightPx = 0; box.post { updateGuestLayout?.invoke() }
                }
                onDispose { }
            }
            if ((showButtons || vm.settings.trackball) && !showLog) {
                Box(Modifier.align(stripAlignment).onSizeChanged { size ->
                    controlsHeightPx = if (sideways) size.width else size.height
                    box.post { updateGuestLayout?.invoke() }
                }) {
                Surface(
                    color = if (hostUi.sunsetNavbar) Color(0xF20B0B0B) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = if (hostUi.sunsetNavbar) androidx.compose.ui.graphics.RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    modifier = (if (sideways) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                        .padding(if (hostUi.sunsetNavbar) 0.dp else 6.dp),
                ) {
                    ControlStrip(stripTurns, Modifier.navigationBarsPadding(), trackball = {
                        if (vm.settings.trackball) Column(
                            if (sideways) Modifier.width(112.dp) else Modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val dpad = vm.settings.trackballDpad
                            val motion = remember(vm.settings.trackballStepDp, dpad) { TrackballMotion(vm.settings.trackballStepDp.toFloat()) }
                            val description = stringResource(R.string.nav_trackball)
                            AndroidView(
                                factory = { ctx -> TrackballView(ctx) },
                                modifier = Modifier.padding(vertical = 8.dp).size(72.dp),
                                update = { view ->
                                    view.contentDescription = description
                                    view.motion = motion; view.dpad = dpad
                                    view.onRoll = { dx, dy ->
                                        if (dpad) vm.input.dpadMotion(dx, dy) else vm.input.trackball(dx, dy)
                                    }
                                    view.onSelect = {
                                        if (dpad) vm.input.press(vm.input.centerCode) else vm.input.trackballClick()
                                    }
                                },
                            )
                            var connected by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { while (true) { connected = vm.input.trackballConnected > 0; delay(500) } }
                            if (!dpad && (!connected || vm.recoveryMode)) {
                                Text(stringResource(if (vm.recoveryMode) R.string.nav_trackball_recovery else R.string.nav_trackball_waiting),
                                    style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                            }
                        }
                    }, buttons = {
                        if (showButtons) ControlButtons(sideways) {
                            (if (stripTurns == 1 || stripTurns == 2) buttons.asReversed() else buttons).forEach { button ->
                                val code = when (button) {
                                    NavButton.HOME -> vm.input.homeCode
                                    NavButton.CENTER -> vm.input.centerCode
                                    else -> button.scanCode
                                }
                                key(button, code) {
                                    HoloNavButton(button, stringResource(button.labelRes()),
                                        onClick = { vm.input.press(code) },
                                        onDown = { heldNavKeys.down(code) }, onUp = { heldNavKeys.up(code) },
                                        iconRotation = app.aemu.core.HostDisplayPolicy.controlIconRotation(tablet,
                                            hostUi.tabletNavbarRotation, iconTurns, windowTurns), original = !hostUi.sunsetNavbar)
                                }
                            }
                        }
                        // Trackball-only layouts still keep it above a navbar-sized safety zone.
                        if (!showButtons && vm.settings.trackball) Spacer(if (sideways) Modifier.width(52.dp) else Modifier.height(52.dp))
                    })
                }
                }
            }
        }
    }

    @Composable
    private fun BootCard(seconds: Long) {
        Card(
            modifier = Modifier.widthIn(max = 360.dp).padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state == GuestVm.State.FAILED) {
                    Text(stringResource(R.string.failed_start), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(vm.failure ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { stopVm() }) { Text(stringResource(R.string.close)) }
                        Button(onClick = { thread { vm.stop(); vm.boot() } }) { Text(stringResource(R.string.retry)) }
                    }
                } else {
                    LoadingIndicator(Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(vm.img.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(vm.img.displayVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val phase = when (state) {
                        GuestVm.State.PREPARING -> stringResource(R.string.st_preparing)
                        GuestVm.State.BOOTING -> stringResource(R.string.st_booting, seconds.toInt())
                        GuestVm.State.STOPPING -> stringResource(R.string.st_stopping)
                        else -> "…"
                    }
                    Text(phase, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (vm.img.bootCount == 0 && state == GuestVm.State.BOOTING) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.first_boot_hint),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(logLines.lastOrNull()?.substringAfter(' ') ?: "", style = Mono, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Composable
    private fun LogPanel(onClose: () -> Unit) {
        var guestTab by remember { mutableStateOf(false) }
        val guestLines = remember { mutableStateListOf<String>() }
        LaunchedEffect(guestTab) {
            while (guestTab) {
                val recs = GuestLog.tail(vm.paths.root, 128 * 1024).takeLast(300)
                guestLines.clear(); guestLines.addAll(recs.map { "${it.prio} ${it.tag}: ${it.msg}" })
                delay(1500)
            }
        }
        val list = if (guestTab) guestLines else logLines
        Surface(Modifier.fillMaxSize(), color = Color(0xF0101410), contentColor = Color(0xFFE2E3DD)) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.log), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { copyLog(list.toList()) }) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.log_copy)) }
                    IconButton(onClick = { pendingGuestExport = guestTab; exportLog.launch("aemu-${vm.img.id}-${if (guestTab) "logcat" else "host"}.log") }) {
                        Icon(Icons.Rounded.Download, stringResource(R.string.log_export))
                    }
                    FilledTonalIconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
                }
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = !guestTab, onClick = { guestTab = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                        Text(stringResource(R.string.host), maxLines = 1)
                    }
                    SegmentedButton(selected = guestTab, onClick = { guestTab = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                        Text(stringResource(R.string.guest_log), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val st = rememberLazyListState()
                LaunchedEffect(list.size) { if (list.isNotEmpty()) st.scrollToItem(list.size - 1) }
                SelectionContainer {
                    LazyColumn(state = st, modifier = Modifier.fillMaxSize()) {
                        items(list) { Text(it, style = Mono, color = Color(0xFFCFE8CF)) }
                    }
                }
            }
        }
    }

    private var pendingGuestExport = false
    private val exportLog = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@registerForActivityResult
        val guest = pendingGuestExport
        thread {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)!!.bufferedWriter().use { w ->
                    if (guest) GuestLog.writeExport(vm.paths.root, vm.paths.bin, w)
                    else {
                        val hostLog = File(vm.paths.bin, "aemu.log")
                        if (hostLog.isFile) hostLog.bufferedReader().use { it.copyTo(w) }
                        else vm.lines().forEach { w.write(it); w.write("\n") }
                    }
                }
            }.isSuccess
            runOnUiThread { Toast.makeText(this, if (ok) R.string.log_exported else R.string.log_export_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun copyLog(lines: List<String>) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("AEmulator log", lines.joinToString("\n")))
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_RECOVERY = "recovery"
        const val EXTRA_LOW_POWER = "low_power"
        fun start(ctx: Context, id: String, recovery: Boolean = false, lowPower: Boolean = false) {
            ctx.startActivity(Intent(ctx, VmActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_RECOVERY, recovery).putExtra(EXTRA_LOW_POWER, lowPower)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
