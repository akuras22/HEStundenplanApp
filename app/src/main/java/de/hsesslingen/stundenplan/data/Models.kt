package de.hsesslingen.stundenplan.data

import java.time.DayOfWeek
import java.time.LocalDate

/** One selectable Studiengang/Semester timetable, as listed on the QIS "Studiengangpläne" page. */
data class Studiengang(
    val code: String,
    val abstgvnr: String,
    val parallelid: String,
) {
    val id: String get() = "$abstgvnr-$parallelid"
}

enum class Weekday(val germanLabel: String, val javaDayOfWeek: DayOfWeek) {
    MONDAY("Montag", DayOfWeek.MONDAY),
    TUESDAY("Dienstag", DayOfWeek.TUESDAY),
    WEDNESDAY("Mittwoch", DayOfWeek.WEDNESDAY),
    THURSDAY("Donnerstag", DayOfWeek.THURSDAY),
    FRIDAY("Freitag", DayOfWeek.FRIDAY);

    companion object {
        fun fromDate(date: LocalDate): Weekday? = entries.firstOrNull { it.javaDayOfWeek == date.dayOfWeek }
    }
}

/** A single lecture/tutorial block as shown on the weekly timetable. */
data class TimetableEvent(
    val day: Weekday,
    val title: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val frequency: String?,
    val room: String?,
    val lecturer: String?,
    val category: String?,
    val startDate: String?,
    val endDate: String?,
) {
    val startLabel: String get() = formatMinutes(startMinutes)
    val endLabel: String get() = formatMinutes(endMinutes)

    /** Whether this recurring event's weekday matches [date] (the page is a weekly template, not per-date data). */
    fun appliesOn(date: LocalDate): Boolean = day == Weekday.fromDate(date)

    /** Stable identity for "this recurring group" across weeks — e.g. one specific parallel
     *  Tutorium group — so the user can hide groups they're not actually in (see SettingsStore's
     *  hiddenEventKeys). Lecturer is the discriminator, not room, since QIS shows real per-week
     *  data and rooms can change week to week while the lecturer/group generally doesn't; falls
     *  back to room, then day+time, when there's no lecturer listed at all. */
    val groupKey: String
        get() = "$title|${lecturer ?: room ?: "$day-$startMinutes"}"

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
