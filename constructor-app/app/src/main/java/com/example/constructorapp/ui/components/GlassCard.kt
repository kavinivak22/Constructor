package com.example.constructorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Webapp Exact Fluid Glass Tokens (.glass-card in globals.css)
// background-color: rgba(255, 255, 255, 0.55) -> 0x8CFFFFFF
// border: 1px solid rgba(255, 255, 255, 0.40) -> 0x66FFFFFF
// border-radius: 1.5rem (24px) -> rounded-3xl
val GlassCardDefaultBackground = Color(0xB8FFFFFF) // 72% translucent white: lower transparency ensures content clarity while retaining fluid refraction
val GlassCardDefaultBorder = Color(0x80FFFFFF)     // 50% translucent specular glass border
val GlassCardDefaultShape = RoundedCornerShape(24.dp)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardDefaultShape,
    backgroundColor: Color = GlassCardDefaultBackground,
    borderColor: Color = GlassCardDefaultBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                spotColor = Color(0x141F2687),
                ambientColor = Color(0x141F2687)
            )
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle top highlight reflection mimicking inset 0 1px 2px rgba(255,255,255,0.6)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xB3FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    }
}
