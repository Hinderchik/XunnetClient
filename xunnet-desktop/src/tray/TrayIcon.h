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
    void toggleRequested();         // user clicked the dynamic Connect/Disconnect item
    void quitRequested();
    void profileImported(const QString &link);
    void importFailed(const QString &reason);

private:
    QString pasteFromClipboard();
    QSystemTrayIcon *m_trayIcon;
    QMenu *m_menu;
    QAction *m_toggleAction = nullptr;
};
