package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.local.entity.WorkPrepCallEntity
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.DocumentDto
import com.example.constructorapp.data.remote.MaterialLogDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val _selectedWorklogProjectId = MutableStateFlow("all")
    val selectedWorklogProjectId: StateFlow<String> = _selectedWorklogProjectId.asStateFlow()

    private val _worklogsWithDetails = MutableStateFlow<List<WorklogWithDetails>>(emptyList())
    val worklogsWithDetails: StateFlow<List<WorklogWithDetails>> = _worklogsWithDetails.asStateFlow()

    // Work Prep State
    private val _workPrepTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val workPrepTasks: StateFlow<List<TaskEntity>> = _workPrepTasks.asStateFlow()

    private val _workPrepCalls = MutableStateFlow<List<WorkPrepCallEntity>>(emptyList())
    val workPrepCalls: StateFlow<List<WorkPrepCallEntity>> = _workPrepCalls.asStateFlow()

    private val _inventoryMaterials = MutableStateFlow<List<InventoryAssetEntity>>(emptyList())
    val inventoryMaterials: StateFlow<List<InventoryAssetEntity>> = _inventoryMaterials.asStateFlow()

    private val _workPrepDispatched = MutableStateFlow(false)
    val workPrepDispatched: StateFlow<Boolean> = _workPrepDispatched.asStateFlow()

    private val _workPrepDispatchedAt = MutableStateFlow<String?>(null)
    val workPrepDispatchedAt: StateFlow<String?> = _workPrepDispatchedAt.asStateFlow()

    // Materials for selected project
    private val _materials = MutableStateFlow<List<ProjectMaterialDto>>(emptyList())
    val materials: StateFlow<List<ProjectMaterialDto>> = _materials.asStateFlow()

    private val _isMaterialsLoading = MutableStateFlow(false)
    val isMaterialsLoading: StateFlow<Boolean> = _isMaterialsLoading.asStateFlow()

    private val _worklogSubmitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val worklogSubmitState: StateFlow<SubmitState> = _worklogSubmitState.asStateFlow()

    // Documents & Storage for Project Pouch
    private val _documents = MutableStateFlow<List<DocumentDto>>(emptyList())
    val documents: StateFlow<List<DocumentDto>> = _documents.asStateFlow()

    private val _isDocumentsLoading = MutableStateFlow(false)
    val isDocumentsLoading: StateFlow<Boolean> = _isDocumentsLoading.asStateFlow()

    init {
        // Collect from cache first
        viewModelScope.launch {
            repository.projects.collect {
                _projects.value = it
            }
        }
        viewModelScope.launch {
            repository.getAllWorklogs().collect {
                _worklogsWithDetails.value = it
            }
        }
        viewModelScope.launch {
            val res = repository.refreshProjects()
            if (res.isFailure) {
                repository.seedFallbackProjectsIfEmpty()
            }
            repository.syncAllWorklogs()
        }
    }

    fun syncRecentActivities() {
        viewModelScope.launch {
            repository.syncAllWorklogs()
        }
    }

    fun loadProjects() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repository.refreshProjects()
            _loading.value = false
            if (result.isFailure) {
                repository.seedFallbackProjectsIfEmpty()
            }
        }
    }

    fun createProject(project: ProjectDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.createProject(project)
            if (res.isSuccess) {
                loadProjects()
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun updateProject(project: ProjectDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateProject(project)
            if (res.isSuccess) {
                loadProjects()
                onDone(true)
            } else {
                onDone(false)
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

    fun loadProjectMaterials(projectId: String) {
        viewModelScope.launch {
            _isMaterialsLoading.value = true
            val result = repository.getProjectMaterials(projectId)
            if (result.isSuccess) {
                _materials.value = result.getOrNull() ?: emptyList()
            }
            _isMaterialsLoading.value = false
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
                loadTasks(task.projectId ?: "")
                onDone()
            }
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean, projectId: String) {
        viewModelScope.launch {
            // Optimistic update
            _tasks.value = _tasks.value.map {
                if (it.id == taskId) it.copy(status = if (isCompleted) "completed" else "pending") else it
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

    fun loadDocuments(projectId: String?) {
        _isDocumentsLoading.value = true
        viewModelScope.launch {
            val res = repository.getDocuments(projectId)
            _isDocumentsLoading.value = false
            if (res.isSuccess) {
                _documents.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun addDocument(doc: DocumentDto, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.createDocument(doc)
            if (res.isSuccess) {
                loadDocuments(doc.projectId)
                onDone()
            }
        }
    }

    fun deleteDocument(docId: String, projectId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
            loadDocuments(projectId)
            onDone()
        }
    }

    fun saveProjectMaterial(material: ProjectMaterialDto, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.createProjectMaterial(material)
            if (res.isSuccess) {
                material.projectId?.let { loadProjectMaterials(it) }
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun addMaterial(
        projectId: String,
        siteId: String?,
        name: String,
        category: String?,
        initialStock: Double,
        minStock: Double,
        unit: String,
        supplier: String?,
        cost: Double?,
        logExpense: Boolean,
        totalCost: Double?,
        expenseDate: String,
        paymentStatus: String,
        invoiceNumber: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createMaterial(
                siteId = siteId,
                projectId = projectId,
                name = name,
                category = category,
                initialStock = initialStock,
                minStock = minStock,
                unit = unit,
                supplier = supplier,
                cost = cost
            )
            if (res.isSuccess) {
                val createdMat = res.getOrNull()
                if (createdMat?.id != null && initialStock > 0) {
                    val logPurpose = "Initial inventory purchase"
                    repository.createMaterialLog(
                        materialId = createdMat.id,
                        changeAmount = initialStock,
                        purpose = logPurpose
                    )
                }
                if (logExpense) {
                    val expAmount = totalCost ?: (initialStock * (cost ?: 0.0))
                    if (expAmount > 0) {
                        repository.recordMaterialExpense(
                            projectId = projectId,
                            amount = expAmount,
                            description = "Initial purchase: $initialStock $unit of $name${if (!supplier.isNullOrBlank()) " from $supplier" else ""}",
                            date = expenseDate,
                            status = paymentStatus,
                            receiver = supplier,
                            notes = if (!invoiceNumber.isNullOrBlank()) "Bill/Inv: $invoiceNumber" else null
                        )
                    }
                }
                loadProjectMaterials(projectId)
                onDone(true, null)
            } else {
                onDone(false, res.exceptionOrNull()?.message ?: "Failed to add material")
            }
        }
    }

    fun updateStock(
        projectId: String,
        material: ProjectMaterialDto,
        amount: Double,
        isAdd: Boolean,
        unitRate: Double?,
        updateActivePrice: Boolean,
        date: String,
        supplier: String?,
        purpose: String,
        logExpense: Boolean,
        restockAmount: Double?,
        paymentStatus: String,
        invoiceNumber: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val currentQty = material.effectiveQuantity
            val change = if (isAdd) amount else -amount
            val newQty = (currentQty + change).coerceAtLeast(0.0)

            val updatedUnitCost = if (isAdd && updateActivePrice && unitRate != null && unitRate > 0) unitRate else null
            val updatedSupplier = if (isAdd && !supplier.isNullOrBlank()) supplier else null

            val res = repository.updateMaterialStock(
                materialId = material.id ?: "",
                newStock = newQty,
                unitCost = updatedUnitCost,
                supplier = updatedSupplier
            )

            if (res.isSuccess) {
                // Log stock change
                val logPurpose = if (isAdd && unitRate != null && unitRate > 0) {
                    val billCost = restockAmount ?: (amount * unitRate)
                    "Restock: rate ₹$unitRate, total ₹$billCost. $purpose"
                } else {
                    purpose
                }
                repository.createMaterialLog(
                    materialId = material.id ?: "",
                    changeAmount = change,
                    purpose = logPurpose
                )

                if (isAdd && logExpense) {
                    val expAmount = restockAmount ?: (amount * (unitRate ?: material.effectiveCost))
                    if (expAmount > 0) {
                        repository.recordMaterialExpense(
                            projectId = projectId,
                            amount = expAmount,
                            description = "Restock: $amount ${material.effectiveUnit} of ${material.name}${if (!supplier.isNullOrBlank()) " from $supplier" else ""}",
                            date = date,
                            status = paymentStatus,
                            receiver = supplier ?: material.effectiveSupplier,
                            notes = if (!invoiceNumber.isNullOrBlank()) "Bill/Inv: $invoiceNumber" else null
                        )
                    }
                }

                loadProjectMaterials(projectId)
                onDone(true, null)
            } else {
                onDone(false, res.exceptionOrNull()?.message ?: "Failed to update stock")
            }
        }
    }

    fun editMaterial(
        projectId: String,
        materialId: String,
        name: String,
        category: String?,
        currentStock: Double,
        minStock: Double,
        unit: String,
        supplier: String?,
        cost: Double?,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateMaterialDetails(
                materialId = materialId,
                name = name,
                category = category,
                currentStock = currentStock,
                minStock = minStock,
                unit = unit,
                supplier = supplier,
                cost = cost
            )
            if (res.isSuccess) {
                loadProjectMaterials(projectId)
                onDone(true, null)
            } else {
                onDone(false, res.exceptionOrNull()?.message ?: "Failed to edit material")
            }
        }
    }

    fun deleteMaterial(
        projectId: String,
        materialId: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.deleteMaterial(materialId)
            if (res.isSuccess) {
                loadProjectMaterials(projectId)
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun fetchMaterialLogs(
        materialId: String,
        onDone: (List<MaterialLogDto>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.getMaterialLogs(materialId)
            onDone(res.getOrNull() ?: emptyList())
        }
    }

    fun selectWorklogProject(projectId: String) {
        _selectedWorklogProjectId.value = projectId
        viewModelScope.launch {
            repository.getProjectWorklogs(projectId).collect {
                _worklogsWithDetails.value = it
            }
        }
    }

    fun deleteWorklog(worklogId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWorklogById(worklogId)
            selectWorklogProject(_selectedWorklogProjectId.value)
            onDone()
        }
    }

    fun loadWorkPrep(tomorrowDate: String, projectId: String? = null) {
        viewModelScope.launch {
            repository.getWorkPrepTasks(tomorrowDate, projectId).collect {
                _workPrepTasks.value = it
            }
        }
        viewModelScope.launch {
            repository.getWorkPrepCalls(tomorrowDate).collect {
                _workPrepCalls.value = it
            }
        }
        viewModelScope.launch {
            repository.getInventoryMaterials().collect {
                _inventoryMaterials.value = it
            }
        }
    }

    fun updateWorkPrepCallStatus(callId: String, status: String) {
        viewModelScope.launch {
            repository.updateWorkPrepCallStatus(callId, status)
        }
    }

    fun addWorkPrepCall(call: WorkPrepCallEntity) {
        viewModelScope.launch {
            repository.upsertWorkPrepCall(call)
        }
    }

    fun dispatchWorkPrepPlan(onDone: () -> Unit = {}) {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        _workPrepDispatched.value = true
        _workPrepDispatchedAt.value = nowStr
        onDone()
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
