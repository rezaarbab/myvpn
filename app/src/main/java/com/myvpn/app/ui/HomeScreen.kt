package com.myvpn.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myvpn.app.VpnManager
import com.myvpn.app.bg.TunnelService
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.data.ServerProfile

@Composable
fun AppNavHost(onConnect: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onConnect = onConnect,
                onOpenServers = { navController.navigate("servers") },
                onOpenLogs = { navController.navigate("logs") },
            )
        }
        composable("servers") {
            ServersScreen(
                onAdd = { navController.navigate("add") },
                onBack = { navController.popBackStack() },
            )
        }
        composable("add") {
            AddServerScreen(onBack = { navController.popBackStack() })
        }
        composable("logs") {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun HomeScreen(
    onConnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    val error by VpnManager.lastError.collectAsStateWithLifecycle()
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { VpnManager.observe() }

    val selected = profiles.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        val (statusLabel, statusColor) = when (status) {
            VpnManager.Status.STOPPED -> "قطع" to MaterialTheme.colorScheme.error
            VpnManager.Status.STARTING -> "در حال اتصال…" to MaterialTheme.colorScheme.tertiary
            VpnManager.Status.STARTED -> "متصل" to Color(0xFF2E7D32)
            VpnManager.Status.STOPPING -> "در حال قطع…" to MaterialTheme.colorScheme.tertiary
        }
        Text(statusLabel, style = MaterialTheme.typography.headlineMedium, color = statusColor)

        Spacer(Modifier.height(32.dp))

        val busy = status == VpnManager.Status.STARTING || status == VpnManager.Status.STOPPING
        FilledIconButton(
            onClick = {
                if (!busy) {
                    if (status == VpnManager.Status.STOPPED) onConnect() else TunnelService.stop(context)
                }
            },
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
        ) {
            Icon(
                if (status == VpnManager.Status.STOPPED) Icons.Filled.PlayArrow else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        if (status == VpnManager.Status.STARTED) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text("↑ ${formatBytes(traffic.uplink)}/s", style = MaterialTheme.typography.bodyLarge)
                        Text("↓ ${formatBytes(traffic.downlink)}/s", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "کل ↑ ${formatBytes(traffic.uplinkTotal)} • کل ↓ ${formatBytes(traffic.downlinkTotal)} • اتصال‌ها: ${traffic.connections}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Card(
            onClick = onOpenServers,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("سرور فعال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected == null) {
                    Text("برای انتخاب سرور ضربه بزنید", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(selected.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${selected.type} • ${selected.displayAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row {
            TextButton(onClick = onOpenLogs) { Text("گزارش‌ها") }
        }
    }
}

fun formatBytes(value: Long): String {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(onAdd: () -> Unit, onBack: () -> Unit) {
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ServerProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سرورها") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "افزودن") }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هنوز سروری اضافه نشده است", textAlign = TextAlign.Center)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(padding)) {
                items(profiles.size) { index ->
                    val profile = profiles[index]
                    ListItem(
                        headlineContent = { Text(profile.name) },
                        supportingContent = { Text("${profile.type} • ${profile.displayAddress}") },
                        leadingContent = {
                            RadioButton(
                                selected = profile.id == selectedId,
                                onClick = { ProfileStore.select(profile.id) },
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { deleteTarget = profile }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف")
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
