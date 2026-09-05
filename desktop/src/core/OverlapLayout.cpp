#include "OverlapLayout.h"

#include <algorithm>

namespace stundenplan {

std::pair<int, int> dayWindowFor(const QList<TimetableEvent> &events)
{
    if (events.isEmpty()) {
        return {kDayStartMinutesDefault, kDayEndMinutesDefault};
    }
    int earliest = events.first().startMinutes;
    int latest = events.first().endMinutes;
    for (const auto &e : events) {
        earliest = std::min(earliest, e.startMinutes);
        latest = std::max(latest, e.endMinutes);
    }
    int start = std::min(kDayStartMinutesDefault, (earliest / 60) * 60);
    int end = std::max(kDayEndMinutesDefault, ((latest + 59) / 60) * 60);
    return {start, end};
}

QList<OverlapSlot> layoutOverlaps(const QList<TimetableEvent> &events)
{
    QList<TimetableEvent> sorted = events;
    std::sort(sorted.begin(), sorted.end(), [](const TimetableEvent &a, const TimetableEvent &b) {
        if (a.startMinutes != b.startMinutes)
            return a.startMinutes < b.startMinutes;
        return a.endMinutes < b.endMinutes;
    });

    QList<OverlapSlot> result;
    QList<TimetableEvent> cluster;
    int clusterEnd = 0;

    auto flushCluster = [&]() {
        if (cluster.isEmpty())
            return;
        QList<int> columnEnds;
        QList<int> columnByEvent;
        columnByEvent.reserve(cluster.size());
        for (const auto &event : cluster) {
            int column = -1;
            for (int i = 0; i < columnEnds.size(); ++i) {
                if (columnEnds[i] <= event.startMinutes) {
                    column = i;
                    break;
                }
            }
            if (column >= 0) {
                columnEnds[column] = event.endMinutes;
                columnByEvent.append(column);
            } else {
                columnEnds.append(event.endMinutes);
                columnByEvent.append(columnEnds.size() - 1);
            }
        }
        const int columnCount = columnEnds.size();
        for (int i = 0; i < cluster.size(); ++i) {
            result.append(OverlapSlot{cluster[i], columnByEvent[i], columnCount});
        }
        cluster.clear();
    };

    for (const auto &event : sorted) {
        if (cluster.isEmpty()) {
            clusterEnd = event.endMinutes;
        } else if (event.startMinutes < clusterEnd) {
            clusterEnd = std::max(clusterEnd, event.endMinutes);
        } else {
            flushCluster();
            clusterEnd = event.endMinutes;
        }
        cluster.append(event);
    }
    flushCluster();
    return result;
}

} // namespace stundenplan
