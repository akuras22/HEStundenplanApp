import QtQuick
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// A scrollable settings page: PrefGroups stacked in a column that is centered and capped in width
// on wide windows (libadwaita's AdwPreferencesPage / KDE's FormCard pages).
Kirigami.ScrollablePage {
    id: root

    default property alias groups: column.data

    Kirigami.Theme.inherit: false
    Kirigami.Theme.colorSet: Kirigami.Theme.Window

    leftPadding: AppTheme.pageMargin
    rightPadding: AppTheme.pageMargin
    topPadding: AppTheme.pageMargin
    bottomPadding: AppTheme.pageMargin * 2

    ColumnLayout {
        spacing: 0

        ColumnLayout {
            id: column
            Layout.alignment: Qt.AlignHCenter | Qt.AlignTop
            Layout.fillWidth: true
            Layout.maximumWidth: AppTheme.pageMaxWidth
            spacing: AppTheme.gnome ? 24 : Kirigami.Units.largeSpacing * 2
        }
    }
}
