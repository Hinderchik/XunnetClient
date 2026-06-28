package dev.xunnet.client.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dev.xunnet.client.MainActivity
import dev.xunnet.client.R
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class XunnetVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats

    private var activeProfile: Profile? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("DEPRECATION")
        val profile: Profile? = intent?.getParcelableExtra(EXTRA_PROFILE)
        if (profile != null) {
            startVpn(profile)
        } else {
            stopVpn()
        }
        return START_NOT_STICKY
    }

    fun startVpn(profile: Profile): Result<Unit> {
        return try {
            activeProfile = profile
            val builder = Builder()
                .setSession(profile.name)
                .addAddress("172.19.0.1", 24)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .establish()

            vpnInterface = builder
            startForeground(NOTIFICATION_ID, buildNotification(profile.name))
            _stats.value = Stats(connected = true, activeProfileId = profile.id)
            Timber.d("VPN started for ${profile.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            Result.failure(e)
        }
    }

    fun stopVpn(): Result<Unit> {
        return try {
            vpnInterface?.close()
            vpnInterface = null
            activeProfile = null
            _stats.value = Stats(connected = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Timber.d("VPN stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop VPN")
            Result.failure(e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(profileName: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text, profileName))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_PROFILE = "extra_profile"
        private const val CHANNEL_ID = "xunnet_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
