package com.nexus.ssh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.NexusLogo
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.PrimaryButton
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.components.relativeTime
import com.nexus.ssh.ui.components.SectionLabel
import com.nexus.ssh.ui.components.StatusDot
import com.nexus.ssh.ui.glass
import com.nexus.ssh.ui.serverIconSpec

private class Fav(val label: String, val path: String?, val custom: Boolean)

/** Drawer-style server manager from the reference design (also the "Servers" tab). */
@Composable
fun ServersScreen(
    servers: List<ServerEntity>,
    selectedId: Long?,
    connection: ConnectionState,
    icons: Map<Long, String>,
    customFavorites: List<String>,
    onClose: () -> Unit,
    onSelect: (ServerEntity) -> Unit,
    onConnect: (ServerEntity) -> Unit,
    onDisconnect: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ServerEntity) -> Unit,
    onChangeIcon: (ServerEntity) -> Unit,
    onDelete: (ServerEntity) -> Unit,
    onOpenFavorite: (String?) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onManage: () -> Unit
) {
    val connectedId = (connection as? ConnectionState.Connected)?.server?.id
    val connecting = connection is ConnectionState.Connecting
    var menuFor by remember { mutableStateOf<ServerEntity?>(null) }
    val favorites = buildList {
        add(Fav("Home", null, false))
        add(Fav("tmp", "/tmp", false))
        add(Fav("etc", "/etc", false))
        add(Fav("var", "/var", false))
        add(Fav("Logs", "/var/log", false))
        customFavorites.forEach { add(Fav(it.trimEnd('/').substringAfterLast('/').ifEmpty { "/" }, it, true)) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.90f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(topEnd = 34.dp, bottomEnd = 34.dp))
                .background(Color(0xF2121419))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(topEnd = 34.dp, bottomEnd = 34.dp)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                NexusLogo(size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Text("Nexus SSH", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Servers", Modifier.weight(1f))
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClickLabel = "Add server", onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add server", tint = Color.White)
                }
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (servers.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No servers yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Add one to get started.", color = Nx.Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton("Add server", onAdd)
                        }
                    }
                }
                items(servers, key = { it.id }) { server ->
                    val spec = serverIconSpec(icons[server.id])
                    val isSelected = server.id == selectedId
                    val isConnected = server.id == connectedId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.09f) else Color.Transparent)
                            .clickable { onSelect(server) }
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(
                            when {
                                isConnected -> Nx.Green
                                connecting && isSelected -> Nx.Amber
                                else -> Nx.Faint
                            },
                            size = 9.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Icon(spec.icon, contentDescription = spec.label, tint = spec.tint, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(server.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("${server.username}@${server.hostname} · ${relativeTime(server.lastConnectedAt)}", color = Nx.Muted, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = { menuFor = server }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Server options", tint = Color.White)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Favorites")
                    Spacer(Modifier.height(6.dp))
                }
                items(favorites, key = { (if (it.custom) "c:" else "d:") + it.label + (it.path ?: "") }) { fav ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenFavorite(fav.path) }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = Nx.Muted, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(fav.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        if (fav.custom && fav.path != null) {
                            IconButton(onClick = { onRemoveFavorite(fav.path) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove favorite", tint = Nx.Muted, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
                    .glass(RoundedCornerShape(18.dp), fill = 0.9f)
                    .clickable(onClick = onManage)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = LocalAccent.current)
                Spacer(Modifier.width(14.dp))
                Text("Manage Servers", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    menuFor?.let { srv ->
        val isConnected = srv.id == connectedId
        val actions = buildList {
            if (isConnected) add(SheetAction("Disconnect", Icons.Rounded.PowerSettingsNew, Nx.Red) { onDisconnect() })
            else add(SheetAction("Connect", Icons.Rounded.PowerSettingsNew, Nx.Green) { onConnect(srv) })
            add(SheetAction("Edit", Icons.Rounded.Edit) { onEdit(srv) })
            add(SheetAction("Change icon", Icons.Rounded.Palette) { onChangeIcon(srv) })
            add(SheetAction("Delete", Icons.Rounded.Delete, Nx.Red) { onDelete(srv) })
        }
        ActionSheet(title = srv.name, subtitle = "${srv.username}@${srv.hostname}:${srv.port}", actions = actions, onDismiss = { menuFor = null })
    }
}
