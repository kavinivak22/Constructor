package com.example.constructorapp.ui.screens.workprep

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.model.workprep.*
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.data.repository.WorkPrepRepository
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.WorkPrepUiState
import com.example.constructorapp.ui.viewmodel.WorkPrepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPrepScreen(
    initialProjectId: String? = null,
    viewModel: WorkPrepViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialProjectId) {
        if (!initialProjectId.isNullOrBlank()) {
            viewModel.filterByProject(initialProjectId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Tomorrow's Work Prep",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = state.formattedTomorrowHeader,
                                fontSize = 11.sp,
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
                    actions = {
                        // Project Filter Dropdown Trigger
                        ExposedDropdownMenuBox(
                            expanded = projectDropdownExpanded,
                            onExpandedChange = { projectDropdownExpanded = !projectDropdownExpanded }
                        ) {
                            IconButton(
                                onClick = { projectDropdownExpanded = true },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter Project", tint = BrandPrimary)
                            }
                            ExposedDropdownMenu(
                                expanded = projectDropdownExpanded,
                                onDismissRequest = { projectDropdownExpanded = false },
                                modifier = Modifier.background(SlateSurfaceSubtle)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Sites", color = TextPrimary) },
                                    onClick = {
                                        viewModel.filterByProject("all")
                                        projectDropdownExpanded = false
                                    }
                                )
                                state.projects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = { Text(proj.name, color = TextPrimary) },
                                        onClick = {
                                            viewModel.filterByProject(proj.id)
                                            projectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Readiness Gauge Card
                ReadinessGaugeHeader(state = state)

                // 4-Step Stepper Header Strip
                WizardStepperTabs(
                    currentStep = state.currentStep,
                    onStepSelected = { viewModel.goToStep(it) }
                )

                // Wizard Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (state.currentStep) {
                        1 -> Step1ScopeSection(state = state, viewModel = viewModel)
                        2 -> Step2CallTrackerSection(state = state, viewModel = viewModel)
                        3 -> Step3StockAuditSection(state = state, viewModel = viewModel)
                        4 -> Step4DispatchManifestSection(state = state, viewModel = viewModel)
                    }
                }

                // Bottom Wizard Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.currentStep > 1) {
                        OutlinedButton(
                            onClick = { viewModel.prevStep() },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", color = TextSecondary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (state.currentStep < 4) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Next Step", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.dispatchPrep {
                                    // Dispatched successfully
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isDispatched) "Dispatched" else "Confirm & Dispatch",
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

// ─────────────────────────────────────────────────────────────────────────────
// TOP READINESS GAUGE & METRICS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReadinessGaugeHeader(state: WorkPrepUiState) {
    val score = state.readinessScore
    val scoreColor = when {
        score >= 80 -> EmeraldSuccess
        score >= 50 -> AmberWarning
        else -> RoseDanger
    }
    val scoreLabel = when {
        score >= 80 -> "Site Ready for Dispatch"
        score >= 50 -> "Attention Needed Before Dispatch"
        else -> "Critical Shortage / High Risk"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        borderColor = scoreColor.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Readiness Score",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "$score%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(scoreColor.copy(alpha = 0.15f))
                        .border(1.dp, scoreColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scoreLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Meter
            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = scoreColor,
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Summary KPI Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selectedCount = state.tasks.count { it.isSelected }
                val confirmedCount = state.contractorCalls.count { it.callStatus == CallStatus.CONFIRMED }
                val totalCalls = state.contractorCalls.size
                val deficitCount = state.stockAudits.count { it.isDeficit }

                KpiBadge(
                    label = "Tasks",
                    value = "$selectedCount Planned",
                    accent = BrandPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiBadge(
                    label = "Workforce",
                    value = "$confirmedCount/$totalCalls Confirmed",
                    accent = if (confirmedCount == totalCalls && totalCalls > 0) EmeraldSuccess else AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                KpiBadge(
                    label = "Materials",
                    value = if (deficitCount > 0) "$deficitCount Shortage" else "Stock OK",
                    accent = if (deficitCount > 0) RoseDanger else EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KpiBadge(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateSurfaceSubtle)
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WIZARD STEPPER TABS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WizardStepperTabs(
    currentStep: Int,
    onStepSelected: (Int) -> Unit
) {
    val steps = listOf(
        1 to "1. Scope",
        2 to "2. Calls",
        3 to "3. Stock",
        4 to "4. Dispatch"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        steps.forEach { (step, title) ->
            val isSelected = currentStep == step
            val isPast = currentStep > step
            val color = when {
                isSelected -> BrandPrimary
                isPast -> EmeraldSuccess
                else -> TextSecondary
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) BrandPrimary.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (isSelected) BrandPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onStepSelected(step) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 1: SCOPE & TASKS (Building Plan Suggestions + Task Checklist)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1ScopeSection(
    state: WorkPrepUiState,
    viewModel: WorkPrepViewModel
) {
    var quickTaskTitle by remember { mutableStateOf("") }
    var quickTaskPriority by remember { mutableStateOf("Medium") }
    var isQuickTaskExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Building Plan Suggestions Banner
        if (state.suggestedTasks.isNotEmpty()) {
            item {
                Text(
                    text = "AI BUILDING PLAN SUGGESTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
            }

            items(state.suggestedTasks) { sug ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sug.taskTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("${sug.projectName} • ${sug.processTitle}", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.addSuggestedTask(sug) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section Title: Scheduled Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCHEDULED TASKS (${state.tasks.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { isQuickTaskExpanded = !isQuickTaskExpanded }) {
                    Icon(if (isQuickTaskExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isQuickTaskExpanded) "Cancel" else "Quick Add", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Add Task Form (Expandable)
        if (isQuickTaskExpanded) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add New Task for Tomorrow", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        OutlinedTextField(
                            value = quickTaskTitle,
                            onValueChange = { quickTaskTitle = it },
                            label = { Text("Task Title *") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                val isSelected = quickTaskPriority == p
                                val color = when (p) {
                                    "High" -> RoseDanger
                                    "Medium" -> AmberWarning
                                    else -> SkyInfo
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.2f) else SlateSurfaceSubtle)
                                        .border(1.dp, if (isSelected) color else SlateBorder, RoundedCornerShape(8.dp))
                                        .clickable { quickTaskPriority = p }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) color else TextSecondary)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (quickTaskTitle.isNotBlank()) {
                                    val pId = if (state.selectedProjectId == "all") state.projects.firstOrNull()?.id ?: "" else state.selectedProjectId
                                    viewModel.quickCreateTask(pId, quickTaskTitle.trim(), quickTaskPriority)
                                    quickTaskTitle = ""
                                    isQuickTaskExpanded = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            enabled = quickTaskTitle.isNotBlank()
                        ) {
                            Text("Create & Schedule Task", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks scheduled for tomorrow yet. Add a suggestion or quick task.", fontSize = 12.sp, color = TextSecondary)
                }
            }
        } else {
            items(state.tasks) { task ->
                var contractorExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (task.isSelected) SlateSurface else SlateSurfaceSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (task.isSelected) BrandPrimary.copy(alpha = 0.5f) else SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isSelected,
                                onCheckedChange = { viewModel.toggleTaskSelection(task.id) },
                                colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("${task.projectName} • Priority: ${task.priority}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        // Inline Contractor Assignment Dropdown
                        if (task.isSelected) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Assign Contractor:", fontSize = 12.sp, color = TextSecondary)

                                ExposedDropdownMenuBox(
                                    expanded = contractorExpanded,
                                    onExpandedChange = { contractorExpanded = !contractorExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = task.assignedContractorName ?: "Select Contractor",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contractorExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .width(180.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandPrimary,
                                            unfocusedBorderColor = SlateBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = contractorExpanded,
                                        onDismissRequest = { contractorExpanded = false }
                                    ) {
                                        state.contractorCalls.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(c.name) },
                                                onClick = {
                                                    viewModel.assignContractorToTask(task.id, c.contractorId)
                                                    contractorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 2: CONTRACTOR CALL READINESS TRACKER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step2CallTrackerSection(
    state: WorkPrepUiState,
    viewModel: WorkPrepViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "CONTRACTOR CALL CONFIRMATION TRACKER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Verify labor availability, confirm headcount, and update call readiness.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        if (state.contractorCalls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No contractors registered or assigned yet.", fontSize = 12.sp, color = TextSecondary)
                }
            }
        } else {
            items(state.contractorCalls) { call ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(call.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    text = "${call.category} • ${if (call.assignedTaskTitles.isNotEmpty()) "${call.assignedTaskTitles.size} tasks" else "General"}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            // Quick Phone Dial Intent Button
                            IconButton(
                                onClick = {
                                    if (call.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.phone}"))
                                        context.startActivity(intent)
                                        // Auto-advance status to Called
                                        if (call.callStatus == CallStatus.PENDING) {
                                            viewModel.updateCallStatus(call.callId, CallStatus.CALLED, call.confirmedWorkerCount)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Dial", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Confirmed Headcount Stepper
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceSubtle)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Confirmed Workers Headcount:", fontSize = 12.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        if (call.confirmedWorkerCount > 0) {
                                            viewModel.updateCallStatus(call.callId, call.callStatus, call.confirmedWorkerCount - 1)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Text("-", fontSize = 16.sp, color = TextPrimary)
                                }
                                Text(
                                    text = "${call.confirmedWorkerCount}",
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateCallStatus(call.callId, call.callStatus, call.confirmedWorkerCount + 1)
                                    },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Text("+", fontSize = 16.sp, color = TextPrimary)
                                }
                            }
                        }

                        // 4 Segmented Call Status Pills
                        Text("Call Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CallStatus.PENDING to TextSecondary,
                                CallStatus.CALLED to SkyInfo,
                                CallStatus.CONFIRMED to EmeraldSuccess,
                                CallStatus.NO_ANSWER to RoseDanger
                            ).forEach { (status, color) ->
                                val isSelected = call.callStatus == status
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.2f) else SlateSurfaceSubtle)
                                        .border(1.dp, if (isSelected) color else SlateBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateCallStatus(call.callId, status, call.confirmedWorkerCount)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 3: MATERIAL STOCK VERIFICATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step3StockAuditSection(
    state: WorkPrepUiState,
    viewModel: WorkPrepViewModel
) {
    val deficitCount = state.stockAudits.count { it.isDeficit }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "MATERIAL STOCK VERIFICATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Verifies if materials needed for tomorrow's work are physically in stock.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Deficit Alert Banner
        if (deficitCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseDanger.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseDanger.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$deficitCount Material Shortages Detected",
                                fontWeight = FontWeight.Bold,
                                color = RoseDanger,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Required quantities exceed current warehouse stock. Procure or transfer before site start.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Search Filter
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Filter materials by name...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        if (state.stockAudits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No material assets match criteria. Attach materials in Step 1 to verify stock.", fontSize = 12.sp, color = TextSecondary)
                }
            }
        } else {
            items(state.stockAudits) { audit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (audit.isDeficit) RoseDanger.copy(alpha = 0.5f) else SlateBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(audit.materialName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("${audit.category} • Required: ${audit.requiredQty} ${audit.unit}", fontSize = 12.sp, color = TextSecondary)
                            }

                            // Adequacy Badge
                            val (badgeText, badgeColor) = when {
                                audit.isDeficit -> "SHORTAGE" to RoseDanger
                                audit.isLowBuffer -> "LOW BUFFER" to AmberWarning
                                else -> "IN STOCK" to EmeraldSuccess
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                        }

                        // Stock numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Stock: ${audit.currentStock} ${audit.unit}", fontSize = 12.sp, color = TextPrimary)
                            if (audit.isDeficit) {
                                Text("Deficit: -${audit.deficitQty} ${audit.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                            } else {
                                Text("Min Level: ${audit.minStockLevel} ${audit.unit}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 4: DISPATCH MANIFEST GENERATOR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step4DispatchManifestSection(
    state: WorkPrepUiState,
    viewModel: WorkPrepViewModel
) {
    val context = LocalContext.current
    val summaryText = viewModel.generateSummaryText(state)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DISPATCH MANIFEST & SHARING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Finalize tomorrow's work prep, copy or share manifest via WhatsApp, and persist dispatch.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Summary Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manifest Summary Text", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = summaryText,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateSurface)
                            .padding(12.dp)
                    )
                }
            }
        }

        // Action: Share via WhatsApp / SMS Intent
        item {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, summaryText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Tomorrow's Prep Manifest"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share via WhatsApp / SMS", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (state.isDispatched) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Plan Dispatched Successfully", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 13.sp)
                            Text("Dispatched at: ${state.dispatchedAt ?: "Today"}. Tasks assigned in Room database.", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun WorkPrepScreen(
    initialProjectId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val workPrepRepository = remember { WorkPrepRepository(database) }
    val projectRepository = remember { ProjectRepository(database) }

    val workPrepViewModel: WorkPrepViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkPrepViewModel(workPrepRepository, projectRepository) as T
            }
        }
    )

    WorkPrepScreen(
        initialProjectId = initialProjectId,
        viewModel = workPrepViewModel,
        onBackClick = onBackClick
    )
}
