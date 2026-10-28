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
data class ProjectDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    val status: String? = null,
    val progress: Int = 0,
    @SerialName("clientName") val clientName: String? = null,
    val location: String? = null,
    val budget: Double = 0.0,
    @SerialName("companyId") val companyId: String
)

@Serializable
data class DailyWorklogDto(
    val id: String? = null,
    @SerialName("project_id") val projectId: String,
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
data class ProjectMaterialDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val name: String,
    val quantity: Double,
    val unit: String
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
data class ContractorDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val trade: String? = null,
    @SerialName("payout_type") val payoutType: String = "NMR",
    @SerialName("default_rate") val defaultRate: Double = 0.0,
    @SerialName("company_id") val companyId: String? = null
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
