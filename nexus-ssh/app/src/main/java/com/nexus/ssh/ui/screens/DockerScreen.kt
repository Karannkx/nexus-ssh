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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.WhaleIcon
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.StatusDot

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
    state: DockerState,
    details: DockerDetails?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onAction: (DockerContainer, String) -> Unit,
    onOpenDetails: (DockerContainer) -> Unit,
    onCloseDetails: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Docker", onBack) {
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
            Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge("$running running", Nx.Green)
                StatusBadge("${state.containers.size - running} stopped", Nx.Muted)
            }
        }
        state.error?.let { err ->
            val hint = when {
                err.contains("not found", ignoreCase = true) -> "Docker doesn't seem to be installed on this server.\n\n$err"
                err.contains("permission denied", ignoreCase = true) -> "Your user can't access Docker. Add it to the docker group or allow passwordless sudo.\n\n$err"
                else -> err
            }
            ErrorBox(hint, Modifier.padding(16.dp), onRefresh)
        }
        if (state.error == null && !state.loading && state.containers.isEmpty()) {
            EmptyState(CubeIcon, "No containers", "Docker is reachable but there are no containers yet.")
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.containers, key = { it.id }) { c ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { onOpenDetails(c) }) {
                    Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(
                                when (c.state) {
                                    "running" -> Nx.Green
                                    "restarting", "paused" -> Nx.Amber
                                    "dead" -> Nx.Red
                                    else -> Nx.Faint
                                },
                                size = 9.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(c.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                            StatusBadge(c.state.replaceFirstChar { it.uppercase() }, if (c.running) Nx.Green else Nx.Muted)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(c.image, color = Nx.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(c.status, color = Nx.Muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
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

    if (details != null) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
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
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LoadingBar(details.loading)
                details.error?.let { ErrorBox(it) }
                Box(Modifier.fillMaxWidth().height(320.dp)) {
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
