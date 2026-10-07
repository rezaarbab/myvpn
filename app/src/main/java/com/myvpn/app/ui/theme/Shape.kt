package com.myvpn.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * گوشه‌ی ابربیضی (superellipse) — چیزی که Flutter با `RoundedSuperellipseBorder` می‌کشد و
 * امضای بصری FlClash است: پیش از رسیدن به گوشه، لبه کمی صاف می‌شود (برخلاف قوس دایره‌ای Material).
 *
 * توان معادله‌ی |x/a|^n + |y/b|^n = ۱ از نسبت شعاع به کوتاه‌ضلع می‌آید:
 * t = ۱ ← n = ۲ (بیضی/پیل) و t → ۰ ← n = ۲۰ (نزدیک مستطیل)؛ همان بازه‌ی Material 3 Expressive.
 */
@Immutable
class SuperEllipseShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.None
        val radius = with(density) { cornerRadius.toPx() }
        if (radius <= 0.5f) {
            return Outline.Generic(Path().apply { addRect(0f, 0f, w, h) })
        }
        return Outline.Generic(superEllipsePath(w, h, radius))
    }
}

private const val SAMPLES = 14
private const val PI = Math.PI.toFloat()
private const val HALF_PI = PI / 2f

fun superEllipsePath(width: Float, height: Float, requestedRadius: Float): Path {
    val r = min(requestedRadius, min(width, height) / 2f)
    val roundness = (2f * r / min(width, height)).coerceIn(0f, 1f)
    val exponent = 2.0 + 18.0 * (1.0 - roundness)
    val path = Path().apply { moveTo(r, 0f) }
    addCorner(path, width - r, r, -HALF_PI, 0f, r, exponent)
    addCorner(path, width - r, height - r, 0f, HALF_PI, r, exponent)
    addCorner(path, r, height - r, HALF_PI, PI, r, exponent)
    addCorner(path, r, r, PI, PI + HALF_PI, r, exponent)
    path.close()
    return path
}

private fun addCorner(path: Path, cx: Float, cy: Float, from: Float, to: Float, r: Float, exponent: Double) {
    val e = 2.0 / exponent
    for (i in 0..SAMPLES) {
        val a = from + (to - from) * i / SAMPLES
        val c = cos(a.toDouble())
        val s = sin(a.toDouble())
        path.lineTo(
            cx + r * abs(c).pow(e).toFloat() * c.sign,
            cy + r * abs(s).pow(e).toFloat() * s.sign,
        )
    }
}

private val Double.sign: Float get() = if (this < 0) -1f else if (this > 0) 1f else 0f

/** توکن‌های شعاع — همان مقیاس AppCorner در FlClash. */
object AppRadius {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 20.dp      // کارت‌ها
    val xl = 24.dp      // کارت پروفایل / دیالوگ کوچک
    val xxl = 28.dp     // دیالوگ و شیت
    val full = 512.dp   // پیل (دکمه‌ی شناور، نوار ناوبری)
}

/** فاصله‌های پیش‌فرض صفحه — spacing 12، padding 16 (FlClash). */
object AppSpacing {
    val grid = 12.dp
    val page = 16.dp
    val cardInnerH = 16.dp
    val cardInnerV = 14.dp
    val cardUnit = 80.dp      // ارتفاع یک خط در گرید ویجت
    val cardGap = 12.dp
}

fun appShapes() = Shapes(
    extraSmall = SuperEllipseShape(AppRadius.xs),
    small = SuperEllipseShape(AppRadius.sm),
    medium = SuperEllipseShape(AppRadius.md),
    large = SuperEllipseShape(AppRadius.lg),
    extraLarge = SuperEllipseShape(AppRadius.xl),
)
