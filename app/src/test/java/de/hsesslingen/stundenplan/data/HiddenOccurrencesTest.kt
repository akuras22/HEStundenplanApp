package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HiddenOccurrencesTest {

    private fun event(day: Weekday, start: Int, startDate: String? = null, endDate: String? = null) = TimetableEvent(
        day = day, title = "Mathematik 1B", startMinutes = start, endMinutes = start + 90, frequency = null,
        room = null, lecturer = "Basler", category = null, startDate = startDate, endDate = endDate,
    )

    private val wed = LocalDate.of(2026, 10, 14)

    @Test
    fun `key round-trips even with separators in the group key`() {
        val occurrence = HiddenOccurrence(wed, 480, "A@B|C")
        assertEquals(occurrence, HiddenOccurrence.parse(occurrence.key))
        assertNull(HiddenOccurrence.parse("garbage"))
    }

    @Test
    fun `hides only the matching slot on the matching date`() {
        val wedLecture = event(Weekday.WEDNESDAY, 480)
        val hidden = setOf(wedLecture.occurrenceKey(wed))
        assertTrue(wedLecture.isHiddenOn(wed, emptySet(), hidden))
        assertFalse(wedLecture.isHiddenOn(wed.plusWeeks(1), emptySet(), hidden))
        // Same group, other slot (Thursday 09:45) stays visible.
        assertFalse(event(Weekday.THURSDAY, 585).isHiddenOn(wed.plusDays(1), emptySet(), hidden))
    }

    @Test
    fun `next event skips a hidden occurrence`() {
        val wedLecture = event(Weekday.WEDNESDAY, 480)
        val now = LocalDateTime.of(wed, LocalTime.of(7, 0))
        val result = findNextEvent(listOf(wedLecture), now, daysAhead = 7, hiddenOccurrenceKeys = setOf(wedLecture.occurrenceKey(wed)))
        assertEquals(wed.plusWeeks(1), result?.date)
    }

    @Test
    fun `candidate dates follow the QIS start and end dates`() {
        val lecture = event(Weekday.WEDNESDAY, 480, startDate = "30.9.2026", endDate = "20.1.2027")
        val dates = lecture.candidateDates(around = wed, today = LocalDate.of(2026, 9, 1))
        assertEquals(LocalDate.of(2026, 9, 30), dates.first())
        assertEquals(LocalDate.of(2027, 1, 20), dates.last())
        assertEquals(17, dates.size)
    }

    @Test
    fun `candidate dates leave out the past but keep the tapped date`() {
        val lecture = event(Weekday.WEDNESDAY, 480, startDate = "30.9.2026", endDate = "20.1.2027")
        val dates = lecture.candidateDates(around = wed, today = LocalDate.of(2026, 10, 20))
        assertEquals(wed, dates.first())
    }
}
