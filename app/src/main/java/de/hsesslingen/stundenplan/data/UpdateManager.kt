package de.hsesslingen.stundenplan.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.hsesslingen.stundenplan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

    /**
     * Hands the APK download+install off to the browser instead of doing it ourselves. An app that
     * downloads another APK and calls the installer on it is exactly the "dropper" behavior Google
     * Play Protect blocks server-side — regardless of how that's presented in our UI — so this app
     * deliberately never touches the APK bytes or the package-installer APIs itself. Chrome (a
     * source Play Protect already trusts) does the actual download and install; the user gets a
     * normal "Downloads > tap to install" flow from there.
     */
    fun openDownloadInBrowser(update: UpdateInfo) {
        // UpdateManager is constructed with the Application context (not an Activity), so
        // startActivity() requires FLAG_ACTIVITY_NEW_TASK — without it this throws
        // AndroidRuntimeException at the tap that's supposed to open the browser.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
