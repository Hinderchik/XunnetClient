#pragma once

#include <QObject>
#include <QString>
#include <QVariantMap>

class XunnetManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(bool connected READ connected NOTIFY connectedChanged)
    Q_PROPERTY(QString activeProfileId READ activeProfileId NOTIFY activeProfileIdChanged)
    Q_PROPERTY(QVariantMap activeProfile READ activeProfile NOTIFY activeProfileIdChanged)

public:
    explicit XunnetManager(QObject *parent = nullptr);

    bool connected() const { return m_connected; }
    QString activeProfileId() const { return m_activeProfileId; }
    QVariantMap activeProfile() const;

    Q_INVOKABLE bool startVpn(const QString &url);
    Q_INVOKABLE bool startVpn(const QVariantMap &profile);
    Q_INVOKABLE bool stopVpn();

signals:
    void connectedChanged(bool connected);
    void activeProfileIdChanged();
    void statsUpdated(qulonglong up, qulonglong down);
    void errorOccurred(const QString &message);

private:
    bool m_connected = false;
    QString m_activeProfileId;
};
