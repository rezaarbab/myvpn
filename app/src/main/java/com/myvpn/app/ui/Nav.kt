package com.myvpn.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.VpnManager
import com.myvpn.app.bg.TunnelService
import com.myvpn.app.data.GroupStore
import com.myvpn.app.ui.components.AppShell
import com.myvpn.app.ui.components.Destination
import com.myvpn.app.ui.components.RunState
import com.myvpn.app.ui.screens.AddProfileScreen
import com.myvpn.app.ui.screens.DashboardScreen
import com.myvpn.app.ui.screens.LogsScreen
import com.myvpn.app.ui.screens.ProxiesScreen
import com.myvpn.app.ui.screens.ProfilesScreen
import com.myvpn.app.ui.screens.ToolsScreen

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_PROXIES = "proxies"
private const val ROUTE_PROFILES = "profiles"
private const val ROUTE_TOOLS = "tools"
private const val ROUTE_LOGS = "logs"
private const val ROUTE_ADD = "profile"

/**
 * ناوبری اصلی: تب پروکسی فقط وقتی دیده می‌شود که هسته گروهی ارسال کند —
 * همان رفتاری که FlClash با گروه‌های خالی دارد.
 */
@Composable
fun AppNavHost(onConnect: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val groups by GroupStore.groups.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val runState = when (status) {
        VpnManager.Status.STARTED -> RunState.Started
        VpnManager.Status.STARTING -> RunState.Starting
        VpnManager.Status.STOPPING -> RunState.Stopping
        VpnManager.Status.STOPPED -> RunState.Stopped
    }

    val destinations = buildList {
        add(Destination(ROUTE_DASHBOARD, "داشبورد", Icons.Filled.Home))
        if (groups.isNotEmpty()) add(Destination(ROUTE_PROXIES, "پروکسی", Icons.Filled.Share))
        add(Destination(ROUTE_PROFILES, "پروفایل", Icons.Filled.List))
        add(Destination(ROUTE_TOOLS, "ابزارها", Icons.Filled.Settings))
    }
    val onTopLevel = destinations.any { it.route == currentRoute }

    val title = when (currentRoute) {
        ROUTE_DASHBOARD -> "داشبورد"
        ROUTE_PROXIES -> "پروکسی‌ها"
        ROUTE_PROFILES -> "پروفایل‌ها"
        ROUTE_TOOLS -> "ابزارها"
        ROUTE_LOGS -> "گزارش‌ها"
        else -> if (currentRoute?.startsWith("$ROUTE_ADD/") == true) "ویرایش پروفایل" else "پروفایل جدید"
    }

    AppShell(
        destinations = destinations,
        currentRoute = currentRoute,
        onSelect = { route -> navigateTab(navController, route) },
        runState = runState,
        onStart = {
            if (runState == RunState.Stopped) onConnect() else TunnelService.stop(context)
        },
        title = title,
        showDock = onTopLevel,
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_DASHBOARD,
            modifier = contentModifier,
        ) {
            composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                    onOpenProfiles = { navigateTab(navController, ROUTE_PROFILES) },
                    onOpenProxies = { navigateTab(navController, ROUTE_PROXIES) },
                )
            }
            composable(ROUTE_PROXIES) { ProxiesScreen() }
            composable(ROUTE_PROFILES) {
                ProfilesScreen(
                    onAdd = { navController.navigate(ROUTE_ADD) },
                    onEdit = { id -> navController.navigate("$ROUTE_ADD/$id") },
                )
            }
            composable(ROUTE_TOOLS) {
                ToolsScreen(onOpenLogs = { navController.navigate(ROUTE_LOGS) })
            }
            composable(ROUTE_LOGS) { LogsScreen() }
            composable(ROUTE_ADD) {
                AddProfileScreen(profileId = null, onBack = { navController.popBackStack() })
            }
            composable("$ROUTE_ADD/{profileId}") { entry ->
                AddProfileScreen(
                    profileId = entry.arguments?.getString("profileId"),
                    onBack = { navController.popBackStack() },
                )
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
