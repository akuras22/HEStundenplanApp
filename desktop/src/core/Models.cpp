#include "Models.h"

#include <algorithm>

namespace stundenplan {

QString weekdayGermanLabel(Weekday day)
{
    switch (day) {
    case Weekday::Monday:
        return QStringLiteral("Montag");
    case Weekday::Tuesday:
        return QStringLiteral("Dienstag");
    case Weekday::Wednesday:
        return QStringLiteral("Mittwoch");
    case Weekday::Thursday:
        return QStringLiteral("Donnerstag");
    case Weekday::Friday:
        return QStringLiteral("Freitag");
    }
    return {};
}

int weekdayQtDayOfWeek(Weekday day)
{
    switch (day) {
    case Weekday::Monday:
        return 1;
    case Weekday::Tuesday:
        return 2;
    case Weekday::Wednesday:
        return 3;
    case Weekday::Thursday:
        return 4;
    case Weekday::Friday:
        return 5;
    }
    return 1;
}

std::optional<Weekday> weekdayFromDate(const QDate &date)
{
    switch (date.dayOfWeek()) {
    case 1:
        return Weekday::Monday;
    case 2:
        return Weekday::Tuesday;
    case 3:
        return Weekday::Wednesday;
    case 4:
        return Weekday::Thursday;
    case 5:
        return Weekday::Friday;
    default:
        return std::nullopt;
    }
}

QString TimetableEvent::startLabel() const
{
    return QStringLiteral("%1:%2")
        .arg(startMinutes / 60, 2, 10, QLatin1Char('0'))
        .arg(startMinutes % 60, 2, 10, QLatin1Char('0'));
}

QString TimetableEvent::endLabel() const
{
    return QStringLiteral("%1:%2")
        .arg(endMinutes / 60, 2, 10, QLatin1Char('0'))
        .arg(endMinutes % 60, 2, 10, QLatin1Char('0'));
}

bool TimetableEvent::appliesOn(const QDate &date) const
{
    auto d = weekdayFromDate(date);
    return d.has_value() && *d == day;
}

QString TimetableEvent::groupKey() const
{
    QString discriminator;
    if (!lecturer.isEmpty()) {
        discriminator = lecturer;
    } else if (!room.isEmpty()) {
        discriminator = room;
    } else {
        discriminator = QStringLiteral("%1-%2").arg(static_cast<int>(day)).arg(startMinutes);
    }
    return title + QLatin1Char('|') + discriminator;
}

QString TimetableEvent::occurrenceKey(const QDate &date) const
{
    return QStringLiteral("%1@%2@%3").arg(date.toString(Qt::ISODate)).arg(startMinutes).arg(groupKey());
}

bool TimetableEvent::isHiddenOn(const QDate &date, const QSet<QString> &hiddenGroupKeys, const QSet<QString> &hiddenOccurrenceKeys) const
{
    const QString key = groupKey();
    if (hiddenGroupKeys.contains(key))
        return true;
    return !hiddenOccurrenceKeys.isEmpty() && hiddenOccurrenceKeys.contains(occurrenceKey(date));
}

QList<QDate> TimetableEvent::candidateDates(const QDate &around, const QDate &today) const
{
    const int qtDay = weekdayQtDayOfWeek(day);
    QDate first = QDate::fromString(startDate.trimmed(), QStringLiteral("d.M.yyyy"));
    if (!first.isValid())
        first = around.addDays(-28);
    QDate last = QDate::fromString(endDate.trimmed(), QStringLiteral("d.M.yyyy"));
    if (!last.isValid() || last < first)
        last = around.addDays(20 * 7);
    QDate date = first.addDays((qtDay - first.dayOfWeek() + 7) % 7);
    const QDate earliest = std::min(around, today);
    QList<QDate> result;
    while (date <= last && result.size() < 60) {
        if (date >= earliest)
            result.append(date);
        date = date.addDays(7);
    }
    if (around.dayOfWeek() == qtDay && !result.contains(around))
        result.append(around);
    std::sort(result.begin(), result.end());
    return result;
}

} // namespace stundenplan
