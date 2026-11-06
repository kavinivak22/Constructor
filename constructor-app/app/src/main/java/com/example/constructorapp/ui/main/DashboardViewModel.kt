package com.example.constructorapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.data.repository.TomorrowPrepSummary
import com.example.constructorapp.ui.components.AlertVariant
import com.example.constructorapp.ui.components.DashboardAlert
import com.example.constructorapp.ui.components.sampleAlerts
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ActivityType {
    LABOR,
    MATERIAL,
    PHOTO,
    GENERAL
}

data class RecentActivityItem(
    val id: String,
    val projectName: String,
    val description: String,
    val timestampAgo: String,
    val type: ActivityType
)

data class DashboardUiState(
    val alerts: List<DashboardAlert> = sampleAlerts,
    val isSentinelMode: Boolean = false,
    val tomorrowPrep: TomorrowPrepSummary = TomorrowPrepSummary("Tomorrow", 3, true),
    val activeProjects: List<ProjectDto> = emptyList(),
    val personalTasks: List<TaskEntity> = emptyList(),
    val recentActivities: List<RecentActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val newTaskTitle: String = "",
    val editingTaskId: String? = null,
    val editingTaskTitle: String = ""
)

class DashboardViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _alerts = MutableStateFlow<List<DashboardAlert>>(sampleAlerts)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _newTaskTitle = MutableStateFlow("")
    private val _editingTaskId = MutableStateFlow<String?>(null)
    private val _editingTaskTitle = MutableStateFlow("")

    private val defaultActivities = listOf(
        RecentActivityItem(
            id = "act-1",
            projectName = "Skyline Residency",
            description = "Civil Team: Poured foundation slab and consumed 45 bags of ACC Cement",
            timestampAgo = "2 HOURS AGO",
            type = ActivityType.LABOR
        ),
        RecentActivityItem(
            id = "act-2",
            projectName = "Metro Station Hub",
            description = "Material Intake: Received 12 MT Fe-550 TMT Rebar on site",
            timestampAgo = "5 HOURS AGO",
            type = ActivityType.MATERIAL
        ),
        RecentActivityItem(
            id = "act-3",
            projectName = "Downtown Office Reno",
            description = "Site Inspection: 4 daily progress photos captured & verified",
            timestampAgo = "YESTERDAY",
            type = ActivityType.PHOTO
        )
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.projects,
        repository.personalTasks,
        repository.getTomorrowPrepSummary(),
        _alerts,
        _isRefreshing,
        _errorMessage,
        _newTaskTitle,
        _editingTaskId,
        _editingTaskTitle
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val projects = args[0] as List<ProjectDto>
        @Suppress("UNCHECKED_CAST")
        val tasks = args[1] as List<TaskEntity>
        val prep = args[2] as TomorrowPrepSummary
        @Suppress("UNCHECKED_CAST")
        val currentAlerts = args[3] as List<DashboardAlert>
        val refreshing = args[4] as Boolean
        val error = args[5] as String?
        val newTitle = args[6] as String
        val editId = args[7] as String?
        val editTitle = args[8] as String

        DashboardUiState(
            alerts = currentAlerts,
            isSentinelMode = currentAlerts.isEmpty(),
            tomorrowPrep = prep,
            activeProjects = projects.filter {
                it.status == null || it.status.equals("active", ignoreCase = true) || it.status.equals("in progress", ignoreCase = true)
            }.ifEmpty { projects },
            personalTasks = tasks,
            recentActivities = defaultActivities,
            isLoading = false,
            isRefreshing = refreshing,
            errorMessage = error,
            newTaskTitle = newTitle,
            editingTaskId = editId,
            editingTaskTitle = editTitle
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardUiState(isLoading = true)
    )

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        _isRefreshing.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = repository.refreshProjects()
            _isRefreshing.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun dismissAlert(alertId: String) {
        _alerts.value = _alerts.value.filter { it.id != alertId }
    }

    fun restoreAlerts() {
        _alerts.value = sampleAlerts
    }

    // ─── My Tasks Inline Operations ───────────────────────────────────────

    fun onNewTaskTitleChange(title: String) {
        _newTaskTitle.value = title
    }

    fun addPersonalTask() {
        val title = _newTaskTitle.value.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            repository.addPersonalTask(title)
            _newTaskTitle.value = ""
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTask(taskId, isCompleted)
        }
    }

    fun startEditingTask(taskId: String, currentTitle: String) {
        _editingTaskId.value = taskId
        _editingTaskTitle.value = currentTitle
    }

    fun onEditTaskTitleChange(newTitle: String) {
        _editingTaskTitle.value = newTitle
    }

    fun saveEditedTask(taskId: String) {
        val newTitle = _editingTaskTitle.value.trim()
        if (newTitle.isNotEmpty()) {
            viewModelScope.launch {
                repository.editTaskTitle(taskId, newTitle)
                _editingTaskId.value = null
                _editingTaskTitle.value = ""
            }
        }
    }

    fun cancelEditingTask() {
        _editingTaskId.value = null
        _editingTaskTitle.value = ""
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }
}
