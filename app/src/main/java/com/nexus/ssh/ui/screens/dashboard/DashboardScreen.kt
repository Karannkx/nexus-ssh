package com.nexus.ssh.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.ssh.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToAddServer: () -> Unit,
    onNavigateToTerminal: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Server Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "production-cluster-01",
                    style = MaterialTheme.typography.titleLarge,
                    color = NexusTextPrimary
                )
                Text(
                    text = "Ubuntu 24.04 LTS · Linux 6.8.0-45-generic · 192.168.1.120",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNavigateToTerminal) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = NexusCyan)
                }
                IconButton(onClick = onNavigateToAddServer) {
                    Icon(Icons.Default.Add, contentDescription = "Add Server", tint = NexusTextPrimary)
                }
            }
        }

        // Live Metric Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "CPU LOAD",
                value = "24.6%",
                subtext = "8 Cores · 2.4 GHz",
                progress = 0.246f,
                accentColor = NexusCyan
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "MEMORY",
                value = "8.4 GB",
                subtext = "of 31.2 GB (26.9%)",
                progress = 0.269f,
                accentColor = NexusEmerald
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "STORAGE /",
                value = "142 GB",
                subtext = "of 480 GB (29.5%)",
                progress = 0.295f,
                accentColor = NexusAmber
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "NETWORK I/O",
                value = "1.2 MB/s",
                subtext = "TX 840 KB/s · RX 410 KB/s",
                progress = 0.42f,
                accentColor = NexusCyanLight
            )
        }

        // System Load Average & Uptime Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexusSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SYSTEM TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoColumn(label = "Uptime", value = "18 days, 4 hours")
                    InfoColumn(label = "Load Avg (1/5/15m)", value = "0.42 · 0.68 · 0.55")
                    InfoColumn(label = "SSH Port", value = "22 (ed25519)")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    progress: Float,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = NexusTextMuted)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = NexusTextPrimary)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = NexusSurfaceElevated,
            )
            Text(text = subtext, style = MaterialTheme.typography.bodySmall, color = NexusTextSecondary)
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = NexusTextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = NexusTextPrimary)
    }
}
