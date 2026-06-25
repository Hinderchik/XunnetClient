# Xunnet Ecosystem — Technical Specification

**Version:** 2.0  
**Date:** 2026-06-25  
**License:** GPL-3.0  
**Status:** Draft for implementation

---

## 1. Executive Summary

Xunnet is a complete open-source VPN ecosystem consisting of:

1. **Xunnet Android** — VPN client for Android (Kotlin + Jetpack Compose + sing-box)
2. **Xunnet Desktop** — VPN client for Windows/Linux (Qt 6 + C++ + sing-box)
3. **Xunnet Panel** — Web-based server management dashboard (Go + React + MUI)
4. **Xunnet Federation Protocol** — Decentralized panel-to-panel communication
5. **Xunnet Formats** — Proprietary link, config, and subscription formats

This document is the authoritative technical specification (ТЗ) for all development teams.

---

## 2. Global Constraints

| Constraint | Rule |
|------------|------|
| **APK builds** | Strictly forbidden on developer devices. Only GitHub Actions CI/CD. |
| **License** | GPL-3.0 for all components. |
| **Code quality** | Industrial grade: tests, linting, documentation, code review. |
| **Design** | Modern, beautiful, intuitive (Material You on Android, Fluent/QML on Desktop). |
| **Compatibility** | Must work with any compatible server, not only self-hosted. |
| **Federation** | Panels must be able to connect to other Xunnet panels. |

---

## 3. System Architecture

### 3.1 High-level diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         XUNNET FEDERATION NETWORK                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐  │
│  │  Xunnet Panel A  │◄────►│  Xunnet Panel B  │◄────►│  Xunnet Panel C  │  │
│  │  (US - Master)   │      │  (EU - Slave)    │      │  (ASIA - Peer)   │  │
│  └────────┬─────────┘      └────────┬─────────┘      └────────┬─────────┘  │
│           │                         │                         │            │
│           └─────────────────────────┼─────────────────────────┘            │
│                                     │                                      │
│                                     ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        XUNNET CLIENTS                               │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐ │  │
│  │  │ Android  │  │ Windows  │  │  Linux   │  │ iOS (future release) │ │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  A client can connect to any panel in the federation and receive servers    │
│  from all federated panels.                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Component matrix

| Component | Stack | Repository |
|-----------|-------|------------|
| Xunnet Panel | Go (Gin) + React + MUI + SQLite/PostgreSQL | `XunnetPanel` |
| Xunnet Android | Kotlin + Compose + sing-box + gRPC | `XunnetClient` |
| Xunnet Desktop | Qt 6 + C++ + sing-box | `XunnetDesktop` |
| Federation | REST + WebSocket + gRPC | Shared protocol |
| Formats | JSON + URI + AES-256-GCM | Shared libraries |

---

## 4. Xunnet Android Client

### 4.1 Target

| Parameter | Value |
|-----------|-------|
| Name | Xunnet (Android) |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 |
| UI | Kotlin + Jetpack Compose |
| Core | sing-box (Go) compiled as `.so` via CGO |
| IPC | gRPC between Kotlin and Go |
| License | GPL-3.0 |
| Distribution | GitHub Releases (APK + AAB) |
| Build | GitHub Actions only |

### 4.2 Supported protocols

| Category | Protocols |
|----------|-----------|
| Basic | SOCKS4/5, HTTP/HTTPS proxy |
| Classical | Shadowsocks (AEAD), Trojan, VMess (incl. alterId), VLESS |
| Modern | Hysteria v1/v2, TUIC v1/v5, WireGuard, NaiveProxy |
| Specialized | ShadowTLS, Mieru, Juicity, TrustTunnel, AmneziaWG, Tailscale, SSH, Tor |
| Advanced | AnyTLS, REALITY (VLESS), XTLS, Custom Outbound, Chaining outbounds |
| Additional | Brook, Gost (via custom config), Snell, V2Ray |

### 4.3 Supported link formats

#### Third-party single links
`ss://`, `trojan://`, `vmess://`, `vless://`, `hysteria://`, `hysteria2://`, `tuic://`, `wireguard://`, `ssh://`, `socks://`, `http://`, `https://`

#### Third-party subscriptions
- SIP008
- v2rayN JSON group
- Clash (YAML/JSON)
- Sing-box JSON
- Happ encrypted links
- Deeplinks: `nekobox://`, `v2raytun://`, `xray://`

#### Xunnet native links
- `xunnet://` — single server
- `xungroup://` — group/subscription reference
- `xunset://` — profile settings
- `xunephem://` — ephemeral one-time link
- `xuncrypt://` — password-encrypted link
- `xunfed://` — federated panel link

