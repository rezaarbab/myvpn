package com.myvpn.app.bg

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.myvpn.app.MyVpnApplication
import com.myvpn.app.MainActivity
import com.myvpn.app.R
import com.myvpn.app.VpnManager
import com.myvpn.app.data.ConfigBuilder
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.data.TrafficStore
import io.nekohasekai.libbox.BoxService
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.Notification as LibboxNotification
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class TunnelService : VpnService(), PlatformInterfaceImpl, CommandServerHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationDelegate by lazy { ServiceNotificationDelegate(this) }

    private var commandServer: CommandServer? = null
    private var boxService: BoxService? = null
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CLOSE) stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (VpnManager.status.value != VpnManager.Status.STOPPED) return START_NOT_STICKY
        VpnManager.setStatus(VpnManager.Status.STARTING)
        VpnManager.setError(null)

        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                receiver,
                IntentFilter(ACTION_CLOSE),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        notificationDelegate.startForeground("در حال اتصال…")

        scope.launch { startBox() }
        return START_NOT_STICKY
    }

    private suspend fun startBox() {
        try {
            val server = Libbox.newCommandServer(this, 300)
            server.start()
            commandServer = server

            val profile = ProfileStore.selected()
            if (profile == null) {
                fail("هیچ سروری انتخاب نشده است")
                return
            }
            val config = ConfigBuilder.build(profile)
            val service = Libbox.newService(config, this)
            server.setService(service)
            service.start()
            boxService = service

            VpnManager.observe()
            withContext(Dispatchers.Main) {
                notificationDelegate.update(profile.name, "متصل")
            }
            VpnManager.setStatus(VpnManager.Status.STARTED)
        } catch (e: Exception) {
            Log.e(TAG, "start service", e)
            fail(e.message ?: e.toString())
        }
    }

    private suspend fun fail(message: String) {
        VpnManager.setError(message)
        shutdown()
    }

    fun stopService() {
        val current = VpnManager.status.value
        if (current == VpnManager.Status.STOPPING || current == VpnManager.Status.STOPPED) return
        VpnManager.setStatus(VpnManager.Status.STOPPING)
        scope.launch { shutdown() }
    }

    private suspend fun shutdown() {
        fileDescriptor?.let {
            runCatching { it.close() }
        }
        fileDescriptor = null
        // ثبت مصرف این نشست در مجموع کل
        val sessionTraffic = VpnManager.traffic.value
        if (sessionTraffic.downlinkTotal > 0 || sessionTraffic.uplinkTotal > 0) {
            TrafficStore.addSession(sessionTraffic.downlinkTotal, sessionTraffic.uplinkTotal)
        }
        boxService?.let { service ->
            runCatching { service.close() }
        }
        boxService = null
        commandServer?.let { server ->
            runCatching { server.setService(null) }
            runCatching { server.close() }
        }
        commandServer = null
        withContext(Dispatchers.Main) {
            notificationDelegate.stopForeground()
            if (receiverRegistered) {
                runCatching { unregisterReceiver(receiver) }
                receiverRegistered = false
            }
            VpnManager.setStatus(VpnManager.Status.STOPPED)
            stopSelf()
        }
    }

    override fun onRevoke() {
        stopService()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ---- PlatformInterface ----

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    private var fileDescriptor: ParcelFileDescriptor? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun openTun(options: TunOptions): Int {
        if (prepare(this) != null) error("android: missing vpn permission")

        val builder = Builder()
            .setSession("MyVPN")
            .setMtu(options.mtu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        val inet4Address = options.inet4Address
        while (inet4Address.hasNext()) {
            val prefix = inet4Address.next()!!
            builder.addAddress(prefix.address(), prefix.prefix())
        }
        val inet6Address = options.inet6Address
        while (inet6Address.hasNext()) {
            val prefix = inet6Address.next()!!
            builder.addAddress(prefix.address(), prefix.prefix())
        }

        if (options.autoRoute) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val inet4RouteAddress = options.inet4RouteAddress
                if (inet4RouteAddress.hasNext()) {
                    while (inet4RouteAddress.hasNext()) {
                        val prefix = inet4RouteAddress.next()!!
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                } else {
                    builder.addRoute("0.0.0.0", 0)
                }
                val inet6RouteAddress = options.inet6RouteAddress
                if (inet6RouteAddress.hasNext()) {
                    while (inet6RouteAddress.hasNext()) {
                        val prefix = inet6RouteAddress.next()!!
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                } else if (options.inet6Address.hasNext()) {
                    builder.addRoute("::", 0)
                }
                val inet4Exclude = options.inet4RouteExcludeAddress
                while (inet4Exclude.hasNext()) {
                    val prefix = inet4Exclude.next()!!
                    builder.excludeRoute(IpPrefix(InetAddress.getByName(prefix.address()), prefix.prefix()))
                }
                val inet6Exclude = options.inet6RouteExcludeAddress
                while (inet6Exclude.hasNext()) {
                    val prefix = inet6Exclude.next()!!
                    builder.excludeRoute(IpPrefix(InetAddress.getByName(prefix.address()), prefix.prefix()))
                }
            } else {
                val inet4RouteRange = options.inet4RouteRange
                if (inet4RouteRange.hasNext()) {
                    while (inet4RouteRange.hasNext()) {
                        val prefix = inet4RouteRange.next()!!
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                }
                val inet6RouteRange = options.inet6RouteRange
                if (inet6RouteRange.hasNext()) {
                    while (inet6RouteRange.hasNext()) {
                        val prefix = inet6RouteRange.next()!!
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                }
            }

            val includePackage = options.includePackage
            while (includePackage.hasNext()) {
                runCatching { builder.addAllowedApplication(includePackage.next()) }
            }
            val excludePackage = options.excludePackage
            while (excludePackage.hasNext()) {
                runCatching { builder.addDisallowedApplication(excludePackage.next()) }
            }
        }

        val pfd = builder.establish()
            ?: error("android: the application is not prepared or is revoked")
        fileDescriptor = pfd
        return pfd.fd
    }

    override fun sendNotification(notification: LibboxNotification) {
        notificationDelegate.showCoreNotification(notification)
    }

    // ---- CommandServerHandler (libbox v1.12) ----

    override fun serviceReload() {
        // در فاز ۱ استفاده نمی‌شود
    }

    override fun postServiceClose() {
        stopService()
    }

    override fun getSystemProxyStatus(): SystemProxyStatus =
        SystemProxyStatus().apply {
            available = false
            enabled = false
        }

    override fun setSystemProxyEnabled(enabled: Boolean) {}

    companion object {
        private const val TAG = "TunnelService"
        const val ACTION_START = "com.myvpn.app.action.START"
        const val ACTION_CLOSE = "com.myvpn.app.action.CLOSE"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TunnelService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.sendBroadcast(Intent(ACTION_CLOSE).setPackage(context.packageName))
        }
    }
}

private const val NOTIFICATION_ID = 1001

class ServiceNotificationDelegate(private val service: android.app.Service) {

    private val manager = NotificationManagerCompat.from(service)

    fun startForeground(text: String) {
        val notification = build("MyVPN", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                service,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            ServiceCompat.startForeground(service, NOTIFICATION_ID, notification, 0)
        }
    }

    fun update(serverName: String, text: String) {
        runCatching { manager.notify(NOTIFICATION_ID, build(serverName, text)) }
    }

    fun stopForeground() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    fun showCoreNotification(notification: LibboxNotification) {
        val channel = "core-${notification.typeID}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyVpnApplication.instance.getSystemService(android.app.NotificationManager::class.java)
                .createNotificationChannel(
                    android.app.NotificationChannel(channel, notification.typeName, android.app.NotificationManager.IMPORTANCE_HIGH),
                )
        }
        val builder = NotificationCompat.Builder(service, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
        runCatching { manager.notify(notification.identifier, notification.typeID, builder.build()) }
    }

    private fun build(title: String, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getBroadcast(
            service,
            1,
            Intent(TunnelService.ACTION_CLOSE).setPackage(service.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(service, MyVpnApplication.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "قطع اتصال", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
