package com.nexus.ssh.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun strokeIcon(name: String, width: Float = 2f, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }.build()

private fun PathBuilder.box(x: Float, y: Float, w: Float, h: Float) {
    moveTo(x, y)
    lineTo(x + w, y)
    lineTo(x + w, y + h)
    lineTo(x, y + h)
    close()
}

/** `>_` command prompt. */
val PromptIcon: ImageVector = strokeIcon("Prompt", 2.2f) {
    moveTo(6f, 8f); lineTo(11f, 12f); lineTo(6f, 16f)
    moveTo(13f, 16.5f); lineTo(18f, 16.5f)
}

/** Heartbeat line used for the Monitor tile. */
val PulseIcon: ImageVector = strokeIcon("Pulse", 2.2f) {
    moveTo(2f, 12f); lineTo(6.5f, 12f); lineTo(9f, 5f); lineTo(13f, 19f)
    lineTo(16f, 9f); lineTo(18f, 12f); lineTo(22f, 12f)
}

/** Simplified Docker whale carrying containers. */
val WhaleIcon: ImageVector = strokeIcon("Whale", 1.7f) {
    box(4f, 9.5f, 3f, 3f); box(7.5f, 9.5f, 3f, 3f); box(11f, 9.5f, 3f, 3f); box(7.5f, 6f, 3f, 3f)
    moveTo(2f, 13.5f); lineTo(17.5f, 13.5f)
    curveTo(19f, 13.5f, 20f, 12.4f, 20.4f, 11f)
    curveTo(21.4f, 11.3f, 22f, 12.2f, 22f, 12.2f)
    curveTo(21.2f, 17.5f, 16f, 20.5f, 10f, 20.5f)
    curveTo(5.2f, 20.5f, 2.6f, 17.6f, 2f, 13.5f)
}

val GridIcon: ImageVector = strokeIcon("Grid") {
    box(4f, 4f, 6f, 6f); box(14f, 4f, 6f, 6f); box(4f, 14f, 6f, 6f); box(14f, 14f, 6f, 6f)
}

val ListIcon: ImageVector = strokeIcon("List") {
    moveTo(5f, 7f); lineTo(19f, 7f)
    moveTo(5f, 12f); lineTo(19f, 12f)
    moveTo(5f, 17f); lineTo(19f, 17f)
}

val DocIcon: ImageVector = strokeIcon("Doc", 1.9f) {
    box(5f, 3f, 14f, 18f)
    moveTo(8.5f, 9f); lineTo(15.5f, 9f)
    moveTo(8.5f, 13f); lineTo(15.5f, 13f)
    moveTo(8.5f, 17f); lineTo(12.5f, 17f)
}

val CubeIcon: ImageVector = strokeIcon("Cube") {
    moveTo(12f, 3f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 21f); lineTo(4f, 16.5f); lineTo(4f, 7.5f); close()
    moveTo(4f, 7.5f); lineTo(12f, 12f); lineTo(20f, 7.5f)
    moveTo(12f, 12f); lineTo(12f, 21f)
}

val ServersIcon: ImageVector = strokeIcon("Servers") {
    box(4f, 4f, 16f, 6f); box(4f, 14f, 16f, 6f)
    moveTo(7.5f, 7f); lineTo(7.6f, 7f)
    moveTo(7.5f, 17f); lineTo(7.6f, 17f)
    moveTo(11f, 7f); lineTo(16f, 7f)
    moveTo(11f, 17f); lineTo(16f, 17f)
}

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
