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
): NextEventResult? {
    val nowMinutes = now.hour * 60 + now.minute
    for (offset in 0..daysAhead) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val weekday = Weekday.fromDate(date) ?: continue
        val next = events
            .filter { it.day == weekday && it.groupKey !in hiddenGroupKeys }
            .filter { offset > 0 || it.endMinutes > nowMinutes }
            .minByOrNull { it.startMinutes }
        if (next != null) return NextEventResult(next, date)
    }
    return null
}
