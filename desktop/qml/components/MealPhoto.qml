import QtQuick
import QtQuick.Effects
import org.kde.kirigami as Kirigami
import de.hsesslingen.stundenplan.desktop

// The photo, cropped to fill its box, with the card's rounded corners. Sits on a placeholder
// (a food glyph) that shows while it loads — and stays for dishes the site has no photo of.
Item {
    id: root

    property string url
    property real radius
    property bool roundRightCorners: true
    // Decoding at display size instead of the full 1920x1080 original keeps memory small.
    property int decodeWidth: 400

    Rectangle {
        id: placeholder
        anchors.fill: parent
        topLeftRadius: root.radius
        bottomLeftRadius: root.radius
        topRightRadius: root.roundRightCorners ? root.radius : 0
        bottomRightRadius: root.roundRightCorners ? root.radius : 0
        color: Qt.rgba(AppTheme.textColor.r, AppTheme.textColor.g, AppTheme.textColor.b, 0.07)

        Kirigami.Icon {
            anchors.centerIn: parent
            width: Kirigami.Units.iconSizes.medium
            height: width
            source: AppTheme.gnome ? "emoji-food-symbolic" : "food"
            color: AppTheme.disabledTextColor
            isMask: true
            opacity: 0.6
        }
    }

    Image {
        id: image
        anchors.fill: parent
        visible: root.url !== ""
        source: root.url
        fillMode: Image.PreserveAspectCrop
        asynchronous: true
        sourceSize.width: root.decodeWidth
        opacity: status === Image.Ready ? 1 : 0
        Behavior on opacity { NumberAnimation { duration: 150 } }

        // The software renderer can't do layer effects; square corners are the fallback there.
        layer.enabled: GraphicsInfo.api !== GraphicsInfo.Software
        layer.effect: MultiEffect {
            maskEnabled: true
            maskSource: mask
            maskThresholdMin: 0.5
            maskSpreadAtMin: 1.0
        }
    }

    Rectangle {
        id: mask
        anchors.fill: parent
        topLeftRadius: placeholder.topLeftRadius
        bottomLeftRadius: placeholder.bottomLeftRadius
        topRightRadius: placeholder.topRightRadius
        bottomRightRadius: placeholder.bottomRightRadius
        visible: false
        layer.enabled: true
    }
}
