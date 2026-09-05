import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.OverlaySheet {
    id: root

    property var eventData: ({})

    header: Kirigami.Heading {
        text: root.eventData.title || ""
        level: 2
        wrapMode: Text.Wrap
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 20
        spacing: Kirigami.Units.smallSpacing

        Kirigami.FormLayout {
            Controls.Label {
                Kirigami.FormData.label: qsTr("Tag:")
                text: root.eventData.dayLabel || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Zeit:")
                text: (root.eventData.startLabel || "") + " – " + (root.eventData.endLabel || "")
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Turnus:")
                visible: !!root.eventData.frequency
                text: root.eventData.frequency || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Raum:")
                visible: !!root.eventData.room
                text: root.eventData.room || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Art:")
                visible: !!root.eventData.category
                text: root.eventData.category || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Dozent:")
                visible: !!root.eventData.lecturer
                text: root.eventData.lecturer || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Start:")
                visible: !!root.eventData.startDate
                text: root.eventData.startDate || ""
            }
            Controls.Label {
                Kirigami.FormData.label: qsTr("Ende:")
                visible: !!root.eventData.endDate
                text: root.eventData.endDate || ""
            }
        }

        Kirigami.Separator { Layout.fillWidth: true }

        RowLayout {
            Layout.fillWidth: true
            Controls.Switch {
                id: hideSwitch
                checked: root.eventData.groupKey ? timetableController.isGroupHidden(root.eventData.groupKey) : false
                onToggled: timetableController.hideGroup(root.eventData.groupKey, checked)
            }
            Controls.Label {
                Layout.fillWidth: true
                text: qsTr("Diese Gruppe immer ausblenden")
                wrapMode: Text.Wrap
            }
        }

        Controls.Button {
            Layout.fillWidth: true
            icon.name: "edit-copy"
            text: qsTr("Teilen (in Zwischenablage kopieren)")
            onClicked: {
                clipboardHelper.text = timetableController.shareTextFor(root.eventData)
                clipboardHelper.selectAll()
                clipboardHelper.copy()
            }
        }
        Controls.TextField {
            id: clipboardHelper
            visible: false
        }
    }
}
