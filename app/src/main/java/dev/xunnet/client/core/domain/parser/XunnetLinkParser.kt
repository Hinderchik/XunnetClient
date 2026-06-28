package dev.xunnet.client.core.domain.parser

import android.util.Base64
import dev.xunnet.client.core.domain.model.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

/**
 * Parses proxy links (xunnet://, vmess://, vless://, trojan://, ss://) into Profile,
 * and generates sing-box config YAML from Profile.
 *
 * Supported formats:
 *   xunnet://[method@]host:port/?protocol=vless&uuid=...&...#Name
 *   vless://uuid@host:port/?...&type=tcp#Name
 *   trojan://password@host:port/?sni=...#Name
 *   ss://base64(method:password)@host:port#Name
 *   vmess://base64(json)#Name
 */
class XunnetLinkParser : LinkParser {

    override fun parse(link: String): Result<Profile> = runCatching {
        val trimmed = link.trim()
        when {
            trimmed.startsWith("xunnet://", ignoreCase = true) -> parseXunnet(trimmed)
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseSs(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("xuncrypt://", ignoreCase = true) ->
                throw IllegalArgumentException("xuncrypt requires password — use decrypt()")
            else -> throw IllegalArgumentException("Unsupported link scheme: ${trimmed.substringBefore("://")}")
        }
    }

    override fun parseFile(file: File): Result<List<Profile>> = runCatching {
        val lines = file.readLines().filter { it.isNotBlank() }
        val profiles = mutableListOf<Profile>()
        val errors = mutableListOf<String>()
        for (line in lines) {
            val r = parse(line)
            r.fold(
                onSuccess = { profiles += it },
                onFailure = { errors += "${line.take(40)}: ${it.message}" }
            )
        }
        if (profiles.isEmpty() && errors.isNotEmpty()) {
            throw IllegalStateException("No valid profiles in file. Errors: ${errors.joinToString("; ")}")
        }
        profiles
    }

    override fun generate(profile: Profile): String {
        val sb = StringBuilder("xunnet://")
        // method@host:port — method is optional
        if (profile.encryption != null) {
            sb.append(profile.encryption).append('@')
        }
        sb.append(profile.address).append(':').append(profile.port)
        sb.append('/')
        val query = buildMap<String, String> {
            put("protocol", profile.protocol)
            // Merge paramsJson keys into query
            runCatching {
                val jo = JSONObject(profile.paramsJson)
                jo.keys().forEach { k -> put(k, jo.get(k).toString()) }
            }
        }
        if (query.isNotEmpty()) {
            sb.append('?')
            sb.append(query.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            })
        }
        sb.append('#').append(URLEncoder.encode(profile.name, "UTF-8"))
        return sb.toString()
    }

    override fun generateXunCrypt(profile: Profile, password: String): String {
        val plain = generate(profile).toByteArray(Charsets.UTF_8)
        val encrypted = XunCrypt.encrypt(plain, password)
        return "xuncrypt://" + Base64.encodeToString(encrypted, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    // ---------------------------------------------------------------
    // xunnet://[method@]host:port/?protocol=X&...#name
    // ---------------------------------------------------------------
    private fun parseXunnet(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("xunnet://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (method, hostPort) = splitAuthorityForXunnet(authorityWithPath)

        val host = hostPort.substringBefore(':')
        val port = hostPort.substringAfter(':').toInt()

        val params = parseQuery(queryStr)
        val protocol = params.remove("protocol")
            ?: throw IllegalArgumentException("Missing 'protocol' in xunnet link")

        val paramsJson = JSONObject(params as Map<String, String>).toString()

        return Profile(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = protocol,
            address = host,
            port = port,
            paramsJson = paramsJson,
            encryption = method
        )
    }

    private fun splitAuthorityForXunnet(s: String): Pair<String?, String> {
        val slash = s.indexOf('/')
        val authority = if (slash >= 0) s.substring(0, slash) else s
        return if ('@' in authority) {
            val method = authority.substringBefore('@')
            val hostPort = authority.substringAfter('@')
            method to hostPort
        } else {
            null to authority
        }
    }

    // ---------------------------------------------------------------
    // vless://uuid@host:port/?...&type=tcp#name
    // ---------------------------------------------------------------
    private fun parseVless(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("vless://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (uuid, hostPort) = splitAuthority(beforeFragment, expectedScheme = "vless")
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "vless",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params + ("uuid" to uuid)
        )
    }

    // ---------------------------------------------------------------
    // trojan://password@host:port/?sni=...#name
    // ---------------------------------------------------------------
    private fun parseTrojan(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("trojan://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (password, hostPort) = splitAuthority(beforeFragment, expectedScheme = "trojan")
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "trojan",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params + ("password" to password)
        )
    }

    // ---------------------------------------------------------------
    // ss://base64(method:password)@host:port#name   (SIP002)
    // ---------------------------------------------------------------
    private fun parseSs(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("ss://")
        val (beforeFragment, name) = splitFragment(noScheme)

        // SIP002: userinfo@host:port#name — userinfo is base64(method:password)
        if ('@' in beforeFragment) {
            val (userInfoRaw, hostPort) = splitAuthority(beforeFragment, expectedScheme = "ss")
            val methodPassword = runCatching {
                String(Base64.decode(userInfoRaw, Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull() ?: userInfoRaw
            val (method, password) = methodPassword.split(':', limit = 2).let {
                it[0] to it.getOrElse(1) { "" }
            }
            return buildProfile(
                name = name,
                protocol = "shadowsocks",
                address = hostPort.substringBefore(':'),
                port = hostPort.substringAfter(':').toInt(),
                method = method,
                password = password
            )
        }

        // Legacy: ss://base64(method:password@host:port)#name
        val decoded = runCatching {
            String(Base64.decode(noScheme.substringBefore('#'), Base64.DEFAULT), Charsets.UTF_8)
        }.getOrThrow()
        // decoded like "method:password@host:port"
        val (mp, hostPort) = decoded.split('@', limit = 2).let {
            it[0] to it.getOrElse(1) { throw IllegalArgumentException("Bad ss link") }
        }
        val (method, password) = mp.split(':', limit = 2).let {
            it[0] to it.getOrElse(1) { "" }
        }
        return buildProfile(
            name = name,
            protocol = "shadowsocks",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            method = method,
            password = password
        )
    }

    // ---------------------------------------------------------------
    // vmess://base64(json)#name
    // ---------------------------------------------------------------
    private fun parseVmess(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("vmess://")
        val (payload, name) = splitFragment(noScheme)
        val jsonStr = runCatching {
            String(Base64.decode(payload, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrThrow()
        val jo = JSONObject(jsonStr)
        val params = mapOf(
            "uuid" to jo.optString("id"),
            "alterId" to jo.optString("aid", "0"),
            "security" to jo.optString("scy", "auto"),
            "network" to jo.optString("net", "tcp"),
            "tls" to jo.optString("tls", "")
        ).filterValues { it.isNotEmpty() }
        return buildProfile(
            name = name.ifEmpty { jo.optString("ps") },
            protocol = "vmess",
            address = jo.optString("add"),
            port = jo.optInt("port"),
            params = params
        )
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun buildProfile(
        name: String,
        protocol: String,
        address: String,
        port: Int,
        params: Map<String, String> = emptyMap(),
        method: String? = null,
        password: String? = null
    ): Profile {
        val merged = params.toMutableMap()
        if (method != null) merged["method"] = method
        if (password != null) merged["password"] = password
        return Profile(
            id = UUID.randomUUID().toString(),
            name = URLDecoder.decode(name.ifEmpty { "${address}:${port}" }, "UTF-8"),
            protocol = protocol,
            address = address,
            port = port,
            paramsJson = JSONObject(merged as Map<String, Any?>).toString()
        )
    }

    private fun splitFragment(s: String): Pair<String, String> {
        val hash = s.indexOf('#')
        return if (hash >= 0) s.substring(0, hash) to s.substring(hash + 1)
        else s to ""
    }

    private fun splitQuery(s: String): Pair<String, String> {
        val q = s.indexOf('?')
        return if (q >= 0) s.substring(0, q) to s.substring(q + 1)
        else s to ""
    }

    private fun splitAuthority(s: String, expectedScheme: String): Pair<String, String> {
        val (authority, _) = splitQuery(s)
        return if ('@' in authority) {
            authority.substringBefore('@') to authority.substringAfter('@')
        } else {
            throw IllegalArgumentException("$expectedScheme link missing '@' in authority")
        }
    }

    private fun parseQuery(q: String): MutableMap<String, String> {
        if (q.isEmpty()) return mutableMapOf()
        return q.split('&')
            .filter { it.isNotEmpty() }
            .associate {
                val eq = it.indexOf('=')
                if (eq < 0) URLDecoder.decode(it, "UTF-8") to ""
                else URLDecoder.decode(it.substring(0, eq), "UTF-8") to
                        URLDecoder.decode(it.substring(eq + 1), "UTF-8")
            }
            .toMutableMap()
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

    /**
     * Generate sing-box outbound JSON for a Profile. Used by SingBoxCore.
     */
    fun toSingBoxOutbound(profile: Profile): JSONObject {
        val params = runCatching { JSONObject(profile.paramsJson) }.getOrDefault(JSONObject())
        val outbound = JSONObject()
        outbound.put("type", singBoxType(profile.protocol))
        outbound.put("tag", "proxy")

        val server = JSONObject()
        server.put("server", profile.address)
        server.put("server_port", profile.port)
        outbound.put("server", server.getJSONObject("server"))
        outbound.put("server_port", server.getInt("server_port"))

        when (profile.protocol.lowercase()) {
            "vless" -> {
                outbound.put("uuid", params.optString("uuid"))
                outbound.put("flow", params.optString("flow", ""))
            }
            "vmess" -> {
                outbound.put("uuid", params.optString("uuid"))
                outbound.put("alter_id", params.optInt("alterId", 0))
                outbound.put("security", params.optString("security", "auto"))
            }
            "trojan" -> {
                outbound.put("password", params.optString("password"))
            }
            "shadowsocks", "ss" -> {
                outbound.put("method", params.optString("method", profile.encryption ?: "chacha20-ietf-poly1305"))
                outbound.put("password", params.optString("password"))
            }
        }

        // TLS
        if (params.optBoolean("tls", false) || params.optString("security").contains("tls")) {
            val tls = JSONObject()
            tls.put("enabled", true)
            if (params.has("sni")) tls.put("server_name", params.optString("sni"))
            if (params.has("alpn")) tls.put("alpn", JSONArray(params.optString("alpn").split(",")))
            outbound.put("tls", tls)
        }

        // Transport
        val network = params.optString("network", params.optString("type", "tcp"))
        if (network != "tcp") {
            val transport = JSONObject().put("type", network)
            if (params.has("path")) transport.put("path", params.optString("path"))
            if (params.has("host")) transport.put("host", params.optString("host"))
            outbound.put("transport", transport)
        }

        return outbound
    }

    private fun singBoxType(protocol: String): String = when (protocol.lowercase()) {
        "ss", "shadowsocks" -> "shadowsocks"
        "vless" -> "vless"
        "vmess" -> "vmess"
        "trojan" -> "trojan"
        else -> protocol
    }
}
