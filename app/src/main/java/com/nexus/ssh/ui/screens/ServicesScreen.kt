package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ServicesState
import com.nexus.ssh.core.ssh.ServiceUnit
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.nexusFieldColors

private val FILTERS = listOf("All", "Running", "Stopped", "Failed")

@Composable
fun ServicesScreen(
    connected: Boolean,
    serverName: String,
    state: ServicesState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onAction: (ServiceUnit, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(1) }
    var menuFor by remember { mutableStateOf<ServiceUnit?>(null) }
    var pending by remember { mutableStateOf<Pair<ServiceUnit, String>?>(null) }

    val filtered = remember(state.units, query, filter) {
        state.units.filter {
            val q = query.trim()
            val matches = it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true)
            val status = when (filter) {
                1 -> it.running
                2 -> !it.running && it.active != "failed"
                3 -> it.active == "failed"
                else -> true
            }
            matches && status
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Services", onBack, subtitle = serverName.ifBlank { null }) {
            IconButton(onClick = onRefresh, enabled = connected) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = if (connected) Color.White else Nx.Faint)
            }
        }
        if (!connected) {
            EmptyState(
                icon = Icons.Rounded.Hub,
                title = "Not connected",
                message = "Connect to a server to manage its systemd services.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            return@Column
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = nexusFieldColors(),
            placeholder = { Text("Search services") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Nx.Muted) }
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FILTERS.forEachIndexed { i, name ->
                Text(
                    name,
                    color = if (filter == i) Color(0xFF0B0D12) else Nx.Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (filter == i) Color.White else Color.White.copy(alpha = 0.08f))
                        .clickable { filter = i }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
        LoadingBar(state.loading)
        state.error?.let { ErrorBox(it, Modifier.padding(16.dp), onRefresh) }
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            if (state.error == null && !state.loading && filtered.isEmpty()) {
                EmptyState(Icons.Rounded.Hub, "No services", "Nothing matches the current filter.")
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.name }) { u ->
                    val tint = when {
                        u.running -> Nx.Green
                        u.active == "failed" -> Nx.Red
                        else -> Nx.Muted
                    }
                    ListRow(
                        title = u.name.removeSuffix(".service"),
                        subtitle = u.description.ifBlank { "${u.active} (${u.sub})" },
                        leading = { IconBadge(Icons.Rounded.Hub, tint) },
                        trailing = {
                            StatusBadge(
                                when {
                                    u.running -> "Running"
                                    u.active == "failed" -> "Failed"
                                    else -> "Stopped"
                                },
                                tint
                            )
                        },
                        onClick = { menuFor = u }
                    )
                    RowDivider()
                }
            }
        }
    }

    menuFor?.let { u ->
        val actions = buildList {
            if (u.running) {
                add(SheetAction("Restart", Icons.Rounded.RestartAlt, Nx.Cyan) { pending = u to "restart" })
                add(SheetAction("Stop", Icons.Rounded.Stop, Nx.Red) { pending = u to "stop" })
            } else {
                add(SheetAction("Start", Icons.Rounded.PlayArrow, Nx.Green) { pending = u to "start" })
            }
        }
        ActionSheet(title = u.name, subtitle = "${u.active} (${u.sub})", actions = actions, onDismiss = { menuFor = null })
    }

    pending?.let { (unit, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            shape = DialogShape,
            containerColor = Nx.SurfaceHigh,
            title = { Text("${action.replaceFirstChar { it.uppercase() }} ${unit.name}?") },
            text = { Text("Runs systemctl $action on the server (via sudo -n if you are not root).", color = Nx.Muted) },
            confirmButton = {
                TextButton(onClick = { onAction(unit, action); pending = null }) {
                    Text("Confirm", color = if (action == "stop") Nx.Red else LocalAccent.current)
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel", color = Nx.Muted) } }
        )
    }
}
