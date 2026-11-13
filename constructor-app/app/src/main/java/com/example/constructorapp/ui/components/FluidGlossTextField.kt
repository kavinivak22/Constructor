package com.example.constructorapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.ConstructorAppTheme
import com.example.constructorapp.theme.GlassBorder
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.RoseDanger
import com.example.constructorapp.theme.TextMutedDark
import com.example.constructorapp.theme.TextPrimary
import com.example.constructorapp.theme.TextSecondary

/**
 * Production-grade tactile FluidGlossTextField with 85% frosted glass fill,
 * top specular reflection line, active focus glow halo, and full validation state support.
 */
@Composable
fun FluidGlossTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(14.dp),
    onClick: (() -> Unit)? = null,
    labelComposable: (@Composable () -> Unit)? = null,
    placeholderComposable: (@Composable () -> Unit)? = null
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val localFocusRequester = remember { FocusRequester() }

    // 85% frosted white unfocused fill allowing background blobs to bleed through,
    // 97% white when focused for high contrast, soft red tint on error
    val targetContainerColor = when {
        !enabled -> Color(0x80F1F5F9)
        isError -> Color(0xFFFFF5F5)
        isFocused -> Color(0xF7FFFFFF)
        else -> Color(0xD9FFFFFF)
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(150),
        label = "fluidGlossContainerColor"
    )

    // Border stroke with BrandPrimary focus ring or RoseDanger error ring
    val borderStroke = when {
        isError -> BorderStroke(1.5.dp, RoseDanger)
        isFocused -> BorderStroke(1.5.dp, BrandPrimary)
        else -> BorderStroke(1.dp, GlassBorder)
    }

    // Outer glow halo when focused
    val shadowElevation = when {
        !enabled -> 0.dp
        isError -> 2.dp
        isFocused -> 4.dp
        else -> 1.dp
    }

    val shadowSpotColor = when {
        !enabled -> Color.Transparent
        isError -> RoseDanger.copy(alpha = 0.25f)
        isFocused -> BrandPrimary.copy(alpha = 0.25f)
        else -> Color(0x0F1F2687)
    }

    val shadowAmbientColor = when {
        !enabled -> Color.Transparent
        isError -> RoseDanger.copy(alpha = 0.12f)
        isFocused -> BrandPrimary.copy(alpha = 0.12f)
        else -> Color(0x0F1F2687)
    }

    // Top specular reflection line (1.dp horizontal gradient)
    val specularBrush = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            Color(0xB3FFFFFF),
            Color.Transparent
        )
    )

    // Label styling
    val labelColor = when {
        !enabled -> TextMutedDark
        isError -> RoseDanger
        isFocused -> BrandPrimary
        else -> TextSecondary
    }

    Column(modifier = modifier) {
        // Label Section
        if (labelComposable != null) {
            Box(modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                labelComposable()
            }
        } else if (!label.isNullOrBlank()) {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        // Frosted Glass Input Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    spotColor = shadowSpotColor,
                    ambientColor = shadowAmbientColor
                )
                .clip(shape)
                .alpha(if (enabled) 1f else 0.6f)
                .then(
                    if (onClick != null && enabled) {
                        Modifier.clickable(onClick = onClick)
                    } else if (enabled && !readOnly) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            localFocusRequester.requestFocus()
                        }
                    } else Modifier
                ),
            shape = shape,
            color = containerColor,
            border = borderStroke,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top Specular Reflection Highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(specularBrush)
                )

                // Input Core
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(localFocusRequester),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (enabled) TextPrimary else TextMutedDark
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(if (isError) RoseDanger else BrandPrimary),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 46.dp)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (leadingIcon != null) {
                                CompositionLocalProvider(
                                    LocalContentColor provides (if (isError) RoseDanger else if (isFocused) BrandPrimary else TextSecondary)
                                ) {
                                    leadingIcon()
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (value.isEmpty()) {
                                    if (placeholderComposable != null) {
                                        placeholderComposable()
                                    } else if (!placeholder.isNullOrBlank()) {
                                        Text(
                                            text = placeholder,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = TextMutedDark,
                                            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                innerTextField()
                            }

                            if (trailingIcon != null) {
                                Spacer(modifier = Modifier.width(10.dp))
                                CompositionLocalProvider(
                                    LocalContentColor provides (if (isError) RoseDanger else if (isFocused) BrandPrimary else TextSecondary)
                                ) {
                                    trailingIcon()
                                }
                            }
                        }
                    }
                )
            }
        }

        // Supporting Error Message
        if (isError && !errorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RoseDanger,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .height(12.dp)
                )
                Text(
                    text = errorMessage,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = RoseDanger
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "FluidGlossTextField States", showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun FluidGlossTextFieldStatesPreview() {
    ConstructorAppTheme {
        var textValue by remember { mutableStateOf("Structural Framing Log") }
        var emptyValue by remember { mutableStateOf("") }
        var passwordValue by remember { mutableStateOf("secret123") }
        var passwordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FluidGlossTextField(
                value = emptyValue,
                onValueChange = { emptyValue = it },
                label = "Project Name",
                placeholder = "e.g. Skyline Tower Block B",
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )

            FluidGlossTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = "Worklog Activity",
                placeholder = "Enter activity title"
            )

            FluidGlossTextField(
                value = passwordValue,
                onValueChange = { passwordValue = it },
                label = "Access Key",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Toggle password")
                    }
                }
            )

            FluidGlossTextField(
                value = "Invalid Material Ratio",
                onValueChange = {},
                label = "Mix Ratio",
                isError = true,
                errorMessage = "Mix ratio must adhere to IS 456 M20 standard"
            )

            FluidGlossTextField(
                value = "Read Only Site Reference",
                onValueChange = {},
                label = "Assigned Site",
                readOnly = true,
                enabled = false
            )
        }
    }
}
