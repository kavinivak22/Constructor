package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.FinancialsDao
import com.example.constructorapp.data.local.entity.PayoutItemEntity
import com.example.constructorapp.data.local.entity.PayoutRunWithItems
import com.example.constructorapp.data.local.entity.WeeklyPayoutRunEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PaydayUiState(
    val payoutRuns: List<PayoutRunWithItems> = emptyList(),
    val selectedRun: PayoutRunWithItems? = null,
    val isGenerateRunDialogOpen: Boolean = false,
    val isSplitItemDialogOpen: Boolean = false,
    val splitTargetItem: PayoutItemEntity? = null,
    val calcMethod: String = "auto", // auto | wage_wise | work_wise
    val includeUnpaid: Boolean = true,
    val weekStartDate: String = "",
    val weekEndDate: String = "",
    val totalWeeklyPayout: Double = 0.0,
    val isLoading: Boolean = false
)

class PaydayViewModel(
    private val financialsDao: FinancialsDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.financialsDao)

    private val _selectedRunId = MutableStateFlow<String?>(null)
    private val _isGenerateRunDialogOpen = MutableStateFlow(false)
    private val _isSplitItemDialogOpen = MutableStateFlow(false)
    private val _splitTargetItem = MutableStateFlow<PayoutItemEntity?>(null)
    private val _calcMethod = MutableStateFlow("auto")
    private val _includeUnpaid = MutableStateFlow(true)
    private val _weekStartDate = MutableStateFlow("")
    private val _weekEndDate = MutableStateFlow("")

    val uiState: StateFlow<PaydayUiState> = combine(
        financialsDao.getAllPayoutRunsWithItems(),
        _selectedRunId,
        _isGenerateRunDialogOpen,
        _isSplitItemDialogOpen,
        _splitTargetItem,
        _calcMethod,
        _includeUnpaid,
        _weekStartDate,
        _weekEndDate
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val runs = args[0] as List<PayoutRunWithItems>
        val selectedId = args[1] as String?
        val isGenOpen = args[2] as Boolean
        val isSplitOpen = args[3] as Boolean
        val splitItem = args[4] as PayoutItemEntity?
        val method = args[5] as String
        val unpaid = args[6] as Boolean
        val start = args[7] as String
        val end = args[8] as String

        val selected = if (selectedId != null) {
            runs.find { it.run.id == selectedId }
        } else {
            runs.firstOrNull()
        }

        val totalAmount = selected?.items?.sumOf { it.amountPaid.takeIf { p -> p > 0 } ?: it.amountDue } ?: 0.0

        PaydayUiState(
            payoutRuns = runs,
            selectedRun = selected,
            isGenerateRunDialogOpen = isGenOpen,
            isSplitItemDialogOpen = isSplitOpen,
            splitTargetItem = splitItem,
            calcMethod = method,
            includeUnpaid = unpaid,
            weekStartDate = start,
            weekEndDate = end,
            totalWeeklyPayout = totalAmount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaydayUiState(isLoading = true)
    )

    init {
        initDefaultDates()
    }

    private fun initDefaultDates() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = sdf.format(cal.time)
        _weekStartDate.value = start
        _weekEndDate.value = end
    }

    fun selectRun(runId: String) {
        _selectedRunId.value = runId
    }

    fun setGenerateRunDialogOpen(open: Boolean) {
        _isGenerateRunDialogOpen.value = open
    }

    fun setSplitItemDialogOpen(open: Boolean, item: PayoutItemEntity? = null) {
        _splitTargetItem.value = item
        _isSplitItemDialogOpen.value = open
    }

    fun setCalcMethod(method: String) {
        _calcMethod.value = method
    }

    fun setIncludeUnpaid(include: Boolean) {
        _includeUnpaid.value = include
    }

    fun setDateRange(startDate: String, endDate: String) {
        _weekStartDate.value = startDate
        _weekEndDate.value = endDate
    }

    fun generatePayoutRun(
        companyId: String = "company-default",
        startDate: String = _weekStartDate.value,
        endDate: String = _weekEndDate.value,
        method: String = _calcMethod.value,
        includePreviousUnpaid: Boolean = _includeUnpaid.value,
        customItems: List<PayoutItemEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val runId = UUID.randomUUID().toString()
            val total = customItems.sumOf { it.amountDue }

            val run = WeeklyPayoutRunEntity(
                id = runId,
                companyId = companyId,
                weekStartDate = startDate,
                weekEndDate = endDate,
                totalAmount = total,
                status = "draft",
                createdBy = "Admin",
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                syncStatus = 1
            )

            val updatedItems = customItems.map { it.copy(payoutId = runId) }
            financialsDao.insertFullPayoutRun(run, updatedItems)
            _selectedRunId.value = runId
            _isGenerateRunDialogOpen.value = false
        }
    }

    fun approvePayoutRun(runId: String) {
        viewModelScope.launch {
            financialsDao.updatePayoutRunStatus(runId, "approved")
        }
    }

    fun disbursePayoutRun(runId: String) {
        viewModelScope.launch {
            financialsDao.updatePayoutRunStatus(runId, "paid")
        }
    }

    fun updatePayoutItemStatus(itemId: String, status: String, amountPaid: Double) {
        viewModelScope.launch {
            financialsDao.updatePayoutItem(itemId, status, amountPaid)
        }
    }

    fun splitPayoutItem(
        originalItem: PayoutItemEntity,
        rateAmount: Double,
        nmrAmount: Double
    ) {
        viewModelScope.launch {
            val rateItem = originalItem.copy(
                id = UUID.randomUUID().toString(),
                payoutClass = "rate",
                amountDue = rateAmount,
                amountPaid = if (originalItem.status == "paid") rateAmount else 0.0,
                notes = "${originalItem.notes ?: ""} (Split Rate contract)".trim()
            )
            val nmrItem = originalItem.copy(
                id = UUID.randomUUID().toString(),
                payoutClass = "nmr",
                amountDue = nmrAmount,
                amountPaid = if (originalItem.status == "paid") nmrAmount else 0.0,
                notes = "${originalItem.notes ?: ""} (Split NMR labor)".trim()
            )
            financialsDao.deletePayoutItemById(originalItem.id)
            financialsDao.insertPayoutItems(listOf(rateItem, nmrItem))
            _isSplitItemDialogOpen.value = false
            _splitTargetItem.value = null
        }
    }
}
