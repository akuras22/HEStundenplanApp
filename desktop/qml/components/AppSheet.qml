import QtQuick
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop
import de.hsesslingen.stundenplan.adwaita as Adwaita

// The app's modal dialogs: a Kirigami.OverlaySheet with a wrapping title. KDE look: exactly that.
// GNOME look: the same sheet on a libadwaita dialog surface — large corner radius, no outline,
// bold title.
Kirigami.OverlaySheet {
    id: root

    header: Kirigami.Heading {
        text: root.title
        level: AppTheme.gnome ? 3 : 2
        font.weight: AppTheme.gnome ? Font.Bold : Font.Normal
        wrapMode: Text.Wrap
        // Keeps the header taller than the close button next to it; when the button is the
        // taller one, Kirigami's centering of it turns into a binding loop.
        topPadding: AppTheme.gnome ? Kirigami.Units.smallSpacing : 0
        bottomPadding: topPadding
    }

    Component.onCompleted: {
        if (AppTheme.gnome)
            background = gnomeBackground.createObject(root.contentItem)
    }

    Component {
        id: gnomeBackground
        Kirigami.ShadowedRectangle {
            radius: Adwaita.Adw.dialogRadius
            color: Adwaita.Adw.dialogBg
            shadow.size: 28
            shadow.yOffset: 6
            shadow.color: Qt.rgba(0, 0, 0, 0.3)
        }
    }
}
