package com.myvpn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Seed = Color(0xFF0B57D0)

private val LightColors = lightColorScheme(
    primary = Seed,
    secondary = Color(0xFF00639B),
    tertiary = Color(0xFF00696E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    secondary = Color(0xFF97CBFF),
    tertiary = Color(0xFF4CDADF),
)

@Composable
fun MyVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
