#include "TrayIcon.h"
#include <QApplication>
#include <QAction>

TrayIcon::TrayIcon(QObject *parent) : QObject(parent) {
    m_trayIcon = new QSystemTrayIcon(this);
    m_menu = new QMenu();

    QAction *showAction = new QAction(tr("Show"), this);
    connect(showAction, &QAction::triggered, this, &TrayIcon::showWindowRequested);
    m_menu->addAction(showAction);

    QAction *connectAction = new QAction(tr("Connect"), this);
    connect(connectAction, &QAction::triggered, this, &TrayIcon::connectRequested);
    m_menu->addAction(connectAction);

    QAction *disconnectAction = new QAction(tr("Disconnect"), this);
    connect(disconnectAction, &QAction::triggered, this, &TrayIcon::disconnectRequested);
    m_menu->addAction(disconnectAction);

    m_menu->addSeparator();

    QAction *quitAction = new QAction(tr("Quit"), this);
    connect(quitAction, &QAction::triggered, this, &TrayIcon::quitRequested);
    m_menu->addAction(quitAction);

    m_trayIcon->setContextMenu(m_menu);
}

void TrayIcon::show() {
    m_trayIcon->show();
}

void TrayIcon::setStatus(bool connected, const QString &profileName) {
    m_trayIcon->setToolTip(QStringLiteral("Xunnet - %1 (%2)")
                               .arg(profileName)
                               .arg(connected ? tr("Connected") : tr("Disconnected")));
}
