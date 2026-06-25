import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import XunnetDesktop

Page {
    required property XunnetManager manager

    ColumnLayout {
        anchors.centerIn: parent
        spacing: 20

        Label {
            text: manager.connected ? qsTr("Connected") : qsTr("Disconnected")
            font.pixelSize: 24
            Layout.alignment: Qt.AlignHCenter
        }

        Button {
            text: manager.connected ? qsTr("Disconnect") : qsTr("Connect")
            Layout.alignment: Qt.AlignHCenter
            onClicked: manager.connected ? manager.stopVpn() : manager.startVpn({})
        }
    }
}
