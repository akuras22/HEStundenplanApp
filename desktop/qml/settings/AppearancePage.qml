import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Darstellung")

    actions: [
        Kirigami.Action {
            text: qsTr("Auf Standard zurücksetzen")
            icon.name: "edit-undo"
            onTriggered: settingsStore.resetAppearance()
        }
    ]

    ColumnLayout {
        width: root.width
        spacing: Kirigami.Units.largeSpacing

        Kirigami.Heading {
            level: 4
            text: qsTr("Standardansicht")
        }
        RowLayout {
            Controls.RadioButton {
                text: qsTr("Woche")
                checked: !settingsStore.defaultViewIsDay
                onToggled: settingsStore.defaultViewIsDay = false
            }
            Controls.RadioButton {
                text: qsTr("Tag")
                checked: settingsStore.defaultViewIsDay
                onToggled: settingsStore.defaultViewIsDay = true
            }
        }

        // Theme and accent-color pickers used to live here, but Kirigami's own chrome always
        // follows the system Plasma color scheme and accent no matter what an app requests —
        // both settings just produced a confusing half-themed look, so the app follows the
        // system theme/accent unconditionally now instead of offering a choice that only ever
        // partially applied.

        Kirigami.Heading {
            level: 4
            text: qsTr("Blockinhalte")
        }
        Controls.CheckBox {
            text: qsTr("Uhrzeit anzeigen")
            checked: settingsStore.blockShowTime
            onToggled: settingsStore.blockShowTime = checked
        }
        Controls.CheckBox {
            text: qsTr("Raum anzeigen")
            checked: settingsStore.blockShowRoom
            onToggled: settingsStore.blockShowRoom = checked
        }
        Controls.CheckBox {
            text: qsTr("Dozent anzeigen")
            checked: settingsStore.blockShowLecturer
            onToggled: settingsStore.blockShowLecturer = checked
        }

        Kirigami.Heading {
            level: 4
            visible: hiddenRepeater.count > 0
            text: qsTr("Ausgeblendete Gruppen")
        }
        QtObject {
            id: hiddenGroupsHolder
            property var list: timetableController.hiddenGroups()
        }
        Connections {
            target: timetableController
            function onWeekEventsChanged() { hiddenGroupsHolder.list = timetableController.hiddenGroups() }
        }

        Repeater {
            id: hiddenRepeater
            model: hiddenGroupsHolder.list
            delegate: RowLayout {
                required property var modelData
                Layout.fillWidth: true
                Controls.Label {
                    Layout.fillWidth: true
                    text: modelData.title
                    elide: Text.ElideRight
                }
                Controls.Button {
                    text: qsTr("Einblenden")
                    onClicked: timetableController.hideGroup(modelData.groupKey, false)
                }
            }
        }
    }
}
