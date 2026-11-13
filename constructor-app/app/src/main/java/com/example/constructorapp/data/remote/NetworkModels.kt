package com.example.constructorapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: SupabaseUser
)

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String? = null
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@Serializable
data class SignupResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: SupabaseUser? = null
)

@Serializable
data class PasswordResetRequest(
    val email: String
)

@Serializable
data class CompanyCreateRequest(
    val name: String,
    val address: String,
    val phone: String,
    val website: String? = null,
    @SerialName("business_type") val businessType: String,
    @SerialName("company_size") val companySize: String,
    @SerialName("owner_id") val ownerId: String
)

@Serializable
data class CompanyDto(
    val id: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    @SerialName("company_size") val companySize: String? = null,
    @SerialName("owner_id") val ownerId: String? = null
)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("company_id") val companyId: String? = null,
    val role: String? = null,
    val status: String? = null,
    val phone: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null
)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    val status: String? = null,
    val progress: Int = 0,
    @SerialName("clientName") val clientName: String? = null,
    @SerialName("clientPhone") val clientPhone: String? = null,
    val location: String? = null,
    val budget: Double = 0.0,
    @SerialName("companyId") val companyId: String,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

@Serializable
data class ProjectSiteDto(
    val id: String,
    @SerialName("site_id") val siteId: String? = null
)

@Serializable
data class DailyWorklogDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    val title: String? = null,
    val date: String, // Format YYYY-MM-DD
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class LaborEntryDto(
    val id: String? = null,
    @SerialName("worklog_id") val worklogId: String,
    @SerialName("contractor_name") val contractorName: String,
    val category: String? = null,
    @SerialName("work_description") val workDescription: String? = null,
    @SerialName("payment_status") val paymentStatus: String = "Pending",
    @SerialName("payout_class") val payoutClass: String = "NMR",
    @SerialName("work_type") val workType: String? = null,
    @SerialName("work_done_quantity") val workDoneQuantity: Double? = null,
    @SerialName("work_done_unit") val workDoneUnit: String? = null,
    @SerialName("payout_rate") val payoutRate: Double? = null,
    @SerialName("total_payout") val totalPayout: Double? = null
)

@Serializable
data class WorkerCountDto(
    val id: String? = null,
    @SerialName("labor_entry_id") val laborEntryId: String,
    @SerialName("worker_type") val workerType: String,
    val count: Int
)

@Serializable
data class WorklogMaterialDto(
    val id: String? = null,
    @SerialName("worklog_id") val worklogId: String,
    @SerialName("project_material_id") val projectMaterialId: String? = null,
    @SerialName("material_name") val materialName: String,
    @SerialName("quantity_consumed") val quantityConsumed: Double,
    val unit: String? = null
)

@Serializable
data class WorklogPhotoDto(
    val id: String? = null,
    @SerialName("worklog_id") val worklogId: String,
    @SerialName("photo_url") val photoUrl: String,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProjectMaterialDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    val name: String = "",
    val category: String? = null,
    @SerialName("unit_of_measurement") val unitOfMeasurement: String? = null,
    @SerialName("current_stock") val currentStock: Double? = null,
    @SerialName("minimum_stock_level") val minimumStockLevel: Double? = null,
    @SerialName("unit_cost") val unitCost: Double? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("supplier_contact") val supplierContact: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val quantity: Double = 0.0,
    val unit: String = ""
) {
    val effectiveQuantity: Double
        get() = currentStock ?: quantity

    val effectiveMinQuantity: Double
        get() = minimumStockLevel ?: 0.0

    val effectiveUnit: String
        get() = unitOfMeasurement?.takeIf { it.isNotBlank() } ?: unit.takeIf { it.isNotBlank() } ?: "units"

    val effectiveCost: Double
        get() = unitCost ?: 0.0

    val effectiveSupplier: String
        get() = supplierName ?: ""
}

