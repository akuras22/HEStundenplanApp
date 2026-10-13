#include "TimetableController.h"

#include "core/CampusLocations.h"
#include "core/NextEvent.h"
#include "core/OverlapLayout.h"
#include "core/QisRepository.h"
#include "core/SettingsStore.h"
#include "core/TimetableCache.h"

#include <QDateTime>
#include <QLocale>
#include <QMap>
#include <algorithm>

namespace stundenplan {

namespace {

QVariantMap studiengangToVariant(const Studiengang &s)
{
    QVariantMap m;
    m[QStringLiteral("code")] = s.code;
    m[QStringLiteral("abstgvnr")] = s.abstgvnr;
    m[QStringLiteral("parallelid")] = s.parallelid;
    m[QStringLiteral("id")] = s.id();
    return m;
}

QVariantMap eventToVariant(const TimetableEvent &e, const QDate &date, int column = 0, int columnCount = 1)
{
    QVariantMap m;
    m[QStringLiteral("date")] = date;
    m[QStringLiteral("occurrenceKey")] = e.occurrenceKey(date);
    m[QStringLiteral("day")] = weekdayQtDayOfWeek(e.day);
    m[QStringLiteral("dayLabel")] = weekdayGermanLabel(e.day);
    m[QStringLiteral("title")] = e.title;
    m[QStringLiteral("startMinutes")] = e.startMinutes;
    m[QStringLiteral("endMinutes")] = e.endMinutes;
    m[QStringLiteral("startLabel")] = e.startLabel();
    m[QStringLiteral("endLabel")] = e.endLabel();
    m[QStringLiteral("frequency")] = e.frequency;
    m[QStringLiteral("room")] = e.room;
    m[QStringLiteral("lecturer")] = e.lecturer;
    m[QStringLiteral("category")] = e.category;
    m[QStringLiteral("startDate")] = e.startDate;
    m[QStringLiteral("endDate")] = e.endDate;
    m[QStringLiteral("groupKey")] = e.groupKey();
    m[QStringLiteral("column")] = column;
    m[QStringLiteral("columnCount")] = columnCount;
    return m;
}

} // namespace

TimetableController::TimetableController(SettingsStore *settings, TimetableCache *cache, QisSession *session, QObject *parent)
    : QObject(parent)
    , m_settings(settings)
    , m_cache(cache)
    , m_repository(new QisRepository(session, this))
{
    const QDate today = QDate::currentDate();
    m_weekMonday = today.addDays(-(today.dayOfWeek() - 1));

    connect(m_repository, &QisRepository::studiengaengeFetched, this, [this](const QList<Studiengang> &list) {
        m_allStudiengaenge = list;
        Q_EMIT studiengaengeChanged();
    });
    connect(m_repository, &QisRepository::timetableFetched, this, [this](const QList<TimetableEvent> &events) {
        applyEvents(events, false);
        if (auto sg = currentStudiengang())
            m_cache->put(*sg, m_weekMonday, events);
        setLoading(false);
    });
    connect(m_repository, &QisRepository::fetchFailed, this, [this](const QString &message, bool forTimetable) {
        if (!forTimetable) {
            setLoading(false);
            setErrorMessage(message);
            return;
        }
        if (auto sg = currentStudiengang()) {
            if (auto cached = m_cache->get(*sg, m_weekMonday)) {
                applyEvents(cached->events, true);
                m_cacheTimestamp = cached->savedAt;
                Q_EMIT cacheTimestampChanged();
                setLoading(false);
                return;
            }
        }
        setLoading(false);
        setErrorMessage(message);
    });
}

QVariantList TimetableController::studiengaenge() const
{
    QVariantList list;
    for (const auto &s : m_allStudiengaenge)
        list.append(studiengangToVariant(s));
    return list;
}

QVariantList TimetableController::favoriteStudiengaenge() const
{
    QVariantList list;
    for (const auto &s : m_settings->favoriteStudiengaenge())
        list.append(studiengangToVariant(s));
    return list;
}

QVariantMap TimetableController::selectedStudiengangVariant() const
{
    if (auto sg = currentStudiengang())
        return studiengangToVariant(*sg);
    return {};
}

std::optional<Studiengang> TimetableController::currentStudiengang() const
{
    return m_settings->selectedStudiengang();
}

void TimetableController::loadStudiengaenge()
{
    m_repository->fetchStudiengaenge();
}

void TimetableController::selectStudiengang(const QString &code, const QString &abstgvnr, const QString &parallelid)
{
    Studiengang s{code, abstgvnr, parallelid};
    m_settings->setSelectedStudiengang(s);
    Q_EMIT selectedStudiengangChanged();
    refresh();
}

void TimetableController::setWeekMonday(const QDate &monday)
{
    if (m_weekMonday == monday)
        return;
    m_weekMonday = monday;
    Q_EMIT weekMondayChanged();
    refresh();
}

void TimetableController::goToday()
{
    // Always force a live refetch, even if we're already showing the current week — "Heute" is
    // meant to mean "show me what's actually current right now", not just "switch back to
    // whichever week happens to already be in memory" (which could be stale/offline data).
    const QDate today = QDate::currentDate();
    const QDate monday = today.addDays(-(today.dayOfWeek() - 1));
    const bool mondayChanged = monday != m_weekMonday;
    m_weekMonday = monday;
    if (mondayChanged)
        Q_EMIT weekMondayChanged();
    refresh();
}

void TimetableController::refresh()
{
    auto sg = currentStudiengang();
    if (!sg.has_value())
        return;
    setLoading(true);
    setErrorMessage(QString());
    setOffline(false);
    m_repository->fetchTimetable(*sg, m_weekMonday);
}

void TimetableController::clearCache()
{
    m_cache->clearAll();
}

void TimetableController::toggleFavorite(const QString &code, const QString &abstgvnr, const QString &parallelid)
{
    Studiengang s{code, abstgvnr, parallelid};
    m_settings->setFavorite(s, !m_settings->isFavorite(s));
    Q_EMIT favoritesChanged();
}

bool TimetableController::isFavorite(const QString &code, const QString &abstgvnr, const QString &parallelid) const
{
    return m_settings->isFavorite(Studiengang{code, abstgvnr, parallelid});
}

void TimetableController::hideGroup(const QString &groupKey, bool hidden)
{
    m_settings->setHidden(groupKey, hidden);
    rebuildWeekEvents();
}

bool TimetableController::isGroupHidden(const QString &groupKey) const
{
    return m_settings->hiddenEventKeys().contains(groupKey);
}

QVariantList TimetableController::occurrenceChoices(const QVariantMap &event) const
{
    const QString groupKey = event.value(QStringLiteral("groupKey")).toString();
    const int startMinutes = event.value(QStringLiteral("startMinutes")).toInt();
    const int qtDay = event.value(QStringLiteral("day")).toInt();
    const QDate around = event.value(QStringLiteral("date")).toDate();
    QVariantList result;
    for (const auto &e : m_events) {
        if (e.groupKey() != groupKey || e.startMinutes != startMinutes || weekdayQtDayOfWeek(e.day) != qtDay)
            continue;
        const auto hidden = m_settings->hiddenOccurrenceKeys();
        const QLocale german(QLocale::German);
        for (const QDate &date : e.candidateDates(around, QDate::currentDate())) {
            QVariantMap m;
            m[QStringLiteral("date")] = date;
            m[QStringLiteral("label")] = german.toString(date, QStringLiteral("ddd dd.MM.yyyy"));
            m[QStringLiteral("key")] = e.occurrenceKey(date);
            m[QStringLiteral("hidden")] = hidden.contains(e.occurrenceKey(date));
            m[QStringLiteral("current")] = date == around;
            result.append(m);
        }
        break;
    }
    return result;
}

void TimetableController::updateHiddenOccurrences(const QStringList &hide, const QStringList &show)
{
    m_settings->updateHiddenOccurrences(hide, show);
    rebuildWeekEvents();
}

QVariantList TimetableController::hiddenOccurrences() const
{
    // Grouped per weekly slot (groupKey + start time), dates sorted.
    QMap<QPair<QString, int>, QList<QPair<QDate, QString>>> bySlot;
    for (const auto &key : m_settings->hiddenOccurrenceKeys()) {
        const QDate date = QDate::fromString(key.section(QLatin1Char('@'), 0, 0), Qt::ISODate);
        bool ok = false;
        const int minutes = key.section(QLatin1Char('@'), 1, 1).toInt(&ok);
        if (!date.isValid() || !ok)
            continue;
        bySlot[{key.section(QLatin1Char('@'), 2), minutes}].append({date, key});
    }
    const QLocale german(QLocale::German);
    QVariantList result;
    for (auto it = bySlot.begin(); it != bySlot.end(); ++it) {
        auto entries = it.value();
        std::sort(entries.begin(), entries.end());
        QVariantList dates;
        QStringList keys;
        for (const auto &[date, key] : entries) {
            QVariantMap d;
            d[QStringLiteral("key")] = key;
            d[QStringLiteral("label")] = german.toString(date, QStringLiteral("ddd dd.MM.yyyy"));
            dates.append(d);
            keys.append(key);
        }
        const QString groupKey = it.key().first;
        const int minutes = it.key().second;
        QVariantMap m;
        m[QStringLiteral("title")] = groupKey.section(QLatin1Char('|'), 0, 0);
        m[QStringLiteral("subtitle")] = QStringLiteral("%1 · %2:%3")
                                             .arg(groupKey.section(QLatin1Char('|'), 1))
                                             .arg(minutes / 60, 2, 10, QLatin1Char('0'))
                                             .arg(minutes % 60, 2, 10, QLatin1Char('0'));
        m[QStringLiteral("keys")] = keys;
        m[QStringLiteral("dates")] = dates;
        result.append(m);
    }
    return result;
}

QDate TimetableController::dateOf(const TimetableEvent &event) const
{
    return m_weekMonday.addDays(weekdayQtDayOfWeek(event.day) - 1);
}

QVariantList TimetableController::hiddenGroups() const
{
    QVariantList result;
    const auto hidden = m_settings->hiddenEventKeys();
    for (const auto &key : hidden) {
        QVariantMap m;
        m[QStringLiteral("groupKey")] = key;
        QString title = key.section(QLatin1Char('|'), 0, 0);
        for (const auto &e : m_events) {
            if (e.groupKey() == key) {
                title = e.title;
                break;
            }
        }
        m[QStringLiteral("title")] = title;
        result.append(m);
    }
    return result;
}

void TimetableController::applyEvents(const QList<TimetableEvent> &events, bool fromCache)
{
    m_events = events;
    setOffline(fromCache);
    if (!fromCache) {
        m_cacheTimestamp = QDateTime::currentMSecsSinceEpoch();
        Q_EMIT cacheTimestampChanged();
    }
    rebuildWeekEvents();
}

void TimetableController::rebuildWeekEvents()
{
    const auto hidden = m_settings->hiddenEventKeys();
    const auto hiddenOccurrences = m_settings->hiddenOccurrenceKeys();
    m_weekEvents.clear();
    for (const auto &e : m_events) {
        if (e.isHiddenOn(dateOf(e), hidden, hiddenOccurrences))
            continue;
        m_weekEvents.append(eventToVariant(e, dateOf(e)));
    }
    Q_EMIT weekEventsChanged();
}

QVariantList TimetableController::eventsForDay(int qtDayOfWeek) const
{
    const auto hidden = m_settings->hiddenEventKeys();
    const auto hiddenOccurrences = m_settings->hiddenOccurrenceKeys();
    QList<TimetableEvent> dayEvents;
    for (const auto &e : m_events) {
        if (weekdayQtDayOfWeek(e.day) == qtDayOfWeek && !e.isHiddenOn(dateOf(e), hidden, hiddenOccurrences))
            dayEvents.append(e);
    }
    const auto overlapSlots = layoutOverlaps(dayEvents);
    QVariantList result;
    for (const auto &slot : overlapSlots)
        result.append(eventToVariant(slot.event, dateOf(slot.event), slot.column, slot.columnCount));
    return result;
}

QVariantList TimetableController::searchEvents(const QString &query) const
{
    QVariantList result;
    if (query.trimmed().isEmpty())
        return result;
    const QString needle = query.trimmed();
    const auto hidden = m_settings->hiddenEventKeys();
    const auto hiddenOccurrences = m_settings->hiddenOccurrenceKeys();
    for (const auto &e : m_events) {
        if (e.isHiddenOn(dateOf(e), hidden, hiddenOccurrences))
            continue;
        if (e.room.contains(needle, Qt::CaseInsensitive) || e.lecturer.contains(needle, Qt::CaseInsensitive)
            || e.title.contains(needle, Qt::CaseInsensitive)) {
            result.append(eventToVariant(e, dateOf(e)));
        }
    }
    return result;
}

QVariantMap TimetableController::nextEventToday() const
{
    // Scoped to the currently loaded week only (a full port would keep this+next week's events
    // around like the Android widget does) — good enough for the Tag-view countdown banner, which
    // only ever looks at "today".
    const auto hidden = m_settings->hiddenEventKeys();
    QSet<QString> hiddenSet(hidden.begin(), hidden.end());
    auto next = findNextEvent(m_events, QDateTime::currentDateTime(), hiddenSet, 0, m_settings->hiddenOccurrenceKeys());
    if (!next.has_value())
        return {};
    QVariantMap m = eventToVariant(next->event, next->date);
    return m;
}

QString TimetableController::shareTextFor(const QVariantMap &event) const
{
    QStringList lines;
    lines << event.value(QStringLiteral("title")).toString();
    lines << QStringLiteral("%1, %2 - %3")
                 .arg(event.value(QStringLiteral("dayLabel")).toString())
                 .arg(event.value(QStringLiteral("startLabel")).toString())
                 .arg(event.value(QStringLiteral("endLabel")).toString());
    const QString room = event.value(QStringLiteral("room")).toString();
    if (!room.isEmpty())
        lines << QStringLiteral("Raum: %1").arg(room);
    const QString lecturer = event.value(QStringLiteral("lecturer")).toString();
    if (!lecturer.isEmpty())
        lines << QStringLiteral("Dozent: %1").arg(lecturer);
    return lines.join(QLatin1Char('\n'));
}

void TimetableController::setLoading(bool loading)
{
    if (m_loading == loading)
        return;
    m_loading = loading;
    Q_EMIT loadingChanged();
}

void TimetableController::setOffline(bool offline)
{
    if (m_offline == offline)
        return;
    m_offline = offline;
    Q_EMIT offlineChanged();
}

void TimetableController::setErrorMessage(const QString &message)
{
    m_errorMessage = message;
    Q_EMIT errorMessageChanged();
}

QVariantMap TimetableController::roomLocation(const QString &room) const
{
    const auto location = CampusLocations::locateRoom(room);
    if (!location)
        return {};
    QVariantMap m;
    m[QStringLiteral("room")] = location->room;
    m[QStringLiteral("campusName")] = location->campusName;
    m[QStringLiteral("buildingName")] = location->buildingName;
    m[QStringLiteral("floorLabel")] = location->floorLabel;
    m[QStringLiteral("address")] = location->address;
    m[QStringLiteral("latitude")] = location->latitude;
    m[QStringLiteral("longitude")] = location->longitude;
    m[QStringLiteral("approximate")] = location->approximate;
    m[QStringLiteral("mapUrl")] = CampusLocations::osmWebUrl(location->latitude, location->longitude);
    return m;
}

} // namespace stundenplan
