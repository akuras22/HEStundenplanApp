import QtQuick
import QtQuick.Effects
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A few OpenStreetMap tiles stitched together around a spot, with a marker in the middle. Not an
// interactive map — a click opens openstreetmap.org. Same idea as the Android app's MiniMap.
Item {
    id: root

    property real latitude
    property real longitude
    property string mapUrl
    // Tiles drawn at their native 256 px: zoom 17 shows the building with its neighbours, about
    // the same area the Android app shows at zoom 18 with its smaller tiles.
    readonly property int zoom: 17
    readonly property int tileSize: 256

    // Slippy-map tile coordinates (Web Mercator), fractional: the integer part is the tile, the
    // rest the position inside it.
    readonly property real tileX: (longitude + 180) / 360 * Math.pow(2, zoom)
    readonly property real tileY: (1 - Math.asinh(Math.tan(latitude * Math.PI / 180)) / Math.PI) / 2 * Math.pow(2, zoom)

    implicitHeight: 170
    clip: true

    Item {
        id: tiles
        anchors.fill: parent

        Repeater {
            // 5 x 3 tiles around the spot always cover the box (it is at most ~2 tiles wide).
            model: 15
            delegate: Image {
                required property int index
                readonly property int tx: Math.floor(root.tileX) + (index % 5) - 2
                readonly property int ty: Math.floor(root.tileY) + Math.floor(index / 5) - 1
                x: root.width / 2 + (tx - root.tileX) * root.tileSize
                y: root.height / 2 + (ty - root.tileY) * root.tileSize
                width: root.tileSize
                height: root.tileSize
                visible: x < root.width && y < root.height && x + width > 0 && y + height > 0
                source: visible ? "https://tile.openstreetmap.org/%1/%2/%3.png".arg(root.zoom).arg(tx).arg(ty) : ""
                asynchronous: true
            }
        }

        // The software renderer can't do layer effects; square corners are the fallback there.
        layer.enabled: GraphicsInfo.api !== GraphicsInfo.Software
        layer.effect: MultiEffect {
            maskEnabled: true
            maskSource: mask
            maskThresholdMin: 0.5
            maskSpreadAtMin: 1.0
        }
    }

    Rectangle {
        id: mask
        anchors.fill: parent
        radius: AppTheme.cardRadius
        visible: false
        layer.enabled: true
    }

    // Shows while the tiles load (and if they can't).
    Rectangle {
        anchors.fill: parent
        z: -1
        radius: AppTheme.cardRadius
        color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.07)
    }

    // The marker: a red dot with a white ring, centred on the spot.
    Rectangle {
        anchors.centerIn: parent
        width: 20
        height: 20
        radius: 10
        color: "#E0473D"
        border.color: "white"
        border.width: 3
    }

    Controls.Label {
        anchors.right: parent.right
        anchors.bottom: parent.bottom
        anchors.margins: 1
        text: qsTr("© OpenStreetMap-Mitwirkende")
        font.pixelSize: 10
        color: "black"
        leftPadding: 4
        rightPadding: 4
        background: Rectangle {
            color: Qt.rgba(1, 1, 1, 0.7)
            radius: 3
        }
    }

    HoverHandler {
        cursorShape: Qt.PointingHandCursor
    }
    MouseArea {
        anchors.fill: parent
        onClicked: Qt.openUrlExternally(root.mapUrl)
    }
}
