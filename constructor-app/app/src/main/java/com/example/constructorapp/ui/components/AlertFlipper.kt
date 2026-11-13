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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
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
import androidx.compose.ui.platform.LocalDensity
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

// Webapp Exact Alert Color Palette
// warning: "bg-yellow-50 border-yellow-200 text-yellow-800"
// danger: "bg-red-50 border-red-200 text-red-800"
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
            background = Color(0xFFFEFCE8), // yellow-50
            border = Color(0xFFFEF08A),     // yellow-200
            text = Color(0xFF854D0E)        // yellow-800
        )
        AlertVariant.DANGER -> AlertStyleColors(
            background = Color(0xFFFEF2F2), // red-50
            border = Color(0xFFFECACA),     // red-200
            text = Color(0xFF991B1B)        // red-800
        )
    }
}

/**
 * Compact Split-Flap Card Body (60.dp)
 * Modern, clean layout with squircle icon, title + variant + timestamp, 1-line description,
 * and trailing chevron. Whole card is tap-to-inspect.
 */
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
        // Left: Squircle Icon Pill
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

        // Center: Title + Variant Badge + Time on row 1, Description on row 2
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = alert.title,
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "• ${alert.time}",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.sp,
                    color = colors.text.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = alert.description,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.text.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Trailing subtle chevron indicator
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Details",
            tint = colors.text.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Clean Half-Slice Helper: Renders exact top or bottom half slice of an AlertCardBody
 * without manual pixel offset or clipping artifacts.
 */
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
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "All project milestones, stock, and site tasks are on track.",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Split-Flap Display Component
 * Exact match with webapp (src/components/dashboard/alert-flipper.tsx):
 * - 86px (86.dp) height split into two 43.dp halves
 * - 600ms flip animation with CubicBezierEasing(0.455f, 0.030f, 0.515f, 0.955f)
 * - 3D rotationX from 0 to -180 degrees
 * - Static background layers + moving flap with front/back lighting shadows
 * - Left/Right manual arrows
 * - Bottom controls bar with Pause status and animated dot pills
 */
@Composable
fun AlertFlipper(
    alerts: List<DashboardAlert>,
    onAlertDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoplayDelay: Long = 5000L,
    onRestoreAlerts: () -> Unit = {}
) {
    if (alerts.isEmpty()) {
        AllClearAlertCard(modifier = modifier)
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var nextIndex by remember { mutableIntStateOf(if (alerts.size > 1) 1 else 0) }
    var isFlipping by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var inspectingAlert by remember { mutableStateOf<DashboardAlert?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val flipAngle = remember { Animatable(0f) }

    // Bound indices safely
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

    // Autoplay Loop
    LaunchedEffect(activeCount, isPaused, isFlipping, autoplayDelay) {
        while (isActive) {
            delay(autoplayDelay)
            if (!isPaused && activeCount > 1 && !isFlipping) {
                triggerFlip()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Outer Split-Flap Display Container (Fixed FLIPPER_TOTAL_HEIGHT)
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
                            inspectingAlert = currentAlert
                        }
                    )
                }
        ) {
            if (!isFlipping) {
                // RESTING STATE: Full uninterrupted card body
                AlertCardBody(
                    alert = currentAlert,
                    onView = { inspectingAlert = currentAlert }
                )
            } else {
                // FLIPPING STATE: 3D mechanical calendar / split-flap flip animation
                val currentRot = flipAngle.value

                // LAYER 1: STATIC BOTTOM HALF (Underneath: Shows currentAlert bottom half until flap covers it)
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

                // LAYER 2: STATIC TOP HALF (Revealed underneath as the top flap flips forward like a calendar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FLIPPER_HALF_HEIGHT)
                        .align(Alignment.TopCenter)
                ) {
                    AlertCardHalf(alert = nextAlert, isTop = true)
                }

                // LAYER 3: FLIPPER (The Moving Half Flap swinging forward and down over the bottom)
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
                        // FRONT FACE: Upper half of currentAlert rotating forward towards viewer
                        AlertCardHalf(alert = currentAlert, isTop = true)
                        val shadowAlpha = ((abs(currentRot) / 90f) * 0.35f).coerceIn(0f, 0.35f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = shadowAlpha))
                        )
                    } else {
                        // BACK FACE: Lower half of nextAlert landing face-up over the bottom half
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationX = 180f }
                        ) {
                            AlertCardHalf(alert = nextAlert, isTop = false)
                            val shadowAlpha = (((180f - abs(currentRot)) / 90f) * 0.35f).coerceIn(0f, 0.35f)
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

        if (activeCount > 1) {
            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Controls Bar: Index Count, Animated Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Status / Pause Indicator
                if (isPaused) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = "Paused",
                            tint = BrandPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Paused",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary
                        )
                    }
                } else {
                    Text(
                        text = "${safeCurrentIndex + 1} of $activeCount",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // Center: Progress Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    alerts.forEachIndexed { index, _ ->
                        val isActive = index == safeCurrentIndex
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 16.dp else 5.dp,
                            animationSpec = tween(durationMillis = 250),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isActive) BrandPrimary else Color(0xFFCBD5E1))
                                .clickable { triggerFlip(index) }
                        )
                    }
                }

                // Right: Clean subtle notification icon
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.45f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }

    // Alert Inspection Modal Dialog (matching webapp Dialog)
    if (inspectingAlert != null) {
        val alert = inspectingAlert!!
        val colors = getAlertColors(alert.variant)

        FluidGlossDialog(
            onDismissRequest = { inspectingAlert = null },
            title = alert.title,
            icon = alert.icon,
            iconTint = colors.text,
            confirmButton = {
                FluidGlossButton(
                    text = "Dismiss Alert",
                    onClick = {
                        onAlertDismiss(alert.id)
                        inspectingAlert = null
                        if (currentIndex >= activeCount - 1) {
                            currentIndex = (activeCount - 2).coerceAtLeast(0)
                        }
                    },
                    variant = FluidGlossButtonVariant.Destructive
                )
            },
            dismissButton = {
                FluidGlossButton(
                    text = "Got it",
                    onClick = { inspectingAlert = null },
                    variant = FluidGlossButtonVariant.Secondary
                )
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.border)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = alert.variant.name.uppercase(),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = alert.time,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = alert.description,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Actionable reminder logged in Constructor site registry.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
