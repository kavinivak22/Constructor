package com.example.constructorapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceWhite,
    secondary = SkyInfo,
    onSecondary = SlateSurface,
    tertiary = EmeraldSuccess,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = RoseDanger,
    onError = SurfaceWhite
)

private val LightColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceWhite,
    secondary = SkyInfo,
    onSecondary = SlateSurface,
    tertiary = EmeraldSuccess,
    background = SlateBackground, // We keep sleek dark aesthetic as primary
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = RoseDanger,
    onError = SurfaceWhite
)

@Composable
fun ConstructorAppTheme(
    darkTheme: Boolean = true, // Default to dark glassmorphic theme to match web
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
