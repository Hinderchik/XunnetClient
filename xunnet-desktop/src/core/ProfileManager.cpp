#include "ProfileManager.h"

ProfileManager::ProfileManager(QObject *parent) : QObject(parent) {}

QList<Profile> ProfileManager::getAll() const {
    return {};
}

Profile ProfileManager::getById(const QString &id) const {
    Q_UNUSED(id)
    return Profile{};
}

bool ProfileManager::add(const Profile &profile) {
    Q_UNUSED(profile)
    emit profilesChanged();
    return true;
}

bool ProfileManager::remove(const QString &id) {
    Q_UNUSED(id)
    emit profilesChanged();
    return true;
}

bool ProfileManager::update(const Profile &profile) {
    Q_UNUSED(profile)
    emit profilesChanged();
    return true;
}

QList<Profile> ProfileManager::importFromLink(const QString &link) {
    Q_UNUSED(link)
    return {};
}

QList<Profile> ProfileManager::importFromFile(const QString &path) {
    Q_UNUSED(path)
    return {};
}

QString ProfileManager::exportToLink(const Profile &profile) const {
    Q_UNUSED(profile)
    return QString();
}
