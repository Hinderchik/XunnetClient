# Xunnet

**Xunnet** — open-source экосистема для создания и управления VPN-инфраструктурой с поддержкой федеративного взаимодействия между панелями.

## Компоненты экосистемы

| Компонент | Описание | Статус |
|-----------|----------|--------|
| **Xunnet Android** | VPN-клиент для Android (Kotlin + Compose + sing-box) | 🚧 В разработке |
| **Xunnet Desktop** | VPN-клиент для Windows/Linux (Qt 6 + C++) | 📋 Запланирован |
| **Xunnet Panel** | Веб-панель управления серверами (Go + React) | 📋 Запланирован |
| **Xunnet Federation** | Протокол обмена между панелями | 🚧 Спецификация готова |
| **Xunnet Formats** | Собственные форматы ссылок и конфигов | ✅ Спецификация готова |

## Ключевые особенности

- **Полная совместимость** с существующими форматами: Clash, v2rayN, Sing-box, SIP008, Happ
- **Собственный формат Xunnet** для ссылок, конфигураций и подписок
- **Федерация панелей** — обмен серверами между независимыми панелями Xunnet
- **Мультиподписка и агрегация** — объединение серверов из разных источников
- **Современный UI** на Material You / Material Design 3
- **Open-source** под лицензией GPL-3.0

## Быстрый старт

### Android

```bash
# Клонировать репозиторий
git clone https://github.com/Hinderchik/XunnetClient.git
cd XunnetClient

# Сборка debug APK (только для разработки, релизные сборки только через CI)
./gradlew assembleDebug
```

> **Важно:** релизные APK/AAB собираются исключительно через GitHub Actions. Локальная сборка релизного APK запрещена.

### Панель (Docker)

```bash
cp .env.example .env
# Отредактируйте .env
docker-compose up -d
```

## Документация

- [Техническое задание](docs/SPECIFICATION.md)
- [Формат ссылок Xunnet](docs/LINK_FORMAT.md)
- [Формат конфигурации Xunnet](docs/CONFIG_FORMAT.md)
- [Протокол федерации](docs/FEDERATION.md)
- [Руководство по сборке](docs/BUILD.md)
- [Участие в проекте](docs/CONTRIBUTING.md)

## Лицензия

[GPL-3.0](LICENSE)
