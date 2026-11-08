package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.MetricSample
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.ChartSeries
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.LineChart
import com.nexus.ssh.ui.components.PrimaryButton
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.Sparkline
import com.nexus.ssh.ui.glass
import java.util.Locale

private class Range(val label: String, val millis: Long)

private val RANGES = listOf(
    Range("Last 1 Minute", 60_000L),
    Range("Last 5 Minutes", 300_000L),
    Range("Last 15 Minutes", 900_000L),
    Range("Last 30 Minutes", 1_800_000L)
)

fun formatRate(bytesPerSec: Float): String = when {
    bytesPerSec < 1024f -> String.format(Locale.US, "%.0f B/s", bytesPerSec)
    bytesPerSec < 1024f * 1024f -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024f)
    bytesPerSec < 1024f * 1024f * 1024f -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1024f / 1024f)
    else -> String.format(Locale.US, "%.2f GB/s", bytesPerSec / 1024f / 1024f / 1024f)
}

private fun niceMax(v: Float): Float {
    val steps = listOf(10e3f, 50e3f, 100e3f, 500e3f, 1e6f, 5e6f, 10e6f, 50e6f, 100e6f, 500e6f, 1e9f)
    return steps.firstOrNull { it >= v } ?: v
}

@Composable
private fun DropPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(16.dp), fill = 0.85f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Nx.Text, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Nx.Text)
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    spark: List<Float>? = null,
    sparkMax: Float? = null
) {
    Column(
        modifier
            .glass(RoundedCornerShape(20.dp), fill = 0.82f)
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = Nx.Muted, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = Nx.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sub != null) Text(sub, color = Nx.Muted, fontSize = 12.sp)
        if (spark != null) {
            Spacer(Modifier.height(6.dp))
            Sparkline(spark, color, Modifier.fillMaxWidth().height(26.dp), sparkMax)
        }
    }
}

