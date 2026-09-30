import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import QtQuick.Templates as T
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// One row of a PrefGroup: optional icon, title (text) and subtitle, and whatever is declared
// inside it at the trailing edge (a Switch, a Button, ...). Activatable rows react to hover and
// click as a whole; the others only host their trailing control.
// Built on the bare template so it looks the same whichever Controls style is active.
T.ItemDelegate {
    id: root

    property string subtitle
    property bool activatable: false
    /** Shows a trailing arrow: the row opens another page. */
    property bool navigates: false
    default property alias trailing: trailingRow.data

    // Position within the card, for the separator and for matching its rounded corners. Rows of a
    // PrefGroup work this out themselves; ListView delegates must set both from their index.
    property bool first: parent?.visibleChildren[0] === root
    property bool last: parent?.visibleChildren[(parent?.visibleChildren.length ?? 1) - 1] === root

    Layout.fillWidth: true
    implicitWidth: implicitContentWidth + leftPadding + rightPadding
    implicitHeight: Math.max(AppTheme.rowHeight, implicitContentHeight + topPadding + bottomPadding)
    leftPadding: AppTheme.gnome ? 12 : Kirigami.Units.largeSpacing
    rightPadding: leftPadding
    topPadding: Kirigami.Units.smallSpacing
    bottomPadding: Kirigami.Units.smallSpacing
    hoverEnabled: activatable

    background: Rectangle {
        readonly property real intensity: !root.activatable ? 0 : root.down ? 0.1 : root.hovered ? 0.05 : 0
        topLeftRadius: root.first ? AppTheme.cardRadius : 0
        topRightRadius: topLeftRadius
        bottomLeftRadius: root.last ? AppTheme.cardRadius : 0
        bottomRightRadius: bottomLeftRadius
        color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, intensity)

        Rectangle {
            visible: !root.first
            width: parent.width
            height: 1
            color: AppTheme.separatorColor
        }
    }

    contentItem: RowLayout {
        spacing: AppTheme.gnome ? 12 : Kirigami.Units.largeSpacing

        Kirigami.Icon {
            visible: root.icon.name !== ""
            source: root.icon.name
            isMask: root.icon.name.endsWith("-symbolic")
            color: AppTheme.textColor
            Layout.preferredWidth: AppTheme.gnome ? 16 : Kirigami.Units.iconSizes.smallMedium
            Layout.preferredHeight: Layout.preferredWidth
        }
        ColumnLayout {
            Layout.fillWidth: true
            spacing: 2
            Controls.Label {
                Layout.fillWidth: true
                text: root.text
                font: root.font
                elide: Text.ElideRight
            }
            Controls.Label {
                Layout.fillWidth: true
                visible: root.subtitle !== ""
                text: root.subtitle
                color: AppTheme.disabledTextColor
                font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                wrapMode: Text.Wrap
            }
        }
        RowLayout {
            id: trailingRow
            spacing: Kirigami.Units.smallSpacing
        }
        Kirigami.Icon {
            visible: root.navigates
            source: AppTheme.icon("go-next")
            isMask: true
            color: AppTheme.disabledTextColor
            Layout.preferredWidth: 16
            Layout.preferredHeight: 16
        }
    }
}
