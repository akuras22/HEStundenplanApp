package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlapLayoutTest {

    private fun event(day: Weekday = Weekday.MONDAY, start: Int, end: Int, title: String = "$start-$end") =
        TimetableEvent(
            day = day,
            title = title,
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
    fun `non-overlapping events each get their own full-width column`() {
        val a = event(start = 8 * 60, end = 9 * 60 + 30)
        val b = event(start = 10 * 60, end = 11 * 60)

        val slots = layoutOverlaps(listOf(a, b))

        assertEquals(2, slots.size)
        slots.forEach { assertEquals(1, it.columnCount) }
        assertEquals(0, slots.first { it.event == a }.column)
        assertEquals(0, slots.first { it.event == b }.column)
    }

    @Test
    fun `back-to-back events that only touch do not count as overlapping`() {
        val a = event(start = 8 * 60, end = 9 * 60)
        val b = event(start = 9 * 60, end = 10 * 60)

        val slots = layoutOverlaps(listOf(a, b))

        slots.forEach { assertEquals(1, it.columnCount) }
    }

    @Test
    fun `two overlapping events split into two columns`() {
        val bwl = event(start = 14 * 60, end = 15 * 60 + 30, title = "BWL")
        val tutorium = event(start = 14 * 60, end = 19 * 60, title = "Tutorium")

        val slots = layoutOverlaps(listOf(bwl, tutorium))

        assertEquals(2, slots.size)
        assertEquals(2, slots[0].columnCount)
        assertEquals(2, slots[1].columnCount)
        assertEquals(setOf(0, 1), slots.map { it.column }.toSet())
    }

    @Test
    fun `three simultaneous parallel groups split into three columns`() {
        val groupA = event(start = 15 * 60 + 45, end = 19 * 60, title = "Tutorium C - Askar")
        val groupB = event(start = 15 * 60 + 45, end = 19 * 60, title = "Tutorium C - Daas")
        val groupC = event(start = 15 * 60 + 45, end = 19 * 60, title = "Tutorium C - Aslan")

        val slots = layoutOverlaps(listOf(groupA, groupB, groupC))

        assertEquals(3, slots.size)
        slots.forEach { assertEquals(3, it.columnCount) }
        assertEquals(setOf(0, 1, 2), slots.map { it.column }.toSet())
    }

    @Test
    fun `an event fully containing another still only needs two columns`() {
        val long = event(start = 9 * 60 + 45, end = 13 * 60, title = "Einfuehrung")
        val short = event(start = 10 * 60, end = 11 * 60, title = "Kurz")

        val slots = layoutOverlaps(listOf(long, short))

        assertEquals(2, slots.size)
        slots.forEach { assertEquals(2, it.columnCount) }
    }

    @Test
    fun `a later event reuses a column freed by an earlier one in the same cluster`() {
        // A(8-9) and B(8:30-9:30) overlap -> 2 columns. C(9:15-10) overlaps B only, but A has
        // already ended by then, so C should reuse A's column instead of needing a 3rd.
        val a = event(start = 8 * 60, end = 9 * 60, title = "A")
        val b = event(start = 8 * 60 + 30, end = 9 * 60 + 30, title = "B")
        val c = event(start = 9 * 60 + 15, end = 10 * 60, title = "C")

        val slots = layoutOverlaps(listOf(a, b, c))

        assertEquals(3, slots.size)
        slots.forEach { assertEquals(2, it.columnCount) }
        val byTitle = slots.associateBy { it.event.title }
        assertEquals(byTitle.getValue("A").column, byTitle.getValue("C").column)
        assertEquals(0, slots.map { it.column }.min())
        assertEquals(1, slots.map { it.column }.max())
    }

    @Test
    fun `empty input produces no slots`() {
        assertEquals(0, layoutOverlaps(emptyList()).size)
    }
}
