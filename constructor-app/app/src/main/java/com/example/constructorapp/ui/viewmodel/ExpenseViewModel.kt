package com.example.constructorapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.dao.ExpenseDao
import com.example.constructorapp.data.local.dao.ProjectDao
import com.example.constructorapp.data.local.dao.WorklogDao
import com.example.constructorapp.data.local.entity.ExpenseEntity
import com.example.constructorapp.util.ImageCompressor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MaterialConsumptionItem(
    val id: String,
    val worklogId: String,
    val worklogTitle: String,
    val date: String,
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String,
    val unitCost: Double,
    val totalCost: Double
)

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val selectedProjectId: String? = null,
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf("All", "materials", "labor", "equipment", "permits", "subcontractor", "other"),
    val searchQuery: String = "",
    val dateFilterPreset: String = "all", // "all", "today", "yesterday", "7days", "this_month", "last_month"
    val userFilter: String = "all", // "all", "me"
    val totalSpend: Double = 0.0,
    val consumptionLogs: List<MaterialConsumptionItem> = emptyList(),
    val totalConsumptionCost: Double = 0.0,
    val isFormSheetOpen: Boolean = false,
    val editingExpense: ExpenseEntity? = null,
    val viewingExpense: ExpenseEntity? = null,
    val isConsumptionSheetOpen: Boolean = false,
    val isDeleteAlertOpen: Boolean = false,
    val expenseToDelete: ExpenseEntity? = null,
    val isLoading: Boolean = false
)

