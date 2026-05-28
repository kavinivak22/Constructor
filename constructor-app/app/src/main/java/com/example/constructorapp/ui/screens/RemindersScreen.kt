package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.ui.viewmodel.ReminderViewModel
import com.example.constructorapp.ui.viewmodel.SyncUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: ReminderViewModel,
    onAddReminderClick: () -> Unit,
    onEditReminderClick: (String) -> Unit
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Reminders, 1 = Sheets Sync Settings

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    LaunchedEffect(syncState) {
        when (val state = syncState) {
            is SyncUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearState()
            }
            is SyncUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Personal Pouch",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.syncData() }) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Supabase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = onAddReminderClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Reminders", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Sheets Backup", fontWeight = FontWeight.Bold) }
                )
            }

            when (activeTab) {
                0 -> RemindersListTab(
                    reminders = reminders,
                    onToggleCompletion = { viewModel.toggleReminderCompletion(it) },
                    onEditClick = onEditReminderClick,
                    onDeleteClick = { viewModel.deleteReminder(it) }
                )
                1 -> GoogleSheetsSyncTab(
                    syncState = syncState,
                    onSyncClick = { spreadsheetId, token ->
                        viewModel.syncGoogleSheets(context, spreadsheetId, token)
                    }
                )
            }
        }
    }
}

@Composable
fun RemindersListTab(
    reminders: List<ReminderEntity>,
    onToggleCompletion: (ReminderEntity) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    var selectedPriorityFilter by remember { mutableStateOf("All") }
    val filteredReminders = remember(reminders, selectedPriorityFilter) {
        if (selectedPriorityFilter == "All") {
            reminders
        } else {
            reminders.filter { it.priority.lowercase() == selectedPriorityFilter.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Priority Filter Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "High", "Medium", "Low").forEach { filter ->
                val isSelected = selectedPriorityFilter == filter
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(containerColor)
                        .clickable { selectedPriorityFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filteredReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No reminders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Clean Slates!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You don't have any tasks matching this filter.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReminders, key = { it.id }) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onToggleCompletion = { onToggleCompletion(reminder) },
                        onEditClick = { onEditClick(reminder.id) },
                        onDeleteClick = { onDeleteClick(reminder.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onToggleCompletion: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priorityColor = when (reminder.priority.lowercase()) {
        "high" -> Color(0xFFEB5757)
        "medium" -> Color(0xFFF2994A)
        else -> Color(0xFF27AE60)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF27AE60))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Text Fields Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Priority Circle indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = reminder.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null
                    )
                }

                if (!reminder.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null
                    )
                }

                // Date and location line
                if (reminder.dueDate != null || reminder.location != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (reminder.dueDate != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "Due Date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.dueDate,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (reminder.location != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.location,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Sync status badges
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val supabaseSyncColor = if (reminder.isSyncedWithSupabase) Color(0xFF27AE60) else Color(0xFF888888)
                    Text(
                        text = if (reminder.isSyncedWithSupabase) "Supabase" else "Offline Only",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = supabaseSyncColor
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sheetsSyncColor = if (reminder.isSyncedWithSheets) Color(0xFF27AE60) else Color(0xFF888888)
                    Text(
                        text = if (reminder.isSyncedWithSheets) "Sheets Synced" else "Sheets Unsynced",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = sheetsSyncColor
                    )
                }
            }

            // Options menu (edit and delete)
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSheetsSyncTab(
    syncState: SyncUiState,
    onSyncClick: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("constructor_prefs", android.content.Context.MODE_PRIVATE) }

    var spreadsheetId by remember { mutableStateOf(sharedPref.getString("spreadsheet_id", "") ?: "") }
    var accessToken by remember { mutableStateOf(sharedPref.getString("google_access_token", "") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Google Sheets Sync Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Export your reminders dynamically to a spreadsheet. If you don't enter an access token, the app runs in Simulated Mode for local demonstration.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = spreadsheetId,
                    onValueChange = { spreadsheetId = it },
                    label = { Text("Spreadsheet ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text("Google OAuth Token (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Empty logs simulated upload") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (spreadsheetId.isBlank()) {
                            Toast.makeText(context, "Spreadsheet ID is required", Toast.LENGTH_SHORT).show()
                        } else {
                            onSyncClick(spreadsheetId, accessToken.ifBlank { null })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = syncState !is SyncUiState.Syncing
                ) {
                    if (syncState is SyncUiState.Syncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Sync to Google Sheets Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Help Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background Auto Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WorkManager is configured to periodically back up your newly added offline reminders to this Google Sheet in the background whenever your device connects to the internet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
