#pragma once

#include <QObject>
#include <QString>
#include <QJsonObject>

struct Stats {
    qint64 upload = 0;
    qint64 download = 0;
};

class XunnetManager : public QObject {
    Q_OBJECT
    Q_PROPERTY(bool connected READ connected NOTIFY connectedChanged)

public:
    explicit XunnetManager(QObject *parent = nullptr);

    bool connected() const { return m_connected; }

    Q_INVOKABLE bool startVpn(const QJsonObject &profile);
    Q_INVOKABLE bool stopVpn();
    Q_INVOKABLE Stats getStats() const;
    Q_INVOKABLE bool testLatency(const QString &server);

signals:
    void connectedChanged(bool connected);
    void statsUpdated(const Stats &stats);

private:
    bool m_connected = false;
};
