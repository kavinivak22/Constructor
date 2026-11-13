package com.example.constructorapp.data.model

// ─── Common Entities & DTOs for Multiplatform UI ─────────────────────────────

data class ProjectEntity(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String = "active",
    val progress: Int = 0,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val location: String? = null,
    val budget: Double = 0.0,
    val spent: Double = 0.0,
    val companyId: String = "",
    val siteId: String? = null,
    val thumbnailUrl: String? = null
)

data class ProjectDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = "active",
    val progress: Int = 0,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val location: String? = null,
    val budget: Double = 0.0,
    val spent: Double = 0.0,
    val companyId: String = "",
    val siteId: String? = null,
    val thumbnailUrl: String? = null
)

data class ExpenseEntity(
    val id: String = "",
    val projectId: String? = null,
    val userId: String = "",
    val amount: Double = 0.0,
    val category: String = "materials",
    val description: String = "",
    val expenseDate: String = "",
    val paymentStatus: String = "pending",
    val paymentMode: String = "cash",
    val notes: String? = null,
    val receiver: String? = null,
    val localReceiptUri: String? = null,
    val receiptUrl: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = 0L,
    val isBillable: Boolean = false,
    val contractorId: String? = null,
    val createdBy: String = ""
)

data class MaterialConsumptionItem(
    val id: String = "",
    val worklogId: String = "",
    val worklogTitle: String = "",
    val materialId: String = "",
    val date: String = "",
    val materialName: String = "",
    val quantityConsumed: Double = 0.0,
    val unit: String = "",
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val projectId: String? = null,
    val quantity: Double = quantityConsumed
)

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val selectedProjectId: String? = null,
    val totalSpend: Double = 0.0,
    val totalCashOutflows: Double = 0.0,
    val totalConsumptionCost: Double = 0.0,
    val isFormSheetOpen: Boolean = false,
    val editingExpense: ExpenseEntity? = null,
    val viewingExpense: ExpenseEntity? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedPaymentStatus: String? = null
)

data class ContractorEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val category: String? = null,
    val contactPerson: String? = null,
    val email: String? = null,
    val accountDetails: String? = null,
    val gstNumber: String? = null,
    val payoutType: String = "NMR", // NMR | Rate
    val defaultRate: Double = 0.0,
    val bankAccount: String? = null,
    val ifsc: String? = null,
    val upiId: String? = null,
    val notes: String? = null,
    val companyId: String? = null,
    val syncStatus: Int = 0
)

data class ContractorDto(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val trade: String? = null,
    val payoutType: String = "NMR",
    val defaultRate: Double = 0.0,
    val companyId: String? = null
)

data class ContractorLedgerEntryEntity(
    val id: String = "",
    val contractorId: String = "",
    val transactionDate: String = "",
    val entryType: String = "work",
    val description: String = "",
    val amount: Double = 0.0,
    val status: String = "pending", // pending | paid
    val payoutClass: String = "rate", // rate | nmr
    val referenceDetails: String? = null,
    val date: String = transactionDate,
    val type: String = entryType,
    val refId: String? = referenceDetails,
    val syncStatus: Int = 0
)

data class ContractorsUiState(
    val contractors: List<ContractorEntity> = emptyList(),
    val filteredContractors: List<ContractorEntity> = emptyList(),
    val selectedContractor: ContractorEntity? = null,
    val allLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val rateLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val nmrLedgerEntries: List<ContractorLedgerEntryEntity> = emptyList(),
    val isAddContractorDialogOpen: Boolean = false,
    val isAddLedgerEntryDialogOpen: Boolean = false,
    val rateDue: Double = 0.0,
    val rateCleared: Double = 0.0,
    val nmrDue: Double = 0.0,
    val nmrCleared: Double = 0.0,
    val searchQuery: String = "",
    val availableTrades: List<String> = emptyList(),
    val selectedTradeFilter: String = "All",
    val selectedLedgerTab: String = "combined",
    val isLoading: Boolean = false
)

data class WeeklyPayoutRunEntity(
    val id: String = "",
    val companyId: String = "company-1",
    val weekStartDate: String = "",
    val weekEndDate: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "draft", // draft | approved | paid
    val createdBy: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0,
    val title: String = "",
    val projectId: String = "",
    val periodStart: String = weekStartDate,
    val periodEnd: String = weekEndDate
)

typealias PayoutRunEntity = WeeklyPayoutRunEntity

data class PayoutItemEntity(
    val id: String = "",
    val payoutId: String = "",
    val recipientType: String = "labor_wage", // employee_salary | contractor_wage | vendor_payment | labor_wage | other
    val recipientId: String = "",
    val recipientName: String = "",
    val amountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val status: String = "pending", // pending | paid | held
    val projectId: String? = null,
    val referenceDetails: String? = null,
    val payoutClass: String = "nmr", // rate | nmr
    val notes: String? = null,
    val syncStatus: Int = 0,
    val runId: String = payoutId,
    val recipientRole: String = recipientType,
    val daysWorked: Double = 0.0,
    val rate: Double = 0.0,
    val grossAmount: Double = amountDue,
    val deductions: Double = 0.0,
    val netAmount: Double = amountDue - deductions,
    val contractorId: String? = recipientId,
    val category: String? = null
)

