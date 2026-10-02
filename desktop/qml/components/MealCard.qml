import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// One dish of the Speiseplan: photo on the left, name, student price and markers on the right.
// Fixed height, so a row of cards in the grid never ends up ragged.
Rectangle {
    id: root

    property var meal: ({})
    signal clicked()

    // "Veganer Renner" is both a category and a marker on every dish in it — repeating it on the
    // card right under that very heading would just be noise (the details still show it).
    readonly property var shownLabels: (meal.labels || [])
        .filter(label => label.title.toLowerCase() !== (meal.category || "").toLowerCase())
        .slice(0, 2)

    implicitWidth: Kirigami.Units.gridUnit * 18
    implicitHeight: Kirigami.Units.gridUnit * 6
    radius: AppTheme.cardRadius
    color: hover.hovered
           ? Qt.tint(AppTheme.cardColor, Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.04))
           : AppTheme.cardColor
    border.color: AppTheme.cardBorderColor
    border.width: 1

    HoverHandler {
        id: hover
        cursorShape: Qt.PointingHandCursor
    }
    MouseArea {
        anchors.fill: parent
        onClicked: root.clicked()
    }

    MealPhoto {
        id: photo
        anchors.left: parent.left
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        width: Math.round(height * 1.2)
        url: root.meal.imageUrl || ""
        radius: root.radius
        roundRightCorners: false
    }

    ColumnLayout {
        anchors.left: photo.right
        anchors.right: parent.right
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        anchors.margins: Kirigami.Units.largeSpacing
        spacing: 2

        Controls.Label {
            Layout.fillWidth: true
            visible: !!root.meal.subcategory
            text: root.meal.subcategory || ""
            color: AppTheme.gnome ? AppTheme.accentColor : Kirigami.Theme.highlightColor
            font.bold: true
            font.pixelSize: Kirigami.Theme.smallFont.pixelSize
            elide: Text.ElideRight
        }
        Controls.Label {
            Layout.fillWidth: true
            text: root.meal.name || ""
            font.bold: true
            wrapMode: Text.Wrap
            maximumLineCount: root.meal.subcategory ? 2 : 3
            elide: Text.ElideRight
        }
        Item {
            Layout.fillHeight: true
        }
        RowLayout {
            Layout.fillWidth: true
            spacing: Kirigami.Units.smallSpacing

            Controls.Label {
                visible: !!root.meal.price
                text: qsTr("%1 €").arg(root.meal.price || "")
                font.bold: true
            }
            Item {
                Layout.fillWidth: true
            }
            Repeater {
                model: root.shownLabels
                delegate: LabelPill {
                    required property var modelData
                    label: modelData
                }
            }
        }
    }
}
