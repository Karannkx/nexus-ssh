package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.DockerDetails
import com.nexus.ssh.DockerState
import com.nexus.ssh.core.ssh.DockerContainer
import com.nexus.ssh.ui.CubeIcon
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.WhaleIcon
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.StatusDot
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SheetAction

@Composable
fun StatusBadge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
fun DockerScreen(
    connected: Boolean,
    serverName: String,
    state: DockerState,
    details: DockerDetails?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onAction: (DockerContainer, String) -> Unit,
    onOpenDetails: (DockerContainer) -> Unit,
    onCloseDetails: () -> Unit,
    onOpenPort: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Docker", onBack, subtitle = serverName.ifBlank { null }) {
            IconButton(onClick = onRefresh, enabled = connected) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = if (connected) Color.White else Nx.Faint)
            }
        }
        if (!connected) {
            EmptyState(
                icon = WhaleIcon,
                title = "Not connected",
                message = "Connect to a server to manage its Docker containers.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            return@Column
        }
        LoadingBar(state.loading)
        val running = state.containers.count { it.running }
        if (state.containers.isNotEmpty()) {
            Text(
                "$running running · ${state.containers.size - running} stopped",
                color = Nx.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        state.error?.let { err ->
            val hint = when {
                err.contains("not found", ignoreCase = true) -> "Docker doesn't seem to be installed on this server.\n\n$err"
                err.contains("permission denied", ignoreCase = true) -> "Your user can't access Docker. Add it to the docker group or allow passwordless sudo.\n\n$err"
                else -> err
            }
            ErrorBox(hint, Modifier.padding(16.dp), onRefresh)
        }
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            if (state.error == null && !state.loading && state.containers.isEmpty()) {
                EmptyState(CubeIcon, "No containers", "Docker is reachable but there are no containers yet.")
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.containers, key = { it.id }) { c ->
                    val tint = when (c.state) {
                        "running" -> Nx.Green
                        "restarting", "paused" -> Nx.Amber
                        "dead" -> Nx.Red
                        else -> Nx.Muted
                    }
                    GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), onClick = { onOpenDetails(c) }) {
                        Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(tint, size = 9.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(c.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                                StatusBadge(c.state.replaceFirstChar { it.uppercase() }, tint)
                                Spacer(Modifier.width(10.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(c.image, color = Nx.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.status, color = Nx.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (c.running) {
                                    IconButton(onClick = { onAction(c, "restart") }) { Icon(Icons.Rounded.RestartAlt, "Restart", tint = Nx.Cyan) }
                                    IconButton(onClick = { onAction(c, "stop") }) { Icon(Icons.Rounded.Stop, "Stop", tint = Nx.Red) }
                                } else {
                                    IconButton(onClick = { onAction(c, "start") }) { Icon(Icons.Rounded.PlayArrow, "Start", tint = Nx.Green) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (details != null) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        // published host ports such as "80/tcp -> 0.0.0.0:8080"
        val hostPorts = remember(details.info) {
            details.info.firstOrNull { it.first == "Ports" }?.second.orEmpty().lines()
                .mapNotNull { Regex(":(\\d+)\\s*$").find(it.trim())?.groupValues?.get(1)?.toIntOrNull() }
                .distinct()
        }
        ModalBottomSheet(onDismissRequest = onCloseDetails, containerColor = Nx.SurfaceHigh) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                Text(details.container.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(details.container.image, color = Nx.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Info", "Logs").forEachIndexed { i, name ->
                        Text(
                            name,
                            color = if (tab == i) Color(0xFF0B0D12) else Nx.Muted,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (tab == i) Color.White else Color.White.copy(alpha = 0.08f))
                                .clickable { tab = i }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LoadingBar(details.loading)
                details.error?.let { ErrorBox(it) }
                if (hostPorts.isNotEmpty() && tab == 0) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hostPorts.forEach { p ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LocalAccent.current.copy(alpha = 0.18f))
                                    .clickable { onOpenPort(p) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = LocalAccent.current)
                                Spacer(Modifier.width(8.dp))
                                Text("Open :$p in browser", color = LocalAccent.current, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    if (tab == 0) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            details.info.forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text(k, color = Nx.Muted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                                    Text(v, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Nx.Terminal)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                details.logs.ifBlank { if (details.loading) "Loading…" else "No log output." },
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
