package de.hsesslingen.stundenplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import de.hsesslingen.stundenplan.data.QisRepository
import de.hsesslingen.stundenplan.data.SettingsStore
import de.hsesslingen.stundenplan.data.Studiengang
import de.hsesslingen.stundenplan.data.TimetableEvent
import de.hsesslingen.stundenplan.data.UpdateInfo
import de.hsesslingen.stundenplan.data.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanUiState(
    val studiengang: Studiengang? = null,
    val events: List<TimetableEvent> = emptyList(),
    val weekMonday: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class UpdateUiState(
    val available: UpdateInfo? = null,
    val downloadProgress: Float? = null,
    val error: String? = null,
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

    private val _planState = MutableStateFlow(PlanUiState())
    val planState: StateFlow<PlanUiState> = _planState.asStateFlow()

    private val _pickerState = MutableStateFlow(StudiengangPickerState())
    val pickerState: StateFlow<StudiengangPickerState> = _pickerState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

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

    /** Silent background check against GitHub Releases — a failed/offline check just means no
     *  update banner shows, never an error the user has to deal with. */
    fun checkForUpdate() {
        viewModelScope.launch {
            try {
                val info = updateManager.checkForUpdate()
                if (info != null) _updateState.value = _updateState.value.copy(available = info)
            } catch (_: Exception) {
                // Ignored — see doc comment.
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val info = _updateState.value.available ?: return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(downloadProgress = 0f, error = null)
            try {
                val file = updateManager.download(info) { progress ->
                    _updateState.value = _updateState.value.copy(downloadProgress = progress)
                }
                _updateState.value = _updateState.value.copy(downloadProgress = null)
                updateManager.install(file)
            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(
                    downloadProgress = null,
                    error = e.message ?: "Update konnte nicht heruntergeladen werden.",
                )
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = _updateState.value.copy(available = null)
    }

    fun canRequestInstall(): Boolean = updateManager.canRequestInstall()

    fun requestInstallPermissionIntent(): Intent = updateManager.requestInstallPermissionIntent()

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
            _planState.value = _planState.value.copy(weekMonday = weekMonday, events = cached, error = null)
            return
        }
        viewModelScope.launch {
            _planState.value = _planState.value.copy(weekMonday = weekMonday, isLoading = true, error = null)
            try {
                val events = repository.fetchTimetable(studiengang, weekMonday)
                weekCache[weekMonday] = events
                if (_planState.value.weekMonday == weekMonday) {
                    _planState.value = _planState.value.copy(events = events, isLoading = false)
                }
            } catch (e: Exception) {
                if (_planState.value.weekMonday == weekMonday) {
                    _planState.value = _planState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Stundenplan konnte nicht geladen werden.",
                    )
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
                weekCache[monday] = repository.fetchTimetable(studiengang, monday)
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
                    error = e.message ?: "Liste der Studiengänge konnte nicht geladen werden.",
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
}
