package com.nexus.ssh.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.AppInfo
import com.nexus.ssh.BuildConfig
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.prefs.AppPrefs
import com.nexus.ssh.ui.AccentOptions
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.accentColor
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.GroupSurface
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.SectionHeader
import com.nexus.ssh.ui.nexusFieldColors
import com.nexus.ssh.ui.serverIconSpec

@Composable
private fun SwitchRow(icon: ImageVector, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    ListRow(
        title = title,
        subtitle = subtitle,
        leading = { IconBadge(icon, tint) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Nx.Muted,
                    uncheckedTrackColor = Nx.Border.copy(alpha = 0.8f),
                    uncheckedBorderColor = Nx.Border
                )
            )
        },
        onClick = { onChange(!checked) }
    )
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
    onOpenAbout: () -> Unit
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
        Text("Settings", color = Nx.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 22.dp))

        SectionHeader("Profile")
        GroupSurface {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Person, Nx.Blue)
                Spacer(Modifier.size(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { prefs.setDisplayName(it.take(24)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Display name") },
                    shape = RoundedCornerShape(14.dp),
                    colors = nexusFieldColors()
                )
            }
        }

        SectionHeader("Appearance")
        GroupSurface {
            SwitchRow(Icons.Rounded.Wallpaper, Nx.Purple, "Wallpaper", "Wave background on Home, Servers and Apps", wallpaper) { prefs.setShowWallpaper(it) }
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Palette, Nx.Magenta)
                Spacer(Modifier.size(14.dp))
                Text("Accent colour", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentOptions.forEach { key ->
                        val c = accentColor(key)
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (key == accentKey) 2.5.dp else 0.dp, Nx.Green, CircleShape)
                                .clickable(onClickLabel = key) { prefs.setAccent(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == accentKey) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            RowDivider()
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.TextFields, Nx.Cyan)
                    Spacer(Modifier.size(14.dp))
                    Text("Terminal font size", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("$fontSize sp", color = Nx.Muted, fontSize = 14.sp)
                }
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { prefs.setTerminalFontSize(it.toInt()) },
                    valueRange = 10f..18f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Nx.Border)
                )
            }
            RowDivider()
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Speed, Nx.Green)
                    Spacer(Modifier.size(14.dp))
                    Text("Monitor refresh", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 10, 30).forEach { s ->
                        Text(
                            "$s s",
                            color = if (refresh == s) Color.White else Nx.Muted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (refresh == s) accent else Nx.SurfaceHigh)
                                .clickable { prefs.setRefreshSeconds(s) }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                }
            }
            RowDivider()
            ListRow(
                title = "Reset home layout",
                subtitle = "Put the home icons back in their default order",
                leading = { IconBadge(Icons.Rounded.RestartAlt, Nx.Amber) },
                onClick = { prefs.resetHomeTileOrder() }
            )
        }

        SectionHeader("Servers")
        GroupSurface {
            servers.forEach { s ->
                val spec = serverIconSpec(icons[s.id])
                ListRow(
                    title = s.name,
                    subtitle = "${s.username}@${s.hostname}:${s.port}",
                    leading = { IconBadge(spec.icon, spec.tint) },
                    trailing = {
                        Row {
                            IconButton(onClick = { onEditServer(s) }) { Icon(Icons.Rounded.Edit, "Edit", tint = Nx.Muted) }
                            IconButton(onClick = { onDeleteServer(s) }) { Icon(Icons.Rounded.Delete, "Delete", tint = Nx.Red) }
                        }
                    }
                )
                RowDivider()
            }
            ListRow(
                title = "Add server",
                titleColor = accent,
                leading = { IconBadge(Icons.Rounded.Add, accent) },
                onClick = onAddServer
            )
        }

        SectionHeader("Security")
        GroupSurface {
            SwitchRow(Icons.Rounded.Fingerprint, Nx.Green, "App lock", "Ask for fingerprint, face or PIN when opening the app", appLock, onToggleAppLock)
            RowDivider()
            SwitchRow(Icons.Rounded.Lock, Nx.Blue, "Protect screen", "Block screenshots and hide the app in recents", secure) { prefs.setSecureScreen(it) }
            RowDivider()
            ListRow(
                title = "Forget trusted host keys",
                titleColor = Nx.Red,
                subtitle = "You will verify each server's fingerprint again",
                leading = { IconBadge(Icons.Rounded.Security, Nx.Red) },
                onClick = { confirmClear = true }
            )
        }
        Text(
            "Passwords, private keys and passphrases are encrypted with a hardware-backed Android Keystore key (AES-256-GCM).",
            color = Nx.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        SectionHeader("About")
        GroupSurface {
            ListRow(
                title = "About Nexus SSH",
                subtitle = "Version ${BuildConfig.VERSION_NAME} · developed by ${AppInfo.DEVELOPER}",
                leading = { IconBadge(Icons.Rounded.Info, Nx.Cyan) },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Nx.Faint) },
                onClick = onOpenAbout
            )
        }
        Text(
            "Made by ${AppInfo.DEVELOPER}",
            color = Nx.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        )
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
