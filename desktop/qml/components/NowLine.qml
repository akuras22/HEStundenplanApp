import QtQuick

// Red "current time" indicator line drawn across the grid, like the original Canvas-drawn line.
Item {
    id: root
    property real y0: 0

    y: y0
    height: 2
    z: 10

    Rectangle {
        anchors.left: parent.left
        anchors.right: parent.right
        height: 2
        color: "#E0473D"
    }
    Rectangle {
        width: 8
        height: 8
        radius: 4
        color: "#E0473D"
        anchors.verticalCenter: parent.verticalCenter
        anchors.left: parent.left
        anchors.leftMargin: -4
    }
}
