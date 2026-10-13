package de.hsesslingen.stundenplan.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single hidden lectures, as opposed to whole hidden groups ([TimetableEvent.groupKey]): "this
 * slot on this exact date", e.g. the Wednesday 08:00 Mathe lecture on 14.10. only, because the
 * Vorlesungskalender says "Keine Vorlesung" that day. Several dates per slot can be hidden at
 * once. Stored as "yyyy-MM-dd@startMinutes@groupKey" — date and start time first since the
 * groupKey itself may contain any character. The start time is part of the key because one group
 * often meets twice a week (or twice a day) and only one of those slots is cancelled.
 */
data class HiddenOccurrence(val date: LocalDate, val startMinutes: Int, val groupKey: String) {
    val key: String get() = "$date@$startMinutes@$groupKey"

    companion object {
        fun parse(key: String): HiddenOccurrence? {
            val parts = key.split("@", limit = 3)
            if (parts.size != 3) return null
            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null
            return HiddenOccurrence(date, minutes, parts[2])
        }
    }
}

fun TimetableEvent.occurrenceKey(date: LocalDate): String = HiddenOccurrence(date, startMinutes, groupKey).key

/** Whether this event is hidden on [date], either as a whole group or just that one occurrence. */
fun TimetableEvent.isHiddenOn(date: LocalDate, hiddenGroupKeys: Set<String>, hiddenOccurrenceKeys: Set<String>): Boolean =
    groupKey in hiddenGroupKeys || (hiddenOccurrenceKeys.isNotEmpty() && occurrenceKey(date) in hiddenOccurrenceKeys)

private val QIS_DATE = DateTimeFormatter.ofPattern("d.M.yyyy")

private fun parseQisDate(raw: String?): LocalDate? = raw?.let { runCatching { LocalDate.parse(it.trim(), QIS_DATE) }.getOrNull() }

/**
 * Every date this weekly slot can fall on, for picking which ones to hide: between its QIS
 * "Start"/"Ende" dates when those are known, otherwise a semester's worth of weeks around [around]
 * (the occurrence the user tapped). Dates already in the past are left out, except [around] itself.
 */
fun TimetableEvent.candidateDates(around: LocalDate, today: LocalDate = LocalDate.now()): List<LocalDate> {
    val weekday = day.javaDayOfWeek
    val first = parseQisDate(startDate) ?: around.minusWeeks(4)
    val last = parseQisDate(endDate)?.takeIf { !it.isBefore(first) } ?: around.plusWeeks(20)
    var date = first.with(java.time.temporal.TemporalAdjusters.nextOrSame(weekday))
    val result = mutableListOf<LocalDate>()
    val earliest = minOf(around, today)
    while (!date.isAfter(last) && result.size < 60) {
        if (!date.isBefore(earliest)) result += date
        date = date.plusWeeks(1)
    }
    if (around.dayOfWeek == weekday && around !in result) result += around
    return result.sorted()
}
