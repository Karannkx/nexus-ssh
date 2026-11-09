package com.nexus.ssh.ui.screens

import com.nexus.ssh.ui.GeistMonoFamily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.LogSource
import com.nexus.ssh.LogsState
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.glass

@Composable
fun LogsScreen(
    connected: Boolean,
    serverName: String,
    state: LogsState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onLoadSource: (LogSource) -> Unit,
    onLoadFile: (String) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var path by remember { mutableStateOf(state.customPath.ifBlank { "/var/log/syslog" }) }
    val listState = rememberLazyListState()
    val shown = remember(state.lines, filter) {
        if (filter.isBlank()) state.lines else state.lines.filter { it.contains(filter.trim(), ignoreCase = true) }
    }

    // load the default source the first time the screen is opened while connected
    LaunchedEffect(connected) {
        if (connected && state.lines.isEmpty() && !state.loading && state.error == null) {
            onLoadSource(state.source ?: LogSource.SYSTEM)
        }
    }
    LaunchedEffect(shown.size) {
        if (shown.isNotEmpty()) listState.scrollToItem(shown.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding().heightIn(min = 58.dp).padding(horizontal = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("Logs", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(serverName.ifBlank { "Remote system logs" }, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1)
            }
            IconButton(
                onClick = { if (state.source != null) onLoadSource(state.source) else onLoadFile(path) },
                enabled = connected
            ) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = if (connected) Color(0xFFB7C4BB) else Color(0xFF666666)) }
        }
        if (!connected) {
            EmptyState(
                icon = Icons.Rounded.Description,
                title = "Not connected",
                message = "Connect to a server to read its system logs.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogSource.values().forEach { src ->
                val sel = state.source == src
                Text(
                    src.label,
                    color = if (sel) Color.White else Color(0xFFB8B8B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) LocalAccent.current else Color(0xFF242424))
                        .clickable { onLoadSource(src) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
            val fileSel = state.source == null
            Text(
                "File",
                color = if (fileSel) Color.White else Color(0xFFB8B8B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (fileSel) LocalAccent.current else Color(0xFF242424))
                    .clickable { onLoadFile(path) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }

        if (state.source == null) {
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Color(0xFF454545),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF171717), unfocusedContainerColor = Color(0xFF171717),
                    focusedLabelColor = LocalAccent.current, unfocusedLabelColor = Color(0xFF9CA3AF),
                    cursorColor = LocalAccent.current, focusedPlaceholderColor = Color(0xFF8B949E),
                    unfocusedPlaceholderColor = Color(0xFF8B949E)
                ),
                label = { Text("Remote log file path") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onLoadFile(path) })
            )
        }
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Color(0xFF454545),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF171717), unfocusedContainerColor = Color(0xFF171717),
                    focusedLabelColor = LocalAccent.current, unfocusedLabelColor = Color(0xFF9CA3AF),
                    cursorColor = LocalAccent.current, focusedPlaceholderColor = Color(0xFF8B949E),
                    unfocusedPlaceholderColor = Color(0xFF8B949E)
                ),
            placeholder = { Text("Filter lines") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF9CA3AF)) }
        )
        LoadingBar(state.loading)
        state.error?.let { ErrorBox(it, Modifier.padding(16.dp)) }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black)
        ) {
            if (shown.isEmpty() && !state.loading && state.error == null) {
                Text("No log lines.", color = Color(0xFF9CA3AF), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(10.dp)
            ) {
                items(shown) { line ->
                    val lower = line.lowercase()
                    val color = when {
                        "error" in lower || "fail" in lower || "denied" in lower || "critical" in lower -> Color(0xFFFF8A9B)
                        "warn" in lower -> Color(0xFFFBBF24)
                        else -> Color(0xFFCBD5E1)
                    }
                    Text(line.ifEmpty { " " }, color = color, fontFamily = GeistMonoFamily, fontSize = 11.sp, softWrap = false)
                }
            }
        }
    }
}
