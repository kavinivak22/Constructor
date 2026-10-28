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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.MaterialConsumedInput
import com.example.constructorapp.data.repository.WorkerCountInput
import com.example.constructorapp.theme.*
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
    var selectedWorklogForModal by remember { mutableStateOf<DailyWorklogDto?>(null) }

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
                            color = TextPrimary
                        )
                        Text(
                            text = "Daily Worklogs & Labor Breakdown",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateSurface,
                contentColor = BrandPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Worklog History",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) BrandPrimary else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "New Daily Log",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) BrandPrimary else TextSecondary
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> WorklogHistoryTab(
                    worklogs = worklogs,
                    onWorklogClick = { selectedWorklogForModal = it }
                )
                1 -> NewWorklogTab(
                    projectId = projectId,
                    materials = materials,
                    submitState = submitState,
                    onSubmit = { date, entries ->
                        viewModel.submitWorklog(projectId, date, entries) {
                            selectedTab = 0
                            viewModel.resetSubmitState()
                        }
                    },
                    onResetState = { viewModel.resetSubmitState() }
                )
            }

            // Worklog Detail Modal
            selectedWorklogForModal?.let { log ->
                WorklogDetailModal(
                    worklog = log,
                    projectName = projectName,
                    onDismiss = { selectedWorklogForModal = null }
                )
            }
        }
    }
}

