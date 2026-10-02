package de.hsesslingen.stundenplan.data

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.tan

/**
 * Where a QIS room actually is. QIS names rooms "Gebäude 01 - F 01.406": campus letter (S =
 * Stadtmitte, F = Flandernstraße, G = Göppingen, W = Weststadt), building number, then the room
 * number, whose first digit is the floor ("-1" = Untergeschoss). QIS itself knows the campus and
 * street of each building but no coordinates, so those come from OpenStreetMap (buildings tagged
 * "Gebäude N" on each campus) and are fixed here — HS Esslingen doesn't move buildings often.
 */
data class RoomLocation(
    /** "F 01.406" — the part you'd find on the door sign. */
    val room: String,
    val campusName: String,
    /** "Gebäude F 01". */
    val buildingName: String,
    /** "4. Obergeschoss", "Erdgeschoss", … — null when the room number has no floor digit. */
    val floorLabel: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    /** True when only the street (not the building itself) is known — the pin is then just near it. */
    val isApproximate: Boolean,
)

private data class CampusBuilding(val latitude: Double, val longitude: Double, val address: String, val isApproximate: Boolean = false)

object CampusLocations {

    private val CAMPUS_NAMES = mapOf(
        'S' to "Campus Esslingen Stadtmitte",
        'F' to "Campus Esslingen Flandernstraße",
        'G' to "Campus Göppingen",
        'W' to "Campus Esslingen Weststadt",
    )

    // Keyed by campus letter + two-digit building number (+ suffix, e.g. "S13A"). Coordinates are
    // each building's centre in OpenStreetMap; addresses from QIS's building pages and OSM.
    private val BUILDINGS = mapOf(
        "S01" to CampusBuilding(48.738052, 9.311017, "Kanalstraße 33, Esslingen"),
        "S02" to CampusBuilding(48.738153, 9.311370, "Mühlstraße 1, Esslingen"),
        "S03" to CampusBuilding(48.738473, 9.311351, "Mühlstraße, Esslingen"),
        "S04" to CampusBuilding(48.738430, 9.311853, "Mühlstraße 5, Esslingen"),
        "S05" to CampusBuilding(48.738434, 9.312406, "Mühlstraße 7, Esslingen"),
        "S06" to CampusBuilding(48.738636, 9.311785, "Obertorstraße 16, Esslingen"),
        "S07" to CampusBuilding(48.738137, 9.312203, "Mühlstraße 3, Esslingen"),
        "S08" to CampusBuilding(48.738566, 9.312616, "Mühlstraße 9, Esslingen"),
        "S09" to CampusBuilding(48.738640, 9.310751, "Kanalstraße 31, Esslingen"),
        "S10" to CampusBuilding(48.739003, 9.310273, "Kanalstraße 29, Esslingen"),
        "S12" to CampusBuilding(48.737598, 9.312810, "Neckarstraße 67, Esslingen"),
        "S13" to CampusBuilding(48.737161, 9.312694, "Neckarstraße 63, Esslingen"),
        "S13A" to CampusBuilding(48.737161, 9.312694, "Neckarstraße, Esslingen", isApproximate = true),
        "S14" to CampusBuilding(48.737167, 9.313030, "Neckarstraße 65/1, Esslingen"),
        "S15" to CampusBuilding(48.737949, 9.312906, "Neckarstraße 67/1, Esslingen"),
        "S16" to CampusBuilding(48.738641, 9.310159, "Kanalstraße 27, Esslingen"),
        "S17" to CampusBuilding(48.738558, 9.309825, "Kanalstraße 12/1, Esslingen"),
        "S18" to CampusBuilding(48.737400, 9.312500, "Neckarstraße, Esslingen", isApproximate = true),
        "S19" to CampusBuilding(48.739139, 9.311391, "Kiesstraße 6, Esslingen"),
        "F01" to CampusBuilding(48.745379, 9.321960, "Flandernstraße 101, Esslingen"),
        "F02" to CampusBuilding(48.745386, 9.323379, "Flandernstraße 103, Esslingen"),
        "F03" to CampusBuilding(48.744536, 9.323845, "Flandernstraße 107, Esslingen"),
        "G01" to CampusBuilding(48.697681, 9.656060, "Robert-Bosch-Straße 1, Göppingen"),
        "G02" to CampusBuilding(48.696605, 9.656542, "Robert-Bosch-Straße 4, Göppingen"),
        "G03" to CampusBuilding(48.697660, 9.656347, "Robert-Bosch-Straße, Göppingen"),
        "G04" to CampusBuilding(48.696875, 9.656001, "Robert-Bosch-Straße 2, Göppingen"),
        "G05" to CampusBuilding(48.696576, 9.656062, "Heinrich-Landerer-Straße 53, Göppingen"),
        "W20" to CampusBuilding(48.742684, 9.294114, "Mettinger Straße, Esslingen", isApproximate = true),
        "W21" to CampusBuilding(48.740133, 9.295945, "Eugenie-von-Soden-Straße, Esslingen", isApproximate = true),
    )

    // "F 01.406", "S 01.-122", "S 03.001a", "S 13A.012" — campus letter, building, floor digit.
    private val ROOM_RE = Regex("""\b([SFGW])\s?(\d{1,2})([A-Z]?)\.(-?)(\d)(\w*)""")

    /** Parses a QIS room ("Gebäude 01 - F 01.406") into where it is, or null for rooms that
     *  aren't on a known campus (virtual rooms like "SV1", external venues). */
    fun locateRoom(rawRoom: String?): RoomLocation? {
        if (rawRoom.isNullOrBlank()) return null
        val match = ROOM_RE.find(rawRoom) ?: return null
        val (campus, number, suffix, minus, floorDigit, rest) = match.destructured
        val buildingNumber = number.padStart(2, '0')
        val building = BUILDINGS["$campus$buildingNumber$suffix"] ?: BUILDINGS["$campus$buildingNumber"] ?: return null
        val floor = floorDigit.toInt() * if (minus.isEmpty()) 1 else -1
        return RoomLocation(
            room = "$campus $buildingNumber$suffix.$minus$floorDigit$rest",
            campusName = CAMPUS_NAMES.getValue(campus.first()),
            buildingName = "Gebäude $campus $buildingNumber$suffix",
            floorLabel = floorLabel(floor),
            address = building.address,
            latitude = building.latitude,
            longitude = building.longitude,
            isApproximate = building.isApproximate,
        )
    }

    private fun floorLabel(floor: Int): String = when {
        floor < 0 -> if (floor == -1) "Untergeschoss" else "${-floor}. Untergeschoss"
        floor == 0 -> "Erdgeschoss"
        else -> "$floor. Obergeschoss"
    }
}

/** OpenStreetMap's slippy-map tile coordinates (Web Mercator) — fractional, so the integer part
 *  is the tile and the rest is the position inside it. */
data class TilePosition(val x: Double, val y: Double)

fun tilePosition(latitude: Double, longitude: Double, zoom: Int): TilePosition {
    val n = 2.0.pow(zoom)
    val latRad = latitude * PI / 180.0
    return TilePosition(
        x = (longitude + 180.0) / 360.0 * n,
        y = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * n,
    )
}

fun osmTileUrl(zoom: Int, x: Int, y: Int): String = "https://tile.openstreetmap.org/$zoom/$x/$y.png"

/** The tile column/row containing [value]. */
fun tileIndex(value: Double): Int = floor(value).toInt()

/** openstreetmap.org with a marker on the spot — the fallback when no map app is installed. */
fun osmWebUrl(latitude: Double, longitude: Double): String =
    "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=18/$latitude/$longitude"