### 4.4 Android module structure

```
app/
├── core/
│   ├── data/
│   │   ├── repositories/
│   │   ├── models/
│   │   └── datasources/
│   ├── domain/
│   │   ├── usecases/
│   │   └── entities/
│   └── di/
├── features/
│   ├── dashboard/
│   ├── proxies/
│   ├── subscriptions/
│   ├── federation/
│   ├── settings/
│   ├── stats/
│   └── onboarding/
├── core-vpn/
│   ├── service/
│   ├── core/
│   └── tun/
├── core-libs/
│   ├── singbox/
│   └── grpc/
└── buildSrc/
```

### 4.5 Key Kotlin interfaces

```kotlin
interface SingBoxCore {
    suspend fun start(config: String): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun getStatus(): Status
    suspend fun updateConfig(config: String): Result<Unit>
    suspend fun testLatency(server: String): Long
}

interface ProfileRepository {
    suspend fun getAll(): List<Profile>
    suspend fun getById(id: String): Profile?
    suspend fun save(profile: Profile): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun importFromLink(link: String): Result<Profile>
    suspend fun exportProfile(id: String): Result<String>
    suspend fun importFromFile(file: File): Result<List<Profile>>
}

interface SubscriptionRepository {
    suspend fun getAll(): List<Subscription>
    suspend fun add(subscription: Subscription): Result<Unit>
    suspend fun update(subscription: Subscription): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun refresh(id: String): Result<List<Profile>>
    suspend fun refreshAll(): Result<Unit>
    suspend fun aggregate(subscriptionIds: List<String>): Result<Subscription>
}

interface FederationRepository {
    suspend fun getAllPanels(): List<FederatedPanel>
    suspend fun addPanel(panel: FederatedPanel): Result<Unit>
    suspend fun updatePanel(panel: FederatedPanel): Result<Unit>
    suspend fun deletePanel(id: String): Result<Unit>
    suspend fun syncPanel(id: String): Result<List<Profile>>
    suspend fun syncAllPanels(): Result<Unit>
    suspend fun getPanelStatus(id: String): PanelStatus
}

interface LinkParser {
    fun parse(link: String): Result<Profile>
    fun parseFile(file: File): Result<List<Profile>>
    fun generate(profile: Profile): String
    fun generateXunCrypt(profile: Profile, password: String): String
}
```

### 4.6 UI requirements

- Material You / Material Design 3 dynamic colors
- Light/dark/system theme
- 60 fps animations, Lottie where appropriate
- Tablet/foldable adaptive layouts
- Dashboard, Proxies, Subscriptions, Federation, Stats, Settings screens
- Real-time speed/connection stats in notification and dashboard

---

## 5. Xunnet Desktop Client

### 5.1 Target

| Parameter | Value |
|-----------|-------|
| Name | Xunnet Desktop |
| Platforms | Windows 10/11 x64/arm64, Linux x64/arm64 |
| Framework | Qt 6 (C++17) |
| UI | QML + optional Qt Widgets fallback |
| Core | sing-box native binary |
| License | GPL-3.0 |
| Distribution | GitHub Releases (exe, AppImage, deb, rpm) |

### 5.2 Unique desktop features

- System proxy configuration (Windows/Linux)
- TUN mode with admin privileges
- Global hotkeys (Ctrl+1..9 profile switching)
- System tray control
- CLI mode for automation
- Auto-start with OS
- Multi-hop proxy chains
- Local SOCKS5/HTTP proxy for other apps

### 5.3 Key C++ classes

```cpp
class XunnetManager : public QObject {
    Q_OBJECT
public:
    bool startVpn(const Profile& profile);
    bool stopVpn();
    void updateConfig(const QString& config);
    Stats getStats() const;
    bool testLatency(const QString& server);
signals:
    void statusChanged(bool connected);
    void statsUpdated(const Stats& stats);
};

class SubscriptionManager : public QObject {
    Q_OBJECT
public:
    QList<Subscription> getAll() const;
    bool add(const Subscription& sub);
    bool remove(const QString& id);
    QList<Profile> refresh(const QString& id);
    QList<Profile> refreshAll();
    Subscription aggregate(const QStringList& ids);
};

class FederationManager : public QObject {
    Q_OBJECT
public:
    QList<FederatedPanel> getPanels() const;
    bool addPanel(const FederatedPanel& panel);
    bool removePanel(const QString& id);
    QList<Profile> syncPanel(const QString& id);
    QList<Profile> syncAllPanels();
    PanelStatus getStatus(const QString& id);
signals:
    void panelUpdated(const QString& id);
    void panelError(const QString& id, const QString& error);
};

class SystemProxy : public QObject {
    Q_OBJECT
public:
    bool setProxy(const QString& host, int port);
    bool clearProxy();
    bool isProxySet() const;
};
```

