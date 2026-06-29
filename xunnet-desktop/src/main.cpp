#include <QApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QIcon>
#include <QSystemTrayIcon>
#include <QObject>
#include <QCommandLineParser>
#include <QCommandLineOption>
#include <QTimer>
#include <QWindow>
#include "core/XunnetManager.h"
#include "core/ProfileManager.h"
#include "tray/TrayIcon.h"

int main(int argc, char *argv[]) {
    // Set Windows application ID so the taskbar groups correctly
    QApplication::setApplicationDisplayName("Xunnet Desktop");
    QApplication::setOrganizationName("Xunnet");
    QApplication::setApplicationName("Xunnet Desktop");
    QApplication::setApplicationVersion("1.5.0");

    QApplication app(argc, argv);
    app.setQuitOnLastWindowClosed(false);   // stay running in tray

    // App-wide window/tray icon
    QIcon appIcon(":/XunnetDesktop/resources/icons/app.png");
    app.setWindowIcon(appIcon);

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
    bool startHidden = parser.isSet(hiddenOpt) || parser.positionalArguments().isEmpty() == false;
    QStringList positional = parser.positionalArguments();

    // Backend managers
    XunnetManager xunnet;
    ProfileManager profiles;

    // Tray icon
    TrayIcon tray;
    QObject::connect(&tray, &TrayIcon::toggleRequested, &xunnet, [&]() {
        if (xunnet.connected()) xunnet.stopVpn();
        else if (xunnet.activeProfile().isValid()) xunnet.startVpn(xunnet.activeProfile());
    });
    QObject::connect(&tray, &TrayIcon::profileImported, &profiles, [&](const QString &link) {
        profiles.addFromLink(link);
    });
    QObject::connect(&xunnet, &XunnetManager::connectedChanged, &tray, [&](bool c) {
        tray.setStatus(c, profiles.getProfile(xunnet.activeProfileId()).value("name").toString());
    });

    // QML engine + types
    QQmlApplicationEngine engine;
    qmlRegisterType<XunnetManager>("XunnetDesktop", 1, 0, "XunnetManager");
    qmlRegisterType<ProfileManager>("XunnetDesktop", 1, 0, "ProfileManager");

    engine.rootContext()->setContextProperty("profilesModel", &profiles);

    engine.load(QUrl(QStringLiteral("qrc:/XunnetDesktop/ui/main.qml")));
    if (engine.rootObjects().isEmpty()) {
        qCritical() << "Failed to load QML";
        return -1;
    }
    auto *window = qobject_cast<QWindow *>(engine.rootObjects().first());
    if (!window) {
        qCritical() << "Root QML object is not a Window";
        return -1;
    }

    if (!startHidden && !doQuit) {
        window->show();
    }
    tray.show();

    // Handle CLI actions
    if (doQuit) {
        QTimer::singleShot(0, &app, &QCoreApplication::quit);
        return 0;
    }
    if (doDisconnect) {
        QTimer::singleShot(0, &xunnet, [&] { xunnet.stopVpn(); });
    }
    if (!connectUrl.isEmpty()) {
        profiles.addFromLink(connectUrl);
        QTimer::singleShot(500, &xunnet, [&] { xunnet.startVpn(connectUrl); });
    } else if (!positional.isEmpty()) {
        QString link = positional.first();
        profiles.addFromLink(link);
    }

    // Show window on tray double-click
    QObject::connect(&tray, &TrayIcon::showWindowRequested, window, [window]() {
        window->show();
        window->raise();
        window->requestActivate();
    });

    return app.exec();
}
