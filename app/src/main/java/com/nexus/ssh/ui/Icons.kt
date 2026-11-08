package com.nexus.ssh.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

/** Consistent rounded vector glyphs for the launcher and navigation. */
val PromptIcon: ImageVector = Icons.Rounded.Terminal
val PulseIcon: ImageVector = Icons.Rounded.MonitorHeart
val GridIcon: ImageVector = Icons.Rounded.GridView
val ListIcon: ImageVector = Icons.Rounded.FormatListBulleted
val DocIcon: ImageVector = Icons.Rounded.Description
val CubeIcon: ImageVector = Icons.Rounded.Inventory2
val ServersIcon: ImageVector = Icons.Rounded.Dns

/** Docker's whale is a brand-specific mark, so keep this small custom vector. */
private fun dockerWhaleIcon(): ImageVector = ImageVector.Builder("DockerWhale", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color(0xFF1684D8))) {
        moveTo(2f, 13.5f); lineTo(17.5f, 13.5f)
        curveTo(19f, 13.5f, 20f, 12.4f, 20.4f, 11f)
        curveTo(21.4f, 11.3f, 22f, 12.2f, 22f, 12.2f)
        curveTo(21.2f, 17.5f, 16f, 20.5f, 10f, 20.5f)
        curveTo(5.2f, 20.5f, 2.6f, 17.6f, 2f, 13.5f)
        close()
    }
    path(fill = SolidColor(Color(0xFF1684D8))) {
        moveTo(4f, 9.5f); lineTo(7f, 9.5f); lineTo(7f, 12.5f); lineTo(4f, 12.5f); close()
        moveTo(7.5f, 9.5f); lineTo(10.5f, 9.5f); lineTo(10.5f, 12.5f); lineTo(7.5f, 12.5f); close()
        moveTo(11f, 9.5f); lineTo(14f, 9.5f); lineTo(14f, 12.5f); lineTo(11f, 12.5f); close()
        moveTo(7.5f, 6f); lineTo(10.5f, 6f); lineTo(10.5f, 9f); lineTo(7.5f, 9f); close()
    }
}.build()

val WhaleIcon: ImageVector = dockerWhaleIcon()

class ServerIconSpec(val key: String, val icon: ImageVector, val tint: Color, val label: String)

val ServerIconOptions = listOf(
    ServerIconSpec("globe", Icons.Rounded.Public, Nx.Blue, "Web"),
    ServerIconSpec("fire", Icons.Rounded.LocalFireDepartment, Color(0xFFFF6B3D), "Hot"),
    ServerIconSpec("db", Icons.Rounded.Storage, Nx.Green, "Database"),
    ServerIconSpec("cube", CubeIcon, Nx.Amber, "Backup"),
    ServerIconSpec("cloud", Icons.Rounded.Cloud, Nx.Purple, "Cloud"),
    ServerIconSpec("server", ServersIcon, Nx.Cyan, "Server")
)

fun serverIconSpec(key: String?): ServerIconSpec =
    ServerIconOptions.firstOrNull { it.key == key } ?: ServerIconOptions[0]
