#include "ProfileManager.h"
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QSaveFile>
#include <QStandardPaths>
#include <QDir>
#include <QRegularExpression>
#include <QUuid>

ProfileManager::ProfileManager(QObject *parent) : QObject(parent) {}

QVariantList ProfileManager::profiles() const {
    QVariantList list;
    for (const auto &p : m_profiles) {
        QVariantMap m;
        m["id"] = p.id;
        m["name"] = p.name;
        m["url"] = p.url;
        m["protocol"] = p.protocol;
        m["address"] = p.address;
        m["port"] = p.port;
        m["params"] = p.params;
        m["latency"] = p.latency;
        m["enabled"] = p.enabled;
        list.append(m);
    }
    return list;
}

QVariantMap ProfileManager::getProfile(const QString &id) const {
    for (const auto &p : m_profiles) {
        if (p.id == id) {
            QVariantMap m;
            m["id"] = p.id;
            m["name"] = p.name;
            m["url"] = p.url;
            m["protocol"] = p.protocol;
            m["address"] = p.address;
            m["port"] = p.port;
            m["params"] = p.params;
            m["latency"] = p.latency;
            m["enabled"] = p.enabled;
            return m;
        }
    }
    return {};
}

bool ProfileManager::addFromLink(const QString &link) {
    Profile p;
    p.id = QUuid::createUuid().toString(QUuid::WithoutBraces);
    p.name = link.section('@', -1).section('#', 0, 0).left(40);
    if (p.name.isEmpty()) p.name = "Imported";
    p.url = link;
    return add(p);
}

bool ProfileManager::addProfile(const QString &name, const QString &url) {
    Profile p;
    p.id = QUuid::createUuid().toString(QUuid::WithoutBraces);
    p.name = name;
    p.url = url;
    return add(p);
}

bool ProfileManager::removeProfile(const QString &id) {
    return remove(id);
}

bool ProfileManager::setEnabled(const QString &id, bool enabled) {
    for (auto &p : m_profiles) {
        if (p.id == id) {
            p.enabled = enabled;
            return update(p);
        }
    }
    return false;
}

bool ProfileManager::setName(const QString &id, const QString &name) {
    for (auto &p : m_profiles) {
        if (p.id == id) {
            p.name = name;
            return update(p);
        }
    }
    return false;
}

QString ProfileManager::exportAll() const {
    QJsonArray arr;
    for (const auto &p : m_profiles) {
        QJsonObject o;
        o["id"] = p.id;
        o["name"] = p.name;
        o["url"] = p.url;
        arr.append(o);
    }
    QJsonDocument doc(arr);
    return QString::fromUtf8(doc.toJson(QJsonDocument::Indented));
}

bool ProfileManager::importAll(const QString &json) {
    QJsonDocument doc = QJsonDocument::fromJson(json.toUtf8());
    if (!doc.isArray()) return false;
    bool ok = true;
    for (const auto &v : doc.array()) {
        const auto o = v.toObject();
        Profile p;
        p.id = o["id"].toString();
        p.name = o["name"].toString();
        p.url = o["url"].toString();
        ok &= add(p);
    }
    return ok;
}

QList<Profile> ProfileManager::getAll() const {
    return m_profiles;
}

Profile ProfileManager::getById(const QString &id) const {
    for (const auto &p : m_profiles) {
        if (p.id == id) return p;
    }
    return {};
}

bool ProfileManager::add(const Profile &profile) {
    m_profiles.append(profile);
    emit profilesChanged();
    return true;
}

bool ProfileManager::update(const Profile &profile) {
    for (auto &p : m_profiles) {
        if (p.id == profile.id) {
            p = profile;
            emit profilesChanged();
            return true;
        }
    }
    return false;
}

bool ProfileManager::remove(const QString &id) {
    for (int i = 0; i < m_profiles.size(); ++i) {
        if (m_profiles[i].id == id) {
            m_profiles.removeAt(i);
            emit profilesChanged();
            return true;
        }
    }
    return false;
}

QList<Profile> ProfileManager::importFromLink(const QString &link) {
    QList<Profile> result;
    if (addFromLink(link)) {
        result.append(m_profiles.last());
    }
    return result;
}

QList<Profile> ProfileManager::importFromFile(const QString &path) {
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly | QIODevice::Text)) return {};
    QString content = QString::fromUtf8(f.readAll());
    f.close();
    QList<Profile> result;
    for (const QString &line : content.split(QRegularExpression("[\\r\\n]+"), Qt::SkipEmptyParts)) {
        QString trimmed = line.trimmed();
        if (trimmed.isEmpty() || trimmed.startsWith('#')) continue;
        if (addFromLink(trimmed)) {
            result.append(m_profiles.last());
        }
    }
    return result;
}

QString ProfileManager::exportToLink(const Profile &profile) const {
    return profile.url;
}

void ProfileManager::refreshLatencies() {
}

void ProfileManager::reload() {
    QFile f(storagePath());
    if (!f.open(QIODevice::ReadOnly)) return;
    auto doc = QJsonDocument::fromJson(f.readAll());
    f.close();
    if (!doc.isArray()) return;
    m_profiles.clear();
    for (const auto &v : doc.array()) {
        const auto o = v.toObject();
        Profile p;
        p.id = o["id"].toString();
        p.name = o["name"].toString();
        p.url = o["url"].toString();
        p.protocol = o["protocol"].toString();
        p.address = o["address"].toString();
        p.port = o["port"].toInt();
        p.params = o["params"].toString();
        p.latency = o["latency"].toInt();
        p.enabled = o["enabled"].toBool();
        m_profiles.append(p);
    }
    emit profilesChanged();
}

void ProfileManager::save() {
    QSaveFile f(storagePath());
    if (!f.open(QIODevice::WriteOnly)) return;
    QJsonArray arr;
    for (const auto &p : m_profiles) {
        QJsonObject o;
        o["id"] = p.id;
        o["name"] = p.name;
        o["url"] = p.url;
        o["protocol"] = p.protocol;
        o["address"] = p.address;
        o["port"] = p.port;
        o["params"] = p.params;
        o["latency"] = p.latency;
        o["enabled"] = p.enabled;
        arr.append(o);
    }
    f.write(QJsonDocument(arr).toJson(QJsonDocument::Indented));
    f.commit();
}

QString ProfileManager::storagePath() const {
    QString dir = QStandardPaths::writableLocation(QStandardPaths::AppConfigLocation);
    QDir().mkpath(dir);
    return dir + "/profiles.json";
}

Profile ProfileManager::parseLink(const QString &link) const {
    Profile p;
    p.url = link;
    return p;
}
