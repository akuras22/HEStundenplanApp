import QtQuick
import QtQuick.Layouts
import QtQuick.Templates as T
import org.kde.kirigami.primitives as Primitives

// Icon + label of a button-like control, honoring its display mode. "-symbolic" icons are tinted
// with the text color like GTK does; anything else (e.g. an app icon) keeps its own colors.
Item {
    id: root

    required property T.AbstractButton control
    property color color: Adw.fg
    property int alignment: Qt.AlignHCenter

    readonly property string iconName: control.icon.name
    readonly property bool hasIcon: control.display !== T.AbstractButton.TextOnly
                                    && (control.icon.name !== "" || control.icon.source.toString() !== "")
    readonly property bool hasText: control.text !== "" && (control.display !== T.AbstractButton.IconOnly || !hasIcon)

    implicitWidth: row.implicitWidth
    implicitHeight: row.implicitHeight

    RowLayout {
        id: row
        x: root.alignment & Qt.AlignLeft ? 0 : Math.round((root.width - width) / 2)
        width: Math.min(implicitWidth, root.width)
        height: root.height
        spacing: root.control.spacing
        // A mirrored control puts its icon after the label (e.g. a drop-down arrow).
        layoutDirection: root.control.mirrored ? Qt.RightToLeft : Qt.LeftToRight

        Primitives.Icon {
            visible: root.hasIcon
            Layout.alignment: Qt.AlignVCenter
            Layout.preferredWidth: root.control.icon.width
            Layout.preferredHeight: root.control.icon.height
            source: root.iconName !== "" ? root.iconName : root.control.icon.source
            isMask: root.iconName.endsWith("-symbolic") || root.control.icon.color.a > 0
            color: root.control.icon.color.a > 0 ? root.control.icon.color : root.color
        }
        Text {
            visible: root.hasText
            Layout.fillWidth: true
            Layout.alignment: Qt.AlignVCenter
            text: root.control.text
            font: root.control.font
            color: root.color
            elide: Text.ElideRight
            horizontalAlignment: root.alignment & Qt.AlignLeft ? Text.AlignLeft : Text.AlignHCenter
        }
    }
}
