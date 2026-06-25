# Xunnet Link Format Specification

**Version:** 2.0  
**Status:** Draft  
**License:** GPL-3.0

---

## 1. Overview

Xunnet Link Format is a URI-based format for sharing single VPN server configurations, groups, settings, ephemeral links, encrypted links, and federated panel references.

All Xunnet links use the following URI scheme prefix:

```
xunnet://    single server
xungroup://  subscription/group reference
xunset://    profile settings
xunephem://  ephemeral (one-time) link
xuncrypt://  encrypted link
xunfed://    federated panel reference
```

---

## 2. Single Server Link

### 2.1 Syntax

```
xunnet://[method]@[host]:[port]/?[parameters]#[name]
```

### 2.2 BNF Grammar

```bnf
<XunnetLink>   ::= "xunnet://" <Method> "@" <Host> ":" <Port> "/?" <Params> "#" <Name>
<Method>       ::= <String>
<Host>         ::= <Domain> | <IPv4> | <IPv6>
<Port>         ::= <Digit>+ /* 1-65535 */
<Params>       ::= <Param> ("&" <Param>)*
<Param>        ::= <Key> "=" <Value>
<Key>          ::= <String>
<Value>        ::= <String> | <Boolean> | <Integer>
<Name>         ::= <String> /* URL-encoded */
```

### 2.3 Validation regex

```regex
^xunnet://([^@]+)@([^:]+):(\d+)/\?(.+)#(.+)$
```

### 2.4 Parameter reference

| Parameter | Required | Type | Description | Example |
|-----------|----------|------|-------------|---------|
| `protocol` | **Yes** | string | Protocol type | `vless`, `vmess`, `trojan`, `shadowsocks` |
| `uuid` | Conditional | string | User UUID | `123e4567-e89b-12d3-a456-426614174000` |
| `password` | Conditional | string | Password | `mySecurePass123` |
| `auth` | No | string | Auth method | `psk`, `tls` |
| `alterId` | No | int | VMess alterId | `0` |
| `security` | No | string | Encryption method | `auto`, `aes-128-gcm`, `chacha20-poly1305` |
| `type` | No | string | Transport | `tcp`, `ws`, `grpc`, `kcp`, `quic`, `httpupgrade` |
| `host` | No | string | Host / SNI | `cdn.xun.net` |
| `path` | No | string | WebSocket / gRPC path | `/ws`, `/grpc` |
| `tls` | No | boolean | Enable TLS | `true`, `false` |
| `sni` | No | string | Server Name Indication | `us.xun.net` |
| `fp` | No | string | Browser fingerprint | `chrome`, `firefox`, `safari`, `random` |
| `pbk` | Conditional | string | REALITY public key | `ABcdEFgh...` |
| `sid` | Conditional | string | REALITY short ID | `1234567890abcdef` |
| `publicKey` | Conditional | string | WireGuard public key | `abc123...` |
| `privateKey` | Conditional | string | WireGuard private key (export only) | `xyz789...` |
| `address` | Conditional | string | WireGuard address | `10.0.0.2/32` |
| `dns` | No | string | DNS server | `1.1.1.1` |
| `obfs` | No | string | Obfuscation | `http`, `tls` |
| `obfs-host` | No | string | Obfuscation host | `www.bing.com` |
| `obfs-uri` | No | string | Obfuscation URI | `/` |
| `mux` | No | boolean | Enable MUX | `true`, `false` |
| `mux-concurrency` | No | int | MUX max connections | `8` |
| `hop` | No | string | Multi-hop chain | `s1:443,s2:443` |
| `expire` | No | string | Expiry ISO 8601 | `2026-12-31T23:59:59Z` |
| `tag` | No | string | Comma-separated tags | `premium,us,no-logs` |
| `priority` | No | int | Priority 1-10 | `10` |
| `remark` | No | string | Note | `My favorite server` |
| `flow` | No | string | XTLS flow | `xtls-rprx-vision` |
| `packetEncoding` | No | string | Packet encoding | `xudp` |
| `congestion` | No | string | Hysteria2 congestion | `bbr`, `cubic` |
| `downlinkMbps` | No | int | Download limit | `100` |
| `uplinkMbps` | No | int | Upload limit | `20` |
| `fingerprint` | No | string | TLS fingerprint | `chrome`, `firefox`, `random` |
| `utls` | No | string | uTLS imitation | `chrome_120`, `firefox_120` |
| `panel_id` | No | string | Source panel ID | `panel_us_01` |

### 2.5 Example

```
xunnet://chacha20-ietf-poly1305@us1.xun.net:443/?protocol=vless&uuid=123e4567-e89b-12d3-a456-426614174000&flow=xtls-rprx-vision&type=tcp&tls=true&sni=us1.xun.net&fp=chrome&pbk=ABcdEFghIJKLmnopQRstuvWXyz1234567890&sid=1234567890abcdef&tag=premium,us&panel_id=panel_us_01#US_VIP_01
```

