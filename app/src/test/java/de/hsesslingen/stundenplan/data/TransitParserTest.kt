package de.hsesslingen.stundenplan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TransitParserTest {

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing resource $name" }
            .bufferedReader(Charsets.UTF_8)
            .readText()

    @Test
    fun `parses real departures with real-time delays and platforms`() {
        val departures = TransitParser.parseDepartures(resource("vvs_departures_sample.json"))

        assertEquals(12, departures.size)
        val first = departures.first()
        assertEquals("S1", first.line)
        assertEquals(1, first.productClass)
        assertTrue(first.isTrain)
        assertEquals("Kirchheim (T)", first.destination)
        assertEquals("Gleis 8", first.platform)
        assertEquals(Instant.parse("2026-10-02T13:13:00Z"), first.planned)
        assertEquals(Instant.parse("2026-10-02T13:28:00Z"), first.estimated)
        assertEquals(15L, first.delayMinutes)
        assertFalse(first.isCancelled)
        // The station's lift notice is attached to every departure from it.
        assertTrue(first.notices.any { it.title.contains("Aufzug") })
    }

    @Test
    fun `bus departures get a Steig, lose the town prefix and honour cancellations`() {
        val json = """
            {"locations":[{"id":"de:08116:3998","name":"Esslingen (N), Hochschulzentrum","type":"stop",
              "parent":{"name":"Esslingen (N)","type":"locality"}}],
             "stopEvents":[{"departureTimePlanned":"2026-10-02T13:28:00Z",
               "realtimeStatus":["TRIP_CANCELLED"],
               "location":{"properties":{"platform":"3"}},
               "transportation":{"disassembledName":"105","number":"105","product":{"class":5,"name":"Bus"},
                 "destination":{"name":"Esslingen (N) ZOB"}}}]}
        """.trimIndent()
        val bus = TransitParser.parseDepartures(json).single()
        assertEquals("105", bus.line)
        assertEquals("ZOB", bus.destination)
        assertEquals("Steig 3", bus.platform)
        assertTrue(bus.isCancelled)
        assertNull(bus.estimated)
        assertNull(bus.delayMinutes)
        assertFalse(bus.isTrain)
    }

    @Test
    fun `a late departure moves below punctual ones that leave earlier`() {
        fun event(planned: String, estimated: String?, line: String) = """
            {"departureTimePlanned":"$planned",${estimated?.let { "\"departureTimeEstimated\":\"$it\"," } ?: ""}
             "transportation":{"disassembledName":"$line","product":{"class":5},"destination":{"name":"ZOB"}}}
        """
        val json = """{"stopEvents":[${event("2026-10-02T13:47:00Z", "2026-10-02T14:03:00Z", "102")},
            ${event("2026-10-02T14:02:00Z", "2026-10-02T14:06:00Z", "103")},
            ${event("2026-10-02T14:03:00Z", null, "111")}]}"""
        assertEquals(listOf("102", "111", "103"), TransitParser.parseDepartures(json).map { it.line })
    }

    @Test
    fun `parses the stop finder`() {
        val stops = TransitParser.parseStops(resource("vvs_stopfinder_sample.json"))
        assertEquals(TransitStop("de:08116:7800", "Esslingen (N), Bahnhof", "Bahnhof"), stops.first())
    }

    @Test
    fun `empty answers parse to nothing`() {
        assertTrue(TransitParser.parseDepartures("""{"stopEvents":[]}""").isEmpty())
        assertTrue(TransitParser.parseStops("""{}""").isEmpty())
    }
}
