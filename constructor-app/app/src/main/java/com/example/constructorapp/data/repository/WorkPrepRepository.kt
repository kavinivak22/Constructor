package com.example.constructorapp.data.repository

import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.ContractorEntity
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.local.entity.WorkPrepCallEntity
import com.example.constructorapp.data.model.workprep.CallStatus
import com.example.constructorapp.data.model.workprep.WorkPrepManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class WorkPrepRepository(
    private val database: AppDatabase
) {
    private val taskDao = database.taskDao
    private val workPrepDao = database.workPrepDao
    private val inventoryDao = database.inventoryDao
    private val financialsDao = database.financialsDao

    fun getTasksForTomorrow(tomorrowDateStr: String, projectId: String = "all"): Flow<List<TaskEntity>> {
        return if (projectId == "all" || projectId.isBlank()) {
            taskDao.getAllTasksForWorkPrep(tomorrowDateStr)
        } else {
            taskDao.getTasksForWorkPrep(tomorrowDateStr, projectId)
        }
    }

    fun getContractors(): Flow<List<ContractorEntity>> = financialsDao.getContractors()

    fun getMaterials(): Flow<List<InventoryAssetEntity>> = inventoryDao.getAllAssets()

    fun getCallsForDate(date: String): Flow<List<WorkPrepCallEntity>> = workPrepDao.getCallsForDate(date)

    suspend fun initializeContractorCalls(tomorrowDateStr: String, contractors: List<ContractorEntity>) = withContext(Dispatchers.IO) {
        val existingCalls = workPrepDao.getCallsForDate(tomorrowDateStr).first()
        val existingContractorIds = existingCalls.map { it.contractorId }.toSet()

        val newCalls = contractors.filter { it.id !in existingContractorIds }.map { c ->
            WorkPrepCallEntity(
                id = UUID.randomUUID().toString(),
                date = tomorrowDateStr,
                contractorId = c.id,
                contractorName = c.name,
                phone = c.phone,
                callStatus = "pending",
                confirmedWorkerCount = 0
            )
        }

        if (newCalls.isNotEmpty()) {
            workPrepDao.upsertCalls(newCalls)
        }
    }

    suspend fun updateCallStatus(callId: String, status: CallStatus, confirmedWorkers: Int = 0) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        workPrepDao.updateCallStatus(callId, status.key, timestamp)
        if (confirmedWorkers > 0) {
            workPrepDao.updateConfirmedWorkers(callId, confirmedWorkers)
        }
    }

    suspend fun quickCreateTask(
        projectId: String,
        title: String,
        priority: String,
        dueDate: String,
        assignedContractorId: String? = null
    ): Result<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                title = title,
                priority = priority,
                dueDate = dueDate,
                assignedTo = assignedContractorId,
                status = "pending",
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                syncStatus = 1
            )
            taskDao.upsertTask(task)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dispatchWorkPrepPlan(
        manifest: WorkPrepManifest,
        taskContractors: Map<String, String>,
        selectedTaskIds: Set<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allTasks = taskDao.getAllTasks().first()
            for (taskId in selectedTaskIds) {
                val contractorId = taskContractors[taskId]
                val currentTask = allTasks.find { it.id == taskId }
                if (currentTask != null) {
                    taskDao.upsertTask(
                        currentTask.copy(
                            dueDate = manifest.date,
                            isUpcoming = false,
                            assignedTo = contractorId ?: currentTask.assignedTo,
                            syncStatus = 2
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun calculateReadinessScore(
            selectedTaskCount: Int,
            assignedContractorTaskCount: Int,
            totalContactsCount: Int,
            confirmedContactsCount: Int,
            criticalDeficitMaterialCount: Int
        ): Int {
            val taskWeight = if (selectedTaskCount > 0) 30.0 else 0.0
            val contractorAssignedWeight = if (selectedTaskCount > 0) {
                (assignedContractorTaskCount.toDouble() / selectedTaskCount.toDouble()) * 20.0
            } else 20.0

            val contactConfirmationWeight = if (totalContactsCount > 0) {
                (confirmedContactsCount.toDouble() / totalContactsCount.toDouble()) * 30.0
            } else 30.0

            val materialDeficitDeduction = criticalDeficitMaterialCount * 15.0
            val materialScore = (20.0 - materialDeficitDeduction).coerceAtLeast(0.0)

            val totalScore = (taskWeight + contractorAssignedWeight + contactConfirmationWeight + materialScore).roundToInt()
            return totalScore.coerceIn(0, 100)
        }
    }
}
