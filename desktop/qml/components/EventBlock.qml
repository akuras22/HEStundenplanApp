import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

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
    // The raw palette color is too dim for text on a dark window and too pale on a light one.
    readonly property color labelColor: AppTheme.dark ? Qt.lighter(eventColor, 1.45) : Qt.darker(eventColor, 1.35)

    function hashCode(str) {
        var hash = 0
        for (var i = 0; i < str.length; i++) {
            hash = ((hash << 5) - hash) + str.charCodeAt(i)
            hash |= 0
        }
        return hash
    }

    radius: AppTheme.controlRadius
    // An opaque tint (not a translucent one), so the hour gridlines don't show through the text.
    color: Qt.tint(AppTheme.backgroundColor, Qt.rgba(eventColor.r, eventColor.g, eventColor.b, hover.hovered ? 0.3 : 0.2))
    border.color: Qt.rgba(eventColor.r, eventColor.g, eventColor.b, 0.7)
    border.width: 1
    clip: true

    HoverHandler {
        id: hover
        cursorShape: Qt.PointingHandCursor
    }
    MouseArea {
        anchors.fill: parent
        onClicked: root.clicked()
    }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: Kirigami.Units.smallSpacing
        anchors.leftMargin: Kirigami.Units.smallSpacing + 2
        spacing: 0

        Controls.Label {
            Layout.fillWidth: true
            text: eventData.title || ""
            color: root.labelColor
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
            color: root.labelColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: settingsStore.blockShowRoom && !!eventData.room
            text: eventData.room || ""
            color: root.labelColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
        Controls.Label {
            Layout.fillWidth: true
            visible: settingsStore.blockShowLecturer && !!eventData.lecturer
            text: eventData.lecturer || ""
            color: root.labelColor
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize * 0.9
            elide: Text.ElideRight
        }
    }
}
