package com.nexus.ssh.ui.screens

import com.nexus.ssh.ui.GeistMonoFamily

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.TerminalTabInfo
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PromptIcon
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ScreenTopBar
import kotlinx.coroutines.delay

private class TermKey(val label: String, val bytes: String)

private val TERM_KEYS = listOf(
    TermKey("ESC", "\u001b"),
    TermKey("TAB", "\t"),
    TermKey("▲", "\u001b[A"),
    TermKey("▼", "\u001b[B"),
    TermKey("◀", "\u001b[D"),
    TermKey("▶", "\u001b[C"),
    TermKey("HOME", "\u001b[H"),
    TermKey("END", "\u001b[F"),
    TermKey("PGUP", "\u001b[5~"),
    TermKey("PGDN", "\u001b[6~"),
    TermKey("^C", "\u0003"),
    TermKey("^D", "\u0004"),
    TermKey("^Z", "\u001a"),
    TermKey("|", "|"),
    TermKey("/", "/"),
    TermKey("-", "-"),
    TermKey("~", "~"),
    TermKey("_", "_")
)

private fun ctrlChar(c: Char): String = when {
    c in 'a'..'z' -> (c.code - 96).toChar().toString()
    c in 'A'..'Z' -> (c.code - 64).toChar().toString()
    c == '[' -> "\u001b"
    c == '\\' -> "\u001c"
    c == ']' -> "\u001d"
    c == ' ' -> "\u0000"
    else -> c.toString()
}

/** Applies the sticky CTRL / ALT modifiers to freshly typed text. */
private fun applyModifiers(text: String, ctrl: Boolean, alt: Boolean): String {
    val sb = StringBuilder()
    for (ch in text) {
        var out = when {
            ch == '\n' -> "\r"
            ch == '\u007f' -> "\u007f"
            ctrl -> ctrlChar(ch)
            else -> ch.toString()
        }
        if (alt && ch != '\u007f') out = "\u001b" + out
        sb.append(out)
    }
    return sb.toString()
}

@Composable
fun TerminalScreen(
    connected: Boolean,
    serverName: String,
    tabs: List<TerminalTabInfo>,
    activeTab: Int,
    lines: List<String>,
    cursor: Int,
    fontSize: Int,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onSendRaw: (String) -> Unit,
    onClear: () -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit
) {
    val accent = LocalAccent.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var ctrl by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }

    fun showKeyboard() {
        try {
            focus.requestFocus()
        } catch (ignored: Exception) {
        }
        keyboard?.show()
    }

    fun send(raw: String) {
        onSendRaw(applyModifiers(raw, ctrl, alt))
        if (ctrl || alt) {
            ctrl = false
            alt = false
        }
    }

    // open the keyboard as soon as there is a live shell, like a normal terminal app
    LaunchedEffect(connected) {
        if (connected) {
            delay(350)
            showKeyboard()
        }
    }
    LaunchedEffect(lines.size, lines.lastOrNull(), activeTab) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Terminal", onBack, subtitle = serverName.ifBlank { null }) {
            IconButton(onClick = { showKeyboard() }, enabled = connected) {
                Icon(Icons.Rounded.Keyboard, contentDescription = "Show keyboard", tint = if (connected) Nx.Green else Nx.Faint)
            }
            IconButton(
                onClick = { clipboard.getText()?.text?.let { send(it.replace("\r\n", "\n")) } },
                enabled = connected
            ) { Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = if (connected) Nx.Green else Nx.Faint) }
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(lines.joinToString("\n") { it.trimEnd() }.trimEnd())) },
                enabled = lines.isNotEmpty()
            ) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy output", tint = if (lines.isNotEmpty()) Color.White else Nx.Faint) }
            IconButton(onClick = onClear, enabled = connected) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", tint = if (connected) Nx.Green else Nx.Faint)
            }
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

        // ---- tabs
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs, key = { it.id }) { tab ->
                    val sel = tab.id == activeTab
                    Row(
                        Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) LocalAccent.current else Nx.SurfaceHigh)
                            .clickable { onSelectTab(tab.id) }
                            .padding(start = 14.dp, end = if (tabs.size > 1) 4.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tab.title, color = if (sel) Color.White else Nx.Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (tabs.size > 1) {
                            IconButton(onClick = { onCloseTab(tab.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Close, "Close tab", tint = if (sel) Color.White else Nx.Muted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onNewTab, enabled = connected) {
                Icon(Icons.Rounded.Add, contentDescription = "New terminal tab", tint = if (connected) accent else Nx.Faint)
            }
        }

        // ---- output
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Nx.Terminal.copy(alpha = 0.94f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) { detectTapGestures(onTap = { showKeyboard() }) }
        ) {
            val cols = ((maxWidth.value - 24f) / (fontSize * 0.62f)).toInt().coerceIn(20, 300)
            val rows = ((maxHeight.value - 16f) / (fontSize * 1.35f)).toInt().coerceIn(6, 120)
            LaunchedEffect(cols, rows) { onResize(cols, rows) }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                itemsIndexed(lines) { index, line ->
                    if (index == lines.lastIndex && connected) {
                        CursorLine(line, cursor, fontSize)
                    } else {
                        Text(
                            text = line.ifEmpty { " " },
                            color = Color(0xFFE2E8F0),
                            fontFamily = GeistMonoFamily,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.35f).sp
                        )
                    }
                }
            }
            // invisible field that receives every key press and forwards it to the shell
            LiveInput(focus = focus, enabled = connected, onText = { send(it) })
        }

        // ---- extra keys: pinned directly above the soft keyboard
        Row(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .background(Color(0xFF10131A))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton("CTRL", active = ctrl, enabled = connected) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                ctrl = !ctrl
                showKeyboard()
            }
            KeyButton("ALT", active = alt, enabled = connected) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                alt = !alt
                showKeyboard()
            }
            TERM_KEYS.forEach { key ->
                KeyButton(key.label, active = false, enabled = connected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSendRaw(key.bytes)
                    showKeyboard()
                }
            }
        }
    }
}

