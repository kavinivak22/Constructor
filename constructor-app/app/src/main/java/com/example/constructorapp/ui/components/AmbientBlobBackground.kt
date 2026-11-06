package com.example.constructorapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.constructorapp.theme.SlateBackground

@Composable
fun AmbientBlobBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientBlobDrift")

    // Blob 1: 25s duration (25000ms), Indigo -> Purple
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
    val blob1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 25000
                1f at 0 using FastOutSlowInEasing
                1.2f at 8250 using FastOutSlowInEasing
                0.85f at 16500 using FastOutSlowInEasing
                1f at 25000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1Scale"
    )

    // Blob 2: 30s duration (30000ms), Amber -> Orange
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 30000
                0f at 0 using FastOutSlowInEasing
                -100f at 9900 using FastOutSlowInEasing
                60f at 19800 using FastOutSlowInEasing
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
                80f at 9900 using FastOutSlowInEasing
                -60f at 19800 using FastOutSlowInEasing
                0f at 30000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2Y"
    )
    val blob2Scale by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 30000
                1.15f at 0 using FastOutSlowInEasing
                0.85f at 9900 using FastOutSlowInEasing
                1.25f at 19800 using FastOutSlowInEasing
                1.15f at 30000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2Scale"
    )

    // Blob 3: 28s duration (28000ms), Cyan -> Emerald
    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 28000
                0f at 0 using FastOutSlowInEasing
                60f at 9240 using FastOutSlowInEasing
                -80f at 18480 using FastOutSlowInEasing
                0f at 28000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob3X"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 28000
                0f at 0 using FastOutSlowInEasing
                70f at 9240 using FastOutSlowInEasing
                -50f at 18480 using FastOutSlowInEasing
                0f at 28000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob3Y"
    )
    val blob3Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 28000
                0.9f at 0 using FastOutSlowInEasing
                1.15f at 9240 using FastOutSlowInEasing
                0.8f at 18480 using FastOutSlowInEasing
                0.9f at 28000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blob3Scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            // Blob 1: Top-Left (70vw equivalent) - Indigo to Purple ambient glow
            val blob1Radius = (width * 0.85f) * blob1Scale
            val blob1Center = Offset(
                x = (width * -0.05f) + blob1X,
                y = (height * -0.05f) + blob1Y
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x406366F1), // Indigo 25%
                        0.30f to Color(0x358B5CF6), // Purple 21%
                        0.60f to Color(0x1F8B5CF6), // Purple 12%
                        0.85f to Color(0x0A8B5CF6), // Purple 4%
                        1.0f to Color.Transparent
                    ),
                    center = blob1Center,
                    radius = blob1Radius
                ),
                radius = blob1Radius,
                center = blob1Center
            )

            // Blob 2: Bottom-Right (60vw equivalent) - Amber to Safety Orange
            val blob2Radius = (width * 0.75f) * blob2Scale
            val blob2Center = Offset(
                x = (width * 1.05f) + blob2X,
                y = (height * 0.95f) + blob2Y
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x38F59E0B), // Amber 22%
                        0.35f to Color(0x30F97316), // Orange 19%
                        0.70f to Color(0x15F97316), // Orange 8%
                        1.0f to Color.Transparent
                    ),
                    center = blob2Center,
                    radius = blob2Radius
                ),
                radius = blob2Radius,
                center = blob2Center
            )

            // Blob 3: Center-Right (50vw equivalent) - Cyan to Emerald
            val blob3Radius = (width * 0.65f) * blob3Scale
            val blob3Center = Offset(
                x = (width * 0.90f) + blob3X,
                y = (height * 0.40f) + blob3Y
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x2806B6D4), // Cyan 16%
                        0.40f to Color(0x2210B981), // Emerald 13%
                        0.75f to Color(0x0C10B981), // Emerald 5%
                        1.0f to Color.Transparent
                    ),
                    center = blob3Center,
                    radius = blob3Radius
                ),
                radius = blob3Radius,
                center = blob3Center
            )
        }
    }
}
