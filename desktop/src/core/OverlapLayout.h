#pragma once

#include "Models.h"
#include <QList>
#include <utility>

namespace stundenplan {

constexpr int kDayStartMinutesDefault = 8 * 60;
constexpr int kDayEndMinutesDefault = 19 * 60;

/** Expands the default 08:00-19:00 grid window only when an event falls outside it. */
std::pair<int, int> dayWindowFor(const QList<TimetableEvent> &events);

struct OverlapSlot {
    TimetableEvent event;
    int column = 0;
    int columnCount = 1;
};

/** Google-Calendar-style side-by-side column packing for overlapping events. */
QList<OverlapSlot> layoutOverlaps(const QList<TimetableEvent> &events);

} // namespace stundenplan
