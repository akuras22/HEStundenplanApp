package de.hsesslingen.stundenplan.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import de.hsesslingen.stundenplan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String?,
)

/**
 * OTA update source: GitHub Releases on [BuildConfig.GITHUB_REPO]. CI (.github/workflows/release.yml)
 * tags every push to main as "v<versionCode>" and attaches the built APK, so the latest release's
 * tag number is directly comparable against the running app's own [BuildConfig.VERSION_CODE].
 */
class UpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val remoteVersionCode = json.optString("tag_name").removePrefix("v").toIntOrNull() ?: return@withContext null
            if (remoteVersionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }
            downloadUrl ?: return@withContext null

            UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = json.optString("name").ifBlank { json.optString("tag_name") },
                downloadUrl = downloadUrl,
                releaseNotes = json.optString("body").ifBlank { null },
            )
        }
    }

    /** Downloads [update]'s APK into the app's cache, reporting 0f..1f progress, and returns the local file. */
    suspend fun download(update: UpdateInfo, onProgress: (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "HEStundenplan-${update.versionCode}.apk")
        val request = Request.Builder().url(update.downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} beim Herunterladen des Updates")
            val body = response.body ?: throw IOException("Leere Antwort beim Herunterladen des Updates")
            val total = body.contentLength()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var copied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress(copied.toFloat() / total)
                    }
                }
            }
        }
        target
    }

    /** Launches the system package installer for [apkFile]. */
    fun install(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Android 8+ requires the user to opt this app into installing packages, once, in Settings. */
    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
}
