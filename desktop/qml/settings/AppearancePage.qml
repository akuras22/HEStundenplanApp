import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

PrefPage {
    id: root
    title: qsTr("Darstellung")

    actions: [
        Kirigami.Action {
            text: qsTr("Auf Standard zurücksetzen")
            icon.name: AppTheme.icon("edit-undo")
            onTriggered: settingsStore.resetAppearance()
        }
    ]

    // Theme and accent-color pickers used to live here, but the window chrome always follows
    // the desktop's own color scheme and accent no matter what an app requests — both settings
    // just produced a confusing half-themed look, so the app follows the system theme/accent
    // unconditionally now instead of offering a choice that only ever partially applied.

    PrefGroup {
        PrefRow {
            text: qsTr("Standardansicht")
            subtitle: qsTr("Ansicht beim Start der App")

            SegmentedSwitch {
                Layout.fillWidth: false
                Layout.preferredWidth: Kirigami.Units.gridUnit * 11
                currentIndex: settingsStore.defaultViewIsDay ? 1 : 0
                model: [
                    { text: qsTr("Woche") },
                    { text: qsTr("Tag") }
                ]
                onActivated: (index) => settingsStore.defaultViewIsDay = (index === 1)
            }
        }
    }

    PrefGroup {
        title: qsTr("Blockinhalte")

        PrefRow {
            text: qsTr("Uhrzeit anzeigen")
            activatable: true
            onClicked: settingsStore.blockShowTime = !settingsStore.blockShowTime
            Controls.Switch {
                checked: settingsStore.blockShowTime
                onToggled: settingsStore.blockShowTime = checked
            }
        }
        PrefRow {
            text: qsTr("Raum anzeigen")
            activatable: true
            onClicked: settingsStore.blockShowRoom = !settingsStore.blockShowRoom
            Controls.Switch {
                checked: settingsStore.blockShowRoom
                onToggled: settingsStore.blockShowRoom = checked
            }
        }
        PrefRow {
            text: qsTr("Dozent anzeigen")
            activatable: true
            onClicked: settingsStore.blockShowLecturer = !settingsStore.blockShowLecturer
            Controls.Switch {
                checked: settingsStore.blockShowLecturer
                onToggled: settingsStore.blockShowLecturer = checked
            }
        }
    }

    QtObject {
        id: hiddenGroupsHolder
        property var list: timetableController.hiddenGroups()
        property var occurrences: timetableController.hiddenOccurrences()
    }
    Connections {
        target: timetableController
        function onWeekEventsChanged() {
            hiddenGroupsHolder.list = timetableController.hiddenGroups()
            hiddenGroupsHolder.occurrences = timetableController.hiddenOccurrences()
        }
    }

    PrefGroup {
        title: qsTr("Ausgeblendete Gruppen")
        visible: hiddenRepeater.count > 0

        Repeater {
            id: hiddenRepeater
            model: hiddenGroupsHolder.list
            delegate: PrefRow {
                required property var modelData
                required property int index
                first: index === 0
                last: index === hiddenRepeater.count - 1
                text: modelData.title

                Controls.Button {
                    text: qsTr("Einblenden")
                    onClicked: timetableController.hideGroup(modelData.groupKey, false)
                }
            }
        }
    }

    // Single lectures hidden on specific dates, one group per weekly slot.
    PrefGroup {
        title: qsTr("Ausgeblendete Einzeltermine")
        visible: occurrenceRepeater.count > 0

        Repeater {
            id: occurrenceRepeater
            model: hiddenGroupsHolder.occurrences
            delegate: ColumnLayout {
                id: slotDelegate
                required property var modelData
                Layout.fillWidth: true
                spacing: 0

                PrefRow {
                    first: true
                    last: slotDelegate.modelData.dates.length === 0
                    text: slotDelegate.modelData.title
                    subtitle: slotDelegate.modelData.subtitle

                    Controls.Button {
                        visible: slotDelegate.modelData.dates.length > 1
                        text: qsTr("Alle einblenden")
                        onClicked: timetableController.updateHiddenOccurrences([], slotDelegate.modelData.keys)
                    }
                }
                Repeater {
                    model: slotDelegate.modelData.dates
                    delegate: PrefRow {
                        required property var modelData
                        required property int index
                        last: index === slotDelegate.modelData.dates.length - 1
                        text: modelData.label

                        Controls.Button {
                            text: qsTr("Einblenden")
                            onClicked: timetableController.updateHiddenOccurrences([], [modelData.key])
                        }
                    }
                }
            }
        }
    }
}
