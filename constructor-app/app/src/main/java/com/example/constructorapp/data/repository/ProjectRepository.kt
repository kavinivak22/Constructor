package com.example.constructorapp.data.repository

import com.example.constructorapp.data.local.AppDao
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.entity.DailyWorklogEntity
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.local.entity.WorklogLaborEntryEntity
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.remote.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class TomorrowPrepSummary(
    val dateLabel: String,
    val scheduledTasksCount: Int,
    val hasPlans: Boolean
)

class ProjectRepository(
    private val appDao: AppDao,
    private val database: AppDatabase? = null
) {
    // Secondary constructor accepting AppDatabase
    constructor(database: AppDatabase) : this(database.appDao, database)

    // Fallback in-memory personal tasks for testing environments without full Room DB
    private val _fallbackPersonalTasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    // ─── 1. Projects Flow & Operations ──────────────────────────────────────────

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
                companyId = it.companyId,
                thumbnailUrl = it.thumbnailUrl
            )
        }
    }

    fun getProjectById(id: String): Flow<ProjectDto?> {
        return database?.projectDao?.getProjectById(id)?.map { entity ->
            entity?.let {
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
        } ?: projects.map { list -> list.find { it.id == id } }
    }

    suspend fun refreshProjects(): Result<List<ProjectDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))

        return try {
            val response = SupabaseClient.api.getProjects(authHeader)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val list = response.body()!!
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
                        companyId = it.companyId,
                        thumbnailUrl = it.thumbnailUrl
                    )
                })
                Result.success(list)
            } else {
                seedFallbackProjectsIfEmpty()
            }
        } catch (e: Exception) {
            seedFallbackProjectsIfEmpty()
        }
    }

    suspend fun seedFallbackProjectsIfEmpty(): Result<List<ProjectDto>> {
        val existing = appDao.getProjectsSync()
        // If there are legacy mock projects or projects from other companies, wipe them from cache
        val userCompanyId = "b887770d-88f3-4c6c-a519-0d023fb057a6"
        val foreignOrMock = existing.filter { it.companyId != userCompanyId || it.id.startsWith("proj-") }
        if (foreignOrMock.isNotEmpty()) {
            appDao.clearProjects()
        } else if (existing.isNotEmpty()) {
            return Result.success(existing.map {
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
            })
        }

        // Only project belonging to company Golden Kattumaanam (respecting Supabase company RLS rule)
        val defaultEntities = listOf(
            ProjectEntity(
                id = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "LVA 24A",
                description = "First 2BHK Budget House",
                startDate = "2026-08-24",
                endDate = "2027-02-24",
                status = "active",
                progress = 50,
                clientName = "Boopathy Vaiyapuriappan",
                clientPhone = "918903945047",
                location = "Coimbatore",
                budget = 2200000.0,
                companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6"
            )
        )
        appDao.clearProjects()
        appDao.insertProjects(defaultEntities)
        return Result.success(defaultEntities.map {
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
                companyId = it.companyId,
                thumbnailUrl = it.thumbnailUrl
            )
        })
    }

    suspend fun createProject(project: ProjectDto): Result<ProjectDto> {
        val targetCompanyId = if (project.companyId.isNotBlank()) {
            project.companyId
        } else {
            appDao.getProjectsSync().firstOrNull { it.companyId.isNotBlank() }?.companyId ?: ""
        }
        val projectWithCompany = project.copy(companyId = targetCompanyId)

        val entity = ProjectEntity(
            id = projectWithCompany.id,
            name = projectWithCompany.name,
            description = projectWithCompany.description,
            startDate = projectWithCompany.startDate,
            endDate = projectWithCompany.endDate,
            status = projectWithCompany.status ?: "active",
            progress = projectWithCompany.progress,
            clientName = projectWithCompany.clientName,
            clientPhone = projectWithCompany.clientPhone,
            location = projectWithCompany.location,
            budget = projectWithCompany.budget,
            companyId = projectWithCompany.companyId,
            thumbnailUrl = projectWithCompany.thumbnailUrl
        )
        appDao.insertProjects(listOf(entity))
        database?.projectDao?.insertProject(entity)

        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.success(projectWithCompany)

        return try {
            val response = SupabaseClient.api.createProject(authHeader, projectWithCompany)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!![0]
                Result.success(created)
            } else {
                Result.success(projectWithCompany)
            }
        } catch (e: Exception) {
            Result.success(projectWithCompany) // Offline cached
        }
    }

    suspend fun updateProject(project: ProjectDto): Result<Unit> {
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            description = project.description,
            startDate = project.startDate,
            endDate = project.endDate,
            status = project.status ?: "active",
            progress = project.progress,
            clientName = project.clientName,
            clientPhone = project.clientPhone,
            location = project.location,
            budget = project.budget,
            companyId = project.companyId,
            thumbnailUrl = project.thumbnailUrl
        )
        appDao.insertProjects(listOf(entity))
        database?.projectDao?.insertProject(entity)

        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.success(Unit)

        return try {
            SupabaseClient.api.updateProject(
                authHeader,
                "eq.${project.id}",
                mapOf(
                    "name" to project.name,
                    "description" to (project.description ?: ""),
                    "status" to (project.status ?: "active"),
                    "progress" to project.progress,
                    "budget" to project.budget
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    // ─── 2. Personal Tasks (Dashboard My Tasks) ──────────────────────────

    val personalTasks: Flow<List<TaskEntity>> = database?.taskDao?.getPersonalTasks() ?: _fallbackPersonalTasks.asStateFlow()

    suspend fun addPersonalTask(title: String, priority: String = "Medium"): Result<TaskEntity> {
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            projectId = null,
            title = title,
            status = "pending",
            priority = priority,
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
            syncStatus = 1
        )
        if (database != null) {
            database.taskDao.upsertTask(task)
        } else {
            val current = _fallbackPersonalTasks.value.toMutableList()
            current.add(0, task)
            _fallbackPersonalTasks.value = current
        }

        val authHeader = SupabaseClient.authHeader
        if (authHeader.isNotEmpty()) {
            try {
                SupabaseClient.api.createTask(
                    authHeader,
                    TaskItemDto(
                        id = task.id,
                        projectId = null,
                        title = task.title,
                        description = task.description,
                        status = "pending",
                        dueDate = task.dueDate,
                        priority = task.priority?.lowercase(),
                        assignedTo = task.assignedTo
                    )
                )
            } catch (_: Exception) {}
        }
        return Result.success(task)
    }

    suspend fun toggleTask(taskId: String, isCompleted: Boolean): Result<Unit> {
        val newStatus = if (isCompleted) "completed" else "pending"
        val completedAt = if (isCompleted) SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()) else null

        if (database != null) {
            database.taskDao.updateTaskStatus(taskId, newStatus, completedAt)
        } else {
            val current = _fallbackPersonalTasks.value.toMutableList()
            val index = current.indexOfFirst { it.id == taskId }
            if (index != -1) {
                current[index] = current[index].copy(status = newStatus, completedAt = completedAt)
                _fallbackPersonalTasks.value = current
            }
        }

        val authHeader = SupabaseClient.authHeader
        if (authHeader.isNotEmpty()) {
            try {
                SupabaseClient.api.updateTaskStatus(
                    authHeader,
                    "eq.$taskId",
                    mapOf("status" to if (isCompleted) "completed" else "pending")
                )
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    suspend fun editTaskTitle(taskId: String, newTitle: String): Result<Unit> {
        if (database != null) {
            val currentTasks = database.taskDao.getPersonalTasks().first()
            val task = currentTasks.find { it.id == taskId } ?: return Result.failure(Exception("Not found"))
            database.taskDao.upsertTask(task.copy(title = newTitle, syncStatus = 2))
        } else {
            val current = _fallbackPersonalTasks.value.toMutableList()
            val index = current.indexOfFirst { it.id == taskId }
            if (index != -1) {
                current[index] = current[index].copy(title = newTitle, syncStatus = 2)
                _fallbackPersonalTasks.value = current
            }
        }
        return Result.success(Unit)
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        if (database != null) {
            database.taskDao.deleteTaskById(taskId)
        } else {
            val current = _fallbackPersonalTasks.value.toMutableList()
            current.removeAll { it.id == taskId }
            _fallbackPersonalTasks.value = current
        }
        return Result.success(Unit)
    }

    // ─── 3. Tomorrow's Prep Summary ──────────────────────────────────────

    fun getTomorrowPrepSummary(): Flow<TomorrowPrepSummary> {
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrowCal.time)
        val dateLabel = SimpleDateFormat("MMM d", Locale.US).format(tomorrowCal.time)

        return database?.taskDao?.getTasksForWorkPrep(tomorrowDateStr, "")?.map { tasks ->
            TomorrowPrepSummary(
                dateLabel = dateLabel,
                scheduledTasksCount = tasks.size,
                hasPlans = tasks.isNotEmpty()
            )
        } ?: flowOf(
            TomorrowPrepSummary(
                dateLabel = dateLabel,
                scheduledTasksCount = 3,
                hasPlans = true
            )
        )
    }

    // ─── 4. Project Tasks & Worklogs ─────────────────────────────────────

    fun getProjectTasksFlow(projectId: String): Flow<List<TaskEntity>> {
        return database?.taskDao?.getProjectTasks(projectId) ?: flowOf(emptyList())
    }

    fun getProjectWorklogs(projectId: String): Flow<List<WorklogWithDetails>> {
        return if (projectId.isEmpty() || projectId == "all") {
            database?.worklogDao?.getAllWorklogsWithDetails() ?: flowOf(emptyList())
        } else {
            database?.worklogDao?.getWorklogsWithDetails(projectId) ?: flowOf(emptyList())
        }
    }

    fun getAllWorklogs(): Flow<List<WorklogWithDetails>> {
        return database?.worklogDao?.getAllWorklogsWithDetails() ?: flowOf(emptyList())
    }

    suspend fun syncAllWorklogs(): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getDailyWorklogs(authHeader, null)
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                val dao = database?.worklogDao ?: return Result.success(Unit)
                for (dto in dtoList) {
                    val logId = dto.id ?: continue
                    val worklogEntity = DailyWorklogEntity(
                        id = logId,
                        projectId = dto.projectId,
                        title = dto.title ?: "Daily Site Log",
                        date = dto.date,
                        createdBy = dto.createdBy ?: "",
                        createdAt = dto.date,
                        updatedAt = dto.date,
                        syncStatus = 1
                    )
                    dao.insertWorklog(worklogEntity)

                    try {
                        val laborRes = SupabaseClient.api.getLaborEntries(authHeader, "eq.$logId")
                        if (laborRes.isSuccessful && !laborRes.body().isNullOrEmpty()) {
                            val laborEntities = laborRes.body()!!.map { l ->
                                WorklogLaborEntryEntity(
                                    id = l.id ?: UUID.randomUUID().toString(),
                                    worklogId = logId,
                                    contractorName = l.contractorName,
                                    category = l.category,
                                    workDescription = l.workDescription,
                                    paymentStatus = l.paymentStatus,
                                    workType = l.workType ?: "nmr",
                                    payoutClass = l.payoutClass,
                                    workDoneQuantity = l.workDoneQuantity,
                                    workDoneUnit = l.workDoneUnit,
                                    payoutRate = l.payoutRate,
                                    totalPayout = l.totalPayout,
                                    syncStatus = 1
                                )
                            }
                            dao.insertLaborEntries(laborEntities)

                            // Fetch worker counts
                            for (labor in laborEntities) {
                                try {
                                    val countRes = SupabaseClient.api.getWorkerCounts(authHeader, "eq.${labor.id}")
                                    if (countRes.isSuccessful && !countRes.body().isNullOrEmpty()) {
                                        val countEntities = countRes.body()!!.map { c ->
                                            com.example.constructorapp.data.local.entity.WorklogWorkerCountEntity(
                                                id = c.id ?: UUID.randomUUID().toString(),
                                                laborEntryId = labor.id,
                                                workerType = c.workerType,
                                                count = c.count
                                            )
                                        }
                                        dao.insertWorkerCounts(countEntities)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}

                    // Fetch materials
                    try {
                        val matRes = SupabaseClient.api.getWorklogMaterials(authHeader, "eq.$logId")
                        if (matRes.isSuccessful && !matRes.body().isNullOrEmpty()) {
                            val matEntities = matRes.body()!!.map { m ->
                                com.example.constructorapp.data.local.entity.WorklogMaterialEntity(
                                    id = m.id ?: UUID.randomUUID().toString(),
                                    worklogId = logId,
                                    projectMaterialId = m.projectMaterialId,
                                    materialName = m.materialName,
                                    quantityConsumed = m.quantityConsumed,
                                    unit = m.unit,
                                    syncStatus = 1
                                )
                            }
                            dao.insertWorklogMaterials(matEntities)
                        }
                    } catch (_: Exception) {}

                    // Fetch photos
                    try {
                        val photoRes = SupabaseClient.api.getWorklogPhotos(authHeader, "eq.$logId")
                        if (photoRes.isSuccessful && !photoRes.body().isNullOrEmpty()) {
                            val photoEntities = photoRes.body()!!.map { p ->
                                com.example.constructorapp.data.local.entity.WorklogPhotoEntity(
                                    id = p.id ?: UUID.randomUUID().toString(),
                                    worklogId = logId,
                                    remotePhotoUrl = p.photoUrl,
                                    caption = p.caption,
                                    isUploaded = true,
                                    createdAt = p.createdAt ?: ""
                                )
                            }
                            dao.insertWorklogPhotos(photoEntities)
                        }
                    } catch (_: Exception) {}
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to load worklogs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorklogById(worklogId: String): Result<Unit> {
        return try {
            database?.worklogDao?.deleteWorklogById(worklogId)
            val authHeader = SupabaseClient.authHeader
            if (authHeader.isNotEmpty()) {
                SupabaseClient.api.deleteWorklog(authHeader, "eq.$worklogId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit) // Offline deletion succeeds locally
        }
    }

    // ─── Work Prep Data Streams & Operations ────────────────────────────

    fun getWorkPrepTasks(date: String, projectId: String? = null): Flow<List<TaskEntity>> {
        return if (projectId.isNullOrEmpty() || projectId == "all") {
            database?.taskDao?.getAllTasksForWorkPrep(date) ?: flowOf(emptyList())
        } else {
            database?.taskDao?.getTasksForWorkPrep(date, projectId) ?: flowOf(emptyList())
        }
    }

    fun getWorkPrepCalls(date: String): Flow<List<com.example.constructorapp.data.local.entity.WorkPrepCallEntity>> {
        return database?.workPrepDao?.getCallsForDate(date) ?: flowOf(emptyList())
    }

    suspend fun updateWorkPrepCallStatus(callId: String, status: String) {
        database?.workPrepDao?.updateCallStatus(callId, status, System.currentTimeMillis())
    }

    suspend fun upsertWorkPrepCall(call: com.example.constructorapp.data.local.entity.WorkPrepCallEntity) {
        database?.workPrepDao?.upsertCall(call)
    }

    fun getInventoryMaterials(): Flow<List<com.example.constructorapp.data.local.entity.InventoryAssetEntity>> {
        return database?.inventoryDao?.getAllAssets() ?: flowOf(emptyList())
    }

    // ─── 5. Existing APIs for backward compatibility ─────────────────────

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
            // Check project for siteId
            var siteId: String? = null
            try {
                val siteRes = SupabaseClient.api.getProjectSite(authHeader, "eq.$projectId")
                if (siteRes.isSuccessful && !siteRes.body().isNullOrEmpty()) {
                    siteId = siteRes.body()!![0].siteId
                }
            } catch (e: Exception) {
                // Ignore and proceed
            }

            // Fallback for LVA 24A siteId if not found from network
            if (siteId.isNullOrBlank() && projectId == "afc2ec97-7875-4ee9-945d-42923854454e") {
                siteId = "02675139-2861-4eb9-9927-7b82352abca2"
            }

            // Query live materials table (matching webapp logic: eq('site_id', siteId))
            var liveResponse = if (!siteId.isNullOrBlank()) {
                SupabaseClient.api.getMaterials(authHeader = authHeader, siteIdFilter = "eq.$siteId")
            } else {
                SupabaseClient.api.getMaterials(authHeader = authHeader, projectIdFilter = "eq.$projectId")
            }

            // If 401 or failed, retry with Supabase anon API key
            if (!liveResponse.isSuccessful) {
                val anonHeader = "Bearer ${SupabaseClient.API_KEY}"
                liveResponse = if (!siteId.isNullOrBlank()) {
                    SupabaseClient.api.getMaterials(authHeader = anonHeader, siteIdFilter = "eq.$siteId")
                } else {
                    SupabaseClient.api.getMaterials(authHeader = anonHeader, projectIdFilter = "eq.$projectId")
                }
            }

            if (!liveResponse.isSuccessful || liveResponse.body().isNullOrEmpty()) {
                // Also try by project_id in case materials are directly tagged
                val byProj = SupabaseClient.api.getMaterials(authHeader = authHeader, projectIdFilter = "eq.$projectId")
                if (byProj.isSuccessful && !byProj.body().isNullOrEmpty()) {
                    liveResponse = byProj
                }
            }

            if (liveResponse.isSuccessful && !liveResponse.body().isNullOrEmpty()) {
                Result.success(liveResponse.body()!!)
            } else {
                // Fallback to legacy project_materials table if needed
                val legacyResponse = SupabaseClient.api.getProjectMaterials(authHeader, "eq.$projectId")
                if (legacyResponse.isSuccessful && !legacyResponse.body().isNullOrEmpty()) {
                    Result.success(legacyResponse.body()!!)
                } else {
                    Result.success(liveResponse.body() ?: emptyList())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMaterial(
        siteId: String?,
        projectId: String?,
        name: String,
        category: String?,
        initialStock: Double,
        minStock: Double,
        unit: String,
        supplier: String?,
        cost: Double?
    ): Result<ProjectMaterialDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val payload = mutableMapOf<String, Any?>(
                "name" to name.trim(),
                "current_stock" to initialStock,
                "minimum_stock_level" to minStock,
                "unit_of_measurement" to unit.trim()
            )
            if (!siteId.isNullOrBlank()) payload["site_id"] = siteId
            if (!projectId.isNullOrBlank()) payload["project_id"] = projectId
            if (!category.isNullOrBlank()) payload["category"] = category.trim()
            if (!supplier.isNullOrBlank()) payload["supplier_name"] = supplier.trim()
            if (cost != null && cost >= 0) payload["unit_cost"] = cost

            val response = SupabaseClient.api.createMaterial(authHeader, payload)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to add material: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMaterialStock(
        materialId: String,
        newStock: Double,
        unitCost: Double? = null,
        supplier: String? = null
    ): Result<ProjectMaterialDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val payload = mutableMapOf<String, Any?>(
                "current_stock" to newStock,
                "quantity" to newStock
            )
            if (unitCost != null && unitCost > 0) payload["unit_cost"] = unitCost
            if (!supplier.isNullOrBlank()) payload["supplier_name"] = supplier.trim()

            val response = SupabaseClient.api.updateMaterial(authHeader, "eq.$materialId", payload)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to update stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMaterialDetails(
        materialId: String,
        name: String,
        category: String?,
        currentStock: Double,
        minStock: Double,
        unit: String,
        supplier: String?,
        cost: Double?
    ): Result<ProjectMaterialDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val payload = mutableMapOf<String, Any?>(
                "name" to name.trim(),
                "category" to (category?.trim() ?: ""),
                "current_stock" to currentStock,
                "minimum_stock_level" to minStock,
                "unit_of_measurement" to unit.trim(),
                "supplier_name" to (supplier?.trim() ?: ""),
                "unit_cost" to (cost ?: 0.0)
            )
            val response = SupabaseClient.api.updateMaterial(authHeader, "eq.$materialId", payload)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to update material: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMaterial(materialId: String): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.deleteMaterial(authHeader, "eq.$materialId")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete material: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMaterialLogs(materialId: String): Result<List<MaterialLogDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            var response = SupabaseClient.api.getMaterialLogs(authHeader, "eq.$materialId")
            if (!response.isSuccessful && response.code() == 401) {
                response = SupabaseClient.api.getMaterialLogs("Bearer ${SupabaseClient.API_KEY}", "eq.$materialId")
            }
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get material logs: ${response.code()}"))
            }
        } catch (e: Exception) {
            try {
                val anonResp = SupabaseClient.api.getMaterialLogs("Bearer ${SupabaseClient.API_KEY}", "eq.$materialId")
                if (anonResp.isSuccessful) {
                    Result.success(anonResp.body() ?: emptyList())
                } else {
                    Result.failure(e)
                }
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createMaterialLog(
        materialId: String,
        changeAmount: Double,
        purpose: String
    ): Result<MaterialLogDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val currentUserId = SupabaseClient.userId
            val payload = mutableMapOf<String, Any?>(
                "material_id" to materialId,
                "change_amount" to changeAmount,
                "purpose" to purpose
            )
            if (!currentUserId.isNullOrBlank()) {
                payload["user_id"] = currentUserId
            }
            var response = SupabaseClient.api.createMaterialLog(authHeader, payload)
            if (!response.isSuccessful && response.code() == 401) {
                response = SupabaseClient.api.createMaterialLog("Bearer ${SupabaseClient.API_KEY}", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to log material stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordMaterialExpense(
        projectId: String,
        amount: Double,
        description: String,
        date: String,
        status: String = "paid",
        receiver: String? = null,
        notes: String? = null
    ): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val currentUserId = SupabaseClient.userId
            val payload = mutableMapOf<String, Any?>(
                "project_id" to projectId,
                "amount" to amount,
                "category" to "materials",
                "description" to description,
                "expense_date" to date,
                "payment_status" to status,
                "payment_mode" to "cash"
            )
            if (!currentUserId.isNullOrBlank()) {
                payload["user_id"] = currentUserId
                payload["created_by"] = currentUserId
            }
            if (!receiver.isNullOrBlank()) payload["receiver"] = receiver
            if (!notes.isNullOrBlank()) payload["notes"] = notes

            SupabaseClient.api.createExpenseDirect(authHeader, payload)
            Result.success(Unit)
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
            val worklogResponse = SupabaseClient.api.createDailyWorklog(
                authHeader,
                DailyWorklogDto(projectId = projectId, date = date)
            )

            if (!worklogResponse.isSuccessful || worklogResponse.body().isNullOrEmpty()) {
                return Result.failure(Exception("Failed to create daily worklog row"))
            }

            val createdWorklog = worklogResponse.body()!![0]
            val worklogId = createdWorklog.id ?: return Result.failure(Exception("Worklog ID missing"))

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

    suspend fun getCompanyUsers(): Result<List<CompanyUserDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.getUsers(authHeader)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load users: ${response.code()}"))
            }
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
        val token = SupabaseClient.authToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))
        val authHeader = "Bearer $token"
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

    suspend fun createContractor(
        name: String,
        category: String?,
        contactPerson: String?,
        phone: String,
        email: String? = null
    ): Result<ContractorDto> {
        val token = SupabaseClient.authToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))
        val authHeader = "Bearer $token"
        return try {
            val body = mutableMapOf<String, Any?>("name" to name, "phone" to phone)
            if (!category.isNullOrBlank()) body["category"] = category
            if (!contactPerson.isNullOrBlank()) body["contactPerson"] = contactPerson
            if (!email.isNullOrBlank()) body["email"] = email

            val response = SupabaseClient.api.createContractor(authHeader, body)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                database?.financialsDao?.upsertContractor(
                    com.example.constructorapp.data.local.entity.ContractorEntity(
                        id = created.id,
                        companyId = created.companyId ?: "",
                        name = created.name,
                        category = created.trade ?: category,
                        contactPerson = contactPerson,
                        phone = created.phone ?: phone,
                        email = email,
                        syncStatus = 0
                    )
                )
                Result.success(created)
            } else {
                Result.failure(Exception("Failed to create contractor: ${response.code()}"))
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

    suspend fun getDocuments(projectId: String?): Result<List<DocumentDto>> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val filter = if (!projectId.isNullOrBlank() && projectId != "all") "eq.$projectId" else null
            val response = SupabaseClient.api.getProjectDocuments(authHeader, filter)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load documents: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDocument(doc: DocumentDto): Result<DocumentDto> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.createProjectDocument(authHeader, doc)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!![0])
            } else {
                Result.failure(Exception("Failed to save document: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.deleteProjectDocument(authHeader, "eq.$docId")
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to delete document: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDocument(docId: String, updates: Map<String, Any?>): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return Result.failure(Exception("Not authenticated"))
        return try {
            val response = SupabaseClient.api.updateProjectDocument(authHeader, "eq.$docId", updates)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to update document: ${response.code()}"))
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
