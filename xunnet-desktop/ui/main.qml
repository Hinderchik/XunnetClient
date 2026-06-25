import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import XunnetDesktop

ApplicationWindow {
    visible: true
    width: 900
    height: 600
    title: "Xunnet Desktop"

    XunnetManager { id: manager }

    StackLayout {
        anchors.fill: parent
        currentIndex: nav.currentIndex

        Dashboard { manager: manager }
        Proxies {}
        Federation {}
        Settings {}
    }

    footer: TabBar {
        id: nav
        TabButton { text: qsTr("Dashboard") }
        TabButton { text: qsTr("Proxies") }
        TabButton { text: qsTr("Federation") }
        TabButton { text: qsTr("Settings") }
    }
}
