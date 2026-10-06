package com.myvpn.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions

class MyVpnApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val baseDir = getExternalFilesDir(null) ?: filesDir
        runCatching {
            Libbox.setup(
                SetupOptions().apply {
                    basePath = baseDir.absolutePath
                    workingPath = baseDir.absolutePath
                    tempPath = cacheDir.absolutePath
                    commandServerListenPort = 0
                    logMaxLines = 300
                    debug = false
                    crashReportSource = "service"
                    appVersion = BuildConfig.VERSION_NAME
                    appMarketingVersion = BuildConfig.VERSION_NAME
                },
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_SERVICE, "وضعیت اتصال", NotificationManager.IMPORTANCE_LOW),
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "هشدارها", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "service"
        const val CHANNEL_ALERTS = "alerts"
        lateinit var instance: MyVpnApplication
            private set
    }
}
