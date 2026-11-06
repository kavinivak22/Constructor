package com.example.constructorapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<ProjectDto> = emptyList(),
    val filteredProjects: List<ProjectDto> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: String = "All",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isCreateDialogOpen: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val editingProject: ProjectDto? = null
)

class ProjectsViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow("All")
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isCreateDialogOpen = MutableStateFlow(false)
    private val _isEditDialogOpen = MutableStateFlow(false)
    private val _editingProject = MutableStateFlow<ProjectDto?>(null)

    val uiState: StateFlow<ProjectsUiState> = combine(
        repository.projects,
        _searchQuery,
        _selectedStatus,
        _isRefreshing,
        _errorMessage,
        _isCreateDialogOpen,
        _isEditDialogOpen,
        _editingProject
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allProjects = args[0] as List<ProjectDto>
        val query = args[1] as String
        val status = args[2] as String
        val refreshing = args[3] as Boolean
        val error = args[4] as String?
        val createOpen = args[5] as Boolean
        val editOpen = args[6] as Boolean
        val editProject = args[7] as ProjectDto?

        val filtered = allProjects.filter { p ->
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                (p.clientName?.contains(query, ignoreCase = true) == true) ||
                (p.location?.contains(query, ignoreCase = true) == true)

            val matchesStatus = when (status) {
                "Active" -> p.status == null || p.status.equals("active", ignoreCase = true) || p.status.equals("in progress", ignoreCase = true)
                "Completed" -> p.status?.equals("completed", ignoreCase = true) == true
                "On-Hold" -> p.status?.equals("on-hold", ignoreCase = true) == true || p.status?.equals("planning", ignoreCase = true) == true
                else -> true
            }
            matchesQuery && matchesStatus
        }

        ProjectsUiState(
            projects = allProjects,
            filteredProjects = filtered,
            searchQuery = query,
            selectedStatus = status,
            isLoading = false,
            isRefreshing = refreshing,
            errorMessage = error,
            isCreateDialogOpen = createOpen,
            isEditDialogOpen = editOpen,
            editingProject = editProject
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProjectsUiState(isLoading = true)
    )

    init {
        refreshProjects()
    }

    fun refreshProjects() {
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedStatus(status: String) {
        _selectedStatus.value = status
    }

    fun openCreateDialog() {
        _isCreateDialogOpen.value = true
    }

    fun closeCreateDialog() {
        _isCreateDialogOpen.value = false
    }

    fun openEditDialog(project: ProjectDto) {
        _editingProject.value = project
        _isEditDialogOpen.value = true
    }

    fun closeEditDialog() {
        _isEditDialogOpen.value = false
        _editingProject.value = null
    }

    fun createProject(project: ProjectDto, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.createProject(project)
            if (result.isSuccess) {
                closeCreateDialog()
                refreshProjects()
                onComplete(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
                onComplete(false)
            }
        }
    }

    fun updateProject(project: ProjectDto, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateProject(project)
            if (result.isSuccess) {
                closeEditDialog()
                refreshProjects()
                onComplete(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
                onComplete(false)
            }
        }
    }
}
