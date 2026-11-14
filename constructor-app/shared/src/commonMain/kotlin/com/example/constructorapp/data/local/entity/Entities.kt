package com.example.constructorapp.data.local.entity

data class CompanyMaterialPriceEntity(
    val id: String = "cmp-${ClockUtils.nowMillis()}",
    val companyId: String = "company-default",
    val materialName: String,
    val materialNameTa: String? = null,
    val category: String = "material", // material | steel | cement | aggregate | sand | blocks
    val unit: String = "bag",
    val standardRate: Double = 0.0,
    val supplierName: String? = null,
    val lastUpdated: String = "",
    val syncStatus: Int = 0
) {
    val masterPrice: Double get() = standardRate
}

data class MaterialTakeoffReconciliationEntity(
    val id: String = "recon-${ClockUtils.nowMillis()}",
    val projectId: String,
    val materialName: String,
    val materialNameTa: String? = null,
    val unit: String = "bag",
    val committedQty: Double = 0.0,
    val committedRate: Double = 0.0,
    val actualQty: Double = 0.0,
    val actualRate: Double = 0.0,
    val severity: String = "green", // green | amber | red
    val notes: String? = null,
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = ClockUtils.nowMillis()
)

data class InventoryAssetEntity(
    val id: String = "asset-${ClockUtils.nowMillis()}",
    val siteId: String? = null,
    val name: String,
    val category: String,
    val assetType: String = "material", // material | tool | machinery
    val unit: String,
    val currentStock: Double,
    val minStockLevel: Double,
    val unitCost: Double,
    val supplierName: String? = null,
    val supplierContact: String? = null,
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = ClockUtils.nowMillis()
)

data class ClientMilestoneEntity(
    val id: String = "milestone-${ClockUtils.nowMillis()}",
    val projectId: String,
    val stageNumber: Int = 1,
    val stageName: String,
    val stageNameTa: String? = null,
    val pctShare: Double = 0.0,
    val expectedAmount: Double = 0.0,
    val collectedAmount: Double = 0.0,
    val dueDate: String? = null,
    val status: String = "upcoming", // collected | pending | upcoming
    val paymentMode: String? = null,
    val paymentDate: String? = null,
    val notes: String? = null,
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = ClockUtils.nowMillis()
)

data class DailyWorklogEntity(
    val id: String = "worklog-${ClockUtils.nowMillis()}",
    val projectId: String,
    val title: String,
    val date: String,             // YYYY-MM-DD
    val createdBy: String = "Site Engineer",
    val createdAt: String = "",
    val updatedAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = ClockUtils.nowMillis()
)

data class WorklogLaborEntryEntity(
    val id: String = "labor-${ClockUtils.nowMillis()}",
    val worklogId: String,
    val contractorName: String,
    val category: String? = null,
    val workDescription: String? = null,
    val paymentStatus: String = "Pending", // Paid | On Payday | Pending
    val workType: String = "nmr",          // nmr | rate
    val payoutClass: String = "nmr",
    val workDoneQuantity: Double? = null,
    val workDoneUnit: String? = null,
    val payoutRate: Double? = null,
    val totalPayout: Double? = null,
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = ClockUtils.nowMillis()
)

data class WorklogWorkerCountEntity(
    val id: String = "count-${ClockUtils.nowMillis()}",
    val laborEntryId: String,
    val workerType: String, // Mason, Helper, Painter, etc.
    val count: Int
)

data class WorklogMaterialEntity(
    val id: String = "mat-${ClockUtils.nowMillis()}",
    val worklogId: String,
    val projectMaterialId: String? = null,
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String? = null,
    val syncStatus: Int = 0
)

data class WorklogPhotoEntity(
    val id: String = "photo-${ClockUtils.nowMillis()}",
    val worklogId: String,
    val localFilePath: String? = null,
    val remotePhotoUrl: String = "",
    val caption: String? = null,
    val isUploaded: Boolean = false,
    val createdAt: String = ""
)

data class LaborEntryWithCounts(
    val laborEntry: WorklogLaborEntryEntity,
    val workerCounts: List<WorklogWorkerCountEntity> = emptyList()
)

data class WorklogWithDetails(
    val worklog: DailyWorklogEntity,
    val laborEntries: List<LaborEntryWithCounts> = emptyList(),
    val materials: List<WorklogMaterialEntity> = emptyList(),
    val photos: List<WorklogPhotoEntity> = emptyList()
)

object ClockUtils {
    private var counter = 1000L
    fun nowMillis(): Long = 1731478000000L + (++counter)
}
