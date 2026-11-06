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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.nexus.ssh.ui.components.AppTile
import com.nexus.ssh.ui.components.GroupSurface
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.NexusLogo
import com.nexus.ssh.ui.components.ReorderableGrid
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.SectionHeader
import com.nexus.ssh.ui.components.ServerStackIllustration
import com.nexus.ssh.ui.components.relativeTime
import com.nexus.ssh.ui.serverIconSpec
import java.util.Locale

private class HomeTile(val route: String, val label: String, val icon: ImageVector, val tint: Color)

private val HOME_TILES = listOf(
    HomeTile(Routes.FILES, "Files", Icons.Rounded.Folder, Nx.Blue),
    HomeTile(Routes.TERMINAL, "Terminal", PromptIcon, Color(0xFFE2E8F0)),
    HomeTile(Routes.MONITOR, "Monitor", PulseIcon, Nx.Green),
    HomeTile(Routes.DOCKER, "Docker", WhaleIcon, Color(0xFF38A3F5)),
    HomeTile(Routes.SERVICES, "Services", Icons.Rounded.Hub, Nx.Purple),
    HomeTile(Routes.APPS, "Apps", GridIcon, Color(0xFFCBD5E1)),
    HomeTile(Routes.LOGS, "Logs", Icons.Rounded.Description, Nx.Amber),
    HomeTile(Routes.BROWSER, "Browser", Icons.Rounded.Language, Nx.Cyan)
)

/** Saved order first, then any tile that is not in the saved list yet. */
private fun orderedKeys(saved: List<String>): List<String> {
    val known = HOME_TILES.map { it.route }
    val kept = saved.filter { it in known }.distinct()
    return kept + known.filter { it !in kept }
}

@Composable
fun HomeScreen(
    displayName: String,
    servers: List<ServerEntity>,
    selected: ServerEntity?,
    connection: ConnectionState,
    telemetry: Telemetry?,
    unread: Int,
    icons: Map<Long, String>,
    tileOrder: List<String>,
    onNavigate: (String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onSelectServer: (ServerEntity) -> Unit,
    onConnectServer: (ServerEntity) -> Unit,
    onDisconnect: () -> Unit,
    onAddServer: () -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val connectedId = (connection as? ConnectionState.Connected)?.server?.id
    val connecting = connection is ConnectionState.Connecting
    val total = servers.size
    val connectedCount = if (connectedId != null) 1 else 0
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val accent = LocalAccent.current
    val listed = (listOfNotNull(selected) + servers.filter { it.id != selected?.id }).take(5)

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
        Row(Modifier.fillMaxWidth().padding(top = 26.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    if (total == 0) "No servers added yet"
                    else "$connectedCount of $total server${if (total == 1) "" else "s"} connected",
                    color = Nx.Muted,
                    fontSize = 16.sp
                )
            }
            ServerStackIllustration(Modifier.size(width = 120.dp, height = 108.dp))
        }

        // ---- quick access: long-press an icon and drag it to rearrange
        Spacer(Modifier.height(24.dp))
        val keys = orderedKeys(tileOrder)
        ReorderableGrid(
            keys = keys,
            columns = if (wide) 8 else 4,
            cellHeight = 102.dp,
            onReorder = onReorder,
            onItemClick = onNavigate
        ) { key, dragging ->
            val tile = HOME_TILES.first { it.route == key }
            AppTile(tile.icon, tile.tint, tile.label, dragging)
        }
        Text(
            "Touch and hold an icon to move it",
            color = Nx.Faint,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )

        // ---- servers
        SectionHeader("Servers", Modifier.padding(top = 4.dp))
        GroupSurface {
            listed.forEach { server ->
                val spec = serverIconSpec(icons[server.id])
                val isConnected = server.id == connectedId
                val isSelected = server.id == selected?.id
                ListRow(
                    title = server.name,
                    subtitle = when {
                        isConnected && telemetry != null -> String.format(
                            Locale.US, "Connected · CPU %.0f%% · %s", telemetry.cpuPercent, telemetry.uptime
                        )
                        isConnected -> "Connected · ${server.username}@${server.hostname}"
                        isSelected && connection is ConnectionState.Error -> connection.message
                        else -> "${server.username}@${server.hostname} · ${relativeTime(server.lastConnectedAt)}"
                    },
                    leading = { IconBadge(spec.icon, spec.tint) },
                    trailing = {
                        when {
                            isConnected -> TextButton(onClick = onDisconnect) { Text("Disconnect", color = Nx.Red, fontWeight = FontWeight.SemiBold) }
                            connecting && isSelected -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = accent)
                            else -> TextButton(onClick = { onConnectServer(server) }) { Text("Connect", color = accent, fontWeight = FontWeight.SemiBold) }
                        }
                    },
                    onClick = { onSelectServer(server) }
                )
                RowDivider()
            }
            ListRow(
                title = "Add server",
                titleColor = accent,
                leading = { IconBadge(Icons.Rounded.Add, accent) },
                onClick = onAddServer
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
