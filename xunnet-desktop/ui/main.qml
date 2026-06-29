import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import XunnetDesktop

ApplicationWindow {
    visible: true
    width: 480
    height: 640
    minimumWidth: 360
    minimumHeight: 480
    title: "Xunnet"
    color: "#0f1115"

    // Managers are exposed from C++ as context properties ("xunnet", "profilesModel")
    // So we reference them directly here. No need to instantiate.

    // Single primary action bar at the top
    header: Rectangle {
        height: 64
        color: "#0f1115"

        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 20
            anchors.rightMargin: 12
            spacing: 12

            Rectangle {
                width: 12; height: 12; radius: 6
                color: xunnet.connected ? "#22c55e" : "#475569"
                Layout.alignment: Qt.AlignVCenter
            }

            Label {
                text: xunnet.connected ? qsTr("Connected") : qsTr("Disconnected")
                color: "#e2e8f0"
                font.pixelSize: 14
                Layout.fillWidth: true
            }

            Button {
                text: xunnet.connected ? qsTr("Disconnect") : qsTr("Connect")
                flat: true
                onClicked: xunnet.connected ? xunnet.stopVpn() : xunnet.startVpn("")
                contentItem: Text {
                    text: parent.text
                    color: xunnet.connected ? "#f87171" : "#22c55e"
                    font.pixelSize: 13
                    font.weight: Font.DemiBold
                }
            }
        }
    }

    // Single content area — profile list + settings
    StackLayout {
        anchors.fill: parent
        currentIndex: 0

        // Profile list
        Item {
            ListView {
                anchors.fill: parent
                anchors.margins: 8
                spacing: 2
                clip: true
                model: profilesModel.profiles
                delegate: ItemDelegate {
                    width: ListView.view.width
                    height: 48
                    onClicked: xunnet.startVpn(modelData.url)
                    contentItem: RowLayout {
                        spacing: 12
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 16

                        Rectangle {
                            width: 8; height: 8; radius: 4
                            color: modelData.url === xunnet.activeProfileId ? "#22c55e" : "#475569"
                        }

                        Label {
                            text: modelData.name
                            color: "#e2e8f0"
                            font.pixelSize: 14
                            Layout.fillWidth: true
                            elide: Text.ElideRight
                        }

                        Label {
                            text: modelData.latency + " ms"
                            color: "#64748b"
                            font.pixelSize: 12
                            visible: modelData.latency > 0
                        }
                    }
                }

                Label {
                    anchors.centerIn: parent
                    text: qsTr("Paste a link to get started")
                    color: "#64748b"
                    font.pixelSize: 14
                    visible: profilesModel.profiles.length === 0
                }
            }
        }

        // Settings — minimal toggles
        ScrollView {
            ColumnLayout {
                width: parent.width
                spacing: 0

                Rectangle {
                    Layout.fillWidth: true
                    height: 48
                    color: "transparent"
                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 12
                        Label {
                            text: qsTr("Auto-connect on launch")
                            color: "#e2e8f0"
                            font.pixelSize: 14
                            Layout.fillWidth: true
                        }
                        Switch { checked: false }
                    }
                }
                Rectangle {
                    Layout.fillWidth: true
                    height: 48
                    color: "transparent"
                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 12
                        Label {
                            text: qsTr("Kill switch")
                            color: "#e2e8f0"
                            font.pixelSize: 14
                            Layout.fillWidth: true
                        }
                        Switch { checked: true }
                    }
                }
            }
        }
    }

    footer: TabBar {
        TabButton { text: qsTr("Proxies"); width: parent.width / 2 }
        TabButton { text: qsTr("Settings"); width: parent.width / 2 }
    }
}
