package de.hsesslingen.stundenplan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Fetches the Studierendenwerk Stuttgart's Speiseplan live. Their page
 * (studierendenwerk-stuttgart.de/essen/speiseplan) only embeds an iframe from sws2.maxmanager.xyz,
 * which in turn loads each day via the same POST this makes — so this asks exactly what the
 * website itself asks, one location and one day at a time.
 */
class MensaRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        private const val ENDPOINT = MensaParser.BASE_URL + "inc/ajax-php_konnektor.inc.php"
    }

    suspend fun fetchDay(locationId: Int, date: LocalDate): MensaDay = withContext(Dispatchers.IO) {
        // The backend answers HTTP 500 without the two week parameters, even though the day alone
        // decides what it returns — any Monday works, so send the requested day's own week.
        val monday = date.with(DayOfWeek.MONDAY)
        val form = FormBody.Builder()
            .add("func", "make_spl")
            .add("locId", locationId.toString())
            .add("date", date.toString())
            .add("lang", "de")
            .add("startThisWeek", monday.toString())
            .add("startNextWeek", monday.plusWeeks(1).toString())
            .build()
        val request = Request.Builder()
            .url(ENDPOINT)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept-Language", "de-DE,de;q=0.9")
            .post(form)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} beim Laden von $ENDPOINT")
            }
            val html = response.body?.string() ?: throw IOException("Leere Antwort von $ENDPOINT")
            MensaParser.parseDay(html)
        }
    }
}
