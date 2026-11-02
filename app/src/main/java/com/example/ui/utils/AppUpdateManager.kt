package com.example.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSizeFormatted: String
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progressPercent: Int, val info: UpdateInfo) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val info: UpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null

    companion object {
        const val GITHUB_API_URL = "https://api.github.com/repos/Arunkumarrana422/Arvexa-Player-/releases/latest"
        const val REPO_WEB_URL = "https://github.com/Arunkumarrana422/Arvexa-Player-/releases"

        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun checkForUpdate(isManual: Boolean = false) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) {
            return
        }

        scope.launch {
            _updateState.value = UpdateState.Checking

            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "ArvexaPlayer-Android")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (isManual) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "No Update Available", Toast.LENGTH_SHORT).show()
                            }
                        }
                        _updateState.value = UpdateState.Idle
                        return@launch
                    }

                    val jsonStr = response.body?.string() ?: ""
                    if (jsonStr.isBlank()) {
                        _updateState.value = UpdateState.Idle
                        return@launch
                    }

                    val root = JSONObject(jsonStr)
                    val tagName = root.optString("tag_name", "").trim()
                    val title = root.optString("name", "New Update Available")
                    val body = root.optString("body", "Bug fixes and performance improvements.")

                    // Find the best matching APK asset (prioritize version match & newest upload)
                    var apkUrl: String? = null
                    var apkSize: Long = 0L

                    val assets = root.optJSONArray("assets")
                    if (assets != null) {
                        val cleanTag = tagName.removePrefix("v").removePrefix("V").trim()
                        val apkList = mutableListOf<JSONObject>()
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                apkList.add(asset)
                            }
                        }

                        // Priority 1: Exact version match in filename (e.g. "ArvexaPlayer.v1.0.2.apk")
                        val versionMatched = apkList.firstOrNull { asset ->
                            val name = asset.optString("name", "")
                            (cleanTag.isNotBlank() && name.contains(cleanTag)) || name.contains(tagName)
                        }

                        // Priority 2: Newest uploaded APK (by updated_at / created_at or latest added)
                        val chosenAsset = versionMatched ?: apkList.maxByOrNull { asset ->
                            asset.optString("updated_at", asset.optString("created_at", ""))
                        } ?: apkList.lastOrNull()

                        if (chosenAsset != null) {
                            apkUrl = chosenAsset.optString("browser_download_url", "")
                            apkSize = chosenAsset.optLong("size", 0L)
                        }
                    }

                    // Fallback to latest direct release URL if no asset attached yet
                    if (apkUrl.isNullOrBlank()) {
                        apkUrl = "https://github.com/Arunkumarrana422/Arvexa-Player-/releases/latest/download/ArvexaPlayer-Release.apk"
                    }

                    val currentVersion = BuildConfig.VERSION_NAME
                    if (isNewerVersion(currentVersion, tagName)) {
                        val sizeText = if (apkSize > 0) {
                            String.format("%.1f MB", apkSize / (1024.0 * 1024.0))
                        } else ""

                        val info = UpdateInfo(
                            versionName = tagName,
                            title = if (title.isNotBlank()) title else "Update $tagName",
                            changelog = if (body.isNotBlank()) body else "New performance updates and fixes.",
                            downloadUrl = apkUrl,
                            fileSizeFormatted = sizeText
                        )
                        _updateState.value = UpdateState.Available(info)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                        if (isManual) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "No Update Available", Toast.LENGTH_SHORT).show()
                            }
                        }
                        kotlinx.coroutines.delay(2000)
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Idle
                if (isManual) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not check updates: ${e.localizedMessage ?: "Network error"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun isNewerVersion(currentVersion: String, latestTag: String): Boolean {
        if (latestTag.isBlank()) return false
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V")

        if (cleanCurrent == cleanLatest) return false

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val curr = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > curr) return true
            if (lat < curr) return false
        }
        return false
    }

    fun startDownload(info: UpdateInfo) {
        if (_updateState.value is UpdateState.Downloading) return

        downloadJob?.cancel()
        downloadJob = scope.launch {
            _updateState.value = UpdateState.Downloading(0, info)

            try {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir

                // Clean up any old downloaded update APKs to prevent stale caching
                try {
                    downloadsDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("ArvexaPlayer") && file.name.endsWith(".apk")) {
                            file.delete()
                        }
                    }
                } catch (_: Exception) {}

                val safeTag = info.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "")
                val apkFile = File(downloadsDir, "ArvexaPlayer-$safeTag.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val request = Request.Builder()
                    .url(info.downloadUrl)
                    .header("User-Agent", "ArvexaPlayer-Android")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Download failed with HTTP ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response from server")
                    val totalBytes = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(apkFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes: Long = 0L
                    var lastReportedPercent = 0

                    outputStream.use { out ->
                        inputStream.use { input ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                out.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0) {
                                    val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                                    if (percent != lastReportedPercent) {
                                        lastReportedPercent = percent
                                        _updateState.value = UpdateState.Downloading(percent, info)
                                    }
                                }
                            }
                        }
                    }

                    _updateState.value = UpdateState.ReadyToInstall(apkFile, info)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Check unknown sources permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Please allow 'Install unknown apps' permission to update", Toast.LENGTH_LONG).show()
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun dismissUpdate() {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Idle
    }
}
