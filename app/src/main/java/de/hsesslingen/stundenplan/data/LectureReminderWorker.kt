package de.hsesslingen.stundenplan.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Runs roughly every [WORK_INTERVAL_MINUTES] (WorkManager's periodic minimum, and also how far
 * ahead a lecture must be to get caught) while reminders are enabled, and notifies for any lecture
 * of the selected Studiengang starting soon today. Best-effort, not to-the-minute precise —
 * WorkManager periodic work is subject to battery/Doze scheduling slack, not an exact alarm, which
 * is the right trade-off for a "heads up" reminder rather than something safety-critical.
 */
class LectureReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        if (!settingsStore.remindersEnabled.first()) return Result.success()
        val studiengang = settingsStore.selectedStudiengang.first() ?: return Result.success()

        val today = LocalDate.now()
        val weekday = Weekday.fromDate(today) ?: return Result.success() // weekend: nothing scheduled
        val monday = today.with(DayOfWeek.MONDAY)

        val events = try {
            QisRepository().fetchTimetable(studiengang, monday)
        } catch (_: Exception) {
            TimetableCache(applicationContext).get(studiengang, monday)?.events ?: return Result.retry()
        }

        val hiddenKeys = settingsStore.hiddenEventKeys.first()
        val leadMinutes = settingsStore.reminderLeadMinutes.first()
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        // A small grace window on both sides absorbs WorkManager's scheduling slack: -5 catches a
        // run that fired a bit late (the lecture just started), +leadMinutes+buffer ensures every
        // lecture gets caught by at least one run even if consecutive runs don't land on perfectly
        // adjacent boundaries. leadMinutes should stay >= WORK_INTERVAL_MINUTES (enforced by the UI
        // picker's option range) so no run's window has a gap before the next one's.
        val window = (nowMinutes - 5)..(nowMinutes + leadMinutes + 5)

        events
            .filter { it.day == weekday && it.groupKey !in hiddenKeys && it.startMinutes in window }
            .forEach { event ->
                if (!settingsStore.hasNotifiedToday(dateKey, event.groupKey)) {
                    NotificationHelper.notifyUpcoming(applicationContext, event, notificationId = event.groupKey.hashCode())
                    settingsStore.markNotifiedToday(dateKey, event.groupKey)
                }
            }
        return Result.success()
    }

    companion object {
        const val WORK_INTERVAL_MINUTES = 15L
        const val WORK_NAME = "lecture_reminder_worker"
    }
}
