package de.hsesslingen.stundenplan.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

private const val STUDIENGANG_FIELD_SEP = "||"

private fun encodeStudiengang(s: Studiengang) = "${s.code}$STUDIENGANG_FIELD_SEP${s.abstgvnr}$STUDIENGANG_FIELD_SEP${s.parallelid}"

private fun decodeStudiengang(raw: String): Studiengang? {
    val parts = raw.split(STUDIENGANG_FIELD_SEP)
    return if (parts.size == 3) Studiengang(code = parts[0], abstgvnr = parts[1], parallelid = parts[2]) else null
}

/**
 * Persists user settings — the chosen/favorite Studiengänge, which recurring event groups the user
 * hid, and small bookkeeping values (last update check, reminders toggle) — never any timetable
 * data itself. The schedule is always re-fetched live from the website (with an offline fallback
 * held separately in [TimetableCache]).
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val CODE = stringPreferencesKey("studiengang_code")
        val ABSTGVNR = stringPreferencesKey("studiengang_abstgvnr")
        val PARALLELID = stringPreferencesKey("studiengang_parallelid")
        val FAVORITES = stringSetPreferencesKey("favorite_studiengaenge")
        val HIDDEN_EVENT_KEYS = stringSetPreferencesKey("hidden_event_keys")
        val LAST_UPDATE_CHECK_AT = longPreferencesKey("last_update_check_at_millis")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val NOTIFIED_DATE = stringPreferencesKey("notified_date")
        val NOTIFIED_KEYS = stringSetPreferencesKey("notified_keys")
    }

    val selectedStudiengang: Flow<Studiengang?> = context.dataStore.data.map { prefs ->
        val code = prefs[Keys.CODE]
        val abstgvnr = prefs[Keys.ABSTGVNR]
        val parallelid = prefs[Keys.PARALLELID]
        if (code != null && abstgvnr != null && parallelid != null) {
            Studiengang(code = code, abstgvnr = abstgvnr, parallelid = parallelid)
        } else {
            null
        }
    }

    suspend fun setSelectedStudiengang(studiengang: Studiengang) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CODE] = studiengang.code
            prefs[Keys.ABSTGVNR] = studiengang.abstgvnr
            prefs[Keys.PARALLELID] = studiengang.parallelid
        }
    }

    /** Studiengänge starred for quick switching — separate from [selectedStudiengang], which is
     *  just "whichever one is currently shown". */
    val favoriteStudiengaenge: Flow<List<Studiengang>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.FAVORITES] ?: emptySet()).mapNotNull(::decodeStudiengang).sortedBy { it.code }
    }

    suspend fun setFavorite(studiengang: Studiengang, favorite: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            val encoded = encodeStudiengang(studiengang)
            prefs[Keys.FAVORITES] = if (favorite) current + encoded else current.filterNot { it == encoded }.toSet()
        }
    }

    /** Recurring event groups (see [TimetableEvent.groupKey]) the user chose to hide — e.g. the two
     *  parallel Tutorium groups they're not actually in. */
    val hiddenEventKeys: Flow<Set<String>> = context.dataStore.data.map { prefs -> prefs[Keys.HIDDEN_EVENT_KEYS] ?: emptySet() }

    suspend fun setHidden(groupKey: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_EVENT_KEYS] ?: emptySet()
            prefs[Keys.HIDDEN_EVENT_KEYS] = if (hidden) current + groupKey else current - groupKey
        }
    }

    val lastUpdateCheckAt: Flow<Long> = context.dataStore.data.map { prefs -> prefs[Keys.LAST_UPDATE_CHECK_AT] ?: 0L }

    suspend fun setLastUpdateCheckAt(millis: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_UPDATE_CHECK_AT] = millis }
    }

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.REMINDERS_ENABLED] ?: false }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.REMINDERS_ENABLED] = enabled }
    }

    /** Whether a reminder for [groupKey] was already sent on [date] (format "yyyy-MM-dd") — the
     *  reminder worker runs every 15 minutes, so without this it would re-notify for the same
     *  upcoming lecture on every run until it actually starts. */
    suspend fun hasNotifiedToday(date: String, groupKey: String): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[Keys.NOTIFIED_DATE] != date) return false
        return groupKey in (prefs[Keys.NOTIFIED_KEYS] ?: emptySet())
    }

    /** Records [groupKey] as notified for [date], resetting the tracked set first if [date] is a
     *  new day — so this only ever holds one day's worth of keys, never grows unbounded. */
    suspend fun markNotifiedToday(date: String, groupKey: String) {
        context.dataStore.edit { prefs ->
            val sameDay = prefs[Keys.NOTIFIED_DATE] == date
            prefs[Keys.NOTIFIED_DATE] = date
            prefs[Keys.NOTIFIED_KEYS] = (if (sameDay) prefs[Keys.NOTIFIED_KEYS] ?: emptySet() else emptySet()) + groupKey
        }
    }
}
