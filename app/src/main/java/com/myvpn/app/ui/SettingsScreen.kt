package com.myvpn.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.BuildConfig
import com.myvpn.app.data.AppSettings
import io.nekohasekai.libbox.Libbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val autoConnect by AppSettings.autoConnect.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("تنظیمات") }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text("عمومی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ظاهر", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "system" to "سیستم",
                            "light" to "روشن",
                            "dark" to "تیره",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = themeMode == value,
                                onClick = { AppSettings.setThemeMode(value) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text("رفتار", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("اتصال خودکار پس از روشن شدن", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "پس از ری‌استارت دستگاه، به سرور انتخاب‌شده وصل شو",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoConnect,
                        onCheckedChange = { AppSettings.setAutoConnect(it) },
                    )
                }
            }

            Column(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text("درباره", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "MyVPN ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "هسته: sing-box ${runCatching { Libbox.version() }.getOrDefault("?")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
