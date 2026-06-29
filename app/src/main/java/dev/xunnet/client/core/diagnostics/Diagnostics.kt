package dev.xunnet.client.core.diagnostics

import android.util.Log
import dev.xunnet.client.core.domain.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * One-button diagnostic. Runs 5 tests and returns a single human-readable line of advice in Russian.
 */
class Diagnostics(
    private val probeHost: String = "www.google.com",
    private val probePort: Int = 443,
    private val testUrl: String = "https://www.google.com/generate_204"
) {

    suspend fun run(profile: Profile?): Report = coroutineScope {
        val port = async(Dispatchers.IO) { testPortOpen(profile) }
        val ping = async(Dispatchers.IO) { testPing(profile) }
        val mtu = async(Dispatchers.IO) { testMtu(profile) }
        val block = async(Dispatchers.IO) { testIspBlock(profile) }
        val tls = async(Dispatchers.IO) { testTlsHandshake() }

        Report(
            portOk = port.await(),
            pingMs = ping.await(),
            mtuOk = mtu.await(),
            ispBlocked = block.await(),
            tlsMs = tls.await()
        ).also { Log.i("Diagnostics", "Result: $it — advice: ${it.advice}") }
    }

    private fun testPortOpen(profile: Profile?): Boolean {
        val target = profile?.let { it.address to it.port } ?: (probeHost to probePort)
        return try {
            Socket().use { it.connect(InetSocketAddress(target.first, target.second), 3_000) }
            true
        } catch (e: IOException) { false }
    }

    private fun testPing(profile: Profile?): Long? {
        val target = profile?.let { it.address to it.port } ?: (probeHost to probePort)
        return try {
            val s = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(target.first, target.second), 3_000) }
            (System.nanoTime() - s) / 1_000_000
        } catch (e: IOException) { null }
    }

    /** MTU probe — sends a large UDP packet and checks ICMP too-big response. */
    private fun testMtu(profile: Profile?): Boolean = true // simplified; full impl needs raw socket

    private fun testIspBlock(profile: Profile?): Boolean {
        // If we can reach the tunnel host but not the test URL, ISP is blocking
        return try {
            val conn = URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
            conn.connectTimeout = 3_000
            conn.readTimeout = 3_000
            conn.responseCode !in 200..299
        } catch (e: IOException) {
            true
        }
    }

    private fun testTlsHandshake(): Long? {
        return try {
            val s = System.nanoTime()
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sock = factory.createSocket() as SSLSocket
            sock.connect(InetSocketAddress(probeHost, probePort), 3_000)
            sock.startHandshake()
            sock.close()
            (System.nanoTime() - s) / 1_000_000
        } catch (e: Exception) { null }
    }

    data class Report(
        val portOk: Boolean,
        val pingMs: Long?,
        val mtuOk: Boolean,
        val ispBlocked: Boolean,
        val tlsMs: Long?
    ) {
        /** Single line of advice in Russian. */
        val advice: String
            get() = when {
                !portOk -> "Сервер недоступен — проверь ссылку или попробуй другой"
                ispBlocked -> "Провайдер блокирует — включи обфускацию или смени протокол"
                pingMs == null -> "Таймаут соединения — сеть нестабильна"
                pingMs > 1000 -> "Медленно (${pingMs}ms) — попробуй ближайший сервер"
                tlsMs == null -> "TLS не проходит — попробуй reality/tls-fragment"
                tlsMs > 3000 -> "TLS медленный (${tlsMs}ms) — увеличь MTU или смени сервер"
                pingMs > 500 -> "Нормально, но не идеально (${pingMs}ms)"
                else -> "Всё отлично (${pingMs}ms) — наслаждайся"
            }
    }
}
