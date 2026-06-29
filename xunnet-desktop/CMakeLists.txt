cmake_minimum_required(VERSION 3.20)  
  
project(XunnetDesktop VERSION 1.5.5 LANGUAGES CXX)  
  
set(CMAKE_CXX_STANDARD 17)  
set(CMAKE_CXX_STANDARD_REQUIRED ON)  
set(CMAKE_AUTOMOC ON)  
set(CMAKE_AUTORCC ON)  
set(CMAKE_AUTOUIC ON)  
  
find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets Network Qml Quick QuickControls2)  
  
set(SOURCES src/main.cpp src/core/XunnetManager.cpp src/core/ProfileManager.cpp src/core/SubscriptionManager.cpp src/core/FederationManager.cpp src/core/SystemProxy.cpp src/tray/TrayIcon.cpp src/cli/CliParser.cpp)  
  
set(HEADERS src/core/XunnetManager.h src/core/ProfileManager.h src/core/SubscriptionManager.h src/core/FederationManager.h src/core/SystemProxy.h src/tray/TrayIcon.h src/cli/CliParser.h)  
  
set(QML_FILES ui/main.qml ui/Dashboard.qml ui/Proxies.qml ui/Federation.qml ui/Settings.qml)  
  
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)  
  
qt_add_executable(XunnetDesktopApp ${SOURCES} ${HEADERS})  
  
if(WIN32)  
    set_target_properties(XunnetDesktopApp PROPERTIES WIN32_EXECUTABLE TRUE)  
endif()  
  
qt_generate_deploy_app_script(TARGET_NAME XunnetDesktopApp OUTPUT_SCRIPT deploy_script NO_UNSUPPORTED_PLATFORM_ERROR)  
  
include(GNUInstallDirs)  
install(TARGETS XunnetDesktopApp RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})  
install(SCRIPT ${deploy_script})  
install(DIRECTORY resources DESTINATION ${CMAKE_INSTALL_DATADIR}/xunnet-desktop)  
  
set(CPACK_GENERATOR NSIS;DEB;RPM;TGZ)  
set(CPACK_PACKAGE_VERSION ${PROJECT_VERSION})  
include(CPack)  
