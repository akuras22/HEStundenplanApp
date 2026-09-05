#pragma once

#include "Models.h"
#include <QDate>
#include <QJsonObject>
#include <QList>
#include <QString>
#include <optional>

namespace stundenplan {

struct CachedWeek {
    QList<TimetableEvent> events;
    qint64 savedAt = 0; // msecs since epoch
};

/**
 * Offline fallback for the last few weeks successfully fetched from QIS, stored as one JSON file
 * under $XDG_CACHE_HOME/hestundenplan/timetable_cache.json. Never a source of truth — the live
 * site always wins when reachable. Capped to MAX_ENTRIES weeks, oldest-first eviction. Ported from
 * TimetableCache.kt.
 */
class TimetableCache
{
public:
    TimetableCache();

    std::optional<CachedWeek> get(const Studiengang &studiengang, const QDate &weekMonday) const;
    void put(const Studiengang &studiengang, const QDate &weekMonday, const QList<TimetableEvent> &events);

    /** Manual "Zwischenspeicher leeren" action. */
    void clearAll();

private:
    static QString weekKey(const Studiengang &studiengang, const QDate &weekMonday);
    QString cacheFilePath() const;
    QJsonObject load() const;
    void save(const QJsonObject &root) const;

    QString m_filePath;
};

} // namespace stundenplan
