package com.example.constructorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.BrandPrimaryContainer
import com.example.constructorapp.theme.GlassBorderElevated
import com.example.constructorapp.theme.GlassCardSurfaceElevated
import com.example.constructorapp.theme.OutfitFontFamily
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.RoseDanger
import com.example.constructorapp.theme.TextPrimary
import com.example.constructorapp.theme.TextSecondary

/**
 * Production-grade custom FluidGlossDialog using DialogProperties(usePlatformDefaultWidth = false)
 * with a 0x59000000 dark scrim backdrop, 95% frosted glass shell (GlassCardSurfaceElevated),
 * specular border, top specular reflection highlight line, 16.dp ambient shadow,
 * header with icon badge, scrollable body with imePadding(), and action footer.
 */
@Composable
fun FluidGlossDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = BrandPrimary,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    showCloseButton: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    loading: Boolean = false,
    contentMaxHeightRatio: Float = 0.88f,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!loading) onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !loading,
            dismissOnBackPress = !loading
        )
    ) {
        // Dark Scrim Backdrop with BoxWithConstraints for multiplatform safe height
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x59000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!loading) onDismissRequest()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val calculatedMaxHeight = maxHeight * contentMaxHeightRatio

            Surface(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
                    .heightIn(max = calculatedMaxHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        spotColor = Color(0x261F2687),
                        ambientColor = Color(0x1F1F2687)
                    )
                    .clip(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept and consume inner clicks
                    )
                    .imePadding(),
                shape = shape,
                color = GlassCardSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorderElevated),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Top Specular Inset Reflection Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .align(Alignment.TopCenter)
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        // Header Section
                        if (!title.isNullOrBlank() || icon != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (icon != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = BrandPrimaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    if (!title.isNullOrBlank()) {
                                        Text(
                                            text = title,
                                            fontFamily = OutfitFontFamily,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            lineHeight = 24.sp
                                        )
                                    }
                                    if (!subtitle.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                if (showCloseButton) {
                                    IconButton(
                                        onClick = onDismissRequest,
                                        enabled = !loading,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close dialog",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Scrollable Content Area with max height constraint
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                content()
                            }
                        }

                        // Action Button Footer
                        if (confirmButton != null || dismissButton != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dismissButton != null && confirmButton != null) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        dismissButton()
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        confirmButton()
                                    }
                                } else {
                                    dismissButton?.let { it() }
                                    confirmButton?.let { it() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-polish replacement for standard flat Material 3 AlertDialogs across the app,
 * supporting primary or destructive confirmation, cancellation, and icon badges.
 */
@Composable
fun FluidGlossAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    FluidGlossDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon,
        iconTint = if (isDestructive) RoseDanger else BrandPrimary,
        loading = loading,
        confirmButton = {
            FluidGlossButton(
                text = confirmText,
                onClick = onConfirm,
                variant = if (isDestructive) FluidGlossButtonVariant.Destructive else FluidGlossButtonVariant.Primary,
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = dismissText,
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
    ) {
        Text(
            text = text,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
