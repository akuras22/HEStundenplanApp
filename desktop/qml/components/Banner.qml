import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// An inline status message. KDE look: Kirigami's own InlineMessage. GNOME look: a tinted rounded
// card with a symbolic icon, the closest thing libadwaita has to it.
Loader {
    id: root

    property string text
    /** One of Kirigami.MessageType. */
    property int type: Kirigami.MessageType.Information

    Layout.fillWidth: true
    sourceComponent: AppTheme.gnome ? gnomeBanner : kdeBanner

    Component {
        id: kdeBanner
        Kirigami.InlineMessage {
            visible: true
            type: root.type
            text: root.text
        }
    }

    Component {
        id: gnomeBanner
        Rectangle {
            readonly property color tint: root.type === Kirigami.MessageType.Error ? AppTheme.errorColor
                                        : root.type === Kirigami.MessageType.Warning ? AppTheme.warningColor
                                        : root.type === Kirigami.MessageType.Positive ? AppTheme.positiveColor
                                        : AppTheme.textColor
            implicitHeight: row.implicitHeight + 20
            radius: AppTheme.cardRadius
            color: Qt.rgba(tint.r, tint.g, tint.b, root.type === Kirigami.MessageType.Information ? 0.08 : 0.14)

            RowLayout {
                id: row
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 12
                spacing: 10

                Kirigami.Icon {
                    Layout.preferredWidth: 16
                    Layout.preferredHeight: 16
                    Layout.alignment: Qt.AlignVCenter
                    isMask: true
                    color: parent.parent.tint
                    source: root.type === Kirigami.MessageType.Error ? "dialog-error-symbolic"
                          : root.type === Kirigami.MessageType.Warning ? "dialog-warning-symbolic"
                          : root.type === Kirigami.MessageType.Positive ? "object-select-symbolic"
                          : "dialog-information-symbolic"
                }
                Controls.Label {
                    Layout.fillWidth: true
                    text: root.text
                    wrapMode: Text.Wrap
                }
            }
        }
    }
}
