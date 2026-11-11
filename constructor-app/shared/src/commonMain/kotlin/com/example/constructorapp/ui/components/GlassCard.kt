package com.example.constructorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GlassCardDefaultBackground = Color(0xFFFFFFFF) // Solid white card ensures zero offscreen caching/rendering artifacts on iOS
val GlassCardDefaultBorder = Color(0xFFE2E8F0)
val GlassCardDefaultShape = RoundedCornerShape(20.dp)

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
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
