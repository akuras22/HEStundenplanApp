import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import QtQuick.Dialogs as Dialogs
import org.kde.kirigami as Kirigami

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Darstellung")

    readonly property var accentColors: ["", "#2E9E5B", "#8B5CF6", "#E0762F", "#E0473D", "#E0508F", ""]
    readonly property var accentNames: [qsTr("Standard"), qsTr("Grün"), qsTr("Lila"), qsTr("Orange"), qsTr("Rot"), qsTr("Pink"), qsTr("Benutzerdefiniert")]

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

        Kirigami.Heading {
            level: 4
            text: qsTr("Theme")
        }
        RowLayout {
            Controls.RadioButton {
                text: qsTr("System")
                checked: settingsStore.themeMode === 0
                onToggled: settingsStore.themeMode = 0
            }
            Controls.RadioButton {
                text: qsTr("Hell")
                checked: settingsStore.themeMode === 1
                onToggled: settingsStore.themeMode = 1
            }
            Controls.RadioButton {
                text: qsTr("Dunkel")
                checked: settingsStore.themeMode === 2
                onToggled: settingsStore.themeMode = 2
            }
        }

        Kirigami.Heading {
            level: 4
            text: qsTr("Akzentfarbe")
        }
        Flow {
            Layout.fillWidth: true
            spacing: Kirigami.Units.smallSpacing
            Repeater {
                model: root.accentNames.length
                delegate: Controls.RoundButton {
                    required property int index
                    readonly property bool isCustom: index === 6
                    text: isCustom ? "" : ""
                    checkable: true
                    checked: settingsStore.accentPreset === index
                    icon.name: checked ? "checkmark" : ""
                    background: Rectangle {
                        radius: width / 2
                        color: isCustom
                               ? (settingsStore.customAccentColor.a > 0 ? settingsStore.customAccentColor : Kirigami.Theme.disabledTextColor)
                               : (index === 0 ? Kirigami.Theme.highlightColor : root.accentColors[index])
                        border.width: parent.checked ? 2 : 0
                        border.color: Kirigami.Theme.textColor
                    }
                    onClicked: {
                        if (isCustom)
                            accentColorDialog.open()
                        else
                            settingsStore.accentPreset = index
                    }
                    Controls.ToolTip.text: root.accentNames[index]
                    Controls.ToolTip.visible: hovered
                }
            }
        }

        Controls.Button {
            text: qsTr("Eigene Hintergrundfarbe…")
            onClicked: backgroundColorDialog.open()
        }

        Dialogs.ColorDialog {
            id: accentColorDialog
            onAccepted: {
                settingsStore.customAccentColor = selectedColor
                settingsStore.accentPreset = 6
            }
        }
        Dialogs.ColorDialog {
            id: backgroundColorDialog
            onAccepted: settingsStore.customBackgroundColor = selectedColor
        }

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
