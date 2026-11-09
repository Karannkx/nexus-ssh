package com.nexus.ssh.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.NexusBackdrop
import com.nexus.ssh.ui.Nx

@Composable
fun StartupExperience(stage: Int) {
    val pulse = rememberInfiniteTransition(label = "startupPulse")
    val progress by pulse.animateFloat(
        0.18f, 0.92f,
        infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Reverse),
        label = "startupProgress"
    )
    val statusTint = if (Nx.Dark) Color(0xFFADB7C8) else Color(0xFF218B55)
    val logoRes = if (Nx.Dark) com.nexus.ssh.R.drawable.nexus_mark_white else com.nexus.ssh.R.drawable.nexus_mark_black

    Box(Modifier.fillMaxSize().background(Nx.Bg)) {
        // Both startup stages follow the selected app appearance and its matching wallpaper.
        NexusBackdrop(wallpaper = true)
        if (stage == 0) {
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = "Nexus SSH",
                    modifier = Modifier.size(82.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text("Nexus SSH", color = Nx.Text, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Secure  •  Manage  •  Anywhere", color = Nx.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(42.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.width(176.dp).height(5.dp).clip(CircleShape),
                    color = statusTint,
                    trackColor = statusTint.copy(alpha = 0.18f)
                )
                Spacer(Modifier.height(13.dp))
                Text("Initializing…", color = Nx.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nexus SSH", color = Nx.Text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(22.dp))
                NexusLoader(size = 112.dp)
                Spacer(Modifier.height(20.dp))
                Text("Loading Modules …", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                listOf("Core Services", "SSH Engine", "System Modules", "UI Components").forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = statusTint, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(item, color = Nx.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), color = statusTint, strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("Preparing Workspace …", color = Nx.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
