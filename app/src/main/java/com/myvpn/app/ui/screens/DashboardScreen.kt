package com.myvpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.VpnManager
import com.myvpn.app.data.GroupStore
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.data.TrafficStore
import com.myvpn.app.ui.components.AppCard
import com.myvpn.app.ui.components.DonutChart
import com.myvpn.app.ui.components.InfoHeader
import com.myvpn.app.ui.components.NetworkSpeedCard
import com.myvpn.app.ui.components.WidgetGrid
import com.myvpn.app.ui.components.WidgetSpec
import com.myvpn.app.ui.formatBytes

/**
 * داشبورد: گرید ویجتی ۸ ستونی با همان ترتیب FlClash —
 * سرعت شبکه (۸×۲)، پروفایل فعال، تشخیص شبکه، مصرف و IP داخلی.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenProfiles: () -> Unit = {},
    onOpenProxies: () -> Unit = {},
) {
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()
    val history by VpnManager.speedHistory.collectAsStateWithLifecycle()
    val publicIp by VpnManager.publicIp.collectAsStateWithLifecycle()
    val intranetIp by VpnManager.intranetIp.collectAsStateWithLifecycle()
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val groups by GroupStore.groups.collectAsStateWithLifecycle()
    val error by VpnManager.lastError.collectAsStateWithLifecycle()
    val totalDown by TrafficStore.totalDown.collectAsStateWithLifecycle()
    val totalUp by TrafficStore.totalUp.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        VpnManager.observe()
        if (status == VpnManager.Status.STARTED) GroupStore.observe()
    }

    val connected = status == VpnManager.Status.STARTED
    val active = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()

    val specs = buildList {
        add(WidgetSpec("speed", 8, 2))
        add(WidgetSpec("profile", 4, 1))
        add(WidgetSpec("detection", 4, 1))
        add(WidgetSpec("usage", 4, 1))
        add(WidgetSpec("intranet", 4, 1))
        if (groups.isNotEmpty()) add(WidgetSpec("groups", 4, 1))
        if (connected) add(WidgetSpec("connections", 4, 1))
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        WidgetGrid(specs = specs) { spec ->
            when (spec.id) {
                "speed" -> NetworkSpeedCard(
                    history = history,
                    downlink = traffic.downlink,
                    uplink = traffic.uplink,
                    live = connected,
                    modifier = Modifier.fillMaxSize(),
                    onClick = onOpenProxies,
                )

                "profile" -> ActiveProfileCard(
                    name = active?.name ?: "سروری انتخاب نشده",
                    detail = active?.let { "${it.type.uppercase()} • ${it.displayAddress}" } ?: "برای انتخاب ضربه بزنید",
                    active = connected,
                    modifier = Modifier.fillMaxSize(),
                    onClick = onOpenProfiles,
                )

                "detection" -> AppCard(modifier = Modifier.fillMaxSize()) {
                    InfoHeader("تشخیص شبکه", Icons.Filled.Info)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (publicIp != null) com.myvpn.app.ui.theme.FlTheme.success
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            publicIp ?: "—",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                "usage" -> UsageCard(
                    down = traffic.downlinkTotal,
                    up = traffic.uplinkTotal,
                    totalDown = totalDown,
                    totalUp = totalUp,
                    modifier = Modifier.fillMaxSize(),
                )

                "intranet" -> AppCard(modifier = Modifier.fillMaxSize()) {
                    InfoHeader("IP داخلی", Icons.Filled.List)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        intranetIp ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                "groups" -> AppCard(
                    modifier = Modifier.fillMaxSize(),
                    onClick = onOpenProxies,
                ) {
                    InfoHeader("گروه‌های پروکسی", Icons.Filled.List)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${groups.size} گروه • ${groups.firstOrNull()?.selected ?: "—"}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                "connections" -> AppCard(modifier = Modifier.fillMaxSize()) {
                    InfoHeader("اتصال‌های فعال", Icons.Filled.Info)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${traffic.connections}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                contentPadding = PaddingValues(12.dp),
            ) {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActiveProfileCard(
    name: String,
    detail: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AppCard(modifier = modifier, selected = active, onClick = onClick) {
        InfoHeader("پروفایل فعال", Icons.Filled.List)
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UsageCard(
    down: Long,
    up: Long,
    totalDown: Long,
    totalUp: Long,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                down = down,
                up = up,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "↓ ${formatBytes(down)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "↑ ${formatBytes(up)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "کل ${formatBytes(totalDown + totalUp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
