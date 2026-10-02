package de.hsesslingen.stundenplan.data

import java.time.Duration
import java.time.Instant

/** A VVS stop. [id] is its global id ("de:08116:3998"), [name] the full name with the town
 *  ("Esslingen (N), Hochschulzentrum"), [shortName] the stop alone ("Hochschulzentrum"). */
data class TransitStop(val id: String, val name: String, val shortName: String)

/** The stops next to the campuses, offered up front in every stop picker. */
object TransitStops {
    data class Preset(val stop: TransitStop, val campus: String)

    val presets: List<Preset> = listOf(
        Preset(TransitStop("de:08116:3998", "Esslingen (N), Hochschulzentrum", "Hochschulzentrum"), "Campus Flandernstraße"),
        Preset(TransitStop("de:08116:4022", "Esslingen (N), Hochschule", "Hochschule"), "Campus Stadtmitte"),
        Preset(TransitStop("de:08116:7800", "Esslingen (N), Bahnhof", "Esslingen Bahnhof"), "S-Bahn & Regionalzüge"),
        Preset(TransitStop("de:08117:3301", "Göppingen, Hochschule", "Hochschule Göppingen"), "Campus Göppingen"),
        Preset(TransitStop("de:08117:154", "Göppingen, Bahnhof", "Göppingen Bahnhof"), "Regionalzüge"),
    )

    /** The campus stop matching a chosen Mensa, so the Abfahrten tab can start somewhere sensible
     *  for anyone who already told the app where they study. */
    fun forMensaLocation(mensaLocationId: Int?): TransitStop? = when (mensaLocationId) {
        6 -> presets[0].stop
        9 -> presets[1].stop
        13 -> presets[3].stop
        else -> null
    }
}

data class TransitNotice(val title: String, val text: String)

data class Departure(
    /** "S1", "105", "MEX18". */
    val line: String,
    /** EFA product class: 0 train, 1 S-Bahn, 2 U-Bahn, 3 Stadtbahn, 4 tram, 5–7 bus, … */
    val productClass: Int,
    val destination: String,
    val planned: Instant,
    /** Real-time departure, when the vehicle reports one. */
    val estimated: Instant?,
    /** "Gleis 8", "Steig 3". */
    val platform: String?,
    val isCancelled: Boolean,
    val notices: List<TransitNotice>,
) {
    /** When it actually leaves — the real-time estimate if there is one. */
    val expected: Instant get() = estimated ?: planned

    /** Minutes late per real-time data; null when there's no real-time data for this trip. */
    val delayMinutes: Long? get() = estimated?.let { Duration.between(planned, it).toMinutes() }

    val isTrain: Boolean get() = productClass == 0 || productClass == 1
}
