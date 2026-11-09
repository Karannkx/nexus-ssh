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
    val progress by pulse.animateFloat(0.18f, 0.92f, infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Reverse), label = "startupProgress")
    Box(Modifier.fillMaxSize().background(Color(0xFFF7F8F3))) {
        if (stage == 0) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF06291D), Color(0xFF0B4A32), Color(0xFF071D16)))))
            Image(
                painter = painterResource(com.nexus.ssh.R.drawable.bg_wallpaper),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.22f
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA06291D), Color(0xCC071D16)))))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                // The first visible frame is the branded initializing screen; no oversized launcher tile.
                Image(
                    painter = painterResource(com.nexus.ssh.R.drawable.nexus_mark_white),
                    contentDescription = "Nexus SSH",
                    modifier = Modifier.size(104.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text("Nexus SSH", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Secure  •  Manage  •  Anywhere", color = Color(0xFFD4E9DA), fontSize = 13.sp)
                Spacer(Modifier.height(42.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(150.dp).height(5.dp).clip(CircleShape), color = Color(0xFF8DE0A8), trackColor = Color(0x6654A879))
                Spacer(Modifier.height(12.dp))
                Text("Initializing…", color = Color(0xFFD4E9DA), fontSize = 12.sp)
            }
        } else {
            NexusBackdrop(wallpaper = true)
            Column(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nexus SSH", color = Nx.Text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(22.dp))
                NexusLoader(size = 112.dp)
                Spacer(Modifier.height(20.dp))
                Text("Loading Modules …", color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                listOf("Core Services", "SSH Engine", "System Modules", "UI Components").forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF31965A), modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(item, color = Nx.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF218B55), strokeWidth = 2.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("Preparing Workspace …", color = Nx.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
