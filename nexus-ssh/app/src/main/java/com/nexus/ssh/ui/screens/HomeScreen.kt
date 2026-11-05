package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.ui.GridIcon
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.PulseIcon
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.WhaleIcon
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.IconTile
import com.nexus.ssh.ui.components.NexusLogo
import com.nexus.ssh.ui.components.PrimaryButton
import com.nexus.ssh.ui.components.ServerStackIllustration
import com.nexus.ssh.ui.components.StatusDot
import com.nexus.ssh.ui.glass
import java.util.Locale

@Composable
fun HomeScreen(
    displayName: String,
    servers: List<ServerEntity>,
    selected: ServerEntity?,
    connection: ConnectionState,
    telemetry: Telemetry?,
    unread: Int,
    onNavigate: (String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAddServer: () -> Unit
) {
    val connected = connection is ConnectionState.Connected
    val connectedCount = if (connected) 1 else 0
    val total = servers.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        // ---- header
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(Routes.SERVERS) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexusLogo()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nexus SSH", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Secure • Manage • Anywhere", color = Nx.Muted, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Box {
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                if (unread > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(16.dp)
                            .background(Nx.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (unread > 9) "9+" else unread.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---- greeting + illustration
        Row(Modifier.fillMaxWidth().padding(top = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (displayName.isBlank()) "Hello" else "Hello, $displayName",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        total == 0 -> "No servers added yet"
                        else -> "$connectedCount of $total server${if (total == 1) "" else "s"} connected"
                    },
                    color = Nx.Muted,
                    fontSize = 16.sp
                )
            }
            ServerStackIllustration(Modifier.size(width = 124.dp, height = 112.dp))
        }

        // ---- quick access grid
        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth()) {
            IconTile(Icons.Rounded.Folder, Nx.Blue, "Files", { onNavigate(Routes.FILES) }, Modifier.weight(1f))
            IconTile(PromptIcon, Color.White, "Terminal", { onNavigate(Routes.TERMINAL) }, Modifier.weight(1f))
            IconTile(PulseIcon, Nx.Green, "Monitor", { onNavigate(Routes.MONITOR) }, Modifier.weight(1f))
            IconTile(WhaleIcon, Nx.Blue, "Docker", { onNavigate(Routes.DOCKER) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            IconTile(Icons.Rounded.Hub, Nx.Purple, "Services", { onNavigate(Routes.SERVICES) }, Modifier.weight(1f))
            IconTile(GridIcon, Color.White, "Apps", { onNavigate(Routes.APPS) }, Modifier.weight(1f))
            IconTile(Icons.Rounded.Description, Color.White, "Logs", { onNavigate(Routes.LOGS) }, Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }

        // ---- active server card (connect / disconnect lives here)
        Spacer(Modifier.height(30.dp))
        if (selected == null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add your first server", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Connect over SSH with a password or private key.", color = Nx.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Add server", onAddServer)
                }
            }
        } else {
            val connecting = connection is ConnectionState.Connecting
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(
                            when {
                                connected -> Nx.Green
                                connecting -> Nx.Amber
                                else -> Nx.Faint
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selected.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                "${selected.username}@${selected.hostname}:${selected.port}",
                                color = Nx.Muted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (connected) Nx.Red.copy(alpha = 0.18f) else LocalAccent.current.copy(alpha = 0.20f))
                                .clickable(enabled = !connecting) { if (connected) onDisconnect() else onConnect() }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                when {
                                    connecting -> "Connecting…"
                                    connected -> "Disconnect"
                                    else -> "Connect"
                                },
                                color = if (connected) Nx.Red else LocalAccent.current,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (connected && telemetry != null) {
                        Spacer(Modifier.height(10.dp))
                        val mem = if (telemetry.memTotalKb > 0) telemetry.memUsedKb * 100f / telemetry.memTotalKb else 0f
                        Text(
                            String.format(Locale.US, "CPU %.1f%%  ·  RAM %.1f%%  ·  %s", telemetry.cpuPercent, mem, telemetry.uptime),
                            color = Nx.Muted,
                            fontSize = 12.sp
                        )
                    }
                    if (connection is ConnectionState.Error) {
                        Spacer(Modifier.height(10.dp))
                        Text(connection.message, color = Nx.Red, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
