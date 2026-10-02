package app.aemu.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.aemu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val title: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

/** Modified for AEmulator Sunset on 2026-09-30: use the fork's releases. */
object AppUpdateManager {
    private const val RELEASES_API = "https://api.github.com/repos/drel4/AEmulator-Sunset/releases"
    private const val GITHUB_REPO_URL = "https://github.com/drel4/AEmulator-Sunset"

    /**
     * Запрос к GitHub API для получения информации о последнем релизе с APK-файлом.
     */
    suspend fun checkLatestRelease(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(RELEASES_API)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "AEmulator-Sunset-App")

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            if (array.length() == 0) return@runCatching null

            for (i in 0 until array.length()) {
                val rel = array.getJSONObject(i)
                if (rel.optBoolean("draft", false)) continue

                val tagName = rel.optString("tag_name", "").trim()
                val versionName = tagName.removePrefix("v").removePrefix("V")
                val title = rel.optString("name", tagName)
                val body = rel.optString("body", "")

                val assets = rel.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.optString("name", "")
                    if (UpdateAssetPolicy.matches(assetName, BuildConfig.APPLICATION_ID)) {
                        val downloadUrl = asset.getString("browser_download_url")
                        val sizeBytes = asset.optLong("size", 0L)
                        return@runCatching ReleaseInfo(
                            versionName = versionName,
                            tagName = tagName,
                            title = title,
                            body = body,
                            downloadUrl = downloadUrl,
                            fileName = assetName,
                            sizeBytes = sizeBytes
                        )
                    }
                }
            }
            null
        }
    }

    /**
     * Сравнение версий (семантическое сравнение сегментов чисел).
     * Возвращает true, если latestTag новее чем currentVersion.
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

        val latestParts = cleanLatest.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val currentParts = cleanCurrent.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        return false
    }

    /**
     * Скачивание APK с поддержкой редиректов (GitHub перенаправляет на AWS S3) и отслеживанием прогресса.
     */
    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(UpdateAssetPolicy.matches(release.fileName, context.packageName)) {
            "Update APK does not match this application variant"
        }
        val updatesDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        // Очищаем старые apk перед новой загрузкой
        updatesDir.listFiles()?.forEach { if (it.extension.equals("apk", ignoreCase = true)) it.delete() }

        val targetFile = File(updatesDir, "aemulator-sunset-${release.versionName}.apk")

        var currentUrl = release.downloadUrl
        var conn: HttpURLConnection
        var redirects = 0
        while (true) {
            val url = URL(currentUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "AEmulator-Sunset-App")

            val code = conn.responseCode
            if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                val newUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (newUrl != null && redirects < 5) {
                    currentUrl = newUrl
                    redirects++
                    continue
                }
            }
            break
        }

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}")
        }

        val totalLength = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
        var downloadedBytes = 0L

        conn.inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val progress = if (totalLength > 0) downloadedBytes.toFloat() / totalLength else 0f
                    onProgress(progress.coerceIn(0f, 1f), downloadedBytes, totalLength)
                }
                output.flush()
            }
        }

        conn.disconnect()
        if (context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)?.packageName != context.packageName) {
            targetFile.delete()
            throw java.io.IOException("Downloaded APK has the wrong application package")
        }
        targetFile
    }

    /**
     * Запуск установки загруженного APK через системный PackageInstaller.
     */
    fun installApk(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        if (context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName != context.packageName) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return false
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return true
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, group.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[group])
    }
}
