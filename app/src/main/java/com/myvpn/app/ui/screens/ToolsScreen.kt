package com.myvpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.BuildConfig
import com.myvpn.app.data.AppSettings
import com.myvpn.app.data.TrafficStore
import com.myvpn.app.ui.components.AppListItem
import com.myvpn.app.ui.components.ListHeader
import com.myvpn.app.ui.components.SwitchListItem
import com.myvpn.app.ui.formatBytes
import com.myvpn.app.ui.theme.AppRadius
import com.myvpn.app.ui.theme.DYNAMIC_SEED
import com.myvpn.app.ui.theme.SeedOrder
import com.myvpn.app.ui.theme.SeedThemes
import com.myvpn.app.ui.theme.SuperEllipseShape
import io.nekohasekai.libbox.Libbox

/**
 * صفحه‌ی «ابزارها» — معادل Tools در FlClash: بخش‌های متوالی با ListHeader
 * و آیتم‌های جداشده با خط نازک، بدون ریل کنار.
 */
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    onOpenLogs: () -> Unit = {},
) {
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val seed by AppSettings.seed.collectAsStateWithLifecycle()
    val pureBlack by AppSettings.pureBlack.collectAsStateWithLifecycle()
    val autoConnect by AppSettings.autoConnect.collectAsStateWithLifecycle()
    val totalDown by TrafficStore.totalDown.collectAsStateWithLifecycle()
    val totalUp by TrafficStore.totalUp.collectAsStateWithLifecycle()

    var themeDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    val coreVersion = remember { runCatching { Libbox.version() }.getOrDefault("?") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        ListHeader("ظاهر")
        AppListItem(
            title = "حالت تم",
            subtitle = when (themeMode) {
                "light" -> "روشن"
                "dark" -> "تیره"
                else -> "همراه سیستم"
            },
            icon = Icons.Filled.Settings,
            onClick = { themeDialog = true },
            trailing = { Text("›", style = MaterialTheme.typography.titleMedium) },
        )
        AppListItem(
            title = "رنگ تم",
            subtitle = if (seed == DYNAMIC_SEED) "رنگوالپیپر دستگاه"
            else SeedThemes[seed]?.label ?: "پیش‌فرض",
            icon = Icons.Filled.List,
            onClick = { colorDialog = true },
            trailing = {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(seedToColor(seed), CircleShape),
                )
            },
        )
        SwitchListItem(
            title = "سیاه مطلق",
            subtitle = "در تم تیره، سطح پس‌زمینه کاملاً سیاه شود",
            icon = Icons.Filled.Info,
            checked = pureBlack,
            onCheckedChange = { AppSettings.setPureBlack(it) },
        )

        ListHeader("رفتار")
        SwitchListItem(
            title = "اتصال خودکار",
            subtitle = "پس از روشن شدن دستگاه به سرور انتخاب‌شده وصل شود",
            checked = autoConnect,
            onCheckedChange = { AppSettings.setAutoConnect(it) },
        )
        AppListItem(
            title = "گزارش‌های هسته",
            subtitle = "خطاها و رویدادهای sing-box",
            icon = Icons.Filled.List,
            onClick = onOpenLogs,
        )

        ListHeader("آمار")
        AppListItem(
            title = "مصرف تجمعی",
            subtitle = "↓${formatBytes(totalDown)}  ↑${formatBytes(totalUp)}",
            icon = Icons.Filled.Info,
            trailing = {
                TextButton(onClick = { TrafficStore.reset() }) { Text("ریست") }
            },
        )

        ListHeader("درباره")
        AppListItem(
            title = "MyVPN ${BuildConfig.VERSION_NAME}",
            subtitle = "هسته: sing-box $coreVersion",
            icon = Icons.Filled.Settings,
            withDivider = false,
        )
    }

    if (themeDialog) {
        ChoiceDialog(
            title = "حالت تم",
            options = listOf("system" to "همراه سیستم", "light" to "روشن", "dark" to "تیره"),
            selected = themeMode,
            onSelected = { AppSettings.setThemeMode(it) },
            onDismiss = { themeDialog = false },
        )
    }

    if (colorDialog) {
        ChoiceDialog(
            title = "رنگ تم",
            options = buildList {
                add(DYNAMIC_SEED to "رنگوالپیپر دستگاه")
                SeedOrder.forEach { key -> add(key to (SeedThemes[key]?.label ?: key)) }
            },
            selected = seed,
            onSelected = { AppSettings.setSeed(it) },
            onDismiss = { colorDialog = false },
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = SuperEllipseShape(AppRadius.xxl),
    )
}

/** رنگ seed برای نشان پیش‌نمایش (کلیدهای SeedThemes به شکل 0xFFRRGGBB هستند). */
private fun seedToColor(seed: String): Color =
    runCatching { Color(seed.removePrefix("0x").removePrefix("0X").toULong(16)) }.getOrDefault(Color.Gray)
