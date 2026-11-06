package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.FinancialsDao
import com.example.constructorapp.data.local.entity.ContractorEntity
import com.example.constructorapp.data.local.entity.ContractorLedgerEntryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ContractorsUiState(
    val contractors: List<ContractorEntity> = emptyList(),
    val filteredContractors: List<ContractorEntity> = emptyList(),
    val selectedContractor: ContractorEntity? = null,
    val selectedLedgerTab: String = "combined", // combined | rate | nmr
    val rateLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val nmrLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val allLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val selectedTradeFilter: String = "All",
    val availableTrades: List<String> = emptyList(),
    val searchQuery: String = "",
    val rateDue: Double = 0.0,
    val rateCleared: Double = 0.0,
    val nmrDue: Double = 0.0,
    val nmrCleared: Double = 0.0,
    val totalDue: Double = 0.0,
    val totalCleared: Double = 0.0,
    val isAddContractorDialogOpen: Boolean = false,
    val isAddLedgerEntryDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

class ContractorsViewModel(
    private val financialsDao: FinancialsDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.financialsDao)

    private val _selectedContractorId = MutableStateFlow<String?>(null)
    private val _selectedTradeFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedLedgerTab = MutableStateFlow("combined")
    private val _isAddContractorDialogOpen = MutableStateFlow(false)
    private val _isAddLedgerEntryDialogOpen = MutableStateFlow(false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _contractorLedgerEntries = _selectedContractorId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList())
        else financialsDao.getAllContractorLedgerEntries(id)
    }

    val uiState: StateFlow<ContractorsUiState> = combine(
        financialsDao.getContractors(),
        _contractorLedgerEntries,
        _selectedContractorId,
        _selectedTradeFilter,
        _searchQuery,
        _selectedLedgerTab,
        _isAddContractorDialogOpen,
        _isAddLedgerEntryDialogOpen
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allContractors = args[0] as List<ContractorEntity>
        val ledgerEntries = args[1] as List<ContractorLedgerEntryEntity>
        val selId = args[2] as String?
        val trade = args[3] as String
        val query = args[4] as String
        val ledgerTab = args[5] as String
        val isAddContOpen = args[6] as Boolean
        val isAddLedgerOpen = args[7] as Boolean

        val trades = listOf("All") + allContractors.mapNotNull { it.category }.distinct().filter { it.isNotBlank() }

        val filtered = allContractors.filter { c ->
            val matchesTrade = trade == "All" || c.category.equals(trade, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                c.name.contains(query, ignoreCase = true) ||
                (c.contactPerson?.contains(query, ignoreCase = true) == true) ||
                (c.category?.contains(query, ignoreCase = true) == true)
            matchesTrade && matchesQuery
        }

        val selected = if (selId != null) {
            allContractors.find { it.id == selId }
        } else {
            filtered.firstOrNull()
        }

        val rateEntries = ledgerEntries.filter { it.payoutClass.equals("rate", ignoreCase = true) }
        val nmrEntries = ledgerEntries.filter { it.payoutClass.equals("nmr", ignoreCase = true) }

        var rDue = 0.0
        var rCleared = 0.0
        rateEntries.forEach {
            if (it.status.equals("paid", ignoreCase = true)) rCleared += it.amount
            else rDue += it.amount
        }

        var nDue = 0.0
        var nCleared = 0.0
        nmrEntries.forEach {
            if (it.status.equals("paid", ignoreCase = true)) nCleared += it.amount
            else nDue += it.amount
        }

        ContractorsUiState(
            contractors = allContractors,
            filteredContractors = filtered,
            selectedContractor = selected,
            selectedLedgerTab = ledgerTab,
            rateLedgerEntries = rateEntries,
            nmrLedgerEntries = nmrEntries,
            allLedgerEntries = ledgerEntries,
            selectedTradeFilter = trade,
            availableTrades = trades,
            searchQuery = query,
            rateDue = rDue,
            rateCleared = rCleared,
            nmrDue = nDue,
            nmrCleared = nCleared,
            totalDue = rDue + nDue,
            totalCleared = rCleared + nCleared,
            isAddContractorDialogOpen = isAddContOpen,
            isAddLedgerEntryDialogOpen = isAddLedgerOpen,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContractorsUiState(isLoading = true)
    )

    fun selectContractor(contractorId: String) {
        _selectedContractorId.value = contractorId
    }

    fun setTradeFilter(trade: String) {
        _selectedTradeFilter.value = trade
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLedgerTab(tab: String) {
        _selectedLedgerTab.value = tab
    }

    fun setAddContractorDialogOpen(open: Boolean) {
        _isAddContractorDialogOpen.value = open
    }

    fun setAddLedgerEntryDialogOpen(open: Boolean) {
        _isAddLedgerEntryDialogOpen.value = open
    }

    fun createContractor(
        name: String,
        category: String? = null,
        contactPerson: String? = null,
        phone: String,
        email: String? = null,
        accountDetails: String? = null,
        gst: String? = null
    ) {
        viewModelScope.launch {
            val contractor = ContractorEntity(
                id = UUID.randomUUID().toString(),
                companyId = "company-default",
                name = name,
                category = category,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                accountDetails = accountDetails,
                gst = gst,
                syncStatus = 1
            )
            financialsDao.upsertContractor(contractor)
            _selectedContractorId.value = contractor.id
            _isAddContractorDialogOpen.value = false
        }
    }

    fun addLedgerEntry(
        contractorId: String,
        projectId: String? = null,
        entryType: String = "work",
        description: String,
        amount: Double,
        status: String = "pending",
        payoutClass: String = "nmr",
        referenceDetails: String? = null
    ) {
        viewModelScope.launch {
            val entry = ContractorLedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                contractorId = contractorId,
                projectId = projectId,
                transactionDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                entryType = entryType,
                description = description,
                amount = amount,
                status = status,
                payoutClass = payoutClass,
                referenceDetails = referenceDetails,
                syncStatus = 1,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            financialsDao.insertContractorLedgerEntry(entry)
            _isAddLedgerEntryDialogOpen.value = false
        }
    }
}
