package com.mindurset.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = NavyDeep,
    onPrimary = SurfaceWhite,
    primaryContainer = PaleBlue,
    onPrimaryContainer = NavyInk,
    secondary = MoodAnxious,
    onSecondary = SurfaceWhite,
    secondaryContainer = Lavender,
    onSecondaryContainer = NavyInk,
    background = CanvasBlue,
    onBackground = NavyInk,
    surface = SurfaceWhite,
    onSurface = NavyInk,
    surfaceVariant = CanvasBlue,
    onSurfaceVariant = MutedGray,
    outline = OutlineSoft,
    error = DangerRed,
    onError = SurfaceWhite,
)

private val DarkColors = darkColorScheme(
    primary = PaleBlue,
    onPrimary = NavyInk,
    primaryContainer = NavyInk,
    onPrimaryContainer = PaleBlue,
    background = NavyInk,
    onBackground = SurfaceSoft,
    surface = NavyDeep,
    onSurface = SurfaceSoft,
    outline = MutedGray,
)

@Composable
fun MindUrSetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
