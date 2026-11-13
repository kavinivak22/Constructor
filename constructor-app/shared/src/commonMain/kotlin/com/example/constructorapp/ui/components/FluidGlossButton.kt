package com.example.constructorapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.BrandPrimaryDark
import com.example.constructorapp.theme.EmeraldSuccess
import com.example.constructorapp.theme.GlassBorder
import com.example.constructorapp.theme.GlassShadow
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.RoseDanger
import com.example.constructorapp.theme.TextPrimary

enum class FluidGlossButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Destructive,
    Success,
    Ghost
}

enum class FluidGlossButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val textStyle: TextStyle,
    val iconSize: Dp
) {
    Small(
        height = 36.dp,
        horizontalPadding = 12.dp,
        textStyle = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        iconSize = 14.dp
    ),
    Medium(
        height = 44.dp,
        horizontalPadding = 16.dp,
        textStyle = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        iconSize = 16.dp
    ),
    Large(
        height = 52.dp,
        horizontalPadding = 24.dp,
        textStyle = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        iconSize = 18.dp
    )
}

/**
 * Production-grade tactile FluidGlossButton with specular top highlight,
 * ambient drop shadows, physical press physics, and full state support.
 */
@Composable
fun FluidGlossButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FluidGlossButtonVariant = FluidGlossButtonVariant.Primary,
    size: FluidGlossButtonSize = FluidGlossButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = size.horizontalPadding),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteractive = enabled && !loading

    // Tactile press physics: scale down to 0.975f on finger press
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.975f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "fluidGlossButtonScale"
    )

    // Background brush formula
    val backgroundBrush: Brush = when (variant) {
        FluidGlossButtonVariant.Primary -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFA043),
                BrandPrimary,
                BrandPrimaryDark
            )
        )
        FluidGlossButtonVariant.Secondary -> Brush.verticalGradient(
            listOf(
                Color(0xF5FFFFFF),
                Color(0xCCFFFFFF)
            )
        )
        FluidGlossButtonVariant.Outlined -> SolidColor(Color(0x1AFFFFFF))
        FluidGlossButtonVariant.Destructive -> Brush.verticalGradient(
            listOf(
                Color(0xFFFF6B6B),
                RoseDanger,
                Color(0xFFDC2626)
            )
        )
        FluidGlossButtonVariant.Success -> Brush.verticalGradient(
            listOf(
                Color(0xFF34D399),
                EmeraldSuccess,
                Color(0xFF059669)
            )
        )
        FluidGlossButtonVariant.Ghost -> SolidColor(Color.Transparent)
    }

    // Border stroke formula
    val borderStroke: BorderStroke? = when (variant) {
        FluidGlossButtonVariant.Primary -> BorderStroke(1.dp, Color(0x66FFFFFF))
        FluidGlossButtonVariant.Secondary -> BorderStroke(1.dp, GlassBorder)
        FluidGlossButtonVariant.Outlined -> BorderStroke(1.dp, Color(0x80FFFFFF))
        FluidGlossButtonVariant.Destructive -> BorderStroke(1.dp, Color(0x4DFFFFFF))
        FluidGlossButtonVariant.Success -> BorderStroke(1.dp, Color(0x4DFFFFFF))
        FluidGlossButtonVariant.Ghost -> null
    }

    // Top specular reflection line brush
    val specularBrush: Brush? = when (variant) {
        FluidGlossButtonVariant.Ghost -> null
        FluidGlossButtonVariant.Outlined -> Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color(0x80FFFFFF),
                Color.Transparent
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color(0xB3FFFFFF),
                Color.Transparent
            )
        )
    }

    // Content & icon colors
    val contentColor = when (variant) {
        FluidGlossButtonVariant.Primary,
        FluidGlossButtonVariant.Destructive,
        FluidGlossButtonVariant.Success -> Color.White
        FluidGlossButtonVariant.Secondary,
        FluidGlossButtonVariant.Outlined,
        FluidGlossButtonVariant.Ghost -> TextPrimary
    }

    // Ambient drop shadow styling
    val shadowElevation = if (isInteractive) {
        when (variant) {
            FluidGlossButtonVariant.Primary,
            FluidGlossButtonVariant.Destructive,
            FluidGlossButtonVariant.Success -> if (isPressed) 2.dp else 4.dp
            FluidGlossButtonVariant.Secondary -> if (isPressed) 1.dp else 2.dp
            FluidGlossButtonVariant.Outlined,
            FluidGlossButtonVariant.Ghost -> 0.dp
        }
    } else 0.dp

    val shadowSpotColor = when (variant) {
        FluidGlossButtonVariant.Primary -> BrandPrimary.copy(alpha = 0.35f)
        FluidGlossButtonVariant.Destructive -> RoseDanger.copy(alpha = 0.35f)
        FluidGlossButtonVariant.Success -> EmeraldSuccess.copy(alpha = 0.35f)
        FluidGlossButtonVariant.Secondary -> GlassShadow
        FluidGlossButtonVariant.Outlined,
        FluidGlossButtonVariant.Ghost -> Color.Transparent
    }

    val shadowAmbientColor = when (variant) {
        FluidGlossButtonVariant.Primary,
        FluidGlossButtonVariant.Destructive,
        FluidGlossButtonVariant.Success -> Color(0x1F1F2687)
        FluidGlossButtonVariant.Secondary -> GlassShadow
        FluidGlossButtonVariant.Outlined,
        FluidGlossButtonVariant.Ghost -> Color.Transparent
    }

    // Ripple color
    val rippleColor = when (variant) {
        FluidGlossButtonVariant.Primary,
        FluidGlossButtonVariant.Destructive,
        FluidGlossButtonVariant.Success -> Color.White.copy(alpha = 0.35f)
        FluidGlossButtonVariant.Secondary,
        FluidGlossButtonVariant.Outlined,
        FluidGlossButtonVariant.Ghost -> BrandPrimary.copy(alpha = 0.15f)
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowSpotColor,
                ambientColor = shadowAmbientColor
            )
            .clip(shape)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = isInteractive,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = rippleColor),
                onClick = onClick
            ),
        shape = shape,
        color = Color.Transparent,
        border = borderStroke,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .defaultMinSize(minHeight = size.height)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            // Top specular reflection line
            if (specularBrush != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.2.dp)
                        .align(Alignment.TopCenter)
                        .background(specularBrush)
                )
            }

            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(value = size.textStyle.copy(color = contentColor)) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(size.iconSize),
                                color = contentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (leadingIcon != null) {
                            leadingIcon()
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        content()

                        if (!loading && trailingIcon != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            trailingIcon()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Text-based overload of FluidGlossButton for standard button usage.
 */
@Composable
fun FluidGlossButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FluidGlossButtonVariant = FluidGlossButtonVariant.Primary,
    size: FluidGlossButtonSize = FluidGlossButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    FluidGlossButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        shape = shape,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
