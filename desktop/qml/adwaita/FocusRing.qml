import QtQuick

// libadwaita's keyboard-focus outline: 2px of half-transparent accent just outside the widget.
Rectangle {
    anchors.fill: parent
    anchors.margins: -2
    radius: (parent.radius ?? 0) + 2
    color: "transparent"
    border.width: 2
    border.color: Qt.alpha(Adw.accentBg, 0.5)
}
