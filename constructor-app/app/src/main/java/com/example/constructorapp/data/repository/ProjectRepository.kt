package com.example.constructorapp.data.repository

import com.example.constructorapp.data.local.AppDao
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val appDao: AppDao) {
    val projects: Flow<List<ProjectDto>> = appDao.getProjects().map { list ->
        list.map {
            ProjectDto(
                id = it.id,
                name = it.name,
                description = it.description,
                startDate = it.startDate,
                endDate = it.endDate,
                status = it.status,
                progress = it.progress,
                clientName = it.clientName,
                location = it.location,
                budget = it.budget,
                companyId = it.companyId
            )
        }
    }

    suspend fun refreshProjects(): Result<List<ProjectDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))

        return try {
            val response = SupabaseClient.api.getProjects(authHeader)
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                appDao.clearProjects()
                appDao.insertProjects(list.map {
                    ProjectEntity(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        startDate = it.startDate,
                        endDate = it.endDate,
                        status = it.status,
                        progress = it.progress,
                        clientName = it.clientName,
                        location = it.location,
                        budget = it.budget,
                        companyId = it.companyId
                    )
                })
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch projects: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyWorklogs(projectId: String): Result<List<DailyWorklogDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))

        return try {
            val response = SupabaseClient.api.getDailyWorklogs(authHeader, "eq.$projectId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load worklogs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectMaterials(projectId: String): Result<List<ProjectMaterialDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))

        return try {
            val response = SupabaseClient.api.getProjectMaterials(authHeader, "eq.$projectId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load materials: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createWorklog(
        projectId: String,
        date: String,
        laborEntries: List<LaborEntryInput>
    ): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))

        return try {
            // 1. Create Daily Worklog
            val worklogResponse = SupabaseClient.api.createDailyWorklog(
                authHeader,
                DailyWorklogDto(projectId = projectId, date = date)
            )

            if (!worklogResponse.isSuccessful || worklogResponse.body().isNullOrEmpty()) {
                return Result.failure(Exception("Failed to create daily worklog row"))
            }

            val createdWorklog = worklogResponse.body()!![0]
            val worklogId = createdWorklog.id ?: return Result.failure(Exception("Worklog ID missing"))

            // 2. Insert Labor Entries, Worker Counts, and Materials
            for (entryInput in laborEntries) {
                val laborResponse = SupabaseClient.api.createLaborEntry(
                    authHeader,
                    LaborEntryDto(
                        worklogId = worklogId,
                        contractorName = entryInput.contractorName,
                        category = entryInput.category,
                        workDescription = entryInput.workDescription,
                        paymentStatus = entryInput.paymentStatus
                    )
                )

                if (laborResponse.isSuccessful && !laborResponse.body().isNullOrEmpty()) {
                    val createdLabor = laborResponse.body()!![0]
                    val laborEntryId = createdLabor.id ?: continue

                    // Insert Worker Counts
                    for (countInput in entryInput.workerCounts) {
                        SupabaseClient.api.createWorkerCount(
                            authHeader,
                            WorkerCountDto(
                                laborEntryId = laborEntryId,
                                workerType = countInput.workerType,
                                count = countInput.count
                            )
                        )
                    }
                }
            }

            // Insert Materials
            for (entryInput in laborEntries) {
                for (materialInput in entryInput.materialsConsumed) {
                    SupabaseClient.api.createWorklogMaterial(
                        authHeader,
                        WorklogMaterialDto(
                            worklogId = worklogId,
                            projectMaterialId = materialInput.projectMaterialId,
                            materialName = materialInput.materialName,
                            quantityConsumed = materialInput.quantityConsumed,
                            unit = materialInput.unit
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Input data structures helper
data class LaborEntryInput(
    val contractorName: String,
    val category: String,
    val workDescription: String,
    val paymentStatus: String,
    val workerCounts: List<WorkerCountInput>,
    val materialsConsumed: List<MaterialConsumedInput>
)

data class WorkerCountInput(
    val workerType: String,
    val count: Int
)

data class MaterialConsumedInput(
    val projectMaterialId: String?,
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String
)
