package com.myvpn.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.VpnManager
import com.myvpn.app.data.LinkParser
import com.myvpn.app.data.ServerProfile

private val PROTOCOL_TYPES = listOf("vless", "vmess", "trojan", "shadowsocks", "hysteria2", "tuic")
private val TRANSPORT_TYPES = listOf("", "ws", "grpc", "http", "httpupgrade")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(onBack: () -> Unit) {
    var link by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    var type by rememberSaveable { mutableStateOf("vless") }
    var address by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var sni by rememberSaveable { mutableStateOf("") }
    var transport by rememberSaveable { mutableStateOf("") }
    var path by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("افزودن سرور") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("چسباندن لینک", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "لینک‌های vless / vmess / trojan / ss / hysteria2 / tuic یا کانفیگ JSON (هر خط یک سرور)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("لینک سرور") },
                    )
                    Button(onClick = {
                        try {
                            val parsed = LinkParser.parse(link)
                            parsed.forEach { ProfileStore.save(it) }
                            message = "${parsed.size} سرور ذخیره شد"
                            link = ""
                        } catch (e: Exception) {
                            message = e.message ?: "خطای ناشناخته"
                        }
                    }) { Text("افزودن") }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("افزودن دستی", style = MaterialTheme.typography.titleMedium)

                    DropdownField("نوع پروتکل", PROTOCOL_TYPES, type) { type = it }
                    OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("آدرس سرور") })
                    OutlinedTextField(port, { port = it }, Modifier.fillMaxWidth(), label = { Text("پورت") })
                    OutlinedTextField(
                        secret, { secret = it }, Modifier.fillMaxWidth(),
                        label = { Text("UUID یا رمز عبور") },
                    )
                    OutlinedTextField(
                        sni, { sni = it }, Modifier.fillMaxWidth(),
                        label = { Text("SNI / نام دامنه (اختیاری)") },
                    )
                    DropdownField("ترنسپورت", TRANSPORT_TYPES, transport) { transport = it }
                    if (transport == "ws" || transport == "http" || transport == "httpupgrade") {
                        OutlinedTextField(
                            path, { path = it }, Modifier.fillMaxWidth(),
                            label = { Text("مسیر (Path)") },
                        )
                    }

                    Button(onClick = {
                        try {
                            val portNumber = port.trim().toIntOrNull() ?: error("پورت نامعتبر است")
                            if (address.isBlank() || secret.isBlank()) error("آدرس و شناسه نمی‌تواند خالی باشد")
                            ProfileStore.save(
                                ServerProfile(
                                    id = LinkParser.newId(),
                                    name = "$address:$portNumber",
                                    type = type,
                                    server = address.trim(),
                                    serverPort = portNumber,
                                    uuid = if (type == "vless" || type == "vmess" || type == "tuic") secret.trim() else "",
                                    password = if (type in listOf("trojan", "shadowsocks", "hysteria2", "tuic")) secret.trim() else "",
                                    tls = true,
                                    serverName = sni.trim(),
                                    transport = transport,
                                    transportPath = path.trim(),
                                ),
                            )
                            message = "سرور ذخیره شد"
                            address = ""; port = ""; secret = ""; sni = ""; path = ""
                        } catch (e: Exception) {
                            message = e.message ?: "خطای ناشناخته"
                        }
                    }) { Text("ذخیره سرور") }
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (selected.isBlank()) "(بدون)" else selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option.isBlank()) "(بدون)" else option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val logs by VpnManager.logs.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { VpnManager.observe() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارش‌ها") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = {
                    TextButton(onClick = { VpnManager.clearLogs() }) { Text("پاک‌سازی") }
                },
            )
        },
    ) { padding ->
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("گزارشی موجود نیست")
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                reverseLayout = true,
            ) {
                itemsIndexed(logs.asReversed()) { _, line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
