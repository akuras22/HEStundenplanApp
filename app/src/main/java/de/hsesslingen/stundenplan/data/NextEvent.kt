package de.hsesslingen.stundenplan.data

import java.time.LocalDate
import java.time.LocalDateTime

data class NextEventResult(val event: TimetableEvent, val date: LocalDate)

/**
 * Finds the next upcoming event at or after [now] within [events] (which should cover at least
 * [now]'s week and the following one, so a search starting on a Friday evening can still find
 * Monday's first lecture). Weekends are skipped since QIS never schedules on them. An event
 * currently in progress still counts as "next" — only ones that have fully ended are skipped.
 */
fun findNextEvent(
    events: List<TimetableEvent>,
    now: LocalDateTime,
    hiddenGroupKeys: Set<String> = emptySet(),
    daysAhead: Int = 7,
): NextEventResult? = findUpcomingEvents(events, now, hiddenGroupKeys, count = 1, daysAhead = daysAhead).firstOrNull()

/** Like [findNextEvent] but returns up to [count] events in chronological order — used by the
 *  home-screen widget's larger size variant, which has room to show more than just the next one. */
fun findUpcomingEvents(
    events: List<TimetableEvent>,
    now: LocalDateTime,
    hiddenGroupKeys: Set<String> = emptySet(),
    count: Int = 3,
    daysAhead: Int = 7,
): List<NextEventResult> {
    val nowMinutes = now.hour * 60 + now.minute
    val result = mutableListOf<NextEventResult>()
    for (offset in 0..daysAhead) {
        if (result.size >= count) break
        val date = now.toLocalDate().plusDays(offset.toLong())
        val weekday = Weekday.fromDate(date) ?: continue
        val dayEvents = events
            .filter { it.day == weekday && it.groupKey !in hiddenGroupKeys }
            .filter { offset > 0 || it.endMinutes > nowMinutes }
            .sortedBy { it.startMinutes }
        for (event in dayEvents) {
            result += NextEventResult(event, date)
            if (result.size >= count) break
        }
    }
    return result
}
