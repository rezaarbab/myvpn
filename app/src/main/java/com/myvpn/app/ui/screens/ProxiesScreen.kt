package com.myvpn.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.VpnManager
import com.myvpn.app.data.GroupStore
import com.myvpn.app.data.ProxyGroup
import com.myvpn.app.data.ProxyGroupItem
import com.myvpn.app.ui.components.AppCard
import com.myvpn.app.ui.delayColor
import com.myvpn.app.ui.theme.SuperEllipseShape
import com.myvpn.app.ui.theme.AppRadius

/**
 * صفحه‌ی پروکسی‌ها مثل FlClash: نوار تب گروه‌ها و شبکه‌ی کارت‌های پروکسی با
 * رنگ تأخیر (سبز <۶۰۰، نارنجی >=۶۰۰، قرمز خطا) و تست با ضربه.
 */
@Composable
fun ProxiesScreen(modifier: Modifier = Modifier) {
    val groups by GroupStore.groups.collectAsStateWithLifecycle()
    val testing by GroupStore.testing.collectAsStateWithLifecycle()
    val status by VpnManager.status.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { if (status == VpnManager.Status.STARTED) GroupStore.observe() }

    var selectedIndex by remember { mutableIntStateOf(0) }

    if (groups.isEmpty()) {
        EmptyState(
            modifier = modifier,
            title = "گروه پروکسی‌ای وجود ندارد",
            body = "این صفحه وقتی پر می‌شود که کانفیگ شما شامل گروه‌های selector یا urltest باشد. " +
                "یک کانفیگ اشتراکی با گروه اضافه کنید و دوباره وصل شوید.",
        )
        return
    }

    val index = selectedIndex.coerceAtMost(groups.lastIndex)
    val group = groups[index]

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabRow(
                selectedTabIndex = index,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
            ) {
                groups.forEachIndexed { i, item ->
                    Tab(
                        selected = i == index,
                        onClick = { selectedIndex = i },
                        text = {
                            Text(
                                item.tag,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
            }
            IconButton(
                onClick = { GroupStore.urlTest(group.tag) },
                enabled = testing == null,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "تست تأخیر گروه")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(group.items, key = { "${group.tag}/${it.tag}" }) { item ->
                ProxyCard(
                    group = group,
                    item = item,
                    busy = testing == group.tag,
                    onClick = {
                        if (group.isSelector) GroupStore.select(group.tag, item.tag)
                        else GroupStore.urlTest(group.tag)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProxyCard(
    group: ProxyGroup,
    item: ProxyGroupItem,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val selected = group.selected == item.tag
    val delay = item.delay.takeIf { it != 0 }?.toLong()
    AppCard(
        selected = selected,
        onClick = onClick,
        shape = SuperEllipseShape(AppRadius.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 10.dp,
        ),
    ) {
        Text(
            item.tag,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (busy && delay == null) {
                Text("…", style = MaterialTheme.typography.labelSmall)
            } else if (delay != null) {
                Text(
                    "$delay",
                    style = MaterialTheme.typography.labelSmall,
                    color = delayColor(delay),
                )
                Text(
                    "ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = delayColor(delay),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
