package dev.xunnet.client.core.tunnel

import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Universal tunnel backend interface. Implementations wrap native or binary
 * VPN/proxy engines (sing-box, wireguard-go, xray, etc.) and expose the same API.
 *
 * Lifecycle: [start] -> connect -> [stats] flow emits ticks -> [stop] disconnects.
 */
interface TunnelAdapter {
    /** Human-readable backend name (e.g. "sing-box", "wireguard-go"). */
    val name: String

    /** Connection state for UI. */
    val state: StateFlow<TunnelState>

    /** Live stats (bytes/sec, totals). */
    val stats: Flow<Stats>

    /** Build the engine config from profile + chain + routing rules. Returns JSON/YAML/etc. */
    fun buildConfig(
        profile: Profile,
        routing: RoutingConfig = RoutingConfig.Default
    ): String

    /** Start the tunnel with the given profile. Idempotent. */
    suspend fun start(profile: Profile, routing: RoutingConfig = RoutingConfig.Default): Result<Unit>

    /** Stop the tunnel. Idempotent. */
    suspend fun stop(): Result<Unit>

    /** Current tunnel state. */
    fun isRunning(): Boolean = state.value.running
}

data class TunnelState(
    val running: Boolean = false,
    val starting: Boolean = false,
    val error: String? = null,
    val activeProfileId: String? = null
)

/**
 * Routing configuration shared across all adapters.
 * - bypass: CIDRs to send DIRECT (no tunnel)
 * - block:  CIDRs to BLOCK (used for ad-block lists)
 * - dns:    DNS servers
 */
data class RoutingConfig(
    val bypass: List<String> = emptyList(),
    val block: List<String> = emptyList(),
    val dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val blockQuic: Boolean = false
) {
    companion object {
        /** Direct everything through the tunnel. */
        val Default = RoutingConfig()

        /** RU-friendly: bypass Russian sites (banks, gov, services) for speed. */
        val Russia = RoutingConfig(
            bypass = SplitTunneling.russia,
            dns = listOf("77.88.8.8", "1.1.1.1")
        )

        /** Streaming-friendly: bypass Netflix/YouTube/AKAMAI for native quality. */
        val Streaming = RoutingConfig(
            bypass = SplitTunneling.streaming,
            dns = listOf("1.1.1.1")
        )

        /** Games-friendly: bypass game servers for low latency, block QUIC to avoid spikes. */
        val Gaming = RoutingConfig(
            bypass = SplitTunneling.gaming,
            dns = listOf("1.1.1.1"),
            blockQuic = true
        )
    }
}
