#pragma once

#include "Models.h"
#include <QDate>
#include <QDateTime>
#include <QList>
#include <QSet>
#include <QString>

namespace stundenplan {

struct NextEventResult {
    TimetableEvent event;
    QDate date;
};

/** Like findNextEvent but returns up to `count` events in chronological order. */
QList<NextEventResult> findUpcomingEvents(const QList<TimetableEvent> &events,
                                           const QDateTime &now,
                                           const QSet<QString> &hiddenGroupKeys = {},
                                           int count = 3,
                                           int daysAhead = 7);

/** Next upcoming event at or after `now` (weekends skipped, in-progress events still count). */
std::optional<NextEventResult> findNextEvent(const QList<TimetableEvent> &events,
                                              const QDateTime &now,
                                              const QSet<QString> &hiddenGroupKeys = {},
                                              int daysAhead = 7);

} // namespace stundenplan
