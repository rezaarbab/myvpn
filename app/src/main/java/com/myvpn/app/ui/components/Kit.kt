package com.myvpn.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myvpn.app.ui.theme.AppRadius
import com.myvpn.app.ui.theme.AppSpacing
import com.myvpn.app.ui.theme.SuperEllipseShape

/**
 * کارت مشترک به سبک FlClash (`CommonCard`):
 * خط‌کش ۱ پیکسل به‌جای سایه، و سه حالت — معمولی (surfaceContainerLow)، پرشده (surfaceContainerHigh)
 * و انتخاب‌شده (secondaryContainer با حاشیه‌ی primary).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    filled: Boolean = false,
    shape: Shape = SuperEllipseShape(AppRadius.lg),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = AppSpacing.cardInnerH,
        vertical = AppSpacing.cardInnerV,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when {
        selected -> scheme.secondaryContainer
        filled -> scheme.surfaceContainerHigh
        else -> scheme.surfaceContainerLow
    }
    val onContainer = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    val border = BorderStroke(
        1.dp,
        if (selected) scheme.primary else scheme.surfaceContainerHighest,
    )
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = container,
        contentColor = onContainer,
        border = border,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** سرِ کارت: یک گلیف ۲۴dp + عنوان `titleSmall` با رنگ دوم — همان `InfoHeader`. */
@Composable
fun InfoHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/**
 * گرید ویجت داشبورد: مثل `SuperGrid` فلت روی موبایل ۸ ستون، فاصله ۱۲dp.
 * هر ویجت (ستون، خط) اعلام می‌کند و ردیف‌ها تا پرشدن ۸ ستون چیده می‌شوند؛
 * ارتفاع هر ردیف = بلندترین ویجت آن ردیف، با واحد پایه‌ی ۸۰dp.
 */
data class WidgetSpec(val id: String, val columns: Int, val lines: Int)

@Composable
fun WidgetGrid(
    specs: List<WidgetSpec>,
    modifier: Modifier = Modifier,
    totalColumns: Int = 8,
    content: @Composable RowScope.(WidgetSpec) -> Unit,
) {
    val rows = rememberRows(specs, totalColumns)
    val unit = AppSpacing.cardUnit
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.grid),
    ) {
        rows.forEach { row ->
            val lines = row.maxOf { it.lines }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(unit * lines + AppSpacing.cardGap * (lines - 1)),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.grid),
            ) {
                val scope = this
                row.forEach { spec ->
                    Box(modifier = Modifier.weight(spec.columns.toFloat())) {
                        content(scope, spec)
                    }
                }
            }
        }
    }
}

private fun rememberRows(specs: List<WidgetSpec>, totalColumns: Int): List<List<WidgetSpec>> {
    val rows = mutableListOf<MutableList<WidgetSpec>>()
    var used = 0
    var current = mutableListOf<WidgetSpec>()
    specs.forEach { spec ->
        val span = spec.columns.coerceAtMost(totalColumns)
        if (current.isNotEmpty() && used + span > totalColumns) {
            rows.add(current)
            current = mutableListOf()
            used = 0
        }
        current.add(spec)
        used += span
    }
    if (current.isNotEmpty()) rows.add(current)
    return rows
}

/** سرِ بخش در صفحه‌ی تنظیمات (`ListHeader`): padding ۱۶/۲۴/۸ و وزن ۶۰۰. */
@Composable
fun ListHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    )
}

/** آیتم لیست به سبک `ListItem` فلاتر: ارتفاع آسان‌پذیر، گلیف ۲۴dp، جداسور نازک. */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    withDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val background = if (selected) scheme.secondaryContainer else Color.Transparent
    val contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    Column(
        modifier
            .fillMaxWidth()
            .background(background)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) trailing()
        }
        if (withDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 52.dp else 16.dp),
                thickness = 1.dp,
                color = scheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
fun SwitchListItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    AppListItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
    )
}

/** نشان کوچک وضعیت (مثلاً نوع پروتکل یا حالت گروه). */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(SuperEllipseShape(AppRadius.sm))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

