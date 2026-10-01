package app.aemu.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.LocalSocket
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Size
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Opt-in, visible-activity-only camera source. Nothing opens a camera during enumeration. */
class HostCameraBridge(private val ctx: Context, paths: VmPaths, private val enabled: () -> Boolean,
                       private val log: (String) -> Unit) {
    private data class Lens(val id: String, val info: CameraProtocol.Info, val size: Size)
    private data class Frame(val nv21: ByteArray, val timestamp: Long)
    private val manager: CameraManager? = ctx.getSystemService(CameraManager::class.java)
    @Volatile private var active = false
    @Volatile private var foreground = false
    private val clients = ConcurrentHashMap.newKeySet<LocalSocket>()
    private val sources = ConcurrentHashMap<Int, Source>()
    private val lenses by lazy { discover() }
    // Metadata does not open a camera: keep it stable during a background VM boot.
    private fun exposed() = active && enabled() &&
        ctx.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun permitted() = foreground && exposed()
    private val server = UnixServer(File(paths.root, "dev/aemu_camera"), "camera") { client ->
        clients.add(client)
        try { serveClient(client) }
        catch (e: Exception) { if (permitted()) log("camera: client failed: ${e.message}") }
        finally { clients.remove(client) }
    }

    fun serve(): Boolean {
        active = true
        val ok = server.start(log)
        if (!ok) active = false
        log("camera: bridge ${if (ok) "ready" else "unavailable"}; permission ${ctx.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED}; enabled ${enabled()}")
        return ok
    }
    fun visible(value: Boolean) { foreground = value; if (!value) closeCapture() }
    fun stop() { active = false; closeCapture(); server.stop() }
    private fun closeCapture() {
        clients.toList().forEach { runCatching { it.close() } }
        sources.values.toList().forEach { it.close() }
    }

    private fun discover(): List<Lens> = runCatching {
        val service = manager ?: return@runCatching emptyList()
        val candidates = service.cameraIdList.mapNotNull { id ->
            val c = service.getCameraCharacteristics(id)
            val facing = when (c[CameraCharacteristics.LENS_FACING]) {
                CameraCharacteristics.LENS_FACING_BACK -> 0
                CameraCharacteristics.LENS_FACING_FRONT -> 1
                else -> return@mapNotNull null
            }
            val sizes = c[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(ImageFormat.YUV_420_888)
                ?: return@mapNotNull null
            val size = sizes.filter { it.width in 2..1920 && it.height in 2..1080 && it.width % 2 == 0 && it.height % 2 == 0 }
                .minByOrNull { kotlin.math.abs(it.width * it.height - CameraProtocol.WIDTH * CameraProtocol.HEIGHT) }
                ?: return@mapNotNull null
            Lens(id, CameraProtocol.Info(facing, c[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0), size)
        }
        // Stable rear/front ordering; don't expose duplicate physical/logical lenses.
        candidates.sortedBy { it.info.facing }.distinctBy { it.info.facing }.take(2)
    }.onFailure { log("camera: enumeration failed: ${it.message}") }.getOrDefault(emptyList())

    private fun serveClient(client: LocalSocket) {
        client.soTimeout = 5000
        val request = ByteArray(CameraProtocol.REQUEST_BYTES)
        DataInputStream(client.inputStream).readFully(request)
        val command = CameraProtocol.request(request) ?: return
        val output = client.outputStream
        fun error(code: Int) { output.write(CameraProtocol.header("ERR1", code)); output.flush() }
        if (command.operation == 0) {
            output.write(CameraProtocol.information(if (exposed()) lenses.map { it.info } else emptyList()))
            output.flush(); return
        }
        if (!permitted()) { error(-13); return }
        val lens = lenses.getOrNull(command.camera) ?: run { error(-19); return }
        if (command.operation == 2) {
            val frame = sources[command.camera]?.latest?.get() ?: run { error(-19); return }
            if (!permitted()) { error(-13); return }
            val jpeg = ByteArrayOutputStream()
            if (!YuvImage(frame.nv21, ImageFormat.NV21, CameraProtocol.WIDTH, CameraProtocol.HEIGHT, null)
                    .compressToJpeg(Rect(0, 0, CameraProtocol.WIDTH, CameraProtocol.HEIGHT), command.quality, jpeg)) {
                error(-5); return
            }
            val bytes = jpeg.toByteArray()
            if (bytes.size !in 1..CameraProtocol.MAX_JPEG_BYTES) { error(-5); return }
            output.write(CameraProtocol.header("JPG1", bytes.size, frame.timestamp)); output.write(bytes); output.flush()
            return
        }
        val source = Source(lens)
        if (sources.putIfAbsent(command.camera, source) != null) { source.close(); error(-16); return }
        try {
            // Recheck after registration so pause/stop cannot miss a newly registered camera.
            if (!permitted()) { error(-13); return }
            source.open()
            if (!source.first.await(5, TimeUnit.SECONDS) || source.latest.get() == null || !permitted()) {
                error(-5); return
            }
            log("camera: ${if (lens.info.facing == 0) "rear" else "front"} preview connected")
            var timestamp = -1L
            while (permitted() && !source.closed.get()) {
                val frame = source.latest.get()
                if (frame != null && frame.timestamp != timestamp) {
                    output.write(CameraProtocol.header("FRM1", frame.nv21.size, frame.timestamp))
                    output.write(frame.nv21); output.flush(); timestamp = frame.timestamp
                }
                Thread.sleep(67)
            }
        } finally { sources.remove(command.camera, source); source.close() }
    }

    private inner class Source(private val lens: Lens) {
        val latest = AtomicReference<Frame?>()
        val first = CountDownLatch(1)
        val closed = AtomicBoolean(false)
        private val thread = HandlerThread("host-camera").apply { start() }
        private val handler = Handler(thread.looper)
        @Volatile private var reader: ImageReader? = null
        @Volatile private var device: CameraDevice? = null
        @Volatile private var session: CameraCaptureSession? = null
        @Volatile private var opening = false
        private var lastConvertedAt = 0L

        @SuppressLint("MissingPermission")
        @Synchronized
        fun open() {
            if (closed.get() || !permitted()) return
            val service = manager ?: error("Host camera service unavailable")
            val images = ImageReader.newInstance(lens.size.width, lens.size.height, ImageFormat.YUV_420_888, 2)
            reader = images
            if (closed.get()) { images.close(); return }
            images.setOnImageAvailableListener({ input ->
                val image = runCatching { input.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                try {
                    val now = SystemClock.elapsedRealtime()
                    if (closed.get() || !permitted() || now - lastConvertedAt < 60) return@setOnImageAvailableListener
                    val crop = image.cropRect
                    val frame = Yuv420Converter.nv21(image.width, image.height,
                        image.planes.map { Yuv420Converter.Plane(it.buffer, it.rowStride, it.pixelStride) },
                        crop.left, crop.top, crop.width(), crop.height())
                    latest.set(Frame(frame, image.timestamp)); lastConvertedAt = now; first.countDown()
                } catch (e: Exception) { log("camera: frame conversion failed: ${e.message}"); close() }
                finally { image.close() }
            }, handler)
            opening = true
            try { service.openCamera(lens.id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera; opening = false
                    if (closed.get() || !permitted()) { close(); return }
                    try {
                        camera.createCaptureSession(listOf(images.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(capture: CameraCaptureSession) {
                                session = capture
                                if (closed.get() || !permitted()) { close(); return }
                                runCatching {
                                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                        addTarget(images.surface)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                                    }.build()
                                    capture.setRepeatingRequest(request, null, handler)
                                }.onFailure { log("camera: request failed: ${it.message}"); close() }
                            }
                            override fun onConfigureFailed(capture: CameraCaptureSession) { capture.close(); close() }
                        }, handler)
                    } catch (e: Exception) { log("camera: session failed: ${e.message}"); close() }
                }
                override fun onDisconnected(camera: CameraDevice) { device = camera; opening = false; camera.close(); close() }
                override fun onError(camera: CameraDevice, error: Int) { device = camera; opening = false; log("camera: host error $error"); camera.close(); close() }
                override fun onClosed(camera: CameraDevice) { thread.quitSafely() }
            }, handler) } catch (e: Exception) { opening = false; close(); throw e }
        }
        @Synchronized
        fun close() {
            closed.set(true); first.countDown(); latest.set(null)
            runCatching { session?.close() }; runCatching { device?.close() }; runCatching { reader?.close() }
            // An in-flight open must deliver onOpened so its CameraDevice can be closed.
            if (device == null && !opening) thread.quitSafely()
        }
    }
}
