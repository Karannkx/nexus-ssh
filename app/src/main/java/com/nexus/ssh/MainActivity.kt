package com.nexus.ssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.KeyType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.security.KeystoreManager
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.SshConnectionManager
import kotlinx.coroutines.launch

val NexusDarkNavy = Color(0xFF070A12)
val NexusCardNavy = Color(0xFF0F172A)
val NexusBorderNavy = Color(0xFF1E293B)
val NexusCyan = Color(0xFF38BDF8)
val NexusEmerald = Color(0xFF10B981)
val NexusAmber = Color(0xFFF59E0B)
val NexusRose = Color(0xFFF43F5E)

private val DarkColorScheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color.Black,
    secondary = NexusEmerald,
    onSecondary = Color.Black,
    background = NexusDarkNavy,
    surface = NexusCardNavy,
    surfaceVariant = NexusBorderNavy,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                NexusRealApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusRealApp() {
    val coroutineScope = rememberCoroutineScope()
    val keystoreManager = remember { KeystoreManager() }
    val sshManager = remember { SshConnectionManager(keystoreManager) }

    val servers = remember {
        mutableStateListOf(
            ServerEntity(
                id = 1,
                name = "My Cloud VPS",
                hostname = "192.168.1.100",
                port = 22,
                username = "root",
                authType = AuthType.PASSWORD,
                encryptedSecret = keystoreManager.encrypt("password123"),
                keyType = KeyType.AUTO
            )
        )
    }

    var selectedServerIndex by remember { mutableIntStateOf(0) }
    val currentServer = servers.getOrNull(selectedServerIndex) ?: servers.first()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    var showAddServerDialog by remember { mutableStateOf(false) }
    var showServerSwitcherSheet by remember { mutableStateOf(false) }
    var pendingHostKeyConfirmation by remember { mutableStateOf<Pair<String, String>?>(null) }

    var remoteKernel by remember { mutableStateOf("Linux 6.8.0-generic x86_64") }
    var remoteUptime by remember { mutableStateOf("Ready to connect") }
    var remoteCpuUsage by remember { mutableStateOf(12.5f) }
    var remoteMemUsage by remember { mutableStateOf("3.8 / 16.0 GB") }
    var remoteDiskUsage by remember { mutableStateOf("32 / 120 GB") }

    val terminalLines = remember {
        mutableStateListOf(
            "NEXUS SSH Engine v1.0.1 (Native Android)",
            "Ready. Tap '+' to add your real server, then tap 'Connect'."
        )
    }

    fun connectToServer(bypassHostCheck: Boolean = false) {
        coroutineScope.launch {
            isConnecting = true
            terminalLines.add("Connecting to ${currentServer.username}@${currentServer.hostname}:${currentServer.port}...")

            val result = sshManager.connect(currentServer, bypassHostCheck = bypassHostCheck)
            isConnecting = false

            result.onSuccess {
                isConnected = true
                terminalLines.add("✓ Authenticated via ${currentServer.authType.name}")
                terminalLines.add("✓ SSH Session established to ${currentServer.hostname}")

                sshManager.executeCommand("uname -a").onSuccess {
                    remoteKernel = it.take(45)
                    terminalLines.add(it)
                }
                sshManager.executeCommand("uptime").onSuccess {
                    remoteUptime = it.trim()
                }
                sshManager.executeCommand("free -m | awk 'NR==2{printf \"%.1f / %.1f GB\", \$3/1024, \$2/1024}'").onSuccess {
                    remoteMemUsage = it.trim()
                }
                sshManager.executeCommand("df -h / | awk 'NR==2{print \$3 \" / \" \$2}'").onSuccess {
                    remoteDiskUsage = it.trim()
                }
            }.onFailure { error ->
                isConnected = false
                if (error is SecurityException && error.message?.contains("Host key") == true) {
                    val state = sshManager.connectionState.value
                    if (state is ConnectionState.HostKeyConfirmationRequired) {
                        pendingHostKeyConfirmation = Pair(state.keyAlgorithm, state.fingerprint)
                    } else {
                        pendingHostKeyConfirmation = Pair("ED25519", "SHA256:${currentServer.hostname.hashCode()}")
                    }
                } else {
                    terminalLines.add("✗ Error: ${error.localizedMessage ?: "Connection error"}")
                }
            }
        }
    }

    fun disconnectFromServer() {
        coroutineScope.launch {
            sshManager.disconnect()
            isConnected = false
            terminalLines.add("Disconnected from ${currentServer.hostname}.")
        }
    }

    val navItems = listOf(
        NavItem("Dashboard", Icons.Default.Dashboard),
        NavItem("Terminal", Icons.Default.Terminal),
        NavItem("Files", Icons.Default.Folder),
        NavItem("Docker", Icons.Default.Layers),
        NavItem("Services", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showServerSwitcherSheet = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isConnected) NexusEmerald else if (isConnecting) NexusAmber else Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentServer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Server",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "${currentServer.username}@${currentServer.hostname}:${currentServer.port}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddServerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Server", tint = NexusCyan)
                    }

                    Button(
                        onClick = {
                            if (isConnected) disconnectFromServer() else connectToServer(bypassHostCheck = false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) NexusRose.copy(alpha = 0.2f) else NexusEmerald.copy(alpha = 0.2f),
                            contentColor = if (isConnected) NexusRose else NexusEmerald
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NexusAmber, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connecting...", fontSize = 12.sp)
                        } else {
                            Text(if (isConnected) "Disconnect" else "Connect", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusCardNavy, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = NexusCardNavy, tonalElevation = 8.dp) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NexusCyan,
                            selectedTextColor = NexusCyan,
                            indicatorColor = NexusCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        },
        containerColor = NexusDarkNavy
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                0 -> RealDashboardScreen(
                    isConnected = isConnected,
                    server = currentServer,
                    kernel = remoteKernel,
                    uptime = remoteUptime,
                    cpuUsage = remoteCpuUsage,
                    memUsage = remoteMemUsage,
                    diskUsage = remoteDiskUsage,
                    onConnectClick = { connectToServer() }
                )
                1 -> RealTerminalScreen(
                    lines = terminalLines,
                    isConnected = isConnected,
                    onExecuteCommand = { cmd ->
                        terminalLines.add("${currentServer.username}@${currentServer.hostname}:~$ $cmd")
                        if (isConnected) {
                            coroutineScope.launch {
                                val res = sshManager.executeCommand(cmd)
                                res.onSuccess { output ->
                                    if (output.isNotBlank()) terminalLines.add(output)
                                }.onFailure { err ->
                                    terminalLines.add("Error: ${err.localizedMessage}")
                                }
                            }
                        } else {
                            terminalLines.add("Not connected. Tap 'Connect' in the top bar to run real commands.")
                        }
                    }
                )
                2 -> RealFilesScreen(
                    isConnected = isConnected,
                    onRefresh = {
                        if (isConnected) {
                            coroutineScope.launch {
                                sshManager.executeCommand("ls -la /").onSuccess { terminalLines.add(it) }
                            }
                        }
                    }
                )
                3 -> RealDockerScreen(
                    isConnected = isConnected,
                    onRunDockerPs = {
                        if (isConnected) {
                            coroutineScope.launch {
                                sshManager.executeCommand("docker ps").onSuccess { terminalLines.add(it) }
                            }
                        }
                    }
                )
                4 -> RealServicesScreen(
                    isConnected = isConnected,
                    onRestartSshd = {
                        if (isConnected) {
                            coroutineScope.launch {
                                sshManager.executeCommand("sudo systemctl status sshd")
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onSave = { newServer ->
                servers.add(newServer)
                selectedServerIndex = servers.lastIndex
                showAddServerDialog = false
            },
            keystoreManager = keystoreManager
        )
    }

    if (pendingHostKeyConfirmation != null) {
        val (keyAlgo, fingerprint) = pendingHostKeyConfirmation!!
        AlertDialog(
            onDismissRequest = { pendingHostKeyConfirmation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NexusAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unknown SSH Host Key", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Authenticity of host '${currentServer.hostname}:${currentServer.port}' cannot be verified automatically.", color = Color.LightGray, fontSize = 13.sp)
                    Text("Key Algorithm: $keyAlgo", color = NexusCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Fingerprint: $fingerprint", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.background(NexusDarkNavy, RoundedCornerShape(6.dp)).padding(8.dp))
                    Text("Trust this server and continue connecting?", color = Color.LightGray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { pendingHostKeyConfirmation = null; connectToServer(bypassHostCheck = true) }, colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)) {
                    Text("Trust & Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHostKeyConfirmation = null }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = NexusCardNavy
        )
    }

    if (showServerSwitcherSheet) {
        ModalBottomSheet(onDismissRequest = { showServerSwitcherSheet = false }, containerColor = NexusCardNavy) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Saved Linux Servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = { showServerSwitcherSheet = false; showAddServerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = NexusCyan)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                servers.forEachIndexed { idx, srv ->
                    val isSelected = idx == selectedServerIndex
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) NexusBorderNavy else NexusDarkNavy),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedServerIndex = idx; showServerSwitcherSheet = false }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(srv.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${srv.username}@${srv.hostname}:${srv.port}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = NexusCyan)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AddServerDialog(onDismiss: () -> Unit, onSave: (ServerEntity) -> Unit, keystoreManager: KeystoreManager) {
    var name by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("root") }
    var authType by remember { mutableStateOf(AuthType.PASSWORD) }
    var secretInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Linux Server", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Name") }, placeholder = { Text("e.g. My Ubuntu VPS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hostname, onValueChange = { hostname = it }, label = { Text("Hostname / IP") }, placeholder = { Text("192.168.1.50") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, singleLine = true, modifier = Modifier.weight(0.3f))
                }
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = authType == AuthType.PASSWORD, onClick = { authType = AuthType.PASSWORD }, label = { Text("Password") })
                    FilterChip(selected = authType == AuthType.PRIVATE_KEY, onClick = { authType = AuthType.PRIVATE_KEY }, label = { Text("Private Key") })
                }
                OutlinedTextField(
                    value = secretInput,
                    onValueChange = { secretInput = it },
                    label = { Text(if (authType == AuthType.PASSWORD) "SSH Password" else "Private Key (OpenSSH format)") },
                    visualTransformation = if (authType == AuthType.PASSWORD) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (authType == AuthType.PASSWORD) 1 else 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && hostname.isNotBlank()) {
                        val encryptedSecret = keystoreManager.encrypt(secretInput)
                        val server = ServerEntity(
                            id = System.currentTimeMillis(),
                            name = name.trim(),
                            hostname = hostname.trim(),
                            port = port.toIntOrNull() ?: 22,
                            username = username.trim(),
                            authType = authType,
                            encryptedSecret = encryptedSecret,
                            keyType = KeyType.AUTO
                        )
                        onSave(server)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)
            ) {
                Text("Save Server", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } },
        containerColor = NexusCardNavy
    )
}

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun RealDashboardScreen(isConnected: Boolean, server: ServerEntity, kernel: String, uptime: String, cpuUsage: Float, memUsage: String, diskUsage: String, onConnectClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "SERVER STATUS", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = if (isConnected) NexusEmerald else NexusRose, fontWeight = FontWeight.Bold)
                        if (!isConnected) TextButton(onClick = onConnectClick) { Text("Connect Now", color = NexusCyan, fontSize = 12.sp) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isConnected) "Active SSH Session to ${server.hostname}" else "Disconnected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Uptime: $uptime", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "HARDWARE RESOURCES", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = NexusCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("CPU Usage", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text("${cpuUsage}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
                        Column { Text("RAM Usage", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(memUsage, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { cpuUsage / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = NexusCyan, trackColor = NexusBorderNavy)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "STORAGE & HOST", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = NexusEmerald, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowItem("Root Disk (/)", diskUsage)
                    InfoRowItem("Kernel", kernel)
                    InfoRowItem("Hostname", server.hostname)
                    InfoRowItem("Port", server.port.toString())
                    InfoRowItem("User", server.username)
                    InfoRowItem("Auth Type", server.authType.name)
                }
            }
        }
    }
}

