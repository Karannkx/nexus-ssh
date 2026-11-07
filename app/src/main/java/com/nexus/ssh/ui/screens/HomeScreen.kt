package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.ui.HOME_DEFAULT
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.appFor
import com.nexus.ssh.ui.components.AppTile
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.NexusLogo
import com.nexus.ssh.ui.components.PrimaryButton
import com.nexus.ssh.ui.components.ReorderableGrid
import com.nexus.ssh.ui.components.ServerStackIllustration
import com.nexus.ssh.ui.components.StatusDot
import java.util.Locale

/** Saved order first, then any default tile that is not in the saved list yet. */
private fun orderedKeys(saved: List<String>): List<String> {
    val kept = saved.filter { it in HOME_DEFAULT }.distinct()
    return kept + HOME_DEFAULT.filter { it !in kept }
}

@Composable
fun HomeScreen(
    displayName: String,
    servers: List<ServerEntity>,
    selected: ServerEntity?,
    connection: ConnectionState,
    telemetry: Telemetry?,
    unread: Int,
    tileOrder: List<String>,
    onNavigate: (String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAddServer: () -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val connected = connection is ConnectionState.Connected
    val connecting = connection is ConnectionState.Connecting
    val total = servers.size
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val accent = LocalAccent.current

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
                NexusLogo(size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nexus SSH", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Secure • Manage • Anywhere", color = Nx.Muted, fontSize = 12.sp, maxLines = 1)
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
        Row(Modifier.fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (displayName.isBlank()) "Hello" else "Hello, $displayName",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (total == 0) "No servers added yet"
                    else "${if (connected) 1 else 0} of $total server${if (total == 1) "" else "s"} connected",
                    color = Nx.Muted,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            ServerStackIllustration(Modifier.size(width = 104.dp, height = 96.dp))
        }

        // ---- quick access: touch and hold an icon, then drag it to rearrange
        Spacer(Modifier.height(26.dp))
        ReorderableGrid(
            keys = orderedKeys(tileOrder),
            columns = if (wide) 8 else 4,
            cellHeight = 104.dp,
            onReorder = onReorder,
            onItemClick = onNavigate
        ) { key, dragging ->
            val app = appFor(key)
            if (app != null) AppTile(app.label, dragging) { app.glyph() }
        }

        // ---- selected server
        Spacer(Modifier.height(20.dp))
        if (selected == null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add your first server", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Connect with a password or a private key.", color = Nx.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Add server", onAddServer)
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(
                            when {
                                connected -> Nx.Green
                                connecting -> Nx.Amber
                                else -> Nx.Faint
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selected.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${selected.username}@${selected.hostname}:${selected.port}",
                                color = Nx.Muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (connected) Nx.Red.copy(alpha = 0.16f) else accent.copy(alpha = 0.18f))
                                .clickable(enabled = !connecting) { if (connected) onDisconnect() else onConnect() }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                when {
                                    connecting -> "Connecting…"
                                    connected -> "Disconnect"
                                    else -> "Connect"
                                },
                                color = if (connected) Nx.Red else accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                    if (connected && telemetry != null) {
                        Spacer(Modifier.height(10.dp))
                        val mem = if (telemetry.memTotalKb > 0) telemetry.memUsedKb * 100f / telemetry.memTotalKb else 0f
                        Text(
                            String.format(Locale.US, "CPU %.0f%%  ·  RAM %.0f%%  ·  %s", telemetry.cpuPercent, mem, telemetry.uptime),
                            color = Nx.Muted,
                            fontSize = 12.sp,
                            maxLines = 2
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
