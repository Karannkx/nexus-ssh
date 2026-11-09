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
import androidx.compose.material3.lightColorScheme
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
    val Bg = Color(0xFFF7F8F3)
    val BgTop = Color(0xFFFBFCF8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceHigh = Color(0xFFF0F3ED)
    val Text = Color(0xFF14271D)
    val Muted = Color(0xFF68776D)
    val Faint = Color(0xFF9AA79D)
    val Blue = Color(0xFF2388D8)
    val Cyan = Color(0xFF20B7C8)
    val Purple = Color(0xFF9876D7)
    val Green = Color(0xFF218B55)
    val Magenta = Color(0xFFE34D8B)
    val Amber = Color(0xFFE59A24)
    val Red = Color(0xFFD94B55)
    val Terminal = Color(0xFF071A14)
    val Border = Color(0xFFDDE5DC)
    val OnAccent = Color(0xFFFFFFFF)
}

val AccentOptions = listOf("green", "blue", "cyan", "purple", "magenta")

fun accentColor(key: String): Color = when (key) {
    "cyan" -> Nx.Cyan
    "purple" -> Nx.Purple
    "green" -> Nx.Green
    "magenta" -> Nx.Magenta
    else -> Nx.Blue
}

val LocalAccent = compositionLocalOf { Nx.Green }

private fun variableFamily(resId: Int, range: IntRange): FontFamily = FontFamily(
    *range.map { weight ->
        Font(
            resId = resId,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight))
        )
    }.toTypedArray()
)

/** Bundled Vercel Geist variable font for the application UI. */
val GeistFamily = variableFamily(R.font.geist_sans, 100..900)

/** Bundled Geist Mono for terminal output, paths, addresses and technical values. */
val GeistMonoFamily = variableFamily(R.font.geist_mono, 100..900)

/** Bricolage Grotesque is reserved for expressive display / screen headings. */
val BricolageFamily = variableFamily(R.font.bricolage_grotesque, 200..800)

private fun Typography.withFonts(body: FontFamily, display: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = display),
    displayMedium = displayMedium.copy(fontFamily = display),
    displaySmall = displaySmall.copy(fontFamily = display),
    headlineLarge = headlineLarge.copy(fontFamily = display),
    headlineMedium = headlineMedium.copy(fontFamily = display),
    headlineSmall = headlineSmall.copy(fontFamily = display),
    titleLarge = titleLarge.copy(fontFamily = body),
    titleMedium = titleMedium.copy(fontFamily = body),
    titleSmall = titleSmall.copy(fontFamily = body),
    bodyLarge = bodyLarge.copy(fontFamily = body),
    bodyMedium = bodyMedium.copy(fontFamily = body),
    bodySmall = bodySmall.copy(fontFamily = body),
    labelLarge = labelLarge.copy(fontFamily = body),
    labelMedium = labelMedium.copy(fontFamily = body),
    labelSmall = labelSmall.copy(fontFamily = body)
)

@Composable
fun NexusTheme(accent: Color = Nx.Green, dark: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF071A14),
        secondary = Color(0xFF79D6A5),
        onSecondary = Color(0xFF071A14),
        background = Color(0xFF101411),
        onBackground = Color(0xFFE8F0E9),
        surface = Color(0xFF171D19),
        onSurface = Color(0xFFE8F0E9),
        surfaceVariant = Color(0xFF252E28),
        onSurfaceVariant = Color(0xFFB2C0B5),
        outline = Color(0xFF526258),
        outlineVariant = Color(0xFF354139),
        error = Color(0xFFFF6B72)
    ) else lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = Nx.Green,
        onSecondary = Color.Black,
        background = Nx.Bg,
        onBackground = Nx.Text,
        surface = Nx.Surface,
        onSurface = Nx.Text,
        surfaceVariant = Nx.SurfaceHigh,
        surfaceContainerLowest = Nx.Bg,
        surfaceContainerLow = Nx.Surface,
        surfaceContainer = Nx.SurfaceHigh,
        surfaceContainerHigh = Nx.SurfaceHigh,
        surfaceContainerHighest = Color(0xFFE9EEE7),
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
            typography = Typography().withFonts(GeistFamily, BricolageFamily),
            content = content
        )
    }
}

/** Clean translucent light surface with a quiet border. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp), fill: Float = 0.96f): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = fill), shape)
    .border(1.dp, Nx.Border.copy(alpha = 0.92f), shape)

@Composable
fun nexusFieldColors(): TextFieldColors {
    val accent = LocalAccent.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = Nx.Border,
        focusedLabelColor = accent,
        unfocusedLabelColor = Nx.Muted,
        cursorColor = accent,
        focusedContainerColor = Color(0xFFFFFFFF),
        unfocusedContainerColor = Color(0xFFFFFFFF),
        focusedTextColor = Nx.Text,
        unfocusedTextColor = Nx.Text,
        disabledTextColor = Nx.Muted,
        focusedPlaceholderColor = Nx.Faint,
        unfocusedPlaceholderColor = Nx.Faint
    )
}

/** Warm light backdrop with the custom flowing green wave wallpaper. */
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
                    .background(Brush.verticalGradient(listOf(Color(0x08FFFFFF), Color.Transparent, Color(0x12FFFFFF))))
            )
        }
    }
}
