package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.entity.ContractorEntity
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.local.entity.WorkPrepCallEntity
import com.example.constructorapp.data.model.workprep.*
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.data.repository.WorkPrepRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WorkPrepUiState(
    val currentStep: Int = 1,
    val tomorrowDateStr: String,
    val formattedTomorrowHeader: String,
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
    val readinessScore: Int = 0,
    val isDispatched: Boolean = false,
    val dispatchedAt: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class WorkPrepViewModel(
    private val workPrepRepository: WorkPrepRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val tomorrowDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    val formattedTomorrowHeader: String = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(calendar.time)

    private val _currentStep = MutableStateFlow(1)
    private val _selectedProjectId = MutableStateFlow("all")
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _taskContractors = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _taskMaterials = MutableStateFlow<Map<String, List<TaskMaterialRequirement>>>(emptyMap())
    private val _searchQuery = MutableStateFlow("")
    private val _isDispatched = MutableStateFlow(false)
    private val _dispatchedAt = MutableStateFlow<String?>(null)
    private val _isSubmitting = MutableStateFlow(false)
    private val _suggestedTasks = MutableStateFlow<List<SuggestedTaskItem>>(emptyList())

    val uiState: StateFlow<WorkPrepUiState> = combine(
        workPrepRepository.getTasksForTomorrow(tomorrowDateStr),
        projectRepository.projects,
        workPrepRepository.getContractors(),
        workPrepRepository.getMaterials(),
        workPrepRepository.getCallsForDate(tomorrowDateStr),
        _currentStep,
        _selectedProjectId,
        _selectedTaskIds,
        _taskContractors,
        _taskMaterials,
        _searchQuery,
        _isDispatched,
        _dispatchedAt,
        _suggestedTasks
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawTasks = args[0] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val projects = args[1] as List<ProjectDto>
        @Suppress("UNCHECKED_CAST")
        val contractors = args[2] as List<ContractorEntity>
        @Suppress("UNCHECKED_CAST")
        val materials = args[3] as List<InventoryAssetEntity>
        @Suppress("UNCHECKED_CAST")
        val calls = args[4] as List<WorkPrepCallEntity>
        val step = args[5] as Int
        val pId = args[6] as String
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[7] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val contractorsMap = args[8] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        val materialsMap = args[9] as Map<String, List<TaskMaterialRequirement>>
        val query = args[10] as String
        val dispatched = args[11] as Boolean
        val dispatchTime = args[12] as String?
        @Suppress("UNCHECKED_CAST")
        val suggested = args[13] as List<SuggestedTaskItem>

        // 1. Map Tasks
        val prepTasks = rawTasks.filter { pId == "all" || it.projectId == pId }.map { t ->
            val assignedCId = contractorsMap[t.id] ?: t.assignedTo
            val assignedCName = contractors.find { it.id == assignedCId }?.name
            val projName = projects.find { it.id == t.projectId }?.name ?: "Project"
            PrepTaskItem(
                id = t.id,
                projectId = t.projectId ?: "",
                projectName = projName,
                title = t.title,
                priority = t.priority,
                dueDate = t.dueDate ?: tomorrowDateStr,
                isSelected = t.id in selectedIds,
                assignedContractorId = assignedCId,
                assignedContractorName = assignedCName,
                materialRequirements = materialsMap[t.id] ?: emptyList()
            )
        }

        // 2. Map Contractor Calls
        val selectedTasks = prepTasks.filter { it.isSelected }
        val assignedCIds = selectedTasks.mapNotNull { it.assignedContractorId }.toSet()
        val relevantContractors = if (assignedCIds.isNotEmpty()) {
            contractors.filter { it.id in assignedCIds }
        } else {
            contractors
        }

        val callItems = relevantContractors.map { c ->
            val callRecord = calls.find { it.contractorId == c.id }
            val tasksAssigned = selectedTasks.filter { it.assignedContractorId == c.id }.map { it.title }
            ContractorCallItem(
                callId = callRecord?.id ?: c.id,
                contractorId = c.id,
                name = c.name,
                category = c.category ?: "General",
                phone = c.phone,
                assignedTaskTitles = tasksAssigned,
                callStatus = CallStatus.fromKey(callRecord?.callStatus ?: "pending"),
                confirmedWorkerCount = callRecord?.confirmedWorkerCount ?: 0,
                lastCallTimestamp = callRecord?.lastCallTimestamp
            )
        }

        // 3. Map Material Stock Audits
        val requiredAggregated = mutableMapOf<String, Double>()
        selectedTasks.forEach { task ->
            task.materialRequirements.forEach { req ->
                requiredAggregated[req.materialId] = (requiredAggregated[req.materialId] ?: 0.0) + req.quantity
            }
        }

        val stockAudits = materials.filter { requiredAggregated.containsKey(it.id) || query.isNotBlank() }
            .map { mat ->
                val reqQty = requiredAggregated[mat.id] ?: 0.0
                val isDeficit = reqQty > mat.currentStock
                val deficitQty = (reqQty - mat.currentStock).coerceAtLeast(0.0)
                val isLowBuffer = !isDeficit && mat.currentStock <= mat.minStockLevel
                MaterialStockAuditItem(
                    materialId = mat.id,
                    materialName = mat.name,
                    category = mat.category,
                    currentStock = mat.currentStock,
                    minStockLevel = mat.minStockLevel,
                    requiredQty = reqQty,
                    unit = mat.unit,
                    isDeficit = isDeficit,
                    deficitQty = deficitQty,
                    isLowBuffer = isLowBuffer
                )
            }.filter { query.isBlank() || it.materialName.contains(query, ignoreCase = true) }

        val criticalDeficitCount = stockAudits.count { it.isDeficit }

        // 4. Calculate Readiness Index Formula
        val totalReadiness = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = selectedTasks.size,
            assignedContractorTaskCount = selectedTasks.count { it.assignedContractorId != null },
            totalContactsCount = callItems.size,
            confirmedContactsCount = callItems.count { it.callStatus == CallStatus.CONFIRMED },
            criticalDeficitMaterialCount = criticalDeficitCount
        )

        WorkPrepUiState(
            currentStep = step,
            tomorrowDateStr = tomorrowDateStr,
            formattedTomorrowHeader = formattedTomorrowHeader,
            selectedProjectId = pId,
            projects = projects,
            tasks = prepTasks,
            selectedTaskIds = selectedIds,
            taskContractors = contractorsMap,
            taskMaterials = materialsMap,
            contractorCalls = callItems,
            stockAudits = stockAudits,
            suggestedTasks = suggested,
            searchQuery = query,
            readinessScore = totalReadiness,
            isDispatched = dispatched,
            dispatchedAt = dispatchTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorkPrepUiState(
            tomorrowDateStr = tomorrowDateStr,
            formattedTomorrowHeader = formattedTomorrowHeader,
            isLoading = true
        )
    )

    init {
        // Pre-select tasks scheduled for tomorrow and build initial suggested tasks
        viewModelScope.launch {
            workPrepRepository.getTasksForTomorrow(tomorrowDateStr).first().let { tasks ->
                _selectedTaskIds.value = tasks.map { it.id }.toSet()
            }
        }
        // Initialize sample suggested building plan tasks
        _suggestedTasks.value = listOf(
            SuggestedTaskItem("1", "Prestige Tech Park", "1st Floor Slab Concreting", "Structural Concrete M25", 4),
            SuggestedTaskItem("2", "Villa Grande", "External Wall Plastering 1:6", "Masonry & Plaster", 3),
            SuggestedTaskItem("3", "Brigade Gateway", "Electrical Conduit Rough-In", "Concealed MEP", 2)
        )
    }

    fun nextStep() {
        if (_currentStep.value < 4) _currentStep.value += 1
    }

    fun prevStep() {
        if (_currentStep.value > 1) _currentStep.value -= 1
    }

    fun goToStep(step: Int) {
        if (step in 1..4) _currentStep.value = step
    }

    fun filterByProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTaskSelection(taskId: String) {
        val current = _selectedTaskIds.value.toMutableSet()
        if (taskId in current) current.remove(taskId) else current.add(taskId)
        _selectedTaskIds.value = current
    }

    fun assignContractorToTask(taskId: String, contractorId: String) {
        _taskContractors.value = _taskContractors.value + (taskId to contractorId)
    }

    fun addMaterialToTask(taskId: String, materialId: String, materialName: String, qty: Double, unit: String) {
        val currentList = _taskMaterials.value[taskId] ?: emptyList()
        val updated = currentList + TaskMaterialRequirement(materialId, materialName, qty, unit)
        _taskMaterials.value = _taskMaterials.value + (taskId to updated)
    }

    fun removeMaterialFromTask(taskId: String, materialId: String) {
        val currentList = _taskMaterials.value[taskId] ?: emptyList()
        val updated = currentList.filter { it.materialId != materialId }
        _taskMaterials.value = _taskMaterials.value + (taskId to updated)
    }

    fun updateCallStatus(callId: String, status: CallStatus, confirmedWorkers: Int = 0) {
        viewModelScope.launch {
            workPrepRepository.updateCallStatus(callId, status, confirmedWorkers)
        }
    }

    fun quickCreateTask(projectId: String, title: String, priority: String, contractorId: String? = null) {
        viewModelScope.launch {
            val result = workPrepRepository.quickCreateTask(projectId, title, priority, tomorrowDateStr, contractorId)
            result.getOrNull()?.let { created ->
                _selectedTaskIds.value = _selectedTaskIds.value + created.id
                if (contractorId != null) {
                    _taskContractors.value = _taskContractors.value + (created.id to contractorId)
                }
            }
        }
    }

    fun addSuggestedTask(suggested: SuggestedTaskItem) {
        quickCreateTask(suggested.projectId, suggested.taskTitle, "High", null)
        _suggestedTasks.value = _suggestedTasks.value.filter { it.taskTitle != suggested.taskTitle }
    }

    fun dispatchPrep(onDone: () -> Unit = {}) {
        val state = uiState.value
        val nowStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        _isDispatched.value = true
        _dispatchedAt.value = nowStr

        val manifest = WorkPrepManifest(
            date = tomorrowDateStr,
            formattedDate = formattedTomorrowHeader,
            readinessScore = state.readinessScore,
            totalTasksScheduled = state.selectedTaskIds.size,
            confirmedContactsCount = state.contractorCalls.count { it.callStatus == CallStatus.CONFIRMED },
            totalContactsCount = state.contractorCalls.size,
            criticalMaterialCount = state.stockAudits.count { it.isDeficit },
            isDispatched = true,
            dispatchedAt = nowStr,
            summaryTextForShare = generateSummaryText(state)
        )

        viewModelScope.launch {
            workPrepRepository.dispatchWorkPrepPlan(manifest, state.taskContractors, state.selectedTaskIds)
            onDone()
        }
    }

    fun generateSummaryText(state: WorkPrepUiState): String {
        val confirmed = state.contractorCalls.count { it.callStatus == CallStatus.CONFIRMED }
        val deficits = state.stockAudits.count { it.isDeficit }
        val selectedTasks = state.tasks.filter { it.isSelected }

        val taskLines = if (selectedTasks.isNotEmpty()) {
            selectedTasks.joinToString("\n") {
                "• ${it.title} (${it.projectName}) -> Contractor: ${it.assignedContractorName ?: "Unassigned"}"
            }
        } else {
            "• No tasks planned"
        }

        return """
*Tomorrow's Site Work Prep Plan (${state.formattedTomorrowHeader})*
*Readiness Score:* ${state.readinessScore}%
*PLANNED TASKS (${selectedTasks.size}):*
$taskLines
*WORKFORCE CONFIRMED:* $confirmed / ${state.contractorCalls.size} Contacts
*MATERIAL DEFICITS:* ${if (deficits > 0) "⚠️ $deficits shortages detected" else "✅ All Required Stock Available"}
_Generated via Constructor Native Android_
        """.trimIndent()
    }
}
