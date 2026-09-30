import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A titled card holding PrefRows (or any other items) — a "boxed list" in GNOME terms.
ColumnLayout {
    id: root

    property string title
    default property alias content: cardColumn.data

    Layout.fillWidth: true
    spacing: AppTheme.gnome ? 10 : Kirigami.Units.smallSpacing

    Controls.Label {
        Layout.fillWidth: true
        Layout.leftMargin: AppTheme.gnome ? 0 : Kirigami.Units.largeSpacing
        visible: root.title !== ""
        text: root.title
        font.bold: true
        elide: Text.ElideRight
    }

    Rectangle {
        Layout.fillWidth: true
        implicitHeight: cardColumn.implicitHeight
        radius: AppTheme.cardRadius
        color: AppTheme.cardColor
        border.width: 1
        border.color: AppTheme.cardBorderColor

        ColumnLayout {
            id: cardColumn
            anchors.fill: parent
            spacing: 0
        }
    }
}
