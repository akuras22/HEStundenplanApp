package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DayWindowTest {

    private fun event(start: Int, end: Int) = TimetableEvent(
        day = Weekday.MONDAY,
        title = "x",
        startMinutes = start,
        endMinutes = end,
        frequency = null,
        room = null,
        lecturer = null,
        category = null,
        startDate = null,
        endDate = null,
    )

    @Test
    fun `empty week falls back to the default window`() {
        assertEquals(DAY_START_MINUTES_DEFAULT to DAY_END_MINUTES_DEFAULT, dayWindowFor(emptyList()))
    }

    @Test
    fun `a normal week within the default window is left untouched`() {
        val events = listOf(event(9 * 60, 10 * 60 + 30), event(14 * 60, 15 * 60))
        assertEquals(DAY_START_MINUTES_DEFAULT to DAY_END_MINUTES_DEFAULT, dayWindowFor(events))
    }

    @Test
    fun `an evening event extends the end of the window to cover it`() {
        // Real case found on QIS: a block running 17:30-21:00, which the old fixed 08:00-19:00
        // grid silently clipped off entirely.
        val events = listOf(event(17 * 60 + 30, 21 * 60))
        val (start, end) = dayWindowFor(events)
        assertEquals(DAY_START_MINUTES_DEFAULT, start)
        assertEquals(21 * 60, end)
    }

    @Test
    fun `an early event extends the start of the window to cover it`() {
        val events = listOf(event(6 * 60 + 15, 7 * 60 + 30))
        val (start, end) = dayWindowFor(events)
        assertEquals(6 * 60, start)
        assertEquals(DAY_END_MINUTES_DEFAULT, end)
    }

    @Test
    fun `window bounds always land on the hour`() {
        val events = listOf(event(6 * 60 + 45, 20 * 60 + 5))
        val (start, end) = dayWindowFor(events)
        assertEquals(0, start % 60)
        assertEquals(0, end % 60)
        assertEquals(6 * 60, start)
        assertEquals(21 * 60, end)
    }
}
