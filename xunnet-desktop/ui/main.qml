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

    XunnetManager { id: manager }

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
                color: manager.connected ? "#22c55e" : "#475569"
                Layout.alignment: Qt.AlignVCenter
            }

            Label {
                text: manager.connected ? qsTr("Connected") : qsTr("Disconnected")
                color: "#e2e8f0"
                font.pixelSize: 14
                Layout.fillWidth: true
            }

            Button {
                text: manager.connected ? qsTr("Disconnect") : qsTr("Connect")
                flat: true
                onClicked: manager.connected ? manager.stopVpn() : manager.startVpn("")
                contentItem: Text {
                    text: parent.text
                    color: manager.connected ? "#f87171" : "#22c55e"
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
                model: manager.profiles
                delegate: ItemDelegate {
                    width: ListView.view.width
                    height: 48
                    onClicked: manager.startVpn(modelData.url)
                    contentItem: RowLayout {
                        spacing: 12
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 16

                        Rectangle {
                            width: 8; height: 8; radius: 4
                            color: modelData.url === manager.activeUrl ? "#22c55e" : "#475569"
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
                    visible: manager.profiles.length === 0
                }
            }
        }

        // Settings — minimal toggles (inlined directly)
        ScrollView {
            ColumnLayout {
                width: parent.width
                spacing: 0

                // Auto-connect toggle row
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
                        Switch {
                            checked: manager.autoConnect
                            onToggled: manager.autoConnect = checked
                        }
                    }
                }

                // Kill switch toggle row
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
                        Switch {
                            checked: manager.killSwitch
                            onToggled: manager.killSwitch = checked
                        }
                    }
                }

                // Block QUIC toggle row
                Rectangle {
                    Layout.fillWidth: true
                    height: 48
                    color: "transparent"
                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 12
                        Label {
                            text: qsTr("Block QUIC (UDP/443)")
                            color: "#e2e8f0"
                            font.pixelSize: 14
                            Layout.fillWidth: true
                        }
                        Switch {
                            checked: manager.blockQuic
                            onToggled: manager.blockQuic = checked
                        }
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: "#1e293b" }

                Label {
                    text: qsTr("Split tunneling") + ": " + (manager.splitPreset || qsTr("default"))
                    color: "#94a3b8"
                    font.pixelSize: 12
                    Layout.leftMargin: 16
                    Layout.topMargin: 16
                    Layout.bottomMargin: 8
                }
                Repeater {
                    model: ["default", "russia", "streaming", "gaming"]
                    delegate: ItemDelegate {
                        text: modelData
                        width: parent.width
                        height: 36
                        onClicked: manager.splitPreset = modelData
                        contentItem: Label {
                            text: modelData
                            color: manager.splitPreset === modelData ? "#22c55e" : "#cbd5e1"
                            font.pixelSize: 14
                            leftPadding: 24
                            verticalAlignment: Text.AlignVCenter
                            height: 36
                        }
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
