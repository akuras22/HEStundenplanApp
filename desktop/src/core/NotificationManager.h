#pragma once

#include <QDate>
#include <QObject>
#include <QString>

namespace stundenplan {

/** Thin KNotification wrapper for lecture-reminder toasts. Ported from NotificationHelper.kt. */
class NotificationManager : public QObject
{
    Q_OBJECT
public:
    explicit NotificationManager(QObject *parent = nullptr);

    /** Posts a "starts soon" reminder; clicking it asks to jump to `date`'s Tag-Ansicht. */
    void notifyLectureReminder(const QString &title, const QString &room, int minutesUntilStart, const QDate &date);

    void notifyTest();

Q_SIGNALS:
    void openRequested(const QDate &date);
};

} // namespace stundenplan
