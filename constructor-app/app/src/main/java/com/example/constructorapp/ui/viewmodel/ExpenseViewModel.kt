package com.example.constructorapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.ExpenseDao
import com.example.constructorapp.data.local.entity.ExpenseEntity
import com.example.constructorapp.util.ImageCompressor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val selectedProjectId: String? = null,
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf("All", "materials", "labor", "equipment", "permits", "subcontractor", "other"),
    val searchQuery: String = "",
    val totalSpend: Double = 0.0,
    val isFormSheetOpen: Boolean = false,
    val selectedExpenseForDetails: ExpenseEntity? = null,
    val isLoading: Boolean = false
)

class ExpenseViewModel(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.expenseDao)

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isFormSheetOpen = MutableStateFlow(false)
    private val _selectedExpenseForDetails = MutableStateFlow<ExpenseEntity?>(null)

    val uiState: StateFlow<ExpenseUiState> = combine(
        expenseDao.getAllCompanyExpenses(),
        _selectedProjectId,
        _selectedCategory,
        _searchQuery,
        _isFormSheetOpen,
        _selectedExpenseForDetails
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allExpenses = args[0] as List<ExpenseEntity>
        val projectId = args[1] as String?
        val category = args[2] as String
        val query = args[3] as String
        val isFormOpen = args[4] as Boolean
        val detailsExpense = args[5] as ExpenseEntity?
        val projectFiltered = if (projectId.isNullOrBlank()) {
            allExpenses
        } else {
            allExpenses.filter { it.projectId == projectId }
        }

        val categoryFiltered = if (category.equals("All", ignoreCase = true)) {
            projectFiltered
        } else {
            projectFiltered.filter { it.category.equals(category, ignoreCase = true) }
        }

        val finalFiltered = if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.description.contains(query, ignoreCase = true) ||
                (it.notes?.contains(query, ignoreCase = true) == true) ||
                (it.receiver?.contains(query, ignoreCase = true) == true)
            }
        }

        val total = finalFiltered.sumOf { it.amount }

        ExpenseUiState(
            expenses = allExpenses,
            filteredExpenses = finalFiltered,
            selectedProjectId = projectId,
            selectedCategory = category,
            searchQuery = query,
            totalSpend = total,
            isFormSheetOpen = isFormOpen,
            selectedExpenseForDetails = detailsExpense,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    fun filterByProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFormSheetOpen(open: Boolean) {
        _isFormSheetOpen.value = open
    }

    fun selectExpenseForDetails(expense: ExpenseEntity?) {
        _selectedExpenseForDetails.value = expense
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

            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
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
        }
    }

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
}
