package com.myvpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myvpn.app.VpnManager
import com.myvpn.app.bg.TunnelService
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.data.ServerPinger
import com.myvpn.app.data.ServerProfile
import com.myvpn.app.data.TrafficStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SERVERS = "servers"
private const val ROUTE_LOGS = "logs"
private const val ROUTE_ADD = "add"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AppNavHost(onConnect: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != ROUTE_ADD) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_HOME,
                        onClick = { navigateTab(navController, ROUTE_HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("اتصال") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_SERVERS,
                        onClick = { navigateTab(navController, ROUTE_SERVERS) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("سرورها") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_LOGS,
                        onClick = { navigateTab(navController, ROUTE_LOGS) },
                        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        label = { Text("گزارش") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_SETTINGS,
                        onClick = { navigateTab(navController, ROUTE_SETTINGS) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("تنظیمات") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onConnect = onConnect,
                    onOpenServers = { navigateTab(navController, ROUTE_SERVERS) },
                )
            }
            composable(ROUTE_SERVERS) {
                ServersScreen(onAdd = { navController.navigate(ROUTE_ADD) })
            }
            composable(ROUTE_ADD) {
                AddServerScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_LOGS) {
                LogsScreen()
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ---------- داشبورد (سبک Exclave) ----------

@Composable
fun HomeScreen(onConnect: () -> Unit, onOpenServers: () -> Unit) {
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    val error by VpnManager.lastError.collectAsStateWithLifecycle()
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val totalDown by TrafficStore.totalDown.collectAsStateWithLifecycle()
    val totalUp by TrafficStore.totalUp.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { VpnManager.observe() }

    val selected = profiles.firstOrNull { it.id == selectedId }
    val busy = status == VpnManager.Status.STARTING || status == VpnManager.Status.STOPPING
    val connected = status == VpnManager.Status.STARTED

    val statusColorRaw = when (status) {
        VpnManager.Status.STOPPED -> MaterialTheme.colorScheme.error
        VpnManager.Status.STARTING -> MaterialTheme.colorScheme.tertiary
        VpnManager.Status.STARTED -> Color(0xFF2E9E6B)
        VpnManager.Status.STOPPING -> MaterialTheme.colorScheme.tertiary
    }
    val statusColor by androidx.compose.animation.animateColorAsState(statusColorRaw, label = "statusColor")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- کارت وضعیت با سوییچ بزرگ (سبک Exclave) ----
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            statusLabel(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = statusColor,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            selected?.name ?: "سروری انتخاب نشده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(40.dp), strokeWidth = 3.dp, color = statusColor)
                        Spacer(Modifier.width(10.dp))
                    }
                    Switch(
                        checked = connected,
                        onCheckedChange = {
                            if (!busy) {
                                if (status == VpnManager.Status.STOPPED) onConnect() else TunnelService.stop(context)
                            }
                        },
                        enabled = !busy,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ترافیک زنده با آیکون
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrafficBadge("↓", formatBytes(traffic.downlink) + "/s", Modifier.weight(1f))
                    TrafficBadge("↑", formatBytes(traffic.uplink) + "/s", Modifier.weight(1f))
                    TrafficBadge("Σ", formatBytes(traffic.downlinkTotal + traffic.uplinkTotal), Modifier.weight(1f))
                }
            }
        }

        // ---- خطا ----
        if (error != null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // ---- مصرف ----
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("مصرف کل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${formatBytes(totalDown + totalUp)}  (↓${formatBytes(totalDown)} ↑${formatBytes(totalUp)})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = { TrafficStore.reset() }) { Text("ریست") }
            }
        }

        // ---- سرور فعال ----
        Card(
            onClick = onOpenServers,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(com.myvpn.app.ui.theme.BrandGradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        selected?.name?.trim()?.firstOrNull()?.uppercase() ?: "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (selected == null) {
                        Text("انتخاب سرور", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "برای افزودن ضربه بزنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            selected.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            selected.type.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrafficBadge(icon: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun statusLabel(): String = when (VpnManager.status.value) {
    VpnManager.Status.STOPPED -> "قطع"
    VpnManager.Status.STARTING -> "در حال اتصال…"
    VpnManager.Status.STARTED -> "متصل"
    VpnManager.Status.STOPPING -> "در حال قطع…"
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    var size = value / 1024.0
    val units = arrayOf("KB", "MB", "GB", "TB")
    var unit = 0
    while (size >= 1024 && unit < units.size - 1) {
        size /= 1024.0
        unit++
    }
    return String.format(java.util.Locale.US, "%.1f %s", size, units[unit])
}

/** رنگ پینگ: خوب سبز، متوسط زرد، ضعیف قرمز */
@Composable
private fun pingColor(ms: Long?): Color = when {
    ms == null -> MaterialTheme.colorScheme.error
    ms < 120 -> Color(0xFF2E9E6B)
    ms < 300 -> Color(0xFFF9A825)
    else -> MaterialTheme.colorScheme.error
}

// ---------- سرورها ----------

private enum class SortMode(val label: String) {
    DEFAULT("پیش‌فرض"),
    NAME("نام"),
    PING("پینگ"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(onAdd: () -> Unit) {
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ServerProfile?>(null) }

    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.DEFAULT) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val pingResults = remember { mutableStateMapOf<String, Long?>() }
    var pinging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val connected = status == VpnManager.Status.STARTED

    fun pingAll() {
        if (pinging || profiles.isEmpty()) return
        pinging = true
        scope.launch {
            coroutineScope {
                profiles.map { p ->
                    async { pingResults[p.id] = ServerPinger.ping(p) }
                }.awaitAll()
            }
            pinging = false
        }
    }

    LaunchedEffect(Unit) { pingAll() }

    val filtered = profiles
        .filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.server.contains(query, ignoreCase = true)
        }
        .let { list ->
            when (sortMode) {
                SortMode.NAME -> list.sortedBy { it.name.lowercase() }
                SortMode.PING -> list.sortedBy { pingResults[it.id] ?: Long.MAX_VALUE }
                SortMode.DEFAULT -> list
            }
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("سرورها") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "افزودن") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("جستجو") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            TextButton(onClick = { sortMenuOpen = true }) {
                                Text("مرتب‌سازی: ${sortMode.label}")
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        onClick = {
                                            sortMode = mode
                                            sortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        TextButton(
                            enabled = !pinging && filtered.isNotEmpty(),
                            onClick = { pingAll() },
                        ) { Text(if (pinging) "در حال گرفتن پینگ…" else "پینگ همه") }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (profiles.isEmpty()) "هنوز سروری اضافه نشده است" else "نتیجه‌ای یافت نشد",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(filtered.size) { index ->
                    val profile = filtered[index]
                    val ping = pingResults[profile.id]
                    val isLive = connected && profile.id == selectedId
                    val down = if (isLive) traffic.downlinkTotal else profile.usedDown
                    val up = if (isLive) traffic.uplinkTotal else profile.usedUp
                    val usageText = when {
                        isLive -> "↓${formatBytes(down)} ↑${formatBytes(up)} (زنده)"
                        down > 0 || up > 0 -> "↓${formatBytes(down)} ↑${formatBytes(up)}"
                        else -> null
                    }
                    ListItem(
                        headlineContent = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Column {
                                Text(
                                    "${profile.type.uppercase()} • ${profile.displayAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (usageText != null) {
                                    Text(
                                        usageText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            RadioButton(
                                selected = profile.id == selectedId,
                                onClick = { ProfileStore.select(profile.id) },
                            )
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when {
                                    pinging && ping == null ->
                                        Text(
                                            "…",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    ping != null ->
                                        Text(
                                            "${ping}ms",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = pingColor(ping),
                                        )
                                }
                                IconButton(onClick = { deleteTarget = profile }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف")
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف سرور") },
            text = { Text("«${target.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    ProfileStore.delete(target.id)
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("انصراف") }
            },
        )
    }
}
