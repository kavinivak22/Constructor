package com.example.constructorapp.data.repository

import android.content.Context
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.*
import com.example.constructorapp.data.model.worklog.CreateWorklogRequest
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.LaborEntryDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.remote.WorkerCountDto
import com.example.constructorapp.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WorklogRepository(
    private val database: AppDatabase
) {
    private val worklogDao = database.worklogDao
    private val inventoryDao = database.inventoryDao

    fun getWorklogs(projectId: String = "all", dateFilter: String = ""): Flow<List<WorklogWithDetails>> {
        return worklogDao.getFilteredWorklogs(projectId, dateFilter)
    }

    suspend fun syncWorklogsFromRemote(projectId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) return@withContext Result.failure(Exception("Not authenticated"))
        try {
            // Retrieve only the projects belonging to the logged-in user's company
            val allowedProjects = database.projectDao.getAllProjectsList()
            val allowedProjectIds = allowedProjects.map { it.id }

            if (allowedProjectIds.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            // Immediately purge any cached worklogs from other companies
            worklogDao.purgeForeignWorklogs(allowedProjectIds)

            val filter = if (!projectId.isNullOrBlank() && projectId != "all") {
                if (projectId !in allowedProjectIds) {
                    return@withContext Result.failure(Exception("Unauthorized project access"))
                }
                "eq.$projectId"
            } else {
                "in.(${allowedProjectIds.joinToString(",")})"
            }

            val response = SupabaseClient.api.getDailyWorklogs(authHeader, filter)
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                for (dto in dtoList) {
                    val logId = dto.id ?: continue
                    if (dto.projectId !in allowedProjectIds) continue // Strict tenant boundary check
                    val worklogEntity = DailyWorklogEntity(
                        id = logId,
                        projectId = dto.projectId,
                        title = dto.title ?: "Daily Site Log",
                        date = dto.date,
                        createdBy = dto.createdBy ?: "",
                        createdAt = dto.date,
                        updatedAt = dto.date,
                        syncStatus = 0
                    )
                    worklogDao.insertWorklog(worklogEntity)

                    // 1. Fetch labor entries & worker counts
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
                                    syncStatus = 0
                                )
                            }
                            worklogDao.insertLaborEntries(laborEntities)

                            // Fetch worker counts for each labor entry
                            for (labor in laborEntities) {
                                try {
                                    val countRes = SupabaseClient.api.getWorkerCounts(authHeader, "eq.${labor.id}")
                                    if (countRes.isSuccessful && !countRes.body().isNullOrEmpty()) {
                                        val countEntities = countRes.body()!!.map { c ->
                                            WorklogWorkerCountEntity(
                                                id = c.id ?: UUID.randomUUID().toString(),
                                                laborEntryId = labor.id,
                                                workerType = c.workerType,
                                                count = c.count
                                            )
                                        }
                                        worklogDao.insertWorkerCounts(countEntities)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}

                    // 2. Fetch materials consumed for this worklog
                    try {
                        val matRes = SupabaseClient.api.getWorklogMaterials(authHeader, "eq.$logId")
                        if (matRes.isSuccessful && !matRes.body().isNullOrEmpty()) {
                            val matEntities = matRes.body()!!.map { m ->
                                WorklogMaterialEntity(
                                    id = m.id ?: UUID.randomUUID().toString(),
                                    worklogId = logId,
                                    projectMaterialId = m.projectMaterialId,
                                    materialName = m.materialName,
                                    quantityConsumed = m.quantityConsumed,
                                    unit = m.unit,
                                    syncStatus = 0
                                )
                            }
                            worklogDao.insertWorklogMaterials(matEntities)
                        }
                    } catch (_: Exception) {}

                    // 3. Fetch photos for this worklog
                    try {
                        val photoRes = SupabaseClient.api.getWorklogPhotos(authHeader, "eq.$logId")
                        if (photoRes.isSuccessful && !photoRes.body().isNullOrEmpty()) {
                            val photoEntities = photoRes.body()!!.map { p ->
                                WorklogPhotoEntity(
                                    id = p.id ?: UUID.randomUUID().toString(),
                                    worklogId = logId,
                                    remotePhotoUrl = p.photoUrl,
                                    caption = p.caption,
                                    isUploaded = true,
                                    createdAt = p.createdAt ?: ""
                                )
                            }
                            worklogDao.insertWorklogPhotos(photoEntities)
                        }
                    } catch (_: Exception) {}
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch worklogs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWorklogById(worklogId: String): Flow<WorklogWithDetails?> {
        return worklogDao.getWorklogDetailsById(worklogId)
    }

    suspend fun createWorklog(request: CreateWorklogRequest, context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val currentUserId = SupabaseClient.userId ?: "offline-user"

            // 1. Process & compress photos locally
            val compressedPhotoEntities = mutableListOf<WorklogPhotoEntity>()
            for (photoInput in request.photos) {
                var localPath = photoInput.localFilePath
                if (photoInput.localUri != null) {
                    try {
                        val compressedFile = ImageCompressor.compressImage(context, photoInput.localUri)
                        localPath = compressedFile.absolutePath
                    } catch (e: Exception) {
                        // Fallback to URI path string if compression fails
                        localPath = photoInput.localUri.toString()
                    }
                }
                compressedPhotoEntities.add(
                    WorklogPhotoEntity(
                        id = photoInput.id,
                        worklogId = request.id,
                        localFilePath = localPath,
                        remotePhotoUrl = photoInput.remoteUrl,
                        caption = photoInput.caption,
                        isUploaded = photoInput.remoteUrl.isNotBlank(),
                        createdAt = nowStr
                    )
                )
            }

            // 2. Prepare Labor & Worker Count Entities
            val laborEntities = mutableListOf<WorklogLaborEntryEntity>()
            val countEntities = mutableListOf<WorklogWorkerCountEntity>()
            for (laborInput in request.labor) {
                laborEntities.add(
                    WorklogLaborEntryEntity(
                        id = laborInput.id,
                        worklogId = request.id,
                        contractorName = laborInput.contractorName,
                        category = laborInput.category,
                        workDescription = laborInput.workDescription,
                        paymentStatus = laborInput.paymentStatus,
                        workType = laborInput.workType,
                        payoutClass = laborInput.payoutClass,
                        workDoneQuantity = laborInput.workDoneQuantity,
                        workDoneUnit = laborInput.workDoneUnit,
                        payoutRate = laborInput.payoutRate,
                        totalPayout = laborInput.totalPayout,
                        syncStatus = 1
                    )
                )
                for (wc in laborInput.workers) {
                    if (wc.count > 0) {
                        countEntities.add(
                            WorklogWorkerCountEntity(
                                id = UUID.randomUUID().toString(),
                                laborEntryId = laborInput.id,
                                workerType = wc.workerType,
                                count = wc.count
                            )
                        )
                    }
                }
            }

            // 3. Prepare Material Entities
            val materialEntities = request.materials.map { mat ->
                WorklogMaterialEntity(
                    id = mat.id,
                    worklogId = request.id,
                    projectMaterialId = mat.projectMaterialId,
                    materialName = mat.materialName,
                    quantityConsumed = mat.quantityConsumed,
                    unit = mat.unit,
                    syncStatus = 1
                )
            }

            // 4. Atomic Room Persistence
            val worklogEntity = DailyWorklogEntity(
                id = request.id,
                projectId = request.projectId,
                title = request.title,
                date = request.date,
                createdBy = currentUserId,
                createdAt = nowStr,
                updatedAt = nowStr,
                syncStatus = 1
            )

            worklogDao.insertFullWorklog(
                worklog = worklogEntity,
                laborEntries = laborEntities,
                workerCounts = countEntities,
                materials = materialEntities,
                photos = compressedPhotoEntities
            )

            // 5. Deduct local inventory stock if matched
            for (mat in request.materials) {
                if (mat.projectMaterialId != null) {
                    try {
                        inventoryDao.deductStock(mat.projectMaterialId, mat.quantityConsumed)
                    } catch (_: Exception) {}
                }
            }

            // 6. Fast-path remote upload if online
            val authHeader = SupabaseClient.authHeader
            if (authHeader.isNotEmpty()) {
                try {
                    val resp = SupabaseClient.api.createDailyWorklog(
                        authHeader,
                        DailyWorklogDto(
                            id = request.id,
                            projectId = request.projectId,
                            title = request.title,
                            date = request.date,
                            createdBy = currentUserId
                        )
                    )
                    if (resp.isSuccessful) {
                        for (labor in laborEntities) {
                            SupabaseClient.api.createLaborEntry(
                                authHeader,
                                LaborEntryDto(
                                    id = labor.id,
                                    worklogId = request.id,
                                    contractorName = labor.contractorName,
                                    category = labor.category,
                                    workDescription = labor.workDescription,
                                    paymentStatus = labor.paymentStatus,
                                    payoutClass = labor.payoutClass,
                                    workType = labor.workType,
                                    workDoneQuantity = labor.workDoneQuantity,
                                    workDoneUnit = labor.workDoneUnit,
                                    payoutRate = labor.payoutRate,
                                    totalPayout = labor.totalPayout
                                )
                            )
                        }
                        for (mat in materialEntities) {
                            SupabaseClient.api.createWorklogMaterial(
                                authHeader,
                                com.example.constructorapp.data.remote.WorklogMaterialDto(
                                    id = mat.id,
                                    worklogId = request.id,
                                    projectMaterialId = mat.projectMaterialId,
                                    materialName = mat.materialName,
                                    quantityConsumed = mat.quantityConsumed,
                                    unit = mat.unit
                                )
                            )
                            if (!mat.projectMaterialId.isNullOrBlank() && mat.quantityConsumed > 0) {
                                try {
                                    val currentUserId = SupabaseClient.userId
                                    val logPayload = mutableMapOf<String, Any?>(
                                        "material_id" to mat.projectMaterialId,
                                        "change_amount" to -mat.quantityConsumed,
                                        "purpose" to "Consumed in Worklog: ${request.title} (${request.date})"
                                    )
                                    if (!currentUserId.isNullOrBlank()) {
                                        logPayload["user_id"] = currentUserId
                                    }
                                    SupabaseClient.api.createMaterialLog(authHeader, logPayload)
                                } catch (_: Exception) {}
                            }
                        }
                        worklogDao.markWorklogSynced(request.id)
                    }
                } catch (_: Exception) {
                    // Stays syncStatus = 1 for background sync
                }
            }

            Result.success(request.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorklog(worklogId: String, request: CreateWorklogRequest, context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

            // 1. Process Photos
            val compressedPhotoEntities = mutableListOf<WorklogPhotoEntity>()
            for (photoInput in request.photos) {
                val finalPath = if (!photoInput.localFilePath.isNullOrBlank()) {
                    photoInput.localFilePath
                } else if (photoInput.localUri != null) {
                    try {
                        val compressedFile = ImageCompressor.compressImage(context, photoInput.localUri)
                        compressedFile.absolutePath
                    } catch (_: Exception) {
                        photoInput.localUri.toString()
                    }
                } else {
                    null
                }

                compressedPhotoEntities.add(
                    WorklogPhotoEntity(
                        id = photoInput.id,
                        worklogId = worklogId,
                        localFilePath = finalPath,
                        remotePhotoUrl = photoInput.remoteUrl,
                        caption = photoInput.caption,
                        isUploaded = photoInput.remoteUrl.isNotBlank(),
                        createdAt = nowStr
                    )
                )
            }

            // 2. Prepare Labor & Worker Counts
            val laborEntities = mutableListOf<WorklogLaborEntryEntity>()
            val countEntities = mutableListOf<WorklogWorkerCountEntity>()
            for (laborInput in request.labor) {
                laborEntities.add(
                    WorklogLaborEntryEntity(
                        id = laborInput.id,
                        worklogId = worklogId,
                        contractorName = laborInput.contractorName,
                        category = laborInput.category,
                        workDescription = laborInput.workDescription,
                        paymentStatus = laborInput.paymentStatus,
                        workType = laborInput.workType,
                        payoutClass = laborInput.payoutClass,
                        workDoneQuantity = laborInput.workDoneQuantity,
                        workDoneUnit = laborInput.workDoneUnit,
                        payoutRate = laborInput.payoutRate,
                        totalPayout = laborInput.totalPayout,
                        syncStatus = 1
                    )
                )
                for (wc in laborInput.workers) {
                    if (wc.count > 0) {
                        countEntities.add(
                            WorklogWorkerCountEntity(
                                id = UUID.randomUUID().toString(),
                                laborEntryId = laborInput.id,
                                workerType = wc.workerType,
                                count = wc.count
                            )
                        )
                    }
                }
            }

            // 3. Prepare Material Entities
            val materialEntities = request.materials.map { mat ->
                WorklogMaterialEntity(
                    id = mat.id,
                    worklogId = worklogId,
                    projectMaterialId = mat.projectMaterialId,
                    materialName = mat.materialName,
                    quantityConsumed = mat.quantityConsumed,
                    unit = mat.unit,
                    syncStatus = 1
                )
            }

            // 4. Update in Room atomically
            val currentUserId = SupabaseClient.userId ?: "offline-user"
            val worklogEntity = DailyWorklogEntity(
                id = worklogId,
                projectId = request.projectId,
                title = request.title,
                date = request.date,
                createdBy = currentUserId,
                createdAt = nowStr,
                updatedAt = nowStr,
                syncStatus = 1
            )

            worklogDao.updateFullWorklog(
                worklog = worklogEntity,
                laborEntries = laborEntities,
                workerCounts = countEntities,
                materials = materialEntities,
                photos = compressedPhotoEntities
            )

            // 5. Remote Sync if online
            val authHeader = SupabaseClient.authHeader
            if (authHeader.isNotEmpty()) {
                try {
                    SupabaseClient.api.updateDailyWorklog(
                        authHeader,
                        "eq.$worklogId",
                        mapOf(
                            "title" to request.title,
                            "date" to request.date,
                            "updated_at" to nowStr
                        )
                    )

                    SupabaseClient.api.deleteLaborEntriesByWorklogId(authHeader, "eq.$worklogId")
                    SupabaseClient.api.deleteMaterialsByWorklogId(authHeader, "eq.$worklogId")
                    SupabaseClient.api.deletePhotosByWorklogId(authHeader, "eq.$worklogId")

                    for (labor in laborEntities) {
                        SupabaseClient.api.createLaborEntry(
                            authHeader,
                            LaborEntryDto(
                                id = labor.id,
                                worklogId = worklogId,
                                contractorName = labor.contractorName,
                                category = labor.category,
                                workDescription = labor.workDescription,
                                paymentStatus = labor.paymentStatus,
                                payoutClass = labor.payoutClass,
                                workType = labor.workType,
                                workDoneQuantity = labor.workDoneQuantity,
                                workDoneUnit = labor.workDoneUnit,
                                payoutRate = labor.payoutRate,
                                totalPayout = labor.totalPayout
                            )
                        )
                    }

                    for (mat in materialEntities) {
                        SupabaseClient.api.createWorklogMaterial(
                            authHeader,
                            com.example.constructorapp.data.remote.WorklogMaterialDto(
                                id = mat.id,
                                worklogId = worklogId,
                                projectMaterialId = mat.projectMaterialId,
                                materialName = mat.materialName,
                                quantityConsumed = mat.quantityConsumed,
                                unit = mat.unit
                            )
                        )
                        if (!mat.projectMaterialId.isNullOrBlank() && mat.quantityConsumed > 0) {
                            try {
                                val currentUserId = SupabaseClient.userId
                                val logPayload = mutableMapOf<String, Any?>(
                                    "material_id" to mat.projectMaterialId,
                                    "change_amount" to -mat.quantityConsumed,
                                    "purpose" to "Consumed in Worklog: ${request.title} (${request.date})"
                                )
                                if (!currentUserId.isNullOrBlank()) {
                                    logPayload["user_id"] = currentUserId
                                }
                                SupabaseClient.api.createMaterialLog(authHeader, logPayload)
                            } catch (_: Exception) {}
                        }
                    }

                    worklogDao.markWorklogSynced(worklogId)
                } catch (_: Exception) {
                    // Stays syncStatus = 1 for background sync
                }
            }

            Result.success(worklogId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorklog(worklogId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            worklogDao.deleteWorklogById(worklogId)
            val authHeader = SupabaseClient.authHeader
            if (authHeader.isNotEmpty()) {
                try {
                    SupabaseClient.api.deleteWorklog(authHeader, "eq.$worklogId")
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
