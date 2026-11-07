package com.nexus.ssh.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexus.ssh.ui.components.BigFolderIcon

/** Filled Docker whale carrying containers. */
@Composable
fun WhaleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val body = Path().apply {
            moveTo(8 * s, 54 * s)
            lineTo(74 * s, 54 * s)
            cubicTo(78 * s, 54 * s, 82 * s, 50 * s, 83 * s, 45 * s)
            cubicTo(88 * s, 46 * s, 92 * s, 50 * s, 93 * s, 54 * s)
            cubicTo(90 * s, 78 * s, 66 * s, 88 * s, 44 * s, 88 * s)
            cubicTo(24 * s, 88 * s, 11 * s, 76 * s, 8 * s, 54 * s)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(Color(0xFF4AA8FF), Color(0xFF2574D9))))
        val box = Color(0xFF9CD3FF)
        val corner = CornerRadius(1.5f * s)
        val cell = Size(12 * s, 11 * s)
        drawRoundRect(box, Offset(16 * s, 42 * s), cell, corner)
        drawRoundRect(box, Offset(30 * s, 42 * s), cell, corner)
        drawRoundRect(box, Offset(44 * s, 42 * s), cell, corner)
        drawRoundRect(box, Offset(30 * s, 30 * s), cell, corner)
        drawRoundRect(box, Offset(44 * s, 30 * s), cell, corner)
        drawCircle(Color(0xFF0E2A4D), 2.4f * s, Offset(70 * s, 62 * s))
    }
}

@Composable
fun FolderGlyph(modifier: Modifier = Modifier) = BigFolderIcon(modifier)

@Composable
fun GlyphIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(30.dp))
}

/** Every app of Nexus SSH, used by the home grid, the app drawer and the search. */
class AppEntry(
    val route: String,
    val label: String,
    val keywords: String,
    val glyph: @Composable () -> Unit
)

val ALL_APPS: List<AppEntry> = listOf(
    AppEntry(Routes.FILES, "Files", "sftp folders upload download edit") { FolderGlyph(Modifier.size(36.dp)) },
    AppEntry(Routes.TERMINAL, "Terminal", "shell ssh console command") { GlyphIcon(PromptIcon, Color.White) },
    AppEntry(Routes.MONITOR, "Monitor", "cpu memory disk network usage") { GlyphIcon(PulseIcon, Nx.Green) },
    AppEntry(Routes.DOCKER, "Docker", "containers images") { WhaleGlyph(Modifier.size(36.dp)) },
    AppEntry(Routes.SERVICES, "Services", "systemd units daemons") { GlyphIcon(Icons.Rounded.Hub, Nx.Purple) },
    AppEntry(Routes.LOGS, "Logs", "journal syslog kernel") { GlyphIcon(DocIcon, Color.White) },
    AppEntry(Routes.BROWSER, "Browser", "web apps http tunnel gui") { GlyphIcon(Icons.Rounded.Language, Color(0xFF4CA6FF)) },
    AppEntry(Routes.PROCESSES, "Processes", "top ps kill task manager") { GlyphIcon(Icons.Rounded.Memory, Nx.Amber) },
    AppEntry(Routes.SNIPPETS, "Snippets", "saved commands scripts shortcuts") { GlyphIcon(Icons.Rounded.Code, Nx.Magenta) },
    AppEntry(Routes.SERVERS, "Servers", "hosts connections manage") { GlyphIcon(ServersIcon, Color.White) },
    AppEntry(Routes.SETTINGS, "Settings", "theme security preferences") { GlyphIcon(Icons.Rounded.Settings, Color(0xFFB8C0CC)) },
    AppEntry(Routes.ABOUT, "About", "developer version licence") { GlyphIcon(Icons.Rounded.Info, Nx.Cyan) },
    AppEntry(Routes.APPS, "Apps", "all applications launcher") { GlyphIcon(GridIcon, Color.White) }
)

val HOME_DEFAULT = listOf(
    Routes.FILES, Routes.TERMINAL, Routes.MONITOR, Routes.DOCKER,
    Routes.SERVICES, Routes.APPS, Routes.LOGS, Routes.BROWSER
)

fun appFor(route: String): AppEntry? = ALL_APPS.firstOrNull { it.route == route }
