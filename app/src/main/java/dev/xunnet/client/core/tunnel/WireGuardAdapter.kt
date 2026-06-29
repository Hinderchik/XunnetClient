package dev.xunnet.client.core.tunnel

import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * [TunnelAdapter] for WireGuard.
 *
 * NOTE: stub — full implementation requires either:
 *   - `wg-quick` shell-out (desktop), or
 *   - `wireguard-go` JNI binding (Android, via `tun2socks` or libwg-go).
 *
 * The config builder is implemented (it just produces the standard wg-quick INI).
 */
class WireGuardAdapter : TunnelAdapter {

    override val name: String = "wireguard"
    private val _state = MutableStateFlow(TunnelState())
    override val state: StateFlow<TunnelState> = _state.asStateFlow()
    override val stats: Flow<Stats> = MutableStateFlow(Stats())

    override fun buildConfig(profile: Profile, routing: RoutingConfig): String {
        // Build wg-quick INI
        val params = runCatching { JSONObject(profile.paramsJson) }.getOrDefault(JSONObject())
        val sb = StringBuilder("[Interface]\n")
        sb.append("PrivateKey = ${params.optString("private_key")}\n")
        params.optString("address").takeIf { it.isNotEmpty() }?.let {
            sb.append("Address = $it\n")
        }
        params.optString("dns").takeIf { it.isNotEmpty() }?.let {
            sb.append("DNS = $it\n")
        }
        sb.append("\n[Peer]\n")
        sb.append("PublicKey = ${params.optString("peer_public_key")}\n")
        sb.append("Endpoint = ${profile.address}:${profile.port}\n")
        sb.append("AllowedIPs = 0.0.0.0/0, ::/0\n")
        sb.append("PersistentKeepalive = 25\n")
        return sb.toString()
    }

    override suspend fun start(profile: Profile, routing: RoutingConfig): Result<Unit> {
        // TODO: shell out to wg-quick (desktop) or call JNI libwg-go (Android)
        return Result.failure(NotImplementedError("WireGuard adapter not yet wired to engine"))
    }

    override suspend fun stop(): Result<Unit> {
        _state.value = TunnelState()
        return Result.success(Unit)
    }
}
