package com.example.constructorapp.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WebColorScheme = lightColorScheme(
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
fun ConstructorAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = WebColorScheme,
        typography = Typography,
        content = content
    )
}
