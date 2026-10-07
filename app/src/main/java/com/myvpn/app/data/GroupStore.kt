package com.myvpn.app.data

import com.myvpn.app.VpnManager
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.Connections
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OutboundGroup
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

data class ProxyGroupItem(
    val tag: String,
    val type: String,
    val delay: Int,
    val testedAt: Long,
)

data class ProxyGroup(
    val tag: String,
    val type: String,
    val selectable: Boolean,
    val selected: String,
    val isExpand: Boolean,
    val items: List<ProxyGroupItem>,
) {
    val isSelector: Boolean get() = selectable || type.equals("selector", ignoreCase = true)
    val isUrlTest: Boolean get() = type.equals("urltest", ignoreCase = true)
}

/**
 * گروه‌های خروجی (selector/urltest) که هسته از طریق CommandClient ارسال می‌کند.
 * کانال وضعیت جدا است؛ اینجا یک اتصال دوم فقط برای CommandGroup باز می‌شود
 * (در libbox ۱.۱۲ هر اتصال یک نوع پیام می‌پذیرد).
 */
object GroupStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _groups = MutableStateFlow<List<ProxyGroup>>(emptyList())
    val groups: StateFlow<List<ProxyGroup>> = _groups.asStateFlow()

    private val _testing = MutableStateFlow<String?>(null)
    val testing: StateFlow<String?> = _testing.asStateFlow()

    private var client: CommandClient? = null

    val hasGroups: Boolean get() = _groups.value.isNotEmpty()

    fun observe() {
        scope.launch {
            if (client != null) return@launch
            val options = CommandClientOptions()
            options.command = Libbox.CommandGroup
            val newClient = CommandClient(Handler(), options)
            try {
                newClient.connect()
                client = newClient
            } catch (e: Exception) {
                runCatching { newClient.disconnect() }
                VpnManager.appendLog("گروه‌ها: در دسترس نیست (${e.message})")
            }
        }
    }

    fun release() {
        synchronized(this) {
            client?.let { runCatching { it.disconnect() } }
            client = null
        }
        _groups.value = emptyList()
    }

    fun onDisconnected(message: String?) {
        synchronized(this) {
            runCatching { client?.disconnect() }
            client = null
        }
        VpnManager.appendLog("گروه‌ها: قطع شد (${message ?: "نامشخص"})")
    }

    /** یک دور تست تأخیر روی کل گروه (urltest). */
    fun urlTest(groupTag: String) {
        scope.launch {
            _testing.value = groupTag
            runCatching {
                standalone().urlTest(groupTag)
            }.onFailure { VpnManager.appendLog("تست تأخیر $groupTag: ${it.message}") }
            _testing.value = null
        }
    }

    /** انتخاب خروجی داخل یک گروه selector. */
    fun select(groupTag: String, itemTag: String) {
        scope.launch {
            runCatching {
                standalone().selectOutbound(groupTag, itemTag)
            }.onFailure { VpnManager.appendLog("انتخاب $itemTag: ${it.message}") }
        }
    }

    fun setExpanded(groupTag: String, expanded: Boolean) {
        scope.launch {
            runCatching { standalone().setGroupExpand(groupTag, expanded) }
        }
    }

    private fun standalone(): CommandClient = Libbox.newStandaloneCommandClient()

    private class Handler : CommandClientHandler {
        override fun connected() = Unit
        override fun disconnected(message: String?) = GroupStore.onDisconnected(message)
        override fun clearLogs() = Unit
        override fun writeLogs(messageList: StringIterator?) = Unit
        override fun writeStatus(message: StatusMessage?) = Unit
        override fun initializeClashMode(modeList: StringIterator?, currentMode: String?) = Unit
        override fun updateClashMode(newMode: String?) = Unit
        override fun writeConnections(message: Connections?) = Unit

        override fun writeGroups(message: OutboundGroupIterator?) {
            if (message == null) return
            val list = mutableListOf<ProxyGroup>()
            while (message.hasNext()) {
                val group: OutboundGroup = message.next() ?: break
                val items = mutableListOf<ProxyGroupItem>()
                group.items?.let { iterator ->
                    while (iterator.hasNext()) {
                        val item = iterator.next() ?: break
                        items.add(
                            ProxyGroupItem(
                                tag = item.tag,
                                type = item.type,
                                delay = item.urlTestDelay,
                                testedAt = item.urlTestTime,
                            ),
                        )
                    }
                }
                list.add(
                    ProxyGroup(
                        tag = group.tag,
                        type = group.type,
                        selectable = group.selectable,
                        selected = group.selected,
                        isExpand = group.isExpand,
                        items = items,
                    ),
                )
            }
            GroupStore.publish(list)
        }
    }

    private fun publish(list: List<ProxyGroup>) {
        _groups.value = list
    }
}

