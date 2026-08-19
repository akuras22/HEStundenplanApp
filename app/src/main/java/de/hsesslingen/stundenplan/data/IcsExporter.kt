package de.hsesslingen.stundenplan.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val QIS_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY)
private val ICS_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

private fun parseQisDate(raw: String?): LocalDate? =
    raw?.trim()?.takeIf { it.isNotBlank() }?.let {
        try {
            LocalDate.parse(it, QIS_DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            null
        }
    }

/** First date on/after [from] that falls on [day] — used to anchor a recurring event's DTSTART to
 *  its actual first real occurrence instead of just its semester start date (which is rarely the
 *  right weekday). */
private fun firstOccurrenceOnOrAfter(from: LocalDate, day: DayOfWeek): LocalDate {
    var date = from
    while (date.dayOfWeek != day) date = date.plusDays(1)
    return date
}

/**
 * Builds an iCalendar (.ics) file from a single fetched week's events. This works for the WHOLE
 * semester from just one week's fetch because QIS already reports each event's full Start/Ende
 * dates and Turnus (woch/14-tägl) on every week's page, not just the visited one — so each event
 * becomes a proper RRULE-recurring VEVENT rather than a one-off for the visited week.
 *
 * Deliberately uses floating local time (no VTIMEZONE/TZID) rather than UTC: correctly converting
 * Europe/Berlin wall-clock times (with their DST transitions) to UTC requires embedding a full
 * VTIMEZONE block, and every realistic user of this HS Esslingen-specific export is themselves in
 * that same timezone anyway, so floating time renders identically for everyone who'd ever import it.
 */
fun buildIcs(events: List<TimetableEvent>, calendarName: String): String {
    val lines = mutableListOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//HEStundenplan//DE",
        "CALSCALE:GREGORIAN",
        "X-WR-CALNAME:${escapeIcsText(calendarName)}",
    )

    events.forEach { event ->
        val start = parseQisDate(event.startDate) ?: return@forEach
        val end = parseQisDate(event.endDate) ?: start.plusMonths(6)
        val firstDate = firstOccurrenceOnOrAfter(start, event.day.javaDayOfWeek)
        val dtStart = LocalDateTime.of(firstDate, minutesToLocalTime(event.startMinutes))
        val dtEnd = LocalDateTime.of(firstDate, minutesToLocalTime(event.endMinutes))
        val untilDateTime = LocalDateTime.of(end, minutesToLocalTime(event.endMinutes))
        val interval = if (event.frequency?.contains("14") == true) 2 else 1
        val uid = "%08x-%s".format(
            "${event.title}|${event.day}|${event.startMinutes}|${event.startDate}".hashCode(),
            "hestundenplan.local",
        )

        lines += "BEGIN:VEVENT"
        lines += "UID:$uid"
        lines += "DTSTART:${dtStart.format(ICS_DATE_TIME_FORMAT)}"
        lines += "DTEND:${dtEnd.format(ICS_DATE_TIME_FORMAT)}"
        lines += "RRULE:FREQ=WEEKLY;INTERVAL=$interval;UNTIL=${untilDateTime.format(ICS_DATE_TIME_FORMAT)}"
        lines += "SUMMARY:${escapeIcsText(event.title)}"
        event.room?.let { lines += "LOCATION:${escapeIcsText(it)}" }
        val description = listOfNotNull(
            event.lecturer?.let { "Dozent: $it" },
            event.category,
        ).joinToString("\\n")
        if (description.isNotBlank()) lines += "DESCRIPTION:${escapeIcsText(description)}"
        lines += "END:VEVENT"
    }

    lines += "END:VCALENDAR"
    // iCalendar lines must be CRLF-terminated per RFC 5545.
    return lines.joinToString("\r\n")
}

private fun minutesToLocalTime(minutes: Int) = java.time.LocalTime.of(minutes / 60, minutes % 60)

private fun escapeIcsText(text: String): String =
    text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
