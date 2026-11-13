package com.example.constructorapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.workprep.*
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDropdownMenu
import com.example.constructorapp.ui.components.FluidGlossDropdownMenuItem
import com.example.constructorapp.ui.components.FluidGlossExposedDropdown
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.Toast

data class WorkPrepUiState(
    val currentStep: Int = 1,
    val tomorrowDateStr: String = "2026-11-14",
    val formattedTomorrowHeader: String = "Saturday, November 14, 2026",
    val selectedProjectId: String = "all",
    val projects: List<ProjectDto> = emptyList(),
    val tasks: List<PrepTaskItem> = emptyList(),
    val selectedTaskIds: Set<String> = emptySet(),
    val taskContractors: Map<String, String> = emptyMap(),
    val taskMaterials: Map<String, List<TaskMaterialRequirement>> = emptyMap(),
    val contractorCalls: List<ContractorCallItem> = emptyList(),
    val stockAudits: List<MaterialStockAuditItem> = emptyList(),
    val suggestedTasks: List<SuggestedTaskItem> = emptyList(),
    val searchQuery: String = "",
    val readinessScore: Int = 85,
    val isDispatched: Boolean = false,
    val dispatchedAt: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

private val initialProjects = listOf(
    ProjectDto(id = "p-1", name = "Greenfield Villa", location = "Trichy", budget = 4500000.0, companyId = "c-1"),
    ProjectDto(id = "p-2", name = "Royal Residency", location = "Kallukuzhi", budget = 8500000.0, companyId = "c-1"),
    ProjectDto(id = "p-3", name = "Modern Duplex", location = "Karur Bypass", budget = 3200000.0, companyId = "c-1")
)

private val initialTasks = listOf(
    PrepTaskItem(
        id = "t-1",
        projectId = "p-1",
        projectName = "Greenfield Villa",
        title = "Foundation Concreting Phase 1 (M20 Grade)",
        priority = "High",
        dueDate = "2026-11-14",
        isSelected = true,
        assignedContractorId = "c-1",
        assignedContractorName = "Selvam Masonry",
        materialRequirements = listOf(
            TaskMaterialRequirement("m-1", "53 Grade Cement", 100.0, "Bags"),
            TaskMaterialRequirement("m-2", "M-Sand", 300.0, "CFT"),
            TaskMaterialRequirement("m-3", "20mm Aggregate", 400.0, "CFT")
        )
    ),
    PrepTaskItem(
        id = "t-2",
        projectId = "p-1",
        projectName = "Greenfield Villa",
        title = "Column Rebar Ties & Shuttering",
        priority = "High",
        dueDate = "2026-11-14",
        isSelected = true,
        assignedContractorId = "c-2",
        assignedContractorName = "Murugan Bar Benders",
        materialRequirements = listOf(
            TaskMaterialRequirement("m-4", "16mm TMT Steel", 500.0, "Kg")
        )
    ),
    PrepTaskItem(
        id = "t-3",
        projectId = "p-2",
        projectName = "Royal Residency",
        title = "External Wall Brickwork & Lintel Prep",
        priority = "Medium",
        dueDate = "2026-11-14",
        isSelected = false,
        assignedContractorId = "c-3",
        assignedContractorName = "Kumar Carpentry & Shuttering"
    )
)

private val initialContractorCalls = listOf(
    ContractorCallItem(
        callId = "call-1",
        contractorId = "c-1",
        name = "Selvam (Masonry)",
        category = "Civil / Masonry",
        phone = "+91 98424 11223",
        assignedTaskTitles = listOf("Foundation Concreting Phase 1"),
        callStatus = CallStatus.CONFIRMED,
        confirmedWorkerCount = 8
    ),
    ContractorCallItem(
        callId = "call-2",
        contractorId = "c-2",
        name = "Murugan (Bar Benders)",
        category = "Steel Reinforcement",
        phone = "+91 94431 55667",
        assignedTaskTitles = listOf("Column Rebar Ties"),
        callStatus = CallStatus.CONFIRMED,
        confirmedWorkerCount = 4
    ),
    ContractorCallItem(
        callId = "call-3",
        contractorId = "c-3",
        name = "Kumar (Carpentry / Shuttering)",
        category = "Formwork",
        phone = "+91 97890 22334",
        assignedTaskTitles = listOf("External Wall Brickwork"),
        callStatus = CallStatus.PENDING,
        confirmedWorkerCount = 0
    )
)

private val initialStockAudits = listOf(
    MaterialStockAuditItem(
        materialId = "m-1",
        materialName = "53 Grade OPC Cement",
        category = "Cement",
        currentStock = 180.0,
        minStockLevel = 100.0,
        requiredQty = 100.0,
        unit = "Bags",
        isDeficit = false,
        deficitQty = 0.0,
        isLowBuffer = false
    ),
    MaterialStockAuditItem(
        materialId = "m-4",
        materialName = "Fe 550D TMT Steel 16mm",
        category = "Steel",
        currentStock = 450.0,
        minStockLevel = 500.0,
        requiredQty = 500.0,
        unit = "Kg",
        isDeficit = true,
        deficitQty = 50.0,
        isLowBuffer = true
    ),
    MaterialStockAuditItem(
        materialId = "m-2",
        materialName = "M-Sand (Fine Aggregate)",
        category = "Sand",
        currentStock = 450.0,
        minStockLevel = 150.0,
        requiredQty = 300.0,
        unit = "CFT",
        isDeficit = false,
        deficitQty = 0.0,
        isLowBuffer = false
    ),
    MaterialStockAuditItem(
        materialId = "m-3",
        materialName = "20mm Blue Metal",
        category = "Aggregate",
        currentStock = 600.0,
        minStockLevel = 200.0,
        requiredQty = 400.0,
        unit = "CFT",
        isDeficit = false,
        deficitQty = 0.0,
        isLowBuffer = false
    )
)

private val initialSuggestedTasks = listOf(
    SuggestedTaskItem(
        projectId = "p-1",
        projectName = "Greenfield Villa",
        taskTitle = "Slab Curing & Water Ponding Check",
        processTitle = "Concrete Curing Process",
        checklistsCount = 3
    ),
    SuggestedTaskItem(
        projectId = "p-1",
        projectName = "Greenfield Villa",
        taskTitle = "Electrical Conduiting in Slab Deck",
        processTitle = "Electrical 1st Fix",
        checklistsCount = 2
    )
)

class WorkPrepController(initialProjectId: String? = null) {
    var state by mutableStateOf(
        WorkPrepUiState(
            selectedProjectId = initialProjectId ?: "all",
            projects = initialProjects,
            tasks = initialTasks,
            selectedTaskIds = setOf("t-1", "t-2"),
            contractorCalls = initialContractorCalls,
            stockAudits = initialStockAudits,
            suggestedTasks = initialSuggestedTasks
        )
    )

    fun filterByProject(projectId: String) {
        state = state.copy(selectedProjectId = projectId)
    }

    fun goToStep(step: Int) {
        state = state.copy(currentStep = step.coerceIn(1, 4))
    }

    fun prevStep() {
        if (state.currentStep > 1) {
            state = state.copy(currentStep = state.currentStep - 1)
        }
    }

    fun nextStep() {
        if (state.currentStep < 4) {
            state = state.copy(currentStep = state.currentStep + 1)
        }
    }

    fun toggleTaskSelection(taskId: String) {
        val newSelection = if (state.selectedTaskIds.contains(taskId)) {
            state.selectedTaskIds - taskId
        } else {
            state.selectedTaskIds + taskId
        }
        val updatedTasks = state.tasks.map {
            if (it.id == taskId) it.copy(isSelected = !it.isSelected) else it
        }
        state = state.copy(selectedTaskIds = newSelection, tasks = updatedTasks)
    }

    fun assignContractorToTask(taskId: String, contractorId: String) {
        val contractor = state.contractorCalls.find { it.contractorId == contractorId }
        val updatedTasks = state.tasks.map {
            if (it.id == taskId) {
                it.copy(assignedContractorId = contractorId, assignedContractorName = contractor?.name)
            } else it
        }
        state = state.copy(tasks = updatedTasks)
    }

    fun updateCallStatus(callId: String, status: CallStatus, workerCount: Int) {
        val count = workerCount.coerceAtLeast(0)
        val updatedCalls = state.contractorCalls.map {
            if (it.callId == callId) {
                it.copy(callStatus = status, confirmedWorkerCount = count)
            } else it
        }
        state = state.copy(contractorCalls = updatedCalls)
    }

    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun addSuggestedTask(sug: SuggestedTaskItem) {
        val newTask = PrepTaskItem(
            id = "t-${state.tasks.size + 1}",
            projectId = sug.projectId,
            projectName = sug.projectName,
            title = sug.taskTitle,
            priority = "Medium",
            dueDate = state.tomorrowDateStr,
            isSelected = true
        )
        state = state.copy(
            tasks = state.tasks + newTask,
            selectedTaskIds = state.selectedTaskIds + newTask.id,
            suggestedTasks = state.suggestedTasks.filter { it != sug }
        )
    }

    fun quickCreateTask(pId: String, title: String, priority: String) {
        val proj = state.projects.find { it.id == pId } ?: state.projects.first()
        val newTask = PrepTaskItem(
            id = "t-${state.tasks.size + 1}",
            projectId = proj.id,
            projectName = proj.name,
            title = title,
            priority = priority,
            dueDate = state.tomorrowDateStr,
            isSelected = true
        )
        state = state.copy(
            tasks = state.tasks + newTask,
            selectedTaskIds = state.selectedTaskIds + newTask.id
        )
    }

    fun dispatchPrep(onSuccess: () -> Unit) {
        state = state.copy(
            isDispatched = true,
            dispatchedAt = "Today, 6:30 PM",
            readinessScore = 100
        )
        onSuccess()
    }

    fun generateSummaryText(state: WorkPrepUiState): String {
        val sb = StringBuilder()
        sb.append("📋 TOMORROW'S SITE PREP MANIFEST\n")
        sb.append("📅 Date: ${state.formattedTomorrowHeader}\n\n")

        sb.append("✅ SCHEDULED TASKS (${state.selectedTaskIds.size}):\n")
        state.tasks.filter { state.selectedTaskIds.contains(it.id) }.forEachIndexed { idx, t ->
            sb.append("${idx + 1}. [${t.projectName}] ${t.title} (${t.priority} Priority)\n")
        }

        sb.append("\n👷 CREW CONFIRMATIONS:\n")
        state.contractorCalls.forEach { c ->
            sb.append("• ${c.name}: ${c.confirmedWorkerCount} workers [${c.callStatus.label}]\n")
        }

        sb.append("\n🧱 MATERIAL AUDIT STATUS:\n")
        state.stockAudits.forEach { m ->
            val status = if (m.isDeficit) "⚠️ SHORTAGE (-${m.deficitQty} ${m.unit})" else "✓ READY"
            sb.append("• ${m.materialName}: ${m.currentStock} ${m.unit} in yard ($status)\n")
        }

        sb.append("\n🚀 Overall Site Readiness: ${state.readinessScore}%")
        return sb.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPrepScreen(
    projectId: String? = null,
    onBackClick: () -> Unit = {}
) {
    val controller = remember { WorkPrepController(projectId) }
    val state = controller.state
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tomorrow's Work Prep",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = when {
                                        state.readinessScore >= 80 -> EmeraldSuccess.copy(alpha = 0.15f)
                                        state.readinessScore >= 50 -> AmberWarning.copy(alpha = 0.15f)
                                        else -> RoseDanger.copy(alpha = 0.15f)
                                    },
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${state.readinessScore}% Ready",
                                        color = when {
                                            state.readinessScore >= 80 -> EmeraldSuccess
                                            state.readinessScore >= 50 -> AmberWarning
                                            else -> RoseDanger
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.formattedTomorrowHeader,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandPrimary)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { projectDropdownExpanded = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter Projects", tint = BrandPrimary)
                            }
                            FluidGlossDropdownMenu(
                                expanded = projectDropdownExpanded,
                                onDismissRequest = { projectDropdownExpanded = false }
                            ) {
                                FluidGlossDropdownMenuItem(
                                    text = "All Sites",
                                    selected = state.selectedProjectId == "all",
                                    onClick = {
                                        controller.filterByProject("all")
                                        projectDropdownExpanded = false
                                    }
                                )
                                state.projects.forEach { proj ->
                                    FluidGlossDropdownMenuItem(
                                        text = proj.name,
                                        selected = state.selectedProjectId == proj.id,
                                        onClick = {
                                            controller.filterByProject(proj.id)
                                            projectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.currentStep > 1) {
                            FluidGlossButton(
                                text = "Previous",
                                onClick = { controller.prevStep() },
                                variant = FluidGlossButtonVariant.Secondary,
                                size = FluidGlossButtonSize.Small
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (state.currentStep < 4) {
                            FluidGlossButton(
                                text = "Next Step",
                                onClick = { controller.nextStep() },
                                variant = FluidGlossButtonVariant.Primary,
                                size = FluidGlossButtonSize.Small
                            )
                        } else {
                            FluidGlossButton(
                                text = if (state.isDispatched) "Dispatched" else "Dispatch Manifest",
                                onClick = {
                                    controller.dispatchPrep {
                                        Toast.makeText(null, "Work Prep Manifest Dispatched & Locked!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                variant = FluidGlossButtonVariant.Success,
                                size = FluidGlossButtonSize.Small,
                                enabled = !state.isDispatched
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Stepper Header (4 steps)
                WorkPrepStepper(
                    currentStep = state.currentStep,
                    onStepSelected = { controller.goToStep(it) }
                )

                // Step Content
                when (state.currentStep) {
                    1 -> Step1TasksSection(state = state, controller = controller)
                    2 -> Step2CallTrackerSection(state = state, controller = controller)
                    3 -> Step3MaterialSection(state = state, controller = controller)
                    4 -> Step4ManifestSection(state = state, controller = controller)
                }
            }
        }
    }
}

@Composable
private fun WorkPrepStepper(
    currentStep: Int,
    onStepSelected: (Int) -> Unit
) {
    val steps = listOf("1. Tasks", "2. Calls", "3. Audit", "4. Share")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isActive = currentStep == stepNumber
            val isDone = currentStep > stepNumber

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isActive -> BrandPrimary
                            isDone -> EmeraldSuccess.copy(alpha = 0.2f)
                            else -> SlateSurfaceSubtle
                        }
                    )
                    .clickable { onStepSelected(stepNumber) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isActive -> Color.White
                        isDone -> EmeraldSuccess
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}

@Composable
private fun Step1TasksSection(
    state: WorkPrepUiState,
    controller: WorkPrepController
) {
    var quickTaskTitle by remember { mutableStateOf("") }
    var quickTaskPriority by remember { mutableStateOf("Medium") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "TOMORROW'S TASK SELECTION & LABOR CREW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Select scheduled tasks to execute tomorrow and assign contractor crews.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Suggested Tasks Banner
        if (state.suggestedTasks.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Suggested Next Sequence Tasks", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SkyInfo)
                        }
                        state.suggestedTasks.forEach { sug ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .clickable { controller.addSuggestedTask(sug) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sug.taskTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("${sug.projectName} • ${sug.processTitle}", fontSize = 10.sp, color = TextSecondary)
                                }
                                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = BrandPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Quick Task Add Input
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluidGlossTextField(
                        value = quickTaskTitle,
                        onValueChange = { quickTaskTitle = it },
                        placeholder = "Add quick tomorrow task...",
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    FluidGlossButton(
                        text = "+ Add",
                        onClick = {
                            if (quickTaskTitle.isNotBlank()) {
                                controller.quickCreateTask(
                                    state.projects.firstOrNull()?.id ?: "p-1",
                                    quickTaskTitle.trim(),
                                    quickTaskPriority
                                )
                                quickTaskTitle = ""
                            }
                        },
                        size = FluidGlossButtonSize.Small,
                        variant = FluidGlossButtonVariant.Primary
                    )
                }
            }
        }

        // Scheduled Tasks List
        items(state.tasks) { task ->
            val isChecked = state.selectedTaskIds.contains(task.id)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (isChecked) BrandPrimary.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.85f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { controller.toggleTaskSelection(task.id) },
                                colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("${task.projectName} • Priority: ${task.priority}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    if (task.materialRequirements.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            task.materialRequirements.forEach { mat ->
                                Surface(
                                    color = SlateSurfaceSubtle,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${mat.materialName}: ${mat.quantity.toInt()} ${mat.unit}",
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Assigned Contractor selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (task.assignedContractorName != null) "Crew: ${task.assignedContractorName}" else "Unassigned Crew",
                            fontSize = 11.sp,
                            color = if (task.assignedContractorName != null) BrandPrimary else RoseDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2CallTrackerSection(
    state: WorkPrepUiState,
    controller: WorkPrepController
) {
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

        items(state.contractorCalls) { call ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
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

                        // Phone Dial Button (Decoupled Toast)
                        IconButton(
                            onClick = {
                                Toast.makeText(null, "Dialing ${call.name} at ${call.phone}", Toast.LENGTH_SHORT).show()
                                if (call.callStatus == CallStatus.PENDING) {
                                    controller.updateCallStatus(call.callId, CallStatus.CALLED, call.confirmedWorkerCount)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Worker Count Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateSurfaceSubtle)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confirmed Headcount:", fontSize = 12.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { controller.updateCallStatus(call.callId, call.callStatus, call.confirmedWorkerCount - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${call.confirmedWorkerCount} workers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { controller.updateCallStatus(call.callId, call.callStatus, call.confirmedWorkerCount + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Status Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CallStatus.entries.forEach { status ->
                            val isSel = call.callStatus == status
                            FilterChip(
                                selected = isSel,
                                onClick = { controller.updateCallStatus(call.callId, status, call.confirmedWorkerCount) },
                                label = { Text(status.label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (status) {
                                        CallStatus.CONFIRMED -> EmeraldSuccess
                                        CallStatus.CALLED -> SkyInfo
                                        CallStatus.NO_ANSWER -> RoseDanger
                                        CallStatus.PENDING -> AmberWarning
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step3MaterialSection(
    state: WorkPrepUiState,
    controller: WorkPrepController
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "MATERIAL STOCK & BUFFER AUDIT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Check site inventory and yard reserves before tomorrow's work begins.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        items(state.stockAudits) { audit ->
            val isDeficit = audit.isDeficit
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                borderColor = if (isDeficit) RoseDanger.copy(alpha = 0.5f) else GlassBorder
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(audit.materialName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Surface(
                            color = if (isDeficit) RoseDangerContainer else EmeraldSuccessContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isDeficit) "DEFICIT: -${audit.deficitQty} ${audit.unit}" else "SUFFICIENT",
                                color = if (isDeficit) RoseDanger else EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Stock: ${audit.currentStock} ${audit.unit}", fontSize = 11.sp, color = TextSecondary)
                        Text("Tomorrow Demand: ${audit.requiredQty} ${audit.unit}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4ManifestSection(
    state: WorkPrepUiState,
    controller: WorkPrepController
) {
    val summaryText = controller.generateSummaryText(state)

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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
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
                            .background(GlassSurfaceSubtle)
                            .padding(12.dp)
                    )
                }
            }
        }

        // Action: Share Manifest
        item {
            FluidGlossButton(
                onClick = {
                    Toast.makeText(null, "Manifest copied to clipboard & ready to share via WhatsApp", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                variant = FluidGlossButtonVariant.Success,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            ) {
                Text("Share via WhatsApp / SMS", fontWeight = FontWeight.Bold)
            }
        }

        if (state.isDispatched) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = EmeraldSuccess.copy(alpha = 0.15f),
                    borderColor = EmeraldSuccess.copy(alpha = 0.4f)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Plan Dispatched Successfully", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 13.sp)
                            Text("Dispatched at: ${state.dispatchedAt ?: "Today"}. Work orders synchronized.", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
