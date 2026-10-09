package de.hsesslingen.stundenplan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.concurrent.TimeUnit

/** QIS answered with the HS Esslingen SSO login instead of the page, and there is no (valid) login
 *  in Einstellungen ▸ Hochschul-Login to get past it. */
class LoginRequiredException : IOException("QIS verlangt eine Anmeldung")

/**
 * Talks directly to HS Esslingen's public QIS/LSF pages. Nothing is cached to disk: every call
 * hits the website live, matching what a browser would show at that moment.
 */
class QisRepository(
    private val session: QisSession? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        // Without a login: plain http on purpose — since October 2026 the https side of QIS
        // redirects every request to the HS Esslingen SSO login, while http still serves the
        // public pages (QIS's own links are http too). Cleartext is allowed for this one host in
        // network_security_config.xml.
        private const val HTTP_BASE = "http://www3.hs-esslingen.de/qislsf/rds"
        // With a login (see QisSession): https plus its session cookie.
        private const val HTTPS_BASE = "https://www3.hs-esslingen.de/qislsf/rds"
        private val LOGIN_HOSTS = setOf("idp.hs-esslingen.de", "login.hs-esslingen.de")
    }

    /** Fetches the full public catalog of Studiengang timetables (all faculties/semesters). */
    suspend fun fetchStudiengaenge(): List<Studiengang> = withContext(Dispatchers.IO) {
        val html = fetch {
            addQueryParameter("state", "verpublish")
            addQueryParameter("publishContainer", "stgPlanList")
            addQueryParameter("navigationPosition", "lectures,curriculaschedulesList")
            addQueryParameter("breadcrumb", "curriculaschedules")
            addQueryParameter("topitem", "lectures")
            addQueryParameter("subitem", "curriculaschedulesList")
        }
        QisParser.parseStudiengangList(html)
    }

    /**
     * Fetches the real timetable for the specific week containing [weekMonday], live — not the
     * recurring semester template. QIS's actual per-week data (via `week=<isoWeek>_<isoYear>`)
     * reflects what's really scheduled that week (e.g. empty outside term dates, room changes,
     * cancellations), unlike the generic `week=-2` "Semesteransicht" template.
     */
    suspend fun fetchTimetable(studiengang: Studiengang, weekMonday: LocalDate): List<TimetableEvent> = withContext(Dispatchers.IO) {
        val isoWeek = weekMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val isoYear = weekMonday.get(IsoFields.WEEK_BASED_YEAR)
        val html = fetch {
            addQueryParameter("state", "wplan")
            addQueryParameter("act", "stg")
            addQueryParameter("pool", "stg")
            addQueryParameter("show", "plan")
            addQueryParameter("P.vx", "lang")
            addQueryParameter("week", "${isoWeek}_$isoYear")
            addQueryParameter("k_parallel.parallelid", studiengang.parallelid)
            addQueryParameter("k_abstgv.abstgvnr", studiengang.abstgvnr)
            addQueryParameter("noDBAction", "y")
        }
        QisParser.parseTimetable(html)
    }

    /** Loads one QIS page: over https with the login if there is one, otherwise (or once QIS turns
     *  the login away) anonymously over http. */
    private fun fetch(query: HttpUrl.Builder.() -> Unit): String {
        val login = session?.activeLogin()
        if (session != null && login != null) {
            execute(HTTPS_BASE.toHttpUrl().newBuilder().apply(query).build(), login.cookie)?.let { return it }
            // The session ran out on QIS's side (idle too long, or ended early) — Einstellungen
            // shows that, and this request carries on without it.
            session.markExpired()
        }
        return execute(HTTP_BASE.toHttpUrl().newBuilder().apply(query).build(), cookie = null)
            ?: throw LoginRequiredException()
    }

    /** Returns null when QIS redirected to the SSO login instead of serving the page. */
    private fun execute(url: HttpUrl, cookie: String?): String? {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept-Language", "de-DE,de;q=0.9")
            .apply { if (cookie != null) header("Cookie", cookie) }
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.request.url.host in LOGIN_HOSTS) return null
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} beim Laden von $url")
            }
            return response.body?.string() ?: throw IOException("Leere Antwort von $url")
        }
    }
}
