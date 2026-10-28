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

    // Employees
    private val _employees = MutableStateFlow<List<com.example.constructorapp.data.remote.EmployeeDto>>(emptyList())
    val employees: StateFlow<List<com.example.constructorapp.data.remote.EmployeeDto>> = _employees.asStateFlow()

    // Contractors
    private val _contractors = MutableStateFlow<List<com.example.constructorapp.data.remote.ContractorDto>>(emptyList())
    val contractors: StateFlow<List<com.example.constructorapp.data.remote.ContractorDto>> = _contractors.asStateFlow()

    // Payout runs
    private val _payoutRuns = MutableStateFlow<List<com.example.constructorapp.data.remote.PayoutRunDto>>(emptyList())
    val payoutRuns: StateFlow<List<com.example.constructorapp.data.remote.PayoutRunDto>> = _payoutRuns.asStateFlow()

    fun resetSubmitState() {
        _worklogSubmitState.value = SubmitState.Idle
    }

    fun loadEmployees() {
        viewModelScope.launch {
            val res = repository.getEmployees()
            if (res.isSuccess) {
                _employees.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun addEmployee(employee: com.example.constructorapp.data.remote.EmployeeDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.createEmployee(employee)
            if (res.isSuccess) {
                loadEmployees()
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun removeEmployee(id: String) {
        viewModelScope.launch {
            val res = repository.deleteEmployee(id)
            if (res.isSuccess) {
                loadEmployees()
            }
        }
    }

    fun loadContractors() {
        viewModelScope.launch {
            val res = repository.getContractors()
            if (res.isSuccess) {
                _contractors.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun loadPayoutRuns(projectId: String) {
        viewModelScope.launch {
            val res = repository.getPayoutRuns(projectId)
            if (res.isSuccess) {
                _payoutRuns.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    // Tasks
    private val _tasks = MutableStateFlow<List<com.example.constructorapp.data.remote.TaskItemDto>>(emptyList())
    val tasks: StateFlow<List<com.example.constructorapp.data.remote.TaskItemDto>> = _tasks.asStateFlow()

    // Purchase Orders
    private val _purchaseOrders = MutableStateFlow<List<com.example.constructorapp.data.remote.PurchaseOrderDto>>(emptyList())
    val purchaseOrders: StateFlow<List<com.example.constructorapp.data.remote.PurchaseOrderDto>> = _purchaseOrders.asStateFlow()

    // Pouch Transactions
    private val _pouchTransactions = MutableStateFlow<List<com.example.constructorapp.data.remote.PouchTransactionDto>>(emptyList())
    val pouchTransactions: StateFlow<List<com.example.constructorapp.data.remote.PouchTransactionDto>> = _pouchTransactions.asStateFlow()

    fun loadTasks(projectId: String) {
        viewModelScope.launch {
            val res = repository.getProjectTasks(projectId)
            if (res.isSuccess) {
                _tasks.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun createTask(task: com.example.constructorapp.data.remote.TaskItemDto, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.createTask(task)
            if (res.isSuccess) {
                loadTasks(task.projectId)
                onDone()
            }
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean, projectId: String) {
        viewModelScope.launch {
            // Optimistic update
            _tasks.value = _tasks.value.map {
                if (it.id == taskId) it.copy(isCompleted = isCompleted) else it
            }
            repository.toggleTask(taskId, isCompleted)
        }
    }

    fun loadPurchaseOrders(projectId: String? = null) {
        viewModelScope.launch {
            val res = repository.getPurchaseOrders(projectId)
            if (res.isSuccess) {
                _purchaseOrders.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun createPurchaseOrder(po: com.example.constructorapp.data.remote.PurchaseOrderDto, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.createPurchaseOrder(po)
            if (res.isSuccess) {
                loadPurchaseOrders(po.projectId)
                onDone()
            }
        }
    }

    fun approvePurchaseOrder(poId: String, projectId: String? = null) {
        viewModelScope.launch {
            val res = repository.updatePurchaseOrderStatus(poId, "Approved")
            if (res.isSuccess) {
                loadPurchaseOrders(projectId)
            }
        }
    }

    fun loadPouchTransactions(projectId: String) {
        viewModelScope.launch {
            val res = repository.getPouchTransactions(projectId)
            if (res.isSuccess) {
                _pouchTransactions.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun createPouchTransaction(transaction: com.example.constructorapp.data.remote.PouchTransactionDto, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.createPouchTransaction(transaction)
            if (res.isSuccess) {
                loadPouchTransactions(transaction.projectId)
                onDone()
            }
        }
    }

    fun saveProjectMaterial(material: ProjectMaterialDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.createProjectMaterial(material)
            if (res.isSuccess) {
                loadProjectMaterials(material.projectId)
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun submitPayoutRun(payoutRun: com.example.constructorapp.data.remote.PayoutRunDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.createPayoutRun(payoutRun)
            if (res.isSuccess) {
                loadPayoutRuns(payoutRun.projectId)
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}
