package de.hsesslingen.stundenplan.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parsing for HS Esslingen's QIS/LSF pages. The HTML is old-school table-layout markup with no
 * stable ids/classes beyond a handful of presentation classes ("plan1"/"plan2"/"notiz"/"klein"),
 * so parsing leans on structural selectors and regexes rather than ids.
 */
object QisParser {

    private val PARALLELID_RE = Regex("""k_parallel\.parallelid=(\d+)""")
    private val ABSTGVNR_RE = Regex("""k_abstgv\.abstgvnr=(\d+)""")
    private val TIME_RE = Regex("""(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})\s*(?:\(([^)]+)\))?""")

    /** Parses the public "Studiengangpläne (Liste)" catalog page into selectable entries. */
    fun parseStudiengangList(html: String): List<Studiengang> {
        val doc = Jsoup.parse(html)
        val header = doc.select("th").firstOrNull { it.text().contains("Studiengänge") } ?: return emptyList()
        val table = header.closest("table") ?: return emptyList()
        val result = mutableListOf<Studiengang>()
        for (row in table.select("tbody tr")) {
            val cells = row.select("> td")
            if (cells.size < 2) continue
            val code = cells[0].selectFirst("a")?.text()?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val planHref = cells[1].selectFirst("a")?.attr("href") ?: continue
            val abstgvnr = ABSTGVNR_RE.find(planHref)?.groupValues?.get(1) ?: continue
            val parallelid = PARALLELID_RE.find(planHref)?.groupValues?.get(1) ?: continue
            result.add(Studiengang(code = code, abstgvnr = abstgvnr, parallelid = parallelid))
        }
        return result.distinctBy { it.id }.sortedBy { it.code }
    }

    /** Parses a "wplan" weekly timetable page (show=plan, P.vx=lang) into individual events. */
    fun parseTimetable(html: String): List<TimetableEvent> {
        val doc = Jsoup.parse(html)
        // The page nests tables for full-page layout, so anchor on the actual weekday header
        // cell and walk up to its immediate table rather than matching any ancestor table.
        val montagHeader = doc.select("th").firstOrNull { it.text().contains("Montag") }
            ?: error("Stundenplan-Tabelle wurde auf der Seite nicht gefunden (unerwartetes Seitenformat).")
        val table = montagHeader.closest("table")
            ?: error("Stundenplan-Tabelle wurde auf der Seite nicht gefunden (unerwartetes Seitenformat).")
        val grid = resolveGrid(table)

        val columnToDay = mapOf(
            2 to Weekday.MONDAY,
            3 to Weekday.TUESDAY,
            4 to Weekday.WEDNESDAY,
            5 to Weekday.THURSDAY,
            6 to Weekday.FRIDAY,
        )

        val events = mutableListOf<TimetableEvent>()
        for (cell in grid) {
            // Day cells are classed "plan2", or "plan22"/"plan222"/... for taller cells (QIS bumps
            // the digit count with the rowspan) — legend/axis cells ("plan1", "plan5-7") never
            // match this. Matching only the exact "plan2" class silently dropped every event sitting
            // in one of the taller cells (e.g. Tutorien scheduled in long afternoon blocks).
            if (!cell.element.className().matches(Regex("plan2+"))) continue
            val day = columnToDay[cell.col] ?: continue
            for (eventTable in cell.element.select("> table")) {
                parseEventTable(eventTable, day)?.let { events.add(it) }
            }
        }
        return events.distinct().sortedWith(compareBy({ it.day }, { it.startMinutes }))
    }

    private fun parseEventTable(eventTable: Element, day: Weekday): TimetableEvent? {
        val title = eventTable.selectFirst("td.klein a")?.text()?.clean()
            ?: eventTable.selectFirst("td.klein")?.text()?.clean()
            ?: return null

        var startMinutes: Int? = null
        var endMinutes: Int? = null
        var frequency: String? = null
        var startDate: String? = null
        var endDate: String? = null
        var room: String? = null
        var category: String? = null
        var lecturer: String? = null

        for (notiz in eventTable.select("td.notiz")) {
            val text = notiz.text().clean()
            if (text.isBlank()) continue

            if (startMinutes == null) {
                val m = TIME_RE.find(text)
                if (m != null) {
                    startMinutes = parseTimeToMinutes(m.groupValues[1])
                    endMinutes = parseTimeToMinutes(m.groupValues[2])
                    frequency = m.groupValues.getOrNull(3)?.clean()?.ifBlank { null }
                    continue
                }
            }
            if (text.startsWith("Start:", ignoreCase = true)) {
                startDate = text.substringAfter(":").clean().ifBlank { null }
                continue
            }
            if (text.startsWith("Ende:", ignoreCase = true)) {
                endDate = text.substringAfter(":").clean().ifBlank { null }
                continue
            }
            if (text.startsWith("Dozent", ignoreCase = true)) {
                lecturer = text.substringAfter(":").clean().ifBlank { null }
                continue
            }
            if (text.startsWith("Einrichtung", ignoreCase = true)) {
                continue
            }
            val roomLink = notiz.selectFirst("a")
            if (roomLink != null && room == null) {
                room = roomLink.text().clean().ifBlank { null }
                category = text.removePrefix(roomLink.text()).clean().trim(':', ' ').ifBlank { null }
                continue
            }
        }

        if (startMinutes == null || endMinutes == null) return null

        return TimetableEvent(
            day = day,
            title = title,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            frequency = frequency,
            room = room,
            lecturer = lecturer,
            category = category,
            startDate = startDate,
            endDate = endDate,
        )
    }

    private fun parseTimeToMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        return parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
    }

    private fun String.clean(): String = this.replace(' ', ' ').trim()

    // --- HTML table grid resolution (handles rowspan/colspan) ---

    private data class GridCell(val row: Int, val col: Int, val element: Element)

    private fun resolveGrid(table: Element): List<GridCell> {
        val rows = table.select("> tbody > tr, > tr").let { direct ->
            if (direct.isNotEmpty()) direct else table.select("tr")
        }
        val occupiedUntilRow = HashMap<Int, Int>()
        val result = mutableListOf<GridCell>()

        for ((r, tr) in rows.withIndex()) {
            var c = 0
            for (cell in tr.children()) {
                if (cell.tagName() != "td" && cell.tagName() != "th") continue
                while ((occupiedUntilRow[c] ?: -1) >= r) c++
                val colspan = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                val rowspan = cell.attr("rowspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                result.add(GridCell(r, c, cell))
                for (cc in c until c + colspan) {
                    occupiedUntilRow[cc] = r + rowspan - 1
                }
                c += colspan
            }
        }
        return result
    }
}
