import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Studiengänge")

    Component.onCompleted: timetableController.loadStudiengaenge()

    actions: [
        Kirigami.Action {
            icon.name: "view-refresh"
            text: qsTr("Neu laden")
            onTriggered: timetableController.loadStudiengaenge()
        }
    ]

    header: Controls.TextField {
        id: filterField
        Layout.fillWidth: true
        placeholderText: qsTr("Suchen…")
        leftPadding: Kirigami.Units.largeSpacing
        topPadding: Kirigami.Units.smallSpacing
        bottomPadding: Kirigami.Units.smallSpacing
    }

    ListView {
        id: listView
        model: timetableController.studiengaenge.filter(function (s) {
            return filterField.text.length === 0 || s.code.toLowerCase().indexOf(filterField.text.toLowerCase()) >= 0
        })

        Connections {
            target: filterField
            function onTextChanged() { listView.forceLayout() }
        }

        delegate: Controls.ItemDelegate {
            id: delegateItem
            required property var modelData
            width: ListView.view.width
            highlighted: timetableController.selectedStudiengang.id === modelData.id

            contentItem: RowLayout {
                Controls.Label {
                    Layout.fillWidth: true
                    text: delegateItem.modelData.code
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

            onClicked: timetableController.selectStudiengang(modelData.code, modelData.abstgvnr, modelData.parallelid)
        }
    }
}
