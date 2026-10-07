package com.myvpn.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.io.File

class MyVpnApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val stderrFile = File(filesDir, "go-stderr.log")

        // اگر اجرای قبلی کرش گو داشته، متنش را در تب گزارش نشان بده
        if (stderrFile.exists() && stderrFile.length() > 0) {
            val previous = runCatching { stderrFile.readText() }.getOrDefault("")
            if (previous.isNotBlank()) {
                VpnManager.appendLog("——— خطای اجرای قبلی ———")
                previous.lines().take(40).forEach { VpnManager.appendLog(it) }
            }
        }

        // حتماً حافظه داخلی: سوکت یونیکس command server روی emulated/external
        // ساخته نمی‌شود (bind: invalid argument)
        runCatching {
            Libbox.setup(
                SetupOptions().apply {
                    basePath = filesDir.absolutePath
                    workingPath = filesDir.absolutePath
                    tempPath = cacheDir.absolutePath
                    fixAndroidStack = true
                },
            )
        }

        // stderr گو (متن panic/fatal error) را به فایل منتقل کن
        runCatching { Libbox.redirectStderr(stderrFile.absolutePath) }

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
