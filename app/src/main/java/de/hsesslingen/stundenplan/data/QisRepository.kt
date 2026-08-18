package de.hsesslingen.stundenplan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.concurrent.TimeUnit

/**
 * Talks directly to HS Esslingen's public QIS/LSF pages. Nothing is cached to disk: every call
 * hits the website live, matching what a browser would show at that moment.
 */
class QisRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        private const val BASE = "https://www3.hs-esslingen.de/qislsf/rds"
    }

    /** Fetches the full public catalog of Studiengang timetables (all faculties/semesters). */
    suspend fun fetchStudiengaenge(): List<Studiengang> = withContext(Dispatchers.IO) {
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("state", "verpublish")
            .addQueryParameter("publishContainer", "stgPlanList")
            .addQueryParameter("navigationPosition", "lectures,curriculaschedulesList")
            .addQueryParameter("breadcrumb", "curriculaschedules")
            .addQueryParameter("topitem", "lectures")
            .addQueryParameter("subitem", "curriculaschedulesList")
            .build()
        val html = execute(url.toString())
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
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("state", "wplan")
            .addQueryParameter("act", "stg")
            .addQueryParameter("pool", "stg")
            .addQueryParameter("show", "plan")
            .addQueryParameter("P.vx", "lang")
            .addQueryParameter("week", "${isoWeek}_$isoYear")
            .addQueryParameter("k_parallel.parallelid", studiengang.parallelid)
            .addQueryParameter("k_abstgv.abstgvnr", studiengang.abstgvnr)
            .addQueryParameter("noDBAction", "y")
            .build()
        val html = execute(url.toString())
        QisParser.parseTimetable(html)
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept-Language", "de-DE,de;q=0.9")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} beim Laden von $url")
            }
            return response.body?.string() ?: throw IOException("Leere Antwort von $url")
        }
    }
}
