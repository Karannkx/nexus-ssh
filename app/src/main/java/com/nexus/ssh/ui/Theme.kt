package com.nexus.ssh.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.ssh.R

/** Colour palette taken from the reference design. */
object Nx {
    val Bg = Color(0xFF0C0E12)
    val BgTop = Color(0xFF15181E)
    val Surface = Color(0xFF1A1D24)
    val SurfaceHigh = Color(0xFF21252D)
    val Muted = Color(0xFF9AA3B2)
    val Faint = Color(0xFF6B7280)
    val Blue = Color(0xFF3B9BFF)
    val Cyan = Color(0xFF22D3EE)
    val Purple = Color(0xFFA78BFA)
    val Green = Color(0xFF34D399)
    val Magenta = Color(0xFFEC4899)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFF43F5E)
    val Terminal = Color(0xFF06080C)
}

val AccentOptions = listOf("blue", "cyan", "purple", "green", "magenta")

fun accentColor(key: String): Color = when (key) {
    "cyan" -> Nx.Cyan
    "purple" -> Nx.Purple
    "green" -> Nx.Green
    "magenta" -> Nx.Magenta
    else -> Nx.Blue
}

val LocalAccent = compositionLocalOf { Nx.Blue }

private fun inter(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val InterFamily = FontFamily(inter(400), inter(500), inter(600), inter(700))

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun NexusTheme(accent: Color = Nx.Blue, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF04101C),
        secondary = Nx.Green,
        onSecondary = Color.Black,
        background = Nx.Bg,
        onBackground = Color.White,
        surface = Nx.Surface,
        onSurface = Color.White,
        surfaceVariant = Nx.SurfaceHigh,
        surfaceContainerLowest = Nx.Bg,
        surfaceContainerLow = Nx.Surface,
        surfaceContainer = Nx.SurfaceHigh,
        surfaceContainerHigh = Nx.SurfaceHigh,
        surfaceContainerHighest = Color(0xFF282C38),
        onSurfaceVariant = Nx.Muted,
        outline = Color(0x33FFFFFF),
        outlineVariant = Color(0x1FFFFFFF),
        error = Nx.Red
    )
    val base = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    // The layouts are designed for ~390dp wide phones. On narrower screens (or when the system
    // "display size" is large) everything is scaled down uniformly instead of clipping or wrapping.
    val k = if (widthDp < 390) (widthDp / 390f).coerceAtLeast(0.78f) else 1f
    val density = Density(base.density * k, fontScale = base.fontScale.coerceIn(0.9f, 1.1f))
    CompositionLocalProvider(LocalAccent provides accent, LocalDensity provides density) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography().withFont(InterFamily),
            content = content
        )
    }
}

/** Translucent "glass" surface: dark tinted fill, soft top highlight and a thin light border. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp), fill: Float = 0.78f): Modifier = this
    .clip(shape)
    .background(Nx.Surface.copy(alpha = fill), shape)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)), shape)
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))),
        shape
    )

@Composable
fun nexusFieldColors(): TextFieldColors {
    val accent = LocalAccent.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedLabelColor = accent,
        unfocusedLabelColor = Nx.Muted,
        cursorColor = accent,
        focusedContainerColor = Color(0x0DFFFFFF),
        unfocusedContainerColor = Color(0x08FFFFFF),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Nx.Muted,
        focusedPlaceholderColor = Nx.Faint,
        unfocusedPlaceholderColor = Nx.Faint
    )
}

/** Dark gradient backdrop with the optional wallpaper (flowing silver waves). */
@Composable
fun NexusBackdrop(wallpaper: Boolean, modifier: Modifier = Modifier) {
    val wp by animateFloatAsState(if (wallpaper) 1f else 0f, tween(350), label = "wallpaper")
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Nx.BgTop, Nx.Bg)))) {
        if (wp > 0.01f) {
            Image(
                painter = painterResource(R.drawable.bg_wallpaper),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(wp),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(wp)
                    .background(Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent, Color(0x80000000))))
            )
        }
    }
}
