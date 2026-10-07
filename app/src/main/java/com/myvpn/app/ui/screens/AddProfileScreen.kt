package com.myvpn.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvpn.app.data.LinkParser
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.ui.components.AppCard
import com.myvpn.app.ui.components.InfoHeader
import com.myvpn.app.ui.theme.AppRadius
import com.myvpn.app.ui.theme.SuperEllipseShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info

/**
 * افزودن/ویرایش پروفایل: لینک اشتراکی یا کانفیگ JSON خام — هر خط یک پروفایل.
 * در حالت ویرایش، محتوای خام پروفایل پیش‌پر می‌شود.
 */
@Composable
fun AddProfileScreen(
    profileId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profiles by ProfileStore.profiles.collectAsStateWithLifecycle()
    val existing = remember(profileId, profiles) { profiles.firstOrNull { it.id == profileId } }

    var text by rememberSaveable(profileId) { mutableStateOf(existing?.rawConfig.orEmpty()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val editing = existing != null

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            InfoHeader(if (editing) "ویرایش کانفیگ" else "لینک یا کانفیگ", Icons.Filled.Add)
            Spacer(Modifier.height(8.dp))
            Text(
                "لینک‌های vless / vmess / trojan / shadowsocks / hysteria2 / tuic، " +
                    "یا کانفیگ JSON خام. هر خط یک پروفایل است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = SuperEllipseShape(AppRadius.md),
                minLines = 5,
                label = { Text("محتوا") },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) text = if (text.isBlank()) clip else "$text\n$clip"
                }) { Text("از کلیپ‌بورد") }
                TextButton(onClick = { text = "" }) { Text("پاک کردن") }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (existing != null) {
                        ProfileStore.save(existing.copy(rawConfig = text))
                        message = "پروفایل به‌روزرسانی شد"
                    } else {
                        try {
                            val parsed = LinkParser.parse(text)
                            parsed.forEach { ProfileStore.save(it) }
                            message = "${parsed.size} پروفایل ذخیره شد"
                            text = ""
                        } catch (e: Exception) {
                            message = e.message ?: "خطای ناشناخته"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = SuperEllipseShape(AppRadius.md),
            ) { Text(if (editing) "ذخیره" else "افزودن") }
            Spacer(Modifier.height(6.dp))
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = SuperEllipseShape(AppRadius.md),
            ) { Text("بازگشت") }
        }

        message?.let {
            AppCard(modifier = Modifier.fillMaxWidth(), filled = true) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
