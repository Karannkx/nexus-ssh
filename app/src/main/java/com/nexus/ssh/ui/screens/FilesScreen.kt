package com.nexus.ssh.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexus.ssh.FilesState
import com.nexus.ssh.OpenFile
import com.nexus.ssh.core.sftp.RemoteFileItem
import com.nexus.ssh.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(Locale.US, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

@Composable
fun FilesScreen(
    connected: Boolean,
    state: FilesState,
    openFile: OpenFile?,
    onOpenItem: (RemoteFileItem) -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onMkdir: (String) -> Unit,
    onDelete: (RemoteFileItem) -> Unit,
    onSaveFile: (String) -> Unit,
    onCloseFile: () -> Unit
) {
    var showMkdir by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RemoteFileItem?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    if (!connected) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Connect to a server to browse files over SFTP.", color = NexusMuted)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUp, enabled = state.path.isNotEmpty() && state.path != "/") {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Parent folder", tint = NexusCyan)
            }
            Text(
                text = state.path.ifEmpty { "…" },
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = NexusCyan,
                maxLines = 1
            )
            IconButton(onClick = { showMkdir = true }, enabled = state.path.isNotEmpty()) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder", tint = NexusCyan)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan)
            }
        }
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NexusCyan)

        state.error?.let {
            Text(it, color = NexusRose, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.entries, key = { it.path }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenItem(item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (item.isDirectory) NexusAmber else NexusMuted
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(
                            text = item.permissions + "  " +
                                (if (item.isDirectory) "" else humanSize(item.size) + "  ") +
                                dateFormat.format(Date(item.mtime)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NexusMuted
                        )
                    }
                    IconButton(onClick = { deleteTarget = item }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusMuted)
                    }
                }
            }
        }
    }

    if (showMkdir) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMkdir = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Folder name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onMkdir(name)
                    showMkdir = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showMkdir = false }) { Text("Cancel") } },
            containerColor = NexusCardNavy
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    if (target.isDirectory) "This folder and everything inside it will be permanently deleted."
                    else "This file will be permanently deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text("Delete", color = NexusRose) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            containerColor = NexusCardNavy
        )
    }

    if (openFile != null) {
        FileEditorDialog(openFile, onSaveFile, onCloseFile)
    }
}

@Composable
private fun FileEditorDialog(file: OpenFile, onSave: (String) -> Unit, onClose: () -> Unit) {
    var text by remember(file.path) { mutableStateOf(file.content) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = NexusDarkNavy) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.path,
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NexusCyan,
                        maxLines = 2
                    )
                    TextButton(onClick = onClose) { Text("Close", color = NexusMuted) }
                    Button(
                        onClick = { onSave(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)
                    ) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                )
            }
        }
    }
}
