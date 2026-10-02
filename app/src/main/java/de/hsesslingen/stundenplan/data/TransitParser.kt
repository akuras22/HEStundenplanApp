package de.hsesslingen.stundenplan.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Parses the VVS EFA backend's "rapidJSON" answers — the same interface vvs.de's own departure
 * monitor and journey planner use.
 */
object TransitParser {

    /** XML_DM_REQUEST: departures from one stop, in the order they actually leave — EFA sorts by
     *  timetable, so a bus running 15 minutes late would otherwise sit above punctual earlier ones. */
    fun parseDepartures(json: String): List<Departure> {
        val root = JSONObject(json)
        val events = root.optJSONArray("stopEvents") ?: return emptyList()
        // "Esslingen (N) ZOB" leaving from "Esslingen (N)" reads fine as just "ZOB".
        val locality = root.optJSONArray("locations")?.optJSONObject(0)
            ?.optJSONObject("parent")?.optString("name")?.takeIf { it.isNotBlank() }
        return events.objects().mapNotNull { event -> parseDeparture(event, locality) }.sortedBy { it.expected }
    }

    private fun parseDeparture(event: JSONObject, locality: String?): Departure? {
        val transportation = event.optJSONObject("transportation") ?: return null
        val planned = event.optString("departureTimePlanned").toInstantOrNull() ?: return null
        val line = transportation.optString("disassembledName").ifBlank { transportation.optString("number") }
            .ifBlank { transportation.optString("name") }
        val productClass = transportation.optJSONObject("product")?.optInt("class", -1) ?: -1
        var destination = transportation.optJSONObject("destination")?.optString("name").orEmpty()
        if (locality != null && destination.startsWith("$locality ") && destination.length > locality.length + 1) {
            destination = destination.removePrefix("$locality ")
        }
        val properties = event.optJSONObject("location")?.optJSONObject("properties")
        val platformNumber = properties?.optString("platform").orEmpty()
        val platform = properties?.optString("platformName")?.takeIf { it.isNotBlank() }
            ?: platformNumber.takeIf { it.isNotBlank() }?.let { if (productClass == 0 || productClass == 1) "Gleis $it" else "Steig $it" }
        val realtimeStatus = event.optJSONArray("realtimeStatus")?.strings().orEmpty()
        val cancelled = event.optBoolean("isCancelled", false) || "TRIP_CANCELLED" in realtimeStatus
        return Departure(
            line = line,
            productClass = productClass,
            destination = destination,
            planned = planned,
            estimated = event.optString("departureTimeEstimated").toInstantOrNull(),
            platform = platform,
            isCancelled = cancelled,
            notices = parseNotices(event.optJSONArray("infos")),
        )
    }

    private fun parseNotices(infos: JSONArray?): List<TransitNotice> =
        infos?.objects().orEmpty().flatMap { info ->
            info.optJSONArray("infoLinks")?.objects().orEmpty().mapNotNull { link ->
                val title = link.optString("subtitle").ifBlank { link.optString("title") }.trim()
                val text = link.optString("content").trim()
                if (title.isEmpty() && text.isEmpty()) null else TransitNotice(title = title.ifEmpty { text }, text = text)
            }
        }.distinct()

    /** XML_STOPFINDER_REQUEST: stops matching a search, best match first. */
    fun parseStops(json: String): List<TransitStop> =
        JSONObject(json).optJSONArray("locations")?.objects().orEmpty()
            .filter { it.optString("type") == "stop" }
            .sortedByDescending { it.optInt("matchQuality", 0) }
            .mapNotNull { location ->
                val id = location.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = location.optString("name").ifBlank { return@mapNotNull null }
                TransitStop(id = id, name = name, shortName = location.optString("disassembledName").ifBlank { name })
            }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }

    private fun String.toInstantOrNull(): Instant? =
        takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
