package com.nexus.ssh.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.nexus.ssh.R
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.glass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val DialogShape = RoundedCornerShape(26.dp)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var m = modifier.glass(shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = Nx.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

@Composable
fun NexusLogo(modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(if (com.nexus.ssh.ui.Nx.Dark) R.drawable.nexus_mark_white else R.drawable.nexus_mark_green),
            contentDescription = "Nexus SSH",
            modifier = Modifier.size(size * 0.78f)
        )
    }
}

/** Light rounded app tile with a label underneath (home quick-access grid). */
@Composable
fun IconTile(icon: ImageVector, tint: Color, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(66.dp)
                .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x221C6B3B), spotColor = Color(0x221C6B3B))
                .glass(RoundedCornerShape(20.dp), fill = 0.92f),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Nx.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun ScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Nx.Text)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Nx.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = Nx.Muted, fontSize = 13.sp, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).glass(RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Nx.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Nx.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val accent = LocalAccent.current
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(18.dp))
            .background(Nx.Red.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Nx.Red)
        Spacer(Modifier.width(10.dp))
        Text(message, color = Color(0xFFFFB4C0), fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry", color = Nx.Red) }
    }
}

@Composable
fun LoadingBar(visible: Boolean, modifier: Modifier = Modifier) {
    if (visible) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth().height(3.dp),
            color = LocalAccent.current,
            trackColor = Color.Transparent
        )
    } else {
        Spacer(modifier.height(3.dp))
    }
}

// ----------------------------------------------------------------------------- navigation

class NavItemSpec(val route: String, val icon: ImageVector, val label: String)

@Composable
private fun NavButton(item: NavItemSpec, selected: Boolean, onSelect: (NavItemSpec) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(if (selected) LocalAccent.current else Color.Transparent, label = "navBg")
    val fg by animateColorAsState(if (selected) Color.White else Nx.Muted, label = "navFg")
    val iconScale by animateFloatAsState(if (selected) 1.10f else 0.94f, label = "navIconScale")
    Box(
        Modifier
            .size(width = 58.dp, height = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClickLabel = item.label) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(item)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(25.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
    }
}

/** Rounded floating bottom navigation; the selected item is a white rounded square. */
@Composable
fun FloatingNavBar(
    items: List<NavItemSpec>,
    selectedRoute: String?,
    onSelect: (NavItemSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x221C6B3B), spotColor = Color(0x221C6B3B))
                .glass(RoundedCornerShape(32.dp), fill = 0.9f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item -> NavButton(item, item.route == selectedRoute, onSelect) }
        }
    }
}

/** Vertical variant used on tablets and in landscape. */
@Composable
fun FloatingNavRail(
    items: List<NavItemSpec>,
    selectedRoute: String?,
    onSelect: (NavItemSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            Modifier
                .shadow(10.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x221C6B3B), spotColor = Color(0x221C6B3B))
                .glass(RoundedCornerShape(30.dp), fill = 0.9f)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item -> NavButton(item, item.route == selectedRoute, onSelect) }
        }
    }
}

// ----------------------------------------------------------------------------- illustrations

/** Premium isometric server-stack illustration drawn with vectors (no bitmap). */
@Composable
fun ServerStackIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val fw = w * 0.60f
        val dx = w * 0.20f
        val dy = h * 0.12f
        val sh = h * 0.27f
        val x0 = w * 0.04f
        val gap = h * 0.03f
        // soft ground shadow
        drawOval(
            Brush.radialGradient(listOf(Color(0x66000000), Color.Transparent)),
            topLeft = Offset(x0, h * 0.86f),
            size = Size(fw + dx, h * 0.12f)
        )
        for (i in 0 until 2) {
            val y = h * 0.84f - (i + 1) * (sh + gap) + dy * 0.0f
            // top face
            val top = Path().apply {
                moveTo(x0, y)
                lineTo(x0 + dx, y - dy)
                lineTo(x0 + dx + fw, y - dy)
                lineTo(x0 + fw, y)
                close()
            }
            drawPath(top, Brush.linearGradient(listOf(Color(0xFFB5DCC0), Color(0xFF75B98A))))
            // side face
            val side = Path().apply {
                moveTo(x0 + fw, y)
                lineTo(x0 + fw + dx, y - dy)
                lineTo(x0 + fw + dx, y - dy + sh)
                lineTo(x0 + fw, y + sh)
                close()
            }
            drawPath(side, Brush.verticalGradient(listOf(Color(0xFF38895A), Color(0xFF155B39))))
            // front face
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF2F9A5C), Color(0xFF17643D))),
                topLeft = Offset(x0, y),
                size = Size(fw, sh),
                cornerRadius = CornerRadius(w * 0.02f)
            )
            drawRoundRect(
                Nx.Border.copy(alpha = 0.8f),
                topLeft = Offset(x0, y),
                size = Size(fw, sh),
                cornerRadius = CornerRadius(w * 0.02f),
                style = Stroke(1.dp.toPx())
            )
            // status LEDs
            val ledY = y + sh / 2f
            val ledColors = listOf(Color(0xFFB7F3C7), Color(0xFF4BA6E8), Color(0xFFF2C45B))
            for (k in 0 until 3) {
                drawCircle(ledColors[k], radius = w * 0.016f, center = Offset(x0 + fw * (0.10f + 0.07f * k), ledY))
            }
            // vent slots
            for (k in 0 until 4) {
                drawLine(
                    Nx.Border,
                    Offset(x0 + fw * (0.50f + 0.10f * k), y + sh * 0.28f),
                    Offset(x0 + fw * (0.50f + 0.10f * k), y + sh * 0.72f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun BigFolderIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF2F86E8), Offset(w * 0.06f, h * 0.14f), Size(w * 0.42f, h * 0.30f), CornerRadius(w * 0.08f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF4DA3FA), Color(0xFF2A74D9))),
            Offset(w * 0.06f, h * 0.24f), Size(w * 0.88f, h * 0.62f), CornerRadius(w * 0.10f)
        )
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF7CC4FF), Color(0xFF3B91F0))),
            Offset(w * 0.06f, h * 0.36f), Size(w * 0.88f, h * 0.50f), CornerRadius(w * 0.10f)
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.22f),
            Offset(w * 0.06f, h * 0.36f), Size(w * 0.88f, h * 0.50f), CornerRadius(w * 0.10f),
            style = Stroke(1.dp.toPx())
        )
    }
}

