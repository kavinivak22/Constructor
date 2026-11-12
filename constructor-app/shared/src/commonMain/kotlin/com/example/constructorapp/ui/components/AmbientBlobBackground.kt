package com.example.constructorapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AmbientBlobBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientBlobDrift")

    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 25000
                0f at 0 using FastOutSlowInEasing
                80f at 8250 using FastOutSlowInEasing
                -50f at 16500 using FastOutSlowInEasing
                0f at 25000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1X"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 25000
                0f at 0 using FastOutSlowInEasing
                -100f at 8250 using FastOutSlowInEasing
                60f at 16500 using FastOutSlowInEasing
                0f at 25000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1Y"
    )

    val blob2X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 30000
                0f at 0 using FastOutSlowInEasing
                -70f at 10000 using FastOutSlowInEasing
                60f at 20000 using FastOutSlowInEasing
                0f at 30000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2X"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 30000
                0f at 0 using FastOutSlowInEasing
                90f at 10000 using FastOutSlowInEasing
                -80f at 20000 using FastOutSlowInEasing
                0f at 30000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2Y"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Blob 1: Orange / Brand Primary accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x18EA580C), Color.Transparent),
                    center = Offset(width * 0.25f + blob1X, height * 0.15f + blob1Y),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.25f + blob1X, height * 0.15f + blob1Y)
            )

            // Blob 2: Sky Blue accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x140284C7), Color.Transparent),
                    center = Offset(width * 0.8f + blob2X, height * 0.45f + blob2Y),
                    radius = width * 0.60f
                ),
                radius = width * 0.60f,
                center = Offset(width * 0.8f + blob2X, height * 0.45f + blob2Y)
            )

            // Blob 3: Emerald Success accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1010B981), Color.Transparent),
                    center = Offset(width * 0.35f, height * 0.8f),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(width * 0.35f, height * 0.8f)
            )
        }
    }
}
