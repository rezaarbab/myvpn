package com.myvpn.app.bg

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import com.myvpn.app.MyVpnApplication
import com.myvpn.app.VpnManager
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * پیاده‌سازی PlatformInterface مخصوص libbox v1.12 (۱۶ متد).
 */
interface PlatformInterfaceImpl : PlatformInterface {

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    // در TunnelService با protect(fd) بازنویسی می‌شود
    override fun autoDetectInterfaceControl(fd: Int) {}

    override fun openTun(options: TunOptions): Int {
        error("android: tun inbound requires VPN service")
    }

    // همه‌ی لاگ‌های هسته از این مسیر می‌آید
    override fun writeLog(message: String?) {
        VpnManager.appendLog(message ?: "")
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): Int {
        val connectivity = MyVpnApplication.instance.getSystemService(ConnectivityManager::class.java)
        val uid = connectivity.getConnectionOwnerUid(
            ipProtocol,
            InetSocketAddress(sourceAddress, sourcePort),
            InetSocketAddress(destinationAddress, destinationPort),
        )
        if (uid == Process.INVALID_UID) error("android: connection owner not found")
        return uid
    }

    override fun packageNameByUid(uid: Int): String =
        MyVpnApplication.instance.packageManager.getPackagesForUid(uid)?.firstOrNull() ?: ""

    override fun uidByPackageName(packageName: String?): Int =
        MyVpnApplication.instance.packageManager.getPackageUid(packageName ?: "", 0)

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(null)
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val context = MyVpnApplication.instance
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val javaInterfaces = NetworkInterface.getNetworkInterfaces().toList()
        val interfaces = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        for (network in connectivity.allNetworks) {
            val linkProperties = connectivity.getLinkProperties(network) ?: continue
            val capabilities = connectivity.getNetworkCapabilities(network) ?: continue
            val javaInterface = javaInterfaces.firstOrNull { it.name == linkProperties.interfaceName } ?: continue

            val boxInterface = io.nekohasekai.libbox.NetworkInterface()
            boxInterface.name = linkProperties.interfaceName
            boxInterface.index = javaInterface.index
            runCatching { boxInterface.mtu = javaInterface.mtu }
            boxInterface.dnsServer =
                StringArray(
                    linkProperties.dnsServers.mapNotNull { dns ->
                        // آدرس‌های IPv6 لینک‌لوکال پسوند zone (‎%wlan0‎) دارند که netip قبول نمی‌کند
                        dns.hostAddress?.substringBefore('%')
                    }.iterator(),
                )
            boxInterface.type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                else -> Libbox.InterfaceTypeOther
            }
            var flags = 0
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                flags = android.system.OsConstants.IFF_UP or android.system.OsConstants.IFF_RUNNING
            }
            if (javaInterface.isLoopback) flags = flags or android.system.OsConstants.IFF_LOOPBACK
            if (javaInterface.supportsMulticast()) flags = flags or android.system.OsConstants.IFF_MULTICAST
            boxInterface.flags = flags
            boxInterface.metered =
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            boxInterface.addresses = StringArray(
                javaInterface.interfaceAddresses.mapNotNull { address ->
                    val host = address.address.hostAddress ?: return@mapNotNull null
                    // حذف zone از IPv6 (‎%wlan0‎ و مانند آن) — وگرنه netip.ParsePrefix پنیک می‌کند
                    "${host.substringBefore('%')}/${address.networkPrefixLength}"
                }.iterator(),
            )
            interfaces.add(boxInterface)
        }
        return InterfaceArray(interfaces.iterator())
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun readWIFIState(): WIFIState? = null

    // گو روی اندروید خودش سرتیفیکیت‌های سیستم را از مسیر استاندارد می‌خواند
    override fun systemCertificates(): StringIterator = StringArray(emptyList<String>().iterator())

    override fun clearDNSCache() {}

    override fun localDNSTransport(): LocalDNSTransport? = null

    // sendNotification در TunnelService بازنویسی می‌شود

    class StringArray(private val iterator: Iterator<String>) : StringIterator {
        override fun len(): Int = 0

        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): String = iterator.next()
    }

    private class InterfaceArray(
        private val iterator: Iterator<io.nekohasekai.libbox.NetworkInterface>,
    ) : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): io.nekohasekai.libbox.NetworkInterface = iterator.next()
    }
}

/**
 * تشخیص اینترفیس پیش‌فرض (Wi-Fi ↔ دیتا) برای هسته.
 */
object DefaultNetworkMonitor {

    private var listener: InterfaceUpdateListener? = null
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            push()
        }

        override fun onLinkPropertiesChanged(network: android.net.Network, properties: android.net.LinkProperties) {
            push()
        }

        override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) {
            push()
        }

        @SuppressLint("NewApi")
        private fun push() {
            val current = listener ?: return
            try {
                val context = MyVpnApplication.instance
                val connectivity = context.getSystemService(ConnectivityManager::class.java)
                val activeNetwork = connectivity.activeNetwork ?: return
                val properties = connectivity.getLinkProperties(activeNetwork) ?: return
                val capabilities = connectivity.getNetworkCapabilities(activeNetwork)
                val name = properties.interfaceName ?: return
                val index = NetworkInterface.getNetworkInterfaces().toList()
                    .firstOrNull { it.name == name }?.index ?: -1
                val expensive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
                current.updateDefaultInterface(name, index, expensive, false)
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "update default interface", e)
            }
        }
    }

    @Synchronized
    fun setListener(newListener: InterfaceUpdateListener?) {
        listener = newListener
        val context = MyVpnApplication.instance
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        if (newListener != null && !registered) {
            runCatching { connectivity.registerDefaultNetworkCallback(callback) }
                .onFailure { Log.e("NetworkMonitor", "register callback", it) }
            registered = true
        } else if (newListener == null && registered) {
            runCatching { connectivity.unregisterNetworkCallback(callback) }
            registered = false
        }
    }
}
