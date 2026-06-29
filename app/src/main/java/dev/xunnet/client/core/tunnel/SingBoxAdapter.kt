package dev.xunnet.client.core.tunnel

import android.util.Log
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import dev.xunnet.client.core.domain.parser.XunnetLinkParser
import dev.xunnet.client.core.vpn.SingBoxCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * [TunnelAdapter] backed by sing-box. Wraps [SingBoxCore] and translates
 * [RoutingConfig] (split tunneling, DNS) into sing-box's JSON config format.
 */
class SingBoxAdapter(
    private val core: SingBoxCore = SingBoxCore(),
    private val parser: XunnetLinkParser = XunnetLinkParser()
) : TunnelAdapter {

    override val name: String = "sing-box"
    private val _state = MutableStateFlow(TunnelState())
    override val state: StateFlow<TunnelState> = _state.asStateFlow()
    override val stats: Flow<Stats> = core.stats

    init {
        // Sync state from underlying core
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            core.stats.collectLatest { s ->
                _state.value = _state.value.copy(
                    running = s.connected,
                    activeProfileId = s.activeProfileId
                )
            }
        }
    }

    override fun buildConfig(profile: Profile, routing: RoutingConfig): String {
        val root = JSONObject()

        // log
        root.put("log", JSONObject().put("level", "info"))

        // inbounds: tun
        val tun = JSONObject().apply {
            put("type", "tun")
            put("inet4_address", "172.19.0.1/30")
            put("inet6_address", "fdfe:dcba:9876::1/126")
            put("auto_route", true)
            put("strict_route", true)
            put("stack", "system")
            put("sniff", true)
        }
        root.put("inbounds", JSONArray().put(tun))

        // outbounds: proxy + direct + block
        val proxy = parser.toSingBoxOutbound(profile)
        val direct = JSONObject().put("type", "direct")
        val block = JSONObject().put("type", "block")
        root.put("outbounds", JSONArray().put(proxy).put(direct).put(block))

        // DNS
        val dns = JSONObject().apply {
            put("servers", JSONArray().put(routing.dns))
            put("strategy", "ipv4_only")
        }
        root.put("dns", dns)

        // route rules
        val rules = JSONArray()
        routing.bypass.forEach { cidr ->
            rules.put(JSONObject()
                .put("ip_cidr", JSONArray().put(cidr))
                .put("outbound", "direct"))
        }
        routing.block.forEach { cidr ->
            rules.put(JSONObject()
                .put("ip_cidr", JSONArray().put(cidr))
                .put("outbound", "block"))
        }
        if (routing.blockQuic) {
            rules.put(JSONObject()
                .put("network", "udp")
                .put("port", 443)
                .put("outbound", "block"))
        }
        val route = JSONObject()
            .put("auto_detect_interface", true)
            .put("final", "proxy")
            .put("rules", rules)
        root.put("route", route)

        // experimental: clash API for stats
        root.put("experimental", JSONObject()
            .put("clash_api", JSONObject()
                .put("external_controller", "127.0.0.1:9090")
                .put("default_mode", "rule"))
            .put("cache_file", "/data/data/dev.xunnet.client/cache/sing-box.db"))

        return root.toString(2)
    }

    override suspend fun start(profile: Profile, routing: RoutingConfig): Result<Unit> {
        _state.value = _state.value.copy(starting = true, error = null, activeProfileId = profile.id)
        val config = buildConfig(profile, routing)
        Log.i(name, "Starting with profile ${profile.name}")
        val result = core.startRaw(config)
        _state.value = _state.value.copy(
            running = result.isSuccess,
            starting = false,
            error = result.exceptionOrNull()?.message
        )
        return result
    }

    override suspend fun stop(): Result<Unit> {
        _state.value = _state.value.copy(starting = true)
        val result = core.stop()
        _state.value = TunnelState(running = false)
        return result
    }

    override fun isRunning(): Boolean = state.value.running
}
