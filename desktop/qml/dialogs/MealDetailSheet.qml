import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

// Everything the Speiseplan says about one dish: big photo, all three prices, markers, allergens,
// additives and nutrition values — one titled section each, like the Android app's dialog.
AppSheet {
    id: root

    property var meal: ({})

    title: root.meal.name || ""

    // A bold section title over its content.
    component Section: ColumnLayout {
        property alias title: heading.text
        Layout.fillWidth: true
        spacing: 2
        Controls.Label {
            id: heading
            font.bold: true
        }
    }

    // One "label ... value" line of a section.
    component ValueRow: RowLayout {
        property alias label: labelText.text
        property alias value: valueText.text
        property bool indented: false
        property bool emphasized: false
        Layout.fillWidth: true
        Controls.Label {
            id: labelText
            Layout.fillWidth: true
            Layout.leftMargin: parent.indented ? Kirigami.Units.largeSpacing : 0
            color: parent.indented ? AppTheme.disabledTextColor : AppTheme.textColor
        }
        Controls.Label {
            id: valueText
            font.bold: parent.emphasized
        }
    }

    ColumnLayout {
        Layout.preferredWidth: Kirigami.Units.gridUnit * 24
        spacing: Kirigami.Units.largeSpacing

        MealPhoto {
            Layout.fillWidth: true
            Layout.preferredHeight: Math.round(width * 9 / 16)
            visible: !!root.meal.imageUrl
            url: root.meal.imageUrl || ""
            radius: AppTheme.cardRadius
            decodeWidth: 1280
        }

        ColumnLayout {
            Layout.fillWidth: true
            spacing: Kirigami.Units.smallSpacing

            Controls.Label {
                Layout.fillWidth: true
                text: [root.meal.category, root.meal.subcategory].filter(part => !!part).join(" · ")
                color: AppTheme.disabledTextColor
                wrapMode: Text.Wrap
            }
            Flow {
                Layout.fillWidth: true
                visible: (root.meal.labels || []).length > 0
                spacing: Kirigami.Units.smallSpacing
                Repeater {
                    model: root.meal.labels || []
                    delegate: LabelPill {
                        required property var modelData
                        label: modelData
                    }
                }
            }
        }

        Section {
            title: qsTr("Preise")
            visible: (root.meal.prices || []).length > 0
            Repeater {
                model: root.meal.prices || []
                delegate: ValueRow {
                    required property var modelData
                    required property int index
                    label: modelData.group
                    value: qsTr("%1 €").arg(modelData.price)
                    emphasized: index === 0
                }
            }
        }

        Section {
            title: qsTr("Allergene")
            visible: !!root.meal.allergens
            Controls.Label {
                Layout.fillWidth: true
                text: root.meal.allergens || ""
                wrapMode: Text.Wrap
            }
        }

        Section {
            title: qsTr("Zusatzstoffe")
            visible: !!root.meal.additives
            Controls.Label {
                Layout.fillWidth: true
                text: root.meal.additives || ""
                wrapMode: Text.Wrap
            }
        }

        Section {
            title: qsTr("Weitere Kennzeichnungen")
            visible: !!root.meal.unknownCodes
            Controls.Label {
                Layout.fillWidth: true
                text: root.meal.unknownCodes || ""
                wrapMode: Text.Wrap
            }
        }

        Section {
            title: root.meal.nutritionBasis ? qsTr("Nährwerte pro %1").arg(root.meal.nutritionBasis) : qsTr("Nährwerte")
            visible: (root.meal.nutrition || []).length > 0
            Repeater {
                model: root.meal.nutrition || []
                delegate: ValueRow {
                    required property var modelData
                    label: modelData.label
                    value: modelData.value
                    indented: modelData.sub
                }
            }
        }
    }
}
