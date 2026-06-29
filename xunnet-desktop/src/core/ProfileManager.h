#pragma once

#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

/**
 * Profile data shared across managers (Profile/Subscription/Federation).
 * Defined at namespace scope so it can be referenced from other headers.
 */
struct Profile {
    QString id;
    QString name;
    QString url;       // the xunnet:// or other scheme link
    QString protocol;  // xunnet, vless, vmess, etc.
    QString address;
    int port = 0;
    QString params;    // base params JSON
    int latency = -1;  // ms; -1 = unknown
    bool enabled = true;
};

Q_DECLARE_METATYPE(Profile)

class ProfileManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantList profiles READ profiles NOTIFY profilesChanged)

public:
    explicit ProfileManager(QObject *parent = nullptr);

    QVariantList profiles() const;
    Q_INVOKABLE QVariantMap getProfile(const QString &id) const;
    Q_INVOKABLE bool addFromLink(const QString &link);
    Q_INVOKABLE bool addProfile(const QString &name, const QString &url);
    Q_INVOKABLE bool removeProfile(const QString &id);
    Q_INVOKABLE bool setEnabled(const QString &id, bool enabled);
    Q_INVOKABLE bool setName(const QString &id, const QString &name);
    Q_INVOKABLE QString exportAll() const;
    Q_INVOKABLE bool importAll(const QString &json);

    // C++-side API used by SubscriptionManager/FederationManager
    QList<Profile> getAll() const;
    Profile getById(const QString &id) const;
    bool add(const Profile &profile);
    bool update(const Profile &profile);
    bool remove(const QString &id);
    QList<Profile> importFromLink(const QString &link);
    QList<Profile> importFromFile(const QString &path);
    QString exportToLink(const Profile &profile) const;

    // Refresh latency for all profiles in background
    Q_INVOKABLE void refreshLatencies();

    // Persist
    Q_INVOKABLE void reload();
    Q_INVOKABLE void save();

signals:
    void profilesChanged();

private:
    QList<Profile> m_profiles;
    QString storagePath() const;
    Profile parseLink(const QString &link) const;
};