@Composable
fun MonitorScreen(
    servers: List<ServerEntity>,
    activeServer: ServerEntity?,
    connection: ConnectionState,
    telemetry: Telemetry?,
    samples: List<MetricSample>,
    onBack: () -> Unit,
    onPickServer: (ServerEntity) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    val connected = connection is ConnectionState.Connected
    var rangeIdx by rememberSaveable { mutableIntStateOf(0) }
    var metric by rememberSaveable { mutableIntStateOf(0) }
    var serverSheet by remember { mutableStateOf(false) }
    var rangeSheet by remember { mutableStateOf(false) }
    val accent = LocalAccent.current

    val lastT = samples.lastOrNull()?.t ?: 0L
    val windowed = samples.filter { it.t >= lastT - RANGES[rangeIdx].millis }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("System Monitor", onBack) {
            IconButton(onClick = onRefresh, enabled = connected) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = if (connected) Nx.Green else Nx.Faint)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropPill(label = activeServer?.name ?: "No servers", modifier = Modifier.weight(1f)) { serverSheet = true }
                DropPill(label = RANGES[rangeIdx].label, modifier = Modifier.weight(1f)) { rangeSheet = true }
            }
            Spacer(Modifier.height(14.dp))

            if (!connected) {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    EmptyState(
                        icon = Icons.Rounded.Speed,
                        title = if (connection is ConnectionState.Connecting) "Connecting…" else "Not connected",
                        message = "Connect to a server to see live CPU, memory, disk and network usage.",
                        actionLabel = if (activeServer != null && connection !is ConnectionState.Connecting) "Connect" else null,
                        onAction = onConnect
                    )
                }
            } else {
                val t = telemetry
                val memPct = if (t != null && t.memTotalKb > 0) t.memUsedKb * 100f / t.memTotalKb else null
                val diskPct = if (t != null && t.diskTotalKb > 0) t.diskUsedKb * 100f / t.diskTotalKb else null
                val cpuSpark = windowed.map { it.cpu }
                val memSpark = windowed.map { it.mem }
                val diskSpark = windowed.map { it.disk }
                val netSpark = windowed.map { it.rxBps }

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 440.dp
                    val serversCard: @Composable (Modifier) -> Unit = { m ->
                        MetricCard(Icons.Rounded.Cloud, "Servers", "1 / ${servers.size}", Nx.Green, m, sub = "Online")
                    }
                    val cpuCard: @Composable (Modifier) -> Unit = { m ->
                        MetricCard(
                            Icons.Rounded.Speed, "CPU",
                            if (t != null) String.format(Locale.US, "%.1f%%", t.cpuPercent) else "—",
                            Nx.Green, m, spark = cpuSpark, sparkMax = 100f
                        )
                    }
                    val memCard: @Composable (Modifier) -> Unit = { m ->
                        MetricCard(
                            Icons.Rounded.Memory, "Memory",
                            if (memPct != null) String.format(Locale.US, "%.1f%%", memPct) else "—",
                            Nx.Purple, m, spark = memSpark, sparkMax = 100f
                        )
                    }
                    val diskCard: @Composable (Modifier) -> Unit = { m ->
                        MetricCard(
                            Icons.Rounded.Storage, "Disk",
                            if (diskPct != null) String.format(Locale.US, "%.1f%%", diskPct) else "—",
                            Nx.Blue, m, spark = diskSpark, sparkMax = 100f
                        )
                    }
                    val netCard: @Composable (Modifier) -> Unit = { m ->
                        MetricCard(
                            Icons.Rounded.Wifi, "Network",
                            if (t != null) "↓ " + formatRate(t.rxBytesPerSec) else "—",
                            Nx.Magenta, m,
                            sub = if (t != null) "↑ " + formatRate(t.txBytesPerSec) else null,
                            spark = netSpark
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (compact) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                serversCard(Modifier.weight(1f)); cpuCard(Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                memCard(Modifier.weight(1f)); diskCard(Modifier.weight(1f))
                            }
                            netCard(Modifier.fillMaxWidth())
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                serversCard(Modifier.weight(1f)); cpuCard(Modifier.weight(1f)); memCard(Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                diskCard(Modifier.weight(1f)); netCard(Modifier.weight(1.4f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(24.dp), fill = 0.82f)
                        .padding(16.dp)
                ) {
                    Text("Resource Usage", color = Nx.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    // segmented selector
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Nx.SurfaceHigh)
                            .padding(4.dp)
                    ) {
                        listOf("CPU", "Memory", "Disk", "Network").forEachIndexed { i, name ->
                            val sel = metric == i
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) LocalAccent.current else Color.Transparent)
                                    .clickable { metric = i }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    name,
                                    color = if (sel) Color.White else Nx.Muted,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val times = windowed.map { it.t }
                    when (metric) {
                        0 -> LineChart(listOf(ChartSeries("CPU", Nx.Green, cpuSpark)), times, 100f, { String.format(Locale.US, "%.0f%%", it) })
                        1 -> LineChart(listOf(ChartSeries("Memory", Nx.Purple, memSpark)), times, 100f, { String.format(Locale.US, "%.0f%%", it) })
                        2 -> LineChart(listOf(ChartSeries("Disk", Nx.Blue, diskSpark)), times, 100f, { String.format(Locale.US, "%.0f%%", it) })
                        else -> {
                            val peak = windowed.maxOfOrNull { maxOf(it.rxBps, it.txBps) } ?: 0f
                            LineChart(
                                listOf(
                                    ChartSeries("↓ In", Nx.Magenta, windowed.map { it.rxBps }),
                                    ChartSeries("↑ Out", Nx.Cyan, windowed.map { it.txBps })
                                ),
                                times,
                                niceMax(peak),
                                { formatRate(it) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${windowed.size} sample${if (windowed.size == 1) "" else "s"} from ${activeServer?.name ?: "server"} · tap or drag the chart to inspect",
                        color = Nx.Muted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (t != null) {
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), fill = 0.82f).padding(16.dp)) {
                        Text("System", color = Nx.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        InfoLine("Hostname", t.hostname)
                        InfoLine("OS", t.os)
                        InfoLine("Kernel", t.kernel)
                        InfoLine("Uptime", t.uptime)
                        InfoLine("Load (1/5/15m)", t.loadAvg)
                        InfoLine("CPU cores", if (t.cores > 0) t.cores.toString() else "—")
                        InfoLine("Memory", "${formatKb(t.memUsedKb)} / ${formatKb(t.memTotalKb)}")
                        InfoLine("Disk (/)", "${formatKb(t.diskUsedKb)} / ${formatKb(t.diskTotalKb)}")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (serverSheet) {
        ActionSheet(
            title = "Monitor which server?",
            subtitle = "Choosing another server connects to it",
            actions = servers.map { srv ->
                SheetAction(
                    label = srv.name,
                    icon = Icons.Rounded.Cloud,
                    selected = srv.id == (connection as? ConnectionState.Connected)?.server?.id
                ) { onPickServer(srv) }
            },
            onDismiss = { serverSheet = false }
        )
    }
    if (rangeSheet) {
        ActionSheet(
            title = "Time range",
            actions = RANGES.mapIndexed { i, r -> SheetAction(label = r.label, selected = i == rangeIdx) { rangeIdx = i } },
            onDismiss = { rangeSheet = false }
        )
    }
}

fun formatKb(kb: Long): String {
    val gb = kb / 1024.0 / 1024.0
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else String.format(Locale.US, "%.0f MB", kb / 1024.0)
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Nx.Muted, fontSize = 13.sp)
        Spacer(Modifier.width(16.dp))
        Text(value, color = Nx.Text, fontSize = 13.sp, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
    }
}
