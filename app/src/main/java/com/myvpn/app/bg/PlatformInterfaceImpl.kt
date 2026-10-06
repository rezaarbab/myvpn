package com.myvpn.app.bg

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.Notification
import com.myvpn.app.MyVpnApplication
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import android.os.Process

interface PlatformInterfaceImpl : PlatformInterface {

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    // در TunnelService با protect(fd) بازنویسی می‌شود
    override fun autoDetectInterfaceControl(fd: Int) {}

    override fun openTun(options: TunOptions): Int {
        error("android: tun inbound requires VPN service")
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner {
        val connectivity = MyVpnApplication.instance.getSystemService(ConnectivityManager::class.java)
        val uid = connectivity.getConnectionOwnerUid(
            ipProtocol,
            InetSocketAddress(sourceAddress, sourcePort),
            InetSocketAddress(destinationAddress, destinationPort),
        )
        if (uid == Process.INVALID_UID) error("android: connection owner not found")
        val packages = MyVpnApplication.instance.packageManager.getPackagesForUid(uid)
        val owner = ConnectionOwner()
        owner.userId = uid
        owner.userName = packages?.firstOrNull() ?: ""
        owner.setAndroidPackageNames(
            StringArray((packages?.toList() ?: emptyList()).iterator()),
        )
        return owner
    }

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
                StringArray(linkProperties.dnsServers.mapNotNull { it.hostAddress }.iterator())
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
                javaInterface.interfaceAddresses.map { address ->
                    if (address.address is Inet6Address) {
                        "${(address.address as Inet6Address).hostAddress}/${address.networkPrefixLength}"
                    } else {
                        "${address.address.hostAddress}/${address.networkPrefixLength}"
                    }
                }.iterator(),
            )
            interfaces.add(boxInterface)
        }
        return InterfaceArray(interfaces.iterator())
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun clearDNSCache() {}

    override fun readWIFIState(): WIFIState? = null

    override fun localDNSTransport(): LocalDNSTransport? = null

    override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}

    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}

    override fun registerMyInterface(name: String?) {}

    override fun usePlatformShell(): Boolean = false

    override fun checkPlatformShell() {
        error("not supported")
    }

    override fun openShellSession(
        user: PlatformUser?,
        command: String?,
        environ: StringIterator?,
        term: String?,
        rows: Int,
        cols: Int,
    ): ShellSession {
        error("not supported")
    }

    override fun lookupUser(username: String?): PlatformUser {
        error("not supported")
    }

    override fun lookupSFTPServer(): String {
        error("not supported")
    }

    override fun readSystemSSHHostKey(): String {
        error("not supported")
    }

    override fun tailscaleHostname(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    override fun usePlatformBridge(): Boolean = false

    override fun createBridge(options: BridgeOptions?): BridgeSession {
        error("not supported")
    }

    // sendNotification / cancelNotification در TunnelService بازنویسی می‌شوند

    class StringArray(private val iterator: Iterator<String>) : StringIterator {
        override fun len(): Int = 0 // توسط هسته استفاده نمی‌شود

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
            connectivity.registerDefaultNetworkCallback(callback)
            registered = true
        } else if (newListener == null && registered) {
            runCatching { connectivity.unregisterNetworkCallback(callback) }
            registered = false
        }
    }
}
