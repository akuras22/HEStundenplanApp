#pragma once

#include <QDateTime>
#include <QList>
#include <QMetaType>
#include <QString>

namespace stundenplan {

/** A VVS stop: global id ("de:08116:3998"), full name with the town, and the stop alone. */
struct TransitStop {
    QString id;
    QString name;
    QString shortName;

    bool operator==(const TransitStop &other) const { return id == other.id; }
};

struct TransitNotice {
    QString title;
    QString text;

    bool operator==(const TransitNotice &other) const { return title == other.title && text == other.text; }
};

struct Departure {
    QString line;            // "S1", "105", "MEX18"
    int productClass = -1;   // EFA: 0 train, 1 S-Bahn, 2 U-Bahn, 3 Stadtbahn, 4 tram, 5–7 bus, …
    QString destination;
    QDateTime planned;
    QDateTime estimated;     // invalid == no real-time data
    QString platform;        // "Gleis 8", "Steig 3"
    bool cancelled = false;
    QList<TransitNotice> notices;

    QDateTime expected() const { return estimated.isValid() ? estimated : planned; }
};

/**
 * Parses the VVS EFA backend's "rapidJSON" answers — the interface vvs.de's own departure monitor
 * uses. Ported from TransitParser.kt.
 */
namespace TransitParser {

/** XML_DM_REQUEST: departures from one stop, in the order they actually leave — EFA sorts by
 *  timetable, so a bus running 15 minutes late would otherwise sit above punctual earlier ones. */
QList<Departure> parseDepartures(const QByteArray &json);

/** XML_STOPFINDER_REQUEST: stops matching a search, best match first. */
QList<TransitStop> parseStops(const QByteArray &json);

} // namespace TransitParser

} // namespace stundenplan

Q_DECLARE_METATYPE(stundenplan::TransitStop)
