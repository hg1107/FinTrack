package com.fintrack.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinTrackLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Secondary,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF7A1F18),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = Color(0xFFE0D9D0)
)

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinTrackLightColorScheme,
        typography = FinTrackTypography,
        content = content
    )
}
