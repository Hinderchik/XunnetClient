#pragma once

#include <QObject>
#include <QSystemTrayIcon>
#include <QMenu>
#include <QString>

class TrayIcon : public QObject {
    Q_OBJECT
public:
    explicit TrayIcon(QObject *parent = nullptr);

    void show();
    void setStatus(bool connected, const QString &profileName);

    // Try to import a profile from the system clipboard.
    // Returns the parsed link on success, empty string on failure.
    Q_INVOKABLE QString pasteFromClipboard();

signals:
    void showWindowRequested();
    void connectRequested();
    void disconnectRequested();
    void quitRequested();
    void profileImported(const QString &link);
    void importFailed(const QString &error);

private:
    QSystemTrayIcon *m_trayIcon;
    QMenu *m_menu;
};

