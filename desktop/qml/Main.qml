import QtQuick
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.ApplicationWindow {
    id: root

    title: timetableController.selectedStudiengang.code
           ? timetableController.selectedStudiengang.code
           : qsTr("Stundenplan")

    width: Kirigami.Units.gridUnit * 45
    height: Kirigami.Units.gridUnit * 32

    // Force single-column navigation: Kirigami's PageRow auto-switches to a multi-column layout
    // once the window is wide enough (showing e.g. the settings hub and a sub-page side by side),
    // which is what caused "back" to not really go back — both pages stayed visible at once, and
    // whichever was pushed last kept its list item looking selected in the one behind it. Single
    // column also re-enables Kirigami's automatic back button in the page header.
    pageStack.defaultColumnWidth: width * 2
    pageStack.globalToolBar.showNavigationButtons: Kirigami.ApplicationHeaderStyle.ShowBackButton

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
