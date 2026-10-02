package de.hsesslingen.stundenplan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Live departures and stop search from the VVS (Verkehrs- und Tarifverbund Stuttgart) — the EFA
 * backend behind vvs.de's own departure monitor, which covers Esslingen's city buses (SVE), the
 * S-Bahn and regional trains, and Göppingen.
 */
class TransitRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        private const val BASE = "https://www3.vvs.de/mngvvs/"
        private val EFA_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val EFA_TIME = DateTimeFormatter.ofPattern("HHmm")
    }

    /** The next departures from [stop] — from now, or from [at] (e.g. when the last lecture ends). */
    suspend fun departures(stop: TransitStop, at: LocalDateTime? = null): List<Departure> = withContext(Dispatchers.IO) {
        val url = (BASE + "XML_DM_REQUEST").toHttpUrl().newBuilder()
            .addQueryParameter("SpEncId", "0")
            .addQueryParameter("coordOutputFormat", "EPSG:4326")
            .addQueryParameter("outputFormat", "rapidJSON")
            .addQueryParameter("language", "de")
            .addQueryParameter("type_dm", "any")
            .addQueryParameter("name_dm", stop.id)
            .addQueryParameter("mode", "direct")
            // Only this stop — without it EFA mixes in departures from stops nearby.
            .addQueryParameter("deleteAssignedStops_dm", "1")
            .addQueryParameter("useRealtime", "1")
            .addQueryParameter("itdDateTimeDepArr", "dep")
            .addQueryParameter("limit", "30")
            .apply {
                if (at != null) {
                    addQueryParameter("itdDate", at.format(EFA_DATE))
                    addQueryParameter("itdTime", at.format(EFA_TIME))
                }
            }
            .build()
        TransitParser.parseDepartures(execute(url.toString()))
    }

    suspend fun searchStops(query: String): List<TransitStop> = withContext(Dispatchers.IO) {
        val url = (BASE + "XML_STOPFINDER_REQUEST").toHttpUrl().newBuilder()
            .addQueryParameter("SpEncId", "0")
            .addQueryParameter("coordOutputFormat", "EPSG:4326")
            .addQueryParameter("outputFormat", "rapidJSON")
            .addQueryParameter("locationServerActive", "1")
            .addQueryParameter("type_sf", "any")
            .addQueryParameter("name_sf", query)
            // Stops only — no addresses or points of interest.
            .addQueryParameter("anyObjFilter_sf", "2")
            .build()
        TransitParser.parseStops(execute(url.toString()))
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HEStundenplan (+https://github.com/akuras22/HEStundenplanApp)")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} beim Laden von $url")
            return response.body?.string() ?: throw IOException("Leere Antwort von $url")
        }
    }
}
