package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ProcessesState
import com.nexus.ssh.core.ssh.ProcessInfo
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.nexusFieldColors
import java.util.Locale

@Composable
fun ProcessesScreen(
    connected: Boolean,
    serverName: String,
    state: ProcessesState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onKill: (ProcessInfo, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var menuFor by remember { mutableStateOf<ProcessInfo?>(null) }
    var pending by remember { mutableStateOf<Pair<ProcessInfo, String>?>(null) }

    val shown = remember(state.items, query, sort) {
        val q = query.trim()
        state.items
            .filter { q.isEmpty() || it.command.contains(q, true) || it.user.contains(q, true) || it.pid.toString() == q }
            .sortedByDescending { if (sort == 0) it.cpu else it.mem }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Processes", onBack, subtitle = serverName.ifBlank { null }) {
            IconButton(onClick = onRefresh, enabled = connected) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = if (connected) Nx.Green else Nx.Faint)
            }
        }
        if (!connected) {
            EmptyState(
                icon = Icons.Rounded.Memory,
                title = "Not connected",
                message = "Connect to a server to see its running processes.",
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
            placeholder = { Text("Search by name, user or PID") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Nx.Muted) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear", tint = Nx.Muted) }
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("CPU", "Memory").forEachIndexed { i, name ->
                Text(
                    "Sort: $name",
                    color = if (sort == i) Color.White else Nx.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sort == i) LocalAccent.current else Nx.SurfaceHigh)
                        .clickable { sort = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Text(
                "${shown.size} shown",
                color = Nx.Muted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        LoadingBar(state.loading)
        state.error?.let { ErrorBox(it, Modifier.padding(16.dp), onRefresh) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(shown, key = { it.pid }) { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { menuFor = p }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.command, color = Nx.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("PID ${p.pid} · ${p.user}", color = Nx.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            String.format(Locale.US, "%.1f%% CPU", p.cpu),
                            color = if (p.cpu >= 50f) Nx.Amber else Nx.Text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(String.format(Locale.US, "%.1f%% MEM", p.mem), color = Nx.Muted, fontSize = 12.sp)
                    }
                }
                RowDivider(startIndent = 20.dp)
            }
        }
    }

    menuFor?.let { p ->
        ActionSheet(
            title = p.command,
            subtitle = "PID ${p.pid} · ${p.user}",
            actions = listOf(
                SheetAction("Terminate (SIGTERM)", Icons.Rounded.Stop, Nx.Amber) { pending = p to "TERM" },
                SheetAction("Force kill (SIGKILL)", Icons.Rounded.Warning, Nx.Red) { pending = p to "KILL" }
            ),
            onDismiss = { menuFor = null }
        )
    }

    pending?.let { (proc, signal) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            shape = DialogShape,
            containerColor = Nx.SurfaceHigh,
            title = { Text(if (signal == "KILL") "Force kill ${proc.command}?" else "Terminate ${proc.command}?") },
            text = {
                Text(
                    "Sends SIG$signal to process ${proc.pid}. Stopping the wrong process can break services on the server.",
                    color = Nx.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { onKill(proc, signal); pending = null }) {
                    Text(if (signal == "KILL") "Kill" else "Terminate", color = if (signal == "KILL") Nx.Red else LocalAccent.current)
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel", color = Nx.Muted) } }
        )
    }
}
