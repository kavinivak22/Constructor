package com.example.constructorapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

data class SiteTelemetryPetal(
    val id: String,
    val title: String,
    val detail: String,
    val angleDeg: Float,
    val distanceDp: Float,
    val badgeColor: Color,
    val icon: ImageVector
)

val defaultTelemetryPetals = listOf(
    SiteTelemetryPetal(
        id = "curing",
        title = "31°C • Curing Optimal",
        detail = "0% Rain Risk • Slab pour window open until 6:30 PM",
        angleDeg = 210f,
        distanceDp = 82f,
        badgeColor = EmeraldSuccess,
        icon = Icons.Default.WbSunny
    ),
    SiteTelemetryPetal(
        id = "wind",
        title = "11 km/h • Crane Safe",
        detail = "Calm breeze • Well below 38 km/h crane lifting limit",
        angleDeg = 330f,
        distanceDp = 82f,
        badgeColor = SkyInfo,
        icon = Icons.Default.Air
    ),
    SiteTelemetryPetal(
        id = "workers",
        title = "18 Active On-Site",
        detail = "3 active project sites • All check-ins accounted for",
        angleDeg = 90f,
        distanceDp = 74f,
        badgeColor = AmberWarning,
        icon = Icons.Default.People
    )
)

@Composable
fun LivingCreatureSentinel(
    onRestoreAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isBloomed by remember { mutableStateOf(false) }
    var isSquished by remember { mutableStateOf(false) }
    var selectedPetal by remember { mutableStateOf<SiteTelemetryPetal?>(null) }
    var isBlinking by remember { mutableStateOf(false) }

    // Pupil glance offsets
    val pupilOffsetX = remember { Animatable(0f) }
    val pupilOffsetY = remember { Animatable(0f) }

    // 1. Continuous breathing pulse (0.97 to 1.03)
    val infiniteTransition = rememberInfiniteTransition(label = "sentinel_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 2. Autonomous blinking every 3.8s for 180ms
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3800)
            isBlinking = true
            delay(180)
            isBlinking = false
        }
    }

    // 3. Autonomous glancing every 3.2s
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3200)
            if (!isBloomed) {
                val targetX = (kotlin.random.Random.nextFloat() * 12f - 6f)
                val targetY = (kotlin.random.Random.nextFloat() * 8f - 4f)
                launch {
                    pupilOffsetX.animateTo(targetX, tween(400, easing = FastOutSlowInEasing))
                }
                launch {
                    pupilOffsetY.animateTo(targetY, tween(400, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    // Squish scale modifier on tap
    val squishScaleX by animateFloatAsState(
        targetValue = if (isSquished) 1.22f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "squish_x"
    )
    val squishScaleY by animateFloatAsState(
        targetValue = if (isSquished) 0.78f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "squish_y"
    )

    // Blooming animation
    val bloomProgress by animateFloatAsState(
        targetValue = if (isBloomed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bloom_spring"
    )

    val density = LocalDensity.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(16.dp),
        borderColor = BrandPrimary.copy(alpha = 0.25f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Restore alerts button in top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ALL SITES GREEN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        letterSpacing = 0.5.sp
                    )
                }

                TextButton(
                    onClick = onRestoreAlerts,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Simulate Alerts",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Simulate Alerts",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Interactive Mascot Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Blooming Petals positioned around creature
                defaultTelemetryPetals.forEach { petal ->
                    val angleRad = petal.angleDeg.toDouble() * (kotlin.math.PI / 180.0)
                    val distancePx = with(density) { (petal.distanceDp * bloomProgress).dp.toPx() }
                    val petalX = cos(angleRad).toFloat() * distancePx
                    val petalY = sin(angleRad).toFloat() * distancePx

                    if (bloomProgress > 0.05f) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { petalX.toDp() },
                                    y = with(density) { petalY.toDp() }
                                )
                                .scale(bloomProgress)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateSurface)
                                .clickable {
                                    selectedPetal = if (selectedPetal?.id == petal.id) null else petal
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = petal.icon,
                                    contentDescription = petal.title,
                                    tint = petal.badgeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = petal.title.substringBefore(" •"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Mascot Body Canvas with touch & glance
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(
                            scaleX = breathingScale * squishScaleX,
                            scaleY = breathingScale * squishScaleY
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    coroutineScope.launch {
                                        isSquished = true
                                        delay(200)
                                        isSquished = false
                                        isBloomed = !isBloomed
                                        if (!isBloomed) selectedPetal = null
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        pupilOffsetX.animateTo(0f, tween(300))
                                        pupilOffsetY.animateTo(0f, tween(300))
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        val newX = (pupilOffsetX.value + dragAmount.x * 0.15f).coerceIn(-6f, 6f)
                                        val newY = (pupilOffsetY.value + dragAmount.y * 0.15f).coerceIn(-4f, 4f)
                                        pupilOffsetX.snapTo(newX)
                                        pupilOffsetY.snapTo(newY)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawMascotBody(
                            isBlinking = isBlinking,
                            isBloomed = isBloomed,
                            pupilX = pupilOffsetX.value,
                            pupilY = pupilOffsetY.value
                        )
                    }
                }
            }

            // Subtitle hint below creature
            Text(
                text = if (isBloomed) "Tap mascot to fold telemetry petals" else "Tap Sentinel to bloom site telemetry",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 105.dp)
            )

            // Detail Popover Card when petal is selected
            AnimatedVisibility(
                visible = selectedPetal != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedPetal?.let { petal ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateSurfaceSubtle)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = petal.icon,
                                contentDescription = null,
                                tint = petal.badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = petal.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = petal.detail,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { selectedPetal = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawMascotBody(
    isBlinking: Boolean,
    isBloomed: Boolean,
    pupilX: Float,
    pupilY: Float
) {
    val w = size.width
    val h = size.height

    // 1. Organic body gradient
    val bodyBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF97316), // BrandPrimary
            Color(0xFFFB923C), // Amber-Orange
            Color(0xFFF59E0B)  // Golden Amber
        ),
        start = Offset(0f, 0f),
        end = Offset(w, h)
    )

    // Draw smooth blob body
    drawRoundRect(
        brush = bodyBrush,
        topLeft = Offset(2f, 2f),
        size = Size(w - 4f, h - 4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.44f, h * 0.44f)
    )

    // Top-left soft gloss highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(w * 0.3f, h * 0.28f),
            radius = w * 0.28f
        ),
        radius = w * 0.28f,
        center = Offset(w * 0.3f, h * 0.28f)
    )

    // 2. Eyes
    val eyeY = h * 0.42f
    val leftEyeX = w * 0.34f
    val rightEyeX = w * 0.66f
    val eyeRadius = w * 0.12f

    if (isBloomed) {
        // Happy crescent eyes
        val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round)
        val eyeArcSize = Size(eyeRadius * 2f, eyeRadius * 1.5f)

        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius * 0.5f),
            size = eyeArcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(rightEyeX - eyeRadius, eyeY - eyeRadius * 0.5f),
            size = eyeArcSize,
            style = stroke
        )
    } else if (isBlinking) {
        // Blinking slit
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(leftEyeX - eyeRadius, eyeY),
            end = Offset(leftEyeX + eyeRadius, eyeY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(rightEyeX - eyeRadius, eyeY),
            end = Offset(rightEyeX + eyeRadius, eyeY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    } else {
        // Normal wide eyes with white sclera and moving pupils
        drawCircle(
            color = Color.White,
            radius = eyeRadius,
            center = Offset(leftEyeX, eyeY)
        )
        drawCircle(
            color = Color.White,
            radius = eyeRadius,
            center = Offset(rightEyeX, eyeY)
        )

        // Pupils
        val pupilRadius = eyeRadius * 0.52f
        drawCircle(
            color = Color(0xFF0F172A),
            radius = pupilRadius,
            center = Offset(leftEyeX + pupilX, eyeY + pupilY)
        )
        drawCircle(
            color = Color(0xFF0F172A),
            radius = pupilRadius,
            center = Offset(rightEyeX + pupilX, eyeY + pupilY)
        )

        // Pupil catchlight specular dot
        drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.35f,
            center = Offset(leftEyeX + pupilX - pupilRadius * 0.3f, eyeY + pupilY - pupilRadius * 0.3f)
        )
        drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.35f,
            center = Offset(rightEyeX + pupilX - pupilRadius * 0.3f, eyeY + pupilY - pupilRadius * 0.3f)
        )
    }

    // 3. Cheerful smile mouth
    val mouthY = h * 0.65f
    val mouthCenterX = w * 0.5f
    val mouthWidth = if (isBloomed) w * 0.36f else w * 0.22f
    val mouthHeight = if (isBloomed) h * 0.22f else h * 0.12f

    drawArc(
        color = Color(0xFF1E293B),
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(mouthCenterX - mouthWidth / 2f, mouthY - mouthHeight / 2f),
        size = Size(mouthWidth, mouthHeight),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}
