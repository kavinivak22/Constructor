package com.example.constructorapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SharedColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandPrimary,
    secondary = SkyInfo,
    onSecondary = SurfaceWhite,
    secondaryContainer = SkyInfoContainer,
    onSecondaryContainer = SkyInfo,
    tertiary = EmeraldSuccess,
    onTertiary = SurfaceWhite,
    tertiaryContainer = EmeraldSuccessContainer,
    onTertiaryContainer = EmeraldSuccess,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    outlineVariant = SlateDivider,
    error = RoseDanger,
    onError = SurfaceWhite,
    errorContainer = RoseDangerContainer,
    onErrorContainer = RoseDanger,
    scrim = Color(0x33000000)
)

@Composable
fun ConstructorSharedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SharedColorScheme,
        content = content
    )
}
