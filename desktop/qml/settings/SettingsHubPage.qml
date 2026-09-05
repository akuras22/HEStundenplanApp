import QtQuick
import QtQuick.Layouts
import QtQuick.Controls as Controls
import org.kde.kirigami as Kirigami

Kirigami.ScrollablePage {
    id: root
    title: qsTr("Einstellungen")

    Component { id: studiengaengePage; StudiengaengePage {} }
    Component { id: notificationsPage; NotificationsPage {} }
    Component { id: appearancePage; AppearancePage {} }
    Component { id: aboutPage; AboutPage {} }

    ListView {
        model: [
            { title: qsTr("Studiengänge"), icon: "view-calendar-list", page: studiengaengePage },
            { title: qsTr("Benachrichtigungen"), icon: "notifications", page: notificationsPage },
            { title: qsTr("Darstellung"), icon: "preferences-desktop-theme", page: appearancePage },
            { title: qsTr("Über die App"), icon: "help-about", page: aboutPage },
        ]
        delegate: Controls.ItemDelegate {
            required property var modelData
            width: ListView.view.width
            text: modelData.title
            icon.name: modelData.icon
            onClicked: applicationWindow().pageStack.push(modelData.page)
        }
    }
}
