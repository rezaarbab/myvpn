package com.myvpn.app

import android.util.Log
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.Connections
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OutboundGroupIterator
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.libbox.StringIterator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrafficStats(
    val uplink: Long = 0,
    val downlink: Long = 0,
    val uplinkTotal: Long = 0,
    val downlinkTotal: Long = 0,
    val connections: Int = 0,
)

/**
 * وضعیت سراسری اپ: پل بین سرویس VPN و رابط Compose.
 * آمار و لاگ از هسته از طریق CommandClient (سوکت یونیکس محلی) می‌آید.
 */
object VpnManager {

    enum class Status { STOPPED, STARTING, STARTED, STOPPING }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(Status.STOPPED)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _traffic = MutableStateFlow(TrafficStats())
    val traffic: StateFlow<TrafficStats> = _traffic.asStateFlow()

    // تاریخچه‌ی سرعت برای نمودار زنده (حداکثر ۶۰ نمونه = ۶۰ ثانیه)
    private val _speedHistory = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
    val speedHistory: StateFlow<List<Pair<Long, Long>>> = _speedHistory.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _publicIp = MutableStateFlow<String?>(null)
    val publicIp: StateFlow<String?> = _publicIp.asStateFlow()

    private val _intranetIp = MutableStateFlow<String?>(null)
    val intranetIp: StateFlow<String?> = _intranetIp.asStateFlow()

    private var observerLoopStarted = false

    fun setStatus(value: Status) {
        _status.value = value
        if (value == Status.STOPPED) {
            _traffic.value = TrafficStats()
            _speedHistory.value = emptyList()
        }
        if (value == Status.STARTED) {
            scope.launch {
                fetchPublicIp()
                detectIntranetIp()
            }
        }
    }

    /** IP عمومی خروجی (از داخل تانل) */
    private fun fetchPublicIp() {
        runCatching {
            val conn = java.net.URL("https://api.ipify.org").openConnection() as javax.net.ssl.HttpsURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val ip = conn.inputStream.bufferedReader().readText().trim()
            if (ip.isNotBlank()) _publicIp.value = ip
        }.onFailure { _publicIp.value = null }
    }

    /** IP داخلی دستگاه در شبکه‌ی فعلی */
    private fun detectIntranetIp() {
        runCatching {
            val ip = java.net.NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
            if (!ip.isNullOrBlank()) _intranetIp.value = ip
        }
    }

    fun onTraffic(stats: TrafficStats) {
        _traffic.value = stats
        val history = _speedHistory.value.toMutableList()
        history.add(stats.downlink to stats.uplink)
        if (history.size > 60) history.removeAt(0)
        _speedHistory.value = history
    }

    fun setError(message: String?) {
        _lastError.value = message
    }

    fun appendLog(message: String) {
        val list = _logs.value.toMutableList()
        list.add(message)
        if (list.size > 500) list.subList(0, list.size - 500).clear()
        _logs.value = list
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // ---- اتصال به هسته برای آمار و لاگ ----

    fun observe() {
        scope.launch {
            if (observerLoopStarted) return@launch
            observerLoopStarted = true
            // حلقه‌ی شفابخش: اگر جریان وضعیت قطع شود دوباره وصل می‌شود
            while (true) {
                if (commandClient == null) {
                    runCatching { ensureClient() }
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    @Synchronized
    private fun ensureClient() {
        if (commandClient != null) return
        // کلاینت 1.12 در هر اتصال فقط یک کامند می‌پذیرد
        val options = CommandClientOptions()
        options.command = Libbox.CommandStatus
        options.statusInterval = 1_000_000_000L
        val client = CommandClient(Handler(), options)
        try {
            client.connect()
            commandClient = client
            appendLog("وضعیت: متصل به جریان آمار هسته")
        } catch (e: Exception) {
            // سرویس هنوز اجرا نشده یا سوکت آماده نیست — حلقه دوباره تلاش می‌کند
            runCatching { client.disconnect() }
            appendLog("command client: ${e.message}")
        }
    }

    fun releaseClient() {
        synchronized(this) {
            commandClient?.let { runCatching { it.disconnect() } }
            commandClient = null
        }
    }

    private var commandClient: CommandClient? = null

    fun onClientDisconnected() {
        synchronized(this) {
            runCatching { commandClient?.disconnect() }
            commandClient = null
        }
    }

    private class Handler : CommandClientHandler {
        override fun connected() {
            Log.d("VpnManager", "command client connected")
        }

        override fun disconnected(message: String?) {
            VpnManager.onClientDisconnected()
        }

        override fun writeStatus(message: StatusMessage?) {
            message ?: return
            VpnManager.onTraffic(
                TrafficStats(
                    uplink = message.uplink,
                    downlink = message.downlink,
                    uplinkTotal = message.uplinkTotal,
                    downlinkTotal = message.downlinkTotal,
                    connections = message.connectionsIn + message.connectionsOut,
                ),
            )
        }

        override fun writeLogs(messageList: StringIterator?) {
            messageList ?: return
            while (messageList.hasNext()) {
                VpnManager.appendLog(messageList.next())
            }
        }

        override fun clearLogs() = VpnManager.clearLogs()

        override fun writeGroups(message: OutboundGroupIterator?) {}

        override fun initializeClashMode(modeList: StringIterator?, currentMode: String?) {}

        override fun updateClashMode(newMode: String?) {}

        override fun writeConnections(message: Connections?) {}
    }
}
