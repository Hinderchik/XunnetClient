#pragma once

#include <QObject>
#include <QString>
#include <QList>
#include "ProfileManager.h"

struct Subscription {
    QString id;
    QString name;
    QString url;
    QString format;
    int updateInterval;
    bool enabled;
    QStringList tags;
    QList<Profile> profiles;
};

class SubscriptionManager : public QObject {
    Q_OBJECT
public:
    explicit SubscriptionManager(QObject *parent = nullptr);

    QList<Subscription> getAll() const;
    bool add(const Subscription &sub);
    bool remove(const QString &id);
    QList<Profile> refresh(const QString &id);
    QList<Profile> refreshAll();
    Subscription aggregate(const QStringList &ids);

signals:
    void subscriptionsChanged();
};
