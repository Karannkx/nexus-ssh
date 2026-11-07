package com.nexus.ssh.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.AppInfo
import com.nexus.ssh.BuildConfig
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.GroupSurface
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.NexusLogo
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SectionHeader
import java.util.Calendar

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val chevron: @Composable () -> Unit = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Nx.Faint) }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("About", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NexusLogo(size = 92.dp)
                Spacer(Modifier.height(14.dp))
                Text(AppInfo.APP_NAME, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME}", color = Nx.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A native Android SSH client for your servers: terminal, files, monitoring, Docker, services and more.",
                    color = Nx.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            SectionHeader("Developer")
            GroupSurface {
                ListRow(
                    title = AppInfo.DEVELOPER,
                    subtitle = "@${AppInfo.DEVELOPER_HANDLE} on GitHub",
                    leading = { IconBadge(Icons.Rounded.Person, Nx.Blue) },
                    trailing = chevron,
                    onClick = { onOpenUrl(AppInfo.DEVELOPER_URL) }
                )
            }

            SectionHeader("Project")
            GroupSurface {
                ListRow(
                    title = "Source code",
                    subtitle = "github.com/${AppInfo.DEVELOPER_HANDLE}/nexus-ssh",
                    leading = { IconBadge(Icons.Rounded.Code, Nx.Green) },
                    trailing = chevron,
                    onClick = { onOpenUrl(AppInfo.REPO_URL) }
                )
                RowDivider()
                ListRow(
                    title = "Report an issue",
                    subtitle = "Bugs and feature requests",
                    leading = { IconBadge(Icons.Rounded.BugReport, Nx.Amber) },
                    trailing = chevron,
                    onClick = { onOpenUrl(AppInfo.ISSUES_URL) }
                )
                RowDivider()
                ListRow(
                    title = "Licence",
                    subtitle = "MIT, free and open source",
                    leading = { IconBadge(Icons.Rounded.Gavel, Nx.Purple) }
                )
                RowDivider()
                ListRow(
                    title = "Built with",
                    subtitle = "Kotlin, Jetpack Compose, SSHJ, Room, BouncyCastle. Typeface: Inter (SIL Open Font License).",
                    leading = { IconBadge(Icons.Rounded.Layers, Nx.Cyan) }
                )
            }

            Text(
                "© ${Calendar.getInstance().get(Calendar.YEAR)} ${AppInfo.DEVELOPER}",
                color = Nx.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp)
            )
        }
    }
}
