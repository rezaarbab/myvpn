package com.myvpn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.myvpn.app.data.AppSettings

const val DYNAMIC_SEED = "dynamic"

data class AppExtras(val success: Color, val warning: Color)

internal val LocalAppExtras: ProvidableCompositionLocal<AppExtras> = staticCompositionLocalOf {
    AppExtras(Color(0xFF4CAF50), Color(0xFFFF9800))
}

/**
 * توکن‌های مخصوص FlClash که در ColorScheme استاندارد متریال نیستند:
 * سبز/نارنجی هماهنگ‌شده با رنگ اصلی برای نمایش پینگ.
 */
object FlTheme {
    val extras: AppExtras
        @Composable @ReadOnlyComposable get() = LocalAppExtras.current

    /** رنگ پینگ سالم (<۶۰۰ms) */
    val success: Color
        @Composable @ReadOnlyComposable get() = LocalAppExtras.current.success

    /** رنگ پینگ ضعیف (>=۶۰۰ms) */
    val warning: Color
        @Composable @ReadOnlyComposable get() = LocalAppExtras.current.warning
}

private fun Color.darken(factor: Float) =
    Color(red * factor, green * factor, blue * factor, alpha)

/** حالت «مشخص مطلق» FlClash: سطح سیاه واقعی و کارت‌های تاریک‌تر. */
private fun ColorScheme.asPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLow.darken(0.55f),
    surfaceContainer = surfaceContainer.darken(0.55f),
    surfaceContainerHigh = surfaceContainerHigh.darken(0.55f),
    surfaceContainerHighest = surfaceContainerHighest.darken(0.55f),
)

@Composable
fun MyVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seed: String = AppSettings.seed.value,
    pureBlack: Boolean = AppSettings.pureBlack.value,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base: ColorScheme = if (seed == DYNAMIC_SEED && dynamicOk) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val entry = SeedThemes[seed] ?: SeedThemes.getValue(DefaultSeed)
        if (darkTheme) entry.dark else entry.light
    }
    val extras = SeedThemes[seed]?.let { e ->
        if (darkTheme) AppExtras(e.extras.successDark, e.extras.warningDark)
        else AppExtras(e.extras.successLight, e.extras.warningLight)
    } ?: AppExtras(Color(0xFF4CAF50), Color(0xFFFF9800))

    val scheme = if (darkTheme && pureBlack) base.asPureBlack() else base

    CompositionLocalProvider(LocalAppExtras provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}
