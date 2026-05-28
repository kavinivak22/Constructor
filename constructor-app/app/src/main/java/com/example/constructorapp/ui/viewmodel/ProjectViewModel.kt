package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {

    private val _projects = MutableStateFlow<List<ProjectDto>>(emptyList())
    val projects: StateFlow<List<ProjectDto>> = _projects.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Worklogs for selected project
    private val _worklogs = MutableStateFlow<List<DailyWorklogDto>>(emptyList())
    val worklogs: StateFlow<List<DailyWorklogDto>> = _worklogs.asStateFlow()

    // Materials for selected project
    private val _materials = MutableStateFlow<List<ProjectMaterialDto>>(emptyList())
    val materials: StateFlow<List<ProjectMaterialDto>> = _materials.asStateFlow()

    private val _worklogSubmitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val worklogSubmitState: StateFlow<SubmitState> = _worklogSubmitState.asStateFlow()

    init {
        // Collect from cache first
        viewModelScope.launch {
            repository.projects.collect {
                _projects.value = it
            }
        }
    }

    fun loadProjects() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repository.refreshProjects()
            _loading.value = false
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to load projects"
            }
        }
    }

    fun selectProject(projectId: String) {
        loadWorklogs(projectId)
        loadProjectMaterials(projectId)
    }

    private fun loadWorklogs(projectId: String) {
        viewModelScope.launch {
            val result = repository.getDailyWorklogs(projectId)
            if (result.isSuccess) {
                _worklogs.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    private fun loadProjectMaterials(projectId: String) {
        viewModelScope.launch {
            val result = repository.getProjectMaterials(projectId)
            if (result.isSuccess) {
                _materials.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun submitWorklog(
        projectId: String,
        date: String,
        laborEntries: List<LaborEntryInput>,
        onSuccess: () -> Unit
    ) {
        _worklogSubmitState.value = SubmitState.Loading
        viewModelScope.launch {
            val result = repository.createWorklog(projectId, date, laborEntries)
            if (result.isSuccess) {
                _worklogSubmitState.value = SubmitState.Success
                loadWorklogs(projectId) // reload history
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit worklog"
                _worklogSubmitState.value = SubmitState.Error(errorMsg)
            }
        }
    }

    fun resetSubmitState() {
        _worklogSubmitState.value = SubmitState.Idle
    }
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}
