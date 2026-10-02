import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "components"

// The Mensa view, next to Woche/Tag: the Studierendenwerk Stuttgart's Speiseplan for the chosen
// Mensa, one weekday at a time — like the Android app's Mensa tab.
Item {
    id: root

    signal mealClicked(var meal)

    // Like libadwaita's AdwClamp: at most two columns of cards, centered — a wide window would
    // otherwise leave most categories (usually a single dish) as one card lost in empty space.
    readonly property real maxContentWidth: Kirigami.Units.gridUnit * 46

    readonly property var hsLocations: mensaController.locations.filter(location => location.atHsEsslingen)
    readonly property var otherLocations: mensaController.locations.filter(location => !location.atHsEsslingen)

    // First visit: which Mensa? There's no telling from the Studiengang which campus someone eats
    // at, so this asks once — switching later goes through the title.
    Controls.ScrollView {
        id: picker
        anchors.fill: parent
        visible: mensaController.locationId === 0
        contentWidth: availableWidth

        ColumnLayout {
            width: Math.min(picker.availableWidth - 2 * AppTheme.pageMargin, AppTheme.pageMaxWidth)
            x: Math.round((picker.availableWidth - width) / 2)
            spacing: AppTheme.gnome ? 24 : Kirigami.Units.largeSpacing * 2

            Kirigami.PlaceholderMessage {
                Layout.fillWidth: true
                Layout.topMargin: Kirigami.Units.largeSpacing
                icon.name: AppTheme.gnome ? "emoji-food-symbolic" : "food"
                text: qsTr("Welche Mensa?")
                explanation: qsTr("Der Speiseplan kommt live vom Studierendenwerk Stuttgart. Wechseln kannst du jederzeit oben über den Namen der Mensa.")
            }

            Repeater {
                model: [
                    { title: qsTr("Hochschule Esslingen"), locations: root.hsLocations },
                    { title: qsTr("Weitere Mensen"), locations: root.otherLocations },
                ]
                delegate: PrefGroup {
                    id: group
                    required property var modelData
                    title: modelData.title

                    Repeater {
                        id: rows
                        model: group.modelData.locations
                        delegate: PrefRow {
                            required property var modelData
                            required property int index
                            // Inside a Repeater the Repeater itself counts among the parent's
                            // children, so say explicitly where the row sits.
                            first: index === 0
                            last: index === rows.count - 1
                            text: modelData.name
                            activatable: true
                            navigates: true
                            onClicked: mensaController.locationId = modelData.id
                        }
                    }
                }
            }

            Item {
                implicitHeight: Kirigami.Units.largeSpacing
            }
        }
    }

    ColumnLayout {
        anchors.fill: parent
        visible: mensaController.locationId !== 0
        spacing: Kirigami.Units.smallSpacing

        // Same date heading as the Tag view.
        RowLayout {
            Layout.fillWidth: true
            Layout.maximumWidth: root.maxContentWidth
            Layout.alignment: Qt.AlignHCenter
            Layout.leftMargin: Kirigami.Units.smallSpacing
            Layout.rightMargin: Kirigami.Units.smallSpacing
            Kirigami.Heading {
                level: 3
                text: mensaController.dateTitle
            }
            Item { Layout.fillWidth: true }
            Kirigami.Chip {
                visible: mensaController.isToday && !AppTheme.gnome
                text: qsTr("Heute")
                checkable: false
                closable: false
            }
            Rectangle {
                visible: mensaController.isToday && AppTheme.gnome
                implicitWidth: todayLabel.implicitWidth + 20
                implicitHeight: todayLabel.implicitHeight + 8
                radius: height / 2
                color: Qt.rgba(AppTheme.accentColor.r, AppTheme.accentColor.g, AppTheme.accentColor.b, 0.2)
                Controls.Label {
                    id: todayLabel
                    anchors.centerIn: parent
                    text: qsTr("Heute")
                    font.bold: true
                    font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                }
            }
        }

        // Quick jumps within the week, on top of the arrows next to the view switcher.
        SegmentedSwitch {
            Layout.fillWidth: true
            Layout.maximumWidth: root.maxContentWidth
            Layout.alignment: Qt.AlignHCenter
            Layout.leftMargin: Kirigami.Units.smallSpacing
            Layout.rightMargin: Kirigami.Units.smallSpacing
            model: mensaController.weekDays.map(day => ({ text: day.dayLabel + " " + day.dateLabel }))
            currentIndex: mensaController.dayIndex
            onActivated: (index) => mensaController.selectWeekday(index)
        }

        // A failed refresh of a day that's already on screen keeps showing it and just says so.
        Banner {
            Layout.maximumWidth: root.maxContentWidth
            Layout.alignment: Qt.AlignHCenter
            type: Kirigami.MessageType.Warning
            visible: mensaController.hasDay && !!mensaController.errorMessage
            text: qsTr("Aktualisieren fehlgeschlagen: %1").arg(mensaController.errorMessage)
        }

        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true

            Controls.ScrollView {
                id: scroll

                readonly property real clampedWidth: Math.min(availableWidth - 2 * Kirigami.Units.smallSpacing, root.maxContentWidth)
                // Every card gets exactly one column's width — also a category's lone dish,
                // which GridLayout would otherwise stretch across the whole row.
                readonly property int cardSpacing: Kirigami.Units.largeSpacing
                readonly property int columns: Math.max(1, Math.floor((clampedWidth + cardSpacing) / (Kirigami.Units.gridUnit * 20 + cardSpacing)))
                readonly property real cardWidth: Math.floor((clampedWidth - (columns - 1) * cardSpacing) / columns)

                anchors.fill: parent
                visible: mensaController.categories.length > 0
                contentWidth: availableWidth

                ColumnLayout {
                    width: scroll.clampedWidth
                    x: Math.round((scroll.availableWidth - width) / 2)
                    spacing: Kirigami.Units.largeSpacing

                    Repeater {
                        model: mensaController.categories
                        delegate: ColumnLayout {
                            id: category
                            required property var modelData
                            Layout.fillWidth: true
                            spacing: Kirigami.Units.smallSpacing

                            Controls.Label {
                                Layout.topMargin: Kirigami.Units.smallSpacing
                                text: category.modelData.name
                                font.bold: true
                                color: AppTheme.disabledTextColor
                            }
                            GridLayout {
                                Layout.fillWidth: true
                                columns: scroll.columns
                                columnSpacing: scroll.cardSpacing
                                rowSpacing: scroll.cardSpacing

                                Repeater {
                                    model: category.modelData.meals
                                    delegate: MealCard {
                                        required property var modelData
                                        Layout.preferredWidth: scroll.cardWidth
                                        meal: modelData
                                        onClicked: root.mealClicked(modelData)
                                    }
                                }
                            }
                        }
                    }

                    Controls.Label {
                        Layout.fillWidth: true
                        Layout.topMargin: Kirigami.Units.smallSpacing
                        Layout.bottomMargin: Kirigami.Units.largeSpacing
                        text: qsTr("Preise für Studierende — Bedienstete und Gäste in den Details. Angaben ohne Gewähr, Quelle: Studierendenwerk Stuttgart.")
                        color: AppTheme.disabledTextColor
                        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                        wrapMode: Text.Wrap
                    }
                }
            }

            Kirigami.PlaceholderMessage {
                anchors.centerIn: parent
                width: parent.width - Kirigami.Units.gridUnit * 4
                visible: mensaController.hasDay && mensaController.categories.length === 0
                icon.name: AppTheme.gnome ? "emoji-food-symbolic" : "food"
                text: mensaController.closedMessage
            }

            Kirigami.PlaceholderMessage {
                anchors.centerIn: parent
                width: parent.width - Kirigami.Units.gridUnit * 4
                visible: !mensaController.hasDay && !mensaController.loading && !!mensaController.errorMessage
                icon.name: AppTheme.gnome ? "dialog-warning-symbolic" : "dialog-warning"
                text: qsTr("Speiseplan konnte nicht geladen werden")
                explanation: mensaController.errorMessage
                helpfulAction: Kirigami.Action {
                    icon.name: AppTheme.icon("view-refresh")
                    text: qsTr("Erneut versuchen")
                    onTriggered: mensaController.refresh()
                }
            }

            Controls.BusyIndicator {
                anchors.centerIn: parent
                running: visible
                visible: mensaController.loading && !mensaController.hasDay
            }
        }
    }
}
