import QtQuick
import QtQuick.Effects
import de.hsesslingen.stundenplan.platform
import de.hsesslingen.stundenplan.adwaita as Adwaita

// GNOME look only: everything a window needs once it has no window-manager frame (see
// HeaderBar.qml, which supplies the title bar part) — rounded corners, a hairline border and
// resize grips along the edges. Works on the window's root item so that page content, the header
// bar and popups/dim overlays are all clipped to the same rounded shape.
Item {
    id: root

    required property Window window

    readonly property bool active: DesktopStyle.clientSideDecorations
    // Maximized/fullscreen windows are plain rectangles without a border or resize edges.
    readonly property bool floating: active && window.visibility === Window.Windowed
    readonly property Item rootItem: window.contentItem.parent

    parent: rootItem
    anchors.fill: parent
    z: 1000
    visible: active

    function syncLayer() {
        // The software renderer cannot do layer effects; square corners are the fallback there.
        const canMask = GraphicsInfo.api !== GraphicsInfo.Software
        rootItem.layer.enabled = root.floating && canMask
        rootItem.layer.effect = root.floating && canMask ? maskEffect : null
    }
    onFloatingChanged: syncLayer()
    GraphicsInfo.onApiChanged: syncLayer()
    Component.onCompleted: syncLayer()

    Component {
        id: maskEffect
        MultiEffect {
            maskEnabled: true
            maskSource: mask
            maskThresholdMin: 0.5
            maskSpreadAtMin: 1.0
        }
    }
    Rectangle {
        id: mask
        anchors.fill: parent
        radius: Adwaita.Adw.windowRadius
        visible: false
        layer.enabled: true
    }

    Rectangle {
        anchors.fill: parent
        visible: root.floating
        radius: Adwaita.Adw.windowRadius
        color: "transparent"
        border.width: 1
        border.color: Adwaita.Adw.windowBorder
    }

    component Grip: MouseArea {
        required property int edges
        visible: root.floating
        acceptedButtons: Qt.LeftButton
        cursorShape: edges === Qt.TopEdge || edges === Qt.BottomEdge ? Qt.SizeVerCursor
                   : edges === Qt.LeftEdge || edges === Qt.RightEdge ? Qt.SizeHorCursor
                   : edges === (Qt.TopEdge | Qt.LeftEdge) || edges === (Qt.BottomEdge | Qt.RightEdge)
                     ? Qt.SizeFDiagCursor : Qt.SizeBDiagCursor
        onPressed: root.window.startSystemResize(edges)
    }

    readonly property int gripSize: 5
    readonly property int cornerSize: 14

    Grip { edges: Qt.TopEdge; height: root.gripSize; anchors { top: parent.top; left: parent.left; right: parent.right } }
    Grip { edges: Qt.BottomEdge; height: root.gripSize; anchors { bottom: parent.bottom; left: parent.left; right: parent.right } }
    Grip { edges: Qt.LeftEdge; width: root.gripSize; anchors { left: parent.left; top: parent.top; bottom: parent.bottom } }
    Grip { edges: Qt.RightEdge; width: root.gripSize; anchors { right: parent.right; top: parent.top; bottom: parent.bottom } }
    Grip { edges: Qt.TopEdge | Qt.LeftEdge; width: root.cornerSize; height: root.cornerSize; anchors { top: parent.top; left: parent.left } }
    Grip { edges: Qt.TopEdge | Qt.RightEdge; width: root.cornerSize; height: root.cornerSize; anchors { top: parent.top; right: parent.right } }
    Grip { edges: Qt.BottomEdge | Qt.LeftEdge; width: root.cornerSize; height: root.cornerSize; anchors { bottom: parent.bottom; left: parent.left } }
    Grip { edges: Qt.BottomEdge | Qt.RightEdge; width: root.cornerSize; height: root.cornerSize; anchors { bottom: parent.bottom; right: parent.right } }
}
