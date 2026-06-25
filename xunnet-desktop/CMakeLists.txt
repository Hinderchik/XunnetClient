cmake_minimum_required(VERSION 3.20)
project(XunnetDesktop VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets Network Qml Quick)

set(SOURCES
    src/main.cpp
    src/core/XunnetManager.cpp
    src/core/ProfileManager.cpp
    src/core/SubscriptionManager.cpp
    src/core/FederationManager.cpp
    src/core/SystemProxy.cpp
    src/tray/TrayIcon.cpp
    src/cli/CliParser.cpp
)

set(HEADERS
    src/core/XunnetManager.h
    src/core/ProfileManager.h
    src/core/SubscriptionManager.h
    src/core/FederationManager.h
    src/core/SystemProxy.h
    src/tray/TrayIcon.h
    src/cli/CliParser.h
)

set(QML_FILES
    ui/main.qml
    ui/Dashboard.qml
    ui/Proxies.qml
    ui/Federation.qml
    ui/Settings.qml
)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

qt_add_executable(XunnetDesktopApp ${SOURCES} ${HEADERS})

qt_add_qml_module(XunnetDesktopApp
    URI XunnetDesktop
    VERSION 1.0
    QML_FILES ${QML_FILES}
    RESOURCES resources/icons/app.svg
)

target_link_libraries(XunnetDesktopApp PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Widgets
    Qt6::Network
    Qt6::Qml
    Qt6::Quick
)

install(TARGETS XunnetDesktopApp RUNTIME DESTINATION bin)
