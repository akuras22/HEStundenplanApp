import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.OverlaySheet {
    id: root

    property var releaseInfo: ({})

    header: Kirigami.Heading {
        text: qsTr("Änderungsprotokoll")
        level: 2
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 22
        spacing: Kirigami.Units.smallSpacing

        Controls.Label {
            Layout.fillWidth: true
            font.bold: true
            text: root.releaseInfo.versionName || ""
        }

        Controls.ScrollView {
            id: scrollView
            Layout.fillWidth: true
            Layout.preferredHeight: Kirigami.Units.gridUnit * 14

            Controls.Label {
                // Binding to the ScrollView's own width (not the implicit "parent" inside its
                // internal Flickable, which has no set width) — otherwise the label never wraps
                // and just grows into one long unreadable line the sheet has to scroll sideways.
                width: scrollView.availableWidth
                text: root.releaseInfo.releaseNotes || qsTr("Keine Angaben verfügbar.")
                textFormat: Text.MarkdownText
                wrapMode: Text.Wrap
            }
        }
    }
}
