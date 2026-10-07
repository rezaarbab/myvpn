package com.myvpn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- پالت برند ----
private val Indigo = Color(0xFF5B7CFA)
private val Cyan = Color(0xFF22D3EE)

/** گرادیان اصلی برند (ایندیگو → فیروزه‌ای) */
val BrandGradient = Brush.linearGradient(listOf(Indigo, Cyan))

/** گرادیان دکمه‌ی اتصال */
val ConnectGradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF22D3EE)))

/** گرادیان دکمه‌ی قطع */
val DisconnectGradient = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB923C)))

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF0A1230),
    primaryContainer = Color(0xFF1B2A55),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Cyan,
    onSecondary = Color(0xFF04222B),
    tertiary = Color(0xFFA78BFA),
    background = Color(0xFF070B12),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF0D121C),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF151C28),
    onSurfaceVariant = Color(0xFF9AA5B8),
    error = Color(0xFFFF5C5C),
    errorContainer = Color(0xFF3A1420),
    onErrorContainer = Color(0xFFFFD9DC),
    outline = Color(0xFF2A3342),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4256C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE5FF),
    onPrimaryContainer = Color(0xFF0A1230),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF10141C),
    surface = Color.White,
    onSurface = Color(0xFF10141C),
    surfaceVariant = Color(0xFFE7EBF5),
    onSurfaceVariant = Color(0xFF4A5468),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFC4CDDC),
)

@Composable
fun MyVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // رنگ ثابت برند؛ رنگ داینامیک دستگاه عمداً حذف شده تا هویت بصری همیشه یکسان بماند
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
