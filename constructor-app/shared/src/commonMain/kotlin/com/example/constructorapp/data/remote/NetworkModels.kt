package com.example.constructorapp.data.remote

data class ProjectDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val progress: Int = 0,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val location: String? = null,
    val budget: Double = 0.0,
    val companyId: String = "company-default",
    val siteId: String? = null,
    val thumbnailUrl: String? = null
)

data class DailyWorklogDto(
    val id: String? = null,
    val projectId: String,
    val title: String? = null,
    val date: String,
    val createdBy: String? = null
)

data class LaborEntryDto(
    val id: String? = null,
    val worklogId: String,
    val contractorName: String,
    val category: String? = null,
    val workDescription: String? = null,
    val paymentStatus: String = "Pending",
    val workType: String = "nmr",
    val payoutClass: String = "nmr",
    val workDoneQuantity: Double? = null,
    val workDoneUnit: String? = null,
    val payoutRate: Double? = null,
    val totalPayout: Double? = null
)

data class ProjectMaterialDto(
    val id: String? = null,
    val projectId: String? = null,
    val siteId: String? = null,
    val name: String = "",
    val category: String? = null,
    val unitOfMeasurement: String? = null,
    val currentStock: Double? = null,
    val minimumStockLevel: Double? = null,
    val unitCost: Double? = null,
    val supplierName: String? = null,
    val supplierContact: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val quantity: Double = 0.0,
    val unit: String = ""
) {
    val materialName: String get() = if (name.isNotBlank()) name else (category ?: "Material")
    val effectiveQuantity: Double get() = currentStock ?: quantity
    val effectiveMinQuantity: Double get() = minimumStockLevel ?: 0.0
    val effectiveUnit: String get() = unitOfMeasurement?.takeIf { it.isNotBlank() } ?: unit.takeIf { it.isNotBlank() } ?: "units"
    val effectiveCost: Double get() = unitCost ?: 0.0
    val effectiveSupplier: String get() = supplierName ?: ""
}

data class MaterialLogDto(
    val id: String? = null,
    val materialId: String = "",
    val projectMaterialId: String = materialId,
    val userId: String? = null,
    val changeAmount: Double = 0.0,
    val quantityChange: Double = changeAmount,
    val transactionType: String = if (changeAmount >= 0) "IN" else "OUT",
    val purpose: String? = null,
    val remarks: String? = purpose,
    val timestamp: String = "",
    val createdAt: String? = timestamp
)

data class TaskItemDto(
    val id: String? = null,
    val projectId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "pending",
    val dueDate: String? = null,
    val priority: String? = "medium",
    val assignedTo: String? = null,
    val createdBy: String? = null
) {
    val isCompleted: Boolean
        get() = status.equals("completed", ignoreCase = true)
}

data class DocumentDto(
    val id: String? = null,
    val projectId: String,
    val uploaderId: String? = null,
    val name: String,
    val url: String = "",
    val size: Long = 0L,
    val type: String = "note", // note, pdf, image, dwg, blueprint, etc.
    val category: String = "Notes", // Notes, Blueprints, Designs, Details, Specifications
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val pinned: Boolean = false
)

object SupabaseClient {
    const val DEFAULT_BUCKET = "blueprints"
    var userId: String? = null
    var userEmail: String? = null
}
