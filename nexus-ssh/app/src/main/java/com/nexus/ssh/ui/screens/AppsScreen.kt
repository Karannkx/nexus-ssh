package com.nexus.ssh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.PulseIcon
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.WhaleIcon
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.glass

class ToolSpec(val route: String, val icon: ImageVector, val tint: Color, val title: String, val subtitle: String)

val TOOLS = listOf(
    ToolSpec(Routes.FILES, Icons.Rounded.Folder, Nx.Blue, "Files", "Browse, edit, upload and download over SFTP"),
    ToolSpec(Routes.TERMINAL, PromptIcon, Color.White, "Terminal", "Interactive shell with multiple tabs"),
    ToolSpec(Routes.MONITOR, PulseIcon, Nx.Green, "Monitor", "Live CPU, memory, disk and network"),
    ToolSpec(Routes.DOCKER, WhaleIcon, Nx.Blue, "Docker", "Containers, logs and controls"),
    ToolSpec(Routes.SERVICES, Icons.Rounded.Hub, Nx.Purple, "Services", "Start, stop and restart systemd units"),
    ToolSpec(Routes.LOGS, Icons.Rounded.Description, Color.White, "Logs", "System, kernel, SSH and custom files")
)

@Composable
fun AppsScreen(onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Apps",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 4.dp)
        )
        Text(
            "Everything Nexus SSH can do on your server",
            color = Nx.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 22.dp, bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TOOLS, key = { it.route }) { tool ->
                GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), onClick = { onNavigate(tool.route) }) {
                    Column(Modifier.padding(18.dp)) {
                        Box(
                            Modifier.size(52.dp).glass(RoundedCornerShape(16.dp), fill = 0.95f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(tool.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(tool.subtitle, color = Nx.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
