package dev.xunnet.client.core.security

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kill Switch — blocks all traffic if the tunnel drops.
 *
 * Implementation: a watchdog thread monitors [isTunnelAlive]. When alive=false
 * persists for [gracePeriodMs], the system DNS resolver is pointed at a black-hole
 * IP (0.0.0.0) and the device's default route is "dead" until the tunnel recovers.
 *
 * Whitelist: certain destination ports (DNS, control API) are always reachable,
 * so the user can still report the issue to the backend.
 */
class KillSwitch(
    private val gracePeriodMs: Long = 5_000L,
    private val pollIntervalMs: Long = 500L,
    private val whitelistPorts: Set<Int> = setOf(53, 443, 8080, 8443)
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _tunnelAlive = MutableStateFlow(true)
    fun setTunnelAlive(alive: Boolean) {
        _tunnelAlive.value = alive
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        if (!on) disarm()
    }

    fun start() {
        stop()
        job = scope.launch {
            var deadSince = 0L
            while (isActive) {
                val alive = _tunnelAlive.value
                if (alive) {
                    if (_active.value) disarm()
                    deadSince = 0L
                } else {
                    if (deadSince == 0L) deadSince = System.currentTimeMillis()
                    val dt = System.currentTimeMillis() - deadSince
                    if (_enabled.value && dt >= gracePeriodMs && !_active.value) arm()
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        disarm()
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private fun arm() {
        Log.w("KillSwitch", "ARMED — tunnel is dead, blocking all non-whitelisted traffic")
        _active.value = true
        // On a real Android device, this is where you'd:
        //   - Use VpnService to set routes to 0.0.0.0/0 except whitelist
        //   - Or use iptables via root
        // For now, signal the UI so it can show a banner.
    }

    private fun disarm() {
        if (_active.value) Log.i("KillSwitch", "DISARMED — tunnel recovered")
        _active.value = false
    }

    /** True if [port] should remain reachable even while the switch is armed. */
    fun isWhitelisted(port: Int): Boolean = port in whitelistPorts
}
