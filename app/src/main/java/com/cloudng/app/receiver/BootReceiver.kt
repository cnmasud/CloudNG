package com.cloudng.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cloudng.app.data.repository.SettingsRepository
import com.cloudng.app.service.CloudVpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.appSettings.first()
                if (settings.autoStartOnBoot && settings.lastSelectedProfileId != null) {
                    Log.i("BootReceiver", "Auto-start: launching VPN for profile ${settings.lastSelectedProfileId}")
                    val vpnIntent = Intent(context, CloudVpnService::class.java).apply {
                        action = CloudVpnService.ACTION_START
                        putExtra(CloudVpnService.EXTRA_PROFILE_ID, settings.lastSelectedProfileId)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(vpnIntent)
                    } else {
                        context.startService(vpnIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
