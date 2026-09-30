cmake_minimum_required(VERSION 3.20)

project(hestundenplan-desktop VERSION 0.1.0 LANGUAGES CXX)

# Set by CI to github.run_number (see .github/workflows/release.yml) so the in-app OTA-update
# check has something meaningful to compare the latest release tag ("v<run number>") against.
# Local/dev builds default to 0, which never reports an update as available.
set(APP_VERSION_CODE "0" CACHE STRING "Build version code compared against GitHub release tags")
# Human-readable version shown in "Über die App" — set by CI to the same APP_VERSION_NAME used
# for the release title.
set(APP_VERSION_NAME "dev" CACHE STRING "Human-readable version shown in the app")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

find_package(ECM REQUIRED NO_MODULE)
list(APPEND CMAKE_MODULE_PATH ${ECM_MODULE_PATH})

include(KDEInstallDirs)
include(KDECMakeSettings)
include(KDECompilerSettings NO_POLICY_SCOPE)

set(QT_DEFAULT_MAJOR_VERSION 6)
# 6.7: per-corner Rectangle radii and the Shape curve renderer, both used by the GNOME look.
find_package(Qt6 6.7 REQUIRED COMPONENTS Core Gui Qml Quick QuickControls2 Network DBus)
find_package(KF6 REQUIRED COMPONENTS
    CoreAddons
    Config
    I18n
    Notifications
    DBusAddons
)
find_package(LibXml2 REQUIRED)

qt_standard_project_setup(REQUIRES 6.7)

qt_add_executable(hestundenplan-desktop
    src/main.cpp
    src/core/Models.h
    src/core/Models.cpp
    src/core/HtmlDocument.h
    src/core/HtmlDocument.cpp
    src/core/QisParser.h
    src/core/QisParser.cpp
    src/core/QisRepository.h
    src/core/QisRepository.cpp
    src/core/OverlapLayout.h
    src/core/OverlapLayout.cpp
    src/core/NextEvent.h
    src/core/NextEvent.cpp
    src/core/SettingsStore.h
    src/core/SettingsStore.cpp
    src/core/TimetableCache.h
    src/core/TimetableCache.cpp
    src/core/UpdateManager.h
    src/core/UpdateManager.cpp
    src/core/NotificationManager.h
    src/core/NotificationManager.cpp
    src/core/ReminderScheduler.h
    src/core/ReminderScheduler.cpp
    src/core/DesktopStyle.h
    src/core/DesktopStyle.cpp
    src/controller/TimetableController.h
    src/controller/TimetableController.cpp
)

