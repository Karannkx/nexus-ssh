package com.nexus.ssh.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.ui.*
import java.util.Locale

fun formatKb(kb: Long): String {
    val gb = kb / 1024.0 / 1024.0
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb)
    else String.format(Locale.US, "%.0f MB", kb / 1024.0)
}

private fun percent(used: Long, total: Long): Float =
    if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = NexusMuted)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

@Composable
private fun Meter(title: String, value: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = NexusMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = NexusBorderNavy
        )
    }
}

@Composable
fun DashboardScreen(
    server: ServerEntity,
    connection: ConnectionState,
    telemetry: Telemetry?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    val connected = connection is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(
                        text = when (connection) {
                            is ConnectionState.Connected -> "CONNECTED"
                            is ConnectionState.Connecting -> "CONNECTING"
                            is ConnectionState.HostKeyConfirmationRequired -> "VERIFY HOST KEY"
                            is ConnectionState.Error -> "ERROR"
                            ConnectionState.Disconnected -> "DISCONNECTED"
                        },
                        color = when (connection) {
                            is ConnectionState.Connected -> NexusEmerald
                            is ConnectionState.Connecting, is ConnectionState.HostKeyConfirmationRequired -> NexusAmber
                            else -> NexusRose
                        }
                    )
                    if (connected) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan)
                        }
                    } else if (connection !is ConnectionState.Connecting) {
                        TextButton(onClick = onConnect) {
                            Text("Connect", color = NexusCyan, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${server.username}@${server.hostname}:${server.port}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (connection is ConnectionState.Error) {
                    Spacer(Modifier.height(4.dp))
                    Text(connection.message, style = MaterialTheme.typography.bodySmall, color = NexusRose)
                }
                if (telemetry != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = telemetry.uptime,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = NexusMuted
                    )
                }
            }
        }

        if (connected && telemetry == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NexusCyan)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading server metrics…", color = NexusMuted)
                }
            }
        }

        if (telemetry != null) {
            item {
                SectionCard {
                    SectionLabel("HARDWARE RESOURCES", NexusCyan)
                    Spacer(Modifier.height(8.dp))
                    Meter(
                        title = "CPU" + if (telemetry.cores > 0) " · ${telemetry.cores} cores" else "",
                        value = String.format(Locale.US, "%.1f%%", telemetry.cpuPercent),
                        fraction = telemetry.cpuPercent / 100f,
                        color = NexusCyan
                    )
                    Meter(
                        title = "Memory",
                        value = "${formatKb(telemetry.memUsedKb)} / ${formatKb(telemetry.memTotalKb)}",
                        fraction = percent(telemetry.memUsedKb, telemetry.memTotalKb),
                        color = NexusEmerald
                    )
                    Meter(
                        title = "Disk (/)",
                        value = "${formatKb(telemetry.diskUsedKb)} / ${formatKb(telemetry.diskTotalKb)}",
                        fraction = percent(telemetry.diskUsedKb, telemetry.diskTotalKb),
                        color = NexusAmber
                    )
                }
            }
            item {
                SectionCard {
                    SectionLabel("SYSTEM", NexusEmerald)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Hostname", telemetry.hostname)
                    InfoRow("OS", telemetry.os)
                    InfoRow("Kernel", telemetry.kernel)
                    InfoRow("Load (1/5/15m)", telemetry.loadAvg)
                    InfoRow("Auth", server.authType.name)
                }
            }
        }
    }
}
