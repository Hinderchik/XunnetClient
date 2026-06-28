package dev.xunnet.client.core.vpn

import android.os.SystemClock
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import dev.xunnet.client.core.domain.parser.XunnetLinkParser
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
import org.json.JSONObject
import timber.log.Timber

/**
 * Controls the underlying sing-box runtime and exposes connection stats.
 *
 * Resolution order:
 *   1. Native library "xunnet-core" (preferred — bundled JNI from core-libs)
 *   2. Fallback to running `sing-box` binary as a subprocess
 *
 * Stats are polled every second while connected. They are derived from the
 * sing-box clash API (port 9090 by default) when available; otherwise we just
 * track elapsed time and report zero counters.
 */
class SingBoxCore(
    private val parser: XunnetLinkParser = XunnetLinkParser()
) {

    private val _stats = MutableStateFlow(Stats(connected = false))
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null
    private var process: Process? = null
    private val startedAt = SystemClock.elapsedRealtime()

    private val nativeAvailable: Boolean = try {
        System.loadLibrary("xunnet-core"); true
    } catch (e: UnsatisfiedLinkError) {
        Timber.w(e, "libxunnet-core not present; will try binary fallback")
        false
    }

    /**
     * Build a minimal sing-box config from a Profile, then start sing-box.
     */
    fun start(profile: Profile): Result<Unit> {
        val config = buildConfig(profile)
        return startRaw(config)
    }

    /**
     * Start sing-box with a raw config string (e.g. from federated panel).
     */
    fun startRaw(config: String): Result<Unit> {
        _stats.value = Stats(connected = true, activeProfileId = _stats.value.activeProfileId)
        startedAt.let { /* reset */ }
        return if (nativeAvailable) startNative(config) else startBinary(config)
    }

    fun stop(): Result<Unit> {
        pollJob?.cancel()
        pollJob = null
        return try {
            val ok = if (nativeAvailable) nativeStop() else stopBinary()
            _stats.value = Stats(connected = false)
            if (ok) Result.success(Unit) else Result.failure(Exception("Failed to stop sing-box"))
        } catch (e: Exception) {
            Timber.e(e, "sing-box stop error")
            _stats.value = Stats(connected = false)
            Result.failure(e)
        }
    }

    fun isRunning(): Boolean = _stats.value.connected

    fun getVersion(): String = try {
        if (nativeAvailable) nativeGetVersion() else "binary"
    } catch (e: Exception) {
        "unknown"
    }

    /**
     * Generate a sing-box JSON config from a Profile.
     *   - tun:    inbounds[0] = tun mode
     *   - direct: outbound DNS via 1.1.1.1
     *   - proxy:  outbound from profile
     */
    fun buildConfig(profile: Profile): String {
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
        root.put("inbounds", org.json.JSONArray().put(tun))
        // outbounds: proxy + direct + block
        val proxy = parser.toSingBoxOutbound(profile)
        val direct = JSONObject().put("type", "direct")
        val block = JSONObject().put("type", "block")
        root.put("outbounds", org.json.JSONArray()
            .put(proxy)
            .put(direct)
            .put(block))
        // DNS
        val dns = JSONObject().apply {
            put("servers", org.json.JSONArray().put("1.1.1.1"))
            put("strategy", "ipv4_only")
        }
        root.put("dns", dns)
        // route
        val route = JSONObject().apply {
            put("auto_detect_interface", true)
            put("final", "proxy")
            val rules = org.json.JSONArray()
            // bypass LAN
            rules.put(JSONObject().apply {
                put("ip_cidr", org.json.JSONArray().put("10.0.0.0/8").put("172.16.0.0/12").put("192.168.0.0/16"))
                put("outbound", "direct")
            })
            root.put("route", JSONObject().put("rules", rules))
        }
        // experimental: clash API for stats
        val experimental = JSONObject().apply {
            val clash = JSONObject().apply {
                put("external_controller", "127.0.0.1:9090")
                put("default_mode", "rule")
            }
            put("clash_api", clash)
            put("cache_file", "/data/data/dev.xunnet.client/cache/sing-box.db")
        }
        root.put("experimental", experimental)
        return root.toString(2)
    }

    // ---------------- private ----------------

    private fun startNative(config: String): Result<Unit> = runCatching {
        val ok = nativeStart(config)
        if (!ok) error("nativeStart returned false")
        startPolling()
    }.recoverCatching {
        Timber.e(it, "native sing-box start failed")
        throw it
    }

    private fun startBinary(config: String): Result<Unit> = runCatching {
        val configFile = java.io.File.createTempFile("xunnet-singbox-", ".json")
        configFile.writeText(config)
        configFile.deleteOnExit()

        val binaryPath = locateBinary() ?: error(
            "sing-box binary not found. Place it in app/src/main/jniLibs/<abi>/sing-box " +
                    "or build via core-libs/singbox/build.sh"
        )

        process = ProcessBuilder(binaryPath, "run", "-c", configFile.absolutePath)
            .redirectErrorStream(true)
            .start()
        // log stdout asynchronously
        scope.launch {
            process?.inputStream?.bufferedReader()?.useLines { lines ->
                lines.forEach { Timber.tag("sing-box").i(it) }
            }
        }
        startPolling()
    }

    private fun stopBinary(): Boolean {
        return try {
            process?.destroy()
            if (process?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) == false) {
                process?.destroyForcibly()
            }
            process = null
            true
        } catch (e: Exception) {
            Timber.e(e, "stopBinary")
            false
        }
    }

    private fun locateBinary(): String? {
        val candidates = listOf(
            "/system/bin/sing-box",
            "/system/xbin/sing-box",
            "/data/local/tmp/sing-box",
            "sing-box" // rely on PATH
        )
        return candidates.firstOrNull { java.io.File(it).canExecute() }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var lastUp = 0L
            var lastDown = 0L
            while (isActive && _stats.value.connected) {
                val (up, down) = readCounters()
                val dt = 1.0
                val upSpeed = ((up - lastUp) / dt).coerceAtLeast(0)
                val downSpeed = ((down - lastDown) / dt).coerceAtLeast(0)
                lastUp = up
                lastDown = down
                _stats.value = _stats.value.copy(
                    uploadBytes = up,
                    downloadBytes = down,
                    uploadSpeed = upSpeed,
                    downloadSpeed = downSpeed,
                    connected = true
                )
                delay(1000)
            }
        }
    }

    /**
     * Read traffic counters from sing-box clash API.
     * Returns (uploadBytes, downloadBytes). Zeroes if API is unreachable.
     */
    private fun readCounters(): Pair<Long, Long> {
        return try {
            val url = java.net.URL("http://127.0.0.1:9090/traffic")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 500
            conn.readTimeout = 500
            if (conn.responseCode !in 200..299) return 0L to 0L
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val jo = JSONObject(body)
            jo.optLong("up") to jo.optLong("down")
        } catch (e: Exception) {
            0L to 0L
        }
    }

    // JNI — populated by libxunnet-core (Go shared lib via gomobile bind)
    private external fun nativeStart(config: String): Boolean
    private external fun nativeStop(): Boolean
    private external fun nativeGetVersion(): String

    fun shutdown() {
        stop()
        scope.cancel()
    }
}
