package com.nexus.ssh.ui.screens

import com.nexus.ssh.ui.GeistMonoFamily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.prefs.Snippet
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.nexusFieldColors

@Composable
fun SnippetsScreen(
    snippets: List<Snippet>,
    connected: Boolean,
    onBack: () -> Unit,
    onRun: (String) -> Unit,
    onSave: (id: Long?, title: String, command: String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<Snippet?>(null) }
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Snippet?>(null) }
    val accent = LocalAccent.current

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Snippets", onBack, subtitle = if (connected) "Run in the terminal" else "Connect to run them") {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "New snippet", tint = Nx.Text)
            }
        }
        if (snippets.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Code,
                title = "No snippets yet",
                message = "Save the commands you type often, then run them in the terminal with one tap.",
                actionLabel = "New snippet",
                onAction = { creating = true }
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(snippets, key = { it.id }) { sn ->
                    GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    sn.title.ifBlank { "Untitled" },
                                    color = Nx.Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { menuFor = sn }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = Nx.Muted)
                                }
                            }
                            Text(
                                sn.command,
                                color = Color(0xFFE2E8F0),
                                fontFamily = GeistMonoFamily,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Nx.Terminal)
                                    .padding(10.dp)
                            )
                            Spacer(Modifier.width(1.dp))
                            Row(Modifier.fillMaxWidth().padding(end = 10.dp, top = 10.dp), horizontalArrangement = Arrangement.End) {
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (connected) accent else accent.copy(alpha = 0.3f))
                                        .let { m -> if (connected) m.then(Modifier.clickableNoRipple { onRun(sn.command) }) else m }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Run", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { sn ->
        ActionSheet(
            title = sn.title.ifBlank { "Untitled" },
            actions = listOf(
                SheetAction("Edit", Icons.Rounded.Edit) { editing = sn },
                SheetAction("Delete", Icons.Rounded.Delete, Nx.Red) { onDelete(sn.id) }
            ),
            onDismiss = { menuFor = null }
        )
    }

    if (creating || editing != null) {
        val target = editing
        var title by remember(target?.id) { mutableStateOf(target?.title ?: "") }
        var command by remember(target?.id) { mutableStateOf(target?.command ?: "") }
        val close = { creating = false; editing = null }
        AlertDialog(
            onDismissRequest = close,
            shape = DialogShape,
            containerColor = Nx.SurfaceHigh,
            title = { Text(if (target == null) "New snippet" else "Edit snippet", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it }, label = { Text("Title") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = command, onValueChange = { command = it }, label = { Text("Command") },
                        minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(target?.id, title.ifBlank { command.take(24) }, command)
                        close()
                    },
                    enabled = command.isNotBlank()
                ) { Text("Save", color = accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = close) { Text("Cancel", color = Nx.Muted) } }
        )
    }
}

/** clickable without the ripple so the filled Run button keeps its flat look */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
