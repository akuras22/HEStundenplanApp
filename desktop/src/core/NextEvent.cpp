#include "NextEvent.h"

#include <algorithm>

namespace stundenplan {

QList<NextEventResult> findUpcomingEvents(const QList<TimetableEvent> &events,
                                           const QDateTime &now,
                                           const QSet<QString> &hiddenGroupKeys,
                                           int count,
                                           int daysAhead)
{
    const int nowMinutes = now.time().hour() * 60 + now.time().minute();
    QList<NextEventResult> result;

    for (int offset = 0; offset <= daysAhead; ++offset) {
        if (result.size() >= count)
            break;
        const QDate date = now.date().addDays(offset);
        auto weekdayOpt = weekdayFromDate(date);
        if (!weekdayOpt.has_value())
            continue;
        const Weekday weekday = *weekdayOpt;

        QList<TimetableEvent> dayEvents;
        for (const auto &event : events) {
            if (event.day != weekday)
                continue;
            if (hiddenGroupKeys.contains(event.groupKey()))
                continue;
            if (!(offset > 0 || event.endMinutes > nowMinutes))
                continue;
            dayEvents.append(event);
        }
        std::sort(dayEvents.begin(), dayEvents.end(), [](const TimetableEvent &a, const TimetableEvent &b) {
            return a.startMinutes < b.startMinutes;
        });

        for (const auto &event : dayEvents) {
            result.append(NextEventResult{event, date});
            if (result.size() >= count)
                break;
        }
    }
    return result;
}

std::optional<NextEventResult> findNextEvent(const QList<TimetableEvent> &events,
                                              const QDateTime &now,
                                              const QSet<QString> &hiddenGroupKeys,
                                              int daysAhead)
{
    auto results = findUpcomingEvents(events, now, hiddenGroupKeys, 1, daysAhead);
    if (results.isEmpty())
        return std::nullopt;
    return results.first();
}

} // namespace stundenplan
