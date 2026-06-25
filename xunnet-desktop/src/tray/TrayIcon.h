#pragma once

#include <QObject>
#include <QSystemTrayIcon>
#include <QMenu>

class TrayIcon : public QObject {
    Q_OBJECT
public:
    explicit TrayIcon(QObject *parent = nullptr);

    void show();
    void setStatus(bool connected, const QString &profileName);

signals:
    void showWindowRequested();
    void connectRequested();
    void disconnectRequested();
    void quitRequested();

private:
    QSystemTrayIcon *m_trayIcon;
    QMenu *m_menu;
};
