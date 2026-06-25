cmake_minimum_required(VERSION 3.22.1)
project("xunnet-core")

set(CMAKE_CXX_STANDARD 17)

find_package(android REQUIRED)

add_library(
    xunnet-core
    SHARED
    xunnet_core.cpp
)

target_link_libraries(
    xunnet-core
    android
    log
)
