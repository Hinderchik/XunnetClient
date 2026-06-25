# Xunnet Federation Protocol

**Version:** 2.0  
**Status:** Draft  
**License:** GPL-3.0

---

## 1. Overview

Xunnet Federation Protocol (XFP) enables decentralized interconnection between Xunnet Panels. Panels can exchange servers, users, and statistics while maintaining independent administration.

---

## 2. Concepts

### 2.1 Roles

| Role | Description |
|------|-------------|
| `master` | Authoritative panel; can push config to slaves |
| `slave` | Receives servers/users from master |
| `peer` | Equal partnership; bidirectional sync |

### 2.2 Modes

| Mode | Direction | Data exchanged |
|------|-----------|----------------|
| `PULL` | Outgoing | Remote servers pulled to local panel |
| `PUSH` | Outgoing | Local servers pushed to remote panel |
| `SYNC` | Bidirectional | Servers + optional users + stats |
| `PROXY` | Transit | Forwards requests to other panels |

### 2.3 Trust model

Each federation relationship is established manually by exchanging:
- Panel ID
- Panel URL
- API key
- Allowed IP addresses (optional)
- Mode and role

---

## 3. Security

### 3.1 Authentication

Every request must include:

```http
X-Panel-ID: panel_us_01
X-API-Key: fed_xxxxxxxxxxxxx
```

### 3.2 Request signing

Sensitive endpoints require HMAC-SHA256 signature:

```http
X-Signature: hmac_sha256(api_key, method + path + timestamp + body)
X-Timestamp: 1718900000
```

### 3.3 Transport

- HTTPS only
- TLS 1.2+
- Valid certificate (or pinned certificate for self-hosted)

### 3.4 Rate limiting

- Default: 60 requests per minute per panel
- Sync endpoints: 1 request per minute per panel

---

## 4. API Endpoints

### 4.1 Info

```http
GET /api/federation/info
Headers:
  X-Panel-ID: panel_us_01
  X-API-Key: fed_xxxxxxxxxxxxx
```

Response:

```json
{
  "panel_id": "panel_us_01",
  "name": "US Panel",
  "version": "2.0",
  "role": "master",
  "servers_count": 45,
  "users_count": 1200,
  "capabilities": ["vless", "vmess", "trojan", "wireguard", "hysteria2"],
  "federation_enabled": true,
  "max_servers_per_sync": 100
}
```

### 4.2 List servers

```http
GET /api/federation/servers?tags=premium,fast&limit=50&offset=0
```

Response:

```json
{
  "servers": [
    {
      "id": "srv_us_01",
      "name": "US_VIP_01",
      "protocol": "vless",
      "address": "us-vip.xun.net",
      "port": 443,
      "params": { ... },
      "tags": ["premium", "us"],
      "priority": 10,
      "enabled": true,
      "source_panel": "panel_us_01"
    }
  ],
  "total": 45,
  "source": "panel_us_01"
}
```

### 4.3 List users (SYNC only)

```http
GET /api/federation/users
```

Response:

```json
{
  "users": [
    {
      "id": "user_1",
      "username": "john_doe",
      "quota": 100,
      "expires": "2026-12-31T23:59:59Z",
      "tags": ["premium"]
    }
  ]
}
```

### 4.4 Submit stats

```http
POST /api/federation/stats
Content-Type: application/json

{
  "panel_id": "panel_eu_01",
  "traffic": {
    "upload": 1024000,
    "download": 2048000
  },
  "users_online": 45,
  "timestamp": "2026-06-20T12:00:00Z"
}
```

### 4.5 Full sync

```http
POST /api/federation/sync
Content-Type: application/json
X-Signature: <hmac>
X-Timestamp: 1718900000

{
  "panel_id": "panel_eu_01",
  "servers": [ ... ],
  "users": [ ... ],
  "stats": { ... },
  "timestamp": "2026-06-20T12:00:00Z",
  "signature": "<hmac>"
}
```

---

## 5. Data models

### 5.1 FederatedPanel

```go
type FederatedPanel struct {
    ID           string    `json:"id"`
    Name         string    `json:"name"`
    URL          string    `json:"url"`
    APIKey       string    `json:"api_key"`
    Role         string    `json:"role"`   // master, slave, peer
    Mode         string    `json:"mode"`   // pull, push, sync, proxy
    Status       string    `json:"status"` // online, offline, pending
    LastSync     time.Time `json:"last_sync"`
    ServersCount int       `json:"servers_count"`
    Tags         []string  `json:"tags"`
    Enabled      bool      `json:"enabled"`
}
```

### 5.2 FederationSync

```go
type FederationSync struct {
    PanelID   string    `json:"panel_id"`
    Servers   []Server  `json:"servers"`
    Users     []User    `json:"users,omitempty"`
    Stats     Stats     `json:"stats,omitempty"`
    Timestamp time.Time `json:"timestamp"`
    Signature string    `json:"signature"`
}
```

### 5.3 FederatedServer

```go
type FederatedServer struct {
    ID          string   `json:"id"`
    Name        string   `json:"name"`
    Protocol    string   `json:"protocol"`
    Address     string   `json:"address"`
    Port        int      `json:"port"`
    Params      map[string]interface{} `json:"params"`
    Tags        []string `json:"tags"`
    SourcePanel string   `json:"source_panel"`
    Priority    int      `json:"priority"`
    Enabled     bool     `json:"enabled"`
}
```

---

## 6. Sync algorithm

1. Local panel sends `/api/federation/info` to remote.
2. If remote accepts, local sends `/api/federation/servers` (PULL) or `/api/federation/sync` (SYNC).
3. Remote validates signature and API key.
4. Remote returns requested data.
5. Local merges servers, tagging them with remote `panel_id`.
6. Local updates `last_sync` and `status`.
7. If mode is SYNC, local also pushes its own servers.

---

## 7. Conflict resolution

- Same `id` from same source panel: overwrite with remote data.
- Same `id` from different panels: keep both with prefixed IDs.
- Disabled servers on remote: disable locally unless manually enabled.
- Deleted servers: soft-delete locally (mark `enabled=false`).

---

## 8. Error codes

| Code | Meaning |
|------|---------|
| `FED_001` | Invalid API key |
| `FED_002` | Invalid signature |
| `FED_003` | Timestamp too old |
| `FED_004` | Rate limit exceeded |
| `FED_005` | Federation disabled on remote |
| `FED_006` | Mode not allowed |
| `FED_007` | IP not whitelisted |
| `FED_008` | Incompatible version |

---

## 9. Client-side federation

Xunnet Android/Desktop clients can also use federation by connecting directly to panels:

```
xunfed://panel_us_01@panel-us.xunnet.dev:443/?token=fed_xxx&sync=true
```

The client will:
1. Register the panel in its config.
2. Periodically sync servers.
3. Display federated servers alongside local/subscription servers.
4. Tag them with the source panel name.
