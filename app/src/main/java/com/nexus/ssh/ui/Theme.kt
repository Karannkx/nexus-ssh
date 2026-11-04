package com.nexus.ssh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NexusDarkNavy = Color(0xFF070A12)
val NexusCardNavy = Color(0xFF0F172A)
val NexusBorderNavy = Color(0xFF1E293B)
val NexusCyan = Color(0xFF38BDF8)
val NexusEmerald = Color(0xFF10B981)
val NexusAmber = Color(0xFFF59E0B)
val NexusRose = Color(0xFFF43F5E)
val NexusMuted = Color(0xFF94A3B8)
val NexusTerminalBg = Color(0xFF030712)

private val NexusColorScheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color.Black,
    secondary = NexusEmerald,
    onSecondary = Color.Black,
    background = NexusDarkNavy,
    onBackground = Color.White,
    surface = NexusCardNavy,
    onSurface = Color.White,
    surfaceVariant = NexusBorderNavy,
    onSurfaceVariant = NexusMuted,
    outline = Color(0xFF334155),
    error = NexusRose
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NexusColorScheme, content = content)
}
