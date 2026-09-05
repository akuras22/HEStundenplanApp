#include "NotificationManager.h"

#include <KNotification>

namespace stundenplan {

NotificationManager::NotificationManager(QObject *parent)
    : QObject(parent)
{
}

void NotificationManager::notifyLectureReminder(const QString &title, const QString &room, int minutesUntilStart, const QDate &date)
{
    auto *notification = new KNotification(QStringLiteral("lectureReminder"));
    notification->setTitle(title);
    const QString roomPart = room.isEmpty() ? QString() : QStringLiteral(" · %1").arg(room);
    notification->setText(minutesUntilStart <= 0
                               ? QStringLiteral("Beginnt jetzt%1").arg(roomPart)
                               : QStringLiteral("Beginnt in %1 Min.%2").arg(minutesUntilStart).arg(roomPart));
    notification->setIconName(QStringLiteral("org.hsesslingen.stundenplan.desktop"));
    auto *openAction = notification->addDefaultAction(QStringLiteral("Öffnen"));
    connect(openAction, &KNotificationAction::activated, this, [this, date]() { Q_EMIT openRequested(date); });
    notification->sendEvent();
}

void NotificationManager::notifyTest()
{
    auto *notification = new KNotification(QStringLiteral("testNotification"));
    notification->setTitle(QStringLiteral("Test-Benachrichtigung"));
    notification->setText(QStringLiteral("So sehen deine Vorlesungs-Erinnerungen aus."));
    notification->setIconName(QStringLiteral("org.hsesslingen.stundenplan.desktop"));
    notification->sendEvent();
}

} // namespace stundenplan
