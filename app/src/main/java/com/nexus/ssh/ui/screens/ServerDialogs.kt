package com.nexus.ssh.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.ui.*

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        host: String,
        port: Int,
        user: String,
        authType: AuthType,
        secret: String,
        passphrase: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(AuthType.PASSWORD) }
    var secret by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text == null || text.length > 100_000) {
                    error = "That doesn't look like a private key file"
                } else {
                    secret = text
                    error = null
                }
            } catch (e: Exception) {
                error = "Could not read key file: ${e.message}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add server", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Hostname / IP") },
                        singleLine = true,
                        modifier = Modifier.weight(0.68f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5) },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.32f)
                    )
                }
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = authType == AuthType.PASSWORD,
                        onClick = { authType = AuthType.PASSWORD; secret = "" },
                        label = { Text("Password") }
                    )
                    FilterChip(
                        selected = authType == AuthType.PRIVATE_KEY,
                        onClick = { authType = AuthType.PRIVATE_KEY; secret = "" },
                        label = { Text("Private key") }
                    )
                }
                if (authType == AuthType.PASSWORD) {
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text("Private key (PEM / OpenSSH)") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { keyPicker.launch(arrayOf("*/*")) }) {
                        Text("Import key file…", color = NexusCyan)
                    }
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Key passphrase (if any)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let { Text(it, color = NexusRose, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 0
                    when {
                        host.isBlank() -> { error = "Hostname is required" }
                        p !in 1..65535 -> { error = "Port must be between 1 and 65535" }
                        user.isBlank() -> { error = "Username is required" }
                        secret.isBlank() -> {
                            error = if (authType == AuthType.PASSWORD) "Password is required" else "Private key is required"
                        }
                        else -> onSave(name.trim(), host.trim(), p, user.trim(), authType, secret, passphrase)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NexusMuted) } },
        containerColor = NexusCardNavy
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NexusAmber)
                Spacer(Modifier.width(8.dp))
                Text("Unknown host key", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The authenticity of ${server.hostname}:${server.port} can't be established. " +
                        "Compare this fingerprint with the one on your server (ssh-keygen -lf /etc/ssh/ssh_host_*_key.pub).",
                    fontSize = 13.sp,
                    color = NexusMuted
                )
                Text("Algorithm: $keyAlgorithm", color = NexusCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(
                    fingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(NexusDarkNavy, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
                Text("Trust this key and connect? It will be remembered for future connections.", fontSize = 13.sp, color = NexusMuted)
            }
        },
        confirmButton = {
            Button(onClick = onTrust, colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)) {
                Text("Trust & connect", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = NexusMuted) } },
        containerColor = NexusCardNavy
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSwitcherSheet(
    servers: List<ServerEntity>,
    selectedId: Long?,
    onSelect: (ServerEntity) -> Unit,
    onAdd: () -> Unit,
    onDelete: (ServerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NexusCardNavy) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onAdd) { Text("Add", color = NexusCyan) }
            }
            servers.forEach { srv ->
                val selected = srv.id == selectedId
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) NexusBorderNavy else NexusDarkNavy),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(srv) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(srv.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${srv.username}@${srv.hostname}:${srv.port}",
                                color = NexusMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = NexusCyan)
                        IconButton(onClick = { onDelete(srv) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusMuted)
                        }
                    }
                }
            }
        }
    }
}
