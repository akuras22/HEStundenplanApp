import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "components"

// The Abfahrten view, next to Woche/Tag/Mensa: live VVS departures from one stop (by default the
// one at the user's campus) — from now, or from when today's last lecture ends. Like the Android
// app's Abfahrten tab.
Item {
    id: root

    signal searchRequested()
    signal noticesRequested(var notices)

    readonly property real maxContentWidth: Kirigami.Units.gridUnit * 40
    readonly property bool hasStop: !!transitController.stop.id
    // Keeps "in 4 Min." counting down between the minute-by-minute refreshes.
    property double now: Date.now()

    Timer {
        interval: 15000
        running: root.visible
        repeat: true
        onTriggered: root.now = Date.now()
    }

    // First visit without a Mensa picked to guess from: which stop?
    Controls.ScrollView {
        id: picker
        anchors.fill: parent
        visible: !root.hasStop
        contentWidth: availableWidth

        ColumnLayout {
            width: Math.min(picker.availableWidth - 2 * AppTheme.pageMargin, AppTheme.pageMaxWidth)
            x: Math.round((picker.availableWidth - width) / 2)
            spacing: AppTheme.gnome ? 24 : Kirigami.Units.largeSpacing * 2

            Kirigami.PlaceholderMessage {
                Layout.fillWidth: true
                Layout.topMargin: Kirigami.Units.largeSpacing
                icon.source: Qt.resolvedUrl("../icons/bus-symbolic.svg")
                text: qsTr("Welche Haltestelle?")
                explanation: qsTr("Abfahrten in Echtzeit vom VVS. Wechseln kannst du jederzeit oben über den Namen der Haltestelle.")
            }

            PrefGroup {
                title: qsTr("Haltestellen an der Hochschule")
                Repeater {
                    id: presetRows
                    model: transitController.presets
                    delegate: PrefRow {
                        required property var modelData
                        required property int index
                        first: index === 0
                        last: false
                        text: modelData.name
                        subtitle: modelData.campus
                        activatable: true
                        navigates: true
                        onClicked: transitController.setStop(modelData.id, modelData.name, modelData.shortName)
                    }
                }
                PrefRow {
                    first: false
                    last: true
                    text: qsTr("Andere Haltestelle suchen …")
                    icon.name: AppTheme.icon("search")
                    activatable: true
                    onClicked: root.searchRequested()
                }
            }
        }
    }

    ColumnLayout {
        anchors.fill: parent
        visible: root.hasStop
        spacing: Kirigami.Units.smallSpacing

        RowLayout {
            Layout.fillWidth: true
            Layout.maximumWidth: root.maxContentWidth
            Layout.alignment: Qt.AlignHCenter
            Layout.leftMargin: Kirigami.Units.smallSpacing
            Layout.rightMargin: Kirigami.Units.smallSpacing

            SegmentedSwitch {
                id: timeSwitch
                readonly property bool lectureEndOffered: transitController.lectureEndLabel !== ""
                Layout.fillWidth: false
                Layout.preferredWidth: lectureEndOffered ? Kirigami.Units.gridUnit * 25 : Kirigami.Units.gridUnit * 6
                // Only worth offering while today's last lecture is still ahead.
                model: lectureEndOffered
                       ? [{ text: qsTr("Jetzt") }, { text: qsTr("Nach der Vorlesung · %1").arg(transitController.lectureEndLabel) }]
                       : [{ text: qsTr("Jetzt") }]
                currentIndex: transitController.afterLecture && lectureEndOffered ? 1 : 0
                onActivated: (index) => transitController.afterLecture = (index === 1)
            }
            Item { Layout.fillWidth: true }
            Controls.BusyIndicator {
                Layout.preferredWidth: Kirigami.Units.iconSizes.smallMedium
                Layout.preferredHeight: Kirigami.Units.iconSizes.smallMedium
                running: transitController.loading
                visible: running
            }
            Controls.Label {
                text: [transitController.stop.name, transitController.updatedLabel].filter(part => !!part).join(" · ")
                color: AppTheme.disabledTextColor
                font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                elide: Text.ElideLeft
                Layout.maximumWidth: Kirigami.Units.gridUnit * 18
            }
        }

        Banner {
            Layout.maximumWidth: root.maxContentWidth
            Layout.alignment: Qt.AlignHCenter
            type: Kirigami.MessageType.Warning
            visible: transitController.departures.length > 0 && !!transitController.errorMessage
            text: qsTr("Aktualisieren fehlgeschlagen: %1").arg(transitController.errorMessage)
        }

        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true

            ListView {
                id: list
                anchors.fill: parent
                visible: transitController.departures.length > 0
                clip: true
                spacing: Kirigami.Units.smallSpacing
                model: transitController.departures
                Controls.ScrollBar.vertical: Controls.ScrollBar {}
                footer: Controls.Label {
                    width: list.width
                    topPadding: Kirigami.Units.smallSpacing
                    bottomPadding: Kirigami.Units.largeSpacing
                    leftPadding: Math.max(Kirigami.Units.smallSpacing, (list.width - root.maxContentWidth) / 2)
                    text: qsTr("Echtzeitdaten: VVS. Angaben ohne Gewähr.")
                    color: AppTheme.disabledTextColor
                    font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                }
                delegate: Item {
                    id: row
                    required property var modelData
                    readonly property int minutes: Math.floor((modelData.expectedMsecs - root.now) / 60000)
                    readonly property bool hasNotices: modelData.notices.length > 0
                    width: list.width
                    implicitHeight: card.implicitHeight

                    Rectangle {
                        id: card
                        width: Math.min(row.width - 2 * Kirigami.Units.smallSpacing, root.maxContentWidth)
                        x: Math.round((row.width - width) / 2)
                        implicitHeight: content.implicitHeight + 2 * Kirigami.Units.largeSpacing
                        radius: AppTheme.cardRadius
                        color: hover.hovered && row.hasNotices
                               ? Qt.tint(AppTheme.cardColor, Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.04))
                               : AppTheme.cardColor
                        border.color: AppTheme.cardBorderColor
                        border.width: 1

                        HoverHandler {
                            id: hover
                            cursorShape: row.hasNotices ? Qt.PointingHandCursor : Qt.ArrowCursor
                        }
                        MouseArea {
                            anchors.fill: parent
                            enabled: row.hasNotices
                            onClicked: root.noticesRequested(row.modelData.notices)
                        }

                        RowLayout {
                            id: content
                            anchors.fill: parent
                            anchors.margins: Kirigami.Units.largeSpacing
                            spacing: Kirigami.Units.largeSpacing

                            Rectangle {
                                Layout.preferredWidth: Math.max(48, lineLabel.implicitWidth + 16)
                                Layout.preferredHeight: lineLabel.implicitHeight + 10
                                radius: 6
                                color: [0, 1].indexOf(row.modelData.productClass) >= 0
                                       ? (row.modelData.productClass === 1 ? "#2E9E5B" : "#6E6E73")
                                       : [2, 3, 4].indexOf(row.modelData.productClass) >= 0 ? "#0A6BB0"
                                       : [5, 6, 7, 10].indexOf(row.modelData.productClass) >= 0 ? "#C0392B"
                                       : "#6E6E73"
                                Controls.Label {
                                    id: lineLabel
                                    anchors.centerIn: parent
                                    text: row.modelData.line
                                    color: "white"
                                    font.bold: true
                                }
                            }
                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 0
                                Controls.Label {
                                    Layout.fillWidth: true
                                    text: row.modelData.destination
                                    font.bold: true
                                    font.strikeout: row.modelData.cancelled
                                    elide: Text.ElideRight
                                }
                                RowLayout {
                                    spacing: Kirigami.Units.smallSpacing
                                    Controls.Label {
                                        visible: !!row.modelData.platform
                                        text: row.modelData.platform
                                        color: AppTheme.disabledTextColor
                                        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                                    }
                                    Kirigami.Icon {
                                        visible: row.hasNotices
                                        source: AppTheme.gnome ? "dialog-information-symbolic" : "dialog-information"
                                        Layout.preferredWidth: 14
                                        Layout.preferredHeight: 14
                                    }
                                }
                            }
                            ColumnLayout {
                                spacing: 0
                                Controls.Label {
                                    Layout.alignment: Qt.AlignRight
                                    text: row.modelData.cancelled ? qsTr("fällt aus")
                                        : row.minutes <= 0 ? qsTr("jetzt")
                                        : row.minutes < 60 ? qsTr("%1 Min.").arg(row.minutes)
                                        : new Date(row.modelData.expectedMsecs).toLocaleTimeString(Qt.locale(), "HH:mm")
                                    font.bold: true
                                    color: row.modelData.cancelled ? AppTheme.errorColor : AppTheme.textColor
                                }
                                RowLayout {
                                    Layout.alignment: Qt.AlignRight
                                    spacing: 4
                                    Controls.Label {
                                        text: row.modelData.plannedLabel
                                        // Green when real-time data says it's on time.
                                        color: row.modelData.delay === 0 && !row.modelData.cancelled ? AppTheme.positiveColor : AppTheme.disabledTextColor
                                        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                                    }
                                    Controls.Label {
                                        visible: row.modelData.delay > 0 && !row.modelData.cancelled
                                        text: "+" + row.modelData.delay
                                        color: AppTheme.errorColor
                                        font.bold: true
                                        font.pixelSize: Kirigami.Theme.smallFont.pixelSize
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Kirigami.PlaceholderMessage {
                anchors.centerIn: parent
                width: parent.width - Kirigami.Units.gridUnit * 4
                visible: transitController.departures.length === 0 && !transitController.loading
                         && !transitController.errorMessage && !!transitController.updatedLabel
                icon.source: Qt.resolvedUrl("../icons/bus-symbolic.svg")
                text: qsTr("Keine Abfahrten gefunden")
            }

            Kirigami.PlaceholderMessage {
                anchors.centerIn: parent
                width: parent.width - Kirigami.Units.gridUnit * 4
                visible: transitController.departures.length === 0 && !transitController.loading && !!transitController.errorMessage
                icon.name: AppTheme.gnome ? "dialog-warning-symbolic" : "dialog-warning"
                text: qsTr("Abfahrten konnten nicht geladen werden")
                explanation: transitController.errorMessage
                helpfulAction: Kirigami.Action {
                    icon.name: AppTheme.icon("view-refresh")
                    text: qsTr("Erneut versuchen")
                    onTriggered: transitController.refresh()
                }
            }

            Controls.BusyIndicator {
                anchors.centerIn: parent
                running: visible
                visible: transitController.loading && transitController.departures.length === 0
            }
        }
    }
}
