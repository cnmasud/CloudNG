package com.cloudng.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.data.db.RoutingRuleDao
import com.cloudng.app.data.repository.ProfileRepository
import com.cloudng.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class CloudProxyService : Service() {

    @Inject lateinit var coreBridge: CoreBridge
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var routingRuleDao: RoutingRuleDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "com.cloudng.app.proxy.START"
        const val ACTION_STOP = "com.cloudng.app.proxy.STOP"
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val TAG = "CloudProxyService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) startProxy(profileId) else stopSelf()
            }
            ACTION_STOP -> stopProxy()
        }
        return START_NOT_STICKY
    }

    private fun startProxy(profileId: String) {
        scope.launch {
            val profile = profileRepository.getById(profileId) ?: run {
                Log.e(TAG, "Profile $profileId not found")
                stopSelf()
                return@launch
            }
            val baseRouting = settingsRepository.routingConfig.first()
            val customRules = routingRuleDao.observeAll().first()
            val routing = baseRouting.copy(customRules = customRules)
            val dns = settingsRepository.dnsConfig.first()
            val result = coreBridge.start(profile, routing, dns, tunFd = -1)
            result.onFailure {
                Log.e(TAG, "Core start failed: ${it.message}")
                stopSelf()
            }
            result.onSuccess {
                Log.i(TAG, "Core started for profile: ${profile.name}")
            }
        }
    }

    private fun stopProxy() {
        scope.launch {
            coreBridge.stop()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.launch { coreBridge.stop() }
        scope.cancel()
        super.onDestroy()
    }
}
