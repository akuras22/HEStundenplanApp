import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

PrefPage {
    id: root
    title: qsTr("Benachrichtigungen")

    readonly property var leadPresets: [5, 10, 15, 20, 30, 45, 60]
    readonly property var customLeadMinutes: settingsStore.reminderLeadMinutes.filter(
        (m) => root.leadPresets.indexOf(m) < 0
    )

    function addCustomLead(minutes) {
        if (minutes <= 0)
            return
        var current = new Set(settingsStore.reminderLeadMinutes)
        current.add(minutes)
        settingsStore.reminderLeadMinutes = Array.from(current)
    }

    function removeLead(minutes) {
        var current = new Set(settingsStore.reminderLeadMinutes)
        current.delete(minutes)
        settingsStore.reminderLeadMinutes = Array.from(current)
    }

    PrefGroup {
        PrefRow {
            text: qsTr("Erinnerungen aktivieren")
            subtitle: qsTr("Vor Beginn einer Veranstaltung benachrichtigen")
            activatable: true
            onClicked: settingsStore.remindersEnabled = !settingsStore.remindersEnabled

            Controls.Switch {
                checked: settingsStore.remindersEnabled
                onToggled: settingsStore.remindersEnabled = checked
            }
        }
    }

    PrefGroup {
        title: qsTr("Vorlauf")
        visible: settingsStore.remindersEnabled

        Flow {
            Layout.fillWidth: true
            Layout.margins: Kirigami.Units.largeSpacing
            spacing: Kirigami.Units.smallSpacing

            Repeater {
                model: root.leadPresets
                delegate: Controls.CheckBox {
                    required property int modelData
                    text: qsTr("%1 Min.").arg(modelData)
                    checked: settingsStore.reminderLeadMinutes.indexOf(modelData) >= 0
                    onToggled: {
                        var current = new Set(settingsStore.reminderLeadMinutes)
                        if (checked)
                            current.add(modelData)
                        else
                            current.delete(modelData)
                        settingsStore.reminderLeadMinutes = Array.from(current)
                    }
                }
            }
        }

        Repeater {
            model: root.customLeadMinutes
            delegate: PrefRow {
                required property int modelData
                first: false
                last: false
                text: qsTr("%1 Min.").arg(modelData)

                Controls.ToolButton {
                    icon.name: AppTheme.gnome ? "user-trash-symbolic" : "edit-delete"
                    onClicked: root.removeLead(modelData)
                    Controls.ToolTip.text: qsTr("Entfernen")
                    Controls.ToolTip.visible: hovered
                }
            }
        }

        PrefRow {
            first: false
            last: true
            text: qsTr("Eigener Vorlauf (Minuten)")

            Controls.SpinBox {
                id: customMinutesSpinBox
                from: 1
                to: 180
                value: 25
                editable: true
            }
            Controls.Button {
                text: qsTr("Hinzufügen")
                onClicked: root.addCustomLead(customMinutesSpinBox.value)
            }
        }
    }

    PrefGroup {
        title: qsTr("Bei geschlossenem Fenster")
        visible: settingsStore.remindersEnabled

        PrefRow {
            first: true
            text: qsTr("Im Hintergrund weiterlaufen")
            subtitle: qsTr("Erinnerungen kommen auch, wenn das Fenster zu ist. Wieder öffnen über das Anwendungsmenü, ganz beenden mit Strg+Q oder hier unten.")
            activatable: true
            onClicked: settingsStore.runInBackground = !settingsStore.runInBackground

            Controls.Switch {
                checked: settingsStore.runInBackground
                onToggled: settingsStore.runInBackground = checked
            }
        }
        PrefRow {
            last: !settingsStore.runInBackground
            enabled: settingsStore.runInBackground
            text: qsTr("Beim Anmelden starten")
            subtitle: qsTr("Startet unsichtbar im Hintergrund, damit Erinnerungen ab dem Login kommen")
            activatable: true
            onClicked: settingsStore.autostart = !settingsStore.autostart

            Controls.Switch {
                checked: settingsStore.autostart && settingsStore.runInBackground
                onToggled: settingsStore.autostart = checked
            }
        }
        PrefRow {
            last: true
            visible: settingsStore.runInBackground
            text: qsTr("App ganz beenden")
            subtitle: qsTr("Bis zum nächsten Start keine Erinnerungen")

            Controls.Button {
                text: qsTr("Beenden")
                onClicked: backgroundService.quit()
            }
        }
    }

    PrefGroup {
        visible: settingsStore.remindersEnabled

        PrefRow {
            text: qsTr("Test-Benachrichtigung senden")
            icon.name: AppTheme.icon("notifications")
            activatable: true
            onClicked: notificationManager.notifyTest()
        }
    }
}
