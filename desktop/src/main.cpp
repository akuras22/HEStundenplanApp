#include "controller/TimetableController.h"
#include "core/NotificationManager.h"
#include "core/ReminderScheduler.h"
#include "core/SettingsStore.h"
#include "core/TimetableCache.h"
#include "core/UpdateManager.h"

#include <KAboutData>
#include <KLocalizedContext>
#include <KLocalizedString>
#include <QGuiApplication>
#include <QIcon>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QUrl>

int main(int argc, char *argv[])
{
    QGuiApplication app(argc, argv);
    QGuiApplication::setOrganizationName(QStringLiteral("HS Esslingen"));
    QGuiApplication::setApplicationName(QStringLiteral("HEStundenplan"));
    QGuiApplication::setWindowIcon(QIcon::fromTheme(QStringLiteral("org.hsesslingen.stundenplan.desktop")));

    KLocalizedString::setApplicationDomain("hestundenplan-desktop");

    KAboutData aboutData(QStringLiteral("hestundenplan-desktop"),
                          i18n("Stundenplan"),
                          QStringLiteral("0.1.0"),
                          i18n("Stundenplan der Hochschule Esslingen"),
                          KAboutLicense::GPL_V3,
                          i18n("© 2026 HS Esslingen Stundenplan"));
    aboutData.setHomepage(QStringLiteral("https://github.com/akuras22/HEStundenplanApp"));
    KAboutData::setApplicationData(aboutData);

    using namespace stundenplan;

    qRegisterMetaType<Studiengang>("stundenplan::Studiengang");
    qRegisterMetaType<TimetableEvent>("stundenplan::TimetableEvent");
    qRegisterMetaType<QList<Studiengang>>("QList<stundenplan::Studiengang>");
    qRegisterMetaType<QList<TimetableEvent>>("QList<stundenplan::TimetableEvent>");
    qRegisterMetaType<UpdateInfo>("stundenplan::UpdateInfo");

    auto *settings = new SettingsStore(&app);
    auto *cache = new TimetableCache();
    auto *controller = new TimetableController(settings, cache, &app);
    auto *updateManager = new UpdateManager(&app);
    auto *notifications = new NotificationManager(&app);
    auto *reminderScheduler = new ReminderScheduler(settings, cache, notifications, &app);

    QQmlApplicationEngine engine;
    engine.rootContext()->setContextObject(new KLocalizedContext(&engine));
    engine.rootContext()->setContextProperty(QStringLiteral("settingsStore"), settings);
    engine.rootContext()->setContextProperty(QStringLiteral("timetableController"), controller);
    engine.rootContext()->setContextProperty(QStringLiteral("updateManager"), updateManager);
    engine.rootContext()->setContextProperty(QStringLiteral("notificationManager"), notifications);
    engine.rootContext()->setContextProperty(QStringLiteral("reminderScheduler"), reminderScheduler);

    QObject::connect(notifications, &NotificationManager::openRequested, &engine, [&engine](const QDate &date) {
        QMetaObject::invokeMethod(engine.rootObjects().value(0), "openDate", Q_ARG(QVariant, date));
    });

    QObject::connect(settings, &SettingsStore::remindersEnabledChanged, reminderScheduler, [settings, reminderScheduler]() {
        if (settings->remindersEnabled())
            reminderScheduler->start();
        else
            reminderScheduler->stop();
    });
    if (settings->remindersEnabled())
        reminderScheduler->start();

    controller->loadStudiengaenge();
    if (settings->selectedStudiengang().has_value())
        controller->refresh();

    engine.loadFromModule("de.hsesslingen.stundenplan.desktop", "Main");
    if (engine.rootObjects().isEmpty())
        return -1;

    return app.exec();
}
