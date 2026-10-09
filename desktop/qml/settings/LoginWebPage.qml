import QtQuick
import QtQuick.Controls as Controls
import QtWebEngine
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// The SSO login itself, in an embedded browser: the user signs in on the university's own pages
// (whatever second factor they use included), and as soon as the page lands back on QIS with a
// Shibboleth session cookie, QisSession keeps that cookie and this page closes.
Kirigami.Page {
    id: root
    title: qsTr("Anmelden")
    padding: 0

    WebEngineView {
        id: webView
        anchors.fill: parent
        // The shared, persistent login profile (see Main.qml): keeping the SSO session cookies is
        // what lets the login be renewed silently later instead of asking for the second factor
        // every time. QisSession already collects this profile's Shibboleth cookies (attached once
        // in Main.qml), so there's nothing to attach here.
        profile: applicationWindow().qisLoginProfile
        // The Studiengangpläne page: opening it over https sends the browser through the SSO
        // login and, once that's done, back to QIS with the session cookie set.
        url: "https://www3.hs-esslingen.de/qislsf/rds?state=verpublish&publishContainer=stgPlanList"
             + "&navigationPosition=lectures,curriculaschedulesList&breadcrumb=curriculaschedules"
             + "&topitem=lectures&subitem=curriculaschedulesList"

        onLoadingChanged: function(loadingInfo) {
            if (loadingInfo.status !== WebEngineLoadingInfo.LoadSucceededStatus)
                return
            if (!loadingInfo.url.toString().startsWith("https://www3.hs-esslingen.de/qislsf"))
                return
            if (qisSession.completeLogin(true)) {
                timetableController.refresh()
                applicationWindow().pageStack.pop()
            }
        }
    }

    Controls.ProgressBar {
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        visible: webView.loading
        indeterminate: true
    }
}
