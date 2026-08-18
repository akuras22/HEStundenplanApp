package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QisParserTest {

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing resource $name" }
            .bufferedReader(Charsets.UTF_8)
            .readText()

    @Test
    fun `parses studiengang list and finds WKB semesters`() {
        val html = resource("stglist_sample.html")
        val list = QisParser.parseStudiengangList(html)

        assertTrue("expected a large catalog, got ${list.size}", list.size > 100)

        val wkb1 = list.first { it.code == "WKB1" }
        assertEquals("1007", wkb1.abstgvnr)
        assertEquals("274", wkb1.parallelid)

        val codes = list.map { it.code }
        assertTrue(codes.containsAll(listOf("WKB1", "WKB2", "WKB3", "WKB4", "WKB6", "WKB7")))
    }

    @Test
    fun `parses wplan timetable into events across weekdays`() {
        val html = resource("wplan_sample.html")
        val events = QisParser.parseTimetable(html)

        assertTrue("expected events to be parsed, got ${events.size}", events.isNotEmpty())
        assertTrue(events.all { it.startMinutes < it.endMinutes })

        val days = events.map { it.day }.toSet()
        assertTrue("expected events on multiple weekdays, got $days", days.size > 1)

        val mathA = events.first { it.title.contains("Mathematik 1 A") && it.startMinutes == 8 * 60 }
        assertEquals(Weekday.THURSDAY, mathA.day)
        assertEquals(9 * 60 + 30, mathA.endMinutes)
        assertEquals("Gebäude 01 - F 01.016", mathA.room)
        assertEquals("Gänswein", mathA.lecturer)

        val bwl = events.first { it.title.contains("BWL und VWL") && it.day == Weekday.TUESDAY }
        assertEquals("woch", bwl.frequency)
    }

    @Test
    fun `parses events sitting in taller plan22 day cells, not just plan2`() {
        // QIS bumps the day-cell class to "plan22" (and beyond, for even taller spans) once the
        // cell's rowspan grows past a threshold — e.g. a Tutorium sharing a cell with a long lecture
        // block. These must parse exactly like plain "plan2" cells.
        val html = resource("wplan_sample.html")
        val events = QisParser.parseTimetable(html)

        val tutorien = events.filter { it.title.contains("Tutorium", ignoreCase = true) }
        assertTrue("expected at least one Tutorium event to be parsed, got ${events.map { it.title }}", tutorien.isNotEmpty())
    }
}
