# Xunnet Desktop

Cross-platform desktop VPN client for Windows and Linux based on Qt 6 and sing-box.

## Features

- Full protocol support (VLESS, VMess, Trojan, Hysteria2, WireGuard, etc.)
- Xunnet Link and Config formats
- Federation support
- System proxy and TUN mode
- Tray icon and hotkeys
- CLI mode

## Build

```bash
mkdir build && cd build
cmake ..
cmake --build . --parallel
```

## Requirements

- Qt 6.5+
- CMake 3.20+
- sing-box binary

## License

GPL-3.0
