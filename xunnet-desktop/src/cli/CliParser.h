#pragma once

#include <QString>
#include <QStringList>

class CliParser {
public:
    struct Options {
        bool help = false;
        bool version = false;
        bool connect = false;
        bool disconnect = false;
        QString profileId;
        QString configPath;
        bool daemon = false;
    };

    static Options parse(const QStringList &args);
    static QString helpText();
};
