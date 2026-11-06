package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.InventoryDao
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.StockTransferEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InventoryUiState(
    val activeTab: String = "material", // material | tool | machinery
    val searchQuery: String = "",
    val categoryFilter: String = "All",
    val availableCategories: List<String> = emptyList(),
    val materials: List<InventoryAssetEntity> = emptyList(),
    val tools: List<InventoryAssetEntity> = emptyList(),
    val machinery: List<InventoryAssetEntity> = emptyList(),
    val filteredAssets: List<InventoryAssetEntity> = emptyList(),
    val lowStockCount: Int = 0,
    val transfers: List<StockTransferEntity> = emptyList(),
    val isTransferDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

class InventoryViewModel(
    private val inventoryDao: InventoryDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.inventoryDao)

    private val _activeTab = MutableStateFlow("material")
    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow("All")
    private val _isTransferDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<InventoryUiState> = combine(
        inventoryDao.getAllAssets(),
        inventoryDao.getLowStockCount(),
        inventoryDao.getAllTransfers(),
        _activeTab,
        _searchQuery,
        _categoryFilter,
        _isTransferDialogOpen
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allAssets = args[0] as List<InventoryAssetEntity>
        val lowStockCount = args[1] as Int
        @Suppress("UNCHECKED_CAST")
        val transfers = args[2] as List<StockTransferEntity>
        val tab = args[3] as String
        val query = args[4] as String
        val catFilter = args[5] as String
        val isTransferOpen = args[6] as Boolean
        val materialsList = allAssets.filter {
            it.assetType.equals("material", ignoreCase = true) ||
            (it.category.lowercase() != "tool" && it.category.lowercase() != "machinery" && it.assetType.isBlank())
        }
        val toolsList = allAssets.filter {
            it.assetType.equals("tool", ignoreCase = true) || it.category.lowercase() == "tool"
        }
        val machineryList = allAssets.filter {
            it.assetType.equals("machinery", ignoreCase = true) || it.category.lowercase() == "machinery"
        }

        val baseList = when (tab.lowercase()) {
            "tool" -> toolsList
            "machinery" -> machineryList
            else -> materialsList
        }

        val categories = listOf("All") + baseList.map { it.category }.distinct().filter { it.isNotBlank() }

        val filtered = baseList.filter { asset ->
            val matchesQuery = query.isBlank() ||
                asset.name.contains(query, ignoreCase = true) ||
                asset.category.contains(query, ignoreCase = true) ||
                (asset.supplierName?.contains(query, ignoreCase = true) == true)
            val matchesCategory = catFilter == "All" || asset.category.equals(catFilter, ignoreCase = true)
            matchesQuery && matchesCategory
        }

        InventoryUiState(
            activeTab = tab,
            searchQuery = query,
            categoryFilter = catFilter,
            availableCategories = categories,
            materials = materialsList,
            tools = toolsList,
            machinery = machineryList,
            filteredAssets = filtered,
            lowStockCount = lowStockCount,
            transfers = transfers,
            isTransferDialogOpen = isTransferOpen,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun selectTab(tab: String) {
        _activeTab.value = tab
        _categoryFilter.value = "All"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setTransferDialogOpen(open: Boolean) {
        _isTransferDialogOpen.value = open
    }

    fun executeStockTransfer(
        materialId: String,
        sourceType: String,
        destinationType: String,
        fromSiteId: String,
        toSiteId: String,
        quantity: Double,
        unit: String,
        itemName: String = "",
        category: String = "material",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val transferNumber = "TRF-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(4).uppercase()}"
            val transfer = StockTransferEntity(
                id = UUID.randomUUID().toString(),
                transferNumber = transferNumber,
                materialId = materialId,
                itemName = itemName,
                category = category,
                sourceType = sourceType,
                destinationType = destinationType,
                fromSiteId = fromSiteId,
                toSiteId = toSiteId,
                quantity = quantity,
                unit = unit,
                transferDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                notes = notes,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                syncStatus = 1
            )
            inventoryDao.executeTransferTransaction(
                transfer = transfer,
                sourceAssetId = materialId.takeIf { it.isNotBlank() }
            )
            _isTransferDialogOpen.value = false
        }
    }

    fun upsertAsset(asset: InventoryAssetEntity) {
        viewModelScope.launch {
            inventoryDao.upsertAsset(asset)
        }
    }

    fun deleteAsset(id: String) {
        viewModelScope.launch {
            inventoryDao.deleteAssetById(id)
        }
    }

    companion object {
        fun getStockStatusLevel(currentStock: Double, minStockLevel: Double): String {
            return when {
                currentStock <= minStockLevel -> "Low Stock"
                currentStock <= minStockLevel * 1.5 -> "Medium Stock"
                else -> "High Stock"
            }
        }
    }
}
