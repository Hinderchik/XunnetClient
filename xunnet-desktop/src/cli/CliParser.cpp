#include "CliParser.h"

CliParser::Options CliParser::parse(const QStringList &args) {
    Options opts;
    for (int i = 1; i < args.size(); ++i) {
        const QString &arg = args.at(i);
        if (arg == QStringLiteral("--help") || arg == QStringLiteral("-h")) {
            opts.help = true;
        } else if (arg == QStringLiteral("--version") || arg == QStringLiteral("-v")) {
            opts.version = true;
        } else if (arg == QStringLiteral("--connect") || arg == QStringLiteral("-c")) {
            opts.connect = true;
        } else if (arg == QStringLiteral("--disconnect") || arg == QStringLiteral("-d")) {
            opts.disconnect = true;
        } else if (arg == QStringLiteral("--profile") || arg == QStringLiteral("-p")) {
            if (i + 1 < args.size()) opts.profileId = args.at(++i);
        } else if (arg == QStringLiteral("--config") || arg == QStringLiteral("-f")) {
            if (i + 1 < args.size()) opts.configPath = args.at(++i);
        } else if (arg == QStringLiteral("--daemon")) {
            opts.daemon = true;
        }
    }
    return opts;
}

QString CliParser::helpText() {
    return QStringLiteral(
        "Xunnet Desktop CLI\n"
        "Usage: XunnetDesktop [options]\n"
        "Options:\n"
        "  -h, --help          Show this help\n"
        "  -v, --version       Show version\n"
        "  -c, --connect       Connect to profile\n"
        "  -d, --disconnect    Disconnect\n"
        "  -p, --profile ID    Profile ID\n"
        "  -f, --config PATH   Config file path\n"
        "  --daemon            Run in background\n");
}
