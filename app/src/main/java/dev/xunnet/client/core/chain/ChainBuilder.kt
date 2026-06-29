package dev.xunnet.client.core.chain

import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.parser.XunnetLinkParser
import org.json.JSONArray
import org.json.JSONObject

/**
 * Chain mode — link multiple profiles so traffic flows:
 *   [App] → profile[0] → profile[1] → ... → Internet
 *
 * sing-box supports this via chained outbounds.
 */
class ChainBuilder(
    private val parser: XunnetLinkParser = XunnetLinkParser()
) {

    /**
     * Build a sing-box config that chains the given profiles.
     * Each profile becomes an outbound, the last one is the final exit,
     * the rule chain sends all traffic through outbound-chain.
     */
    fun buildChainedConfig(profiles: List<Profile>, listenPort: Int = 2080): String {
        require(profiles.isNotEmpty()) { "Chain must have at least one profile" }

        val root = JSONObject()
        root.put("log", JSONObject().put("level", "info"))

        // Inbound: mixed socks/http on localhost
        val inbound = JSONObject().apply {
            put("type", "mixed")
            put("listen", "127.0.0.1")
            put("listen_port", listenPort)
        }
        root.put("inbounds", JSONArray().put(inbound))

        // Outbounds: each profile + direct + block
        val outbounds = JSONArray()
        val outboundNames = mutableListOf<String>()
        profiles.forEachIndexed { index, profile ->
            val outbound = parser.toSingBoxOutbound(profile)
            val name = "chain-${index}"
            outbound.put("tag", name)
            // Intermediate hops detour through the next outbound
            if (index < profiles.size - 1) {
                outbound.put("detour", "chain-${index + 1}")
            }
            outbounds.put(outbound)
            outboundNames.add(name)
        }
        outbounds.put(JSONObject().put("type", "direct").put("tag", "direct"))
        outbounds.put(JSONObject().put("type", "block").put("tag", "block"))
        root.put("outbounds", outbounds)

        // Route everything through the first chain hop
        root.put("route", JSONObject()
            .put("final", "chain-0")
            .put("rules", JSONArray()
                .put(JSONObject()
                    .put("network", "udp")
                    .put("port", 53)
                    .put("outbound", "direct"))))

        return root.toString(2)
    }

    /**
     * Build a chained profile from a "chain://" URI:
     *   chain://profile1_link|profile2_link|profile3_link#name
     */
    fun fromChainUri(uri: String): Result<Pair<String, List<Profile>>> = runCatching {
        val noScheme = uri.removePrefix("chain://")
        val (beforeFragment, name) = if ('#' in noScheme) {
            val i = noScheme.indexOf('#')
            noScheme.substring(0, i) to noScheme.substring(i + 1)
        } else noScheme to ""

        val parts = beforeFragment.split('|').filter { it.isNotBlank() }
        val profiles = parts.map { parser.parse(it).getOrThrow() }
        java.net.URLDecoder.decode(name.ifEmpty { "Chain (${profiles.size})" }, "UTF-8") to profiles
    }

    /**
     * Serialize profiles back to a "chain://" URI for sharing.
     */
    fun toChainUri(name: String, profiles: List<Profile>): String {
        val encoded = profiles.joinToString("|") { parser.generate(it) }
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        return "chain://$encoded#$encodedName"
    }
}