---

## 6. Xunnet Panel

### 6.1 Target

| Parameter | Value |
|-----------|-------|
| Name | Xunnet Panel |
| Backend | Go (Gin) |
| Frontend | React + TypeScript + MUI |
| Database | SQLite (default), PostgreSQL (optional) |
| Core | Xray-core + sing-box (dual support) |
| API | RESTful + Swagger/OpenAPI |
| License | GPL-3.0 |
| Distribution | Docker image + binaries |

### 6.2 Server management

- Add servers manually, via subscription, file import, API, or federation
- Types: own (Xray-core), external (any compatible), federated (from other panels)
- Status monitoring: online/offline, CPU/RAM, traffic
- Auto config sync
- Availability/latency testing

### 6.3 Subscription & aggregation

- Multi-subscription support
- Composite subscriptions aggregating multiple sources
- Filtering by include/exclude tags
- Strategy: latency-based, priority-based, random
- Export formats: Xunnet JSON, Clash, SIP008, Happ

### 6.4 User management

- Create/edit/delete users
- Traffic quota and expiry date
- Tags and notes
- Generate Xunnet links, QR codes, subscription URLs
- Reset passwords

### 6.5 Federation

#### Modes

| Mode | Description |
|------|-------------|
| PULL | Pull servers from remote panel |
| PUSH | Push own servers to remote panel |
| SYNC | Full bidirectional exchange |
| PROXY | Transit gateway for other panels |

#### Security

- API key per panel (32+ chars)
- HMAC request signing
- IP whitelist
- Rate limiting
- TLS/HTTPS only
- JWT for sync sessions

#### Federation API endpoints

```http
GET    /api/federation/info
GET    /api/federation/servers?tags=premium,fast&limit=50
GET    /api/federation/users
POST   /api/federation/stats
POST   /api/federation/sync
```

### 6.6 REST API summary

#### Public

```http
GET /api/public/servers
GET /api/public/servers/:id
GET /api/public/subscription/:id
GET /api/public/subscription/:id/clash
GET /api/public/subscription/:id/sip008
GET /api/public/subscription/:id/happ
GET /api/public/status
GET /api/public/stats/overview
GET /api/public/federation/info
```

#### Private (JWT)

- `/api/v1/users/*`
- `/api/v1/servers/*`
- `/api/v1/subscriptions/*`
- `/api/v1/federation/*`
- `/api/v1/stats/*`

---

## 7. Xunnet Formats

### 7.1 Xunnet Link Format

```
xunnet://[method]@[host]:[port]/?[params]#[name]
```

Required params: `protocol`
Optional params: `uuid`, `password`, `flow`, `type`, `host`, `path`, `tls`, `sni`, `fp`, `pbk`, `sid`, `mux`, `panel_id`, etc.

See [LINK_FORMAT.md](LINK_FORMAT.md) for full specification.

### 7.2 Xunnet Config Format

JSON schema with sections:
- `version`
- `meta`
- `settings` (mode, tun, dns, routing)
- `federation`
- `subscriptions`
- `servers`
- `groups`
- `rules`

See [CONFIG_FORMAT.md](CONFIG_FORMAT.md) for full JSON schema.

---

## 8. Testing & Documentation

### 8.1 Testing requirements

| Component | Tests |
|-----------|-------|
| Android | JUnit + MockK (80% coverage), Espresso, integration, performance, security, federation |
| Desktop | Google Test, Qt Test, cross-platform, federation |
| Panel | Go unit tests, Postman/Newman, Cypress/Playwright, federation |

### 8.2 Documentation deliverables

- README.md
- API docs (Swagger/OpenAPI)
- Federation protocol spec
- Build guide
- Contributor guide
- User manual
- FAQ

---

## 9. Final Requirements

1. Full documentation
2. Comprehensive testing
3. Localization (RU + EN minimum)
4. CI/CD for all components
5. Security: encrypted data, hashed passwords, protected API keys
6. Performance: thousands of users, hundreds of servers
7. Scalability: horizontal scaling ready
8. Backward compatibility with Clash, v2rayN, Sing-box
9. Full federation support

---

## 10. Glossary

| Term | Meaning |
|------|---------|
| TUN | Virtual network interface mode |
| MUX | Multiplexing multiple streams over one connection |
| SNI | Server Name Indication |
| REALITY | XTLS advanced TLS fingerprinting |
| Federation | Decentralized panel interconnection |
| XunCrypt | Password-based AES-256-GCM link encryption |
