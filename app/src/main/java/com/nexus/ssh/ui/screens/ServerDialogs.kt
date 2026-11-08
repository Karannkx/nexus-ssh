package com.nexus.ssh.ui.screens

import com.nexus.ssh.ui.GeistMonoFamily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexus.ssh.AppNotice
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.ui.ALL_APPS
import com.nexus.ssh.ui.AppEntry
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.ServerIconOptions
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.PrimaryButton
import com.nexus.ssh.ui.glass
import com.nexus.ssh.ui.nexusFieldColors
import com.nexus.ssh.ui.serverIconSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Add a server (initial == null) or edit an existing one. */
@Composable
fun ServerEditorDialog(
    initial: ServerEntity?,
    initialIconKey: String,
    onPickKeyFile: ((String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        host: String,
        port: Int,
        user: String,
        authType: AuthType,
        secret: String,
        passphrase: String,
        iconKey: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.hostname ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: 22).toString()) }
    var user by remember { mutableStateOf(initial?.username ?: "") }
    var authType by remember { mutableStateOf(initial?.authType ?: AuthType.PASSWORD) }
    var secret by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    var error by remember { mutableStateOf<String?>(null) }
    val accent = LocalAccent.current
    val editing = initial != null

    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = Nx.SurfaceHigh,
        title = { Text(if (editing) "Edit server" else "Add server", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host, onValueChange = { host = it.trim() }, label = { Text("Hostname / IP") },
                        singleLine = true, modifier = Modifier.weight(0.66f), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port") },
                        singleLine = true, modifier = Modifier.weight(0.34f), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                }
                OutlinedTextField(
                    value = user, onValueChange = { user = it.trim() }, label = { Text("Username") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                )

                Text("Icon", color = Nx.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ServerIconOptions.forEach { opt ->
                        val sel = opt.key == iconKey
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) opt.tint.copy(alpha = 0.22f) else Nx.SurfaceHigh)
                                .border(if (sel) 2.dp else 1.dp, if (sel) opt.tint else Nx.Border, RoundedCornerShape(14.dp))
                                .clickable(onClickLabel = opt.label) { iconKey = opt.key },
                            contentAlignment = Alignment.Center
                        ) { Icon(opt.icon, contentDescription = opt.label, tint = opt.tint, modifier = Modifier.size(24.dp)) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf(AuthType.PASSWORD to "Password", AuthType.PRIVATE_KEY to "Private key").forEach { (type, label) ->
                        val sel = authType == type
                        Text(
                            label,
                            color = if (sel) Color.White else Nx.Muted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) LocalAccent.current else Nx.SurfaceHigh)
                                .clickable { if (authType != type) { authType = type; secret = "" } }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                if (authType == AuthType.PASSWORD) {
                    OutlinedTextField(
                        value = secret, onValueChange = { secret = it },
                        label = { Text(if (editing) "New password (blank = keep)" else "Password") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = secret, onValueChange = { secret = it },
                        label = { Text(if (editing) "New private key (blank = keep)" else "Private key (PEM / OpenSSH)") },
                        minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(),
                        colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            error = null
                            onPickKeyFile { text -> secret = text }
                        }) { Text("Import key file…", color = accent) }
                        TextButton(onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) error = "The clipboard is empty" else { secret = pasted; error = null }
                        }) { Text("Paste", color = accent) }
                    }
                    OutlinedTextField(
                        value = passphrase, onValueChange = { passphrase = it },
                        label = { Text("Key passphrase (if any)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(), shape = RoundedCornerShape(14.dp)
                    )
                }
                error?.let { Text(it, color = Nx.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = port.toIntOrNull() ?: 0
                val needsSecret = !editing || authType != initial?.authType
                when {
                    host.isBlank() -> { error = "Hostname is required" }
                    p !in 1..65535 -> { error = "Port must be between 1 and 65535" }
                    user.isBlank() -> { error = "Username is required" }
                    needsSecret && secret.isBlank() -> {
                        error = if (authType == AuthType.PASSWORD) "Password is required" else "Private key is required"
                    }
                    else -> onSave(name.trim(), host.trim(), p, user.trim(), authType, secret, passphrase, iconKey)
                }
            }) { Text("Save", color = accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nx.Muted) } }
    )
}

