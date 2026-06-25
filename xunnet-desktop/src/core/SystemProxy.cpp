#include "SystemProxy.h"

SystemProxy::SystemProxy(QObject *parent) : QObject(parent) {}

bool SystemProxy::setProxy(const QString &host, int port) {
    Q_UNUSED(host)
    Q_UNUSED(port)
    m_set = true;
    return true;
}

bool SystemProxy::clearProxy() {
    m_set = false;
    return true;
}

bool SystemProxy::isProxySet() const {
    return m_set;
}
