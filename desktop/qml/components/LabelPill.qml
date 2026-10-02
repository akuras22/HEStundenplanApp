import QtQuick
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A meal marker ("vegan", "Schwein", ...) as a small tinted pill — green for the vegetarian/vegan
// ones, neutral for the rest. Same idea as the Android app's LabelPill.
Rectangle {
    id: root

    property var label: ({})

    readonly property color tint: root.label.veggie ? (AppTheme.dark ? "#78e9ab" : "#2E9E5B") : AppTheme.textColor

    implicitWidth: text.implicitWidth + 16
    implicitHeight: text.implicitHeight + 6
    radius: height / 2
    color: Qt.rgba(tint.r, tint.g, tint.b, 0.14)

    Controls.Label {
        id: text
        anchors.centerIn: parent
        text: root.label.title || ""
        color: root.tint
        font.bold: true
        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
    }
}
