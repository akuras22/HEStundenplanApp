package de.hsesslingen.stundenplan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persists only the user's chosen Studiengang (a setting), never any timetable data itself —
 * the schedule is always re-fetched live from the website.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val CODE = stringPreferencesKey("studiengang_code")
        val ABSTGVNR = stringPreferencesKey("studiengang_abstgvnr")
        val PARALLELID = stringPreferencesKey("studiengang_parallelid")
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
}
