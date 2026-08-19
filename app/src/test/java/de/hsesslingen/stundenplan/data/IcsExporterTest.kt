package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsExporterTest {

    private fun event(
        day: Weekday = Weekday.TUESDAY,
        title: String = "WKB1 Programmieren",
        start: Int = 9 * 60 + 45,
        end: Int = 13 * 60,
        frequency: String? = "woch",
        room: String? = "Gebäude 01 - F 01.015",
        lecturer: String? = "Rodach",
        startDate: String? = "29.9.2026",
        endDate: String? = "19.1.2027",
    ) = TimetableEvent(
        day = day, title = title, startMinutes = start, endMinutes = end, frequency = frequency,
        room = room, lecturer = lecturer, category = null, startDate = startDate, endDate = endDate,
    )

    @Test
    fun `weekly event produces a VEVENT anchored to its first real occurrence`() {
        // 29.9.2026 is a Tuesday, matching the event's own weekday, so DTSTART should land exactly there.
        val ics = buildIcs(listOf(event()), "WKB1")

        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("DTSTART:20260929T094500"))
        assertTrue(ics.contains("DTEND:20260929T130000"))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;INTERVAL=1;UNTIL=20270119T130000"))
        assertTrue(ics.contains("SUMMARY:WKB1 Programmieren"))
        assertTrue(ics.contains("LOCATION:Gebäude 01 - F 01.015"))
        assertTrue(ics.contains("DESCRIPTION:Dozent: Rodach"))
    }

    @Test
    fun `anchor date advances to the event's own weekday when the semester start date differs`() {
        // Start date given as a Monday, but the event itself is on Wednesday.
        val ics = buildIcs(listOf(event(day = Weekday.WEDNESDAY, startDate = "28.9.2026")), "WKB1")

        assertTrue(ics.contains("DTSTART:20260930T"))
    }

    @Test
    fun `14-taegl frequency maps to a biweekly interval`() {
        val ics = buildIcs(listOf(event(frequency = "14-tägl")), "WKB1")

        assertTrue(ics.contains("INTERVAL=2"))
    }

    @Test
    fun `event without a start date is skipped rather than crashing`() {
        val ics = buildIcs(listOf(event(startDate = null)), "WKB1")

        assertFalse(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
    }

    @Test
    fun `missing end date falls back to a bounded recurrence instead of running forever`() {
        val ics = buildIcs(listOf(event(endDate = null)), "WKB1")

        assertTrue(ics.contains("UNTIL="))
    }

    @Test
    fun `special characters in title are escaped per RFC 5545`() {
        val ics = buildIcs(listOf(event(title = "BWL; VWL, Teil 1")), "WKB1")

        assertTrue(ics.contains("SUMMARY:BWL\\; VWL\\, Teil 1"))
    }

    @Test
    fun `calendar name is set from the passed-in title`() {
        val ics = buildIcs(listOf(event()), "WKB2")

        assertTrue(ics.contains("X-WR-CALNAME:WKB2"))
    }
}
