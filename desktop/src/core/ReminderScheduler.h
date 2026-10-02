#pragma once

#include "Models.h"
#include <QDate>
#include <QObject>
#include <QTimer>

namespace stundenplan {

class SettingsStore;
class TimetableCache;
class NotificationManager;
class QisRepository;

/**
 * Polls every WORK_INTERVAL_MINUTES while the app is running — with "Im Hintergrund
 * weiterlaufen" on, also after its window was closed (see BackgroundService); there's no
 * equivalent of Android's WorkManager for a fully quit app — and posts lecture-reminder
 * notifications. Ported from LectureReminderWorker.kt.
 */
class ReminderScheduler : public QObject
{
    Q_OBJECT
public:
    explicit ReminderScheduler(SettingsStore *settings,
                                TimetableCache *cache,
                                NotificationManager *notifications,
                                QObject *parent = nullptr);

    void start();
    void stop();

Q_SIGNALS:
    void openRequested(const QDate &date);

private:
    void runCheck();
    void processDayEvents(const QList<TimetableEvent> &dayEvents, const QDate &today);

    static constexpr int kWorkIntervalMinutes = 15;

    QTimer m_timer;
    SettingsStore *m_settings;
    TimetableCache *m_cache;
    NotificationManager *m_notifications;
    QisRepository *m_repository;
};

} // namespace stundenplan
