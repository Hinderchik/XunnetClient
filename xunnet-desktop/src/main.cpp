#include <QApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickWindow>
#include <QIcon>
#include <QSystemTrayIcon>
#include <QObject>
#include <QCommandLineParser>
#include <QCommandLineOption>
#include <QTimer>
#include <QWindow>
#include <QStandardPaths>
#include <QDir>
#include <QLoggingCategory>
#include "core/XunnetManager.h"
#include "core/ProfileManager.h"
#include "tray/TrayIcon.h"

int main(int argc, char *argv[]) {
    QCoreApplication::setOrganizationName("Xunnet");
    QCoreApplication::setApplicationName("Xunnet Desktop");
    QCoreApplication::setApplicationVersion("1.5.5");

    QApplication app(argc, argv);
    QApplication::setQuitOnLastWindowClosed(false);   // stay running in tray

    // App-wide window icon
    QIcon appIcon(":/XunnetDesktop/resources/icons/app.png");
    app.setWindowIcon(appIcon);

    qInfo() << "Xunnet Desktop starting...";
    qInfo() << "Config dir:" << QStandardPaths::writableLocation(QStandardPaths::AppConfigLocation);

    // CLI args
    QCommandLineParser parser;
    parser.setApplicationDescription("Xunnet Desktop - VPN client");
    parser.addHelpOption();
    parser.addVersionOption();
    QCommandLineOption connectOpt("connect", "Connect to a profile link", "url");
    QCommandLineOption disconnectOpt("disconnect", "Disconnect from VPN");
    QCommandLineOption quitOpt("quit", "Quit running instance");
    QCommandLineOption hiddenOpt("hidden", "Start hidden in tray");
    parser.addOption(connectOpt);
    parser.addOption(disconnectOpt);
    parser.addOption(quitOpt);
    parser.addOption(hiddenOpt);
    parser.addPositionalArgument("link", "Optional xunnet:// link to import and connect", "[link]");
    parser.process(app);
    QString connectUrl = parser.value(connectOpt);
    bool doDisconnect = parser.isSet(disconnectOpt);
    bool doQuit = parser.isSet(quitOpt);
    bool startHidden = parser.isSet(hiddenOpt);
    QStringList positional = parser.positionalArguments();

    // Backend managers — use heap so they survive app.exec()
    auto *xunnet = new XunnetManager(&app);
    auto *profiles = new ProfileManager(&app);
    profiles->reload();

    qInfo() << "Loaded" << profiles->profiles().size() << "profiles";

    // Tray icon — skip if not supported on this system
    auto *tray = new TrayIcon(&app);
    QObject::connect(tray, &TrayIcon::toggleRequested, xunnet, [xunnet, profiles]() {
        if (xunnet->connected()) {
            xunnet->stopVpn();
        } else {
            QVariantMap active = xunnet->activeProfile();
            if (active.isEmpty()) {
                auto all = profiles->getAll();
                if (!all.isEmpty()) active = profiles->getProfile(all.first().id);
            }
            if (!active.isEmpty()) xunnet->startVpn(active);
        }
    });
    QObject::connect(tray, &TrayIcon::profileImported, profiles, [profiles](const QString &link) {
        profiles->addFromLink(link);
    });
    QObject::connect(xunnet, &XunnetManager::connectedChanged, tray, [tray, profiles, xunnet](bool c) {
        QVariantMap p = profiles->getProfile(xunnet->activeProfileId());
        tray->setStatus(c, p.value("name").toString());
    });

    // QML engine — expose managers as context properties (singleton-like)
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("xunnet", xunnet);
    engine.rootContext()->setContextProperty("profilesModel", profiles);

    engine.load(QUrl(QStringLiteral("qrc:/XunnetDesktop/ui/main.qml")));
    if (engine.rootObjects().isEmpty()) {
        qCritical() << "Failed to load QML - check Qt platform plugin and QML resources";
        return -1;
    }
    auto *window = qobject_cast<QWindow *>(engine.rootObjects().first());
    if (!window) {
        qCritical() << "Root QML object is not a Window";
        return -1;
    }
    qInfo() << "QML loaded OK";

    if (QSystemTrayIcon::isSystemTrayAvailable()) {
        tray->show();
        qInfo() << "Tray icon shown";
    } else {
        qWarning() << "System tray not available";
    }

    if (!startHidden && !doQuit && positional.isEmpty()) {
        window->show();
        qInfo() << "Window shown";
    } else {
        qInfo() << "Starting hidden (startHidden=" << startHidden
                 << ", doQuit=" << doQuit
                 << ", positional=" << positional.length() << ")";
    }

    // Handle CLI actions
    if (doQuit) {
        QTimer::singleShot(0, &app, &QCoreApplication::quit);
        return 0;
    }
    if (doDisconnect) {
        QTimer::singleShot(0, xunnet, [xunnet]() { xunnet->stopVpn(); });
    }
    if (!connectUrl.isEmpty()) {
        profiles->addFromLink(connectUrl);
        QTimer::singleShot(500, xunnet, [xunnet, connectUrl]() { xunnet->startVpn(connectUrl); });
    } else if (!positional.isEmpty()) {
        QString link = positional.first();
        profiles->addFromLink(link);
        QTimer::singleShot(500, xunnet, [xunnet, profiles, link]() {
            auto all = profiles->getAll();
            if (!all.isEmpty()) xunnet->startVpn(profiles->getProfile(all.last().id));
        });
    }

    // Show window on tray double-click
    QObject::connect(tray, &TrayIcon::showWindowRequested, window, [window]() {
        window->show();
        window->raise();
        window->requestActivate();
    });

    return app.exec();
}
