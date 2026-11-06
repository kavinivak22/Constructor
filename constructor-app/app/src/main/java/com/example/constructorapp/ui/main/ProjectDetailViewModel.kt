package com.example.constructorapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.data.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val projectId: String,
    val project: ProjectDto? = null,
    val tasks: List<TaskItemDto> = emptyList(),
    val worklogs: List<DailyWorklogDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditProjectOpen: Boolean = false,
    val isCreateTaskOpen: Boolean = false,
    val teamMemberCount: Int = 12
)

class ProjectDetailViewModel(
    val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    private val _isEditProjectOpen = MutableStateFlow(false)
    private val _isCreateTaskOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _tasks = MutableStateFlow<List<TaskItemDto>>(emptyList())
    private val _worklogs = MutableStateFlow<List<DailyWorklogDto>>(emptyList())

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        repository.getProjectById(projectId),
        _tasks,
        _worklogs,
        _isEditProjectOpen,
        _isCreateTaskOpen,
        _errorMessage
    ) { args: Array<Any?> ->
        val project = args[0] as ProjectDto?
        @Suppress("UNCHECKED_CAST")
        val tasks = args[1] as List<TaskItemDto>
        @Suppress("UNCHECKED_CAST")
        val worklogs = args[2] as List<DailyWorklogDto>
        val editOpen = args[3] as Boolean
        val taskOpen = args[4] as Boolean
        val error = args[5] as String?

        ProjectDetailUiState(
            projectId = projectId,
            project = project,
            tasks = tasks,
            worklogs = worklogs,
            isLoading = project == null,
            errorMessage = error,
            isEditProjectOpen = editOpen,
            isCreateTaskOpen = taskOpen,
            teamMemberCount = 12
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProjectDetailUiState(projectId = projectId, isLoading = true)
    )

    init {
        loadTasks()
        loadWorklogs()
    }

    fun loadTasks() {
        viewModelScope.launch {
            val result = repository.getProjectTasks(projectId)
            if (result.isSuccess) {
                _tasks.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun loadWorklogs() {
        viewModelScope.launch {
            val result = repository.getDailyWorklogs(projectId)
            if (result.isSuccess) {
                _worklogs.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index != -1) {
            current[index] = current[index].copy(status = if (isCompleted) "completed" else "pending")
            _tasks.value = current
        }
        viewModelScope.launch {
            repository.toggleTask(taskId, isCompleted)
            loadTasks()
        }
    }

    fun createTask(task: TaskItemDto, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.createTask(task)
            if (result.isSuccess) {
                closeCreateTask()
                loadTasks()
                onComplete()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun updateProject(project: ProjectDto, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateProject(project)
            if (result.isSuccess) {
                closeEditProject()
                repository.refreshProjects()
                onComplete()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun openEditProject() {
        _isEditProjectOpen.value = true
    }

    fun closeEditProject() {
        _isEditProjectOpen.value = false
    }

    fun openCreateTask() {
        _isCreateTaskOpen.value = true
    }

    fun closeCreateTask() {
        _isCreateTaskOpen.value = false
    }
}
