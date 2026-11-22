package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PyForgeDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = BgCanvas,
    primaryContainer = AccentCyanDark,
    onPrimaryContainer = AccentCyan,
    secondary = AccentEmerald,
    onSecondary = BgCanvas,
    secondaryContainer = AccentEmeraldDark,
    onSecondaryContainer = AccentEmerald,
    tertiary = AccentAmber,
    onTertiary = BgCanvas,
    tertiaryContainer = AccentAmberDark,
    onTertiaryContainer = AccentAmber,
    error = AccentCoral,
    onError = BgCanvas,
    errorContainer = AccentCoralDark,
    onErrorContainer = AccentCoral,
    background = BgCanvas,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BgSurfaceHover,
)

@Composable
fun PyForgeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PyForgeDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
