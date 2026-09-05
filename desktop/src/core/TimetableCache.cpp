#include "TimetableCache.h"

#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QStandardPaths>

namespace stundenplan {

namespace {
constexpr int kMaxEntries = 12;

QString weekdayName(Weekday day)
{
    switch (day) {
    case Weekday::Monday:
        return QStringLiteral("MONDAY");
    case Weekday::Tuesday:
        return QStringLiteral("TUESDAY");
    case Weekday::Wednesday:
        return QStringLiteral("WEDNESDAY");
    case Weekday::Thursday:
        return QStringLiteral("THURSDAY");
    case Weekday::Friday:
        return QStringLiteral("FRIDAY");
    }
    return {};
}

std::optional<Weekday> weekdayFromName(const QString &name)
{
    if (name == QStringLiteral("MONDAY"))
        return Weekday::Monday;
    if (name == QStringLiteral("TUESDAY"))
        return Weekday::Tuesday;
    if (name == QStringLiteral("WEDNESDAY"))
        return Weekday::Wednesday;
    if (name == QStringLiteral("THURSDAY"))
        return Weekday::Thursday;
    if (name == QStringLiteral("FRIDAY"))
        return Weekday::Friday;
    return std::nullopt;
}

QJsonValue orNull(const QString &s)
{
    return s.isEmpty() ? QJsonValue(QJsonValue::Null) : QJsonValue(s);
}

QString fromMaybeNull(const QJsonValue &v)
{
    return v.isNull() || !v.isString() ? QString() : v.toString();
}

} // namespace

TimetableCache::TimetableCache()
{
    const QString cacheDir = QStandardPaths::writableLocation(QStandardPaths::CacheLocation);
    QDir().mkpath(cacheDir + QStringLiteral("/hestundenplan"));
    m_filePath = cacheDir + QStringLiteral("/hestundenplan/timetable_cache.json");
}

QString TimetableCache::weekKey(const Studiengang &studiengang, const QDate &weekMonday)
{
    int isoYear = 0;
    const int isoWeek = weekMonday.weekNumber(&isoYear);
    return QStringLiteral("%1_%2_%3").arg(studiengang.id()).arg(isoYear).arg(isoWeek);
}

QString TimetableCache::cacheFilePath() const
{
    return m_filePath;
}

QJsonObject TimetableCache::load() const
{
    QFile file(m_filePath);
    if (!file.open(QIODevice::ReadOnly))
        return {};
    const auto doc = QJsonDocument::fromJson(file.readAll());
    return doc.isObject() ? doc.object() : QJsonObject();
}

void TimetableCache::save(const QJsonObject &root) const
{
    QFile file(m_filePath);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate))
        return;
    file.write(QJsonDocument(root).toJson(QJsonDocument::Compact));
}

std::optional<CachedWeek> TimetableCache::get(const Studiengang &studiengang, const QDate &weekMonday) const
{
    const QJsonObject root = load();
    const QString key = weekKey(studiengang, weekMonday);
    const QJsonObject weeks = root.value(QStringLiteral("weeks")).toObject();
    if (!weeks.contains(key))
        return std::nullopt;

    const QJsonObject weekObj = weeks.value(key).toObject();
    CachedWeek result;
    result.savedAt = static_cast<qint64>(weekObj.value(QStringLiteral("savedAt")).toDouble());
    for (const auto &v : weekObj.value(QStringLiteral("events")).toArray()) {
        const QJsonObject o = v.toObject();
        auto day = weekdayFromName(o.value(QStringLiteral("day")).toString());
        if (!day.has_value())
            continue;
        TimetableEvent e;
        e.day = *day;
        e.title = o.value(QStringLiteral("title")).toString();
        e.startMinutes = o.value(QStringLiteral("startMinutes")).toInt();
        e.endMinutes = o.value(QStringLiteral("endMinutes")).toInt();
        e.frequency = fromMaybeNull(o.value(QStringLiteral("frequency")));
        e.room = fromMaybeNull(o.value(QStringLiteral("room")));
        e.lecturer = fromMaybeNull(o.value(QStringLiteral("lecturer")));
        e.category = fromMaybeNull(o.value(QStringLiteral("category")));
        e.startDate = fromMaybeNull(o.value(QStringLiteral("startDate")));
        e.endDate = fromMaybeNull(o.value(QStringLiteral("endDate")));
        result.events.append(e);
    }
    return result;
}

void TimetableCache::put(const Studiengang &studiengang, const QDate &weekMonday, const QList<TimetableEvent> &events)
{
    QJsonObject root = load();
    QJsonObject weeks = root.value(QStringLiteral("weeks")).toObject();
    const QString key = weekKey(studiengang, weekMonday);

    QJsonArray eventsArr;
    for (const auto &e : events) {
        QJsonObject o;
        o[QStringLiteral("day")] = weekdayName(e.day);
        o[QStringLiteral("title")] = e.title;
        o[QStringLiteral("startMinutes")] = e.startMinutes;
        o[QStringLiteral("endMinutes")] = e.endMinutes;
        o[QStringLiteral("frequency")] = orNull(e.frequency);
        o[QStringLiteral("room")] = orNull(e.room);
        o[QStringLiteral("lecturer")] = orNull(e.lecturer);
        o[QStringLiteral("category")] = orNull(e.category);
        o[QStringLiteral("startDate")] = orNull(e.startDate);
        o[QStringLiteral("endDate")] = orNull(e.endDate);
        eventsArr.append(o);
    }
    QJsonObject weekObj;
    weekObj[QStringLiteral("savedAt")] = double(QDateTime::currentMSecsSinceEpoch());
    weekObj[QStringLiteral("events")] = eventsArr;
    weeks[key] = weekObj;

    // Oldest-first eviction, tracked via a manifest array (insertion order).
    QJsonArray manifest = root.value(QStringLiteral("manifest")).toArray();
    QStringList manifestList;
    for (const auto &v : manifest) {
        const QString k = v.toString();
        if (k != key)
            manifestList.append(k);
    }
    manifestList.append(key);
    while (manifestList.size() > kMaxEntries) {
        const QString evicted = manifestList.takeFirst();
        weeks.remove(evicted);
    }
    manifest = QJsonArray::fromStringList(manifestList);

    root[QStringLiteral("weeks")] = weeks;
    root[QStringLiteral("manifest")] = manifest;
    save(root);
}

void TimetableCache::clearAll()
{
    save(QJsonObject());
}

} // namespace stundenplan
