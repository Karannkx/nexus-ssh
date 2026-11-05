package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.TerminalTabInfo
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.glass
import com.nexus.ssh.ui.nexusFieldColors

private class TermKey(val label: String, val bytes: String)

private val TERM_KEYS = listOf(
    TermKey("ESC", "\u001b"),
    TermKey("TAB", "\t"),
    TermKey("CTRL+C", "\u0003"),
    TermKey("CTRL+D", "\u0004"),
    TermKey("CTRL+Z", "\u001a"),
    TermKey("▲", "\u001b[A"),
    TermKey("▼", "\u001b[B"),
    TermKey("◀", "\u001b[D"),
    TermKey("▶", "\u001b[C"),
    TermKey("/", "/"),
    TermKey("-", "-"),
    TermKey("|", "|"),
    TermKey("~", "~")
)

@Composable
fun TerminalScreen(
    connected: Boolean,
    serverName: String,
    tabs: List<TerminalTabInfo>,
    activeTab: Int,
    lines: List<String>,
    fontSize: Int,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onSendLine: (String) -> Unit,
    onSendRaw: (String) -> Unit,
    onClear: () -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val accent = LocalAccent.current

    LaunchedEffect(lines.size, lines.lastOrNull(), activeTab) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Terminal", onBack) {
            if (serverName.isNotBlank()) {
                Text(serverName, color = Nx.Muted, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(end = 4.dp))
            }
            TextKeyButton("CLEAR", enabled = connected) { onClear() }
            Spacer(Modifier.width(8.dp))
        }

        if (!connected && tabs.isEmpty()) {
            EmptyState(
                icon = PromptIcon,
                title = "Not connected",
                message = "Connect to a server to open an interactive shell.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            return@Column
        }

        // tabs
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs, key = { it.id }) { tab ->
                    val sel = tab.id == activeTab
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.08f))
                            .clickable { onSelectTab(tab.id) }
                            .padding(start = 14.dp, end = if (tabs.size > 1) 6.dp else 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tab.title,
                            color = if (sel) Color(0xFF0B0D12) else Nx.Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (tabs.size > 1) {
                            IconButton(onClick = { onCloseTab(tab.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close tab",
                                    tint = if (sel) Color(0xFF0B0D12) else Nx.Muted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onNewTab, enabled = connected) {
                Icon(Icons.Rounded.Add, contentDescription = "New terminal tab", tint = if (connected) accent else Nx.Faint)
            }
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .glass(RoundedCornerShape(20.dp), fill = 0.5f)
                .background(Nx.Terminal.copy(alpha = 0.78f))
        ) {
            val cols = ((maxWidth.value - 24f) / (fontSize * 0.62f)).toInt().coerceIn(20, 300)
            val rows = ((maxHeight.value - 16f) / (fontSize * 1.35f)).toInt().coerceIn(8, 120)
            LaunchedEffect(cols, rows) { onResize(cols, rows) }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                itemsIndexed(lines) { _, line ->
                    Text(
                        text = line.ifEmpty { " " },
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.35f).sp
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TERM_KEYS.forEach { key -> TextKeyButton(key.label, enabled = connected) { onSendRaw(key.bytes) } }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fun submit() {
                if (!connected) return
                onSendLine(input)
                input = ""
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                enabled = connected,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = nexusFieldColors(),
                placeholder = { Text("Type a command…", fontSize = 14.sp) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { submit() })
            )
            IconButton(onClick = { submit() }, enabled = connected) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = if (connected) accent else Nx.Faint)
            }
        }
    }
}

@Composable
private fun TextKeyButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (enabled) Color.White else Nx.Faint,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
