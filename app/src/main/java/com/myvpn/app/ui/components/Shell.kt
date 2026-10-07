package com.myvpn.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvpn.app.ui.theme.AppRadius
import com.myvpn.app.ui.theme.SuperEllipseShape

/** مسیرهای نوار ناوبری پایین (معادل `NavigationItem` در FlClash). */
data class Destination(val route: String, val label: String, val icon: ImageVector)

enum class RunState { Stopped, Starting, Started, Stopping }

val AppPageToolbarHeight = 64.dp
private val DockBarHeight = 62.dp
private val DockEdgeMargin = 21.dp
private val DockItemExtent = 72.dp

/**
 * پوسته‌ی مشترک صفحات: هدر شناور روی محتوا (scroll-behind)، داک ناوبری شناور
 * و دکمه‌ی اتصال داخل شیار انتهایی همان داک — دقیقاً چیدمان FlClash در موبایل.
 */
@Composable
fun AppShell(
    destinations: List<Destination>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    runState: RunState,
    onStart: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    showDock: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val toolbarPx = with(density) { AppPageToolbarHeight.toPx() }
    val headerProgress = remember { mutableFloatStateOf(0f) }
    val scrollConnection = remember(toolbarPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    headerProgress.floatValue =
                        (headerProgress.floatValue - available.y / toolbarPx).coerceIn(0f, 1f)
                }
                return Offset.Zero
            }
        }
    }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockReserve = if (showDock) DockBarHeight + DockEdgeMargin * 2 else 12.dp

    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection),
    ) {
        content(
            Modifier
                .fillMaxSize()
                .padding(top = AppPageToolbarHeight + 12.dp)
                .padding(bottom = dockReserve + bottomInset),
        )

        // هدر: شفاف تا وقتی صفحه اسکرول نشده، بعد از آن رنگ surface
        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(scheme.surface.copy(alpha = headerProgress.floatValue))
                .statusBarsPadding()
                .height(AppPageToolbarHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            actions()
        }

        if (showDock) {
            NavigationDock(
                destinations = destinations,
                currentRoute = currentRoute,
                onSelect = onSelect,
                runState = runState,
                onStart = onStart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = DockEdgeMargin)
                    .padding(bottom = DockEdgeMargin),
            )
        }
    }
}


/** داک شناور: پیل با ارتفاع ۶۲، padding داخلی ۴، آیتم‌ها حداکثر ۷۲dp و FAB در شیار انتها. */
@Composable
fun NavigationDock(
    destinations: List<Destination>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    runState: RunState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SuperEllipseShape(AppRadius.full),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .height(DockBarHeight)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            destinations.forEach { destination ->
                DockItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onSelect(destination.route) },
                )
            }
            Spacer(Modifier.width(4.dp))
            StartButton(runState = runState, onClick = onStart)
        }
    }
}

@Composable
private fun DockItem(destination: Destination, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(max = DockItemExtent)
            .height(DockBarHeight - 8.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .background(
                    if (selected) scheme.secondaryContainer else Color.Transparent,
                    SuperEllipseShape(AppRadius.full),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            destination.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }
}

/**
 * دکمه‌ی اتصال: دایره‌ی هم‌اندازه‌ی ارتفاع داک با آیکن ۲۸dp و
 * «نفس» (BreathingFill) هنگام اتصال — پالس رنگ onPrimaryContainer تا آلفای ۰.۱۴.
 */
@Composable
fun StartButton(
    runState: RunState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val busy = runState == RunState.Starting || runState == RunState.Stopping
    val running = runState == RunState.Started
    val transition = rememberInfiniteTransition(label = "breathe")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val size = DockBarHeight - 8.dp
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (running && !busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        scheme.onPrimaryContainer.copy(alpha = 0.14f * breath),
                        SuperEllipseShape(AppRadius.full),
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (running) scheme.errorContainer else scheme.primaryContainer,
                    SuperEllipseShape(AppRadius.full),
                )
                .clickable(enabled = !busy, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                busy -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = if (running) scheme.onErrorContainer else scheme.onPrimaryContainer,
                    strokeWidth = 2.5.dp,
                )
                else -> Icon(
                    if (running) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (running) "قطع اتصال" else "اتصال",
                    tint = if (running) scheme.onErrorContainer else scheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
