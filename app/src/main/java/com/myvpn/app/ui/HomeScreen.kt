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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.myvpn.app.data.ServerProfile

private const val ROUTE_HOME = "home"
private const val ROUTE_SERVERS = "servers"
private const val ROUTE_LOGS = "logs"
private const val ROUTE_ADD = "add"

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

@Composable
fun HomeScreen(onConnect: () -> Unit, onOpenServers: () -> Unit) {
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    val error by VpnManager.lastError.collectAsStateWithLifecycle()
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { VpnManager.observe() }

    val selected = profiles.firstOrNull { it.id == selectedId }
    val busy = status == VpnManager.Status.STARTING || status == VpnManager.Status.STOPPING
    val connected = status == VpnManager.Status.STARTED

    val (statusLabel, statusColor) = when (status) {
        VpnManager.Status.STOPPED -> "قطع" to MaterialTheme.colorScheme.error
        VpnManager.Status.STARTING -> "در حال اتصال…" to MaterialTheme.colorScheme.tertiary
        VpnManager.Status.STARTED -> "متصل" to Color(0xFF2E7D32)
        VpnManager.Status.STOPPING -> "در حال قطع…" to MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // نشان وضعیت
        Surface(
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.14f),
            contentColor = statusColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(statusLabel, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.weight(0.8f))

        // دکمه‌ی اتصال
        Box(contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 3.dp,
                    color = statusColor,
                )
            }
            FilledIconButton(
                onClick = {
                    if (!busy) {
                        if (status == VpnManager.Status.STOPPED) onConnect() else TunnelService.stop(context)
                    }
                },
                modifier = Modifier.size(136.dp),
                shape = CircleShape,
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (connected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (connected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    if (connected) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            if (connected) "ضربه بزنید تا قطع شود" else "ضربه بزنید تا وصل شود",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(0.8f))

        // کاشی‌های آمار
        if (connected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile("↓", formatBytes(traffic.downlink) + "/s", Modifier.weight(1f))
                StatTile("↑", formatBytes(traffic.uplink) + "/s", Modifier.weight(1f))
                StatTile("⇅", formatBytes(traffic.downlinkTotal + traffic.uplinkTotal), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        // کارت سرور فعال
        Card(
            onClick = onOpenServers,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (selected?.name?.trim()?.firstOrNull()?.uppercase() ?: "+"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
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

        error?.let { message ->
            Spacer(Modifier.height(10.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun StatTile(icon: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(onAdd: () -> Unit) {
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ServerProfile?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("سرورها") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "افزودن") }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("هنوز سروری اضافه نشده است", textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(profiles.size) { index ->
                    val profile = profiles[index]
                    ListItem(
                        headlineContent = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${profile.type.uppercase()} • ${profile.displayAddress}") },
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
