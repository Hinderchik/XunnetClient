#pragma once

#include <QObject>
#include <QString>

class SystemProxy : public QObject {
    Q_OBJECT
public:
    explicit SystemProxy(QObject *parent = nullptr);

    bool setProxy(const QString &host, int port);
    bool clearProxy();
    bool isProxySet() const;

private:
    bool m_set = false;
};
