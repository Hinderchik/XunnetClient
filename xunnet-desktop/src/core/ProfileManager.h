#pragma once

#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

class ProfileManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantList profiles READ profiles NOTIFY profilesChanged)

public:
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
