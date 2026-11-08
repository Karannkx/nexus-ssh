package com.nexus.ssh.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import kotlin.math.roundToInt

/** Refined light launcher tile with a soft surface and restrained border. */
@Composable
fun AppTile(label: String, dragging: Boolean, modifier: Modifier = Modifier, glyph: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .shadow(if (dragging) 14.dp else 5.dp, shape, ambientColor = Color(0x221C6B3B), spotColor = Color(0x221C6B3B))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F4ED))), shape)
                .border(1.dp, Color(0xFFE1E9DF), shape),
            contentAlignment = Alignment.Center
        ) {
            glyph()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Nx.Text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Android-launcher style grid: long-press an icon, drag it over another slot and drop it.
 * The new order is reported through [onReorder].
 */
@Composable
fun ReorderableGrid(
    keys: List<String>,
    columns: Int,
    cellHeight: Dp,
    onReorder: (List<String>) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (key: String, dragging: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var order by remember { mutableStateOf(keys) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(keys) { if (draggingKey == null) order = keys }

    val rows = ((order.size + columns - 1) / columns).coerceAtLeast(1)
    BoxWithConstraints(modifier.fillMaxWidth().height(cellHeight * rows)) {
        val cellW = with(density) { maxWidth.toPx() } / columns
        val cellH = with(density) { cellHeight.toPx() }
        val cellWDp = with(density) { cellW.toDp() }
        val currentOrder by rememberUpdatedState(order)

        for (k in order) {
            key(k) {
                val index = order.indexOf(k)
                val slot = IntOffset(((index % columns) * cellW).roundToInt(), ((index / columns) * cellH).roundToInt())
                val dragging = draggingKey == k
                val animated by animateIntOffsetAsState(slot, spring(stiffness = Spring.StiffnessMediumLow), label = "slot")
                val pos = if (dragging) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else animated
                Box(
                    Modifier
                        .offset { pos }
                        .size(cellWDp, cellHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            val s = if (dragging) 1.12f else 1f
                            scaleX = s
                            scaleY = s
                            alpha = if (dragging) 0.95f else 1f
                        }
                        .pointerInput(k) { detectTapGestures(onTap = { onItemClick(k) }) }
                        .pointerInput(k, cellW, cellH) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val i = currentOrder.indexOf(k)
                                    draggingKey = k
                                    dragPos = Offset((i % columns) * cellW, (i / columns) * cellH)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragPos += amount
                                    val col = ((dragPos.x + cellW / 2) / cellW).toInt().coerceIn(0, columns - 1)
                                    val row = ((dragPos.y + cellH / 2) / cellH).toInt().coerceIn(0, rows - 1)
                                    val target = (row * columns + col).coerceIn(0, currentOrder.lastIndex)
                                    val cur = currentOrder.indexOf(k)
                                    if (cur >= 0 && target != cur) {
                                        val list = currentOrder.toMutableList()
                                        list.removeAt(cur)
                                        list.add(target, k)
                                        order = list
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    draggingKey = null
                                    onReorder(currentOrder)
                                },
                                onDragCancel = { draggingKey = null }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    itemContent(k, dragging)
                }
            }
        }
    }
}

/** Branded loading indicator: a rotating arc around the logo. */
@Composable
fun NexusLoader(modifier: Modifier = Modifier, size: Dp = 76.dp) {
    val accent = LocalAccent.current
    val transition = rememberInfiniteTransition(label = "loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            rotate(angle) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, accent)),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        NexusLogo(size = size * 0.56f)
    }
}
