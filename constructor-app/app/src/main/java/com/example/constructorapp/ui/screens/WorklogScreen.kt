package com.example.constructorapp.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.MaterialConsumedInput
import com.example.constructorapp.data.repository.WorkerCountInput
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.SubmitState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorklogScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val worklogs by viewModel.worklogs.collectAsState()
    val materials by viewModel.materials.collectAsState()
    val submitState by viewModel.worklogSubmitState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(projectId) {
        viewModel.selectProject(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily Worklogs",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("History", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("New Log", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> WorklogHistoryTab(worklogs = worklogs)
                1 -> NewWorklogTab(
                    projectId = projectId,
                    materials = materials,
                    submitState = submitState,
                    onSubmit = { date, entries ->
                        viewModel.submitWorklog(projectId, date, entries) {
                            selectedTab = 0 // Go back to history tab on success
                            viewModel.resetSubmitState()
                        }
                    },
                    onResetState = { viewModel.resetSubmitState() }
                )
            }
        }
    }
}

@Composable
fun WorklogHistoryTab(worklogs: List<DailyWorklogDto>) {
    if (worklogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Worklogs Found",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daily worklogs submitted by contractors will appear here.",
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
            items(worklogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Worklog Date: ${log.date}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!log.createdBy.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Submitted by user: ${log.createdBy.take(8)}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF27AE60).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "SUBMITTED",
                                color = Color(0xFF27AE60),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorklogTab(
    projectId: String,
    materials: List<ProjectMaterialDto>,
    submitState: SubmitState,
    onSubmit: (String, List<LaborEntryInput>) -> Unit,
    onResetState: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var dateText by remember { mutableStateOf(sdf.format(calendar.time)) }
    val laborEntries = remember { mutableStateListOf<LaborEntryInput>() }

    // State fields for the entry being added
    var contractorName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Masonry") }
    var workDescription by remember { mutableStateOf("") }

    // Workers list under active form
    val workerCounts = remember { mutableStateListOf<WorkerCountInput>() }
    // Materials list under active form
    val materialsConsumed = remember { mutableStateListOf<MaterialConsumedInput>() }

    // Dropdown expanded states
    var categoryExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    // Selected material for active entry
    var selectedMaterial by remember { mutableStateOf<ProjectMaterialDto?>(null) }
    var materialQuantity by remember { mutableStateOf("") }

    val categories = listOf("Masonry", "Painting", "Electrical", "Plumbing", "Carpentry", "General Labor")
    val workerTypes = listOf("Mason", "Carpenter", "Electrician", "Plumber", "Painter", "Helper", "Supervisor")

    // Show date picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val tempCal = Calendar.getInstance()
            tempCal.set(year, month, dayOfMonth)
            dateText = sdf.format(tempCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Initial worker types setup
    LaunchedEffect(Unit) {
        if (workerCounts.isEmpty()) {
            workerCounts.addAll(workerTypes.map { WorkerCountInput(it, 0) })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { datePickerDialog.show() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Worklog Date",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Active List of Labor Entries Added So Far
        if (laborEntries.isNotEmpty()) {
            item {
                Text(
                    text = "Added Labor Logs (${laborEntries.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(laborEntries.toList()) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.contractorName} (${entry.category})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { laborEntries.remove(entry) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (entry.workDescription.isNotEmpty()) {
                            Text(
                                text = entry.workDescription,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Workers counts summarized
                        val activeWorkers = entry.workerCounts.filter { it.count > 0 }
                        if (activeWorkers.isNotEmpty()) {
                            Text(
                                text = "Workers: " + activeWorkers.joinToString { "${it.workerType} x${it.count}" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Materials summary
                        if (entry.materialsConsumed.isNotEmpty()) {
                            Text(
                                text = "Materials: " + entry.materialsConsumed.joinToString { "${it.materialName} (${it.quantityConsumed} ${it.unit})" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Add New Labor Entry Form Builder
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Contractor Labor & Materials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = contractorName,
                        onValueChange = { contractorName = it },
                        label = { Text("Contractor Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Category Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            selectedCategory = selectionOption
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = workDescription,
                        onValueChange = { workDescription = it },
                        label = { Text("Work Done Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Worker Count Stepper Section
                    Text(
                        text = "Worker Attendance Count",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workerCounts.forEachIndexed { index, wc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wc.workerType,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            if (wc.count > 0) {
                                                workerCounts[index] = wc.copy(count = wc.count - 1)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = CircleShape
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = wc.count.toString(),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            workerCounts[index] = wc.copy(count = wc.count + 1)
                                        },
                                        modifier = Modifier.size(36.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = CircleShape
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Material Picker Section
                    Text(
                        text = "Materials Consumed (Inventory Deduction)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Project Materials Dropdown selector
                        Box(modifier = Modifier.weight(1.5f)) {
                            ExposedDropdownMenuBox(
                                expanded = materialExpanded,
                                onExpandedChange = { materialExpanded = !materialExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedMaterial?.name ?: "Select Material",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                ExposedDropdownMenu(
                                    expanded = materialExpanded,
                                    onDismissRequest = { materialExpanded = false }
                                ) {
                                    if (materials.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No Materials Loaded") },
                                            onClick = { materialExpanded = false }
                                        )
                                    } else {
                                        materials.forEach { mat ->
                                            DropdownMenuItem(
                                                text = { Text("${mat.name} (Stock: ${mat.quantity} ${mat.unit})") },
                                                onClick = {
                                                    selectedMaterial = mat
                                                    materialExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quantity Textbox
                        OutlinedTextField(
                            value = materialQuantity,
                            onValueChange = { materialQuantity = it },
                            placeholder = { Text("Qty") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Suffix Unit Label
                        Text(
                            text = selectedMaterial?.unit ?: "units",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 40.dp)
                        )

                        // Add Material Button
                        IconButton(
                            onClick = {
                                val mat = selectedMaterial
                                val qtyVal = materialQuantity.toDoubleOrNull()
                                if (mat != null && qtyVal != null && qtyVal > 0) {
                                    materialsConsumed.add(
                                        MaterialConsumedInput(
                                            projectMaterialId = mat.id,
                                            materialName = mat.name,
                                            quantityConsumed = qtyVal,
                                            unit = mat.unit
                                        )
                                    )
                                    // Reset inputs
                                    selectedMaterial = null
                                    materialQuantity = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Material",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Display added materials
                    if (materialsConsumed.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            materialsConsumed.forEach { mc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${mc.materialName}: ${mc.quantityConsumed} ${mc.unit}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { materialsConsumed.remove(mc) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Entry to Worklog List Button
                    Button(
                        onClick = {
                            if (contractorName.isNotBlank()) {
                                // Capture deep copies of states
                                val entries = workerCounts.filter { it.count > 0 }.map { it.copy() }
                                val mats = materialsConsumed.map { it.copy() }

                                laborEntries.add(
                                    LaborEntryInput(
                                        contractorName = contractorName,
                                        category = selectedCategory,
                                        workDescription = workDescription,
                                        paymentStatus = "Pending",
                                        workerCounts = entries,
                                        materialsConsumed = mats
                                    )
                                )

                                // Reset form variables
                                contractorName = ""
                                workDescription = ""
                                workerCounts.clear()
                                workerCounts.addAll(workerTypes.map { WorkerCountInput(it, 0) })
                                materialsConsumed.clear()
                                selectedMaterial = null
                                materialQuantity = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        enabled = contractorName.isNotBlank()
                    ) {
                        Text("Add Contractor Log to Draft", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Global Submit Section
        item {
            if (submitState is SubmitState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = submitState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (laborEntries.isNotEmpty()) {
                        onSubmit(dateText, laborEntries.toList())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = laborEntries.isNotEmpty() && submitState !is SubmitState.Loading
            ) {
                if (submitState is SubmitState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Submit Worklog (${laborEntries.size} entries)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