@Composable
fun BigFileIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val left = w * 0.22f
        val right = w * 0.78f
        val top = h * 0.10f
        val bottom = h * 0.90f
        val fold = w * 0.18f
        val page = Path().apply {
            moveTo(left, top)
            lineTo(right - fold, top)
            lineTo(right, top + fold)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(page, Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFCBD2DC))))
        val corner = Path().apply {
            moveTo(right - fold, top)
            lineTo(right - fold, top + fold)
            lineTo(right, top + fold)
            close()
        }
        drawPath(corner, Color(0xFFAEB6C4))
        for (i in 0 until 3) {
            val y = h * (0.46f + 0.14f * i)
            drawLine(
                Color(0xFF8A93A3),
                Offset(left + w * 0.08f, y),
                Offset(right - w * 0.08f - (if (i == 2) w * 0.12f else 0f), y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// ----------------------------------------------------------------------------- charts

@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier, maxValue: Float? = null) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (values.size < 2) {
            drawLine(color.copy(alpha = 0.35f), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 2.dp.toPx(), StrokeCap.Round)
            return@Canvas
        }
        val top = (maxValue ?: (values.maxOrNull() ?: 1f)).coerceAtLeast(1f)
        val n = values.size
        fun px(i: Int) = w * i / (n - 1)
        fun py(v: Float) = h - (v / top).coerceIn(0f, 1f) * h * 0.82f - h * 0.08f
        val line = Path().apply {
            moveTo(px(0), py(values[0]))
            for (i in 1 until n) lineTo(px(i), py(values[i]))
        }
        // Flat, crisp telemetry trace: no bloom/glow fill, keeping the monitor restrained
        // and reducing overdraw on lower-end devices.
        drawPath(line, color.copy(alpha = 0.92f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

class ChartSeries(val name: String, val color: Color, val values: List<Float>)

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

/** Interactive multi-series line chart: tap or drag to inspect a real sample. */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    times: List<Long>,
    yMax: Float,
    yFormat: (Float) -> String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 170.dp
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val n = times.size
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier.height(chartHeight).padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (i in 4 downTo 0) {
                    Text(yFormat(yMax * i / 4f), color = Nx.Muted, fontSize = 11.sp)
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .pointerInput(n) {
                        detectTapGestures { off ->
                            if (n > 0) selected = ((off.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                        }
                    }
                    .pointerInput(n) {
                        detectDragGestures(
                            onDragStart = { off ->
                                if (n > 0) selected = ((off.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            },
                            onDrag = { change, _ ->
                                if (n > 0) selected = ((change.position.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            },
                            onDragEnd = {},
                            onDragCancel = {}
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    for (i in 0..4) {
                        val y = h * i / 4f
                        drawLine(Nx.Border.copy(alpha = 0.7f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                    }
                    if (n >= 2) {
                        series.forEachIndexed { si, s ->
                            val line = Path()
                            for (i in 0 until minOf(n, s.values.size)) {
                                val x = w * i / (n - 1)
                                val y = h - (s.values[i] / yMax).coerceIn(0f, 1f) * h
                                if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                            }
                            if (si == 0) {
                                val fill = Path().apply {
                                    addPath(line)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(fill, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.22f), Color.Transparent)))
                            }
                            drawPath(line, s.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        val sel = selected
                        if (sel != null && sel < n) {
                            val x = w * sel / (n - 1)
                            drawLine(Color.White.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, h), 1.dp.toPx())
                            series.forEach { s ->
                                if (sel < s.values.size) {
                                    val y = h - (s.values[sel] / yMax).coerceIn(0f, 1f) * h
                                    drawCircle(Color.White, 5.dp.toPx(), Offset(x, y))
                                    drawCircle(s.color, 3.5.dp.toPx(), Offset(x, y))
                                }
                            }
                        }
                    }
                }
                if (n < 2) {
                    Text(
                        "Collecting samples…",
                        color = Nx.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                val sel = selected
                if (sel != null && sel < n) {
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .glass(RoundedCornerShape(12.dp), fill = 0.92f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(timeFormat.format(Date(times[sel])), color = Nx.Muted, fontSize = 11.sp)
                        series.forEach { s ->
                            if (sel < s.values.size) {
                                Text(
                                    "${s.name}  ${yFormat(s.values[sel])}",
                                    color = s.color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 40.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = if (n >= 2) listOf(times[0], times[n / 3], times[(2 * n) / 3], times[n - 1]) else emptyList()
            labels.forEach { Text(timeFormat.format(Date(it)).substring(0, 8), color = Nx.Muted, fontSize = 10.sp) }
        }
    }
}
