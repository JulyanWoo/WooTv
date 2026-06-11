package com.wootv.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = OnSurfaceDark,
    secondary = Teal200,
    onSecondary = Blue900,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorRed,
    border = FocusBorder
)

@Composable
fun WooTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