@Composable
fun InfoRowItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.White)
    }
}

@Composable
fun RealTerminalScreen(lines: List<String>, isConnected: Boolean, onExecuteCommand: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF030712)).padding(12.dp)) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lines) { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = if (line.contains("Error") || line.contains("✗")) NexusRose else if (line.contains("✓")) NexusEmerald else Color(0xFFE2E8F0))
            }
        }
        val quickKeys = listOf("uname -a", "uptime", "free -m", "df -h", "docker ps", "clear")
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickKeys.forEach { key ->
                Surface(shape = RoundedCornerShape(6.dp), color = NexusCardNavy, modifier = Modifier.clickable { onExecuteCommand(key) }) {
                    Text(text = key, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = NexusCyan)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Command to run on server...", fontSize = 13.sp, color = Color.Gray) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexusCyan, unfocusedBorderColor = NexusBorderNavy, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { if (input.isNotBlank()) { onExecuteCommand(input.trim()); input = "" } }, modifier = Modifier.background(NexusCyan, RoundedCornerShape(12.dp)).size(48.dp)) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun RealFilesScreen(isConnected: Boolean, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SFTP Remote File Explorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), modifier = Modifier.fillMaxWidth()) {
            Text(if (isConnected) "Directory: / (Tap Refresh icon to list real remote files)" else "Connect to your server in the top bar to browse files over SFTP.", color = Color.LightGray, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
        }
    }
}

@Composable
fun RealDockerScreen(isConnected: Boolean, onRunDockerPs: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Docker Containers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRunDockerPs) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), modifier = Modifier.fillMaxWidth()) {
            Text(if (isConnected) "Docker CLI active. Tap Refresh to run 'docker ps' over SSH." else "Connect to remote host to manage Docker containers.", color = Color.LightGray, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
        }
    }
}

@Composable
fun RealServicesScreen(isConnected: Boolean, onRestartSshd: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Systemd Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRestartSshd) { Icon(Icons.Default.Refresh, contentDescription = "Check", tint = NexusCyan) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = NexusCardNavy), modifier = Modifier.fillMaxWidth()) {
            Text(if (isConnected) "Systemd service controller active." else "Connect to inspect systemd unit services.", color = Color.LightGray, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
        }
    }
}
