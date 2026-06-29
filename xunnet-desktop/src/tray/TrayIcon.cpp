#include "TrayIcon.h"
#include <QApplication>
#include <QAction>
#include <QClipboard>
#include <QGuiApplication>
#include <QMessageBox>
#include <QStyle>

TrayIcon::TrayIcon(QObject *parent) : QObject(parent) {
    m_trayIcon = new QSystemTrayIcon(this);
    m_menu = new QMenu();

    // Load icons from Qt resources (PNG for cross-platform compat)
    m_iconDefault = QIcon(":/XunnetDesktop/resources/icons/app.png");
    m_iconConnected = QIcon(":/XunnetDesktop/resources/icons/app-connected.png");
    m_iconError = QIcon(":/XunnetDesktop/resources/icons/app-error.png");
    m_trayIcon->setIcon(m_iconDefault);

    // Single dynamic action — Connect/Disconnect
    m_toggleAction = new QAction(this);
    m_toggleAction->setText(tr("Connect"));
    connect(m_toggleAction, &QAction::triggered, this, &TrayIcon::toggleRequested);
    m_menu->addAction(m_toggleAction);

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

    // Double-click on tray icon -> show window
    // Qt 6: doubleClicked signal was removed; use activated(reason) instead
    connect(m_trayIcon, &QSystemTrayIcon::activated, this,
            [this](QSystemTrayIcon::ActivationReason reason) {
                if (reason == QSystemTrayIcon::DoubleClick) {
                    emit showWindowRequested();
                }
            });
}

void TrayIcon::show() {
    m_trayIcon->show();
    m_trayIcon->showMessage(tr("Xunnet"), tr("Xunnet Desktop is running in the system tray"),
                             QSystemTrayIcon::Information, 3000);
}

void TrayIcon::setStatus(bool connected, const QString &profileName, bool error) {
    m_trayIcon->setToolTip(QStringLiteral("Xunnet - %1 (%2)")
                               .arg(profileName)
                               .arg(error ? tr("Error")
                                    : connected ? tr("Connected")
                                    : tr("Disconnected")));
    m_trayIcon->setIcon(error ? m_iconError : (connected ? m_iconConnected : m_iconDefault));
    if (m_toggleAction) {
        m_toggleAction->setText(connected ? tr("Disconnect") : tr("Connect"));
    }
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
        "ssh://", "wireguard://", "awg://", "amneziawg://"
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
                             tr("Imported link from clipboard"),
                             QSystemTrayIcon::Information, 3000);
    return text;
}
