package com.example.constructorapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onSubmit: (ProjectDto) -> Unit
) {
    val today = remember { "2026-11-13" }

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Residential") }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf("") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    // Material 3 Date Picker Dialogs
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = startDatePickerState.selectedDateMillis
                    if (millis != null) {
                        startDate = DateTimeUtil.formatEpochToDateString(millis)
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = endDatePickerState.selectedDateMillis
                    if (millis != null) {
                        endDate = DateTimeUtil.formatEpochToDateString(millis)
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    val parsedBudget = remember(budgetStr) { CurrencyFormatter.parseInr(budgetStr) ?: 0.0 }
    val isFormValid = name.trim().length >= 2 && parsedBudget > 0.0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassCardSurfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.4f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header Row: Icon Badge, Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Create New Project",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Draft site scope, budget & timeline",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SlateSurfaceSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = SlateDivider.copy(alpha = 0.5f))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo / Cover Placeholder
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PROJECT PHOTO / COVER",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandPrimary
                    )

                    Surface(
                        onClick = {
                            selectedPhotoUrl = "default_project_cover"
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = SlateSurfaceSubtle.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, SlateDivider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Upload Photo",
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedPhotoUrl != null) "Cover Selected ✓" else "Add Project Photo / Cover",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPhotoUrl != null) EmeraldSuccess else TextPrimary
                            )
                            Text(
                                text = "Tap to choose from gallery",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Project Identity
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PROJECT IDENTITY",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandPrimary
                    )

                    FluidGlossTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Project Name *",
                        placeholder = "e.g. Skyline Residency",
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Project Type Selector Pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PROJECT TYPE",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Residential", "Commercial", "Villa", "Renovation", "Interior").forEach { type ->
                                val isSelected = selectedType == type
                                Surface(
                                    onClick = { selectedType = type },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) BrandPrimaryContainer else SlateSurfaceSubtle,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BrandPrimary else SlateDivider
                                    )
                                ) {
                                    Text(
                                        text = type,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) BrandPrimary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateDivider.copy(alpha = 0.5f))

                // Budget & Schedule
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BUDGET & TIMELINE",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandPrimary
                    )

                    FluidGlossTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it },
                        label = "Target Budget (₹) *",
                        placeholder = "e.g. 2500000 or 25L or 1.5Cr",
                        leadingIcon = {
                            Text("₹", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 16.sp)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (parsedBudget > 0.0) {
                        Surface(
                            color = EmeraldSuccessContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "Live Estimate: ${CurrencyFormatter.formatCompact(parsedBudget)} (${CurrencyFormatter.formatFull(parsedBudget)})",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    // Clickable Date Picker Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { showStartDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (startDate.isNotBlank()) BrandPrimary.copy(alpha = 0.5f) else SlateDivider),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "START DATE",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = startDate.ifBlank { "Select Date" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (startDate.isNotBlank()) TextPrimary else TextMutedDark
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { showEndDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (endDate.isNotBlank()) BrandPrimary.copy(alpha = 0.5f) else SlateDivider),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TARGET END",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = endDate.ifBlank { "Select Date" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (endDate.isNotBlank()) TextPrimary else TextMutedDark
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateDivider.copy(alpha = 0.5f))

                // Client Details
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CLIENT DETAILS",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandPrimary
                    )

                    FluidGlossTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = "Client Name",
                        placeholder = "e.g. Apex Real Estate",
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FluidGlossTextField(
                        value = clientPhone,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 15) {
                                clientPhone = digitsOnly
                            }
                        },
                        label = "Client Contact Phone (Numbers only)",
                        placeholder = "e.g. 9876543210",
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FluidGlossTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "Location / City",
                        placeholder = "e.g. Coimbatore, Tamil Nadu",
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = SlateDivider.copy(alpha = 0.5f))

                // Scope & Specs
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SCOPE & DELIVERABLES",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandPrimary
                    )

                    FluidGlossTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description & Work Scope",
                        placeholder = "Structural specifications, deliverables, civil work summary...",
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bottom Sticky Actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FluidGlossButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = FluidGlossButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )

                    FluidGlossButton(
                        text = "Create Project",
                        onClick = {
                            if (isFormValid) {
                                val fullDescription = if (selectedType.isNotBlank()) {
                                    val prefix = "[$selectedType]"
                                    if (description.isNotBlank()) "$prefix ${description.trim()}" else prefix
                                } else {
                                    description.ifBlank { null }
                                }

                                onSubmit(
                                    ProjectDto(
                                        id = DateTimeUtil.randomUuid(),
                                        name = name.trim(),
                                        description = fullDescription,
                                        clientName = clientName.ifBlank { null },
                                        clientPhone = clientPhone.ifBlank { null },
                                        location = location.ifBlank { null },
                                        budget = parsedBudget,
                                        startDate = startDate.ifBlank { null },
                                        endDate = endDate.ifBlank { null },
                                        status = "active",
                                        progress = 0,
                                        companyId = "",
                                        thumbnailUrl = selectedPhotoUrl
                                    )
                                )
                            }
                        },
                        enabled = isFormValid,
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        variant = FluidGlossButtonVariant.Primary,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
