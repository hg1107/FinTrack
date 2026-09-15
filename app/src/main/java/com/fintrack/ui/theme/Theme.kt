package com.fintrack.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinTrackDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PaidGreen,
    secondary = Amber,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Amber,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFB4A9),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = Color(0xFF333333)
)

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinTrackDarkColorScheme,
        typography = FinTrackTypography,
        content = content
    )
}
