package com.nexus.ssh.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.PulseIcon
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.WhaleIcon
import com.nexus.ssh.ui.components.GroupSurface
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.SectionHeader
import com.nexus.ssh.ui.components.StatusDot

class ToolSpec(val route: String, val icon: ImageVector, val tint: Color, val title: String, val subtitle: String)

val TOOLS = listOf(
    ToolSpec(Routes.FILES, Icons.Rounded.Folder, Nx.Blue, "Files", "Browse, edit, upload and download"),
    ToolSpec(Routes.TERMINAL, PromptIcon, Color.White, "Terminal", "Shell with tabs and sticky keys"),
    ToolSpec(Routes.MONITOR, PulseIcon, Nx.Green, "Monitor", "CPU, memory, disk and network"),
    ToolSpec(Routes.DOCKER, WhaleIcon, Nx.Blue, "Docker", "Containers, logs and controls"),
    ToolSpec(Routes.SERVICES, Icons.Rounded.Hub, Nx.Purple, "Services", "systemd units"),
    ToolSpec(Routes.LOGS, Icons.Rounded.Description, Color(0xFFFBBF24), "Logs", "Journal, kernel, SSH and files"),
    ToolSpec(Routes.BROWSER, Icons.Rounded.Language, Nx.Cyan, "Browser", "Open web apps running on the server")
)

fun toolFor(route: String): ToolSpec? = TOOLS.firstOrNull { it.route == route }

@Composable
private fun ToolRow(route: String, onNavigate: (String) -> Unit) {
    val tool = toolFor(route) ?: return
    ListRow(
        title = tool.title,
        subtitle = tool.subtitle,
        leading = { IconBadge(tool.icon, tool.tint) },
        trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Nx.Faint) },
        onClick = { onNavigate(route) }
    )
}

@Composable
fun AppsScreen(
    selected: ServerEntity?,
    connection: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val connected = connection is ConnectionState.Connected
    val connecting = connection is ConnectionState.Connecting
    val accent = LocalAccent.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Text("Apps", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 22.dp))

        SectionHeader("Server")
        GroupSurface {
            ListRow(
                title = selected?.name ?: "No server selected",
                subtitle = when {
                    selected == null -> "Add a server from the Servers tab"
                    connected -> "Connected · ${selected.username}@${selected.hostname}"
                    connecting -> "Connecting…"
                    else -> "${selected.username}@${selected.hostname}"
                },
                leading = {
                    StatusDot(
                        when {
                            connected -> Nx.Green
                            connecting -> Nx.Amber
                            else -> Nx.Faint
                        }
                    )
                },
                trailing = {
                    if (selected != null && !connecting) {
                        TextButton(onClick = { if (connected) onDisconnect() else onConnect() }) {
                            Text(
                                if (connected) "Disconnect" else "Connect",
                                color = if (connected) Nx.Red else accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            )
        }

        SectionHeader("Connect")
        GroupSurface {
            ToolRow(Routes.TERMINAL, onNavigate)
            RowDivider()
            ToolRow(Routes.FILES, onNavigate)
            RowDivider()
            ToolRow(Routes.BROWSER, onNavigate)
        }

        SectionHeader("Observe")
        GroupSurface {
            ToolRow(Routes.MONITOR, onNavigate)
            RowDivider()
            ToolRow(Routes.LOGS, onNavigate)
        }

        SectionHeader("Manage")
        GroupSurface {
            ToolRow(Routes.DOCKER, onNavigate)
            RowDivider()
            ToolRow(Routes.SERVICES, onNavigate)
        }
        Spacer(Modifier.height(24.dp))
    }
}