@Composable
fun WorklogHistoryTab(
    worklogs: List<DailyWorklogDto>,
    onWorklogClick: (DailyWorklogDto) -> Unit
) {
    if (worklogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Worklogs Found",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap 'New Daily Log' to record contractor entries and materials.",
                    fontSize = 13.sp,
                    color = TextSecondary,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .clickable { onWorklogClick(log) },
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }
                            if (!log.createdBy.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Submitted by user: ${log.createdBy.take(8)}...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSuccess.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "VERIFIED",
                                color = EmeraldSuccess,
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

@Composable
fun WorklogDetailModal(
    worklog: DailyWorklogDto,
    projectName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Worklog Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$projectName • ${worklog.date}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Job Costing Status", fontSize = 13.sp, color = TextSecondary)
                            Text("Active & Synced", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Classification", fontSize = 13.sp, color = TextSecondary)
                            Text("Rate & NMR Hybrid", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SkyInfo)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
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

    // Active Entry Input States
    var contractorName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Masonry") }
    var payoutClass by remember { mutableStateOf("NMR") } // "NMR" or "Rate"
    var workType by remember { mutableStateOf("Brickwork") }
    var workDescription by remember { mutableStateOf("") }
    var workDoneQuantity by remember { mutableStateOf("") }
    var workDoneUnit by remember { mutableStateOf("sq.ft") }
    var payoutRate by remember { mutableStateOf("") }

    // Worker Counts for NMR
    val workerTypes = listOf("Mason", "Carpenter", "Electrician", "Plumber", "Painter", "Helper", "Supervisor")
    val workerCounts = remember { mutableStateListOf<WorkerCountInput>() }

    // Materials Consumed
    val materialsConsumed = remember { mutableStateListOf<MaterialConsumedInput>() }
    var selectedMaterial by remember { mutableStateOf<ProjectMaterialDto?>(null) }
    var materialQuantity by remember { mutableStateOf("") }

    // Dropdowns
    var categoryExpanded by remember { mutableStateOf(false) }
    var workTypeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Masonry", "Painting", "Electrical", "Plumbing", "Carpentry", "Civil", "Flooring")
    val workTypesList = listOf("Brickwork", "Plastering", "Tiling", "Painting", "Excavation", "Concreting", "Steel / Barbending", "Flooring")
    val unitsList = listOf("sq.ft", "cu.ft", "bags", "rft", "hrs", "nos")

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
        // Date Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Log Date (YYYY-MM-DD)", fontSize = 12.sp, color = TextSecondary)
                        Text(dateText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        // Active List of Labor Entries Added So Far
        if (laborEntries.isNotEmpty()) {
            item {
                Text(
                    text = "Draft Labor Entries (${laborEntries.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            items(laborEntries.toList()) { entry ->
                val isRate = entry.payoutClass.equals("Rate", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = entry.contractorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${entry.category} • ${if (isRate) entry.workType else "NMR Daily"}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isRate) SkyInfo.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f))
                                        .border(1.dp, if (isRate) SkyInfo.copy(alpha = 0.3f) else EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = entry.payoutClass,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRate) SkyInfo else EmeraldSuccess
                                    )
                                }

                                IconButton(onClick = { laborEntries.remove(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger)
                                }
                            }
                        }

                        if (isRate && entry.totalPayout != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Work Done: ${entry.workDoneQuantity} ${entry.workDoneUnit} @ ₹${entry.payoutRate} = ₹${entry.totalPayout}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SkyInfo
                            )
                        }

                        val activeWorkers = entry.workerCounts.filter { it.count > 0 }
                        if (activeWorkers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Workers: " + activeWorkers.joinToString { "${it.workerType} x${it.count}" },
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        if (entry.materialsConsumed.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Materials: " + entry.materialsConsumed.joinToString { "${it.materialName} (${it.quantityConsumed} ${it.unit})" },
                                fontSize = 12.sp,
                                color = PurpleAccent
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
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add Contractor Labor & Material Log",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )

                    // Payout Class Toggle (NMR vs Rate Work)
                    Text("Classification", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NMR", "Rate").forEach { pClass ->
                            val isSelected = payoutClass == pClass
                            val accentColor = if (pClass == "Rate") SkyInfo else EmeraldSuccess
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else SlateSurfaceSubtle)
                                    .border(1.dp, if (isSelected) accentColor else SlateBorder, RoundedCornerShape(10.dp))
                                    .clickable { payoutClass = pClass }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pClass == "NMR") "NMR (Daily Wage)" else "Rate Work (Unit-based)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else TextSecondary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contractorName,
                        onValueChange = { contractorName = it },
                        label = { Text("Contractor / Gang Name *", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Trade Category", color = TextSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = outlinedFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Rate Work Fields
                    if (payoutClass == "Rate") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SkyInfo.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Rate Work Details", fontWeight = FontWeight.Bold, color = SkyInfo, fontSize = 14.sp)

                                // Work Type
                                ExposedDropdownMenuBox(
                                    expanded = workTypeExpanded,
                                    onExpandedChange = { workTypeExpanded = !workTypeExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = workType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Work Type", color = TextSecondary) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workTypeExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        colors = outlinedFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = workTypeExpanded,
                                        onDismissRequest = { workTypeExpanded = false }
                                    ) {
                                        workTypesList.forEach { wt ->
                                            DropdownMenuItem(
                                                text = { Text(wt) },
                                                onClick = {
                                                    workType = wt
                                                    workTypeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = workDoneQuantity,
                                        onValueChange = { workDoneQuantity = it },
                                        label = { Text("Quantity", color = TextSecondary) },
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedFieldColors()
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = unitExpanded,
                                        onExpandedChange = { unitExpanded = !unitExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = workDoneUnit,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Unit", color = TextSecondary) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            colors = outlinedFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = unitExpanded,
                                            onDismissRequest = { unitExpanded = false }
                                        ) {
                                            unitsList.forEach { u ->
                                                DropdownMenuItem(
                                                    text = { Text(u) },
                                                    onClick = {
                                                        workDoneUnit = u
                                                        unitExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = payoutRate,
                                    onValueChange = { payoutRate = it },
                                    label = { Text("Rate (₹ per unit)", color = TextSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = outlinedFieldColors()
                                )

                                val qty = workDoneQuantity.toDoubleOrNull() ?: 0.0
                                val rate = payoutRate.toDoubleOrNull() ?: 0.0
                                if (qty > 0 && rate > 0) {
                                    val total = qty * rate
                                    Text(
                                        text = "Estimated Payout: ₹$total",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }

                    // NMR Worker Counts
                    if (payoutClass == "NMR") {
                        Text(
                            text = "Worker Attendance (NMR)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            workerCounts.forEachIndexed { index, wc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateSurfaceSubtle, RoundedCornerShape(8.dp))
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(wc.workerType, fontSize = 14.sp, color = TextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                if (wc.count > 0) workerCounts[index] = wc.copy(count = wc.count - 1)
                                            },
                                            modifier = Modifier.size(36.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                        ) {
                                            Text("-", fontSize = 18.sp, color = TextPrimary)
                                        }
                                        Text(
                                            text = wc.count.toString(),
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                workerCounts[index] = wc.copy(count = wc.count + 1)
                                            },
                                            modifier = Modifier.size(36.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                        ) {
                                            Text("+", fontSize = 18.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = workDescription,
                        onValueChange = { workDescription = it },
                        label = { Text("Description / Location on Site", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )

                    // Material Consumption
                    Text(
                        text = "Materials Consumed (Project Inventory)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = materialExpanded,
                            onExpandedChange = { materialExpanded = !materialExpanded },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = selectedMaterial?.name ?: "Select Material",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                colors = outlinedFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = materialExpanded,
                                onDismissRequest = { materialExpanded = false }
                            ) {
                                if (materials.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No Materials Found") },
                                        onClick = { materialExpanded = false }
                                    )
                                } else {
                                    materials.forEach { mat ->
                                        DropdownMenuItem(
                                            text = { Text("${mat.name} (${mat.quantity} ${mat.unit})") },
                                            onClick = {
                                                selectedMaterial = mat
                                                materialExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = materialQuantity,
                            onValueChange = { materialQuantity = it },
                            placeholder = { Text("Qty", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = outlinedFieldColors()
                        )

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
                                    selectedMaterial = null
                                    materialQuantity = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Material", tint = Color.White)
                        }
                    }

                    // Display added materials
                    if (materialsConsumed.isNotEmpty()) {
                        materialsConsumed.forEach { mc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateSurfaceSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mc.materialName}: ${mc.quantityConsumed} ${mc.unit}",
                                    fontSize = 12.sp,
                                    color = PurpleAccent
                                )
                                IconButton(
                                    onClick = { materialsConsumed.remove(mc) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Add Entry to Draft Button
                    Button(
                        onClick = {
                            if (contractorName.isNotBlank()) {
                                val qty = workDoneQuantity.toDoubleOrNull()
                                val rate = payoutRate.toDoubleOrNull()
                                val total = if (qty != null && rate != null) qty * rate else null

                                laborEntries.add(
                                    LaborEntryInput(
                                        contractorName = contractorName.trim(),
                                        category = selectedCategory,
                                        payoutClass = payoutClass,
                                        workType = if (payoutClass == "Rate") workType else null,
                                        workDoneQuantity = if (payoutClass == "Rate") qty else null,
                                        workDoneUnit = if (payoutClass == "Rate") workDoneUnit else null,
                                        payoutRate = if (payoutClass == "Rate") rate else null,
                                        totalPayout = total,
                                        workDescription = workDescription.trim(),
                                        paymentStatus = "Pending",
                                        workerCounts = workerCounts.filter { it.count > 0 }.map { it.copy() },
                                        materialsConsumed = materialsConsumed.map { it.copy() }
                                    )
                                )

                                // Reset form variables
                                contractorName = ""
                                workDescription = ""
                                workDoneQuantity = ""
                                payoutRate = ""
                                workerCounts.clear()
                                workerCounts.addAll(workerTypes.map { WorkerCountInput(it, 0) })
                                materialsConsumed.clear()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary.copy(alpha = 0.2f)),
                        enabled = contractorName.isNotBlank()
                    ) {
                        Text("Add to Worklog Draft", color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Global Submit
        item {
            if (submitState is SubmitState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseDanger.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = submitState.message,
                        color = RoseDanger,
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
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                enabled = laborEntries.isNotEmpty() && submitState !is SubmitState.Loading
            ) {
                if (submitState is SubmitState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Submit Worklog (${laborEntries.size} entries)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = SlateBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = BrandPrimary
)
