#!/bin/bash
set -e

PLATFORM=${1:-android}
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
BUILD_DIR="$SCRIPT_DIR/build"

echo "Building sing-box for platform: $PLATFORM"

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [ ! -d "sing-box" ]; then
    git clone --depth 1 --branch v1.8.0 https://github.com/SagerNet/sing-box.git
fi

cd sing-box

case $PLATFORM in
    android)
        for arch in arm64 arm7 amd64; do
            echo "Building for android/$arch"
            GOOS=android GOARCH=$arch CGO_ENABLED=1 go build -ldflags '-s -w' -o "$BUILD_DIR/libs/android/$arch/libsingbox.so" ./cmd/sing-box
        done
        ;;
    linux)
        GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -ldflags '-s -w' -o "$BUILD_DIR/libs/linux/sing-box" ./cmd/sing-box
        ;;
    windows)
        GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -ldflags '-s -w' -o "$BUILD_DIR/libs/windows/sing-box.exe" ./cmd/sing-box
        ;;
    *)
        echo "Unknown platform: $PLATFORM"
        exit 1
        ;;
esac

echo "Build complete. Output: $BUILD_DIR/libs"
