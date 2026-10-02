#include "BackgroundService.h"

#include "NotificationManager.h"
#include "SettingsStore.h"

#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QGuiApplication>
#include <QStandardPaths>

namespace stundenplan {

namespace {

QString autostartFilePath()
{
    return QStandardPaths::writableLocation(QStandardPaths::GenericConfigLocation)
        + QStringLiteral("/autostart/org.hsesslingen.stundenplan.desktop.desktop");
}

} // namespace

BackgroundService::BackgroundService(SettingsStore *settings, NotificationManager *notifications, QObject *parent)
    : QObject(parent)
    , m_settings(settings)
    , m_notifications(notifications)
{
    connect(m_settings, &SettingsStore::runInBackgroundChanged, this, &BackgroundService::apply);
    connect(m_settings, &SettingsStore::autostartChanged, this, &BackgroundService::syncAutostartEntry);
    apply();
}

void BackgroundService::apply()
{
    QGuiApplication::setQuitOnLastWindowClosed(!m_settings->runInBackground());
    syncAutostartEntry();
}

void BackgroundService::syncAutostartEntry()
{
    const QString path = autostartFilePath();
    // Starting at login only makes sense when the app then stays in the background.
    if (!(m_settings->autostart() && m_settings->runInBackground())) {
        QFile::remove(path);
        return;
    }
    QDir().mkpath(QFileInfo(path).absolutePath());
    QFile file(path);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text))
        return;
    const QString exec = QCoreApplication::applicationFilePath();
    file.write(QStringLiteral(
                   "[Desktop Entry]\n"
                   "Type=Application\n"
                   "Name=Stundenplan\n"
                   "Comment=Vorlesungs-Erinnerungen im Hintergrund\n"
                   "Exec=\"%1\" --background\n"
                   "Icon=org.hsesslingen.stundenplan.desktop\n"
                   "Terminal=false\n"
                   "NoDisplay=true\n"
                   "X-GNOME-Autostart-enabled=true\n")
                   .arg(exec)
                   .toUtf8());
}

void BackgroundService::windowClosed()
{
    if (!m_settings->runInBackground() || m_settings->backgroundHintShown())
        return;
    m_settings->setBackgroundHintShown(true);
    m_notifications->notifyRunningInBackground();
}

void BackgroundService::quit()
{
    QCoreApplication::quit();
}

} // namespace stundenplan
