package com.nexus.ssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.ui.*
import com.nexus.ssh.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                NexusApp()
            }
        }
    }
}

private data class NavTab(val label: String, val icon: ImageVector)

private val NAV_TABS = listOf(
    NavTab("Dashboard", Icons.Default.Dashboard),
    NavTab("Terminal", Icons.Default.Terminal),
    NavTab("Files", Icons.Default.Folder),
    NavTab("Docker", Icons.Default.Layers),
    NavTab("Services", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(vm: NexusViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val loaded by vm.serversLoaded.collectAsState()
    val selected by vm.selectedServer.collectAsState()
    val connection by vm.connectionState.collectAsState()
    val telemetry by vm.telemetry.collectAsState()
    val terminalLines by vm.terminalLines.collectAsState()
    val files by vm.files.collectAsState()
    val openFile by vm.openFile.collectAsState()
    val docker by vm.docker.collectAsState()
    val services by vm.services.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbarHost.showSnackbar(it) } }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showSwitcher by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServerEntity?>(null) }

    val isConnected = connection is ConnectionState.Connected
    val isConnecting = connection is ConnectionState.Connecting

    LaunchedEffect(selectedTab, isConnected) {
        vm.setDashboardVisible(selectedTab == 0 && isConnected)
        if (isConnected) {
            when (selectedTab) {
                3 -> vm.refreshDocker()
                4 -> vm.refreshServices()
                else -> {}
            }
        }
    }

    val server = selected

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = NexusDarkNavy,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = server != null) { showSwitcher = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        isConnected -> NexusEmerald
                                        isConnecting -> NexusAmber
                                        else -> Color.Gray
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = server?.name ?: "Nexus SSH",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (server != null) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch server",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (server != null) {
                                Text(
                                    text = "${server.username}@${server.hostname}:${server.port}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = NexusMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add server", tint = NexusCyan)
                    }
                    if (server != null) {
                        Button(
                            onClick = { if (isConnected) vm.disconnect() else vm.connect() },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) NexusRose.copy(alpha = 0.2f) else NexusEmerald.copy(alpha = 0.2f),
                                contentColor = if (isConnected) NexusRose else NexusEmerald,
                                disabledContainerColor = NexusAmber.copy(alpha = 0.2f),
                                disabledContentColor = NexusAmber
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NexusAmber,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Connecting…", fontSize = 12.sp)
                            } else {
                                Text(
                                    text = if (isConnected) "Disconnect" else "Connect",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexusCardNavy,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (server != null) {
                NavigationBar(containerColor = NexusCardNavy, tonalElevation = 8.dp) {
                    NAV_TABS.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NexusCyan,
                                selectedTextColor = NexusCyan,
                                indicatorColor = NexusCyan.copy(alpha = 0.15f),
                                unselectedIconColor = NexusMuted,
                                unselectedTextColor = NexusMuted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (server == null) {
                if (loaded) {
                    EmptyServers(onAdd = { showAdd = true })
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NexusCyan)
                }
            } else {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        server = server,
                        connection = connection,
                        telemetry = telemetry,
                        onConnect = vm::connect,
                        onRefresh = vm::refreshTelemetry
                    )
                    1 -> TerminalScreen(
                        connected = isConnected,
                        lines = terminalLines,
                        onSendLine = { vm.sendTerminalInput(it + "\r") },
                        onSendRaw = vm::sendTerminalInput,
                        onClear = vm::clearTerminal,
                        onResize = vm::resizeTerminal
                    )
                    2 -> FilesScreen(
                        connected = isConnected,
                        state = files,
                        openFile = openFile,
                        onOpenItem = vm::filesOpen,
                        onUp = vm::filesUp,
                        onRefresh = vm::filesRefresh,
                        onMkdir = vm::filesMkdir,
                        onDelete = vm::filesDelete,
                        onSaveFile = vm::saveOpenFile,
                        onCloseFile = vm::closeOpenFile
                    )
                    3 -> DockerScreen(
                        connected = isConnected,
                        state = docker,
                        onRefresh = vm::refreshDocker,
                        onAction = vm::dockerAction
                    )
                    4 -> ServicesScreen(
                        connected = isConnected,
                        state = services,
                        onRefresh = vm::refreshServices,
                        onAction = vm::serviceAction
                    )
                    else -> {}
                }
            }
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onSave = { name, host, port, user, authType, secret, passphrase ->
                vm.addServer(name, host, port, user, authType, secret, passphrase)
                showAdd = false
            }
        )
    }

    val state = connection
    if (state is ConnectionState.HostKeyConfirmationRequired) {
        HostKeyDialog(
            server = state.server,
            keyAlgorithm = state.keyAlgorithm,
            fingerprint = state.fingerprint,
            onTrust = vm::trustHostKey,
            onCancel = vm::dismissHostKeyPrompt
        )
    }

    if (showSwitcher) {
        ServerSwitcherSheet(
            servers = servers,
            selectedId = server?.id,
            onSelect = {
                vm.selectServer(it.id)
                showSwitcher = false
            },
            onAdd = {
                showSwitcher = false
                showAdd = true
            },
            onDelete = { deleteTarget = it },
            onDismiss = { showSwitcher = false }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("The saved credentials and trusted host key for this server will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteServer(target)
                    deleteTarget = null
                    showSwitcher = false
                }) { Text("Delete", color = NexusRose) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            containerColor = NexusCardNavy
        )
    }
}

@Composable
private fun EmptyServers(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Terminal, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("No servers yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Add a Linux server to connect over SSH.", color = NexusMuted)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = NexusCyan)) {
            Text("Add server", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
