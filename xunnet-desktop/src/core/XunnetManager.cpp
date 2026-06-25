#include "XunnetManager.h"
#include <QProcess>
#include <QDebug>

XunnetManager::XunnetManager(QObject *parent) : QObject(parent) {}

bool XunnetManager::startVpn(const QJsonObject &profile) {
    Q_UNUSED(profile)
    qDebug() << "Starting sing-box";
    m_connected = true;
    emit connectedChanged(true);
    return true;
}

bool XunnetManager::stopVpn() {
    qDebug() << "Stopping sing-box";
    m_connected = false;
    emit connectedChanged(false);
    return true;
}

Stats XunnetManager::getStats() const {
    return Stats{1024, 2048};
}

bool XunnetManager::testLatency(const QString &server) {
    Q_UNUSED(server)
    return true;
}
