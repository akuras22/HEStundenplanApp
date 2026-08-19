package de.hsesslingen.stundenplan.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent color presets offered in Darstellung, on top of Dark/Light/System — CUSTOM defers to the
 *  user's own picked colors (see SettingsStore.customAccentColor/customBackgroundColor). */
enum class AccentPreset(val label: String, val color: Color?) {
    DEFAULT("Standard", null),
    GREEN("Grün", Color(0xFF2E9E5B)),
    PURPLE("Lila", Color(0xFF8B5CF6)),
    ORANGE("Orange", Color(0xFFE0762F)),
    RED("Rot", Color(0xFFE0473D)),
    PINK("Pink", Color(0xFFE0508F)),
    CUSTOM("Benutzerdefiniert", null),
}

/** Default lecture-reminder lead time, used both as the DataStore default and the initial chip
 *  selection in Benachrichtigungen. */
val DEFAULT_REMINDER_LEAD_MINUTES = setOf(15)

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
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val REMINDER_LEAD_MINUTES_SET = stringSetPreferencesKey("reminder_lead_minutes_set")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val CUSTOM_ACCENT_ARGB = intPreferencesKey("custom_accent_argb")
        val CUSTOM_BACKGROUND_ARGB = intPreferencesKey("custom_background_argb")
        val DEFAULT_VIEW_IS_DAY = booleanPreferencesKey("default_view_is_day")
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

    /** How many minutes before a lecture starts to notify — supports several simultaneous lead
     *  times (e.g. a 30-min heads-up AND a 5-min "it's starting" nudge). Falls back to the old
     *  single-value key so upgrades from before multi-reminder support keep their choice. */
    val reminderLeadMinutes: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        val set = prefs[Keys.REMINDER_LEAD_MINUTES_SET]
        when {
            !set.isNullOrEmpty() -> set.mapNotNull { it.toIntOrNull() }.toSet()
            prefs[Keys.REMINDER_LEAD_MINUTES] != null -> setOf(prefs[Keys.REMINDER_LEAD_MINUTES]!!)
            else -> DEFAULT_REMINDER_LEAD_MINUTES
        }
    }

    suspend fun setReminderLeadMinutes(minutes: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_LEAD_MINUTES_SET] = minutes.map { it.toString() }.toSet()
            prefs.remove(Keys.REMINDER_LEAD_MINUTES)
        }
    }

    /** Whether to use Android's wallpaper-tinted Material You palette instead of the app's own
     *  fixed neutral One UI colors (see StundenplanTheme) — off by default, since Samsung's own
     *  first-party apps don't tint their chrome by wallpaper either. */
    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_COLOR] ?: false }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR] = enabled }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    val accentPreset: Flow<AccentPreset> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_PRESET]?.let { raw -> runCatching { AccentPreset.valueOf(raw) }.getOrNull() } ?: AccentPreset.DEFAULT
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.dataStore.edit { prefs -> prefs[Keys.ACCENT_PRESET] = preset.name }
    }

    val customAccentColor: Flow<Color?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_ACCENT_ARGB]?.let { Color(it) }
    }

    suspend fun setCustomAccentColor(color: Color) {
        context.dataStore.edit { prefs -> prefs[Keys.CUSTOM_ACCENT_ARGB] = color.toArgb() }
    }

    val customBackgroundColor: Flow<Color?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_BACKGROUND_ARGB]?.let { Color(it) }
    }

    suspend fun setCustomBackgroundColor(color: Color) {
        context.dataStore.edit { prefs -> prefs[Keys.CUSTOM_BACKGROUND_ARGB] = color.toArgb() }
    }

    /** Whether the app opens straight into the single-day Tag view instead of Woche. */
    val defaultViewIsDay: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.DEFAULT_VIEW_IS_DAY] ?: false }

    suspend fun setDefaultViewIsDay(isDay: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DEFAULT_VIEW_IS_DAY] = isDay }
    }

    /** "Zurücksetzen" in Darstellung — puts every appearance-related setting back to the app's
     *  defaults (System theme, no dynamic color, default One UI accent, no custom colors). */
    suspend fun resetAppearance() {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = ThemeMode.SYSTEM.name
            prefs[Keys.DYNAMIC_COLOR] = false
            prefs[Keys.ACCENT_PRESET] = AccentPreset.DEFAULT.name
            prefs.remove(Keys.CUSTOM_ACCENT_ARGB)
            prefs.remove(Keys.CUSTOM_BACKGROUND_ARGB)
        }
    }
}
