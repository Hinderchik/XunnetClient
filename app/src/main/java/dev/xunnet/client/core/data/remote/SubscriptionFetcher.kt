package dev.xunnet.client.core.data.remote

import android.util.Base64
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.parser.LinkParser
import org.json.JSONArray
import org.json.JSONObject

/**
 * Subscriptions can be in different formats:
 *   1. Plain text: one link per line (vless://, vmess://, ss://, trojan://, xunnet://)
 *   2. Base64: same lines but base64-encoded
 *   3. JSON: Clash-style proxy list ({ "proxies": [...] })
 *   4. YAML: Clash config — skipped here (handled separately if needed)
 */
class SubscriptionFetcher(private val parser: LinkParser) {

    fun parse(raw: String): List<Profile> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        // JSON?
        if (trimmed.startsWith("{")) return parseClashJson(trimmed)

        // Base64? — try if it has no recognizable scheme prefix
        val hasLineScheme = trimmed.lineSequence().any { line ->
            line.trim().matches(Regex("^(xunnet|vless|trojan|ss|vmess|xuncrypt)://", RegexOption.IGNORE_CASE))
        }
        if (!hasLineScheme) {
            val decoded = runCatching {
                String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull()
            if (decoded != null && decoded.contains("://")) {
                return parsePlain(decoded)
            }
        }

        return parsePlain(trimmed)
    }

    private fun parsePlain(body: String): List<Profile> {
        return body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                parser.parse(line).getOrElse {
                    // skip unparseable lines silently
                    null
                }
            }
    }

    private fun parseClashJson(body: String): List<Profile> {
        return runCatching {
            val root = org.json.JSONObject(body)
            val proxies = root.optJSONArray("proxies") ?: JSONArray()
            val out = mutableListOf<Profile>()
            for (i in 0 until proxies.length()) {
                val node = proxies.getJSONObject(i)
                val type = node.optString("type")
                // skip selector/urltest/fallback groups
                if (type in setOf("selector", "urltest", "fallback", "load-balance")) continue
                val profile = clashNodeToProfile(node) ?: continue
                out += profile
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun clashNodeToProfile(node: org.json.JSONObject): Profile? {
        val type = node.optString("type")
        val name = node.optString("name", "unknown")
        val server = node.optString("server")
        val port = node.optInt("port", 0)
        if (server.isEmpty() || port == 0) return null

        val params = JSONObject()
        when (type) {
            "vless" -> {
                params.put("uuid", node.optString("uuid"))
                node.optString("flow").takeIf { it.isNotEmpty() }?.let { params.put("flow", it) }
            }
            "vmess" -> {
                params.put("uuid", node.optString("uuid"))
                params.put("alterId", node.optInt("alterId", 0))
                params.put("security", node.optString("cipher", "auto"))
            }
            "trojan" -> params.put("password", node.optString("password"))
            "ss", "shadowsocks" -> {
                params.put("method", node.optString("cipher"))
                params.put("password", node.optString("password"))
            }
            else -> return null
        }

        // TLS
        if (node.optBoolean("tls", false) || node.has("tls")) {
            val sni = node.optString("sni").ifEmpty { node.optString("servername") }
            if (sni.isNotEmpty()) params.put("sni", sni)
            node.optString("alpn").takeIf { it.isNotEmpty() }?.let { params.put("alpn", it) }
        }

        // Transport
        node.optString("network").takeIf { it.isNotEmpty() }?.let { params.put("network", it) }
        node.optJSONObject("ws-opts")?.let { ws ->
            ws.optString("path").takeIf { it.isNotEmpty() }?.let { params.put("path", it) }
            ws.optJSONObject("headers")?.optString("Host")?.takeIf { !it.isNullOrEmpty() }?.let { params.put("host", it) }
        }
        node.optJSONObject("grpc-opts")?.let { g ->
            g.optString("grpc-service-name").takeIf { it.isNotEmpty() }?.let { params.put("path", it) }
        }

        return Profile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            protocol = when (type) { "ss" -> "shadowsocks"; else -> type },
            address = server,
            port = port,
            paramsJson = params.toString()
        )
    }
}
