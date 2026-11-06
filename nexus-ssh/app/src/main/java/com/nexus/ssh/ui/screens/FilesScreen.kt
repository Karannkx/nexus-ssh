package com.nexus.ssh.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexus.ssh.FilesState
import com.nexus.ssh.OpenFile
import com.nexus.ssh.core.sftp.RemoteFileItem
import com.nexus.ssh.ui.GridIcon
import com.nexus.ssh.ui.ListIcon
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.BigFileIcon
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.components.BigFolderIcon
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.glass
import com.nexus.ssh.ui.nexusFieldColors
import com.nexus.ssh.ui.serverIconSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(Locale.US, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(
    connected: Boolean,
    serverName: String,
    serverIconKey: String?,
    state: FilesState,
    openFile: OpenFile?,
    transfer: String?,
    showHidden: Boolean,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onOpenItem: (RemoteFileItem) -> Unit,
    onNavigate: (String) -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onMkdir: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onDelete: (RemoteFileItem) -> Unit,
    onUpload: (Uri) -> Unit,
    onDownload: (RemoteFileItem, Uri) -> Unit,
    onSaveFile: (String) -> Unit,
    onCloseFile: () -> Unit,
    onToggleHidden: () -> Unit,
    onAddFavorite: (String) -> Unit
) {
    var gridMode by remember { mutableStateOf(true) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var overflow by remember { mutableStateOf(false) }
    var fabMenu by remember { mutableStateOf(false) }
    var showMkdir by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<RemoteFileItem?>(null) }
    var deleteTarget by remember { mutableStateOf<RemoteFileItem?>(null) }
    var pendingDownload by remember { mutableStateOf<RemoteFileItem?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val accent = LocalAccent.current

    val context = LocalContext.current
    val uploadPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onUpload(uri)
    }
    val downloadPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val item = pendingDownload
        if (uri != null && item != null) onDownload(item, uri)
        pendingDownload = null
    }
    fun launchUpload() {
        try {
            uploadPicker.launch("*/*")
        } catch (e: Exception) {
            Toast.makeText(context, "No file manager found on this device", Toast.LENGTH_LONG).show()
        }
    }
    fun launchDownload(item: RemoteFileItem) {
        pendingDownload = item
        try {
            downloadPicker.launch(item.name)
        } catch (e: Exception) {
            pendingDownload = null
            Toast.makeText(context, "No file manager found on this device", Toast.LENGTH_LONG).show()
        }
    }
    BackHandler(enabled = connected && state.path.isNotEmpty() && state.path != "/") { onUp() }

    val visible = remember(state.entries, query, showHidden) {
        state.entries.filter { (showHidden || !it.name.startsWith(".")) && it.name.contains(query.trim(), ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---- toolbar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp).height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                val spec = serverIconSpec(serverIconKey)
                Icon(spec.icon, contentDescription = null, tint = spec.tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    serverName.ifBlank { "Files" },
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { gridMode = !gridMode }) {
                    Icon(if (gridMode) ListIcon else GridIcon, contentDescription = "Toggle view", tint = Color.White)
                }
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = { overflow = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }

            if (!connected) {
                EmptyState(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Not connected",
                    message = "Connect to a server to browse its files over SFTP.",
                    actionLabel = "Connect",
                    onAction = onConnect
                )
            } else {
                // ---- breadcrumb
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .glass(RoundedCornerShape(20.dp), fill = 0.85f)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onUp, enabled = state.path.isNotEmpty() && state.path != "/", modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Parent folder",
                            tint = Nx.Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val crumbScroll = rememberScrollState()
                    LaunchedEffect(state.path) { crumbScroll.scrollTo(Int.MAX_VALUE) }
                    Row(Modifier.weight(1f).horizontalScroll(crumbScroll), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "/",
                            color = Nx.Muted,
                            fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onNavigate("/") }.padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                        val parts = state.path.split('/').filter { it.isNotEmpty() }
                        var acc = ""
                        parts.forEachIndexed { i, part ->
                            acc += "/$part"
                            val target = acc
                            Icon(Icons.Rounded.ChevronRight, null, tint = Nx.Faint, modifier = Modifier.size(16.dp))
                            Text(
                                part,
                                color = if (i == parts.lastIndex) Color.White else Nx.Muted,
                                fontWeight = if (i == parts.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onNavigate(target) }.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        singleLine = true,
                        placeholder = { Text("Search in this folder") },
                        colors = nexusFieldColors(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = { query = ""; searching = false }) { Icon(Icons.Rounded.Close, "Close search", tint = Nx.Muted) }
                        }
                    )
                }

                LoadingBar(state.loading || transfer != null)
                transfer?.let { Text(it, color = accent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
                state.error?.let { ErrorBox(it, Modifier.padding(16.dp), onRefresh) }

                PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (visible.isEmpty() && !state.loading && state.error == null) {
                        EmptyState(Icons.Rounded.FolderOpen, "Nothing here", if (query.isNotBlank()) "No files match your search." else "This folder is empty.")
                    } else if (gridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(104.dp),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visible, key = { it.path }) { item ->
                                Column(
                                    Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .combinedClickable(onClick = { onOpenItem(item) }, onLongClick = { actionsFor = item })
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (item.isDirectory) BigFolderIcon(Modifier.size(68.dp)) else BigFileIcon(Modifier.size(68.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        item.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                            items(visible, key = { it.path }) { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(onClick = { onOpenItem(item) }, onLongClick = { actionsFor = item })
                                        .padding(start = 20.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.isDirectory) BigFolderIcon(Modifier.size(40.dp)) else BigFileIcon(Modifier.size(40.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            item.permissions + "   " + (if (item.isDirectory) "" else humanSize(item.size) + "   ") + dateFormat.format(Date(item.mtime)),
                                            color = Nx.Muted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    IconButton(onClick = { actionsFor = item }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Actions", tint = Nx.Muted)
                                    }
                                }
                            }
                        }
                    }

                    // footer
                    val files = visible.filter { !it.isDirectory }
                    Text(
                        "${visible.size} item${if (visible.size == 1) "" else "s"} • ${humanSize(files.sumOf { it.size })}",
                        color = Nx.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 26.dp)
                    )

                    // floating add button
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 12.dp)
                            .size(60.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable(onClickLabel = "Add") { fabMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color(0xFF0B0D12), modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
    }

    if (showMkdir) NameDialog("New folder", "Folder name", { showMkdir = false }) { onMkdir(it); showMkdir = false }
    if (showNewFile) NameDialog("New text file", "File name", { showNewFile = false }) { onCreateFile(it); showNewFile = false }

    if (overflow) {
        ActionSheet(
            title = state.path.ifEmpty { "Files" },
            actions = listOf(
                SheetAction("Refresh", Icons.Rounded.Refresh) { onRefresh() },
                SheetAction("Parent folder", Icons.Rounded.ArrowUpward) { onUp() },
                SheetAction(if (showHidden) "Hide hidden files" else "Show hidden files", if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility) { onToggleHidden() },
                SheetAction("Add folder to favorites", Icons.Rounded.Star) { if (state.path.isNotEmpty()) onAddFavorite(state.path) }
            ),
            onDismiss = { overflow = false }
        )
    }
    if (fabMenu) {
        ActionSheet(
            title = "Add to ${state.path.ifEmpty { "folder" }}",
            actions = listOf(
                SheetAction("Upload file", Icons.Rounded.FileUpload) { launchUpload() },
                SheetAction("New folder", Icons.Rounded.CreateNewFolder) { showMkdir = true },
                SheetAction("New text file", Icons.Rounded.NoteAdd) { showNewFile = true }
            ),
            onDismiss = { fabMenu = false }
        )
    }

    actionsFor?.let { item ->
        val actions = buildList {
            add(SheetAction(if (item.isDirectory) "Open folder" else "Open / edit as text", Icons.Rounded.FolderOpen) { onOpenItem(item) })
            if (!item.isDirectory) add(SheetAction("Download", Icons.Rounded.FileDownload) { launchDownload(item) })
            add(SheetAction("Delete", Icons.Rounded.Delete, Nx.Red) { deleteTarget = item })
        }
        ActionSheet(
            title = item.name,
            subtitle = item.permissions + if (item.isDirectory) "" else "  ·  " + humanSize(item.size),
            actions = actions,
            onDismiss = { actionsFor = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = DialogShape,
            containerColor = Nx.SurfaceHigh,
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    if (target.isDirectory) "This folder and everything inside it will be permanently deleted."
                    else "This file will be permanently deleted.",
                    color = Nx.Muted
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(target); deleteTarget = null }) { Text("Delete", color = Nx.Red) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = Nx.Muted) } }
        )
    }

    if (openFile != null) FileEditorDialog(openFile, onSaveFile, onCloseFile)
}

@Composable
fun NameDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = Nx.SurfaceHigh,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(label) },
                colors = nexusFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create", color = LocalAccent.current) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nx.Muted) } }
    )
}

@Composable
private fun FileEditorDialog(file: OpenFile, onSave: (String) -> Unit, onClose: () -> Unit) {
    var text by remember(file.path) { mutableStateOf(file.content) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = Nx.Bg) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        file.path,
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = LocalAccent.current,
                        maxLines = 2
                    )
                    TextButton(onClick = onClose) { Text("Close", color = Nx.Muted) }
                    Button(onClick = { onSave(text) }, colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current)) {
                        Text("Save", color = Color(0xFF04101C), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = nexusFieldColors(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                )
            }
        }
    }
}
