# Xunnet Build Guide

**Version:** 2.0  
**License:** GPL-3.0

---

## ⚠️ Critical Rule

**APK builds on developer devices are strictly forbidden.** All release APKs and AABs must be built via GitHub Actions CI/CD.

Local builds are allowed only for development, unit tests, and debug APKs that are never distributed.

---

## Prerequisites

### Android development

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK API 34
- NDK r26b
- Go 1.21+
- CMake 3.22+

### Desktop development

- Qt 6.6+
- CMake 3.22+
- C++17 compiler (MSVC 2022 / GCC 11 / Clang 15)
- Go 1.21+ (for sing-box build)

### Panel development

- Go 1.21+
- Node.js 20+
- Docker (optional)

---

## Android

### Clone and setup

```bash
git clone https://github.com/Hinderchik/XunnetClient.git
cd XunnetClient
```

### Build sing-box native library

```bash
cd core-libs/singbox
./build.sh android
```

### Build debug APK (local only)

```bash
./gradlew assembleDebug
```

### Run tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Release build (CI only)

Pushing a tag `android-v*` triggers GitHub Actions:

```bash
git tag android-v2.0.0
git push origin android-v2.0.0
```

---

## Desktop

### Build

```bash
cd xunnet-desktop
mkdir build && cd build
cmake ..
cmake --build . --config Release
```

### Package

```bash
cpack -G NSIS          # Windows
cpack -G DEB           # Linux Debian
cpack -G RPM           # Linux RedHat
cpack -G AppImage      # Linux AppImage
```

---

## Panel

### Build binary

```bash
cd xunnet-panel
go build -o xunnet-panel ./cmd/server
```

### Build frontend

```bash
cd xunnet-panel/web
npm install
npm run build
```

### Run with Docker

```bash
cd xunnet-panel
docker-compose up --build
```

---

## CI/CD

See `.github/workflows/` for all workflows:

- `android.yml` — build and sign Android release
- `desktop.yml` — build Windows/Linux packages
- `panel.yml` — build Docker image and Go binary
- `docs.yml` — validate documentation

---

## Signing

Release signing keys are stored in GitHub Secrets:

- `SIGNING_KEY`
- `ALIAS`
- `KEY_STORE_PASSWORD`
- `KEY_PASSWORD`

Never commit keys to the repository.
