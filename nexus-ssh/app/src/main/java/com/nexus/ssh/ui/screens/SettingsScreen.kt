package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.BuildConfig
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.prefs.AppPrefs
import com.nexus.ssh.ui.AccentOptions
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.accentColor
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.GlassCard
import com.nexus.ssh.ui.components.SectionLabel
import com.nexus.ssh.ui.nexusFieldColors
import com.nexus.ssh.ui.serverIconSpec

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        SectionLabel(title, Modifier.padding(start = 6.dp, bottom = 8.dp))
        GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Nx.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Nx.Muted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
fun SettingsScreen(
    prefs: AppPrefs,
    servers: List<ServerEntity>,
    icons: Map<Long, String>,
    onAddServer: () -> Unit,
    onEditServer: (ServerEntity) -> Unit,
    onDeleteServer: (ServerEntity) -> Unit,
    onClearHostKeys: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val name by prefs.displayName.collectAsState()
    val wallpaper by prefs.showWallpaper.collectAsState()
    val accentKey by prefs.accent.collectAsState()
    val fontSize by prefs.terminalFontSize.collectAsState()
    val refresh by prefs.refreshSeconds.collectAsState()
    val appLock by prefs.appLock.collectAsState()
    val secure by prefs.secureScreen.collectAsState()
    val accent = LocalAccent.current
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 22.dp))

        SettingsSection("Profile") {
            OutlinedTextField(
                value = name,
                onValueChange = { prefs.setDisplayName(it.take(24)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Display name") },
                supportingText = { Text("Shown in the greeting on the home screen", color = Nx.Muted) },
                shape = RoundedCornerShape(14.dp),
                colors = nexusFieldColors()
            )
        }

        SettingsSection("Appearance") {
            ToggleRow("Wallpaper", "Show the flowing wave wallpaper on Home, Servers and Apps", wallpaper) { prefs.setShowWallpaper(it) }
            Spacer(Modifier.height(8.dp))
            Text("Accent colour", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentOptions.forEach { key ->
                    val c = accentColor(key)
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (key == accentKey) 3.dp else 1.dp, if (key == accentKey) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable(onClickLabel = key) { prefs.setAccent(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == accentKey) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Terminal font size: $fontSize sp", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { prefs.setTerminalFontSize(it.toInt()) },
                valueRange = 10f..18f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color.White.copy(alpha = 0.12f))
            )
            Text("Monitor refresh interval", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 30).forEach { s ->
                    Text(
                        "${s}s",
                        color = if (refresh == s) Color(0xFF0B0D12) else Nx.Muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (refresh == s) Color.White else Color.White.copy(alpha = 0.08f))
                            .clickable { prefs.setRefreshSeconds(s) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        SettingsSection("Servers") {
            if (servers.isEmpty()) {
                Text("No servers saved yet.", color = Nx.Muted, fontSize = 14.sp)
            }
            servers.forEach { s ->
                val spec = serverIconSpec(icons[s.id])
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(spec.icon, contentDescription = null, tint = spec.tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("${s.username}@${s.hostname}:${s.port}", color = Nx.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    IconButton(onClick = { onEditServer(s) }) { Icon(Icons.Rounded.Edit, "Edit", tint = Nx.Muted) }
                    IconButton(onClick = { onDeleteServer(s) }) { Icon(Icons.Rounded.Delete, "Delete", tint = Nx.Red) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .clickable(onClick = onAddServer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add server", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        SettingsSection("Security") {
            ToggleRow("App lock", "Require fingerprint, face or device PIN when opening the app", appLock, onToggleAppLock)
            ToggleRow("Protect screen", "Block screenshots and hide content in the recent apps list", secure) { prefs.setSecureScreen(it) }
            Spacer(Modifier.height(6.dp))
            Text(
                "Passwords, private keys and passphrases are encrypted with a hardware-backed Android Keystore key (AES-256-GCM).",
                color = Nx.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Forget all trusted host keys",
                color = Nx.Red,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { confirmClear = true }
                    .padding(vertical = 8.dp)
            )
        }

        SettingsSection("About") {
            AboutRow("Version", BuildConfig.VERSION_NAME)
            AboutRow("License", "MIT")
            AboutRow("Built with", "Kotlin · Jetpack Compose · SSHJ · Room")
            AboutRow("Typeface", "Inter (SIL Open Font License)")
            Spacer(Modifier.height(8.dp))
            Text(
                "Source code on GitHub",
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenUrl("https://github.com/Karannkx/nexus-ssh") }
                    .padding(vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = DialogShape,
            containerColor = Nx.SurfaceHigh,
            title = { Text("Forget trusted host keys?") },
            text = { Text("You will be asked to verify each server's fingerprint again on the next connection.", color = Nx.Muted) },
            confirmButton = { TextButton(onClick = { onClearHostKeys(); confirmClear = false }) { Text("Forget", color = Nx.Red) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Nx.Muted) } }
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Nx.Muted, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text(value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
    }
}
