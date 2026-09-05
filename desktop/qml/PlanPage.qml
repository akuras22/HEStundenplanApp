import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "components"
import "dialogs"
import "settings"

Kirigami.Page {
    id: root

    title: timetableController.selectedStudiengang.code || qsTr("Stundenplan")
    padding: Kirigami.Units.smallSpacing

    // Tappable title with a dropdown to quickly switch between starred Studiengänge — only
    // worth showing once there's actually a choice to make (matches the Android app, which also
    // only shows this when the user has more than one favorite).
    titleDelegate: favoriteSwitcherComponent

    Component {
        id: favoriteSwitcherComponent
        Controls.ToolButton {
            id: titleButton
            readonly property var favorites: timetableController.favoriteStudiengaenge
            text: root.title
            font.bold: true
            font.pointSize: Kirigami.Theme.defaultFont.pointSize * 1.15
            icon.name: favorites.length > 1 ? "arrow-down" : ""
            display: Controls.AbstractButton.TextBesideIcon
            LayoutMirroring.enabled: false
            enabled: favorites.length > 1
            onClicked: favoriteMenu.open()

            Controls.Menu {
                id: favoriteMenu
                Repeater {
                    model: titleButton.favorites
                    delegate: Controls.MenuItem {
                        required property var modelData
                        text: modelData.code
                        onTriggered: timetableController.selectStudiengang(modelData.code, modelData.abstgvnr, modelData.parallelid)
                    }
                }
            }
        }
    }

    property bool dayView: settingsStore.defaultViewIsDay
    property date currentDate: new Date()

    function mondayOf(date) {
        var d = new Date(date)
        var jsDay = d.getDay()
        var diff = jsDay === 0 ? -6 : 1 - jsDay
        d.setDate(d.getDate() + diff)
        d.setHours(0, 0, 0, 0)
        return d
    }

    function showDayView(date) {
        root.currentDate = date
        root.dayView = true
        var monday = mondayOf(date)
        if (monday.getTime() !== mondayOf(timetableController.weekMonday).getTime())
            timetableController.setWeekMonday(monday)
    }

    function goToday() {
        var today = new Date()
        root.currentDate = today
        timetableController.goToday()
    }

    function stepDay(delta) {
        var d = new Date(root.currentDate)
        d.setDate(d.getDate() + delta)
        // Skip weekends, matching QIS (which never schedules Sat/Sun).
        while (d.getDay() === 0 || d.getDay() === 6)
            d.setDate(d.getDate() + delta)
        showDayView(d)
    }

    function stepWeek(delta) {
        var d = new Date(timetableController.weekMonday)
        d.setDate(d.getDate() + delta * 7)
        timetableController.setWeekMonday(d)
    }

    actions: [
        Kirigami.Action {
            icon.name: "search"
            text: qsTr("Suche")
            enabled: !!timetableController.selectedStudiengang.code
            onTriggered: searchSheet.open()
        },
        Kirigami.Action {
            icon.name: "go-jump-today"
            text: qsTr("Heute")
            enabled: !!timetableController.selectedStudiengang.code
            onTriggered: root.goToday()
        },
        Kirigami.Action {
            icon.name: "view-refresh"
            text: qsTr("Aktualisieren")
            enabled: !!timetableController.selectedStudiengang.code
            onTriggered: timetableController.refresh()
        },
        Kirigami.Action {
            icon.name: "configure"
            text: qsTr("Einstellungen")
            onTriggered: applicationWindow().pageStack.push(settingsHubComponent)
        }
    ]

    Component {
        id: settingsHubComponent
        SettingsHubPage {}
    }

    ColumnLayout {
        anchors.fill: parent
        spacing: Kirigami.Units.smallSpacing

        OfflineBanner {}

        Kirigami.InlineMessage {
            Layout.fillWidth: true
            type: Kirigami.MessageType.Error
            visible: !!timetableController.errorMessage
            text: timetableController.errorMessage
        }

        Kirigami.PlaceholderMessage {
            visible: !timetableController.selectedStudiengang.code
            Layout.fillWidth: true
            Layout.fillHeight: true
            icon.name: "view-calendar-week"
            text: qsTr("Kein Studiengang ausgewählt")
            explanation: qsTr("Wähle in den Einstellungen einen Studiengang aus, um deinen Stundenplan zu sehen.")
            helpfulAction: Kirigami.Action {
                text: qsTr("Studiengang wählen")
                onTriggered: applicationWindow().pageStack.push(settingsHubComponent)
            }
        }

        ColumnLayout {
            visible: !!timetableController.selectedStudiengang.code
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: Kirigami.Units.smallSpacing

            RowLayout {
                Layout.fillWidth: true

                Controls.ToolButton {
                    icon.name: "go-previous"
                    onClicked: root.dayView ? root.stepDay(-1) : root.stepWeek(-1)
                }

                SegmentedSwitch {
                    Layout.fillWidth: true
                    currentIndex: root.dayView ? 1 : 0
                    model: [
                        { text: qsTr("Woche"), icon: "view-calendar-week" },
                        { text: qsTr("Tag"), icon: "view-calendar-day" }
                    ]
                    onActivated: (index) => root.dayView = (index === 1)
                }

                Controls.ToolButton {
                    icon.name: "go-next"
                    onClicked: root.dayView ? root.stepDay(1) : root.stepWeek(1)
                }
            }

            // A fixed-size Item (not a Loader whose own visibility toggles) keeps the header
            // above stable during a refetch — only a small overlay spinner appears/disappears,
            // instead of the whole grid (and the toolbar above it) jumping around.
            Item {
                Layout.fillWidth: true
                Layout.fillHeight: true

                Loader {
                    anchors.fill: parent
                    sourceComponent: root.dayView ? dayViewComponent : weekViewComponent
                }

                Rectangle {
                    anchors.fill: parent
                    visible: timetableController.loading
                    color: Qt.rgba(AppTheme.backgroundColor.r, AppTheme.backgroundColor.g, AppTheme.backgroundColor.b, 0.6)

                    Controls.BusyIndicator {
                        anchors.centerIn: parent
                        running: parent.visible
                    }
                }
            }
        }
    }

    Component {
        id: weekViewComponent
        WeekGridView {
            weekMonday: timetableController.weekMonday
            onEventClicked: (eventData) => {
                detailSheet.eventData = eventData
                detailSheet.open()
            }
        }
    }

    Component {
        id: dayViewComponent
        DayGridView {
            currentDate: root.currentDate
            onEventClicked: (eventData) => {
                detailSheet.eventData = eventData
                detailSheet.open()
            }
        }
    }

    EventDetailSheet {
        id: detailSheet
    }

    SearchDialog {
        id: searchSheet
        onEventSelected: (eventData) => {
            detailSheet.eventData = eventData
            detailSheet.open()
        }
    }
}
