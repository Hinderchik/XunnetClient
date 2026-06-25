#include "SubscriptionManager.h"

SubscriptionManager::SubscriptionManager(QObject *parent) : QObject(parent) {}

QList<Subscription> SubscriptionManager::getAll() const {
    return {};
}

bool SubscriptionManager::add(const Subscription &sub) {
    Q_UNUSED(sub)
    emit subscriptionsChanged();
    return true;
}

bool SubscriptionManager::remove(const QString &id) {
    Q_UNUSED(id)
    emit subscriptionsChanged();
    return true;
}

QList<Profile> SubscriptionManager::refresh(const QString &id) {
    Q_UNUSED(id)
    return {};
}

QList<Profile> SubscriptionManager::refreshAll() {
    return {};
}

Subscription SubscriptionManager::aggregate(const QStringList &ids) {
    Q_UNUSED(ids)
    return Subscription{};
}
