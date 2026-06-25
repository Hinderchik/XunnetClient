#pragma once

#include <QObject>
#include <QString>
#include <QList>
#include <QJsonObject>

struct Profile {
    QString id;
    QString name;
    QString protocol;
    QString address;
    int port;
    QJsonObject params;
    QStringList tags;
    int priority;
    bool enabled;
    QString source;
};

class ProfileManager : public QObject {
    Q_OBJECT
public:
    explicit ProfileManager(QObject *parent = nullptr);

    QList<Profile> getAll() const;
    Profile getById(const QString &id) const;
    bool add(const Profile &profile);
    bool remove(const QString &id);
    bool update(const Profile &profile);
    QList<Profile> importFromLink(const QString &link);
    QList<Profile> importFromFile(const QString &path);
    QString exportToLink(const Profile &profile) const;

signals:
    void profilesChanged();
};
