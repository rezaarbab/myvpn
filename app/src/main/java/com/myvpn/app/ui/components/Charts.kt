package com.myvpn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.myvpn.app.ui.formatBytes
import kotlin.math.abs

/**
 * نمودار خطی زنده — همان الگوریتم `line_chart.dart` در FlClash:
 * نرم‌کردن گاوسی (sigma ۱.۵)، اسپلاین Catmull-Rom، خط پایه روی ۷۰٪ ارتفاع و مقیاس حداقل ۸KiB.
 */
object ChartMetrics {
    const val CAPACITY = 60
    const val MIN_SCALE = 8.0 * 1024.0
    const val SIGMA = 1.5
}

private val BLUR_RADIUS = kotlin.math.ceil(ChartMetrics.SIGMA * 2.5).toInt()
private val BLUR_KERNEL = FloatArray(BLUR_RADIUS + 1) { d ->
    kotlin.math.exp(-d * d / (2.0 * ChartMetrics.SIGMA * ChartMetrics.SIGMA)).toFloat()
}

private fun gaussianBlur(values: List<Double>): List<Double> = List(values.size) { i ->
    var sum = 0.0
    var weight = 0.0
    for (j in maxOf(0, i - BLUR_RADIUS)..minOf(values.size - 1, i + BLUR_RADIUS)) {
        val k = BLUR_KERNEL[abs(j - i)]
        sum += k * values[j]
        weight += k
    }
    sum / weight
}

@Composable
fun LineChart(
    history: List<Pair<Long, Long>>,
    modifier: Modifier = Modifier,
    valueSelector: (Pair<Long, Long>) -> Long = { it.first },
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier) {
        val length = ChartMetrics.CAPACITY + BLUR_RADIUS + 1
        val values = history.map { valueSelector(it).toDouble() }
        val window = DoubleArray(length)
        val count = minOf(values.size, length)
        for (i in 0 until count) window[length - count + i] = values[values.size - count + i]
        val heights = gaussianBlur(window.toList())

        val strokeWidthPx = 2.dp.toPx()
        val baseline = size.height * 0.7f
        val step = size.width / (ChartMetrics.CAPACITY - 1)
        val firstX = size.width - (length - 1) * step

        fun xOf(i: Int) = firstX + i * step
        fun yf(i: Int) = -heights[i]

        val slopes = DoubleArray(length) { i ->
            if (i == 0 || i == length - 1) 0.0
            else ((heights[i + 1] - heights[i - 1]) / 2.0).coerceIn(-3.0 * heights[i], 3.0 * heights[i])
        }

        val top = maxOf(ChartMetrics.MIN_SCALE, heights.max())
        val yScale = ((baseline - strokeWidthPx) / top).toFloat()
        fun mapY(v: Double) = (baseline + v * yScale).toFloat()

        val line = Path()
        line.moveTo(xOf(0), mapY(yf(0)))
        for (i in 1 until length) {
            line.cubicTo(
                xOf(i - 1) + step / 3f, mapY(yf(i - 1) - slopes[i - 1] / 3f),
                xOf(i) - step / 3f, mapY(yf(i) + slopes[i] / 3f),
                xOf(i), mapY(yf(i)),
            )
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(xOf(0), size.height)
            close()
        }
        clipRect(0f, 0f, size.width, size.height) {
            drawPath(
                area,
                Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0.10f))),
            )
            drawPath(
                line,
                color = lineColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * دونات مصرف — پورت `donut_chart.dart`: ضخامت ۱۲٪ قطر (کرانده به ۱۰..۱۶dp)،
 * فاصله‌ی گپ ۰.۳ و سرِ گرد؛ قطعه‌ی خیلی کوچک به‌صورت نقطه رسم می‌شود.
 */
@Composable
fun DonutChart(
    down: Long,
    up: Long,
    modifier: Modifier = Modifier,
    upColor: Color = MaterialTheme.colorScheme.tertiary,
    downColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Canvas(modifier) {
        val diameter = minOf(size.width, size.height)
        val strokeWidthPx = (diameter * 0.12f).coerceIn(10.dp.toPx(), 16.dp.toPx())
        val radius = (diameter - strokeWidthPx) / 2f
        if (radius <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val rect = Rect(center - Offset(radius, radius), GeoSize(radius * 2f, radius * 2f))

        val total = (down + up).coerceAtLeast(1L)
        val fractions = listOf(up.toFloat() / total, down.toFloat() / total)
        val trackAlpha = 1f - fractions.fold(0f) { s, f -> s + f }
        if (trackAlpha * 255f >= 1f) {
            drawCircle(
                trackColor.copy(alpha = trackAlpha),
                radius = radius,
                center = center,
                style = Stroke(strokeWidthPx),
            )
        }

        val gapRatio = 0.3f
        val fullDotFraction = 0.01f
        val minSweep = 1e-3f
        val colors = listOf(upColor, downColor)
        val weights = fractions.map { minOf(1f, it / fullDotFraction) }
        val slot = strokeWidthPx * (1f + gapRatio) / radius
        val reserved = weights.fold(0f) { s, w -> s + w * slot }
        val available = (2.0 * Math.PI - reserved).toFloat().coerceAtLeast(0f)

        var start = (-Math.PI / 2).toFloat()
        for (i in fractions.indices) {
            val weight = weights[i]
            val sweep = available * fractions[i]
            start += weight * slot / 2f
            if (weight > 0f) {
                if (sweep > minSweep) {
                    drawArc(
                        colors[i],
                        start,
                        sweep,
                        false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(strokeWidthPx * weight, cap = StrokeCap.Round),
                    )
                } else {
                    drawCircle(
                        colors[i],
                        radius = strokeWidthPx * weight / 2f,
                        center = center + Offset(
                            kotlin.math.cos(start) * radius,
                            kotlin.math.sin(start) * radius,
                        ),
                    )
                }
            }
            start += sweep + weight * slot / 2f
        }
    }
}

/** لجند دونات: pill رنگی ۲۰×۸dp + برچسب + مقدار */
@Composable
fun UsageLegendRow(label: String, dotColor: Color, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 20.dp, height = 8.dp)
                .background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/** کارت سرعت شبکه: دو نمودار روی هم (دانلود بزرگ‌تر، آپلود کوچک‌تر) + سرِ عددی. */
@Composable
fun NetworkSpeedCard(
    history: List<Pair<Long, Long>>,
    downlink: Long,
    uplink: Long,
    live: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "↑ ${formatBytes(uplink)}/s",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                "↓ ${formatBytes(downlink)}/s",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                LineChart(
                    history,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    valueSelector = { it.first },
                )
                Spacer(Modifier.height(2.dp))
                LineChart(
                    history,
                    Modifier
                        .fillMaxWidth()
                        .weight(0.5f),
                    valueSelector = { it.second },
                    lineColor = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (!live && history.isEmpty()) {
                Text(
                    "برای دیدن نمودار وصل شوید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
