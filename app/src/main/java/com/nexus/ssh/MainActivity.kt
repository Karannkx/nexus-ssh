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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

val NexusDarkNavy = Color(0xFF0B0F19)
val NexusCardNavy = Color(0xFF111827)
val NexusBorderNavy = Color(0xFF1F2937)
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

@AndroidEntryPoint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isConnected by remember { mutableStateOf(true) }

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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) NexusEmerald else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Production Cluster 01",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "nexus-admin@192.168.1.120:22 · Ubuntu 24.04",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { isConnected = !isConnected },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isConnected) NexusRose.copy(alpha = 0.15f) else NexusEmerald.copy(alpha = 0.15f),
                            contentColor = if (isConnected) NexusRose else NexusEmerald
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isConnected) "Disconnect" else "Connect",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexusCardNavy,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NexusCardNavy,
                tonalElevation = 8.dp
            ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> NativeDashboardScreen(isConnected)
                1 -> NativeTerminalScreen(isConnected)
                2 -> NativeFilesScreen()
                3 -> NativeDockerScreen()
                4 -> NativeServicesScreen()
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun NativeDashboardScreen(isConnected: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NexusCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CPU Usage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("24.5%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("RAM Usage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("8.4 / 32 GB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.245f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = NexusCyan,
                        trackColor = NexusBorderNavy,
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STORAGE & NETWORK",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NexusEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Root Disk (/)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("142 / 490 GB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Network RX / TX", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("420 KB/s · 840 KB/s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.31f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = NexusEmerald,
                        trackColor = NexusBorderNavy,
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HOST SPECIFICATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NexusAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Kernel", "Linux 6.8.0-45-generic x86_64")
                    InfoRow("Uptime", "18 days, 4 hours, 22 mins")
                    InfoRow("Load Average", "0.42, 0.68, 0.55")
                    InfoRow("SSH Cipher", "chacha20-poly1305 (ED25519)")
                    InfoRow("Public IP", "54.214.88.92")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.White)
    }
}

@Composable
fun NativeTerminalScreen(isConnected: Boolean) {
    var commandInput by remember { mutableStateOf("") }
    val lines = remember {
        mutableStateListOf(
            "Connected to Production Cluster 01 (192.168.1.120:22)",
            "SSH-2.0-OpenSSH_9.6p1 · Public key authorized (ED25519)",
            "Welcome to Ubuntu 24.04 LTS (GNU/Linux 6.8.0-45-generic x86_64)",
            "nexus-admin@prod-01:~$ uname -a",
            "Linux prod-01 6.8.0-45-generic #45-Ubuntu SMP x86_64 GNU/Linux",
            "nexus-admin@prod-01:~$ docker ps --format 'table {{.Names}}\\t{{.Status}}'",
            "NAMES                STATUS",
            "nginx-ingress        Up 18 days (healthy)",
            "postgres-db-primary  Up 18 days",
            "redis-cache-cluster  Up 18 days"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (line.startsWith("nexus-admin@")) NexusCyan else Color(0xFFE2E8F0)
                )
            }
        }

        val quickKeys = listOf("ESC", "TAB", "CTRL", "ALT", "|", "-", "/", "~", "^C", "clear")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickKeys.forEach { key ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NexusCardNavy,
                    modifier = Modifier.clickable {
                        if (key == "clear") {
                            lines.clear()
                        } else if (key == "^C") {
                            lines.add("nexus-admin@prod-01:~$ ^C")
                        } else {
                            commandInput += key
                        }
                    }
                ) {
                    Text(
                        text = key,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter shell command...", fontSize = 13.sp, color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusBorderNavy,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        val cmd = commandInput.trim()
                        lines.add("nexus-admin@prod-01:~$ $cmd")
                        if (cmd == "uptime") {
                            lines.add(" 07:15:00 up 18 days, 4:22, 2 users, load average: 0.42, 0.68, 0.55")
                        } else if (cmd == "whoami") {
                            lines.add("nexus-admin")
                        } else if (cmd == "date") {
                            lines.add(java.util.Date().toString())
                        } else {
                            lines.add("bash: $cmd: executed successfully on remote server")
                        }
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .background(NexusCyan, RoundedCornerShape(12.dp))
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun NativeFilesScreen() {
    val files = listOf(
        FileInfo("bin", true, "4.0 KB", "drwxr-xr-x", "root"),
        FileInfo("etc", true, "4.0 KB", "drwxr-xr-x", "root"),
        FileInfo("home", true, "4.0 KB", "drwxr-xr-x", "root"),
        FileInfo("opt", true, "4.0 KB", "drwxr-xr-x", "root"),
        FileInfo("var", true, "4.0 KB", "drwxr-xr-x", "root"),
        FileInfo("docker-compose.yml", false, "820 B", "-rw-r--r--", "nexus-admin"),
        FileInfo(".bashrc", false, "3.7 KB", "-rw-r--r--", "nexus-admin")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (file.isDir) NexusCyan else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("${file.permissions} · ${file.owner}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    }
                    Text(file.size, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

data class FileInfo(val name: String, val isDir: Boolean, val size: String, val permissions: String, val owner: String)

@Composable
fun NativeDockerScreen() {
    val containers = listOf(
        DockerInfo("nginx-ingress", "nginx:1.27-alpine", "Up 18 days", "80:80, 443:443", true),
        DockerInfo("postgres-db-primary", "postgres:16-alpine", "Up 18 days", "5432:5432", true),
        DockerInfo("redis-cache-cluster", "redis:7.2-alpine", "Up 18 days", "6379:6379", true),
        DockerInfo("node-backend-api", "node-backend:latest", "Up 4 days", "3000:3000", true)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(containers) { container ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(container.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NexusEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = container.status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = NexusEmerald
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(container.image, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("Ports: ${container.ports}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = NexusCyan)
                }
            }
        }
    }
}

data class DockerInfo(val name: String, val image: String, val status: String, val ports: String, val isRunning: Boolean)

@Composable
fun NativeServicesScreen() {
    val services = listOf(
        ServiceInfo("sshd.service", "OpenSSH Server Daemon", "running"),
        ServiceInfo("docker.service", "Docker Container Engine", "running"),
        ServiceInfo("nginx.service", "High-performance HTTP Server", "running"),
        ServiceInfo("ufw.service", "Uncomplicated Firewall", "active"),
        ServiceInfo("fail2ban.service", "SSH Protection System", "running")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(services) { service ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusCardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorderNavy, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(service.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(service.desc, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NexusEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = service.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = NexusEmerald
                        )
                    }
                }
            }
        }
    }
}

data class ServiceInfo(val name: String, val desc: String, val status: String)
