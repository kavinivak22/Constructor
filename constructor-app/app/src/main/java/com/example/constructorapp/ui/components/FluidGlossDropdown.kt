package com.example.constructorapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.ConstructorAppTheme
import com.example.constructorapp.theme.GlassBorderElevated
import com.example.constructorapp.theme.GlassCardSurfaceElevated
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.RoseDanger
import com.example.constructorapp.theme.TextMutedDark
import com.example.constructorapp.theme.TextPrimary
import com.example.constructorapp.theme.TextSecondary

/**
 * Floating frosted glass dropdown menu popup with 16.dp rounded corners,
 * specular top reflection line, GlassBorderElevated rim, and ambient indigo shadow.
 */
@Composable
fun FluidGlossDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    shape: Shape = RoundedCornerShape(16.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        properties = properties,
        modifier = modifier
    ) {
        Surface(
            shape = shape,
            color = GlassCardSurfaceElevated,
            border = BorderStroke(1.dp, GlassBorderElevated),
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    spotColor = Color(0x261F2687),
                    ambientColor = Color(0x1F1F2687)
                )
                .clip(shape)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top Specular Reflection Highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
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
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .verticalScroll(scrollState)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * High-polish menu item for FluidGlossDropdownMenu with hover/selected background
 * tint, active checkmark indicator, and typography hierarchy.
 */
@Composable
fun FluidGlossDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val backgroundColor = if (selected) BrandPrimary.copy(alpha = 0.10f) else Color.Transparent
    val titleColor = if (!enabled) TextMutedDark else if (selected) BrandPrimary else TextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                indication = ripple(bounded = true, color = BrandPrimary.copy(alpha = 0.15f)),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = if (!enabled) TextMutedDark else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        } else if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = BrandPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Anchored exposed dropdown selector combining a readOnly FluidGlossTextField
 * with a floating FluidGlossDropdownMenu and an animated rotating chevron.
 */
@Composable
fun <T> FluidGlossExposedDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it?.toString() ?: "" },
    itemSubtitle: ((T) -> String)? = null,
    label: String? = null,
    placeholder: String = "Select option...",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fluidGlossChevronRotation"
    )

    Box(modifier = modifier) {
        FluidGlossTextField(
            value = selectedItem?.let(itemLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label,
            placeholder = placeholder,
            isError = isError,
            errorMessage = errorMessage,
            leadingIcon = leadingIcon,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation),
                    tint = if (isError) RoseDanger else if (expanded) BrandPrimary else TextSecondary
                )
            },
            shape = shape,
            onClick = {
                if (enabled) expanded = !expanded
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    anchorWidth = coordinates.size.width
                }
        )

        FluidGlossDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = if (anchorWidth > 0) {
                Modifier.width(with(density) { anchorWidth.toDp() })
            } else Modifier
        ) {
            if (items.isEmpty()) {
                FluidGlossDropdownMenuItem(
                    text = "No options available",
                    onClick = {},
                    enabled = false
                )
            } else {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    FluidGlossDropdownMenuItem(
                        text = itemLabel(item),
                        subtitle = itemSubtitle?.invoke(item),
                        selected = isSelected,
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "FluidGlossExposedDropdown Preview", showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun FluidGlossExposedDropdownPreview() {
    ConstructorAppTheme {
        val options = listOf("Skyline Tower Phase 1", "Greenfield Villas", "Commercial Plaza B")
        var selected by remember { mutableStateOf<String?>(options.first()) }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FluidGlossExposedDropdown(
                items = options,
                selectedItem = selected,
                onItemSelected = { selected = it },
                label = "Active Construction Project",
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
            )

            FluidGlossExposedDropdown(
                items = listOf("Supervisor", "Mason", "Electrician", "Surveyor"),
                selectedItem = null,
                onItemSelected = {},
                label = "Role Classification",
                placeholder = "Choose role assignment",
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
        }
    }
}
