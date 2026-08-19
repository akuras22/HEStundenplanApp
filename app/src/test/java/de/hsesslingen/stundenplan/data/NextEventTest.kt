package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NextEventTest {

    private fun event(day: Weekday, start: Int, end: Int, title: String = "x", lecturer: String? = null) = TimetableEvent(
        day = day, title = title, startMinutes = start, endMinutes = end, frequency = null,
        room = null, lecturer = lecturer, category = null, startDate = null, endDate = null,
    )

    // Tuesday, 28.9.2026 (matches the real-world dates used elsewhere in this suite).
    private val tuesday: LocalDate = LocalDate.of(2026, 9, 29)

    @Test
    fun `finds a later event today`() {
        val now = LocalDateTime.of(tuesday, LocalTime.of(10, 0))
        val morning = event(Weekday.TUESDAY, 8 * 60, 9 * 60 + 30, title = "already over")
        val afternoon = event(Weekday.TUESDAY, 14 * 60, 15 * 60 + 30, title = "next")

        val result = findNextEvent(listOf(morning, afternoon), now)

        assertEquals("next", result?.event?.title)
        assertEquals(tuesday, result?.date)
    }

    @Test
    fun `an event currently in progress still counts as next`() {
        val now = LocalDateTime.of(tuesday, LocalTime.of(9, 0))
        val inProgress = event(Weekday.TUESDAY, 8 * 60, 9 * 60 + 30)

        val result = findNextEvent(listOf(inProgress), now)

        assertEquals(inProgress, result?.event)
    }

    @Test
    fun `an event that already ended today is skipped, not just re-found next week`() {
        val now = LocalDateTime.of(tuesday, LocalTime.of(10, 0))
        val ended = event(Weekday.TUESDAY, 8 * 60, 9 * 60 + 30)

        // daysAhead=3 stays within this week (Tue/Wed/Thu/Fri) so this actually tests "skipped for
        // today", rather than the intentional weekly-recurrence wraparound tested separately below.
        assertNull(findNextEvent(listOf(ended), now, daysAhead = 3))
    }

    @Test
    fun `with a wide enough daysAhead, a weekly-recurring event is found again next week`() {
        // Events represent a weekly-recurring slot (just "TUESDAY", not a specific date), so once
        // today's occurrence has passed, the default 7-day lookahead should find next week's.
        val now = LocalDateTime.of(tuesday, LocalTime.of(10, 0))
        val weekly = event(Weekday.TUESDAY, 8 * 60, 9 * 60 + 30)

        val result = findNextEvent(listOf(weekly), now)

        assertEquals(tuesday.plusWeeks(1), result?.date)
    }

    @Test
    fun `falls through to the next weekday when today has nothing left`() {
        val now = LocalDateTime.of(tuesday, LocalTime.of(20, 0))
        val wednesdayEvent = event(Weekday.WEDNESDAY, 8 * 60, 9 * 60 + 30, title = "wednesday")

        val result = findNextEvent(listOf(wednesdayEvent), now)

        assertEquals("wednesday", result?.event?.title)
        assertEquals(tuesday.plusDays(1), result?.date)
    }

    @Test
    fun `a friday evening search skips the weekend into next week`() {
        val friday = tuesday.plusDays(3) // 2.10.2026
        val now = LocalDateTime.of(friday, LocalTime.of(20, 0))
        val nextMonday = event(Weekday.MONDAY, 8 * 60, 9 * 60 + 30, title = "monday")

        val result = findNextEvent(listOf(nextMonday), now, daysAhead = 7)

        assertEquals("monday", result?.event?.title)
        assertEquals(friday.plusDays(3), result?.date) // the following Monday
    }

    @Test
    fun `hidden groups are excluded`() {
        val now = LocalDateTime.of(tuesday, LocalTime.of(9, 0))
        val hidden = event(Weekday.TUESDAY, 10 * 60, 11 * 60, title = "hidden", lecturer = "X")
        val visible = event(Weekday.TUESDAY, 12 * 60, 13 * 60, title = "visible")

        val result = findNextEvent(listOf(hidden, visible), now, hiddenGroupKeys = setOf(hidden.groupKey))

        assertEquals("visible", result?.event?.title)
    }

    @Test
    fun `no events at all returns null`() {
        assertNull(findNextEvent(emptyList(), LocalDateTime.of(tuesday, LocalTime.NOON)))
    }
}
