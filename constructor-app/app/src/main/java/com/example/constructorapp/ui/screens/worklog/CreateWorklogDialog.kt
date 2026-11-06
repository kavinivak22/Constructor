package com.example.constructorapp.ui.screens.worklog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.constructorapp.data.model.worklog.*
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.MixRatioHelperWidget
import com.example.constructorapp.ui.viewmodel.WorklogUiState
import com.example.constructorapp.ui.viewmodel.WorklogViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorklogDialog(
    state: WorklogUiState,
    viewModel: WorklogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = state.editingWorklogId != null

    // Media Picker for Photo Attachment (up to 10 photos matching webapp)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(uris, context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(24.dp))
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ─── Header: Clean Webapp DialogHeader ─────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (isEditing) "Edit Daily Worklog" else "New Daily Worklog",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Record site activity, labor, materials, and photos.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // ─── Segmented Tabs Strip: Matching Webapp TabsList ───────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        WorklogCreationTab.entries.forEach { tab ->
                            val isSelected = state.activeCreationTab == tab
                            val countBadge = when (tab) {
                                WorklogCreationTab.DETAILS -> 0
                                WorklogCreationTab.LABOR -> state.draftLabor.size
                                WorklogCreationTab.MATERIALS -> state.draftMaterials.size
                                WorklogCreationTab.PHOTOS -> state.draftPhotos.size
                            }

                            val tabIcon = when (tab) {
                                WorklogCreationTab.DETAILS -> Icons.Default.CalendarToday
                                WorklogCreationTab.LABOR -> Icons.Default.Group
                                WorklogCreationTab.MATERIALS -> Icons.Default.Inventory2
                                WorklogCreationTab.PHOTOS -> Icons.Default.Image
                            }

                            val tabLabel = when (tab) {
                                WorklogCreationTab.DETAILS -> "Details"
                                WorklogCreationTab.LABOR -> "Labor"
                                WorklogCreationTab.MATERIALS -> "Material"
                                WorklogCreationTab.PHOTOS -> "Photos"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) Color(0x20000000) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setCreationTab(tab) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = tabLabel,
                                        tint = if (isSelected) BrandPrimary else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )

                                    Text(
                                        text = tabLabel,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        maxLines = 1
                                    )

                                    if (countBadge > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) BrandPrimary else Color(0xFFCBD5E1))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$countBadge",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Tab Content Body ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (state.activeCreationTab) {
                        WorklogCreationTab.DETAILS -> TabDetailsWeb(state, viewModel)
                        WorklogCreationTab.LABOR -> TabLaborWeb(state, viewModel)
                        WorklogCreationTab.MATERIALS -> TabMaterialsWeb(state, viewModel)
                        WorklogCreationTab.PHOTOS -> TabPhotosWeb(state, viewModel, onPickPhotos = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        })
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // ─── Sticky Footer Controls: Summary + Step Buttons (Mobile-First Layout) ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top: Live Summary Ticker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Summary:", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.draftLabor.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("teams •", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.draftMaterials.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("materials •", fontSize = 11.sp, color = TextSecondary)
                            Text("${state.draftPhotos.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("photos", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Bottom: Two-Button Action Row with Proper Weights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentTab = state.activeCreationTab

                        // Left Action: Cancel or Back
                        if (currentTab == WorklogCreationTab.DETAILS) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Cancel", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val prevIndex = (currentTab.ordinal - 1).coerceAtLeast(0)
                                    viewModel.setCreationTab(WorklogCreationTab.entries[prevIndex])
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Right Action: Continue / Submit
                        when (currentTab) {
                            WorklogCreationTab.DETAILS -> {
                                Button(
                                    onClick = { viewModel.setCreationTab(WorklogCreationTab.LABOR) },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Continue to Labor", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                }
                            }
                            WorklogCreationTab.LABOR -> {
                                Button(
                                    onClick = { viewModel.setCreationTab(WorklogCreationTab.MATERIALS) },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Continue to Materials", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                }
                            }
                            WorklogCreationTab.MATERIALS -> {
                                Button(
                                    onClick = { viewModel.setCreationTab(WorklogCreationTab.PHOTOS) },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Continue to Photos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                }
                            }
                            WorklogCreationTab.PHOTOS -> {
                                Button(
                                    onClick = {
                                        viewModel.submitWorklog(context) {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    enabled = !state.isSubmitting && state.draftTitle.isNotBlank(),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (state.isSubmitting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saving...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    } else {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEditing) "Update Worklog" else "Save Worklog", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Error message banner if any
                state.errorMessage?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoseDanger.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = err,
                            color = RoseDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: DETAILS (Webapp Exact Match)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabDetailsWeb(
    state: WorklogUiState,
    viewModel: WorklogViewModel
) {
    val context = LocalContext.current
    var projectExpanded by remember { mutableStateOf(false) }

    val selectedProject = state.projects.find { it.id == state.selectedProjectId }
        ?: state.projects.firstOrNull()

    // Native Material 3 Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember(state.draftDate) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(state.draftDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val formattedDate = sdf.format(Date(millis))
                        viewModel.updateDraftDetails(state.draftTitle, formattedDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Project Selection Dropdown
                    Column {
                        Text(
                            text = "Project *",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = projectExpanded,
                            onExpandedChange = { projectExpanded = !projectExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProject?.name ?: "Select a project...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = projectExpanded,
                                onDismissRequest = { projectExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                state.projects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = proj.name,
                                                fontWeight = if (proj.id == selectedProject?.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (proj.id == selectedProject?.id) BrandPrimary else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.filterByProject(proj.id)
                                            projectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Work Title Field
                    Column {
                        Text(
                            text = "Work Title *",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.draftTitle,
                            onValueChange = { viewModel.updateDraftDetails(it, state.draftDate) },
                            placeholder = { Text("e.g. Foundation Pouring, Brickwork & Plastering", fontSize = 13.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    // Date Picker Trigger Field
                    Column {
                        Text(
                            text = "Date *",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = state.draftDate.ifBlank { "Pick a date" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Summary Card of Current Draft
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Worklog Setup Summary",
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Contractor Labor Teams:", fontSize = 12.sp, color = TextSecondary)
                        Text("${state.draftLabor.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Materials Logged:", fontSize = 12.sp, color = TextSecondary)
                        Text("${state.draftMaterials.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Site Photos Attached:", fontSize = 12.sp, color = TextSecondary)
                        Text("${state.draftPhotos.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: LABOR (Webapp Exact Match: Multi-Team, Classification, Categories)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabLaborWeb(
    state: WorklogUiState,
    viewModel: WorklogViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Ribbon Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Labor Teams",
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Who worked on site today?",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = {
                        viewModel.addLaborTeam(
                            WorklogLaborInput(
                                contractorName = "",
                                category = "Masonry",
                                payoutClass = "NMR",
                                workType = "nmr",
                                paymentStatus = "Pending",
                                workers = listOf(WorkerCountInput(workerType = "Mason", count = 1))
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Team", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Empty state
        if (state.draftLabor.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = "No labor entries yet",
                            fontFamily = OutfitFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Add contractors, attendance headcount, or piece-rate measurements.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.addLaborTeam(
                                    WorklogLaborInput(
                                        contractorName = "",
                                        category = "Masonry",
                                        payoutClass = "NMR",
                                        workType = "nmr",
                                        paymentStatus = "Pending",
                                        workers = listOf(WorkerCountInput(workerType = "Mason", count = 1))
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.3f))
                        ) {
                            Text("Add First Team", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Interactive Team Cards
            itemsIndexed(state.draftLabor) { index, team ->
                LaborTeamCardWeb(
                    index = index,
                    team = team,
                    contractors = state.contractors,
                    onUpdate = { updated -> viewModel.updateLaborTeam(index, updated) },
                    onRemove = { viewModel.removeLaborTeam(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaborTeamCardWeb(
    index: Int,
    team: WorklogLaborInput,
    contractors: List<com.example.constructorapp.data.remote.ContractorDto>,
    onUpdate: (WorklogLaborInput) -> Unit,
    onRemove: () -> Unit
) {
    val isRate = team.payoutClass.equals("Rate", ignoreCase = true)
    var contractorDropdownOpen by remember { mutableStateOf(false) }
    var paymentDropdownOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Header: Team Badge + NMR vs Rate Toggle ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TEAM #${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }

                // Work Classification Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isRate) BrandPrimary else Color.Transparent)
                            .clickable {
                                onUpdate(team.copy(payoutClass = "NMR", workType = "nmr"))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NMR (Daily)",
                            fontSize = 11.sp,
                            fontWeight = if (!isRate) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRate) Color.White else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isRate) EmeraldSuccess else Color.Transparent)
                            .clickable {
                                onUpdate(team.copy(payoutClass = "Rate", workType = "rate"))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Rate (Piece)",
                            fontSize = 11.sp,
                            fontWeight = if (isRate) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRate) Color.White else TextSecondary
                        )
                    }
                }
            }

            // ─── Contractor / Team (Full Width Row) ──────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Contractor / Team *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = contractorDropdownOpen,
                    onExpandedChange = { contractorDropdownOpen = !contractorDropdownOpen }
                ) {
                    OutlinedTextField(
                        value = team.contractorName,
                        onValueChange = { onUpdate(team.copy(contractorName = it)) },
                        placeholder = { Text("Select or type contractor name", fontSize = 12.sp, color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contractorDropdownOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                    if (contractors.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = contractorDropdownOpen,
                            onDismissRequest = { contractorDropdownOpen = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            contractors.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        onUpdate(team.copy(contractorName = c.name, category = c.trade ?: team.category))
                                        contractorDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─── Trade Category & Payment Status (Side by Side 50/50 Row) ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Trade Category
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trade Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = team.category,
                        onValueChange = { onUpdate(team.copy(category = it)) },
                        placeholder = { Text("e.g. Masonry", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // Payment Status
                Column(modifier = Modifier.weight(1f)) {
                    Text("Payment Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = paymentDropdownOpen,
                        onExpandedChange = { paymentDropdownOpen = !paymentDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = team.paymentStatus,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDropdownOpen) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = when (team.paymentStatus) {
                                    "Paid" -> EmeraldSuccess
                                    "Pending" -> AmberWarning
                                    else -> SkyInfo
                                },
                                unfocusedTextColor = when (team.paymentStatus) {
                                    "Paid" -> EmeraldSuccess
                                    "Pending" -> AmberWarning
                                    else -> SkyInfo
                                },
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = paymentDropdownOpen,
                            onDismissRequest = { paymentDropdownOpen = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            listOf("Pending", "On Payday", "Paid").forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = status,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (status) {
                                                "Paid" -> EmeraldSuccess
                                                "Pending" -> AmberWarning
                                                else -> SkyInfo
                                            }
                                        )
                                    },
                                    onClick = {
                                        onUpdate(team.copy(paymentStatus = status))
                                        paymentDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Description / Scope of Work
            Column {
                Text("Description / Scope of Work", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = team.workDescription,
                    onValueChange = { onUpdate(team.copy(workDescription = it)) },
                    placeholder = { Text("What did they work on today? e.g. North wall brickwork & lintel beam", fontSize = 12.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    maxLines = 3
                )
            }

            // If Rate Work: Quantity, Unit & Rate
            if (isRate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rate Work Output Measurement",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            if ((team.totalPayout ?: 0.0) > 0.0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSuccess)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.0f", team.totalPayout)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Row 1: Output Qty + Unit (Side by Side 50/50)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = team.workDoneQuantity?.toString() ?: "",
                                onValueChange = {
                                    val q = it.toDoubleOrNull()
                                    val total = if (q != null && team.payoutRate != null) q * team.payoutRate else null
                                    onUpdate(team.copy(workDoneQuantity = q, totalPayout = total))
                                },
                                label = { Text("Output Quantity", fontSize = 11.sp) },
                                placeholder = { Text("e.g. 500", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = team.workDoneUnit ?: "",
                                onValueChange = { onUpdate(team.copy(workDoneUnit = it)) },
                                label = { Text("Work Done Unit", fontSize = 11.sp) },
                                placeholder = { Text("sq.ft, cum, rft", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        // Row 2: Rate per Unit (₹) + Live Estimated Payout Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = team.payoutRate?.toString() ?: "",
                                onValueChange = {
                                    val r = it.toDoubleOrNull()
                                    val total = if (r != null && team.workDoneQuantity != null) team.workDoneQuantity * r else null
                                    onUpdate(team.copy(payoutRate = r, totalPayout = total))
                                },
                                label = { Text("Rate per Unit (₹)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. 45", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            // Clean Estimated Payout Badge Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("Estimated Total", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = if ((team.totalPayout ?: 0.0) > 0.0) {
                                            "₹${String.format(Locale.US, "%,.0f", team.totalPayout)}"
                                        } else {
                                            "₹ 0.00"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Worker Counts Attendance Section ────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Worker Counts Attendance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    TextButton(
                        onClick = {
                            val nextRole = if (team.workers.any { it.workerType == "Mason" }) "Helper" else "Mason"
                            val updated = team.workers + WorkerCountInput(workerType = nextRole, count = 1)
                            onUpdate(team.copy(workers = updated))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                }

                // List of Worker Roles
                team.workers.forEachIndexed { wIdx, worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = worker.workerType,
                            onValueChange = { newType ->
                                val updated = team.workers.mapIndexed { idx, w ->
                                    if (idx == wIdx) w.copy(workerType = newType) else w
                                }
                                onUpdate(team.copy(workers = updated))
                            },
                            placeholder = { Text("Role e.g. Mason", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Stepper for Count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    if (worker.count > 0) {
                                        val updated = team.workers.mapIndexed { idx, w ->
                                            if (idx == wIdx) w.copy(count = w.count - 1) else w
                                        }
                                        onUpdate(team.copy(workers = updated))
                                    }
                                },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Text(
                                text = "${worker.count}",
                                modifier = Modifier.padding(horizontal = 10.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )

                            OutlinedButton(
                                onClick = {
                                    val updated = team.workers.mapIndexed { idx, w ->
                                        if (idx == wIdx) w.copy(count = w.count + 1) else w
                                    }
                                    onUpdate(team.copy(workers = updated))
                                },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        IconButton(
                            onClick = {
                                val updated = team.workers.filterIndexed { idx, _ -> idx != wIdx }
                                onUpdate(team.copy(workers = updated))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Role", tint = RoseDanger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Card Footer: Total Headcount + Remove Team
            val totalTeamWorkers = team.workers.sumOf { it.count }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Headcount: $totalTeamWorkers Workers",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary
                )

                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Team", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: MATERIALS (Webapp Exact Match: MixRatio Helper + Manual Items)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabMaterialsWeb(
    state: WorklogUiState,
    viewModel: WorklogViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Ribbon Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Materials Consumed",
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Deduct directly from project inventory stock.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = {
                        val available = state.projectMaterials
                        val defaultMat = available.firstOrNull { inv ->
                            state.draftMaterials.none { it.projectMaterialId == inv.id }
                        } ?: available.firstOrNull()

                        viewModel.addMaterial(
                            WorklogMaterialInput(
                                projectMaterialId = defaultMat?.id,
                                materialName = defaultMat?.name ?: "",
                                quantityConsumed = 1.0,
                                unit = defaultMat?.effectiveUnit ?: "Units",
                                unitCost = defaultMat?.effectiveCost
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Warning banner if project inventory has no materials
        if (state.projectMaterials.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                        Text(
                            text = "No materials found in this project's inventory. Add materials in the Project Materials section to track stock.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Embedded Concrete Mix Ratio Helper Widget
        item {
            MixRatioHelperWidget(
                onAddMaterials = { mixItems ->
                    mixItems.forEach { item ->
                        val matched = state.projectMaterials.find { pm ->
                            pm.name.contains("cement", ignoreCase = true) && item.materialName.contains("cement", ignoreCase = true) ||
                            pm.name.contains("sand", ignoreCase = true) && item.materialName.contains("sand", ignoreCase = true) ||
                            (pm.name.contains("metal", ignoreCase = true) || pm.name.contains("gravel", ignoreCase = true) || pm.name.contains("aggregate", ignoreCase = true)) &&
                            (item.materialName.contains("metal", ignoreCase = true) || item.materialName.contains("gravel", ignoreCase = true) || item.materialName.contains("aggregate", ignoreCase = true))
                        } ?: state.projectMaterials.find { pm ->
                            pm.name.contains(item.materialName, ignoreCase = true) || item.materialName.contains(pm.name, ignoreCase = true)
                        } ?: state.projectMaterials.firstOrNull()

                        if (matched != null) {
                            viewModel.addMaterial(
                                WorklogMaterialInput(
                                    projectMaterialId = matched.id,
                                    materialName = matched.name,
                                    quantityConsumed = item.quantityConsumed,
                                    unit = matched.effectiveUnit,
                                    unitCost = matched.effectiveCost
                                )
                            )
                        } else {
                            viewModel.addMaterial(
                                WorklogMaterialInput(
                                    materialName = item.materialName,
                                    quantityConsumed = item.quantityConsumed,
                                    unit = item.unit
                                )
                            )
                        }
                    }
                }
            )
        }

        // Empty state
        if (state.draftMaterials.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No materials logged today. Use Mix Ratio Helper above or tap 'Add Item' to select from inventory.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Inventory Items to Deduct (${state.draftMaterials.size})",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }

            itemsIndexed(state.draftMaterials) { index, material ->
                MaterialEntryCardWeb(
                    index = index,
                    material = material,
                    projectMaterials = state.projectMaterials,
                    onUpdate = { updated -> viewModel.updateMaterial(index, updated) },
                    onRemove = { viewModel.removeMaterial(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialEntryCardWeb(
    index: Int,
    material: WorklogMaterialInput,
    projectMaterials: List<com.example.constructorapp.data.remote.ProjectMaterialDto>,
    onUpdate: (WorklogMaterialInput) -> Unit,
    onRemove: () -> Unit
) {
    var dropdownOpen by remember { mutableStateOf(false) }
    val currentInventory = projectMaterials.find { it.id == material.projectMaterialId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Inventory Material Dropdown (Strict Dropdown) + Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownOpen,
                    onExpandedChange = { dropdownOpen = !dropdownOpen },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (material.materialName.isNotBlank()) material.materialName else "Select Material from Inventory",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inventory Material *", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (projectMaterials.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No inventory materials found", color = TextSecondary, fontSize = 12.sp) },
                                onClick = { dropdownOpen = false }
                            )
                        } else {
                            projectMaterials.forEach { pm ->
                                val isSelected = pm.id == material.projectMaterialId
                                DropdownMenuItem(
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = pm.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) BrandPrimary else TextPrimary,
                                                fontSize = 13.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "In Stock: ${pm.effectiveQuantity} ${pm.effectiveUnit}",
                                                    fontSize = 11.sp,
                                                    color = if (pm.effectiveQuantity > 0) EmeraldSuccess else RoseDanger,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (pm.effectiveCost > 0) {
                                                    Text(
                                                        text = "₹${pm.effectiveCost}/${pm.effectiveUnit}",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        onUpdate(
                                            material.copy(
                                                projectMaterialId = pm.id,
                                                materialName = pm.name,
                                                unit = pm.effectiveUnit,
                                                unitCost = pm.effectiveCost
                                            )
                                        )
                                        dropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(20.dp))
                }
            }

            // Real-time Stock Status & Deduction Preview
            if (currentInventory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (material.quantityConsumed > currentInventory.effectiveQuantity) RoseDanger.copy(alpha = 0.1f) else Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available: ${currentInventory.effectiveQuantity} ${currentInventory.effectiveUnit}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentInventory.effectiveQuantity > 0) EmeraldSuccess else RoseDanger
                    )
                    if (material.quantityConsumed > currentInventory.effectiveQuantity) {
                        Text(
                            text = "⚠️ Exceeds Available Stock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseDanger
                        )
                    } else if (material.quantityConsumed > 0) {
                        val remaining = currentInventory.effectiveQuantity - material.quantityConsumed
                        Text(
                            text = "Remaining: ${String.format(Locale.US, "%.1f", remaining)} ${currentInventory.effectiveUnit}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Row 2: Quantity Consumed & Unit (Locked to Inventory Unit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = if (material.quantityConsumed > 0) material.quantityConsumed.toString() else "",
                    onValueChange = {
                        val q = it.toDoubleOrNull() ?: 0.0
                        onUpdate(material.copy(quantityConsumed = q))
                    },
                    label = { Text("Quantity Consumed", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 10", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                OutlinedTextField(
                    value = material.unit.ifBlank { currentInventory?.effectiveUnit ?: "Units" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit", fontSize = 11.sp) },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2E8F0),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedTextColor = TextSecondary,
                        unfocusedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: PHOTOS (Webapp Exact Match: Grid, Thumbnail, Captions)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabPhotosWeb(
    state: WorklogUiState,
    viewModel: WorklogViewModel,
    onPickPhotos: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Ribbon Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Site Photos",
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Upload site progress images with captions.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onPickPhotos,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Empty state trigger card
        if (state.draftPhotos.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickPhotos() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BrandPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Photos",
                                tint = BrandPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Text(
                            text = "Click to upload site photos",
                            fontFamily = OutfitFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Auto-compressed client-side via ImageCompressor (<1.5MB)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Attached Photos (${state.draftPhotos.size})",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }

            // Photo Cards Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.draftPhotos.chunked(2).forEachIndexed { rowIdx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEachIndexed { colIdx, photo ->
                                val realIdx = rowIdx * 2 + colIdx
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .background(Color.Black)
                                        ) {
                                            val imageModel = photo.localFilePath?.let { File(it) } ?: photo.localUri
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = "Worklog photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            IconButton(
                                                onClick = { viewModel.removePhoto(realIdx) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xCC000000))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Caption input field matching webapp
                                        OutlinedTextField(
                                            value = photo.caption,
                                            onValueChange = { viewModel.updatePhotoCaption(realIdx, it) },
                                            placeholder = { Text("Caption...", fontSize = 11.sp, color = TextSecondary) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedContainerColor = Color(0xFFF8FAFC),
                                                unfocusedContainerColor = Color(0xFFF8FAFC)
                                            ),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