---

## 3. Group / Subscription Link

### 3.1 Syntax

```
xungroup://[group_name]/?url=[subscription_url]&update=[seconds]&tag=[tags]&enabled=[bool]
```

### 3.2 Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `url` | Yes | Subscription URL |
| `update` | No | Auto-update interval in seconds (default 3600) |
| `tag` | No | Comma-separated tags |
| `enabled` | No | Default enabled state |

### 3.3 Example

```
xungroup://Premium/?url=https%3A%2F%2Fexample.com%2Fsub.json&update=3600&tag=premium,us&enabled=true
```

---

## 4. Profile Settings Link

### 4.1 Syntax

```
xunset://default/?mode=[mode]&dns=[dns]&tun-stack=[stack]&tun-address=[cidr]&allow-apps=[apps]&block-apps=[apps]
```

### 4.2 Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `mode` | `tun`, `proxy`, `socks` | `tun` |
| `dns` | DNS servers comma-separated | `1.1.1.1,8.8.8.8` |
| `tun-stack` | `gvisor`, `system` | `gvisor` |
| `tun-address` | TUN interface address | `172.19.0.1/24` |
| `allow-apps` | Allowed app package names | `com.google.android.youtube` |
| `block-apps` | Blocked app package names | `com.facebook.katana` |

### 4.3 Example

```
xunset://default/?mode=tun&dns=1.1.1.1&tun-stack=gvisor&tun-address=172.19.0.1/24&allow-apps=com.google.android.youtube,com.spotify.music&block-apps=com.facebook.katana
```

---

## 5. Ephemeral Link

### 5.1 Syntax

```
xunephem://[host]:[port]/?protocol=[proto]&uuid=[uuid]&expire=[iso8601]&max-uses=[int]
```

### 5.2 Behavior

- Link becomes invalid after `expire` timestamp.
- Link becomes invalid after `max-uses` activations.
- Server must track usage count.

### 5.3 Example

```
xunephem://temp.xun.net:443/?protocol=vmess&uuid=123e4567-e89b-12d3-a456-426614174000&expire=2026-07-01T00:00:00Z&max-uses=5
```

---

## 6. Encrypted Link (XunCrypt)

### 6.1 Syntax

```
xuncrypt://[base64_encrypted_data]?salt=[base64_salt]
```

### 6.2 Encryption

- Algorithm: AES-256-GCM
- Key derivation: PBKDF2-HMAC-SHA256, 100000 iterations
- Salt: 16 random bytes, base64-encoded
- Nonce/IV: 12 random bytes, prepended to ciphertext
- Tag: GCM authentication tag, appended to ciphertext

### 6.3 Encoding steps

1. Serialize server parameters to query string.
2. Encrypt with AES-256-GCM using password-derived key.
3. Encode `nonce + ciphertext + tag` as base64.
4. Build URI: `xuncrypt://[base64]?salt=[base64_salt]`.

### 6.4 Example

```
xuncrypt://AbCdEfGhIjKlMnOpQrStUvWxYz1234567890...?salt=MTIzNDU2Nzg5MGFiY2RlZg==
```

---

## 7. Federated Panel Link

### 7.1 Syntax

```
xunfed://[panel_id]@[panel_host]:[panel_port]/?token=[api_key]&sync=[bool]
```

### 7.2 Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `token` | Yes | API key for federation access |
| `sync` | No | Enable automatic synchronization |

### 7.3 Example

```
xunfed://panel_us_01@panel-us.xunnet.dev:443/?token=fed_xxxxxxxxxxxxx&sync=true
```

---

## 8. Parsing Rules

1. Scheme must be recognized.
2. Host must be a valid domain, IPv4, or bracketed IPv6.
3. Port must be in range 1-65535.
4. All parameter keys must be known or ignored with warning.
5. Required parameters must be present based on protocol.
6. Name fragment must be URL-decoded.
7. Duplicate keys: last value wins.

---

## 9. Generation Rules

1. Always URL-encode parameter values.
2. Use standard port omission only for well-known defaults if desired.
3. Include `panel_id` when link originates from a federated panel.
4. Do not include `privateKey` unless explicitly exporting full config.
5. Use ISO 8601 UTC for timestamps.

---

## 10. Compatibility

Xunnet clients must also parse the following third-party link formats:

- `ss://`
- `trojan://`
- `vmess://`
- `vless://`
- `hysteria://` / `hysteria2://`
- `tuic://`
- `wireguard://`
- `ssh://`
- `socks://` / `socks5://`
- `http://` / `https://` (proxy links)
