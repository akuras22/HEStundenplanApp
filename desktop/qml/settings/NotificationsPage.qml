import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Benachrichtigungen")

    readonly property var leadPresets: [5, 10, 15, 20, 30, 45, 60]
    readonly property var customLeadMinutes: settingsStore.reminderLeadMinutes().filter(
        (m) => root.leadPresets.indexOf(m) < 0
    )

    function addCustomLead(minutes) {
        if (minutes <= 0)
            return
        var current = new Set(settingsStore.reminderLeadMinutes())
        current.add(minutes)
        settingsStore.setReminderLeadMinutes(Array.from(current))
    }

    function removeLead(minutes) {
        var current = new Set(settingsStore.reminderLeadMinutes())
        current.delete(minutes)
        settingsStore.setReminderLeadMinutes(Array.from(current))
    }

    ColumnLayout {
        width: root.width
        spacing: Kirigami.Units.largeSpacing

        Kirigami.FormLayout {
            Layout.fillWidth: true

            Controls.Switch {
                Kirigami.FormData.label: qsTr("Erinnerungen aktivieren")
                checked: settingsStore.remindersEnabled
                onToggled: settingsStore.remindersEnabled = checked
            }
        }

        Kirigami.Heading {
            level: 4
            text: qsTr("Vorlauf")
            visible: settingsStore.remindersEnabled
        }

        Flow {
            Layout.fillWidth: true
            visible: settingsStore.remindersEnabled
            spacing: Kirigami.Units.smallSpacing

            Repeater {
                model: root.leadPresets
                delegate: Controls.CheckBox {
                    required property int modelData
                    text: qsTr("%1 Min.").arg(modelData)
                    checked: settingsStore.reminderLeadMinutes().indexOf(modelData) >= 0
                    onToggled: {
                        var current = new Set(settingsStore.reminderLeadMinutes())
                        if (checked)
                            current.add(modelData)
                        else
                            current.delete(modelData)
                        settingsStore.setReminderLeadMinutes(Array.from(current))
                    }
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            visible: settingsStore.remindersEnabled
            spacing: Kirigami.Units.smallSpacing

            Repeater {
                model: root.customLeadMinutes
                delegate: Kirigami.Chip {
                    required property int modelData
                    text: qsTr("%1 Min.").arg(modelData)
                    closable: true
                    onRemoved: root.removeLead(modelData)
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            visible: settingsStore.remindersEnabled
            spacing: Kirigami.Units.smallSpacing

            Controls.SpinBox {
                id: customMinutesSpinBox
                from: 1
                to: 180
                value: 25
                editable: true
            }
            Controls.Button {
                text: qsTr("Eigenen Vorlauf hinzufügen")
                icon.name: "list-add"
                onClicked: root.addCustomLead(customMinutesSpinBox.value)
            }
        }

        Controls.Button {
            visible: settingsStore.remindersEnabled
            text: qsTr("Test-Benachrichtigung senden")
            icon.name: "notifications"
            onClicked: notificationManager.notifyTest()
        }
    }
}
