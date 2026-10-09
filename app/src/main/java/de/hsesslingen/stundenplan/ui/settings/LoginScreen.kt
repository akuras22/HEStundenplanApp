package de.hsesslingen.stundenplan.ui.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.hsesslingen.stundenplan.data.QisLogin
import de.hsesslingen.stundenplan.data.QisSession
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val QIS_HOST = "www3.hs-esslingen.de"
private const val QIS_COOKIE_URL = "https://www3.hs-esslingen.de/qislsf/"

// The Studiengangpläne page: opening it over https sends the WebView through the SSO login and,
// once that's done, back to QIS with the session cookie set.
private const val LOGIN_START_URL = "https://www3.hs-esslingen.de/qislsf/rds?state=verpublish" +
    "&publishContainer=stgPlanList&navigationPosition=lectures,curriculaschedulesList" +
    "&breadcrumb=curriculaschedules&topitem=lectures&subitem=curriculaschedulesList"

/** How long the invisible SSO refresh waits for the redirect chain to land back on QIS before
 *  giving up. On the happy path it takes a second or two; the full wait only happens when the SSO
 *  session has actually ended and the bounce stalls on the login form instead of coming back. */
private const val SILENT_REFRESH_TIMEOUT_MS = 15_000L

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

private fun formatTime(millis: Long): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(if (dateTime.toLocalDate() == LocalDate.now()) TIME_FORMAT else DATE_TIME_FORMAT)
}

@Composable
fun LoginScreen(viewModel: StundenplanViewModel, onBack: () -> Unit, onStartLogin: () -> Unit) {
    val login by viewModel.qisLogin.collectAsState()

    SettingsPageScaffold(title = "Hochschul-Login", onBack = onBack) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LoginStatusCard(login)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Filled.Login,
                    title = if (login == null) "Anmelden" else "Erneut anmelden",
                    subtitle = "Auf der Login-Seite der Hochschule, mit zweitem Faktor",
                    onClick = onStartLogin,
                )
                if (login != null) {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Abmelden",
                        subtitle = "Sitzung aus der App entfernen",
                        onClick = {
                            // Also drop the SSO cookies the login WebView keeps, so the next login
                            // really asks again instead of silently reusing the old session.
                            CookieManager.getInstance().removeAllCookies(null)
                            viewModel.logout()
                        },
                    )
                }
            }
            Text(
                "Normalerweise nicht nötig: Der Stundenplan ist öffentlich und lädt auch ohne " +
                    "Anmeldung. Angemeldet lädt die App ihn verschlüsselt über https – und auch dann, " +
                    "wenn die Hochschule den Zugang ohne Anmeldung sperrt.\n\n" +
                    "Dein Passwort gibst du direkt auf der Login-Seite der Hochschule ein; die App " +
                    "speichert es nicht, nur die Sitzung. Sie erneuert sich danach von selbst – ohne " +
                    "erneute Anmeldung – bis zu rund 10 Stunden nach dem Login; danach ist eine neue " +
                    "Anmeldung nötig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoginStatusCard(login: QisLogin?) {
    val (title, subtitle) = when {
        login == null -> "Nicht angemeldet" to "Der Stundenplan wird ohne Anmeldung geladen."
        login.expired -> "Anmeldung abgelaufen" to "Seit ${formatTime(login.loggedInAt)} angemeldet, " +
            "inzwischen abgelaufen. Der Stundenplan wird wieder ohne Anmeldung geladen."
        else -> "Angemeldet" to "Seit ${formatTime(login.loggedInAt)}. Verlängert sich automatisch " +
            "ohne erneute Anmeldung, bis spätestens ${formatTime(login.ssoExpiresAt)}."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The SSO login itself, in a WebView: the user signs in on the university's own pages (whatever
 * second factor they use included), and as soon as the WebView lands back on QIS with a
 * Shibboleth session cookie, that cookie is handed to [onLoggedIn].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebScreen(onLoggedIn: (String) -> Unit, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = "Anmelden", onBack = onBack) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val cookies = CookieManager.getInstance()
                    cookies.setAcceptCookie(true)
                    WebView(context).apply {
                        // Keep the SSO session (Keycloak) cookies: the login bounces across www3,
                        // idp and login.hs-esslingen.de, so third-party cookies must be accepted,
                        // and they're flushed to disk below so the session survives an app restart
                        // and later lets SilentLoginRefresh renew the login without asking again.
                        cookies.setAcceptThirdPartyCookies(this, true)
                        // The SSO pages need both: JavaScript for their auto-submitting forms,
                        // DOM storage for the IdP's session check.
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                val uri = Uri.parse(url)
                                if (done || uri.host != QIS_HOST || uri.path?.startsWith("/qislsf") != true) return
                                val cookie = QisSession.shibbolethCookies(cookies.getCookie(QIS_COOKIE_URL))
                                if (cookie != null) {
                                    done = true
                                    cookies.flush()
                                    onLoggedIn(cookie)
                                }
                            }
                        }
                        loadUrl(LOGIN_START_URL)
                    }
                },
                onRelease = { it.destroy() },
            )
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

/**
 * An invisible WebView that renews the login without any UI. It loads the Studiengangpläne page,
 * and because the HS Esslingen SSO session (Keycloak, up to 10 h) still lives in the shared
 * CookieManager from the last login, Shibboleth hands back a fresh QIS session cookie with no
 * password and no second factor.
 *
 * [onResult] gets that cookie, or null if the SSO session has ended too — then the bounce stalls
 * on the login form instead of returning, so [SILENT_REFRESH_TIMEOUT_MS] elapses and a full login
 * is needed again. MainActivity mounts this only while a refresh is wanted (see
 * StundenplanViewModel.silentRefreshActive).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SilentLoginRefresh(onResult: (String?) -> Unit) {
    val currentOnResult by rememberUpdatedState(onResult)
    AndroidView(
        // Zero-size and non-interactive: the user never sees it, it just drives the SSO redirects.
        modifier = Modifier.size(0.dp),
        factory = { context ->
            val cookies = CookieManager.getInstance()
            cookies.setAcceptCookie(true)
            WebView(context).apply {
                cookies.setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                var done = false
                fun finish(cookie: String?) {
                    if (done) return
                    done = true
                    cookies.flush()
                    currentOnResult(cookie)
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val uri = Uri.parse(url)
                        // Success only once the chain has landed back on QIS with a session cookie.
                        // Intermediate stops on the SSO hosts are ignored on purpose: with a live
                        // session Keycloak auto-submits straight through them, so treating those as
                        // failure would abort a refresh that was about to succeed. A dead session
                        // instead stalls on the login form and is caught by the timeout below.
                        if (uri.host != QIS_HOST || uri.path?.startsWith("/qislsf") != true) return
                        finish(QisSession.shibbolethCookies(cookies.getCookie(QIS_COOKIE_URL)))
                    }
                }
                postDelayed({ finish(null) }, SILENT_REFRESH_TIMEOUT_MS)
                loadUrl(LOGIN_START_URL)
            }
        },
        onRelease = { it.destroy() },
    )
}
