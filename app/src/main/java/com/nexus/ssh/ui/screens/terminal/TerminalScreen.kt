package com.nexus.ssh.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.theme.*

@Composable
fun TerminalScreen() {
    var commandInput by remember { mutableStateOf("") }
    val terminalLines = remember {
        mutableStateListOf(
            "Welcome to Nexus SSH Terminal Session (v1.0.0)",
            "Linux production-cluster-01 6.8.0-45-generic #45-Ubuntu SMP PREEMPT_DYNAMIC",
            "Last login: Sun Nov 04 06:42:15 2026 from 192.168.1.45",
            "user@production-cluster-01:~$ uname -a",
            "Linux production-cluster-01 6.8.0-45-generic x86_64 GNU/Linux",
            "user@production-cluster-01:~$ docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'",
            "NAMES              STATUS          PORTS",
            "nginx-proxy        Up 4 days       0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp",
            "postgres-db        Up 4 days       127.0.0.1:5432->5432/tcp",
            "redis-cache        Up 4 days       127.0.0.1:6379->6379/tcp",
            "user@production-cluster-01:~$ "
        )
    }

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusTerminalBg)
    ) {
        // Top session bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "user@production-cluster-01: ~ (xterm-256color)",
                style = MaterialTheme.typography.labelSmall,
                color = NexusCyanLight
            )
            Text(
                text = "80x24 · UTF-8",
                style = MaterialTheme.typography.labelSmall,
                color = NexusTextMuted
            )
        }

        // Terminal Output Console
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(terminalLines.size) { index ->
                Text(
                    text = terminalLines[index],
                    color = if (terminalLines[index].startsWith("user@")) NexusTerminalGreen else NexusTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Mobile Keyboard Accessory Bar (Esc, Tab, Ctrl, Alt, Arrow keys)
        val accessoryScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusSurfaceElevated)
                .horizontalScroll(accessoryScroll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton("ESC") { /* send byte 0x1B */ }
            KeyButton("TAB") {
                commandInput += "\t"
            }
            KeyButton("CTRL") {}
            KeyButton("ALT") {}
            KeyButton("▲") { /* Up arrow */ }
            KeyButton("▼") { /* Down arrow */ }
            KeyButton("◀") { /* Left arrow */ }
            KeyButton("▶") { /* Right arrow */ }
            KeyButton("CTRL+C") {
                terminalLines.add("^C")
                terminalLines.add("user@production-cluster-01:~$ ")
            }
            KeyButton("CTRL+D") {
                terminalLines.add("logout")
            }
            KeyButton("CLEAR") {
                terminalLines.clear()
                terminalLines.add("user@production-cluster-01:~$ ")
            }
        }

        // Command Prompt Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusSurface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NexusBackground,
                    unfocusedContainerColor = NexusBackground,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commandInput.isNotBlank()) {
                        terminalLines.add("user@production-cluster-01:~$ $commandInput")
                        terminalLines.add("Executing: $commandInput...")
                        commandInput = ""
                    }
                })
            )
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        terminalLines.add("user@production-cluster-01:~$ $commandInput")
                        terminalLines.add("Executing: $commandInput...")
                        commandInput = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NexusCyan)
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NexusBorder,
            contentColor = NexusTextPrimary
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
