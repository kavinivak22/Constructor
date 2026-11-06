package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.FinancialsDao
import com.example.constructorapp.data.local.entity.CompanyMaterialPriceEntity
import com.example.constructorapp.data.local.entity.MaterialTakeoffReconciliationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MaterialsUiState(
    val selectedProjectId: String? = null,
    val companyPrices: List<CompanyMaterialPriceEntity> = emptyList(),
    val reconciliationItems: List<MaterialTakeoffReconciliationEntity> = emptyList(),
    val searchQuery: String = "",
    val totalCommitted: Double = 0.0,
    val totalActual: Double = 0.0,
    val netDrift: Double = 0.0,
    val driftPct: Double = 0.0,
    val greenCount: Int = 0,
    val amberCount: Int = 0,
    val redCount: Int = 0,
    val isAddDriftDialogOpen: Boolean = false,
    val isAddCatalogDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

class MaterialsViewModel(
    private val financialsDao: FinancialsDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.financialsDao)

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isAddDriftDialogOpen = MutableStateFlow(false)
    private val _isAddCatalogDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<MaterialsUiState> = combine(
        financialsDao.getCompanyMaterialPrices(),
        financialsDao.getAllReconciliationItems(),
        _selectedProjectId,
        _searchQuery,
        _isAddDriftDialogOpen,
        _isAddCatalogDialogOpen
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val prices = args[0] as List<CompanyMaterialPriceEntity>
        @Suppress("UNCHECKED_CAST")
        val allRecon = args[1] as List<MaterialTakeoffReconciliationEntity>
        val projectId = args[2] as String?
        val query = args[3] as String
        val isAddDrift = args[4] as Boolean
        val isAddCatalog = args[5] as Boolean
        val projectFiltered = if (projectId.isNullOrBlank()) {
            allRecon
        } else {
            allRecon.filter { it.projectId == projectId }
        }

        val searchFiltered = if (query.isBlank()) {
            projectFiltered
        } else {
            projectFiltered.filter {
                it.materialName.contains(query, ignoreCase = true) ||
                (it.materialNameTa?.contains(query, ignoreCase = true) == true)
            }
        }

        var committedSum = 0.0
        var actualSum = 0.0
        var green = 0
        var amber = 0
        var red = 0

        searchFiltered.forEach { item ->
            val committedCost = item.committedQty * item.committedRate
            val actualCost = item.actualQty * item.actualRate
            committedSum += committedCost
            actualSum += actualCost

            val severity = computeSeverity(
                committedQty = item.committedQty,
                committedRate = item.committedRate,
                actualQty = item.actualQty,
                actualRate = item.actualRate
            )
            when (severity) {
                "red" -> red++
                "amber" -> amber++
                else -> green++
            }
        }

        val drift = actualSum - committedSum
        val driftPercentage = if (committedSum > 0.0) (drift / committedSum) * 100.0 else 0.0

        MaterialsUiState(
            selectedProjectId = projectId,
            companyPrices = prices,
            reconciliationItems = searchFiltered,
            searchQuery = query,
            totalCommitted = committedSum,
            totalActual = actualSum,
            netDrift = drift,
            driftPct = driftPercentage,
            greenCount = green,
            amberCount = amber,
            redCount = red,
            isAddDriftDialogOpen = isAddDrift,
            isAddCatalogDialogOpen = isAddCatalog,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MaterialsUiState(isLoading = true)
    )

    fun filterByProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAddDriftDialogOpen(open: Boolean) {
        _isAddDriftDialogOpen.value = open
    }

    fun setAddCatalogDialogOpen(open: Boolean) {
        _isAddCatalogDialogOpen.value = open
    }

    fun addReconciliationItem(
        projectId: String,
        materialName: String,
        materialNameTa: String? = null,
        unit: String,
        committedQty: Double,
        committedRate: Double,
        actualQty: Double,
        actualRate: Double,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val severity = computeSeverity(committedQty, committedRate, actualQty, actualRate)
            val item = MaterialTakeoffReconciliationEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                materialName = materialName,
                materialNameTa = materialNameTa,
                unit = unit,
                committedQty = committedQty,
                committedRate = committedRate,
                actualQty = actualQty,
                actualRate = actualRate,
                severity = severity,
                notes = notes,
                syncStatus = 1,
                lastUpdatedLocally = System.currentTimeMillis()
            )
            financialsDao.upsertReconciliationItem(item)
            _isAddDriftDialogOpen.value = false
        }
    }

    fun deleteReconciliationItem(id: String) {
        viewModelScope.launch {
            financialsDao.deleteReconciliationItem(id)
        }
    }

    fun addCompanyPrice(
        companyId: String = "company-default",
        materialName: String,
        materialNameTa: String? = null,
        category: String = "material",
        unit: String = "bag",
        standardRate: Double,
        supplierName: String? = null
    ) {
        viewModelScope.launch {
            val price = CompanyMaterialPriceEntity(
                id = UUID.randomUUID().toString(),
                companyId = companyId,
                materialName = materialName,
                materialNameTa = materialNameTa,
                category = category,
                unit = unit,
                standardRate = standardRate,
                supplierName = supplierName,
                lastUpdated = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                syncStatus = 1
            )
            financialsDao.upsertCompanyMaterialPrice(price)
            _isAddCatalogDialogOpen.value = false
        }
    }

    fun deleteCompanyPrice(id: String) {
        viewModelScope.launch {
            financialsDao.deleteCompanyMaterialPrice(id)
        }
    }

    companion object {
        fun computeSeverity(
            committedQty: Double,
            committedRate: Double,
            actualQty: Double,
            actualRate: Double
        ): String {
            val rateDriftPct = if (committedRate > 0) ((actualRate - committedRate) / committedRate) * 100.0 else 0.0
            val qtyDriftPct = if (committedQty > 0) ((actualQty - committedQty) / committedQty) * 100.0 else 0.0

            return when {
                rateDriftPct > 10.0 || qtyDriftPct > 10.0 -> "red"
                rateDriftPct > 0.0 || qtyDriftPct > 0.0 -> "amber"
                else -> "green"
            }
        }
    }
}
