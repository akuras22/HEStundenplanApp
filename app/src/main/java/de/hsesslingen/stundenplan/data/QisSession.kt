package de.hsesslingen.stundenplan.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A login made in Einstellungen ▸ Hochschul-Login. [expired] means QIS no longer accepts it (or
 *  its maximum lifetime has passed) — it's kept rather than dropped so Einstellungen can say so. */
data class QisLogin(
    val cookie: String,
    val loggedInAt: Long,
    /** When the user last did a full SSO login (password + second factor). The HS Esslingen SSO
     *  session lives this long no matter how often the QIS session cookie is silently renewed from
     *  it, so it's the real "you won't have to log in again until" time. */
    val ssoLoginAt: Long,
    val expired: Boolean,
) {
    /** When the current QIS session cookie reaches its maximum lifetime and a fresh one has to be
     *  fetched — silently from the still-valid SSO session where possible (see SilentLoginRefresh). */
    val expiresAt: Long get() = loggedInAt + QisSession.MAX_LIFETIME_MILLIS

    /** When the SSO session itself runs out — after this a silent refresh no longer works and a
     *  full login (password + second factor) is needed again. */
    val ssoExpiresAt: Long get() = ssoLoginAt + QisSession.SSO_LIFETIME_MILLIS
}

/**
 * The optional QIS login: since October 2026 https QIS pages need the HS Esslingen SSO login
 * (username, password and a second factor), which only a real browser can do — so the login runs
 * in a WebView (see LoginWebScreen) and only the Shibboleth session cookie it leaves behind for
 * www3.hs-esslingen.de is kept here. QisRepository sends it with every request; without it (or
 * once QIS turns it away) the public pages are loaded anonymously over http instead.
 *
 * The state is process-wide, so the ViewModel, the reminder worker and Einstellungen all see the
 * same login, and a login that expired during a background fetch shows up right away.
 */
class QisSession(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("qis_session", Context.MODE_PRIVATE)

    init {
        synchronized(QisSession) {
            if (!loaded) {
                state.value = read()
                loaded = true
            }
        }
    }

    val login: StateFlow<QisLogin?> get() = state.asStateFlow()

    /** The login to send, or null if there is none or it has expired. */
    fun activeLogin(): QisLogin? {
        val current = state.value ?: return null
        if (current.expired) return null
        if (System.currentTimeMillis() >= current.expiresAt) {
            markExpired()
            return null
        }
        return current
    }

    /** @param fullLogin true for a real SSO login (resets the SSO session clock), false for a
     *   silent refresh (keeps the original SSO clock — reusing the SSO session doesn't extend its
     *   maximum lifetime). */
    fun save(cookie: String, fullLogin: Boolean) {
        val now = System.currentTimeMillis()
        val ssoAt = if (fullLogin) now else (state.value?.ssoLoginAt ?: now)
        prefs.edit()
            .putString(KEY_COOKIE, cookie)
            .putLong(KEY_LOGGED_IN_AT, now)
            .putLong(KEY_SSO_LOGIN_AT, ssoAt)
            .putBoolean(KEY_EXPIRED, false)
            .apply()
        state.value = QisLogin(cookie, now, ssoAt, expired = false)
    }

    /** Whether the SSO session is still young enough that a fresh QIS session cookie can be fetched
     *  from it without the user logging in again (see SilentLoginRefresh). Stays true once the
     *  current cookie has expired — that's exactly when the silent refresh is wanted. */
    fun canSilentlyRefresh(): Boolean {
        val current = state.value ?: return false
        return System.currentTimeMillis() < current.ssoLoginAt + SSO_LIFETIME_MILLIS
    }

    fun markExpired() {
        val current = state.value ?: return
        prefs.edit().putBoolean(KEY_EXPIRED, true).apply()
        state.value = current.copy(expired = true)
    }

    fun clear() {
        prefs.edit().clear().apply()
        state.value = null
    }

    private fun read(): QisLogin? {
        val cookie = prefs.getString(KEY_COOKIE, null) ?: return null
        val loggedInAt = prefs.getLong(KEY_LOGGED_IN_AT, 0)
        return QisLogin(
            cookie = cookie,
            loggedInAt = loggedInAt,
            // Older stored logins predate the separate SSO clock — treat the login time as its start.
            ssoLoginAt = prefs.getLong(KEY_SSO_LOGIN_AT, loggedInAt),
            expired = prefs.getBoolean(KEY_EXPIRED, false),
        )
    }

    companion object {
        /** The Shibboleth session of www3.hs-esslingen.de ends after 8 hours at the latest (it
         *  can end earlier after a long idle stretch, which QisRepository notices on its own). */
        const val MAX_LIFETIME_MILLIS = 8 * 60 * 60 * 1000L

        /** The HS Esslingen SSO session (Keycloak `KEYCLOAK_IDENTITY`) lasts at most 10 hours from
         *  the login — measured in a browser, and there's no "stay signed in" to extend it. Within
         *  this window a new QIS session cookie can be minted silently; after it, a full login. */
        const val SSO_LIFETIME_MILLIS = 10 * 60 * 60 * 1000L

        private const val KEY_COOKIE = "cookie"
        private const val KEY_LOGGED_IN_AT = "logged_in_at"
        private const val KEY_SSO_LOGIN_AT = "sso_logged_in_at"
        private const val KEY_EXPIRED = "expired"

        private val state = MutableStateFlow<QisLogin?>(null)
        private var loaded = false

        /** Picks the Shibboleth session cookie(s) out of a WebView "a=1; b=2" cookie string —
         *  null if the login hasn't produced one (yet). */
        fun shibbolethCookies(cookieHeader: String?): String? =
            cookieHeader?.split(';')
                ?.map { it.trim() }
                ?.filter { it.startsWith("_shibsession_") }
                ?.joinToString("; ")
                ?.ifEmpty { null }
    }
}
