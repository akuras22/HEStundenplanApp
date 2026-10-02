#include "CampusLocations.h"

#include <QHash>
#include <QRegularExpression>

namespace stundenplan {

namespace {

struct CampusBuilding {
    double latitude;
    double longitude;
    const char *address;
    bool approximate = false;
};

// Keyed by campus letter + two-digit building number (+ suffix, e.g. "S13A"). Coordinates are
// each building's centre in OpenStreetMap; addresses from QIS's building pages and OSM.
const QHash<QString, CampusBuilding> &buildings()
{
    static const QHash<QString, CampusBuilding> table = {
        {QStringLiteral("S01"), {48.738052, 9.311017, "Kanalstraße 33, Esslingen"}},
        {QStringLiteral("S02"), {48.738153, 9.311370, "Mühlstraße 1, Esslingen"}},
        {QStringLiteral("S03"), {48.738473, 9.311351, "Mühlstraße, Esslingen"}},
        {QStringLiteral("S04"), {48.738430, 9.311853, "Mühlstraße 5, Esslingen"}},
        {QStringLiteral("S05"), {48.738434, 9.312406, "Mühlstraße 7, Esslingen"}},
        {QStringLiteral("S06"), {48.738636, 9.311785, "Obertorstraße 16, Esslingen"}},
        {QStringLiteral("S07"), {48.738137, 9.312203, "Mühlstraße 3, Esslingen"}},
        {QStringLiteral("S08"), {48.738566, 9.312616, "Mühlstraße 9, Esslingen"}},
        {QStringLiteral("S09"), {48.738640, 9.310751, "Kanalstraße 31, Esslingen"}},
        {QStringLiteral("S10"), {48.739003, 9.310273, "Kanalstraße 29, Esslingen"}},
        {QStringLiteral("S12"), {48.737598, 9.312810, "Neckarstraße 67, Esslingen"}},
        {QStringLiteral("S13"), {48.737161, 9.312694, "Neckarstraße 63, Esslingen"}},
        {QStringLiteral("S13A"), {48.737161, 9.312694, "Neckarstraße, Esslingen", true}},
        {QStringLiteral("S14"), {48.737167, 9.313030, "Neckarstraße 65/1, Esslingen"}},
        {QStringLiteral("S15"), {48.737949, 9.312906, "Neckarstraße 67/1, Esslingen"}},
        {QStringLiteral("S16"), {48.738641, 9.310159, "Kanalstraße 27, Esslingen"}},
        {QStringLiteral("S17"), {48.738558, 9.309825, "Kanalstraße 12/1, Esslingen"}},
        {QStringLiteral("S18"), {48.737400, 9.312500, "Neckarstraße, Esslingen", true}},
        {QStringLiteral("S19"), {48.739139, 9.311391, "Kiesstraße 6, Esslingen"}},
        {QStringLiteral("F01"), {48.745379, 9.321960, "Flandernstraße 101, Esslingen"}},
        {QStringLiteral("F02"), {48.745386, 9.323379, "Flandernstraße 103, Esslingen"}},
        {QStringLiteral("F03"), {48.744536, 9.323845, "Flandernstraße 107, Esslingen"}},
        {QStringLiteral("G01"), {48.697681, 9.656060, "Robert-Bosch-Straße 1, Göppingen"}},
        {QStringLiteral("G02"), {48.696605, 9.656542, "Robert-Bosch-Straße 4, Göppingen"}},
        {QStringLiteral("G03"), {48.697660, 9.656347, "Robert-Bosch-Straße, Göppingen"}},
        {QStringLiteral("G04"), {48.696875, 9.656001, "Robert-Bosch-Straße 2, Göppingen"}},
        {QStringLiteral("G05"), {48.696576, 9.656062, "Heinrich-Landerer-Straße 53, Göppingen"}},
        {QStringLiteral("W20"), {48.742684, 9.294114, "Mettinger Straße, Esslingen", true}},
        {QStringLiteral("W21"), {48.740133, 9.295945, "Eugenie-von-Soden-Straße, Esslingen", true}},
    };
    return table;
}

QString campusName(QChar campus)
{
    switch (campus.unicode()) {
    case 'S':
        return QStringLiteral("Campus Esslingen Stadtmitte");
    case 'F':
        return QStringLiteral("Campus Esslingen Flandernstraße");
    case 'G':
        return QStringLiteral("Campus Göppingen");
    default:
        return QStringLiteral("Campus Esslingen Weststadt");
    }
}

QString floorLabel(int floor)
{
    if (floor < 0)
        return floor == -1 ? QStringLiteral("Untergeschoss") : QStringLiteral("%1. Untergeschoss").arg(-floor);
    if (floor == 0)
        return QStringLiteral("Erdgeschoss");
    return QStringLiteral("%1. Obergeschoss").arg(floor);
}

} // namespace

std::optional<RoomLocation> CampusLocations::locateRoom(const QString &rawRoom)
{
    // "F 01.406", "S 01.-122", "S 03.001a", "S 13A.012" — campus letter, building, floor digit.
    static const QRegularExpression roomRe(QStringLiteral("\\b([SFGW])\\s?(\\d{1,2})([A-Z]?)\\.(-?)(\\d)(\\w*)"));
    const auto match = roomRe.match(rawRoom);
    if (!match.hasMatch())
        return std::nullopt;
    const QString campus = match.captured(1);
    const QString number = match.captured(2).rightJustified(2, QLatin1Char('0'));
    const QString suffix = match.captured(3);
    const QString minus = match.captured(4);
    const QString floorDigit = match.captured(5);

    auto it = buildings().constFind(campus + number + suffix);
    if (it == buildings().constEnd())
        it = buildings().constFind(campus + number);
    if (it == buildings().constEnd())
        return std::nullopt;

    RoomLocation location;
    location.room = QStringLiteral("%1 %2%3.%4%5%6").arg(campus, number, suffix, minus, floorDigit, match.captured(6));
    location.campusName = campusName(campus.at(0));
    location.buildingName = QStringLiteral("Gebäude %1 %2%3").arg(campus, number, suffix);
    location.floorLabel = floorLabel(floorDigit.toInt() * (minus.isEmpty() ? 1 : -1));
    location.address = QString::fromUtf8(it->address);
    location.latitude = it->latitude;
    location.longitude = it->longitude;
    location.approximate = it->approximate;
    return location;
}

QString CampusLocations::osmWebUrl(double latitude, double longitude)
{
    return QStringLiteral("https://www.openstreetmap.org/?mlat=%1&mlon=%2#map=18/%1/%2")
        .arg(latitude, 0, 'f', 6)
        .arg(longitude, 0, 'f', 6);
}

} // namespace stundenplan
