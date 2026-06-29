#include "XunnetManager.h"
#include <QProcess>
#include <QDebug>
#include <QVariantMap>

XunnetManager::XunnetManager(QObject *parent) : QObject(parent) {}

QVariantMap XunnetManager::activeProfile() const {
    QVariantMap m;
    m["id"] = m_activeProfileId;
    return m;
}

bool XunnetManager::startVpn(const QString &url) {
    Q_UNUSED(url)
    qDebug() << "Starting sing-box";
    m_connected = true;
    emit connectedChanged(true);
    return true;
}

bool XunnetManager::startVpn(const QVariantMap &profile) {
    if (profile.isEmpty()) return false;
    m_activeProfileId = profile.value("id").toString();
    emit activeProfileIdChanged();
    qDebug() << "Starting sing-box for profile" << profile.value("name").toString();
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
