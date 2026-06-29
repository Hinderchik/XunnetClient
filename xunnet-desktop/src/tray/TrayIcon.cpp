#include "TrayIcon.h"
#include <QApplication>
#include <QAction>
#include <QClipboard>
#include <QGuiApplication>
#include <QMessageBox>

TrayIcon::TrayIcon(QObject *parent) : QObject(parent) {
    m_trayIcon = new QSystemTrayIcon(this);
    m_menu = new QMenu();

    QAction *showAction = new QAction(tr("Show"), this);
    connect(showAction, &QAction::triggered, this, &TrayIcon::showWindowRequested);
    m_menu->addAction(showAction);

    m_menu->addSeparator();

    QAction *connectAction = new QAction(tr("Connect"), this);
    connect(connectAction, &QAction::triggered, this, &TrayIcon::connectRequested);
    m_menu->addAction(connectAction);

    QAction *disconnectAction = new QAction(tr("Disconnect"), this);
    connect(disconnectAction, &QAction::triggered, this, &TrayIcon::disconnectRequested);
    m_menu->addAction(disconnectAction);

    m_menu->addSeparator();

    QAction *pasteAction = new QAction(tr("Paste link from clipboard"), this);
    pasteAction->setShortcut(QKeySequence("Ctrl+Shift+V"));
    connect(pasteAction, &QAction::triggered, this, [this]() {
        QString link = pasteFromClipboard();
        if (!link.isEmpty()) {
            emit profileImported(link);
        }
    });
    m_menu->addAction(pasteAction);

    m_menu->addSeparator();

    QAction *quitAction = new QAction(tr("Quit"), this);
    connect(quitAction, &QAction::triggered, this, &TrayIcon::quitRequested);
    m_menu->addAction(quitAction);

    m_trayIcon->setContextMenu(m_menu);

    // Double-click on tray icon → show window
    connect(m_trayIcon, &QSystemTrayIcon::doubleClicked, this, &TrayIcon::showWindowRequested);
}

void TrayIcon::show() {
    m_trayIcon->show();
    m_trayIcon->showMessage(tr("Xunnet"), tr("Xunnet Desktop is running in the system tray"),
                             QSystemTrayIcon::Information, 3000);
}

void TrayIcon::setStatus(bool connected, const QString &profileName) {
    m_trayIcon->setToolTip(QStringLiteral("Xunnet - %1 (%2)")
                               .arg(profileName)
                               .arg(connected ? tr("Connected") : tr("Disconnected")));
    QString iconPath = connected ? ":/icons/app-connected.svg" : ":/icons/app.svg";
    m_trayIcon->setIcon(QIcon(iconPath));
}

QString TrayIcon::pasteFromClipboard() {
    QClipboard *clipboard = QGuiApplication::clipboard();
    if (!clipboard) {
        emit importFailed(tr("Clipboard unavailable"));
        return QString();
    }
    QString text = clipboard->text().trimmed();
    if (text.isEmpty()) {
        emit importFailed(tr("Clipboard is empty"));
        return QString();
    }
    // Basic sanity check — should look like a proxy link
    static const QStringList schemes = {
        "xunnet://", "vless://", "vmess://", "trojan://", "ss://",
        "hysteria://", "hysteria2://", "hy2://", "tuic://",
        "ssh://", "wireguard://"
    };
    bool ok = false;
    for (const auto &s : schemes) {
        if (text.startsWith(s, Qt::CaseInsensitive)) { ok = true; break; }
    }
    if (!ok) {
        emit importFailed(tr("Clipboard doesn't contain a supported proxy link"));
        return QString();
    }
    m_trayIcon->showMessage(tr("Xunnet"),
                             tr("Imported link from clipboard. Opening add dialog…"),
                             QSystemTrayIcon::Information, 3000);
    return text;
}

