package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ReminderEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: String? = null,
    existingReminder: ReminderEntity? = null,
    onBackClick: () -> Unit = {},
    onSaveReminder: (id: String?, title: String, description: String?, priority: String, location: String?, dueDate: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val isEditMode = reminderId != null

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var description by remember { mutableStateOf(existingReminder?.description ?: "") }
    var priority by remember { mutableStateOf(existingReminder?.priority ?: "Medium") }
    var location by remember { mutableStateOf(existingReminder?.location ?: "") }
    var dueDate by remember { mutableStateOf(existingReminder?.dueDate ?: "") }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showLocationPickerDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dueDate = DateTimeUtil.formatEpochToDateString(millis)
                    }
                    showDatePickerDialog = false
                }) {
                    Text("OK", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Reminder" else "New Reminder",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes & Description (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(10.dp)
            )

            // Priority Selector Row
            Text(
                text = "Priority Level",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("High", "Medium", "Low").forEach { level ->
                    val isSelected = priority.lowercase() == level.lowercase()
                    val color = when (level.lowercase()) {
                        "high" -> Color(0xFFEB5757)
                        "medium" -> Color(0xFFF2994A)
                        else -> Color(0xFF27AE60)
                    }
                    val containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface
                    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderModifier = if (isSelected) Modifier else Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerColor)
                            .then(borderModifier)
                            .clickable { priority = level }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = level,
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Due Date field with Picker
            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Due Date (YYYY-MM-DD)") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerDialog = true },
                shape = RoundedCornerShape(10.dp)
            )

            // Location field with Picker trigger
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Description or Lat,Long)") },
                trailingIcon = {
                    IconButton(onClick = { showLocationPickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Select on Map",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveReminder(
                            reminderId,
                            title,
                            description.takeIf { it.isNotBlank() },
                            priority,
                            location.takeIf { it.isNotBlank() },
                            dueDate.takeIf { it.isNotBlank() }
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = title.isNotBlank()
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Create Reminder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Coordinates Picker Dialog
    if (showLocationPickerDialog) {
        FluidGlossDialog(
            onDismissRequest = { showLocationPickerDialog = false },
            title = "Select Coordinates / Preset",
            icon = Icons.Default.Place,
            dismissButton = {
                FluidGlossButton(
                    text = "Cancel",
                    onClick = { showLocationPickerDialog = false },
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Choose a default coordinate pair or preset for constructor site mapping:",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                val presets = listOf(
                    "Googleplex Site" to "37.4220,-122.0841",
                    "NYC Construction Site" to "40.7128,-74.0060",
                    "London Office" to "51.5074,-0.1278",
                    "Sydney Project Site" to "-33.8688,151.2093"
                )

                presets.forEach { (name, coords) ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            location = coords
                            showLocationPickerDialog = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = coords,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Select",
                                tint = BrandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
