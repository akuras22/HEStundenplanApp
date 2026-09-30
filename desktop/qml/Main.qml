import QtQuick
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import de.hsesslingen.stundenplan.platform
import "components"

Kirigami.ApplicationWindow {
    id: root

    title: timetableController.selectedStudiengang.code
           ? timetableController.selectedStudiengang.code
           : qsTr("Stundenplan")

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

    function openDate(date) {
        planPage.showDayView(date)
        while (pageStack.depth > 1)
            pageStack.pop()
    }

    pageStack.initialPage: PlanPage {
        id: planPage
    }

    globalDrawer: null
}
