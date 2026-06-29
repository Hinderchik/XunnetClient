#pragma once

#include <QObject>
#include <QSystemTrayIcon>
#include <QMenu>
#include <QIcon>

class TrayIcon : public QObject {
    Q_OBJECT
public:
    explicit TrayIcon(QObject *parent = nullptr);

    void show();
    void setStatus(bool connected, const QString &profileName, bool error = false);

signals:
    void showWindowRequested();
    void toggleRequested();
    void quitRequested();
    void profileImported(const QString &link);
    void importFailed(const QString &reason);

private:
    QString pasteFromClipboard();
    QSystemTrayIcon *m_trayIcon;
    QMenu *m_menu;
    QAction *m_toggleAction = nullptr;
    QIcon m_iconDefault;
    QIcon m_iconConnected;
    QIcon m_iconError;
};
