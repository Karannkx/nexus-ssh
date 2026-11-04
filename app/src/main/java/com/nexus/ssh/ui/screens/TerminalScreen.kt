package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.*

private data class TermKey(val label: String, val bytes: String)

private val TERM_KEYS = listOf(
    TermKey("ESC", "\u001b"),
    TermKey("TAB", "\t"),
    TermKey("^C", "\u0003"),
    TermKey("^D", "\u0004"),
    TermKey("^Z", "\u001a"),
    TermKey("▲", "\u001b[A"),
    TermKey("▼", "\u001b[B"),
    TermKey("◀", "\u001b[D"),
    TermKey("▶", "\u001b[C")
)

@Composable
fun TerminalScreen(
    connected: Boolean,
    lines: List<String>,
    onSendLine: (String) -> Unit,
    onSendRaw: (String) -> Unit,
    onClear: () -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, lines.lastOrNull()) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(NexusTerminalBg)) {
        // 12sp monospace is ~0.6em wide; keep PTY size in sync with what fits on screen
        val cols = ((maxWidth.value - 24f) / (12f * 0.62f)).toInt().coerceIn(20, 300)
        val rows = ((maxHeight.value - 130f) / 16f).toInt().coerceIn(8, 100)
        LaunchedEffect(cols, rows) { onResize(cols, rows) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!connected && lines.isEmpty()) {
                    Text(
                        "Not connected. Tap Connect in the top bar to open a shell.",
                        color = NexusMuted,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(lines) { _, line ->
                            Text(
                                text = line.ifEmpty { " " },
                                color = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TERM_KEYS.forEach { key ->
                    KeyChip(key.label, enabled = connected) { onSendRaw(key.bytes) }
                }
                KeyChip("CLEAR", enabled = true) { onClear() }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexusCardNavy)
                    .padding(8.dp),
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
                    placeholder = { Text("Type a command…", fontSize = 13.sp, color = NexusMuted) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { submit() })
                )
                IconButton(onClick = { submit() }, enabled = connected) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NexusCyan)
                }
            }
        }
    }
}

@Composable
private fun KeyChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = NexusBorderNavy,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (enabled) NexusCyan else NexusMuted
        )
    }
}
