#include "controller/MensaController.h"
#include "controller/TimetableController.h"
#include "controller/TransitController.h"
#include "core/BackgroundService.h"
#include "core/DesktopStyle.h"
#include "core/ImageNetworkCache.h"
#include "core/NotificationManager.h"
#include "core/ReminderScheduler.h"
#include "core/SettingsStore.h"
#include "core/TimetableCache.h"
#include "core/UpdateManager.h"

#include <KAboutData>
#include <KDBusService>
#include <KLocalizedContext>
#include <KLocalizedString>
#include <QCommandLineParser>
#include <QGuiApplication>
#include <QIcon>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QtQml>
#include <QUrl>

int main(int argc, char *argv[])
{
    // See DesktopStyle.h: org.kde.desktop under Plasma, our own Adwaita-shaped style everywhere
    // else — QQuickStyle::setStyle() must run before QGuiApplication exists.
    stundenplan::DesktopStyle::selectQuickControlsStyle();

    QGuiApplication app(argc, argv);
    // Must exist before the QML engine loads anything: both the app's QML and the Adwaita style
    // import it as the DesktopStyle singleton.
    auto *desktopStyle = new stundenplan::DesktopStyle(&app);
    qmlRegisterSingletonInstance("de.hsesslingen.stundenplan.platform", 1, 0, "DesktopStyle", desktopStyle);
    QGuiApplication::setOrganizationName(QStringLiteral("HS Esslingen"));
    QGuiApplication::setApplicationName(QStringLiteral("HEStundenplan"));
    QGuiApplication::setWindowIcon(QIcon::fromTheme(QStringLiteral("org.hsesslingen.stundenplan.desktop")));

    KLocalizedString::setApplicationDomain("hestundenplan-desktop");

    KAboutData aboutData(QStringLiteral("hestundenplan-desktop"),
                          i18n("Stundenplan"),
                          stundenplan::UpdateManager::appVersionName(),
                          i18n("Stundenplan der Hochschule Esslingen"),
                          KAboutLicense::GPL_V3,
                          i18n("© 2026 HS Esslingen Stundenplan"));
    aboutData.setHomepage(QStringLiteral("https://github.com/akuras22/HEStundenplanApp"));
    // Must match the installed .desktop file's name — it becomes the window's Wayland app id,
    // which is how GNOME Shell and Plasma's task manager find the app's icon and launcher entry.
    // (KAboutData's default would be "org.kde.hestundenplan-desktop".)
    aboutData.setDesktopFileName(QStringLiteral("org.hsesslingen.stundenplan.desktop"));
    KAboutData::setApplicationData(aboutData);

    QCommandLineParser parser;
    aboutData.setupCommandLine(&parser);
    const QCommandLineOption backgroundOption(QStringLiteral("background"),
                                              i18n("Ohne Fenster starten, nur für Erinnerungen (Autostart)"));
    parser.addOption(backgroundOption);
    parser.process(app);
    aboutData.processCommandLine(&parser);
    const bool startHidden = parser.isSet(backgroundOption);

    // One instance only — launching the app again (e.g. from the app grid while it runs in the
    // background) hands over to the running one, which shows its window, and this one exits here.
    KDBusService dbusService(KDBusService::Unique);

    using namespace stundenplan;

    qRegisterMetaType<Studiengang>("stundenplan::Studiengang");
    qRegisterMetaType<TimetableEvent>("stundenplan::TimetableEvent");
    qRegisterMetaType<QList<Studiengang>>("QList<stundenplan::Studiengang>");
    qRegisterMetaType<QList<TimetableEvent>>("QList<stundenplan::TimetableEvent>");
    qRegisterMetaType<MensaDay>("stundenplan::MensaDay");

    auto *settings = new SettingsStore(&app);
    auto *cache = new TimetableCache();
    auto *controller = new TimetableController(settings, cache, &app);
    auto *updateManager = new UpdateManager(&app);
    auto *notifications = new NotificationManager(&app);
    auto *reminderScheduler = new ReminderScheduler(settings, cache, notifications, &app);
    auto *mensaController = new MensaController(settings, &app);
    auto *transitController = new TransitController(settings, controller, &app);
    auto *backgroundService = new BackgroundService(settings, notifications, &app);

    // Declared before the engine so it outlives it — the engine doesn't take ownership.
    ImageNetworkCache imageNetworkCache;
    QQmlApplicationEngine engine;
    engine.setNetworkAccessManagerFactory(&imageNetworkCache);
    engine.rootContext()->setContextObject(new KLocalizedContext(&engine));
    engine.rootContext()->setContextProperty(QStringLiteral("settingsStore"), settings);
    engine.rootContext()->setContextProperty(QStringLiteral("timetableController"), controller);
    engine.rootContext()->setContextProperty(QStringLiteral("updateManager"), updateManager);
    engine.rootContext()->setContextProperty(QStringLiteral("notificationManager"), notifications);
    engine.rootContext()->setContextProperty(QStringLiteral("reminderScheduler"), reminderScheduler);
    engine.rootContext()->setContextProperty(QStringLiteral("mensaController"), mensaController);
    engine.rootContext()->setContextProperty(QStringLiteral("transitController"), transitController);
    engine.rootContext()->setContextProperty(QStringLiteral("backgroundService"), backgroundService);
    engine.rootContext()->setContextProperty(QStringLiteral("startHidden"), startHidden);

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

    QObject::connect(&dbusService, &KDBusService::activateRequested, &engine, [&engine](const QStringList &arguments, const QString &) {
        // The autostart entry firing while the app already runs mustn't pop the window up.
        if (!arguments.contains(QStringLiteral("--background")))
            QMetaObject::invokeMethod(engine.rootObjects().value(0), "showFromBackground");
    });

    return app.exec();
}
