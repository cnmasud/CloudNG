package com.cloudng.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cloudng.app.service.CloudProxyService
import com.cloudng.app.service.CloudVpnService

class VpnControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISCONNECT = "com.cloudng.app.ACTION_DISCONNECT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISCONNECT -> {
                context.startService(
                    Intent(context, CloudProxyService::class.java).apply {
                        action = CloudProxyService.ACTION_STOP
                    }
                )
                context.startService(
                    Intent(context, CloudVpnService::class.java).apply {
                        action = CloudVpnService.ACTION_STOP
                    }
                )
            }
        }
    }
}
