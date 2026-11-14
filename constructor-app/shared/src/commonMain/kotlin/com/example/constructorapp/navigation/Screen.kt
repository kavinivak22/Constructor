package com.example.constructorapp.navigation

sealed interface Screen {
    data object Dashboard : Screen
    data object Projects : Screen
    data class ProjectDetail(val projectId: String, val projectName: String) : Screen
    data class ProjectMaterials(val projectId: String, val projectName: String = "") : Screen
    data class ProjectPouch(val projectId: String, val projectName: String = "") : Screen
    data class Worklog(val projectId: String = "", val projectName: String = "All Projects") : Screen
    data class WorkPrep(val projectId: String? = null) : Screen
    data class MaterialEstimation(val projectId: String? = null) : Screen
    data class MaterialReconciliation(val projectId: String? = null) : Screen
    data class PurchaseOrders(val projectId: String? = null) : Screen
    data class Inventory(val siteId: String? = null) : Screen
    data object CompanyMaterials : Screen
    data class BuildingPlan(val projectId: String = "", val projectName: String = "Employees") : Screen
    data class ClientMilestones(val projectId: String = "") : Screen
    data class Expenses(val projectId: String? = null) : Screen
    data class Payday(val payoutId: String? = null) : Screen
    data class Contractors(val contractorId: String? = null) : Screen
    data object Financials : Screen
    data class TeamHub(val projectId: String? = null) : Screen
    data object Notifications : Screen
    data object Profile : Screen
    data class AddEditReminder(val reminderId: String? = null) : Screen
    data object Login : Screen
    data class RegisterCompany(val initialDisplayName: String = "") : Screen
    data object Employees : Screen
}
