package de.hsesslingen.stundenplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.hsesslingen.stundenplan.BuildConfig
import de.hsesslingen.stundenplan.data.QisRepository
import de.hsesslingen.stundenplan.data.SettingsStore
import de.hsesslingen.stundenplan.data.Studiengang
import de.hsesslingen.stundenplan.data.TimetableCache
import de.hsesslingen.stundenplan.data.TimetableEvent
import de.hsesslingen.stundenplan.data.UpdateInfo
import de.hsesslingen.stundenplan.data.UpdateManager
import de.hsesslingen.stundenplan.data.friendlyNetworkErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    private val _planState = MutableStateFlow(PlanUiState())
    val planState: StateFlow<PlanUiState> = _planState.asStateFlow()

    private val _pickerState = MutableStateFlow(StudiengangPickerState())
    val pickerState: StateFlow<StudiengangPickerState> = _pickerState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** Studiengänge starred for quick switching (see [SettingsStore.favoriteStudiengaenge]). */
    val favorites: StateFlow<List<Studiengang>> =
        settingsStore.favoriteStudiengaenge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recurring event groups the user hid, e.g. parallel Tutorium groups they're not in — see
     *  [TimetableEvent.groupKey]. Filtering happens in the UI layer, not here, so the raw fetched
     *  week stays intact in [weekCache]/[timetableCache] regardless of what's currently hidden. */
    val hiddenGroupKeys: StateFlow<Set<String>> =
        settingsStore.hiddenEventKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
    fun checkForUpdate() {
        if (BuildConfig.DEBUG) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val lastCheck = settingsStore.lastUpdateCheckAt.first()
            if (now - lastCheck < UPDATE_CHECK_MIN_INTERVAL_MS) return@launch
            settingsStore.setLastUpdateCheckAt(now)
            try {
                val info = updateManager.checkForUpdate()
                if (info != null) _updateState.value = _updateState.value.copy(available = info)
            } catch (_: Exception) {
                // Ignored — see doc comment.
            }
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

    companion object {
        private const val UPDATE_CHECK_MIN_INTERVAL_MS = 60_000L
    }
}
