cmake_minimum_required(VERSION 3.20)
project(XunnetDesktop VERSION 1.5.0 LANGUAGES CXX)

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

# Windows: build as GUI app (no console window pop-up)
if(WIN32)
    set_target_properties(XunnetDesktopApp PROPERTIES WIN32_EXECUTABLE TRUE)
endif()

qt_add_qml_module(XunnetDesktopApp
    URI XunnetDesktop
    VERSION 1.0
    QML_FILES ${QML_FILES}
    RESOURCES
        resources/icons/app.svg
        resources/icons/app.png
        resources/icons/app-connected.png
        resources/icons/app-error.png
        resources/icons/app_16.png
        resources/icons/app_24.png
        resources/icons/app_32.png
        resources/icons/app_48.png
        resources/icons/app_64.png
        resources/icons/app_128.png
        resources/icons/app_256.png
        resources/icons/app-connected_16.png
        resources/icons/app-connected_24.png
        resources/icons/app-connected_32.png
        resources/icons/app-connected_48.png
        resources/icons/app-connected_64.png
        resources/icons/app-connected_128.png
        resources/icons/app-connected_256.png
        resources/icons/app-error_16.png
        resources/icons/app-error_24.png
        resources/icons/app-error_32.png
        resources/icons/app-error_48.png
        resources/icons/app-error_64.png
        resources/icons/app-error_128.png
        resources/icons/app-error_256.png
        resources/icons/app.ico
)

target_link_libraries(XunnetDesktopApp PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Widgets
    Qt6::Network
    Qt6::Qml
    Qt6::Quick
)

# --- Install + CPack installer ---
include(GNUInstallDirs)
install(TARGETS XunnetDesktopApp RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})
install(DIRECTORY resources DESTINATION ${CMAKE_INSTALL_DATADIR}/xunnet-desktop)

set(CPACK_GENERATOR "NSIS;IFW;DEB;RPM;TGZ")
set(CPACK_PACKAGE_NAME "xunnet-desktop")
set(CPACK_PACKAGE_VENDOR "Xunnet")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Xunnet Desktop - VPN client")
set(CPACK_PACKAGE_DESCRIPTION "Cross-platform desktop VPN client for the Xunnet network.")
set(CPACK_PACKAGE_VERSION "${PROJECT_VERSION}")
set(CPACK_PACKAGE_INSTALL_DIRECTORY "Xunnet")
set(CPACK_PACKAGE_HOMEPAGE_URL "https://github.com/Hinderchik/XunnetClient")
set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/../LICENSE")
set(CPACK_NSIS_DISPLAY_NAME "Xunnet Desktop ${PROJECT_VERSION}")
set(CPACK_NSIS_PACKAGE_NAME "Xunnet Desktop")
set(CPACK_NSIS_INSTALL_ROOT "$PROGRAMFILES64\\Xunnet")
set(CPACK_NSIS_CREATE_ICONS "ONE")
set(CPACK_NSIS_MUI_ICON "${CMAKE_CURRENT_SOURCE_DIR}/resources/icons/app.svg")
set(CPACK_NSIS_MUI_UNI_CODE "")
set(CPACK_PACKAGE_ICON "${CMAKE_CURRENT_SOURCE_DIR}/resources/icons/app.svg")
if(WIN32)
    set(CPACK_GENERATOR "NSIS")
    set(CPACK_NSIS_EXECUTABLES_DIRECTORY "bin")
    set(CPACK_NSIS_INSTALLED_ICON_NAME "bin\\\\XunnetDesktopApp.exe")
    set(CPACK_NSIS_CREATE_ICONS_EXTRA
        "CreateShortcut '$SMPROGRAMS\\\\$STARTMENU_FOLDER\\\\Xunnet Desktop.lnk' '$INSTDIR\\\\bin\\\\XunnetDesktopApp.exe'")
endif()
include(CPack)