set_source_files_properties(qml/components/AppTheme.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(hestundenplan-desktop
    URI de.hsesslingen.stundenplan.desktop
    VERSION 1.0
    # Linking the (static, plugin-less) Adwaita style module below would otherwise make Qt scan
    # for QML plugins to link statically and warn about every Kirigami one it cannot.
    NO_IMPORT_SCAN
    QML_FILES
        qml/Main.qml
        qml/PlanPage.qml
        qml/WeekGridView.qml
        qml/DayGridView.qml
        qml/components/EventBlock.qml
        qml/components/NowLine.qml
        qml/components/DateChip.qml
        qml/components/OfflineBanner.qml
        qml/components/AppTheme.qml
        qml/components/SegmentedSwitch.qml
        qml/components/HeaderBar.qml
        qml/components/Banner.qml
        qml/components/PrefPage.qml
        qml/components/PrefGroup.qml
        qml/components/PrefRow.qml
        qml/components/AppSheet.qml
        qml/components/WindowFrame.qml
        qml/dialogs/EventDetailSheet.qml
        qml/dialogs/SearchDialog.qml
        qml/dialogs/UpdateDialog.qml
        qml/dialogs/ChangelogDialog.qml
        qml/settings/SettingsHubPage.qml
        qml/settings/StudiengaengePage.qml
        qml/settings/NotificationsPage.qml
        qml/settings/AppearancePage.qml
        qml/settings/AboutPage.qml
    RESOURCES
        icons/org.hsesslingen.stundenplan.desktop.svg
)

# The GNOME look's Qt Quick Controls style (see DesktopStyle.h): a plain QML module compiled into
# the binary and selected by name through QQuickStyle. Its files are aliased to the module root so
# they sit next to the generated qmldir and see each other (and the Adw singleton) without imports.
set(ADWAITA_STYLE_QML
    qml/adwaita/Adw.qml
    qml/adwaita/ButtonContent.qml
    qml/adwaita/FocusRing.qml
    qml/adwaita/BusyIndicator.qml
    qml/adwaita/Button.qml
    qml/adwaita/CheckBox.qml
    qml/adwaita/ItemDelegate.qml
    qml/adwaita/Label.qml
    qml/adwaita/Menu.qml
    qml/adwaita/MenuItem.qml
    qml/adwaita/MenuSeparator.qml
    qml/adwaita/RadioButton.qml
    qml/adwaita/ScrollBar.qml
    qml/adwaita/ScrollView.qml
    qml/adwaita/SpinBox.qml
    qml/adwaita/Switch.qml
    qml/adwaita/TextField.qml
    qml/adwaita/ToolButton.qml
    qml/adwaita/ToolTip.qml
)
foreach(qml_file IN LISTS ADWAITA_STYLE_QML)
    get_filename_component(qml_name ${qml_file} NAME)
    set_source_files_properties(${qml_file} PROPERTIES QT_RESOURCE_ALIAS ${qml_name})
endforeach()
set_source_files_properties(qml/adwaita/Adw.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(hestundenplan-adwaita-style STATIC
    URI de.hsesslingen.stundenplan.adwaita
    VERSION 1.0
    NO_PLUGIN
    OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/de/hsesslingen/stundenplan/adwaita
    QML_FILES ${ADWAITA_STYLE_QML}
)

target_include_directories(hestundenplan-desktop PRIVATE
    src
    ${LIBXML2_INCLUDE_DIR}
)

# APP_VERSION_CODE/APP_VERSION_NAME were previously only declared as CMake cache variables and
# never actually forwarded to the compiler — every build silently compiled in the "0"/"dev"
# fallback from UpdateManager.cpp regardless of what CI passed via -D, so the OTA-update check
# never worked and the About page always showed a stale hardcoded version string.
target_compile_definitions(hestundenplan-desktop PRIVATE
    APP_VERSION_CODE=${APP_VERSION_CODE}
    APP_VERSION_NAME="${APP_VERSION_NAME}"
)

target_link_libraries(hestundenplan-desktop PRIVATE
    hestundenplan-adwaita-style
    Qt6::Core
    Qt6::Gui
    Qt6::Qml
    Qt6::Quick
    Qt6::QuickControls2
    Qt6::Network
    Qt6::DBus
    KF6::CoreAddons
    KF6::ConfigCore
    KF6::ConfigGui
    KF6::I18n
    KF6::Notifications
    KF6::DBusAddons
    ${LIBXML2_LIBRARIES}
)

install(TARGETS hestundenplan-desktop ${KDE_INSTALL_TARGETS_DEFAULT_ARGS})
install(FILES org.hsesslingen.stundenplan.desktop.desktop DESTINATION ${KDE_INSTALL_APPDIR})
install(FILES org.hsesslingen.stundenplan.desktop.appdata.xml DESTINATION ${KDE_INSTALL_METAINFODIR})
install(FILES icons/org.hsesslingen.stundenplan.desktop.svg
    DESTINATION ${KDE_INSTALL_FULL_ICONDIR}/hicolor/scalable/apps
)
# KDE_INSTALL_KNOTIFYRCDIR (from this ECM version) still points at the legacy "knotifications5"
# directory, but KF6's KNotification only searches "knotifications6" (verified against other KF6
# apps' installed .notifyrc files on this system) — install there explicitly instead. The file is
# also named after KAboutData's componentName ("hestundenplan-desktop", set in main.cpp), which is
# what KNotification looks up by default, not the app/icon id.
install(FILES hestundenplan-desktop.notifyrc DESTINATION ${KDE_INSTALL_FULL_DATADIR}/knotifications6)
