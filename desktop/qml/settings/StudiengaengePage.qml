import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Studiengänge")

    // Same centered, width-capped column as the other settings pages (see PrefPage.qml), just
    // built around a ListView since this list is long.
    readonly property real sideMargin: Math.max(AppTheme.pageMargin, (width - AppTheme.pageMaxWidth) / 2)

    Kirigami.Theme.inherit: false
    Kirigami.Theme.colorSet: Kirigami.Theme.Window

    Component.onCompleted: timetableController.loadStudiengaenge()

    actions: [
        Kirigami.Action {
            icon.name: AppTheme.icon("view-refresh")
            text: qsTr("Neu laden")
            onTriggered: timetableController.loadStudiengaenge()
        }
    ]

    header: Item {
        implicitHeight: filterField.implicitHeight + AppTheme.pageMargin
        Controls.TextField {
            id: filterField
            anchors.left: parent.left
            anchors.right: parent.right
            anchors.top: parent.top
            anchors.leftMargin: root.sideMargin
            anchors.rightMargin: root.sideMargin
            placeholderText: qsTr("Suchen…")
        }
    }

    ListView {
        id: listView

        // Spacing above/below the card. (Not ListView's own margins: ScrollablePage manages
        // those itself, which is also why delegates are full-width items with the row inset
        // by hand rather than the whole view being inset.)
        header: Item { height: AppTheme.pageMargin }
        footer: Item { height: AppTheme.pageMargin * 2 }

        model: timetableController.studiengaenge.filter(function (s) {
            return filterField.text.length === 0 || s.code.toLowerCase().indexOf(filterField.text.toLowerCase()) >= 0
        })

        Connections {
            target: filterField
            function onTextChanged() { listView.forceLayout() }
        }

        // The card all rows sit on — one item behind the delegates rather than a piece per row,
        // so it can have a continuous border.
        Rectangle {
            parent: listView.contentItem
            z: -1
            x: root.sideMargin
            y: listView.originY + AppTheme.pageMargin
            width: listView.width - 2 * root.sideMargin
            height: listView.contentHeight - 3 * AppTheme.pageMargin
            visible: listView.count > 0
            radius: AppTheme.cardRadius
            color: AppTheme.cardColor
            border.width: 1
            border.color: AppTheme.cardBorderColor
        }

        delegate: Item {
            id: delegateItem
            required property var modelData
            required property int index
            readonly property bool selected: timetableController.selectedStudiengang.id === modelData.id

            width: ListView.view.width
            height: row.implicitHeight

            PrefRow {
                id: row
                x: root.sideMargin
                width: parent.width - 2 * root.sideMargin
                first: delegateItem.index === 0
                last: delegateItem.index === delegateItem.ListView.view.count - 1
                text: delegateItem.modelData.code
                font.bold: delegateItem.selected
                activatable: true
                onClicked: timetableController.selectStudiengang(delegateItem.modelData.code, delegateItem.modelData.abstgvnr, delegateItem.modelData.parallelid)

                Kirigami.Icon {
                    visible: delegateItem.selected
                    source: AppTheme.gnome ? "object-select-symbolic" : "checkmark"
                    isMask: true
                    color: AppTheme.accentColor
                    Layout.preferredWidth: 16
                    Layout.preferredHeight: 16
                    Layout.rightMargin: Kirigami.Units.smallSpacing
                }
                Controls.ToolButton {
                    // Bound to the NOTIFY-backed favoriteStudiengaenge property (not the plain
                    // isFavorite() invokable, which QML has no way to know it must re-check when
                    // favorites change elsewhere) so the star actually updates on click.
                    icon.name: timetableController.favoriteStudiengaenge.some((f) => f.id === delegateItem.modelData.id)
                               ? "starred-symbolic" : "non-starred-symbolic"
                    onClicked: timetableController.toggleFavorite(delegateItem.modelData.code, delegateItem.modelData.abstgvnr, delegateItem.modelData.parallelid)
                }
            }
        }
    }
}
