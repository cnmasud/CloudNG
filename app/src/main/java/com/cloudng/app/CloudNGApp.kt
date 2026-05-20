package com.cloudng.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CloudNGApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val CHANNEL_VPN = "cloudng_vpn"
        const val CHANNEL_UPDATES = "cloudng_updates"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_VPN,
                    getString(R.string.notification_channel_vpn),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_vpn_desc)
                    setShowBadge(false)
                }
            )
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_UPDATES,
                    "Subscription Updates",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    setShowBadge(false)
                }
            )
        }
    }
}
