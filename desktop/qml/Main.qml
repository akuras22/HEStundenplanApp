import QtQuick
import QtQuick.Controls as Controls
import QtWebEngine
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import de.hsesslingen.stundenplan.platform
import "components"
import "settings"

Kirigami.ApplicationWindow {
    id: root

    title: timetableController.selectedStudiengang.code
           ? timetableController.selectedStudiengang.code
           : qsTr("Stundenplan")

    // "--background" (the autostart entry): no window until a reminder or a relaunch asks for it.
    visible: !startHidden

    width: Kirigami.Units.gridUnit * 45
    height: Kirigami.Units.gridUnit * 32
    minimumWidth: Kirigami.Units.gridUnit * 20
    minimumHeight: Kirigami.Units.gridUnit * 16

    // GNOME look: like a libadwaita app, the window draws its own header bar (HeaderBar.qml) and
    // frame (WindowFrame.qml) instead of getting a title bar from the window manager — GNOME has
    // none to give to Wayland apps, and Qt's built-in fallback one looks nothing like GNOME's.
    // KDE look: a normal KWin-decorated window with Kirigami's own toolbar.
    flags: DesktopStyle.clientSideDecorations ? Qt.Window | Qt.FramelessWindowHint : Qt.Window
    color: DesktopStyle.clientSideDecorations ? "transparent" : AppTheme.backgroundColor

    header: Loader {
        active: AppTheme.gnome
        visible: active
        sourceComponent: HeaderBar {
            window: root
        }
    }

    WindowFrame {
        window: root
    }

    // Force single-column navigation: Kirigami's PageRow auto-switches to a multi-column layout
    // once the window is wide enough (showing e.g. the settings hub and a sub-page side by side),
    // which is what caused "back" to not really go back — both pages stayed visible at once, and
    // whichever was pushed last kept its list item looking selected in the one behind it. Single
    // column also re-enables Kirigami's automatic back button in the page header.
    pageStack.defaultColumnWidth: width * 2
    pageStack.globalToolBar.showNavigationButtons: Kirigami.ApplicationHeaderStyle.ShowBackButton
    pageStack.globalToolBar.style: AppTheme.gnome ? Kirigami.ApplicationHeaderStyle.None
                                                  : Kirigami.ApplicationHeaderStyle.Auto

    /** Brings the window back — also after it was closed while running in the background. */
    function showFromBackground() {
        if (root.visibility === Window.Minimized)
            root.showNormal()
        else
            root.show()
        root.raise()
        root.requestActivate()
    }

    // With "Im Hintergrund weiterlaufen" on, closing only hides the window (see BackgroundService).
    onClosing: backgroundService.windowClosed()

    // A real quit, also when closing the window would just send the app to the background.
    Shortcut {
        sequences: [StandardKey.Quit]
        context: Qt.ApplicationShortcut
        onActivated: Qt.quit()
    }

    function openDate(date) {
        root.showFromBackground()
        planPage.showDayView(date)
        while (pageStack.depth > 1)
            pageStack.pop()
    }

    pageStack.initialPage: PlanPage {
        id: planPage
    }

    globalDrawer: null

    // Shared, persistent browser profile for the Hochschul-Login. Unlike an off-the-record one it
    // keeps the SSO session (Keycloak) cookies, so the login survives an app restart and can be
    // renewed silently (SilentLoginRefresh.qml) instead of asking for password + second factor
    // again. Both the visible login page and the invisible refresh view use this one profile.
    WebEngineProfile {
        id: qisLoginProfileItem
        storageName: "qis-login"
        offTheRecord: false
        persistentCookiesPolicy: WebEngineProfile.AllowPersistentCookies
    }
    property alias qisLoginProfile: qisLoginProfileItem

    // Invisible: renews the QIS session from the still-valid SSO session with no UI, whenever
    // QisSession asks for it (current session ran out, SSO session still alive).
    Loader {
        active: qisSession.silentRefreshActive
        sourceComponent: SilentLoginRefresh {}
    }

    Component.onCompleted: {
        // Collect the Shibboleth cookies from the shared profile for the whole run, and renew a
        // session that expired while the app was closed right away.
        qisSession.attachProfile(qisLoginProfileItem)
        qisSession.startSilentRefreshIfPossible()
    }
}
