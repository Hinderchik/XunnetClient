package dev.xunnet.client.core.health

import android.util.Log
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.tunnel.TunnelAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pings the active profile every [intervalMs] via TCP connect to its host:port.
 * Quality levels:
 *   - GREEN:  latency <= 200 ms, no recent failures
 *   - YELLOW: 200 < latency <= 1000 ms, OR 1 failed attempt in last 3
 *   - RED:    latency > 1000 ms, OR 2+ consecutive failures
 *
 * Auto-failover: when RED persists for [failoverAfterMs] ms, swaps to the next
 * enabled profile in [profiles] (cyclic, never the same one).
 */
class HealthChecker(
    private val adapter: TunnelAdapter,
    private val intervalMs: Long = 5_000L,
    private val pingTimeoutMs: Int = 2_500,
    private val failoverAfterMs: Long = 15_000L,
    private val probeTarget: String = "1.1.1.1",
    private val probePort: Int = 53
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val _quality = MutableStateFlow(Quality.UNKNOWN)
    val quality: StateFlow<Quality> = _quality.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    private val _failoverCount = MutableStateFlow(0)
    val failoverCount: StateFlow<Int> = _failoverCount.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    private val consecutiveFailures = AtomicInteger(0)
    private val recentFailures = ArrayDeque<Long>(3)
    private var redSinceMs: Long = 0L

    private var profiles: List<Profile> = emptyList()

    fun start(initialProfile: Profile, allProfiles: List<Profile>) {
        stop()
        profiles = allProfiles
        _activeProfileId.value = initialProfile.id
        job = scope.launch {
            while (isActive) {
                tick()
                delay(intervalMs)
            }
        }
    }

    fun updateProfiles(profiles: List<Profile>) {
        this.profiles = profiles
    }

    fun stop() {
        job?.cancel()
        job = null
        _quality.value = Quality.UNKNOWN
        _lastLatencyMs.value = null
        consecutiveFailures.set(0)
        recentFailures.clear()
        redSinceMs = 0L
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private suspend fun tick() {
        val activeId = _activeProfileId.value ?: return
        val active = profiles.firstOrNull { it.id == activeId } ?: return
        if (!active.enabled) return

        val latency = ping(active)
        _lastLatencyMs.value = latency

        if (latency == null) {
            val fails = consecutiveFailures.incrementAndGet()
            val now = System.currentTimeMillis()
            recentFailures.addLast(now)
            while (recentFailures.size > 3) recentFailures.removeFirst()

            if (fails >= 2) {
                _quality.value = Quality.RED
                if (redSinceMs == 0L) redSinceMs = now
                if (now - redSinceMs >= failoverAfterMs) {
                    failoverToNext()
                    return
                }
            } else {
                _quality.value = Quality.YELLOW
            }
        } else {
            consecutiveFailnessReset()
            redSinceMs = 0L
            _quality.value = when {
                latency <= 200 -> Quality.GREEN
                latency <= 1000 -> Quality.YELLOW
                else -> Quality.RED
            }
        }
    }

    private fun consecutiveFailnessReset() {
        consecutiveFailures.set(0)
        recentFailures.clear()
    }

    private fun failoverToNext() {
        if (profiles.size < 2) return
        val currentIndex = profiles.indexOfFirst { it.id == _activeProfileId.value }
        val startIndex = if (currentIndex < 0) 0 else currentIndex
        var nextIndex = (startIndex + 1) % profiles.size
        var attempts = 0
        while (attempts < profiles.size) {
            val candidate = profiles[nextIndex]
            if (candidate.enabled && candidate.id != _activeProfileId.value) {
                Log.i("HealthChecker", "Failover: ${_activeProfileId.value} -> ${candidate.id}")
                _activeProfileId.value = candidate.id
                _failoverCount.value++
                consecutiveFailnessReset()
                redSinceMs = 0L
                _quality.value = Quality.YELLOW
                return
            }
            nextIndex = (nextIndex + 1) % profiles.size
            attempts++
        }
    }

    private fun ping(profile: Profile): Long? {
        return try {
            val start = System.nanoTime()
            Socket().use { sock ->
                sock.connect(InetSocketAddress(profile.address, profile.port), pingTimeoutMs)
            }
            val ms = (System.nanoTime() - start) / 1_000_000
            if (ms in 1..30_000) ms else null
        } catch (e: IOException) {
            Log.d("HealthChecker", "ping ${profile.address}:${profile.port} failed: ${e.message}")
            null
        } catch (e: Exception) {
            null
        }
    }

    enum class Quality { UNKNOWN, GREEN, YELLOW, RED }
}
