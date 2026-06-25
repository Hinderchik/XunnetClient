# Xunnet Config Format Specification

**Version:** 2.0  
**Status:** Draft  
**License:** GPL-3.0

---

## 1. Overview

Xunnet Config Format (XCF) is a JSON-based configuration format used by all Xunnet clients and panels. It describes application settings, subscriptions, servers, groups, routing rules, and federation peers.

---

## 2. Top-level structure

```json
{
  "$schema": "https://xunnet.dev/schema/v2/config.json",
  "version": "2.0",
  "meta": { ... },
  "settings": { ... },
  "federation": { ... },
  "subscriptions": [ ... ],
  "servers": [ ... ],
  "groups": [ ... ],
  "rules": [ ... ]
}
```

### Required fields

- `version`
- `servers`

---

## 3. Sections

### 3.1 `meta`

```json
{
  "meta": {
    "name": "My Xunnet Config",
    "description": "Premium build with federation",
    "created": "2026-06-20T12:00:00Z",
    "updated": "2026-06-20T12:00:00Z",
    "author": "Xunnet User",
    "tags": ["premium", "federation"]
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Config name |
| `description` | string | No | Description |
| `created` | string (ISO 8601) | No | Creation time |
| `updated` | string (ISO 8601) | No | Last update time |
| `author` | string | No | Author name |
| `tags` | string[] | No | Tags |

### 3.2 `settings`

```json
{
  "settings": {
    "mode": "tun",
    "tun": {
      "enabled": true,
      "stack": "gvisor",
      "address": "172.19.0.1/24",
      "mtu": 9000,
      "auto_route": true,
      "strict_route": false,
      "allow_apps": ["com.google.android.youtube"],
      "block_apps": ["com.facebook.katana"]
    },
    "dns": {
      "servers": ["1.1.1.1", "8.8.8.8"],
      "mode": "fakeip",
      "fakeip_range": "198.18.0.0/15",
      "cache": true
    },
    "routing": {
      "mode": "auto",
      "rules": []
    }
  }
}
```

#### `settings.mode`

| Value | Description |
|-------|-------------|
| `tun` | Virtual network interface mode |
| `proxy` | Local proxy mode |
| `socks` | SOCKS5 proxy only |

#### `settings.tun`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | false | Enable TUN |
| `stack` | string | `gvisor` | `gvisor`, `system`, `mixed` |
| `address` | string | `172.19.0.1/24` | TUN interface address |
| `mtu` | int | 9000 | MTU |
| `auto_route` | boolean | true | Auto route all traffic |
| `strict_route` | boolean | false | Strict routing |
| `allow_apps` | string[] | [] | Allowed app packages (Android) |
| `block_apps` | string[] | [] | Blocked app packages (Android) |

#### `settings.dns`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `servers` | string[] | `["1.1.1.1"]` | DNS servers |
| `mode` | string | `normal` | `normal`, `fakeip`, `redir` |
| `fakeip_range` | string | `198.18.0.0/15` | FakeIP range |
| `cache` | boolean | true | Enable DNS cache |

#### `settings.routing`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | string | `auto` | `auto`, `global`, `direct`, `custom` |
| `rules` | Rule[] | [] | Custom routing rules |

### 3.3 `federation`

```json
{
  "federation": {
    "enabled": true,
    "strategy": "latency_based",
    "panels": [
      {
        "id": "panel_us_01",
        "name": "US Panel",
        "host": "https://panel-us.xunnet.dev",
        "api_key": "fed_xxxxxxxxxxxxx",
        "sync_interval": 300,
        "enabled": true,
        "tags": ["external", "us"]
      }
    ]
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `enabled` | boolean | No | Enable federation |
| `strategy` | string | No | `latency_based`, `priority_based`, `random` |
| `panels` | FederatedPanel[] | No | Federated panels |

#### `FederatedPanel`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Panel ID |
| `name` | string | Yes | Display name |
| `host` | string (URL) | Yes | Panel URL |
| `api_key` | string | Yes | API key |
| `sync_interval` | int | No | Sync interval in seconds |
| `enabled` | boolean | No | Enabled |
| `tags` | string[] | No | Tags applied to imported servers |

### 3.4 `subscriptions`

```json
{
  "subscriptions": [
    {
      "id": "sub_1",
      "name": "Premium Subscription",
      "url": "https://example.com/subscription.json",
      "format": "xunnet",
      "update_interval": 3600,
      "enabled": true,
      "tags": ["premium"],
      "servers": []
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Subscription ID |
| `name` | string | Yes | Display name |
| `url` | string (URL) | Yes | Subscription URL |
| `format` | string | No | `xunnet`, `singbox`, `clash`, `v2ray`, `sip008`, `happ` |
| `update_interval` | int | No | Auto-update interval in seconds |
| `enabled` | boolean | No | Enabled |
| `tags` | string[] | No | Tags |
| `servers` | Server[] | No | Cached servers |

### 3.5 `servers`

```json
{
  "servers": [
    {
      "id": "server_1",
      "name": "US_VIP_01",
      "protocol": "vless",
      "address": "us-vip.xun.net",
      "port": 443,
      "encryption": "chacha20-ietf-poly1305",
      "params": {
        "uuid": "123e4567-e89b-12d3-a456-426614174000",
        "flow": "xtls-rprx-vision",
        "type": "tcp",
        "tls": true,
        "sni": "us-vip.xun.net",
        "fp": "chrome",
        "pbk": "ABcdEFgh...",
        "sid": "1234567890abcdef"
      },
      "tags": ["premium", "us"],
      "priority": 10,
      "enabled": true,
      "source": "panel_us_01",
      "latency": 23,
      "speed": 100
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Server ID |
| `name` | string | Yes | Display name |
| `protocol` | string | Yes | Protocol type |
| `address` | string | Yes | Server address |
| `port` | int | Yes | Server port |
| `encryption` | string | No | Encryption method |
| `params` | object | No | Protocol-specific parameters |
| `tags` | string[] | No | Tags |
| `priority` | int (1-10) | No | Priority |
| `enabled` | boolean | No | Enabled |
| `source` | string | No | Source panel/subscription ID |
| `latency` | int | No | Measured latency in ms |
| `speed` | int | No | Measured speed in Mbps |

### 3.6 `groups`

```json
{
  "groups": [
    {
      "id": "group_1",
      "name": "America",
      "server_ids": ["server_1"],
      "strategy": "latency"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Group ID |
| `name` | string | Yes | Group name |
| `server_ids` | string[] | No | Server IDs |
| `strategy` | string | No | `latency`, `random`, `round-robin`, `priority` |

### 3.7 `rules`

```json
{
  "rules": [
    {
      "type": "domain",
      "value": "google.com",
      "outbound": "proxy",
      "action": "route"
    },
    {
      "type": "geoip",
      "value": "cn",
      "outbound": "direct",
      "action": "route"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | `domain`, `ip`, `geoip`, `geosite`, `process` |
| `value` | string | Yes | Rule value |
| `outbound` | string | Yes | Outbound tag |
| `action` | string | No | `route`, `block`, `bypass` |

---

## 4. JSON Schema

```json
{
  "$schema": "https://xunnet.dev/schema/v2/config.json",
  "type": "object",
  "required": ["version", "servers"],
  "properties": {
    "version": {
      "type": "string",
      "enum": ["2.0"]
    },
    "meta": {
      "type": "object",
      "properties": {
        "name": { "type": "string" },
        "description": { "type": "string" },
        "created": { "type": "string", "format": "date-time" },
        "updated": { "type": "string", "format": "date-time" },
        "author": { "type": "string" },
        "tags": { "type": "array", "items": { "type": "string" } }
      }
    },
    "settings": {
      "type": "object",
      "properties": {
        "mode": { "type": "string", "enum": ["tun", "proxy", "socks"] },
        "tun": {
          "type": "object",
          "properties": {
            "enabled": { "type": "boolean" },
            "stack": { "type": "string", "enum": ["gvisor", "system", "mixed"] },
            "address": { "type": "string" },
            "mtu": { "type": "integer" },
            "auto_route": { "type": "boolean" },
            "strict_route": { "type": "boolean" },
            "allow_apps": { "type": "array", "items": { "type": "string" } },
            "block_apps": { "type": "array", "items": { "type": "string" } }
          }
        },
        "dns": {
          "type": "object",
          "properties": {
            "servers": { "type": "array", "items": { "type": "string" } },
            "mode": { "type": "string", "enum": ["normal", "fakeip", "redir"] },
            "fakeip_range": { "type": "string" },
            "cache": { "type": "boolean" }
          }
        },
        "routing": {
          "type": "object",
          "properties": {
            "mode": { "type": "string", "enum": ["auto", "global", "direct", "custom"] },
            "rules": { "type": "array", "items": { "$ref": "#/definitions/Rule" } }
          }
        }
      }
    },
    "federation": {
      "type": "object",
      "properties": {
        "enabled": { "type": "boolean" },
        "strategy": { "type": "string", "enum": ["latency_based", "priority_based", "random"] },
        "panels": { "type": "array", "items": { "$ref": "#/definitions/FederatedPanel" } }
      }
    },
    "subscriptions": {
      "type": "array",
      "items": { "$ref": "#/definitions/Subscription" }
    },
    "servers": {
      "type": "array",
      "items": { "$ref": "#/definitions/Server" }
    },
    "groups": {
      "type": "array",
      "items": { "$ref": "#/definitions/Group" }
    },
    "rules": {
      "type": "array",
      "items": { "$ref": "#/definitions/Rule" }
    }
  },
  "definitions": {
    "FederatedPanel": {
      "type": "object",
      "required": ["id", "name", "host"],
      "properties": {
        "id": { "type": "string" },
        "name": { "type": "string" },
        "host": { "type": "string", "format": "uri" },
        "api_key": { "type": "string" },
        "sync_interval": { "type": "integer", "minimum": 60 },
        "enabled": { "type": "boolean" },
        "tags": { "type": "array", "items": { "type": "string" } }
      }
    },
    "Subscription": {
      "type": "object",
      "required": ["id", "name", "url"],
      "properties": {
        "id": { "type": "string" },
        "name": { "type": "string" },
        "url": { "type": "string", "format": "uri" },
        "format": { "type": "string", "enum": ["xunnet", "singbox", "clash", "v2ray", "sip008", "happ"] },
        "update_interval": { "type": "integer", "minimum": 60 },
        "enabled": { "type": "boolean" },
        "tags": { "type": "array", "items": { "type": "string" } },
        "servers": { "type": "array", "items": { "$ref": "#/definitions/Server" } }
      }
    },
    "Server": {
      "type": "object",
      "required": ["id", "name", "protocol", "address", "port"],
      "properties": {
        "id": { "type": "string" },
        "name": { "type": "string" },
        "protocol": { "type": "string" },
        "address": { "type": "string" },
        "port": { "type": "integer", "minimum": 1, "maximum": 65535 },
        "encryption": { "type": "string" },
        "params": { "type": "object" },
        "tags": { "type": "array", "items": { "type": "string" } },
        "priority": { "type": "integer", "minimum": 1, "maximum": 10 },
        "enabled": { "type": "boolean" },
        "source": { "type": "string" },
        "latency": { "type": "integer" },
        "speed": { "type": "integer" }
      }
    },
    "Group": {
      "type": "object",
      "required": ["id", "name"],
      "properties": {
        "id": { "type": "string" },
        "name": { "type": "string" },
        "server_ids": { "type": "array", "items": { "type": "string" } },
        "strategy": { "type": "string", "enum": ["latency", "random", "round-robin", "priority"] }
      }
    },
    "Rule": {
      "type": "object",
      "required": ["type", "value", "outbound"],
      "properties": {
        "type": { "type": "string", "enum": ["domain", "ip", "geoip", "geosite", "process"] },
        "value": { "type": "string" },
        "outbound": { "type": "string" },
        "action": { "type": "string", "enum": ["route", "block", "bypass"] }
      }
    }
  }
}
```

---

## 5. Aggregated Subscription Format

Used when a panel or client combines multiple subscriptions into one.

```json
{
  "version": "2.0",
  "format": "xunnet-aggregated",
  "meta": {
    "name": "Mega Subscription",
    "total_servers": 142,
    "sources": [
      {
        "id": "source_1",
        "name": "Premium Subscription",
        "url": "https://example.com/premium",
        "servers_count": 42,
        "last_update": "2026-06-20T12:00:00Z"
      }
    ],
    "updated": "2026-06-20T12:00:00Z"
  },
  "servers": [ ... ],
  "groups": [ ... ]
}
```

---

## 6. Validation Rules

1. `version` must be exactly `"2.0"`.
2. Every `server` must have unique `id`.
3. `port` must be in range 1-65535.
4. `priority` must be in range 1-10.
5. `subscription.url` must be a valid HTTPS URL.
6. `federation.panels[].host` must be a valid HTTPS URL.
7. Unknown fields should be ignored with a warning.
8. Circular `source` references are not allowed.

---

## 7. Migration from v1

- Add `version: "2.0"`.
- Move `profile` settings into `settings`.
- Rename `proxies` to `servers`.
- Add `federation` section if using federation.
- Convert old link params to new `params` object.
