package com.cloudng.app.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.cloudng.app.CloudNGApp
import com.cloudng.app.MainActivity
import com.cloudng.app.R
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.data.db.RoutingRuleDao
import com.cloudng.app.data.model.AppSettings
import com.cloudng.app.data.model.RoutingConfig
import com.cloudng.app.data.repository.ProfileRepository
import com.cloudng.app.data.repository.SettingsRepository
import com.cloudng.app.receiver.VpnControlReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import androidx.core.app.NotificationCompat

@AndroidEntryPoint
class CloudVpnService : VpnService() {

    @Inject lateinit var coreBridge: CoreBridge
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var routingRuleDao: RoutingRuleDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_START = "com.cloudng.app.vpn.START"
        const val ACTION_STOP = "com.cloudng.app.vpn.STOP"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "CloudVpnService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                start(profileId)
            }
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start(profileId: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        scope.launch {
            try {
                val baseRouting = settingsRepository.routingConfig.first()
                val customRules = routingRuleDao.observeAll().first()
                val routing = baseRouting.copy(customRules = customRules)
                val dns = settingsRepository.dnsConfig.first()
                val settings = settingsRepository.appSettings.first()
                val profile = profileId?.let { profileRepository.getById(it) }
                    ?: run { Log.e(TAG, "No profile found for id=$profileId"); stopSelf(); return@launch }
                coreBridge.registerSocketProtector { socketFd -> protect(socketFd) }
                tun = buildTun(settings, routing)
                val fd = tun?.fd ?: run {
                    Log.e(TAG, "TUN fd is null after establish")
                    stopSelf(); return@launch
                }
                Log.d(TAG, "TUN interface opened: $fd")
                coreBridge.start(profile, routing, dns, fd).onFailure { e ->
                    Log.e(TAG, "Core start failed", e)
                    tun?.close(); tun = null
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "VPN start failed", e)
                coreBridge.stop()
                stopSelf()
            }
        }
    }

    private fun stop() {
        scope.launch {
            coreBridge.stop()
            tun?.close()
            tun = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildTun(settings: AppSettings, routing: RoutingConfig): ParcelFileDescriptor {
        val builder = Builder()
            .setMtu(settings.mtu)
            .addAddress("10.0.0.1", 32)
            .addAddress("fd00::1", 128)
            .addDnsServer("1.1.1.1")
            .addDnsServer("2606:4700:4700::1111")
            .setSession("CloudNG")
            .setConfigureIntent(mainActivityIntent())

        val bypassLan = routing.mode == com.cloudng.app.data.model.RoutingMode.BYPASS_LAN ||
                routing.mode == com.cloudng.app.data.model.RoutingMode.BYPASS_MAINLAND

        if (bypassLan) {
            builder.addRoute("0.0.0.0", 5)
            builder.addRoute("8.0.0.0", 7)
            builder.addRoute("11.0.0.0", 8)
            builder.addRoute("12.0.0.0", 6)
            builder.addRoute("14.0.0.0", 7)
            builder.addRoute("16.0.0.0", 4)
            builder.addRoute("32.0.0.0", 3)
            builder.addRoute("64.0.0.0", 3)
            builder.addRoute("96.0.0.0", 4)
            builder.addRoute("112.0.0.0", 5)
            builder.addRoute("120.0.0.0", 6)
            builder.addRoute("124.0.0.0", 7)
            builder.addRoute("126.0.0.0", 8)
            builder.addRoute("128.0.0.0", 1)
            builder.addRoute("::", 1)
            builder.addRoute("8000::", 2)
        } else {
            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)
        }

        if (routing.perAppMode != com.cloudng.app.data.model.PerAppProxyMode.DISABLED) {
            routing.bypassedApps.forEach { pkg ->
                runCatching { builder.addDisallowedApplication(pkg) }
            }
            routing.proxiedApps.forEach { pkg ->
                runCatching { builder.addAllowedApplication(pkg) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }
        return builder.establish() ?: throw IllegalStateException("VpnService.establish() returned null")
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CloudNGApp.CHANNEL_VPN)
        .setContentTitle(getString(R.string.notification_connected))
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setOngoing(true)
        .setShowWhen(false)
        .addAction(
            android.R.drawable.ic_media_pause,
            getString(R.string.notification_action_disconnect),
            disconnectIntent()
        )
        .setContentIntent(mainActivityIntent())
        .build()

    private fun mainActivityIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun disconnectIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 0,
        Intent(this, VpnControlReceiver::class.java).apply {
            action = VpnControlReceiver.ACTION_DISCONNECT
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun onRevoke() {
        Log.d(TAG, "VPN revoked by system")
        stop()
        super.onRevoke()
    }

    override fun onDestroy() {
        scope.launch { runCatching { coreBridge.stop() } }
        tun?.close()
        tun = null
        scope.cancel()
        super.onDestroy()
    }
}
