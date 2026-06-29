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
        if (trimmed.startsWith("chain://", ignoreCase = true)) {
            throw IllegalArgumentException("chain:// returns multiple profiles — use parseChain()")
        }
        when {
            trimmed.startsWith("xunnet://", ignoreCase = true) -> parseXunnet(trimmed)
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseSs(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("hysteria://", ignoreCase = true) -> parseHysteria1(trimmed)
            trimmed.startsWith("hysteria2://", ignoreCase = true) ||
            trimmed.startsWith("hy2://", ignoreCase = true) -> parseHysteria2(trimmed)
            trimmed.startsWith("tuic://", ignoreCase = true) -> parseTuic(trimmed)
            trimmed.startsWith("ssh://", ignoreCase = true) -> parseSsh(trimmed)
            trimmed.startsWith("naive+", ignoreCase = true) -> parseNaive(trimmed)
            trimmed.startsWith("wireguard://", ignoreCase = true) -> parseWireGuard(trimmed)
            trimmed.startsWith("awg://", ignoreCase = true) ||
            trimmed.startsWith("amneziawg://", ignoreCase = true) -> parseAmneziaWG(trimmed)
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
    // hysteria://host:port?protocol=udp&auth=...&...#name
    // ---------------------------------------------------------------
    private fun parseHysteria1(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("hysteria://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        // hysteria:// may have no @ — host:port is the authority
        val hostPort = authorityWithPath.substringAfter('@').ifEmpty { authorityWithPath }
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "hysteria",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params
        )
    }

    // ---------------------------------------------------------------
    // hysteria2://auth@host:port/?...#name  (or hy2://)
    // ---------------------------------------------------------------
    private fun parseHysteria2(link: String): Profile {
        val normalized = when {
            link.startsWith("hy2://", ignoreCase = true) -> link
            else -> link
        }
        val noScheme = normalized.removePrefixIgnoreCase("hysteria2://")
            .let { if (it.startsWith("//")) it else it }
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (auth, hostPort) = if ('@' in beforeFragment.substringBefore('?')) {
            beforeFragment.substringBefore('?').substringBefore('@') to beforeFragment.substringBefore('?').substringAfter('@')
        } else {
            "" to beforeFragment.substringBefore('?')
        }
        val params = parseQuery(queryStr)
        if (auth.isNotEmpty()) params["password"] = auth
        return buildProfile(
            name = name,
            protocol = "hysteria2",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params
        )
    }

    // ---------------------------------------------------------------
    // tuic://uuid:password@host:port/?...#name
    // ---------------------------------------------------------------
    private fun parseTuic(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("tuic://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (userInfo, hostPort) = splitAuthority(beforeFragment, expectedScheme = "tuic")
        val uuid = userInfo.substringBefore(':')
        val password = userInfo.substringAfter(':')
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "tuic",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params + mapOf("uuid" to uuid, "password" to password)
        )
    }

    // ---------------------------------------------------------------
    // ssh://user:password@host:port#name  (treated as TCP via sing-box ssh)
    // ---------------------------------------------------------------
    private fun parseSsh(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("ssh://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (userInfo, hostPort) = splitAuthority(beforeFragment, expectedScheme = "ssh")
        val (user, password) = userInfo.split(':', limit = 2).let {
            it[0] to it.getOrElse(1) { "" }
        }
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "ssh",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':', "22").toInt(),
            params = params + mapOf("user" to user, "password" to password)
        )
    }

    // ---------------------------------------------------------------
    // naive+https://user:password@host:port#name
    // ---------------------------------------------------------------
    private fun parseNaive(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("naive+")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (userInfo, hostPort) = splitAuthority(beforeFragment, expectedScheme = "naive")
        val (user, password) = userInfo.split(':', limit = 2).let {
            it[0] to it.getOrElse(1) { "" }
        }
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "naive",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params + mapOf("username" to user, "password" to password)
        )
    }

    // ---------------------------------------------------------------
    // awg:// or amneziawg:// — WireGuard with Amnezia obfuscation
    // Params: jc, jmin, jmax, s1, s2, h1, h2, h3, h4
    // ---------------------------------------------------------------
    private fun parseAmneziaWG(link: String): Profile {
        val profile = parseWireGuard(link)
        return profile.copy(protocol = "amneziawg")
    }

    // ---------------------------------------------------------------
    // wireguard://privatekey@address:port/?publickey=...&...#name
    // ---------------------------------------------------------------
    private fun parseWireGuard(link: String): Profile {
        val noScheme = link.removePrefixIgnoreCase("wireguard://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val (authorityWithPath, queryStr) = splitQuery(beforeFragment)
        val (privateKey, hostPort) = splitAuthority(beforeFragment, expectedScheme = "wireguard")
        val params = parseQuery(queryStr)
        return buildProfile(
            name = name,
            protocol = "wireguard",
            address = hostPort.substringBefore(':'),
            port = hostPort.substringAfter(':').toInt(),
            params = params + ("private_key" to privateKey)
        )
    }

    // ---------------------------------------------------------------
    // chain://profile1|profile2|profile3#name
    // Returns a list of profiles to chain in order.
    // ---------------------------------------------------------------
    fun parseChain(link: String): List<Profile> {
        val noScheme = link.removePrefixIgnoreCase("chain://")
        val (beforeFragment, name) = splitFragment(noScheme)
        val parts = beforeFragment.split('|').filter { it.isNotBlank() }
        require(parts.isNotEmpty()) { "chain:// must contain at least one profile" }
        return parts.map { parse(it).getOrThrow() }
            .also { if (name.isNotEmpty()) Unit /* name applied by caller */ }
    }

    /**
     * Generate a xunnet:// link for a single profile (the standard form).
     * For chain:// use [ChainBuilder.toChainUri].
     */
    fun toXunnetUri(profile: Profile): String = generate(profile)

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
            "hysteria" -> {
                outbound.put("auth_str", params.optString("auth", params.optString("auth_str", "")))
                outbound.put("up_mbps", params.optInt("upmbps", 50))
                outbound.put("down_mbps", params.optInt("downmbps", 100))
                val obfs = JSONObject().put("type", params.optString("obfs-type", "plain"))
                if (params.has("obfs-password")) obfs.put("password", params.optString("obfs-password"))
                outbound.put("obfs", obfs)
            }
            "hysteria2", "hy2" -> {
                outbound.put("password", params.optString("password", ""))
            }
            "tuic" -> {
                outbound.put("uuid", params.optString("uuid", ""))
                outbound.put("password", params.optString("password", ""))
                outbound.put("congestion_control", params.optString("congestion_control", "cubic"))
                outbound.put("udp_relay_mode", params.optString("udp_relay_mode", "native"))
            }
            "ssh" -> {
                outbound.put("user", params.optString("user", ""))
                outbound.put("password", params.optString("password", ""))
                params.optString("private_key").takeIf { it.isNotEmpty() }?.let { outbound.put("private_key", it) }
            }
            "naive" -> {
                outbound.put("username", params.optString("username", ""))
                outbound.put("password", params.optString("password", ""))
            }
            "wireguard" -> {
                outbound.put("private_key", params.optString("private_key", ""))
                outbound.put("peer_public_key", params.optString("publickey", ""))
                outbound.put("local_address", JSONArray().put(params.optString("address", "10.0.0.2/32")))
            }
            "amneziawg" -> {
                // AmneziaWG is sing-box-compatible WireGuard with obfuscation
                outbound.put("type", "wireguard")
                outbound.put("private_key", params.optString("private_key", ""))
                outbound.put("peer_public_key", params.optString("publickey", ""))
                outbound.put("local_address", JSONArray().put(params.optString("address", "10.0.0.2/32")))
                val amnezia = JSONObject().apply {
                    if (params.has("jc")) put("Jc", params.optInt("jc"))
                    if (params.has("jmin")) put("Jmin", params.optInt("jmin"))
                    if (params.has("jmax")) put("Jmax", params.optInt("jmax"))
                    if (params.has("s1")) put("S1", params.optInt("s1"))
                    if (params.has("s2")) put("S2", params.optInt("s2"))
                    if (params.has("h1")) put("H1", params.optString("h1"))
                    if (params.has("h2")) put("H2", params.optString("h2"))
                    if (params.has("h3")) put("H3", params.optString("h3"))
                    if (params.has("h4")) put("H4", params.optString("h4"))
                }
                outbound.put("amnezia", amnezia)
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
        "hysteria" -> "hysteria"
        "hysteria2", "hy2" -> "hysteria2"
        "tuic" -> "tuic"
        "ssh" -> "ssh"
        "naive" -> "naive"
        "wireguard" -> "wireguard"
        "amneziawg", "awg" -> "wireguard"  // AmneziaWG uses wireguard type with amnezia field
        else -> protocol
    }
}
