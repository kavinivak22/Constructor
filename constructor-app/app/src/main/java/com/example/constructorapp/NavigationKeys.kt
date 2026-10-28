package com.example.constructorapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object Main : NavKey
@Serializable data class ProjectDetail(val projectId: String, val projectName: String) : NavKey
@Serializable data class Worklog(val projectId: String, val projectName: String) : NavKey
@Serializable data class MaterialEstimation(val projectId: String? = null) : NavKey
@Serializable data class PurchaseOrders(val projectId: String? = null) : NavKey
@Serializable data class BuildingPlan(val projectId: String, val projectName: String) : NavKey
@Serializable data class ProjectPouch(val projectId: String, val projectName: String) : NavKey
@Serializable data class AddEditReminder(val reminderId: String? = null) : NavKey
