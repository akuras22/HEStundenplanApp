#include "TransitParser.h"

#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <algorithm>

namespace stundenplan {

namespace {

QDateTime parseTime(const QJsonValue &value)
{
    const QString raw = value.toString();
    if (raw.isEmpty())
        return {};
    return QDateTime::fromString(raw, Qt::ISODate);
}

QList<TransitNotice> parseNotices(const QJsonArray &infos)
{
    QList<TransitNotice> notices;
    for (const auto &infoValue : infos) {
        for (const auto &linkValue : infoValue.toObject().value(QStringLiteral("infoLinks")).toArray()) {
            const QJsonObject link = linkValue.toObject();
            QString title = link.value(QStringLiteral("subtitle")).toString().trimmed();
            if (title.isEmpty())
                title = link.value(QStringLiteral("title")).toString().trimmed();
            const QString text = link.value(QStringLiteral("content")).toString().trimmed();
            if (title.isEmpty() && text.isEmpty())
                continue;
            const TransitNotice notice{title.isEmpty() ? text : title, text};
            if (!notices.contains(notice))
                notices.append(notice);
        }
    }
    return notices;
}

} // namespace

QList<Departure> TransitParser::parseDepartures(const QByteArray &json)
{
    QList<Departure> departures;
    const QJsonObject root = QJsonDocument::fromJson(json).object();
    // "Esslingen (N) ZOB" leaving from "Esslingen (N)" reads fine as just "ZOB".
    const QString locality = root.value(QStringLiteral("locations")).toArray().at(0).toObject()
                                 .value(QStringLiteral("parent")).toObject().value(QStringLiteral("name")).toString();

    for (const auto &eventValue : root.value(QStringLiteral("stopEvents")).toArray()) {
        const QJsonObject event = eventValue.toObject();
        const QJsonObject transportation = event.value(QStringLiteral("transportation")).toObject();
        Departure departure;
        departure.planned = parseTime(event.value(QStringLiteral("departureTimePlanned")));
        if (transportation.isEmpty() || !departure.planned.isValid())
            continue;
        departure.estimated = parseTime(event.value(QStringLiteral("departureTimeEstimated")));
        for (const char *key : {"disassembledName", "number", "name"}) {
            departure.line = transportation.value(QLatin1String(key)).toString();
            if (!departure.line.isEmpty())
                break;
        }
        departure.productClass = transportation.value(QStringLiteral("product")).toObject().value(QStringLiteral("class")).toInt(-1);
        departure.destination = transportation.value(QStringLiteral("destination")).toObject().value(QStringLiteral("name")).toString();
        if (!locality.isEmpty() && departure.destination.startsWith(locality + QLatin1Char(' '))
            && departure.destination.size() > locality.size() + 1)
            departure.destination = departure.destination.mid(locality.size() + 1);

        const QJsonObject properties = event.value(QStringLiteral("location")).toObject().value(QStringLiteral("properties")).toObject();
        departure.platform = properties.value(QStringLiteral("platformName")).toString();
        const QString platformNumber = properties.value(QStringLiteral("platform")).toString();
        if (departure.platform.isEmpty() && !platformNumber.isEmpty()) {
            const bool train = departure.productClass == 0 || departure.productClass == 1;
            departure.platform = (train ? QStringLiteral("Gleis ") : QStringLiteral("Steig ")) + platformNumber;
        }

        const QJsonArray realtimeStatus = event.value(QStringLiteral("realtimeStatus")).toArray();
        departure.cancelled = event.value(QStringLiteral("isCancelled")).toBool()
            || realtimeStatus.contains(QJsonValue(QStringLiteral("TRIP_CANCELLED")));
        departure.notices = parseNotices(event.value(QStringLiteral("infos")).toArray());
        departures.append(departure);
    }
    std::stable_sort(departures.begin(), departures.end(),
                     [](const Departure &a, const Departure &b) { return a.expected() < b.expected(); });
    return departures;
}

QList<TransitStop> TransitParser::parseStops(const QByteArray &json)
{
    QList<QPair<int, TransitStop>> ranked;
    const QJsonArray locations = QJsonDocument::fromJson(json).object().value(QStringLiteral("locations")).toArray();
    for (const auto &value : locations) {
        const QJsonObject location = value.toObject();
        if (location.value(QStringLiteral("type")).toString() != QLatin1String("stop"))
            continue;
        TransitStop stop;
        stop.id = location.value(QStringLiteral("id")).toString();
        stop.name = location.value(QStringLiteral("name")).toString();
        stop.shortName = location.value(QStringLiteral("disassembledName")).toString();
        if (stop.id.isEmpty() || stop.name.isEmpty())
            continue;
        if (stop.shortName.isEmpty())
            stop.shortName = stop.name;
        ranked.append({location.value(QStringLiteral("matchQuality")).toInt(), stop});
    }
    std::stable_sort(ranked.begin(), ranked.end(), [](const auto &a, const auto &b) { return a.first > b.first; });
    QList<TransitStop> stops;
    for (const auto &entry : ranked)
        stops.append(entry.second);
    return stops;
}

} // namespace stundenplan
