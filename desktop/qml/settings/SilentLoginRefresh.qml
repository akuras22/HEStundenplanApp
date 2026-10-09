import QtQuick
import QtWebEngine
import de.hsesslingen.stundenplan.desktop

// Renews the login with no UI. It loads the Studiengangpläne page in an invisible browser, and
// because the HS Esslingen SSO session (Keycloak, up to 10 h) still lives in the shared, persistent
// login profile from the last login, Shibboleth hands back a fresh QIS session cookie with no
// password and no second factor. On success QisSession keeps that cookie; if the SSO session has
// ended too, the bounce stalls on the login form and the timeout below gives up until a real login.
// Mounted by Main.qml only while qisSession.silentRefreshActive is true.
Item {
    id: root
    // Invisible and non-interactive, but kept visible/sized so the embedded browser actually runs
    // its JavaScript and redirects instead of being suspended off-screen.
    width: 1
    height: 1
    opacity: 0
    enabled: false

    WebEngineView {
        anchors.fill: parent
        profile: applicationWindow().qisLoginProfile
        url: "https://www3.hs-esslingen.de/qislsf/rds?state=verpublish&publishContainer=stgPlanList"
             + "&navigationPosition=lectures,curriculaschedulesList&breadcrumb=curriculaschedules"
             + "&topitem=lectures&subitem=curriculaschedulesList"

        onLoadingChanged: function(loadingInfo) {
            if (loadingInfo.status !== WebEngineLoadingInfo.LoadSucceededStatus)
                return
            // Success only once the chain has landed back on QIS. Intermediate stops on the SSO
            // hosts are ignored: with a live session Keycloak auto-submits straight through them.
            if (!loadingInfo.url.toString().startsWith("https://www3.hs-esslingen.de/qislsf"))
                return
            qisSession.completeLogin(false)
        }
    }

    // A dead SSO session stalls on the login form instead of coming back — give up after this so
    // the refresh view isn't left running forever. The happy path finishes in a second or two.
    Timer {
        interval: 15000
        running: true
        onTriggered: qisSession.failSilentRefresh()
    }
}
