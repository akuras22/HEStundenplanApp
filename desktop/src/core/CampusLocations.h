#pragma once

#include <QString>
#include <optional>

namespace stundenplan {

/**
 * Where a QIS room actually is. QIS names rooms "Gebäude 01 - F 01.406": campus letter (S =
 * Stadtmitte, F = Flandernstraße, G = Göppingen, W = Weststadt), building number, then the room
 * number, whose first digit is the floor ("-1" = Untergeschoss). QIS knows each building's campus
 * and street but no coordinates; those come from OpenStreetMap. Ported from CampusLocations.kt.
 */
struct RoomLocation {
    QString room;          // "F 01.406"
    QString campusName;
    QString buildingName;  // "Gebäude F 01"
    QString floorLabel;    // "4. Obergeschoss"
    QString address;
    double latitude = 0;
    double longitude = 0;
    /** Only the street (not the building itself) is known — the pin is then just near it. */
    bool approximate = false;
};

namespace CampusLocations {

/** Null for rooms that aren't on a known campus (virtual rooms like "SV1", external venues). */
std::optional<RoomLocation> locateRoom(const QString &rawRoom);

/** openstreetmap.org with a marker on the spot. */
QString osmWebUrl(double latitude, double longitude);

} // namespace CampusLocations

} // namespace stundenplan