@Composable
private fun CursorLine(line: String, cursor: Int, fontSize: Int) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val blink by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "blink"
    )
    val padded = if (line.length <= cursor) line.padEnd(cursor + 1) else line
    val text = buildAnnotatedString {
        append(padded)
        addStyle(SpanStyle(background = Color.White.copy(alpha = blink), color = Color.Black), cursor, cursor + 1)
    }
    Text(
        text = text,
        color = Color(0xFFE2E8F0),
        fontFamily = GeistMonoFamily,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.35f).sp
    )
}

/**
 * Zero-size text field used purely as an input sink. The value is reset to a sentinel after every
 * change, so typed characters, backspace and Enter can be forwarded to the shell as they happen.
 */
@Composable
private fun LiveInput(focus: FocusRequester, enabled: Boolean, onText: (String) -> Unit) {
    val sentinel = remember { "\u200B".repeat(16) }
    var value by remember { mutableStateOf(TextFieldValue(sentinel, TextRange(sentinel.length))) }
    BasicTextField(
        value = value,
        onValueChange = { new ->
            val old = value.text
            val t = new.text
            val out = when {
                t == old -> ""
                t.length > old.length && t.startsWith(old) -> t.substring(old.length)
                t.length < old.length && old.startsWith(t) -> "\u007f".repeat(old.length - t.length)
                else -> {
                    val common = t.commonPrefixWith(old).length
                    "\u007f".repeat(old.length - common) + t.substring(common)
                }
            }.replace("\u200B", "")
            if (out.isNotEmpty()) onText(out)
            value = TextFieldValue(sentinel, TextRange(sentinel.length))
        },
        enabled = enabled,
        modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus),
        textStyle = TextStyle(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.None
        ),
        singleLine = false
    )
}

@Composable
private fun KeyButton(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        Modifier
            .heightIn(min = 42.dp)
            .widthIn(min = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) accent else Color(0xFF17251F))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                active -> Color.White
                enabled -> Color.White
                else -> Nx.Faint
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeistMonoFamily
        )
    }
}
