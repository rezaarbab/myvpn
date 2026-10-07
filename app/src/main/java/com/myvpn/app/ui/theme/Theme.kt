package com.myvpn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- پالت برند: seed رسمی FlClash (defaultPrimaryColor = 0xFFD8C0C3) ----
private val Indigo = Color(0xFF8E4956)
private val Cyan = Color(0xFFE3BDC1)

/** گرادیان اصلی برند */
val BrandGradient = Brush.linearGradient(listOf(Color(0xFF8E4956), Color(0xFFD8A9B0)))

/** گرادیان دکمه‌ی اتصال */
val ConnectGradient = Brush.linearGradient(listOf(Color(0xFFB25F6D), Color(0xFFD8C0C3)))

/** گرادیان دکمه‌ی قطع */
val DisconnectGradient = Brush.linearGradient(listOf(Color(0xFFB25F6D), Color(0xFFE0B7A6)))

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1BC),
    onPrimary = Color(0xFF541321),
    primaryContainer = Color(0xFF6B3340),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFE3BDC1),
    onSecondary = Color(0xFF422A2E),
    secondaryContainer = Color(0xFF5A3F44),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFE3C26F),
    onTertiary = Color(0xFF3F2E00),
    background = Color(0xFF171215),
    onBackground = Color(0xFFEBDFE0),
    surface = Color(0xFF1B1416),
    onSurface = Color(0xFFEBDFE0),
    surfaceVariant = Color(0xFF3B2F31),
    onSurfaceVariant = Color(0xFFD3C2C4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF574144),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8E4956),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3A0C15),
    secondary = Color(0xFF75565C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0DCDF),
    onSecondaryContainer = Color(0xFF2B1519),
    tertiary = Color(0xFF7A5761),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFDF8F8),
    onBackground = Color(0xFF1F1B1C),
    surface = Color(0xFFFDF8F8),
    onSurface = Color(0xFF1F1B1C),
    surfaceVariant = Color(0xFFF2DDDF),
    onSurfaceVariant = Color(0xFF534347),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF857376),
)

@Composable
fun MyVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // اسکیم متریال ۳ از seed رسمی FlClash — رنگ داینامیک دستگاه حذف شده
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
