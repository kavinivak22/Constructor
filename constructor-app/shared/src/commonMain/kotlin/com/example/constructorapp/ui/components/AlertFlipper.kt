package com.example.constructorapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AlertVariant {
    WARNING,
    DANGER
}

data class DashboardAlert(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val variant: AlertVariant,
    val icon: ImageVector = Icons.Outlined.Warning
)

val sampleAlerts = listOf(
    DashboardAlert(
        id = "alert-1",
        title = "Low Stock Warning",
        description = "Cement stock is running low (20 bags left) for LVA 24A.",
        time = "2h ago",
        variant = AlertVariant.WARNING,
        icon = Icons.Outlined.Inventory2
    ),
    DashboardAlert(
        id = "alert-2",
        title = "Approval Overdue",
        description = "Purchase Order #7890 for LVA 24A is awaiting approval.",
        time = "1d ago",
        variant = AlertVariant.DANGER,
        icon = Icons.Outlined.ErrorOutline
    ),
    DashboardAlert(
        id = "alert-3",
        title = "Task Nearing Deadline",
        description = "Task 'Finalize electrical plan' for LVA 24A is due tomorrow.",
        time = "22h ago",
        variant = AlertVariant.WARNING,
        icon = Icons.Outlined.Schedule
    )
)

data class AlertStyleColors(
    val background: Color,
    val border: Color,
    val text: Color
)

private val FLIPPER_TOTAL_HEIGHT = 60.dp
private val FLIPPER_HALF_HEIGHT = 30.dp

fun getAlertColors(variant: AlertVariant): AlertStyleColors {
    return when (variant) {
        AlertVariant.WARNING -> AlertStyleColors(
            background = Color(0xFFFEFCE8),
            border = Color(0xFFFEF08A),
            text = Color(0xFF854D0E)
        )
        AlertVariant.DANGER -> AlertStyleColors(
            background = Color(0xFFFEF2F2),
            border = Color(0xFFFECACA),
            text = Color(0xFF991B1B)
        )
    }
}

@Composable
fun AlertCardBody(
    alert: DashboardAlert,
    onView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getAlertColors(alert.variant)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FLIPPER_TOTAL_HEIGHT)
            .background(colors.background)
            .clickable(onClick = onView)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = alert.icon,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = alert.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "• ${alert.time}",
                    fontSize = 10.sp,
                    color = colors.text.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = alert.description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.text.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Details",
            tint = colors.text.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AlertCardHalf(
    alert: DashboardAlert,
    isTop: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = getAlertColors(alert.variant)
    val shape = if (isTop) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    } else {
        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FLIPPER_HALF_HEIGHT)
            .clip(shape)
            .background(colors.background)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val heightPx = FLIPPER_TOTAL_HEIGHT.roundToPx()
                    val placeable = measurable.measure(
                        Constraints.fixed(constraints.maxWidth, heightPx)
                    )
                    val yOffset = if (isTop) 0 else -FLIPPER_HALF_HEIGHT.roundToPx()
                    layout(placeable.width, FLIPPER_HALF_HEIGHT.roundToPx()) {
                        placeable.placeRelative(0, yOffset)
                    }
                }
        ) {
            AlertCardBody(
                alert = alert,
                onView = {}
            )
        }
    }
}

@Composable
fun AllClearAlertCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(FLIPPER_TOTAL_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFECFDF5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Clear • No Active Alerts",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "All project milestones, stock, and site tasks are on track.",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlertFlipper(
    alerts: List<DashboardAlert>,
    onAlertDismiss: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    autoplayDelay: Long = 5000L
) {
    if (alerts.isEmpty()) {
        AllClearAlertCard(modifier = modifier)
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var nextIndex by remember { mutableIntStateOf(if (alerts.size > 1) 1 else 0) }
    var isFlipping by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val flipAngle = remember { Animatable(0f) }

    val activeCount = alerts.size
    val safeCurrentIndex = currentIndex.coerceIn(0, (activeCount - 1).coerceAtLeast(0))
    val safeNextIndex = nextIndex.coerceIn(0, (activeCount - 1).coerceAtLeast(0))

    val currentAlert = alerts[safeCurrentIndex]
    val nextAlert = alerts[safeNextIndex]
    val currentColors = getAlertColors(currentAlert.variant)

    fun triggerFlip(targetIndex: Int? = null) {
        if (isFlipping || activeCount <= 1) return
        val target = targetIndex ?: ((safeCurrentIndex + 1) % activeCount)
        nextIndex = target

        coroutineScope.launch {
            isFlipping = true
            flipAngle.snapTo(0f)
            flipAngle.animateTo(
                targetValue = -180f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.455f, 0.030f, 0.515f, 0.955f)
                )
            )
            currentIndex = target
            nextIndex = (target + 1) % activeCount
            flipAngle.snapTo(0f)
            isFlipping = false
        }
    }

    LaunchedEffect(activeCount, isPaused, isFlipping, autoplayDelay) {
        while (isActive) {
            delay(autoplayDelay)
            if (!isPaused && activeCount > 1 && !isFlipping) {
                triggerFlip()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FLIPPER_TOTAL_HEIGHT)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(currentColors.background)
                .border(1.dp, currentColors.border, RoundedCornerShape(12.dp))
                .pointerInput(activeCount) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = {
                            triggerFlip()
                        }
                    )
                }
        ) {
            if (!isFlipping) {
                AlertCardBody(
                    alert = currentAlert,
                    onView = { triggerFlip() }
                )
            } else {
                val currentRot = flipAngle.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FLIPPER_HALF_HEIGHT)
                        .align(Alignment.BottomCenter)
                ) {
                    AlertCardHalf(alert = currentAlert, isTop = false)
                    val landingShadowAlpha = ((abs(currentRot) / 180f) * 0.25f).coerceIn(0f, 0.25f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = landingShadowAlpha))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FLIPPER_HALF_HEIGHT)
                        .align(Alignment.TopCenter)
                ) {
                    AlertCardHalf(alert = nextAlert, isTop = true)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FLIPPER_HALF_HEIGHT)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            rotationX = currentRot
                            transformOrigin = TransformOrigin(0.5f, 1.0f)
                            cameraDistance = 2500f
                        }
                ) {
                    if (currentRot > -90f) {
                        AlertCardHalf(alert = currentAlert, isTop = true)
                        val shadowAlpha = (abs(currentRot) / 90f * 0.35f).coerceIn(0f, 0.35f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = shadowAlpha))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationX = 180f }
                        ) {
                            AlertCardHalf(alert = nextAlert, isTop = false)
                            val shadowAlpha = ((180f - abs(currentRot)) / 90f * 0.35f).coerceIn(0f, 0.35f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = shadowAlpha))
                            )
                        }
                    }
                }
            }
        }
    }
}
