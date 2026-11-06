package com.nexus.ssh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx

class SheetAction(
    val label: String,
    val icon: ImageVector? = null,
    val tint: Color = Color.White,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

/** Mobile-friendly replacement for dropdown menus: big touch rows in a bottom sheet. */
@Composable
fun ActionSheet(title: String?, subtitle: String? = null, actions: List<SheetAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Nx.SurfaceHigh) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .navigationBarsPadding()
        ) {
            if (title != null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, color = Nx.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            a.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (a.icon != null) {
                        Icon(a.icon, contentDescription = null, tint = a.tint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(a.label, color = a.tint, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (a.selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = LocalAccent.current)
                }
            }
        }
    }
}

/** One flat rounded surface that holds several rows separated by hairlines. */
@Composable
fun GroupSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Nx.Surface.copy(alpha = 0.92f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)),
        content = content
    )
}

@Composable
fun RowDivider(startIndent: Dp = 70.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Nx.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(start = 8.dp, top = 22.dp, bottom = 8.dp)
    )
}

/** Flat tinted rounded-square icon, like the system Settings app. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = Color.White,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(
        m.heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, color = Nx.Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

fun relativeTime(millis: Long?): String {
    if (millis == null) return "never connected"
    val diff = System.currentTimeMillis() - millis
    val min = diff / 60_000
    return when {
        diff < 60_000 -> "used just now"
        min < 60 -> "used $min min ago"
        min < 60 * 24 -> "used ${min / 60} h ago"
        else -> "used ${min / (60 * 24)} d ago"
    }
}
