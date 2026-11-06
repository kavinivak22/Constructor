package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.InventoryDao
import com.example.constructorapp.data.local.dao.PurchaseOrderDao
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.PurchaseOrderEntity
import com.example.constructorapp.data.local.entity.PurchaseOrderItemEntity
import com.example.constructorapp.data.local.entity.PurchaseOrderWithItems
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ProcurementUiState(
    val statusFilter: String = "All", // All | Draft | Awaiting Approval | Approved | Ordered | Delivered
    val selectedProjectId: String? = null,
    val ordersWithItems: List<PurchaseOrderWithItems> = emptyList(),
    val filteredOrders: List<PurchaseOrderWithItems> = emptyList(),
    val availableMaterials: List<InventoryAssetEntity> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

class ProcurementViewModel(
    private val purchaseOrderDao: PurchaseOrderDao,
    private val inventoryDao: InventoryDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(
        database.purchaseOrderDao,
        database.inventoryDao
    )

    private val _statusFilter = MutableStateFlow("All")
    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _isCreateDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<ProcurementUiState> = combine(
        purchaseOrderDao.getPurchaseOrdersWithItems(),
        inventoryDao.getAllAssets(),
        _statusFilter,
        _selectedProjectId,
        _isCreateDialogOpen
    ) { allOrders, allAssets, status, projectId, isCreateOpen ->
        val projectFiltered = if (projectId.isNullOrBlank()) {
            allOrders
        } else {
            allOrders.filter { it.order.projectId == projectId }
        }

        val finalFiltered = if (status.equals("All", ignoreCase = true)) {
            projectFiltered
        } else {
            projectFiltered.filter { it.order.status.equals(status, ignoreCase = true) }
        }

        ProcurementUiState(
            statusFilter = status,
            selectedProjectId = projectId,
            ordersWithItems = projectFiltered,
            filteredOrders = finalFiltered,
            availableMaterials = allAssets,
            isCreateDialogOpen = isCreateOpen,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProcurementUiState(isLoading = true)
    )

    fun filterByStatus(status: String) {
        _statusFilter.value = status
    }

    fun filterByProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun setCreateDialogOpen(open: Boolean) {
        _isCreateDialogOpen.value = open
    }

    fun createPurchaseOrder(
        projectId: String,
        supplierName: String,
        supplierContact: String? = null,
        deliveryDate: String? = null,
        specialInstructions: String? = null,
        items: List<PurchaseOrderItemEntity>,
        createdBy: String = "Current User"
    ) {
        viewModelScope.launch {
            val totalAmount = items.sumOf { it.totalPrice }
            val poNumber = "PO-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(4).uppercase()}"
            val orderId = UUID.randomUUID().toString()
            val order = PurchaseOrderEntity(
                id = orderId,
                projectId = projectId,
                poNumber = poNumber,
                supplierName = supplierName,
                supplierContact = supplierContact,
                totalAmount = totalAmount,
                status = "Draft",
                deliveryDate = deliveryDate,
                specialInstructions = specialInstructions,
                createdBy = createdBy,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                syncStatus = 1
            )
            val updatedItems = items.map { it.copy(poId = orderId) }
            purchaseOrderDao.insertFullPurchaseOrder(order, updatedItems)
            _isCreateDialogOpen.value = false
        }
    }

    fun approvePurchaseOrder(poId: String, approverName: String = "Admin") {
        viewModelScope.launch {
            purchaseOrderDao.approvePurchaseOrder(poId, approverName)
        }
    }

    fun updateOrderStatus(poId: String, newStatus: String) {
        viewModelScope.launch {
            purchaseOrderDao.updateOrderStatus(poId, newStatus)
        }
    }
}
