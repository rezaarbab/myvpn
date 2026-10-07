package com.myvpn.app.bg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myvpn.app.data.AppSettings

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && AppSettings.autoConnect.value) {
            TunnelService.start(context)
        }
    }
}
