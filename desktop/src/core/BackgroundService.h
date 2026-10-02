#pragma once

#include <QObject>

namespace stundenplan {

class NotificationManager;
class SettingsStore;

/**
 * Keeps the app (and with it ReminderScheduler's lecture reminders) running once its window is
 * closed, if the user turned that on, and optionally starts it hidden at login via an autostart
 * entry. Launching the app again (app grid, launcher) shows the running instance's window (see
 * KDBusService in main.cpp); Strg+Q or "Beenden" in the settings quits for good.
 *
 * Deliberately no tray icon: GNOME — the default look — shows none without an extension, and
 * KStatusNotifierItem would pull in QtWidgets and a QApplication just for its menu.
 */
class BackgroundService : public QObject
{
    Q_OBJECT
public:
    BackgroundService(SettingsStore *settings, NotificationManager *notifications, QObject *parent = nullptr);

    /** Called by the window when it closes: tells the user once that the app keeps running. */
    Q_INVOKABLE void windowClosed();

    /** A real quit, also while closing the window would only send the app to the background. */
    Q_INVOKABLE void quit();

private:
    void apply();
    void syncAutostartEntry();

    SettingsStore *m_settings;
    NotificationManager *m_notifications;
};

} // namespace stundenplan
