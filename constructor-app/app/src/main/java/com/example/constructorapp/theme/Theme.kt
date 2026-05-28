package com.example.constructorapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = SurfaceWhite,
    secondary = CoralLight,
    onSecondary = CoralDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = RedDanger,
    onError = SurfaceWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = SurfaceWhite,
    secondary = CoralLight,
    onSecondary = CoralDark,
    background = GreyBackground,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    error = RedDanger,
    onError = SurfaceWhite
)

@Composable
fun ConstructorAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

