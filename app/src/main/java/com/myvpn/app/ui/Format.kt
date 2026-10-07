package com.myvpn.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.myvpn.app.ui.theme.FlTheme
import kotlin.math.abs

/** قالب‌بایت حجم/سرعت با واحدهای باینری — معادل `formatSize` در FlClash. */
fun formatBytes(value: Long): String {
    if (value < 0) return "0 B"
    if (value < 1024) return "$value B"
    var size = value / 1024.0
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var unit = 0
    while (size >= 1024 && unit < units.size - 1) {
        size /= 1024.0
        unit++
    }
    return String.format(java.util.Locale.US, "%.1f %s", size, units[unit])
}

/** مدت‌زمان اتصال به شکل hh:mm:ss */
fun formatDuration(seconds: Long): String {
    val s = abs(seconds)
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}

/**
 * رنگ تأخیر با آستانه‌های FlClash: منفی → قرمز (خطا)، زیر ۶۰۰ms → سبز هماهنگ‌شده،
 * ۶۰۰ms و بیشتر → نارنجی هماهنگ‌شده.
 */
@Composable
fun delayColor(delayMs: Long?): Color = when {
    delayMs == null -> Color.Unspecified
    delayMs < 0 -> MaterialTheme.colorScheme.error
    delayMs < 600 -> FlTheme.success
    else -> FlTheme.warning
}
