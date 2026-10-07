package com.myvpn.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val connected = status == VpnManager.Status.STARTED
    val busy = status == VpnManager.Status.STARTING || status == VpnManager.Status.STOPPING
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (currentRoute != ROUTE_ADD) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_HOME,
                        onClick = { navigateTab(navController, ROUTE_HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("داشبورد") },
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
        floatingActionButton = {
            if (currentRoute == ROUTE_HOME) {
                LargeFloatingActionButton(
                    onClick = {
                        if (!busy) {
                            if (status == VpnManager.Status.STOPPED) onConnect() else TunnelService.stop(context)
                        }
                    },
                    containerColor = if (connected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (connected) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                        )
                    }
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

// ---------- داشبورد (سبک FlClash) ----------

@Composable
fun HomeScreen(onConnect: () -> Unit, onOpenServers: () -> Unit) {
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    val speedHistory by VpnManager.speedHistory.collectAsStateWithLifecycle()
    val error by VpnManager.lastError.collectAsStateWithLifecycle()
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val totalDown by TrafficStore.totalDown.collectAsStateWithLifecycle()
    val totalUp by TrafficStore.totalUp.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { VpnManager.observe() }

    val selected = profiles.firstOrNull { it.id == selectedId }
    val connected = status == VpnManager.Status.STARTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- هدر: عنوان + دکمه وضعیت (سبک FlClash) ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("داشبورد", style = MaterialTheme.typography.headlineSmall)
            if (status == VpnManager.Status.STARTING || status == VpnManager.Status.STOPPING) {
                CircularProgressIndicator(Modifier.size(34.dp), strokeWidth = 3.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (connected) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (connected) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (connected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // ---- کارت سرعت شبکه با نمودار زنده ----
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("سرعت شبکه", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "↑ ${formatBytes(traffic.uplink)}/s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            "↓ ${formatBytes(traffic.downlink)}/s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(150.dp)) {
                    SpeedChart(
                        history = speedHistory,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (!connected && speedHistory.isEmpty()) {
                        Text(
                            "برای مشاهده‌ی نمودار وصل شوید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }

        // ---- ردیف دوم: مصرف + سرور فعال ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // کارت مصرف با دونات
            Card(Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("مصرف", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { TrafficStore.reset() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) { Text("ریست", style = MaterialTheme.typography.labelMedium) }
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageDonut(
                        down = traffic.downlinkTotal,
                        up = traffic.uplinkTotal,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    UsageLegend("↓", "نشست", formatBytes(traffic.downlinkTotal))
                    UsageLegend("↑", "نشست", formatBytes(traffic.uplinkTotal))
                    UsageLegend("Σ", "کل", formatBytes(totalDown + totalUp))
                }
            }

            // کارت سرور فعال
            Card(
                onClick = onOpenServers,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("سرور فعال", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(com.myvpn.app.ui.theme.BrandGradient, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                selected?.name?.trim()?.firstOrNull()?.uppercase() ?: "+",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            selected?.type?.uppercase() ?: "—",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        selected?.name ?: "برای انتخاب ضربه بزنید",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    }
}

/** نمودار خطی زنده با فیل گرادیانی (سبک FlClash) */
@Composable
private fun SpeedChart(history: List<Pair<Long, Long>>, modifier: Modifier = Modifier) {
    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        if (history.size < 2) return@Canvas
        val maxV = maxOf(history.maxOf { it.first }, history.maxOf { it.second }, 1L).toFloat()
        val w = size.width
        val h = size.height
        val step = w / 59f

        fun buildPath(get: (Pair<Long, Long>) -> Long): Path {
            val path = Path()
            history.forEachIndexed { i, sample ->
                val x = w - (history.size - 1 - i) * step
                val y = h - (get(sample) / maxV) * (h * 0.88f) - h * 0.06f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        val downPath = buildPath { it.first }
        val firstX = w - (history.size - 1) * step
        val fillPath = Path().apply {
            addPath(downPath)
            lineTo(w, h)
            lineTo(firstX, h)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                listOf(downColor.copy(alpha = 0.35f), Color.Transparent),
            ),
        )
        drawPath(downPath, downColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        drawPath(buildPath { it.second }, upColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

/** دونات نسبت مصرف دانلود/آپلود */
@Composable
private fun UsageDonut(down: Long, up: Long, modifier: Modifier = Modifier) {
    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val total = (down + up).coerceAtLeast(1L).toFloat()
        val stroke = 14f
        val inset = stroke
        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        val downSweep = (down.toFloat() / total) * 360f
        if (downSweep > 0f) {
            drawArc(
                color = downColor,
                startAngle = -90f,
                sweepAngle = downSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (downSweep < 360f && up > 0) {
            drawArc(
                color = upColor,
                startAngle = -90f + downSweep,
                sweepAngle = 360f - downSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun UsageLegend(icon: String, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge)
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
