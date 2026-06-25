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
        # Install Android NDK if not present (GitHub Actions ubuntu-latest has no NDK by default)
        if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK_ROOT" ]; then
            echo "ANDROID_NDK_HOME not set; installing NDK via sdkmanager..."
            SDKMANAGER=$(command -v sdkmanager || echo "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager")
            yes | "$SDKMANAGER" --install "ndk;26.1.10909125" >/dev/null 2>&1 || true
            export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/26.1.10909125"
            export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
        fi
        if [ -z "$ANDROID_NDK_HOME" ]; then
            echo "ERROR: ANDROID_NDK_HOME is not set and could not be installed."
            exit 1
        fi
        TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
        for arch in arm64 armv7 amd64; do
            echo "Building for android/$arch"
            case $arch in
                arm64)
                    CC="$TOOLCHAIN/bin/aarch64-linux-android21-clang"
                    GOARCH=arm64
                    OUT_ARCH=arm64-v8a
                    ;;
                armv7)
                    CC="$TOOLCHAIN/bin/armv7a-linux-androideabi21-clang"
                    GOARCH=arm
                    OUT_ARCH=armeabi-v7a
                    ;;
                amd64)
                    CC="$TOOLCHAIN/bin/x86_64-linux-android21-clang"
                    GOARCH=amd64
                    OUT_ARCH=x86_64
                    ;;
            esac
            CC="$CC" CGO_ENABLED=1 GOOS=android GOARCH="$GOARCH" go build -ldflags '-s -w' -buildmode=c-shared -o "$BUILD_DIR/libs/android/$OUT_ARCH/libsingbox.so" ./cmd/sing-box
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
