#pragma once

#include <QObject>
#include <QString>
#include <QList>
#include "ProfileManager.h"

struct FederatedPanel {
    QString id;
    QString name;
    QString url;
    QString apiKey;
    QString role;
    QString mode;
    QString status;
    int serversCount;
    QStringList tags;
    bool enabled;
};

class FederationManager : public QObject {
    Q_OBJECT
public:
    explicit FederationManager(QObject *parent = nullptr);

    QList<FederatedPanel> getPanels() const;
    bool addPanel(const FederatedPanel &panel);
    bool removePanel(const QString &id);
    QList<Profile> syncPanel(const QString &id);
    QList<Profile> syncAllPanels();
    QString getStatus(const QString &id) const;

signals:
    void panelUpdated(const QString &id);
    void panelError(const QString &id, const QString &error);
};
