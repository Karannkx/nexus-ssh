package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.DockerState
import com.nexus.ssh.ServicesState
import com.nexus.ssh.core.ssh.DockerContainer
import com.nexus.ssh.core.ssh.ServiceUnit
import com.nexus.ssh.ui.*

@Composable
private fun NotConnected(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = NexusMuted)
    }
}

@Composable
private fun ListHeader(title: String, loading: Boolean, onRefresh: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan)
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NexusCyan)
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
fun DockerScreen(
    connected: Boolean,
    state: DockerState,
    onRefresh: () -> Unit,
    onAction: (DockerContainer, String) -> Unit
) {
    if (!connected) {
        NotConnected("Connect to a server to manage Docker containers.")
        return
    }
    Column(Modifier.fillMaxSize()) {
        ListHeader("Docker containers", state.loading, onRefresh)
        state.error?.let {
            Text(it, color = NexusRose, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
        if (state.error == null && !state.loading && state.containers.isEmpty()) {
            Text("No containers found.", color = NexusMuted, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.containers, key = { it.id }) { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(
                                when (c.state) {
                                    "running" -> NexusEmerald
                                    "exited", "created" -> NexusMuted
                                    else -> NexusAmber
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(c.name, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            if (c.running) {
                                IconButton(onClick = { onAction(c, "restart") }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Restart", tint = NexusCyan)
                                }
                                IconButton(onClick = { onAction(c, "stop") }) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = NexusRose)
                                }
                            } else {
                                IconButton(onClick = { onAction(c, "start") }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = NexusEmerald)
                                }
                            }
                        }
                        Text(c.image, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NexusMuted)
                        Text(c.status, fontSize = 12.sp, color = NexusMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun ServicesScreen(
    connected: Boolean,
    state: ServicesState,
    onRefresh: () -> Unit,
    onAction: (ServiceUnit, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var runningOnly by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf<Pair<ServiceUnit, String>?>(null) }

    val filtered = remember(state.units, query, runningOnly) {
        state.units.filter {
            (!runningOnly || it.running) && it.name.contains(query.trim(), ignoreCase = true)
        }
    }

    if (!connected) {
        NotConnected("Connect to a server to manage systemd services.")
        return
    }

    Column(Modifier.fillMaxSize()) {
        ListHeader("systemd services", state.loading, onRefresh)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Filter", color = NexusMuted) }
            )
            FilterChip(
                selected = runningOnly,
                onClick = { runningOnly = !runningOnly },
                label = { Text("Running") }
            )
        }
        state.error?.let {
            Text(it, color = NexusRose, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.name }) { u ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(
                            when {
                                u.running -> NexusEmerald
                                u.active == "failed" -> NexusRose
                                else -> NexusMuted
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("${u.active} (${u.sub})", fontSize = 12.sp, color = NexusMuted)
                            if (u.description.isNotBlank()) {
                                Text(u.description, fontSize = 12.sp, color = NexusMuted, maxLines = 1)
                            }
                        }
                        if (u.running) {
                            IconButton(onClick = { pending = u to "restart" }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Restart", tint = NexusCyan)
                            }
                            IconButton(onClick = { pending = u to "stop" }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = NexusRose)
                            }
                        } else {
                            IconButton(onClick = { pending = u to "start" }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = NexusEmerald)
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { (unit, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("${action.replaceFirstChar { it.uppercase() }} ${unit.name}?") },
            text = { Text("Runs systemctl $action on the server (via sudo if you are not root).") },
            confirmButton = {
                TextButton(onClick = {
                    onAction(unit, action)
                    pending = null
                }) { Text("Confirm", color = NexusCyan) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
            containerColor = NexusCardNavy
        )
    }
}
