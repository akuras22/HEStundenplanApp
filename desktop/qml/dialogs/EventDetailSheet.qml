import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

AppSheet {
    id: root

    property var eventData: ({})

    title: root.eventData.title || ""

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

        // "Wo ist das?" — campus, building and floor of the room, on a small map.
        ColumnLayout {
            id: locationSection
            readonly property var location: timetableController.roomLocation(root.eventData.room || "")
            Layout.fillWidth: true
            visible: !!location.buildingName
            spacing: Kirigami.Units.smallSpacing

            Controls.Label {
                Layout.fillWidth: true
                text: qsTr("Wo ist das?")
                font.bold: true
            }
            Controls.Label {
                Layout.fillWidth: true
                text: [locationSection.location.buildingName, locationSection.location.floorLabel].filter(part => !!part).join(" · ")
                wrapMode: Text.Wrap
            }
            Controls.Label {
                Layout.fillWidth: true
                text: (locationSection.location.campusName || "") + " · " + (locationSection.location.address || "")
                      + (locationSection.location.approximate ? qsTr(" (ungefähre Lage)") : "")
                color: AppTheme.disabledTextColor
                font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                wrapMode: Text.Wrap
            }
            MiniMap {
                Layout.fillWidth: true
                // Only loads tiles once there's actually a location to show.
                visible: locationSection.visible
                latitude: locationSection.location.latitude || 0
                longitude: locationSection.location.longitude || 0
                mapUrl: locationSection.location.mapUrl || ""
            }
            Controls.Button {
                icon.name: AppTheme.gnome ? "mark-location-symbolic" : "mark-location"
                text: qsTr("In Karte öffnen")
                onClicked: Qt.openUrlExternally(locationSection.location.mapUrl)
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
            icon.name: AppTheme.icon("edit-copy")
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
