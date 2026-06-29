package dev.xunnet.client.core.tunnel

import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Selects and exposes the active [TunnelAdapter]. For now only [SingBoxAdapter]
 * is fully wired; [WireGuardAdapter] is a stub.
 */
class TunnelManager(
    private val adapters: List<TunnelAdapter> = listOf(SingBoxAdapter(), WireGuardAdapter())
) {
    private val _activeBackend = MutableStateFlow("sing-box")
    val activeBackend: StateFlow<String> = _activeBackend

    val current: TunnelAdapter
        get() = adapters.firstOrNull { it.name == _activeBackend.value } ?: adapters.first()

    val state: StateFlow<TunnelState> get() = current.state
    val stats: Flow<Stats> get() = current.stats

    fun selectBackend(name: String) {
        if (adapters.any { it.name == name }) _activeBackend.value = name
    }

    fun available(): List<String> = adapters.map { it.name }

    suspend fun start(profile: Profile, routing: RoutingConfig = RoutingConfig.Default): Result<Unit> {
        // Stop others first
        adapters.filter { it.name != current.name && it.state.value.running }.forEach {
            it.stop()
        }
        return current.start(profile, routing)
    }

    suspend fun stop(): Result<Unit> = current.stop()
}
