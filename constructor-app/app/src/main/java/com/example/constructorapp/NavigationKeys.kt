package com.example.constructorapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data class RegisterCompany(val userId: String = "", val email: String = "", val displayName: String = "") : NavKey
@Serializable data object Main : NavKey
@Serializable data class ProjectDetail(val projectId: String, val projectName: String) : NavKey
@Serializable data class Worklog(val projectId: String = "", val projectName: String = "All Projects") : NavKey
@Serializable data class WorkPrep(val projectId: String? = null) : NavKey
@Serializable data class MaterialEstimation(val projectId: String? = null) : NavKey
@Serializable data class PurchaseOrders(val projectId: String? = null) : NavKey
@Serializable data class BuildingPlan(val projectId: String, val projectName: String) : NavKey
@Serializable data class ProjectMaterials(val projectId: String, val projectName: String = "") : NavKey
@Serializable data class ProjectPouch(val projectId: String, val projectName: String) : NavKey
@Serializable data class AddEditReminder(val reminderId: String? = null) : NavKey

// Milestone 4 Navigation Keys
@Serializable data class Inventory(val siteId: String? = null) : NavKey
@Serializable data object CompanyMaterials : NavKey
@Serializable data class MaterialReconciliation(val projectId: String? = null) : NavKey
@Serializable data class Payday(val payoutId: String? = null) : NavKey
@Serializable data class Contractors(val contractorId: String? = null) : NavKey
@Serializable data class Expenses(val projectId: String? = null) : NavKey
@Serializable data class ClientMilestones(val projectId: String = "") : NavKey

// Milestone 5 Navigation Keys
@Serializable data class TeamHub(val projectId: String? = null) : NavKey
@Serializable data object Notifications : NavKey
@Serializable data object Profile : NavKey
@Serializable data object PersonalPouchNav : NavKey
