import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

// One lecture/tutorial block on the grid — color hashed by title, like the Android app's fixed
// 8-color event palette.
Rectangle {
    id: root

    property var eventData: ({})
    signal clicked()

    readonly property var eventPalette: [
        "#0381FE", "#2E9E5B", "#8B5CF6", "#E0762F",
        "#E0473D", "#E0508F", "#3FA7A0", "#B08900",
    ]
    readonly property color eventColor: eventPalette[Math.abs(hashCode(eventData.title || "")) % eventPalette.length]

    function hashCode(str) {
        var hash = 0
        for (var i = 0; i < str.length; i++) {
            hash = ((hash << 5) - hash) + str.charCodeAt(i)
            hash |= 0
        }
        return hash
    }

    radius: Kirigami.Units.cornerRadius
    color: Qt.rgba(eventColor.r, eventColor.g, eventColor.b, 0.18)
    border.color: eventColor
    border.width: 1
    clip: true

    MouseArea {
        anchors.fill: parent
        onClicked: root.clicked()
    }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: Kirigami.Units.smallSpacing
        spacing: 0

        Controls.Label {
            Layout.fillWidth: true
            text: eventData.title || ""
            color: eventColor
            font.bold: true
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize
            elide: Text.ElideRight
            maximumLineCount: 2
            wrapMode: Text.Wrap
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: settingsStore.blockShowTime
            text: (eventData.startLabel || "") + "–" + (eventData.endLabel || "")
            color: eventColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: settingsStore.blockShowRoom && !!eventData.room
            text: eventData.room || ""
            color: eventColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: settingsStore.blockShowLecturer && !!eventData.lecturer
            text: eventData.lecturer || ""
            color: eventColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
    }
}
