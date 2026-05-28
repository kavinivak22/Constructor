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
    @SerialName("payment_status") val paymentStatus: String = "Pending"
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
