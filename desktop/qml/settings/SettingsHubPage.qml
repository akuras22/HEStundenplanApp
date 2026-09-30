import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import "../components"

PrefPage {
    id: root
    title: qsTr("Einstellungen")

    Component { id: studiengaengePage; StudiengaengePage {} }
    Component { id: notificationsPage; NotificationsPage {} }
    Component { id: appearancePage; AppearancePage {} }
    Component { id: aboutPage; AboutPage {} }

    PrefGroup {
        Repeater {
            model: [
                { title: qsTr("Studiengänge"), icon: "view-calendar-list", page: studiengaengePage },
                { title: qsTr("Benachrichtigungen"), icon: "notifications", page: notificationsPage },
                { title: qsTr("Darstellung"), icon: "preferences-desktop-theme", page: appearancePage },
                { title: qsTr("Über die App"), icon: "help-about", page: aboutPage },
            ]
            delegate: PrefRow {
                required property var modelData
                required property int index
                // Inside a Repeater the Repeater itself counts among the parent's children, so
                // say explicitly where the row sits.
                first: index === 0
                last: index === 3
                text: modelData.title
                icon.name: AppTheme.icon(modelData.icon)
                activatable: true
                navigates: true
                onClicked: applicationWindow().pageStack.push(modelData.page)
            }
        }
    }
}