@Composable
fun IconPickerDialog(currentKey: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = Nx.SurfaceHigh,
        title = { Text("Choose icon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ServerIconOptions.forEach { opt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (opt.key == currentKey) Nx.SurfaceHigh else Color.Transparent)
                            .clickable { onPick(opt.key) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(opt.icon, contentDescription = null, tint = opt.tint, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(opt.label, color = Nx.Text, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nx.Muted) } }
    )
}

@Composable
fun HostKeyDialog(
    server: ServerEntity,
    keyAlgorithm: String,
    fingerprint: String,
    onTrust: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = DialogShape,
        containerColor = Nx.SurfaceHigh,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = Nx.Amber)
                Spacer(Modifier.width(8.dp))
                Text("Unknown host key", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "The authenticity of ${server.hostname}:${server.port} can't be established. " +
                        "Compare this fingerprint with the one on your server (ssh-keygen -lf /etc/ssh/ssh_host_*_key.pub).",
                    fontSize = 13.sp, color = Nx.Muted
                )
                Text("Algorithm: $keyAlgorithm", color = LocalAccent.current, fontFamily = GeistMonoFamily, fontSize = 12.sp)
                Text(
                    fingerprint,
                    fontFamily = GeistMonoFamily, fontSize = 12.sp, color = Nx.Text,
                    modifier = Modifier.background(Nx.Bg, RoundedCornerShape(10.dp)).padding(10.dp)
                )
                Text("Trust this key and connect? It will be remembered for future connections.", fontSize = 13.sp, color = Nx.Muted)
            }
        },
        confirmButton = { TextButton(onClick = onTrust) { Text("Trust & connect", color = LocalAccent.current, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Nx.Muted) } }
    )
}

/** Command-palette style search over tools and saved servers. */
@Composable
fun SearchDialog(
    servers: List<ServerEntity>,
    icons: Map<Long, String>,
    onTool: (AppEntry) -> Unit,
    onServer: (ServerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = query.trim()
    val tools = ALL_APPS.filter { q.isEmpty() || it.label.contains(q, true) || it.keywords.contains(q, true) }
    val matched = servers.filter { q.isEmpty() || it.name.contains(q, true) || it.hostname.contains(q, true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), fill = 0.96f).padding(16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text("Search tools and servers") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Nx.Muted) },
                colors = nexusFieldColors(), shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.height(320.dp)) {
                if (tools.isNotEmpty()) item { Text("TOOLS", color = Nx.Muted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(6.dp)) }
                items(tools, key = { "t" + it.route }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onTool(t) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Nx.SurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) { t.glyph() }
                        Spacer(Modifier.width(14.dp))
                        Text(t.label, color = Nx.Text, fontSize = 15.sp)
                    }
                }
                if (matched.isNotEmpty()) item { Text("SERVERS", color = Nx.Muted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(6.dp)) }
                items(matched, key = { "s" + it.id }) { s ->
                    val spec = serverIconSpec(icons[s.id])
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onServer(s) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(spec.icon, null, tint = spec.tint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(s.name, color = Nx.Text, fontSize = 15.sp)
                            Text("${s.username}@${s.hostname}", color = Nx.Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private val noticeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
fun NoticesSheet(notices: List<AppNotice>, onClear: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Nx.SurfaceHigh) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", color = Nx.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (notices.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear", color = LocalAccent.current) }
            }
            if (notices.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    EmptyState(Icons.Rounded.Notifications, "All quiet", "Connection events, transfers and errors will show up here.")
                }
            } else {
                LazyColumn(Modifier.height(360.dp)) {
                    items(notices, key = { it.id }) { n ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(noticeFormat.format(Date(n.time)), color = Nx.Muted, fontSize = 12.sp, fontFamily = GeistMonoFamily, modifier = Modifier.width(66.dp))
                            Text(n.text, color = Nx.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "lockPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "fingerprintPulse"
    )
    val alpha by pulse.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "fingerprintAlpha"
    )
    Box(Modifier.fillMaxSize().background(Nx.Bg), contentAlignment = Alignment.Center) {
        com.nexus.ssh.ui.NexusBackdrop(wallpaper = true)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(104.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(Color(0xFFE6F3E8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    tint = Nx.Green.copy(alpha = alpha),
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text("Welcome back", color = Nx.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Unlock Nexus SSH to access your servers", color = Nx.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(30.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Color.White).border(1.dp, Nx.Border, RoundedCornerShape(22.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Secure access", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("Use fingerprint, face unlock or your device PIN", color = Nx.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Unlock Nexus SSH", onUnlock)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Your server credentials remain encrypted on this device", color = Nx.Faint, fontSize = 11.sp)
        }
    }
}
