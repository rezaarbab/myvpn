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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.VpnManager
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.data.ServerPinger
import com.myvpn.app.data.ServerProfile
import com.myvpn.app.ui.components.AppCard
import com.myvpn.app.ui.components.StatusChip
import com.myvpn.app.ui.delayColor
import com.myvpn.app.ui.formatBytes
import com.myvpn.app.ui.theme.AppRadius
import com.myvpn.app.ui.theme.SuperEllipseShape
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class ProfileSort(val label: String) {
    Default("پیش‌فرض"),
    Name("نام"),
    Delay("تأخیر"),
}

/**
 * صفحه‌ی پروفایل‌ها: کارت‌های شبکه‌ی آجری (staggered) با هایلایت پروفایل فعال،
 * منوی هر کارت (ویرایش/حذف/کپی) و تست تأخیر دسته‌جمعی — سبک FlClash profiles.
 */
@Composable
fun ProfilesScreen(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit = {},
    onEdit: (String) -> Unit = {},
) {
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val selectedId by ProfileStore.selectedId.collectAsStateWithLifecycle()
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val traffic by VpnManager.traffic.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ProfileSort.Default) }
    var sortMenu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServerProfile?>(null) }
    val delays = remember { mutableStateMapOf<String, Long?>() }
    var pinging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun pingAll() {
        if (pinging || profiles.isEmpty()) return
        pinging = true
        scope.launch {
            coroutineScope {
                profiles.map { profile ->
                    async { delays[profile.id] = ServerPinger.ping(profile) }
                }.awaitAll()
            }
            pinging = false
        }
    }

    LaunchedEffect(Unit) { pingAll() }

    val visible = profiles
        .filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.server.contains(query, ignoreCase = true)
        }
        .let { list ->
            when (sort) {
                ProfileSort.Name -> list.sortedBy { it.name.lowercase() }
                ProfileSort.Delay -> list.sortedBy { delays[it.id] ?: Long.MAX_VALUE }
                ProfileSort.Default -> list
            }
        }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SuperEllipseShape(AppRadius.md),
                    label = { Text("جستجو") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { sortMenu = true }) {
                        Text("مرتب‌سازی: ${sort.label}")
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            ProfileSort.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        sort = mode
                                        sortMenu = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { pingAll() }, enabled = !pinging) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تست تأخیر همه")
                    }
                }
            }

            if (visible.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxWidth(),
                    title = if (profiles.isEmpty()) "پروفایلی ندارید" else "نتیجه‌ای یافت نشد",
                    body = "یک لینک اشتراکی (vless، vmess، trojan، hysteria2 و…) یا کانفیگ JSON خام اضافه کنید.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 270.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { profile ->
                        val isSelected = profile.id == selectedId
                        val live = status == VpnManager.Status.STARTED && isSelected
                        ProfileCard(
                            profile = profile,
                            selected = isSelected,
                            live = live,
                            down = if (live) traffic.downlinkTotal else profile.usedDown,
                            up = if (live) traffic.uplinkTotal else profile.usedUp,
                            delay = delays[profile.id],
                            pinging = pinging && !delays.containsKey(profile.id),
                            onSelect = { ProfileStore.select(profile.id) },
                            onEdit = { onEdit(profile.id) },
                            onDelete = { deleteTarget = profile },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = SuperEllipseShape(AppRadius.md),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "افزودن پروفایل")
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف پروفایل") },
            text = { Text("«${target.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    ProfileStore.delete(target.id)
                    deleteTarget = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("انصراف") }
            },
            shape = SuperEllipseShape(AppRadius.xxl),
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ServerProfile,
    selected: Boolean,
    live: Boolean,
    down: Long,
    up: Long,
    delay: Long?,
    pinging: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(
        selected = selected,
        shape = SuperEllipseShape(AppRadius.xl),
        onClick = onSelect,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "+",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    profile.displayAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "گزینه‌ها")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("انتخاب") }, onClick = { menuOpen = false; onSelect() })
                    DropdownMenuItem(text = { Text("ویرایش") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(profile.type.uppercase(), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            when {
                pinging -> Text("…", style = MaterialTheme.typography.labelSmall)
                delay != null -> Text(
                    "$delay ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = delayColor(delay),
                )
            }
            Spacer(Modifier.weight(1f))
            if (down > 0 || up > 0) {
                Text(
                    "↓${formatBytes(down)}  ↑${formatBytes(up)}${if (live) " (زنده)" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
