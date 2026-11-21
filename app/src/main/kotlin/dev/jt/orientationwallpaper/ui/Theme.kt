package dev.jt.orientationwallpaper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Pine = Color(0xFF1C3D36)
private val PineInk = Color(0xFF14302A)
private val Paper = Color(0xFFF3F0EA)
private val PaperRaised = Color(0xFFFFFCF7)
private val PaperMuted = Color(0xFFE7E1D8)
private val Ink = Color(0xFF1A1C1A)
private val Terracotta = Color(0xFFB85C38)
private val Night = Color(0xFF101412)
private val NightRaised = Color(0xFF1A211E)
private val NightMuted = Color(0xFF2A3330)
private val Mist = Color(0xFFC9E6DC)
private val Clay = Color(0xFFF0B59A)

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Paper,
    secondary = Terracotta,
    onSecondary = Color(0xFFFFF8F4),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperMuted,
    onSurfaceVariant = Color(0xFF4E4944),
    outline = Color(0xFF8A847A),
)

private val DarkColors = darkColorScheme(
    primary = Mist,
    onPrimary = PineInk,
    secondary = Clay,
    onSecondary = Color(0xFF3D2116),
    background = Night,
    onBackground = Color(0xFFE6E4DF),
    surface = NightRaised,
    onSurface = Color(0xFFE6E4DF),
    surfaceVariant = NightMuted,
    onSurfaceVariant = Color(0xFFC5C7C2),
    outline = Color(0xFF8C9691),
)

@Composable
fun OrientationWallpaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
