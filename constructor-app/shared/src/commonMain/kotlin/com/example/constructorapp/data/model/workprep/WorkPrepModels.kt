package com.example.constructorapp.data.model.workprep

enum class CallStatus(val key: String, val label: String) {
    PENDING("pending", "Pending"),
    CALLED("called", "Contacted"),
    CONFIRMED("confirmed", "Confirmed"),
    NO_ANSWER("no-answer", "No Answer");

    companion object {
        fun fromKey(key: String): CallStatus = entries.find { it.key.equals(key, ignoreCase = true) } ?: PENDING
    }
}

data class TaskMaterialRequirement(
    val materialId: String,
    val materialName: String,
    val quantity: Double,
    val unit: String
)

data class PrepTaskItem(
    val id: String,
    val projectId: String,
    val projectName: String,
    val title: String,
    val priority: String = "Medium",
    val dueDate: String,
    val isSelected: Boolean = false,
    val assignedContractorId: String? = null,
    val assignedContractorName: String? = null,
    val materialRequirements: List<TaskMaterialRequirement> = emptyList()
)

data class SuggestedTaskItem(
    val projectId: String,
    val projectName: String,
    val taskTitle: String,
    val processTitle: String,
    val checklistsCount: Int = 0
)

data class ContractorCallItem(
    val callId: String,
    val contractorId: String,
    val name: String,
    val category: String,
    val phone: String,
    val assignedTaskTitles: List<String> = emptyList(),
    val callStatus: CallStatus = CallStatus.PENDING,
    val confirmedWorkerCount: Int = 0,
    val lastCallTimestamp: Long? = null
)

data class MaterialStockAuditItem(
    val materialId: String,
    val materialName: String,
    val category: String,
    val currentStock: Double,
    val minStockLevel: Double,
    val requiredQty: Double,
    val unit: String,
    val isDeficit: Boolean,
    val deficitQty: Double,
    val isLowBuffer: Boolean
)

data class WorkPrepManifest(
    val date: String,
    val formattedDate: String,
    val readinessScore: Int,
    val totalTasksScheduled: Int,
    val confirmedContactsCount: Int,
    val totalContactsCount: Int,
    val criticalMaterialCount: Int,
    val isDispatched: Boolean = false,
    val dispatchedAt: String? = null,
    val summaryTextForShare: String = ""
)
