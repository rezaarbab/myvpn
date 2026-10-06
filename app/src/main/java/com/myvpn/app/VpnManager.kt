package com.myvpn.app

import android.util.Log
import go.Seq
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.ConnectionEvents
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LogIterator
import io.nekohasekai.libbox.OutboundGroup
import io.nekohasekai.libbox.OutboundGroupItem
import io.nekohasekai.libbox.OutboundGroupItemIterator
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

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun setStatus(value: Status) {
        _status.value = value
        if (value == Status.STOPPED) {
            _traffic.value = TrafficStats()
        }
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
            runCatching { ensureClient() }
        }
    }

    @Synchronized
    private fun ensureClient() {
        if (commandClient != null) return
        val options = CommandClientOptions()
        options.addCommand(Libbox.CommandStatus)
        options.addCommand(Libbox.CommandLog)
        options.statusInterval = 1 * 1000 * 1000 * 1000
        val client = CommandClient(Handler(), options)
        try {
            client.connect()
            commandClient = client
        } catch (e: Exception) {
            // سرویس هنوز اجرا نشده؛ بعداً دوباره تلاش می‌شود
            runCatching { client.disconnect() }
        }
    }

    fun releaseClient() {
        synchronized(this) {
            commandClient?.let { runCatching { it.disconnect() } }
            commandClient = null
        }
    }

    private var commandClient: CommandClient? = null

    private inner class Handler : CommandClientHandler {
        override fun connected() {
            Log.d(TAG, "command client connected")
        }

        override fun disconnected(message: String?) {
            synchronized(this@VpnManager) {
                runCatching { commandClient?.disconnect() }
                commandClient = null
            }
        }

        override fun writeStatus(message: StatusMessage?) {
            message ?: return
            _traffic.value = TrafficStats(
                uplink = message.uplink,
                downlink = message.downlink,
                uplinkTotal = message.uplinkTotal,
                downlinkTotal = message.downlinkTotal,
                connections = message.connectionsIn + message.connectionsOut,
            )
        }

        override fun writeLogs(messageList: LogIterator?) {
            messageList ?: return
            while (messageList.hasNext()) {
                val entry = messageList.next() ?: continue
                appendLog(entry.message)
            }
        }

        override fun clearLogs() = VpnManager.clearLogs()

        override fun setDefaultLogLevel(level: Int) {}

        override fun writeGroups(message: OutboundGroupIterator?) {}

        override fun writeOutbounds(message: OutboundGroupItemIterator?) {}

        override fun initializeClashMode(modeList: StringIterator?, currentMode: String?) {}

        override fun updateClashMode(newMode: String?) {}

        override fun writeConnectionEvents(events: ConnectionEvents?) {}
    }

    private const val TAG = "VpnManager"
}
