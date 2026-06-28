package dev.xunnet.client.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.xunnet.client.MainActivity
import dev.xunnet.client.R
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.vpn.SingBoxCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class XunnetVpnService : VpnService() {

    @Inject lateinit var singBoxCore: SingBoxCore

    private var vpnInterface: ParcelFileDescriptor? = null
    private val _state = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = _state

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // observe core stats
        statsJob = scope.launch {
            singBoxCore.stats.collectLatest { core ->
                _state.value = _state.value.copy(
                    stats = core,
                    running = core.connected,
                    activeProfileId = core.activeProfileId
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> stopVpn()
            else -> {
                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                val profile: Profile? = intent?.getParcelableExtra<android.os.Parcelable>(EXTRA_PROFILE) as Profile?
                if (profile != null) startVpn(profile)
                else stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    fun startVpn(profile: Profile): Result<Unit> {
        if (_state.value.running) {
            Timber.w("VPN already running")
            return Result.success(Unit)
        }
        return try {
            // 1) Establish Android VPN interface
            val builder = Builder()
                .setSession(profile.name)
                .setMtu(1500)
                .addAddress("172.19.0.2", 30)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .setBlocking(true)
            vpnInterface = builder.establish()
                ?: error("Failed to establish VPN interface")

            // 2) Build sing-box config and start
            val config = singBoxCore.buildConfig(profile)
            val result = singBoxCore.startRaw(config)
            if (result.isFailure) {
                vpnInterface?.close()
                vpnInterface = null
                return result
            }

            startForeground(NOTIFICATION_ID, buildNotification(profile.name))
            _state.value = _state.value.copy(
                running = true,
                activeProfileId = profile.id,
                activeProfileName = profile.name
            )
            Timber.d("VPN started for ${profile.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            vpnInterface?.close()
            vpnInterface = null
            Result.failure(e)
        }
    }

    fun stopVpn(): Result<Unit> {
        if (!_state.value.running && vpnInterface == null) {
            return Result.success(Unit)
        }
        return try {
            singBoxCore.stop()
            vpnInterface?.close()
            vpnInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _state.value = VpnState()
            Timber.d("VPN stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop VPN")
            _state.value = _state.value.copy(running = false)
            Result.failure(e)
        }
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        statsJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(profileName: String): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val disconnectIntent = Intent(this, XunnetVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPi = PendingIntent.getService(
            this, 1, disconnectIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text, profileName))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.disconnect),
                disconnectPi
            )
            .build()
    }

    data class VpnState(
        val running: Boolean = false,
        val activeProfileId: String? = null,
        val activeProfileName: String? = null,
        val stats: dev.xunnet.client.core.domain.model.Stats = dev.xunnet.client.core.domain.model.Stats()
    )

    companion object {
        const val EXTRA_PROFILE = "extra_profile"
        const val ACTION_DISCONNECT = "dev.xunnet.client.action.DISCONNECT"
        private const val CHANNEL_ID = "xunnet_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
