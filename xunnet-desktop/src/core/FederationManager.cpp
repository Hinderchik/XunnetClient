#include "FederationManager.h"

FederationManager::FederationManager(QObject *parent) : QObject(parent) {}

QList<FederatedPanel> FederationManager::getPanels() const {
    return {};
}

bool FederationManager::addPanel(const FederatedPanel &panel) {
    Q_UNUSED(panel)
    emit panelUpdated(panel.id);
    return true;
}

bool FederationManager::removePanel(const QString &id) {
    Q_UNUSED(id)
    return true;
}

QList<Profile> FederationManager::syncPanel(const QString &id) {
    Q_UNUSED(id)
    return {};
}

QList<Profile> FederationManager::syncAllPanels() {
    return {};
}

QString FederationManager::getStatus(const QString &id) const {
    Q_UNUSED(id)
    return QStringLiteral("unknown");
}
