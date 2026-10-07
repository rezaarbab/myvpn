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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.abs

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
                // نویگیشن پیل شناور (سبک FlClash)
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 36.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBar(containerColor = Color.Transparent) {
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
    val publicIp by VpnManager.publicIp.collectAsStateWithLifecycle()
    val intranetIp by VpnManager.intranetIp.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { VpnManager.observe() }

    val selected = profiles.firstOrNull { it.id == selectedId }
    val connected = status == VpnManager.Status.STARTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- هدر ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("داشبورد", style = MaterialTheme.typography.headlineSmall)
            when (status) {
                VpnManager.Status.STARTED -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E9E6B),
                    modifier = Modifier.size(30.dp),
                )
                VpnManager.Status.STARTING, VpnManager.Status.STOPPING ->
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                else -> Unit
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "↓ ${formatBytes(traffic.downlink)}/s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(150.dp)) {
                    SpeedChart(history = speedHistory, modifier = Modifier.fillMaxSize())
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

        // ---- کارت سرور فعال + تشخیص شبکه (ترتیب FlClash: outboundMode → networkDetection) ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            InfoCard("IP عمومی", publicIp ?: "—", Modifier.weight(1f), green = publicIp != null)

            // کارت مصرف با دونات + لجند
            Card(Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("مصرف", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    UsageDonut(
                        down = traffic.downlinkTotal,
                        up = traffic.uplinkTotal,
                        modifier = Modifier.size(84.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    UsageLegend("آپلود", MaterialTheme.colorScheme.secondary, "↑ ${formatBytes(traffic.uplinkTotal)}")
                    UsageLegend("دانلود", MaterialTheme.colorScheme.primary, "↓ ${formatBytes(traffic.downlinkTotal)}")
                    Text(
                        "کل: ${formatBytes(totalDown + totalUp)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            InfoCard("IP داخلی", intranetIp ?: "—", Modifier.weight(1f))
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

@Composable
private fun InfoCard(title: String, value: String, modifier: Modifier = Modifier, green: Boolean = false) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (green) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UsageLegend(label: String, dotColor: Color, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

/** نمودار خطی زنده — پورت دقیق line_chart.dart از FlClash */
private const val CHART_CAPACITY = 60
private const val MIN_SPEED_SCALE = 8.0 * 1024.0
private const val BLUR_SIGMA = 1.5
private val BLUR_RADIUS = kotlin.math.ceil(BLUR_SIGMA * 2.5).toInt()
private val BLUR_KERNEL = FloatArray(BLUR_RADIUS + 1) { d ->
    kotlin.math.exp(-d * d / (2.0 * BLUR_SIGMA * BLUR_SIGMA)).toFloat()
}

private fun gaussianBlur(values: List<Double>): List<Double> = List(values.size) { i ->
    var sum = 0.0
    var weight = 0.0
    for (j in maxOf(0, i - BLUR_RADIUS)..minOf(values.size - 1, i + BLUR_RADIUS)) {
        val k = BLUR_KERNEL[abs(j - i)]
        sum += k * values[j]
        weight += k
    }
    sum / weight
}

@Composable
private fun SpeedChart(history: List<Pair<Long, Long>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val length = CHART_CAPACITY + BLUR_RADIUS + 1
        val values = history.map { it.first.toDouble() }
        val window = DoubleArray(length)
        val count = minOf(values.size, length)
        for (i in 0 until count) window[length - count + i] = values[values.size - count + i]
        val heights = gaussianBlur(window.toList())

        val strokeWidthPx = 2.dp.toPx()
        val baseline = size.height * 0.7f
        val step = size.width / (CHART_CAPACITY - 1)
        val firstX = size.width - (length - 1) * step

        fun xOf(i: Int) = firstX + i * step
        fun yf(i: Int) = -heights[i]

        val slopes = DoubleArray(length) { i ->
            if (i == 0 || i == length - 1) 0.0
            else (((heights[i + 1] - heights[i - 1]) / 2.0).coerceIn(-3.0 * heights[i], 3.0 * heights[i]))
        }

        val top = maxOf(MIN_SPEED_SCALE, heights.max())
        val yScale = ((baseline - strokeWidthPx) / top).toFloat()
        fun mapY(yf: Double) = (baseline + yf * yScale).toFloat()

        val line = Path()
        line.moveTo(xOf(0), mapY(yf(0)))
        for (i in 1 until length) {
            line.cubicTo(
                xOf(i - 1) + step / 3f, mapY(yf(i - 1) - slopes[i - 1] / 3f),
                xOf(i) - step / 3f, mapY(yf(i) + slopes[i] / 3f),
                xOf(i), mapY(yf(i)),
            )
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(xOf(0), size.height)
            close()
        }
        clipRect(0f, 0f, size.width, size.height) {
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0.10f)),
                ),
            )
            drawPath(
                line,
                color = lineColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

/** دونات مصرف — پورت دقیق donut_chart.dart */
@Composable
private fun UsageDonut(down: Long, up: Long, modifier: Modifier = Modifier) {
    val upColor = MaterialTheme.colorScheme.secondary
    val downColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val diameter = minOf(size.width, size.height)
        val strokeWidthPx = (diameter * 0.12f).coerceIn(10.dp.toPx(), 16.dp.toPx())
        val radius = (diameter - strokeWidthPx) / 2f
        if (radius <= 0f) return@Canvas
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val rect = androidx.compose.ui.geometry.Rect(
            center - androidx.compose.ui.geometry.Offset(radius, radius),
            androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
        )

        val total = (down + up).coerceAtLeast(1L)
        val fractions = listOf(up.toFloat() / total, down.toFloat() / total)
        val trackAlpha = 1f - fractions.fold(0f) { s, f -> s + f }
        if (trackAlpha * 255f >= 1f) {
            drawCircle(
                trackColor.copy(alpha = trackAlpha),
                radius = radius,
                center = center,
                style = Stroke(strokeWidthPx),
            )
        }

        val gapRatio = 0.3f
        val fullDotFraction = 0.01f
        val minSweep = 1e-3f
        val colors = listOf(upColor, downColor)
        val weights = fractions.map { minOf(1f, it / fullDotFraction) }
        val slot = strokeWidthPx * (1f + gapRatio) / radius
        val reserved = weights.fold(0f) { s, w -> s + w * slot }
        val available = (2.0 * Math.PI - reserved).toFloat().coerceAtLeast(0f)

        var start = (-Math.PI / 2).toFloat()
        for (i in fractions.indices) {
            val weight = weights[i]
            val sweep = available * fractions[i]
            start += weight * slot / 2f
            if (weight > 0f) {
                if (sweep > minSweep) {
                    drawArc(
                        colors[i],
                        start,
                        sweep,
                        false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(strokeWidthPx * weight, cap = StrokeCap.Round),
                    )
                } else {
                    drawCircle(
                        colors[i],
                        radius = strokeWidthPx * weight / 2f,
                        center = center + androidx.compose.ui.geometry.Offset(
                            kotlin.math.cos(start) * radius,
                            kotlin.math.sin(start) * radius,
                        ),
                    )
                }
            }
            start += sweep + weight * slot / 2f
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
