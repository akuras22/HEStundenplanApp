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

    title: root.transitView ? (transitController.stop.shortName || qsTr("Abfahrten"))
         : root.mensaView ? (mensaController.location.name || qsTr("Mensa"))
         : (timetableController.selectedStudiengang.code || qsTr("Stundenplan"))
    padding: Kirigami.Units.smallSpacing
    topPadding: AppTheme.gnome ? 0 : Kirigami.Units.smallSpacing

    // Tappable title with a dropdown to quickly switch between starred Studiengänge — only
    // worth showing once there's actually a choice to make (matches the Android app, which also
    // only shows this when the user has more than one favorite).
    // KDE look: Kirigami's toolbar shows it via titleDelegate. GNOME look: our HeaderBar picks it
    // up as headerBarTitle instead — titleDelegate must stay untouched there, or Kirigami would
    // put the delegate in a strip of its own above the page now that its toolbar is off.
    // In the Mensa/Abfahrten views the title is the chosen Mensa/stop instead, switchable the
    // same way.
    readonly property Component headerBarTitle: root.transitView ? transitSwitcherComponent
                                              : root.mensaView ? mensaSwitcherComponent
                                              : favoriteSwitcherComponent
    Component.onCompleted: {
        if (!AppTheme.gnome)
            titleDelegate = Qt.binding(() => root.headerBarTitle)
    }

    Component {
        id: favoriteSwitcherComponent
        Controls.ToolButton {
            id: titleButton
            readonly property var favorites: timetableController.favoriteStudiengaenge
            text: root.title
            font.bold: true
            font.pointSize: Kirigami.Theme.defaultFont.pointSize * 1.15
            icon.name: favorites.length > 1 ? AppTheme.icon("arrow-down") : ""
            display: Controls.AbstractButton.TextBesideIcon
            // GNOME puts the drop-down arrow after the title.
            LayoutMirroring.enabled: AppTheme.gnome
            enabled: favorites.length > 1
            // With nothing to switch to it is just the title — don't let a style dim it as disabled.
            opacity: 1
            onClicked: favoriteMenu.open()

            Controls.Menu {
                id: favoriteMenu
                // Drops down below the title rather than covering it; GNOME centers its
                // popovers on the button they belong to.
                x: AppTheme.gnome ? Math.round((titleButton.width - width) / 2) : 0
                y: titleButton.height + (AppTheme.gnome ? 6 : 0)
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

    component MensaLocationItem: Controls.MenuItem {
        required property var modelData
        text: modelData.name
        font.bold: modelData.id === mensaController.locationId
        onTriggered: mensaController.locationId = modelData.id
    }

    Component {
        id: mensaSwitcherComponent
        Controls.ToolButton {
            id: mensaTitleButton
            readonly property bool hasLocation: mensaController.locationId !== 0
            text: root.title
            font.bold: true
            font.pointSize: Kirigami.Theme.defaultFont.pointSize * 1.15
            icon.name: hasLocation ? AppTheme.icon("arrow-down") : ""
            display: Controls.AbstractButton.TextBesideIcon
            LayoutMirroring.enabled: AppTheme.gnome
            enabled: hasLocation
            opacity: 1
            onClicked: mensaMenu.open()

            Controls.Menu {
                id: mensaMenu
                x: AppTheme.gnome ? Math.round((mensaTitleButton.width - width) / 2) : 0
                y: mensaTitleButton.height + (AppTheme.gnome ? 6 : 0)

                // The HS Esslingen campuses first, set apart from the Studierendenwerk's others.
                Repeater {
                    model: mensaController.locations.filter(location => location.atHsEsslingen)
                    delegate: MensaLocationItem {}
                }
                Controls.MenuSeparator {}
                Repeater {
                    model: mensaController.locations.filter(location => !location.atHsEsslingen)
                    delegate: MensaLocationItem {}
                }
            }
        }
    }

    component TransitStopItem: Controls.MenuItem {
        required property var modelData
        text: modelData.name
        font.bold: modelData.id === transitController.stop.id
        onTriggered: transitController.setStop(modelData.id, modelData.name, modelData.shortName)
    }

    Component {
        id: transitSwitcherComponent
        Controls.ToolButton {
            id: transitTitleButton
            readonly property bool hasStop: !!transitController.stop.id
            text: root.title
            font.bold: true
            font.pointSize: Kirigami.Theme.defaultFont.pointSize * 1.15
            icon.name: hasStop ? AppTheme.icon("arrow-down") : ""
            display: Controls.AbstractButton.TextBesideIcon
            LayoutMirroring.enabled: AppTheme.gnome
            enabled: hasStop
            opacity: 1
            onClicked: transitMenu.open()

            Controls.Menu {
                id: transitMenu
                x: AppTheme.gnome ? Math.round((transitTitleButton.width - width) / 2) : 0
                y: transitTitleButton.height + (AppTheme.gnome ? 6 : 0)

                // Campus stops first, then stops found through the search before.
                Repeater {
                    model: transitController.presets
                    delegate: TransitStopItem {}
                }
                Controls.MenuSeparator {
                    visible: recentStops.count > 0
                }
                Repeater {
                    id: recentStops
                    model: transitController.recentStops
                    delegate: TransitStopItem {}
                }
                Controls.MenuSeparator {}
                Controls.MenuItem {
                    text: qsTr("Andere Haltestelle suchen …")
                    icon.name: AppTheme.icon("search")
                    onTriggered: stopSearch.open()
                }
            }
        }
    }

    property bool dayView: settingsStore.defaultViewIsDay
    property date currentDate: new Date()
    /** The third view next to Woche/Tag (see MensaView.qml). */
    property bool mensaView: false
    /** The fourth view: live departures (see TransitView.qml). */
    property bool transitView: false
    /** Mensa and Abfahrten don't depend on a Studiengang and aren't views of the plan. */
    readonly property bool sideView: mensaView || transitView

    // Nothing is fetched from the Speiseplan site / the VVS until their view is actually shown.
    Binding {
        target: mensaController
        property: "active"
        value: root.mensaView
    }
    // The departure board refreshes every minute while active — not while the window is closed
    // (the app may keep running in the background for reminders) or minimized.
    readonly property bool windowShown: !root.Window.window
        || (root.Window.window.visibility !== Window.Hidden && root.Window.window.visibility !== Window.Minimized)
    Binding {
        target: transitController
        property: "active"
        value: root.transitView && root.windowShown
    }

    /** Woche (0), Tag (1) or Mensa (2), as picked in the view switcher. The Mensa view opens on
     *  the day the plan was showing and hands its own day back on the way out, so switching
     *  between them stays on the same day — like the Android app, whose tabs share one date. */
    function selectView(index) {
        if (index === 3) {
            root.mensaView = false
            root.transitView = true
            return
        }
        root.transitView = false
        if (index === 2) {
            if (!root.mensaView) {
                if (root.dayView)
                    mensaController.setDate(root.currentDate)
                else if (mondayOf(timetableController.weekMonday).getTime() === mondayOf(new Date()).getTime())
                    mensaController.setDate(new Date())
                else
                    mensaController.setDate(timetableController.weekMonday)
            }
            root.mensaView = true
            return
        }
        if (root.mensaView) {
            const date = mensaController.date
            root.mensaView = false
            if (index === 1) {
                showDayView(date)
            } else {
                root.dayView = false
                root.currentDate = date
                if (mondayOf(date).getTime() !== mondayOf(timetableController.weekMonday).getTime())
                    timetableController.setWeekMonday(mondayOf(date))
            }
            return
        }
        root.dayView = (index === 1)
    }

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
        root.mensaView = false
        root.transitView = false
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

    readonly property int currentDateQtDay: {
        var jsDay = root.currentDate.getDay()
        return jsDay === 0 ? 7 : jsDay
    }
    // Day view only has events for one specific weekday, not the whole loaded week, so its
    // "nothing here" check must look at that one day rather than timetableController.weekEvents —
    // but eventsForDay() itself has no NOTIFY, so this ternary's day-view branch must still touch
    // weekEvents.length first (see WeekGridView.qml) or a refresh/week-switch won't re-run it.
    readonly property bool viewHasEvents: root.dayView
        ? (timetableController.weekEvents.length >= 0 && timetableController.eventsForDay(root.currentDateQtDay).length > 0)
        : timetableController.weekEvents.length > 0

    // Whether the current view has anything to navigate/refresh: a Studiengang for Woche/Tag, a
    // chosen Mensa for the Mensa view.
    readonly property bool viewReady: root.transitView ? !!transitController.stop.id
                                    : root.mensaView ? mensaController.locationId !== 0
                                    : !!timetableController.selectedStudiengang.code

    actions: [
        Kirigami.Action {
            icon.name: AppTheme.icon("search")
            text: qsTr("Suche")
            visible: !root.sideView
            enabled: !!timetableController.selectedStudiengang.code
            onTriggered: searchSheet.open()
        },
        Kirigami.Action {
            icon.name: AppTheme.icon("go-jump-today")
            text: qsTr("Heute")
            visible: !root.transitView
            enabled: root.viewReady
            onTriggered: root.mensaView ? mensaController.goToday() : root.goToday()
        },
        Kirigami.Action {
            icon.name: AppTheme.icon("view-refresh")
            text: qsTr("Aktualisieren")
            enabled: root.viewReady
            onTriggered: root.transitView ? transitController.refresh()
                       : root.mensaView ? mensaController.refresh()
                       : timetableController.refresh()
        },
        Kirigami.Action {
            icon.name: AppTheme.icon("configure")
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

        OfflineBanner {
            visible: timetableController.offline && !root.sideView
        }

        Banner {
            type: Kirigami.MessageType.Error
            visible: !!timetableController.errorMessage && !root.sideView
            text: timetableController.errorMessage
        }

        Banner {
            id: emptyWeekMessage
            type: Kirigami.MessageType.Information
            // Shown once a fetch actually succeeded but this specific week has no events —
            // e.g. semester break or before the term starts — so an empty grid doesn't read as
            // "the app can't find my schedule" when QIS itself has nothing to show.
            visible: !root.sideView
                     && !!timetableController.selectedStudiengang.code && !timetableController.loading
                     && !timetableController.errorMessage && !timetableController.offline
                     && !root.viewHasEvents
            text: root.dayView
                  ? qsTr("Keine Termine an diesem Tag — vermutlich vorlesungsfreie Zeit oder frei. Mit den Pfeilen andere Tage ansehen.")
                  : qsTr("Keine Termine in dieser Woche gefunden — vermutlich vorlesungsfreie Zeit. Mit den Pfeilen oder \"Heute\" andere Wochen ansehen.")
        }

        // The view switcher stays up even without a Studiengang: the Mensa view doesn't need one.
        // KDE look: the switcher spans the full width. GNOME look: a compact toggle group,
        // centered, with the previous/next arrows right next to it.
        RowLayout {
            Layout.fillWidth: true

            Item {
                visible: AppTheme.gnome
                Layout.fillWidth: true
            }

            Controls.ToolButton {
                icon.name: AppTheme.icon("go-previous")
                // The departure board has no days to step through.
                visible: !root.transitView
                enabled: root.viewReady
                onClicked: root.mensaView ? mensaController.stepDay(-1)
                                          : root.dayView ? root.stepDay(-1) : root.stepWeek(-1)
            }

            SegmentedSwitch {
                Layout.fillWidth: !AppTheme.gnome
                Layout.preferredWidth: AppTheme.gnome ? Kirigami.Units.gridUnit * 24 : -1
                currentIndex: root.transitView ? 3 : root.mensaView ? 2 : root.dayView ? 1 : 0
                model: [
                    { text: qsTr("Woche"), icon: "view-calendar-week" },
                    { text: qsTr("Tag"), icon: "view-calendar-day" },
                    { text: qsTr("Mensa"), icon: "food" },
                    // No bus in Breeze's or Adwaita's icon sets, so it's bundled; the GNOME
                    // toggle group is text-only anyway.
                    { text: qsTr("Abfahrten"), icon: AppTheme.gnome ? "" : Qt.resolvedUrl("../icons/bus-symbolic.svg") }
                ]
                onActivated: (index) => root.selectView(index)
            }

            Controls.ToolButton {
                icon.name: AppTheme.icon("go-next")
                visible: !root.transitView
                enabled: root.viewReady
                onClicked: root.mensaView ? mensaController.stepDay(1)
                                          : root.dayView ? root.stepDay(1) : root.stepWeek(1)
            }

            Item {
                visible: AppTheme.gnome
                Layout.fillWidth: true
            }
        }

        // A fixed-size Item (not a Loader whose own visibility toggles) keeps the header
        // above stable during a refetch — only a small overlay spinner appears/disappears,
        // instead of the whole grid (and the toolbar above it) jumping around.
        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true

            Kirigami.PlaceholderMessage {
                anchors.centerIn: parent
                width: parent.width - Kirigami.Units.gridUnit * 4
                visible: !root.sideView && !timetableController.selectedStudiengang.code
                icon.name: AppTheme.gnome ? "x-office-calendar-symbolic" : "view-calendar-week"
                text: qsTr("Kein Studiengang ausgewählt")
                explanation: qsTr("Wähle in den Einstellungen einen Studiengang aus, um deinen Stundenplan zu sehen.")
                helpfulAction: Kirigami.Action {
                    text: qsTr("Studiengang wählen")
                    onTriggered: applicationWindow().pageStack.push(settingsHubComponent)
                }
            }

            Loader {
                anchors.fill: parent
                active: root.sideView || !!timetableController.selectedStudiengang.code
                sourceComponent: root.transitView ? transitViewComponent
                               : root.mensaView ? mensaViewComponent
                               : root.dayView ? dayViewComponent : weekViewComponent
            }

            Rectangle {
                anchors.fill: parent
                visible: timetableController.loading && !root.sideView
                color: Qt.rgba(AppTheme.backgroundColor.r, AppTheme.backgroundColor.g, AppTheme.backgroundColor.b, 0.6)

                Controls.BusyIndicator {
                    anchors.centerIn: parent
                    running: parent.visible
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

    Component {
        id: mensaViewComponent
        MensaView {
            onMealClicked: (meal) => {
                mealSheet.meal = meal
                mealSheet.open()
            }
        }
    }

    EventDetailSheet {
        id: detailSheet
    }

    MealDetailSheet {
        id: mealSheet
    }

    Component {
        id: transitViewComponent
        TransitView {
            onSearchRequested: stopSearch.open()
            onNoticesRequested: (notices) => {
                noticesSheet.notices = notices
                noticesSheet.open()
            }
        }
    }

    StopSearchSheet {
        id: stopSearch
    }

    // Disruption notices of one departure (diversions, broken lifts, ...).
    AppSheet {
        id: noticesSheet
        property var notices: []
        title: qsTr("Hinweise")

        ColumnLayout {
            Layout.preferredWidth: Kirigami.Units.gridUnit * 24
            spacing: Kirigami.Units.largeSpacing
            Repeater {
                model: noticesSheet.notices
                delegate: ColumnLayout {
                    required property var modelData
                    Layout.fillWidth: true
                    spacing: 2
                    Controls.Label {
                        Layout.fillWidth: true
                        text: modelData.title
                        font.bold: true
                        wrapMode: Text.Wrap
                    }
                    Controls.Label {
                        Layout.fillWidth: true
                        visible: !!modelData.text && modelData.text !== modelData.title
                        text: modelData.text
                        wrapMode: Text.Wrap
                    }
                }
            }
        }
    }

    SearchDialog {
        id: searchSheet
        onEventSelected: (eventData) => {
            detailSheet.eventData = eventData
            detailSheet.open()
        }
    }
}