@Serializable
data class MaterialLogDto(
    val id: String? = null,
    @SerialName("material_id") val materialId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("change_amount") val changeAmount: Double = 0.0,
    val purpose: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PersonalNoteDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    val priority: String = "Medium",
    val location: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

// ─── Granular Permissions (Matching Web AddEmployeeDialog) ───────────────────

@Serializable
data class CompanyPermissions(
    val manageCompany: Boolean = false,
    val manageEmployees: Boolean = false,
    val viewFinancials: Boolean = false,
    val manageSalaries: Boolean = false
)

@Serializable
data class ProjectsPermissions(
    val viewAllProjects: Boolean = false,
    val createProjects: Boolean = false,
    val editProjects: Boolean = false,
    val deleteProjects: Boolean = false,
    val manageProjectMembers: Boolean = false
)

@Serializable
data class ExpensesPermissions(
    val viewExpenses: Boolean = false,
    val createExpenses: Boolean = false,
    val approveExpenses: Boolean = false,
    val deleteExpenses: Boolean = false,
    val exportExpenses: Boolean = false
)

@Serializable
data class InventoryPermissions(
    val viewInventory: Boolean = false,
    val manageInventory: Boolean = false,
    val logMaterialUsage: Boolean = false,
    val manageSuppliers: Boolean = false,
    val materialEstimation: Boolean = false
)

@Serializable
data class PurchaseOrdersPermissions(
    val viewPurchaseOrders: Boolean = false,
    val createPurchaseOrders: Boolean = false,
    val approvePurchaseOrders: Boolean = false,
    val completePurchaseOrders: Boolean = false
)

@Serializable
data class WorklogsPermissions(
    val viewWorklogs: Boolean = false,
    val createWorklogs: Boolean = false,
    val approveWorklogs: Boolean = false,
    val exportWorklogs: Boolean = false
)

@Serializable
data class PouchesPermissions(
    val viewPouches: Boolean = false,
    val requestPouchFunds: Boolean = false,
    val approvePouchFunds: Boolean = false
)

@Serializable
data class DocumentsPermissions(
    val viewDocuments: Boolean = false,
    val uploadDocuments: Boolean = false,
    val deleteDocuments: Boolean = false
)

@Serializable
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

@Serializable
data class EmployeeDto(
    val id: String? = null,
    @SerialName("company_id") val companyId: String? = null,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val status: String = "active",
    val permissions: EmployeePermissions = EmployeePermissions()
)

@Serializable
data class CompanyUserDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerialName("company_id") val companyId: String? = null
)

@Serializable
data class ContractorDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    @SerialName("category") val trade: String? = null,
    @SerialName("payout_type") val payoutType: String = "NMR",
    @SerialName("default_rate") val defaultRate: Double = 0.0,
    @SerialName("companyId") val companyId: String? = null
)

@Serializable
data class PayoutRunDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    @SerialName("contractor_name") val contractorName: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("payout_class") val payoutClass: String,
    val status: String = "Pending",
    @SerialName("run_date") val runDate: String
)

@Serializable
data class TaskItemDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "pending",
    @SerialName("due_date") val dueDate: String? = null,
    val priority: String? = "medium",
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("created_by") val createdBy: String? = null
) {
    val isCompleted: Boolean
        get() = status.equals("completed", ignoreCase = true)
}

@Serializable
data class DocumentDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    @SerialName("uploader_id") val uploaderId: String? = null,
    val name: String,
    val url: String = "",
    val size: Long = 0L,
    val type: String = "note", // note, pdf, image, dwg, blueprint, etc.
    val category: String = "Notes", // Notes, Blueprints, Designs, Details, Specifications
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PurchaseOrderItemDto(
    @SerialName("material_name") val materialName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    val unit: String = "units",
    @SerialName("total_price") val totalPrice: Double = quantity * unitPrice
)

@Serializable
data class PurchaseOrderDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    @SerialName("supplier_name") val supplierName: String,
    @SerialName("supplier_contact") val supplierContact: String? = null,
    val status: String = "Draft", // Draft, Approved, Fulfilled, Cancelled
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("delivery_date") val deliveryDate: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val items: List<PurchaseOrderItemDto> = emptyList()
)

@Serializable
data class PouchTransactionDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    val amount: Double,
    val category: String, // Transport, Tea/Snacks, Urgent Hardware, Fuel, Misc
    val description: String? = null,
    @SerialName("transaction_type") val transactionType: String = "Expense", // Expense vs FundRequest
    val status: String = "Approved",
    val date: String
)

@Serializable
data class BuildingPlanDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
    val title: String,
    @SerialName("floor_level") val floorLevel: String = "Ground Floor",
    @SerialName("plan_image_url") val planImageUrl: String? = null
)
