#include <QApplication>
#include <QQmlApplicationEngine>
#include "core/XunnetManager.h"

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    app.setApplicationName("Xunnet Desktop");
    app.setOrganizationName("Xunnet");

    qmlRegisterType<XunnetManager>("XunnetDesktop", 1, 0, "XunnetManager");

    QQmlApplicationEngine engine;
    engine.load(QUrl(QStringLiteral("qrc:/XunnetDesktop/ui/main.qml")));

    if (engine.rootObjects().isEmpty()) return -1;
    return app.exec();
}