class ExpenseViewModel(
    private val expenseDao: ExpenseDao,
    private val projectDao: ProjectDao,
    private val worklogDao: WorklogDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(
        database.expenseDao,
        database.projectDao,
        database.worklogDao
    )

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _dateFilterPreset = MutableStateFlow("all")
    private val _userFilter = MutableStateFlow("all")
    private val _isFormSheetOpen = MutableStateFlow(false)
    private val _editingExpense = MutableStateFlow<ExpenseEntity?>(null)
    private val _viewingExpense = MutableStateFlow<ExpenseEntity?>(null)
    private val _isConsumptionSheetOpen = MutableStateFlow(false)
    private val _expenseToDelete = MutableStateFlow<ExpenseEntity?>(null)
    private val _isDeleteAlertOpen = MutableStateFlow(false)

    private data class FilterParams(
        val projectId: String?,
        val category: String,
        val query: String,
        val datePreset: String,
        val userFilter: String,
        val isFormOpen: Boolean,
        val editing: ExpenseEntity?,
        val viewing: ExpenseEntity?,
        val isConsumptionOpen: Boolean,
        val toDelete: ExpenseEntity?,
        val isDeleteOpen: Boolean
    )

    private val _filters = combine(
        combine(_selectedProjectId, _selectedCategory, _searchQuery, _dateFilterPreset, _userFilter) { p, c, q, d, u ->
            arrayOf(p, c, q, d, u)
        },
        combine(_isFormSheetOpen, _editingExpense, _viewingExpense, _isConsumptionSheetOpen, _expenseToDelete) { f, ed, v, cons, del ->
            arrayOf(f, ed, v, cons, del)
        },
        _isDeleteAlertOpen
    ) { part1, part2, isDelOpen ->
        FilterParams(
            projectId = part1[0] as String?,
            category = part1[1] as String,
            query = part1[2] as String,
            datePreset = part1[3] as String,
            userFilter = part1[4] as String,
            isFormOpen = part2[0] as Boolean,
            editing = part2[1] as ExpenseEntity?,
            viewing = part2[2] as ExpenseEntity?,
            isConsumptionOpen = part2[3] as Boolean,
            toDelete = part2[4] as ExpenseEntity?,
            isDeleteOpen = isDelOpen
        )
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        expenseDao.getAllCompanyExpenses(),
        projectDao.getProjects(),
        worklogDao.getAllWorklogsWithDetails(),
        _filters
    ) { allExpenses, projects, allWorklogs, filters ->
        val projectFiltered = if (filters.projectId.isNullOrBlank() || filters.projectId == "all") {
            allExpenses
        } else {
            allExpenses.filter { it.projectId == filters.projectId }
        }

        val categoryFiltered = if (filters.category.equals("All", ignoreCase = true)) {
            projectFiltered
        } else {
            projectFiltered.filter { it.category.equals(filters.category, ignoreCase = true) }
        }

        val userFiltered = if (filters.userFilter == "me") {
            categoryFiltered.filter { it.userId == "current-user" || it.userId.isNotBlank() }
        } else {
            categoryFiltered
        }

        val dateFiltered = userFiltered.filter { expense ->
            matchesDatePreset(expense.expenseDate, filters.datePreset)
        }

        val finalFiltered = if (filters.query.isBlank()) {
            dateFiltered
        } else {
            dateFiltered.filter {
                it.description.contains(filters.query, ignoreCase = true) ||
                (it.notes?.contains(filters.query, ignoreCase = true) == true) ||
                (it.receiver?.contains(filters.query, ignoreCase = true) == true) ||
                it.category.contains(filters.query, ignoreCase = true)
            }
        }

        val total = finalFiltered.sumOf { it.amount }

        // Compute Material Consumption Logs like webapp
        val relevantWorklogs = if (filters.projectId.isNullOrBlank() || filters.projectId == "all") {
            allWorklogs
        } else {
            allWorklogs.filter { it.worklog.projectId == filters.projectId }
        }

        val consumptionItems = mutableListOf<MaterialConsumptionItem>()
        relevantWorklogs.forEach { wl ->
            wl.materials.forEach { wm ->
                val unitCost = estimateUnitCost(wm.materialName)
                val totalCost = wm.quantityConsumed * unitCost
                consumptionItems.add(
                    MaterialConsumptionItem(
                        id = wm.id,
                        worklogId = wl.worklog.id,
                        worklogTitle = wl.worklog.title,
                        date = wl.worklog.date,
                        materialName = wm.materialName,
                        quantityConsumed = wm.quantityConsumed,
                        unit = wm.unit ?: "units",
                        unitCost = unitCost,
                        totalCost = totalCost
                    )
                )
            }
        }
        val totalConsumption = consumptionItems.sumOf { it.totalCost }

        ExpenseUiState(
            expenses = allExpenses,
            filteredExpenses = finalFiltered,
            projects = projects,
            selectedProjectId = filters.projectId,
            selectedCategory = filters.category,
            searchQuery = filters.query,
            dateFilterPreset = filters.datePreset,
            userFilter = filters.userFilter,
            totalSpend = total,
            consumptionLogs = consumptionItems,
            totalConsumptionCost = totalConsumption,
            isFormSheetOpen = filters.isFormOpen,
            editingExpense = filters.editing,
            viewingExpense = filters.viewing,
            isConsumptionSheetOpen = filters.isConsumptionOpen,
            isDeleteAlertOpen = filters.isDeleteOpen,
            expenseToDelete = filters.toDelete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    fun filterByProject(projectId: String?) {
        _selectedProjectId.value = if (projectId == "all") null else projectId
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilterPreset(preset: String) {
        _dateFilterPreset.value = preset
    }

    fun setUserFilter(filter: String) {
        _userFilter.value = filter
    }

    fun setFormSheetOpen(open: Boolean) {
        _isFormSheetOpen.value = open
        if (!open) {
            _editingExpense.value = null
        }
    }

    fun setEditingExpense(expense: ExpenseEntity?) {
        _editingExpense.value = expense
        _isFormSheetOpen.value = (expense != null)
    }

    fun setViewingExpense(expense: ExpenseEntity?) {
        _viewingExpense.value = expense
    }

    fun setConsumptionSheetOpen(open: Boolean) {
        _isConsumptionSheetOpen.value = open
    }

    fun requestDeleteExpense(expense: ExpenseEntity) {
        _expenseToDelete.value = expense
        _isDeleteAlertOpen.value = true
    }

    fun confirmDeleteExpense() {
        val expense = _expenseToDelete.value
        if (expense != null) {
            deleteExpense(expense.id)
            if (_viewingExpense.value?.id == expense.id) {
                _viewingExpense.value = null
            }
        }
        _isDeleteAlertOpen.value = false
        _expenseToDelete.value = null
    }

    fun cancelDeleteExpense() {
        _isDeleteAlertOpen.value = false
        _expenseToDelete.value = null
    }

    fun saveExpense(
        id: String?,
        projectId: String?,
        amount: Double,
        category: String,
        description: String,
        expenseDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        paymentMode: String = "cash",
        paymentStatus: String = "paid",
        notes: String? = null,
        receiver: String? = null,
        localReceiptUri: Uri? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            var compressedPath: String? = null
            if (localReceiptUri != null && context != null) {
                try {
                    val file = ImageCompressor.compressImage(context, localReceiptUri)
                    compressedPath = file.absolutePath
                } catch (e: Exception) {
                    compressedPath = localReceiptUri.toString()
                }
            }

            val expenseId = id ?: UUID.randomUUID().toString()
            val expense = ExpenseEntity(
                id = expenseId,
                projectId = projectId,
                userId = "current-user",
                amount = amount,
                category = category,
                description = description,
                expenseDate = expenseDate,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                notes = notes,
                receiver = receiver,
                localReceiptUri = compressedPath,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                syncStatus = 1,
                lastUpdatedLocally = System.currentTimeMillis()
            )
            expenseDao.upsertExpense(expense)
            _isFormSheetOpen.value = false
            _editingExpense.value = null
        }
    }

    fun createExpense(
        projectId: String?,
        amount: Double,
        category: String,
        description: String,
        expenseDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        paymentMode: String = "cash",
        paymentStatus: String = "paid",
        notes: String? = null,
        receiver: String? = null,
        localReceiptUri: Uri? = null,
        context: Context? = null
    ) = saveExpense(
        id = null,
        projectId = projectId,
        amount = amount,
        category = category,
        description = description,
        expenseDate = expenseDate,
        paymentMode = paymentMode,
        paymentStatus = paymentStatus,
        notes = notes,
        receiver = receiver,
        localReceiptUri = localReceiptUri,
        context = context
    )

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            expenseDao.deleteExpenseById(id)
        }
    }

    fun updatePaymentStatus(id: String, status: String) {
        viewModelScope.launch {
            expenseDao.updateExpensePaymentStatus(id, status)
        }
    }

    private fun matchesDatePreset(expenseDate: String, preset: String): Boolean {
        if (preset == "all") return true
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return when (preset) {
            "today" -> expenseDate == today
            "yesterday" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                expenseDate == yesterday
            }
            "7days" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                expenseDate in sevenDaysAgo..today
            }
            "this_month" -> {
                val thisMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                expenseDate.startsWith(thisMonth)
            }
            "last_month" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                val lastMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
                expenseDate.startsWith(lastMonth)
            }
            else -> true
        }
    }

    private fun estimateUnitCost(name: String): Double {
        val lower = name.lowercase()
        return when {
            lower.contains("cement") -> 380.0
            lower.contains("steel") || lower.contains("tmt") || lower.contains("rebar") -> 65.0
            lower.contains("sand") -> 45.0
            lower.contains("aggregate") || lower.contains("gravel") -> 40.0
            lower.contains("brick") -> 12.0
            lower.contains("block") -> 45.0
            lower.contains("paint") -> 280.0
            lower.contains("tile") -> 60.0
            lower.contains("pipe") -> 180.0
            lower.contains("wire") -> 95.0
            else -> 350.0
        }
    }
}
