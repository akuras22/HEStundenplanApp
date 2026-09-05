#include "ReminderScheduler.h"

#include "NotificationManager.h"
#include "QisRepository.h"
#include "SettingsStore.h"
#include "TimetableCache.h"

#include <QDateTime>
#include <QTime>

namespace stundenplan {

ReminderScheduler::ReminderScheduler(SettingsStore *settings, TimetableCache *cache, NotificationManager *notifications, QObject *parent)
    : QObject(parent)
    , m_settings(settings)
    , m_cache(cache)
    , m_notifications(notifications)
    , m_repository(new QisRepository(this))
{
    connect(&m_timer, &QTimer::timeout, this, &ReminderScheduler::runCheck);
    connect(m_notifications, &NotificationManager::openRequested, this, &ReminderScheduler::openRequested);
}

void ReminderScheduler::start()
{
    m_timer.start(kWorkIntervalMinutes * 60 * 1000);
    runCheck();
}

void ReminderScheduler::stop()
{
    m_timer.stop();
}

void ReminderScheduler::runCheck()
{
    if (!m_settings->remindersEnabled())
        return;
    const auto studiengang = m_settings->selectedStudiengang();
    if (!studiengang.has_value())
        return;

    const QDate today = QDate::currentDate();
    auto weekdayOpt = weekdayFromDate(today);
    if (!weekdayOpt.has_value())
        return; // weekend: nothing scheduled

    const QDate monday = today.addDays(-(today.dayOfWeek() - 1));

    auto onEvents = [this, today](const QList<TimetableEvent> &events) {
        processDayEvents(events, today);
    };

    // One-shot connections scoped to this fetch, then fall back to the offline cache on failure —
    // mirrors the worker's try/catch-then-cache-fallback structure.
    auto *successConn = new QMetaObject::Connection;
    auto *failConn = new QMetaObject::Connection;
    *successConn = connect(m_repository, &QisRepository::timetableFetched, this, [this, onEvents, successConn, failConn](const QList<TimetableEvent> &events) {
        QObject::disconnect(*successConn);
        QObject::disconnect(*failConn);
        delete successConn;
        delete failConn;
        onEvents(events);
    });
    *failConn = connect(m_repository, &QisRepository::fetchFailed, this,
                         [this, onEvents, studiengang, monday, successConn, failConn](const QString &, bool forTimetable) {
                             if (!forTimetable)
                                 return;
                             QObject::disconnect(*successConn);
                             QObject::disconnect(*failConn);
                             delete successConn;
                             delete failConn;
                             if (auto cached = m_cache->get(*studiengang, monday))
                                 onEvents(cached->events);
                         });
    m_repository->fetchTimetable(*studiengang, monday);
}

void ReminderScheduler::processDayEvents(const QList<TimetableEvent> &events, const QDate &today)
{
    auto weekdayOpt = weekdayFromDate(today);
    if (!weekdayOpt.has_value())
        return;
    const Weekday weekday = *weekdayOpt;
    const auto hiddenKeys = m_settings->hiddenEventKeys();
    const auto leadMinutesSet = m_settings->reminderLeadMinutesSet();
    const int nowMinutes = QTime::currentTime().hour() * 60 + QTime::currentTime().minute();
    const QString dateKey = today.toString(Qt::ISODate);

    QList<TimetableEvent> dayEvents;
    for (const auto &e : events) {
        if (e.day == weekday && !hiddenKeys.contains(e.groupKey()))
            dayEvents.append(e);
    }

    // Each configured lead time is tracked as its own dedup key ("<groupKey>@<lead>") so multiple
    // simultaneous lead times for the same lecture each fire independently.
    for (int leadMinutes : leadMinutesSet) {
        const int windowStart = nowMinutes - 5;
        const int windowEnd = nowMinutes + leadMinutes + 5;
        for (const auto &event : dayEvents) {
            if (event.startMinutes < windowStart || event.startMinutes > windowEnd)
                continue;
            const QString dedupKey = QStringLiteral("%1@%2").arg(event.groupKey()).arg(leadMinutes);
            if (m_settings->hasNotifiedToday(dateKey, dedupKey))
                continue;
            const int minutesUntilStart = event.startMinutes - nowMinutes;
            m_notifications->notifyLectureReminder(event.title, event.room, minutesUntilStart, today);
            m_settings->markNotifiedToday(dateKey, dedupKey);
        }
    }
}

} // namespace stundenplan