data class PayoutRunWithItems(
    val run: WeeklyPayoutRunEntity,
    val items: List<PayoutItemEntity> = emptyList()
)

data class PaydayUiState(
    val payoutRuns: List<PayoutRunWithItems> = emptyList(),
    val runs: List<PayoutRunWithItems> = payoutRuns,
    val selectedRun: PayoutRunWithItems? = null,
    val isGenerateRunDialogOpen: Boolean = false,
    val isSplitItemDialogOpen: Boolean = false,
    val splitTargetItem: PayoutItemEntity? = null,
    val calcMethod: String = "auto",
    val includeUnpaid: Boolean = true,
    val weekStartDate: String = "",
    val weekEndDate: String = "",
    val totalWeeklyPayout: Double = 0.0,
    val isLoading: Boolean = false
)

data class NotificationEntity(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "general",
    val isRead: Boolean = false,
    val timestamp: String = ""
)

data class ReminderEntity(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = "Medium", // High | Medium | Low
    val location: String? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
)

data class TaskItemDto(
    val id: String? = null,
    val projectId: String? = null,
    val title: String = "",
    val description: String? = null,
    val status: String = "pending",
    val dueDate: String? = null,
    val priority: String? = "medium",
    val assignedTo: String? = null,
    val createdBy: String? = null
) {
    val isCompleted: Boolean
        get() = status.equals("completed", ignoreCase = true)
}

data class PurchaseOrderItemDto(
    val materialName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val unit: String = "units",
    val totalPrice: Double = 0.0
)

data class PurchaseOrderDto(
    val id: String? = null,
    val projectId: String = "",
    val supplierName: String = "",
    val supplierContact: String? = null,
    val deliveryDate: String? = null,
    val specialInstructions: String? = null,
    val status: String = "Draft",
    val totalAmount: Double = 0.0,
    val itemsCount: Int = 0,
    val items: List<PurchaseOrderItemDto> = emptyList()
)

data class InventoryAssetEntity(
    val id: String = "",
    val name: String = "",
    val category: String = "material",
    val assetType: String = "material",
    val quantity: Double = 0.0,
    val currentStock: Double = 0.0,
    val unit: String = "Units",
    val location: String = "Central Warehouse",
    val siteId: String? = null
)

// Permissions
data class CompanyPermissions(
    val manageCompany: Boolean = false,
    val manageEmployees: Boolean = false,
    val viewFinancials: Boolean = false,
    val manageSalaries: Boolean = false
)

data class ProjectsPermissions(
    val viewAllProjects: Boolean = false,
    val createProjects: Boolean = false,
    val editProjects: Boolean = false,
    val deleteProjects: Boolean = false,
    val manageProjectMembers: Boolean = false
)

data class ExpensesPermissions(
    val viewExpenses: Boolean = false,
    val createExpenses: Boolean = false,
    val approveExpenses: Boolean = false,
    val deleteExpenses: Boolean = false,
    val exportExpenses: Boolean = false
)

data class InventoryPermissions(
    val viewInventory: Boolean = false,
    val manageInventory: Boolean = false,
    val logMaterialUsage: Boolean = false,
    val manageSuppliers: Boolean = false,
    val materialEstimation: Boolean = false
)

data class PurchaseOrdersPermissions(
    val viewPurchaseOrders: Boolean = false,
    val createPurchaseOrders: Boolean = false,
    val approvePurchaseOrders: Boolean = false,
    val completePurchaseOrders: Boolean = false
)

data class WorklogsPermissions(
    val viewWorklogs: Boolean = false,
    val createWorklogs: Boolean = false,
    val approveWorklogs: Boolean = false,
    val exportWorklogs: Boolean = false
)

data class PouchesPermissions(
    val viewPouches: Boolean = false,
    val requestPouchFunds: Boolean = false,
    val approvePouchFunds: Boolean = false
)

data class DocumentsPermissions(
    val viewDocuments: Boolean = false,
    val uploadDocuments: Boolean = false,
    val deleteDocuments: Boolean = false
)

data class EmployeePermissions(
    val company: CompanyPermissions = CompanyPermissions(),
    val projects: ProjectsPermissions = ProjectsPermissions(),
    val expenses: ExpensesPermissions = ExpensesPermissions(),
    val inventory: InventoryPermissions = InventoryPermissions(),
    val purchaseOrders: PurchaseOrdersPermissions = PurchaseOrdersPermissions(),
    val worklogs: WorklogsPermissions = WorklogsPermissions(),
    val pouches: PouchesPermissions = PouchesPermissions(),
    val documents: DocumentsPermissions = DocumentsPermissions()
)

data class EmployeeDto(
    val id: String? = null,
    val companyId: String? = null,
    val name: String = "",
    val email: String = "",
    val role: String = "Site Engineer",
    val phone: String? = null,
    val status: String = "active",
    val permissions: EmployeePermissions = EmployeePermissions()
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val isAuthenticated: Boolean = false
)
