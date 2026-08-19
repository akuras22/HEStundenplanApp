package de.hsesslingen.stundenplan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.IsoFields

private val Context.timetableCacheStore by preferencesDataStore(name = "timetable_cache")

data class CachedWeek(val events: List<TimetableEvent>, val savedAt: Long)

/**
 * Offline fallback for the last few weeks successfully fetched from QIS. Never a source of truth —
 * the live site always wins when reachable — this only exists so a dropped connection (e.g. bad
 * reception on campus) shows the schedule as of the last successful fetch instead of a bare error.
 * Capped to [MAX_ENTRIES] weeks with oldest-first eviction so this doesn't grow forever across a
 * whole degree's worth of semesters.
 */
class TimetableCache(private val context: Context) {

    private fun weekKey(studiengang: Studiengang, weekMonday: LocalDate): String {
        val isoWeek = weekMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val isoYear = weekMonday.get(IsoFields.WEEK_BASED_YEAR)
        return "${studiengang.id}_${isoYear}_$isoWeek"
    }

    suspend fun get(studiengang: Studiengang, weekMonday: LocalDate): CachedWeek? {
        val key = weekKey(studiengang, weekMonday)
        val raw = context.timetableCacheStore.data.first()[stringPreferencesKey(key)] ?: return null
        return decode(raw)
    }

    /** Manual "Zwischenspeicher leeren" action — the offline fallback rebuilds itself from the next
     *  successful live fetch, so this is always safe, just occasionally inconvenient offline. */
    suspend fun clearAll() {
        context.timetableCacheStore.edit { it.clear() }
    }

    suspend fun put(studiengang: Studiengang, weekMonday: LocalDate, events: List<TimetableEvent>) {
        val key = weekKey(studiengang, weekMonday)
        context.timetableCacheStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encode(events, System.currentTimeMillis())
            val manifestKey = stringPreferencesKey(MANIFEST_KEY)
            val manifest = (prefs[manifestKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
                .filterNot { it == key } + key
            if (manifest.size > MAX_ENTRIES) {
                manifest.take(manifest.size - MAX_ENTRIES).forEach { prefs.remove(stringPreferencesKey(it)) }
                prefs[manifestKey] = manifest.takeLast(MAX_ENTRIES).joinToString(",")
            } else {
                prefs[manifestKey] = manifest.joinToString(",")
            }
        }
    }

    private fun encode(events: List<TimetableEvent>, savedAt: Long): String {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("day", e.day.name)
                    put("title", e.title)
                    put("startMinutes", e.startMinutes)
                    put("endMinutes", e.endMinutes)
                    put("frequency", e.frequency)
                    put("room", e.room)
                    put("lecturer", e.lecturer)
                    put("category", e.category)
                    put("startDate", e.startDate)
                    put("endDate", e.endDate)
                },
            )
        }
        return JSONObject().apply { put("savedAt", savedAt); put("events", arr) }.toString()
    }

    private fun decode(raw: String): CachedWeek? = try {
        val root = JSONObject(raw)
        val arr = root.getJSONArray("events")
        val events = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TimetableEvent(
                day = Weekday.valueOf(o.getString("day")),
                title = o.getString("title"),
                startMinutes = o.getInt("startMinutes"),
                endMinutes = o.getInt("endMinutes"),
                frequency = o.optStringOrNull("frequency"),
                room = o.optStringOrNull("room"),
                lecturer = o.optStringOrNull("lecturer"),
                category = o.optStringOrNull("category"),
                startDate = o.optStringOrNull("startDate"),
                endDate = o.optStringOrNull("endDate"),
            )
        }
        CachedWeek(events, root.getLong("savedAt"))
    } catch (_: Exception) {
        null
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)

    companion object {
        private const val MAX_ENTRIES = 12
        private const val MANIFEST_KEY = "manifest"
    }
}
