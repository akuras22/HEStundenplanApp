package de.hsesslingen.stundenplan.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import de.hsesslingen.stundenplan.BuildConfig
import de.hsesslingen.stundenplan.data.AccentPreset
import de.hsesslingen.stundenplan.data.CalendarExporter
import de.hsesslingen.stundenplan.data.LectureReminderWorker
import de.hsesslingen.stundenplan.data.NotificationHelper
import de.hsesslingen.stundenplan.data.QisRepository
import de.hsesslingen.stundenplan.data.SettingsStore
import de.hsesslingen.stundenplan.data.Studiengang
import de.hsesslingen.stundenplan.data.ThemeMode
import de.hsesslingen.stundenplan.data.TimetableCache
import de.hsesslingen.stundenplan.data.TimetableEvent
import de.hsesslingen.stundenplan.data.UpdateInfo
import de.hsesslingen.stundenplan.data.UpdateManager
import de.hsesslingen.stundenplan.data.friendlyNetworkErrorMessage
import de.hsesslingen.stundenplan.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class PlanUiState(
    val studiengang: Studiengang? = null,
    val events: List<TimetableEvent> = emptyList(),
    val weekMonday: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Set when the events shown are a stale offline fallback (see TimetableCache) rather than a
    // fresh fetch — offlineSince is when that fallback was itself originally fetched.
    val isOffline: Boolean = false,
    val offlineSince: Long? = null,
)

data class UpdateUiState(
    val available: UpdateInfo? = null,
)

data class StudiengangPickerState(
    val all: List<Studiengang> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val filtered: List<Studiengang>
        get() = if (query.isBlank()) all else all.filter { it.code.contains(query, ignoreCase = true) }
}

class StundenplanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QisRepository()
    private val settingsStore = SettingsStore(application)
    private val updateManager = UpdateManager(application)
    private val timetableCache = TimetableCache(application)
    private val calendarExporter = CalendarExporter(application)

    private val _planState = MutableStateFlow(PlanUiState())
    val planState: StateFlow<PlanUiState> = _planState.asStateFlow()

    private val _pickerState = MutableStateFlow(StudiengangPickerState())
    val pickerState: StateFlow<StudiengangPickerState> = _pickerState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** One-shot action-feedback messages (e.g. "Zwischenspeicher geleert.") shown as a Snackbar
     *  from MainActivity — a SharedFlow rather than state, so the same message can be posted twice
     *  in a row and each still shows its own Snackbar. */
    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    fun postFeedback(message: String) {
        viewModelScope.launch { _feedback.emit(message) }
    }

    /** Studiengänge starred for quick switching (see [SettingsStore.favoriteStudiengaenge]). */
    val favorites: StateFlow<List<Studiengang>> =
        settingsStore.favoriteStudiengaenge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recurring event groups the user hid, e.g. parallel Tutorium groups they're not in — see
     *  [TimetableEvent.groupKey]. Filtering happens in the UI layer, not here, so the raw fetched
     *  week stays intact in [weekCache]/[timetableCache] regardless of what's currently hidden. */
    val hiddenGroupKeys: StateFlow<Set<String>> =
        settingsStore.hiddenEventKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Whether lecture-start reminders are turned on — see [LectureReminderWorker]. */
    val remindersEnabled: StateFlow<Boolean> =
        settingsStore.remindersEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Lead times (minutes before a lecture starts) to notify at — see [LectureReminderWorker].
     *  Several can be active at once, e.g. a 30-min heads-up and a 5-min "starting now" nudge. */
    val reminderLeadMinutes: StateFlow<Set<Int>> =
        settingsStore.reminderLeadMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), setOf(15))

    /** Whether to use Android's wallpaper-tinted Material You palette (see StundenplanTheme). */
    val dynamicColorEnabled: StateFlow<Boolean> =
        settingsStore.dynamicColorEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val themeMode: StateFlow<ThemeMode> =
        settingsStore.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val accentPreset: StateFlow<AccentPreset> =
        settingsStore.accentPreset.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentPreset.DEFAULT)

    val customAccentColor: StateFlow<Color?> =
        settingsStore.customAccentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customBackgroundColor: StateFlow<Color?> =
        settingsStore.customBackgroundColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether the app should open straight into the single-day Tag view instead of Woche. */
    val defaultViewIsDay: StateFlow<Boolean> =
        settingsStore.defaultViewIsDay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDefaultViewIsDay(isDay: Boolean) {
        viewModelScope.launch { settingsStore.setDefaultViewIsDay(isDay) }
    }

    // Live per-week results — QIS shows real per-week data (empty outside term dates, room
    // changes, cancellations), not a recurring template, so each visited week gets its own fetch.
    private val weekCache = mutableMapOf<LocalDate, List<TimetableEvent>>()
    private val weekPrefetchInFlight = mutableSetOf<LocalDate>()

    init {
        viewModelScope.launch {
            val saved = settingsStore.selectedStudiengang.first()
            if (saved != null) {
                _planState.value = _planState.value.copy(studiengang = saved)
            }
        }
        checkForUpdate()
        // Re-assert the periodic work's enqueued/cancelled state on every process start —
        // enqueueUniquePeriodicWork with KEEP is a no-op if it's already scheduled (it survives
        // process death/app restarts on its own via WorkManager's own persisted DB), so this only
        // actually does something the first time, or if it was somehow lost (e.g. app data cleared
        // without going through the toggle).
        viewModelScope.launch { updateReminderWork(settingsStore.remindersEnabled.first()) }
    }

    /**
     * Silent background check against GitHub Releases, called on every app start — but actually
     * hitting the network is throttled to once per [UPDATE_CHECK_MIN_INTERVAL_MS], so reopening the
     * app repeatedly doesn't spam GitHub's API. A failed/offline check just means no update banner
     * shows, never an error the user has to deal with.
     *
     * Skipped entirely in debug builds: local builds default to versionCode 1 (see
     * app/build.gradle.kts — only CI sets APP_VERSION_CODE), which is lower than any real release,
     * so every debug build would otherwise "find" an update on every single start.
     */
    /**
     * @param force Bypasses both the debug-build skip and the rate limit — for the explicit
     *   "Nach Updates suchen" button in Einstellungen ▸ Über die App, where the user asking right
     *   now is exactly the point, not something to throttle.
     * @param onResult Reports whether an update was found, once the check finishes — lets a manual
     *   check show "Du hast die neueste Version" instead of just doing nothing when there's none.
     */
    fun checkForUpdate(force: Boolean = false, onResult: (foundUpdate: Boolean) -> Unit = {}) {
        if (BuildConfig.DEBUG && !force) return
        viewModelScope.launch {
            if (!force) {
                val now = System.currentTimeMillis()
                val lastCheck = settingsStore.lastUpdateCheckAt.first()
                if (now - lastCheck < UPDATE_CHECK_MIN_INTERVAL_MS) return@launch
                settingsStore.setLastUpdateCheckAt(now)
            }
            try {
                val info = updateManager.checkForUpdate()
                if (info != null) _updateState.value = _updateState.value.copy(available = info)
                onResult(info != null)
            } catch (_: Exception) {
                // Ignored — see doc comment.
                onResult(false)
            }
        }
    }

    /** Fetches the latest published release's notes for the in-app changelog popup — independent
     *  of [updateState], since that only ever holds an update newer than the running build. */
    fun fetchChangelog(onResult: (UpdateInfo?) -> Unit) {
        viewModelScope.launch {
            val info = try { updateManager.fetchLatestReleaseNotes() } catch (_: Exception) { null }
            onResult(info)
        }
    }

    fun openUpdateInBrowser() {
        val info = _updateState.value.available ?: return
        updateManager.openDownloadInBrowser(info)
    }

    fun dismissUpdate() {
        _updateState.value = _updateState.value.copy(available = null)
    }

    /** Loads (or serves from cache) the real timetable for the week starting on [weekMonday]. */
    fun loadWeek(weekMonday: LocalDate) {
        val studiengang = _planState.value.studiengang ?: return
        // The week either side is the obvious next swipe target — fetch it now, quietly, so it's
        // already sitting in the cache (and the pager already has it composed, see
        // beyondBoundsPageCount below) by the time the user actually swipes there.
        prefetchWeek(weekMonday.minusWeeks(1))
        prefetchWeek(weekMonday.plusWeeks(1))

        val cached = weekCache[weekMonday]
        if (cached != null) {
            _planState.value = _planState.value.copy(
                weekMonday = weekMonday, events = cached, error = null, isOffline = false, offlineSince = null,
            )
            return
        }
        viewModelScope.launch {
            _planState.value = _planState.value.copy(weekMonday = weekMonday, isLoading = true, error = null)
            try {
                val events = repository.fetchTimetable(studiengang, weekMonday)
                weekCache[weekMonday] = events
                timetableCache.put(studiengang, weekMonday, events)
                WidgetUpdater.refresh(getApplication())
                if (_planState.value.weekMonday == weekMonday) {
                    _planState.value = _planState.value.copy(
                        events = events, isLoading = false, isOffline = false, offlineSince = null,
                    )
                }
            } catch (e: Exception) {
                if (_planState.value.weekMonday != weekMonday) return@launch
                // Live fetch failed (most likely no connection) — fall back to whatever was last
                // successfully fetched for this exact week, if anything, rather than just an error.
                val offline = timetableCache.get(studiengang, weekMonday)
                _planState.value = if (offline != null) {
                    _planState.value.copy(
                        events = offline.events, isLoading = false, error = null,
                        isOffline = true, offlineSince = offline.savedAt,
                    )
                } else {
                    _planState.value.copy(isLoading = false, error = friendlyNetworkErrorMessage(e))
                }
            }
        }
    }

    /** Fire-and-forget background fetch that only populates [weekCache] — no loading/error state,
     *  since a failed prefetch just means the real navigation later retries and surfaces it then. */
    private fun prefetchWeek(monday: LocalDate) {
        if (weekCache.containsKey(monday) || monday in weekPrefetchInFlight) return
        val studiengang = _planState.value.studiengang ?: return
        weekPrefetchInFlight += monday
        viewModelScope.launch {
            try {
                val events = repository.fetchTimetable(studiengang, monday)
                weekCache[monday] = events
                timetableCache.put(studiengang, monday, events)
            } catch (_: Exception) {
                // Ignored — see doc comment.
            } finally {
                weekPrefetchInFlight -= monday
            }
        }
    }

    fun refresh() {
        val monday = _planState.value.weekMonday ?: return
        weekCache.remove(monday)
        loadWeek(monday)
    }

    /** Shares the currently loaded week's events as an .ics file — see [buildIcs] for why one
     *  week's fetch is enough to cover the whole semester. No-op if nothing has loaded yet. */
    fun exportIcs() {
        val state = _planState.value
        val studiengang = state.studiengang ?: return
        if (state.events.isEmpty()) return
        calendarExporter.shareAsIcs(state.events, studiengang.code)
    }

    fun loadStudiengangList(forceReload: Boolean = false) {
        if (_pickerState.value.all.isNotEmpty() && !forceReload) return
        viewModelScope.launch {
            _pickerState.value = _pickerState.value.copy(isLoading = true, error = null)
            try {
                val list = repository.fetchStudiengaenge()
                _pickerState.value = _pickerState.value.copy(all = list, isLoading = false)
            } catch (e: Exception) {
                _pickerState.value = _pickerState.value.copy(
                    isLoading = false,
                    error = friendlyNetworkErrorMessage(e),
                )
            }
        }
    }

    fun setPickerQuery(query: String) {
        _pickerState.value = _pickerState.value.copy(query = query)
    }

    fun selectStudiengang(studiengang: Studiengang) {
        viewModelScope.launch {
            settingsStore.setSelectedStudiengang(studiengang)
        }
        weekCache.clear()
        weekPrefetchInFlight.clear()
        _planState.value = _planState.value.copy(
            studiengang = studiengang,
            events = emptyList(),
            weekMonday = null,
        )
    }

    fun toggleFavorite(studiengang: Studiengang) {
        viewModelScope.launch {
            val isFavorite = favorites.value.any { it.id == studiengang.id }
            settingsStore.setFavorite(studiengang, favorite = !isFavorite)
        }
    }

    fun setGroupHidden(groupKey: String, hidden: Boolean) {
        viewModelScope.launch { settingsStore.setHidden(groupKey, hidden) }
    }

    /** Turns lecture-start reminders on/off. The caller (SettingsScreen) is responsible for
     *  requesting the POST_NOTIFICATIONS runtime permission first on API 33+ — this only persists
     *  the preference and (de)schedules the worker; NotificationHelper silently no-ops each
     *  individual notification if the permission isn't actually granted. */
    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindersEnabled(enabled)
            updateReminderWork(enabled)
        }
    }

    private fun updateReminderWork(enabled: Boolean) {
        val workManager = WorkManager.getInstance(getApplication())
        if (enabled) {
            NotificationHelper.ensureChannel(getApplication())
            val request = PeriodicWorkRequestBuilder<LectureReminderWorker>(
                LectureReminderWorker.WORK_INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).build()
            workManager.enqueueUniquePeriodicWork(LectureReminderWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.cancelUniqueWork(LectureReminderWorker.WORK_NAME)
        }
    }

    fun setReminderLeadMinutes(minutes: Set<Int>) {
        viewModelScope.launch { settingsStore.setReminderLeadMinutes(minutes) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColorEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setAccentPreset(preset: AccentPreset) {
        viewModelScope.launch { settingsStore.setAccentPreset(preset) }
    }

    fun setCustomAccentColor(color: Color) {
        viewModelScope.launch {
            settingsStore.setCustomAccentColor(color)
            settingsStore.setAccentPreset(AccentPreset.CUSTOM)
        }
    }

    fun setCustomBackgroundColor(color: Color) {
        viewModelScope.launch {
            settingsStore.setCustomBackgroundColor(color)
        }
    }

    fun resetAppearance() {
        viewModelScope.launch { settingsStore.resetAppearance() }
    }

    /** Manual "Zwischenspeicher leeren" action (Einstellungen ▸ Über die App) — clears both the
     *  in-memory per-session cache and the persisted offline fallback. Always safe: the next
     *  successful live fetch repopulates both. */
    fun clearCache() {
        weekCache.clear()
        weekPrefetchInFlight.clear()
        viewModelScope.launch { timetableCache.clearAll() }
        postFeedback("Zwischenspeicher geleert.")
    }

    companion object {
        private const val UPDATE_CHECK_MIN_INTERVAL_MS = 60_000L
    }
}
