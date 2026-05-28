package com.example.constructorapp.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: String?,
    viewModel: ReminderViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val isEditMode = reminderId != null

    // Find the reminder if editing
    val existingReminder = remember(reminderId, reminders) {
        if (reminderId != null) {
            reminders.find { it.id == reminderId }
        } else {
            null
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var location by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    var showLocationPickerDialog by remember { mutableStateOf(false) }

    // Pre-populate if in edit mode
    LaunchedEffect(existingReminder) {
        existingReminder?.let {
            title = it.title
            description = it.description ?: ""
            priority = it.priority
            location = it.location ?: ""
            dueDate = it.dueDate ?: ""
        }
    }

    // Set up Date Picker
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val tempCal = Calendar.getInstance()
            tempCal.set(year, month, dayOfMonth)
            dueDate = sdf.format(tempCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
                            imageVector = Icons.Default.ArrowBack,
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
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
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
                        viewModel.saveReminder(
                            id = reminderId,
                            title = title,
                            description = description,
                            priority = priority,
                            location = location,
                            dueDate = dueDate
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

    // Coordinates Picker Dialog (Simulates Maps API Picker)
    if (showLocationPickerDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPickerDialog = false },
            title = {
                Text(
                    text = "Select Coordinates / Preset",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a default coordinate pair or preset for constructor site mapping:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        "Googleplex Site" to "37.4220,-122.0841",
                        "NYC Construction Site" to "40.7128,-74.0060",
                        "London Office" to "51.5074,-0.1278",
                        "Sydney Project Site" to "-33.8688,151.2093"
                    )

                    presets.forEach { (name, coords) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    location = coords
                                    showLocationPickerDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = coords,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Select",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
