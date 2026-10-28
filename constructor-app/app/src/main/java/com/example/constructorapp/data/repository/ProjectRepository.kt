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
                clientPhone = it.clientPhone,
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
                        clientPhone = it.clientPhone,
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
                        paymentStatus = entryInput.paymentStatus,
                        payoutClass = entryInput.payoutClass,
                        workType = entryInput.workType,
                        workDoneQuantity = entryInput.workDoneQuantity,
                        workDoneUnit = entryInput.workDoneUnit,
                        payoutRate = entryInput.payoutRate,
                        totalPayout = entryInput.totalPayout
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

    suspend fun getEmployees(): Result<List<EmployeeDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getEmployees(authHeader)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load employees: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEmployee(employee: EmployeeDto): Result<EmployeeDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createEmployee(authHeader, employee)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to create employee: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(id: String): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.deleteEmployee(authHeader, "eq.$id")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete employee: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContractors(): Result<List<ContractorDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getContractors(authHeader)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load contractors: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPayoutRuns(projectId: String): Result<List<PayoutRunDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getPayoutRuns(authHeader, "eq.$projectId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load payout runs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPayoutRun(payoutRun: PayoutRunDto): Result<PayoutRunDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createPayoutRun(authHeader, payoutRun)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to create payout run: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectTasks(projectId: String): Result<List<TaskItemDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getProjectTasks(authHeader, "eq.$projectId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load tasks: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(task: TaskItemDto): Result<TaskItemDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createTask(authHeader, task)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to create task: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTask(taskId: String, isCompleted: Boolean): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.updateTaskStatus(authHeader, "eq.$taskId", mapOf("is_completed" to isCompleted))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update task: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPurchaseOrders(projectId: String?): Result<List<PurchaseOrderDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val filter = if (projectId != null) "eq.$projectId" else null
            val response = SupabaseClient.api.getPurchaseOrders(authHeader, filter)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load POs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPurchaseOrder(po: PurchaseOrderDto): Result<PurchaseOrderDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createPurchaseOrder(authHeader, po)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to create purchase order: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPouchTransactions(projectId: String): Result<List<PouchTransactionDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getPouchTransactions(authHeader, "eq.$projectId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load pouch transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProjectMaterial(material: ProjectMaterialDto): Result<ProjectMaterialDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createProjectMaterial(authHeader, material)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to save material: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchaseOrderStatus(poId: String, status: String): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.updatePurchaseOrderStatus(authHeader, "eq.$poId", mapOf("status" to status))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update PO status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPouchTransaction(transaction: PouchTransactionDto): Result<PouchTransactionDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createPouchTransaction(authHeader, transaction)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to record pouch expense: ${response.code()}"))
            }
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
    val payoutClass: String = "NMR",
    val workType: String? = null,
    val workDoneQuantity: Double? = null,
    val workDoneUnit: String? = null,
    val payoutRate: Double? = null,
    val totalPayout: Double? = null,
    val workerCounts: List<WorkerCountInput> = emptyList(),
    val materialsConsumed: List<MaterialConsumedInput> = emptyList()
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
