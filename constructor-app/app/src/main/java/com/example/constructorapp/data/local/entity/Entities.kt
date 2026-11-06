package com.example.constructorapp.data.local.entity

import androidx.room.*
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.ReminderEntity
import java.util.UUID

// Typealiases for seamless reference across packages
typealias ProjectEntity = com.example.constructorapp.data.local.ProjectEntity
typealias ReminderEntity = com.example.constructorapp.data.local.ReminderEntity

@Entity(
    tableName = "tasks",
    indices = [Index("projectId"), Index("assignedTo"), Index("syncStatus")]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null, // null = Personal Task / Dashboard Checklist
    val title: String,
    val description: String? = null,
    val assignedTo: String? = null,
    val status: String = "pending", // pending | completed
    val priority: String = "Medium", // High | Medium | Low
    val dueDate: String? = null,     // YYYY-MM-DD
    val isUpcoming: Boolean = false, // Tomorrow Prep upcoming task
    val duration: String? = null,
    val createdBy: String? = null,
    val completedAt: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_worklogs",
    indices = [Index("projectId"), Index("date"), Index("syncStatus")]
)
data class DailyWorklogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val date: String,             // YYYY-MM-DD
    val createdBy: String,
    val createdAt: String = "",
    val updatedAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "worklog_labor_entries",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorklogEntity::class,
            parentColumns = ["id"],
            childColumns = ["worklogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("worklogId"), Index("syncStatus")]
)
data class WorklogLaborEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "worklog_worker_counts",
    foreignKeys = [
        ForeignKey(
            entity = WorklogLaborEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["laborEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("laborEntryId")]
)
data class WorklogWorkerCountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val laborEntryId: String,
    val workerType: String, // Mason, Helper, Painter, etc.
    val count: Int
)

@Entity(
    tableName = "worklog_materials",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorklogEntity::class,
            parentColumns = ["id"],
            childColumns = ["worklogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("worklogId")]
)
data class WorklogMaterialEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val worklogId: String,
    val projectMaterialId: String? = null,
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String? = null,
    val syncStatus: Int = 0
)

@Entity(
    tableName = "worklog_photos",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorklogEntity::class,
            parentColumns = ["id"],
            childColumns = ["worklogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("worklogId")]
)
data class WorklogPhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val worklogId: String,
    val localFilePath: String? = null,
    val remotePhotoUrl: String = "",
    val caption: String? = null,
    val isUploaded: Boolean = false,
    val createdAt: String = ""
)

@Entity(
    tableName = "inventory_assets",
    indices = [Index("siteId"), Index("assetType"), Index("syncStatus")]
)
data class InventoryAssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_transfers",
    indices = [Index("materialId"), Index("syncStatus")]
)
data class StockTransferEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transferNumber: String = "",
    val materialId: String,
    val itemName: String = "",
    val category: String = "material",
    val sourceType: String = "warehouse",
    val destinationType: String = "project",
    val fromSiteId: String,
    val toSiteId: String,
    val quantity: Double,
    val unit: String = "Units",
    val transferDate: String, // YYYY-MM-DD
    val notes: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0
)

@Entity(
    tableName = "purchase_orders",
    indices = [Index("projectId"), Index("status"), Index("syncStatus")]
)
data class PurchaseOrderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val poNumber: String,
    val supplierName: String,
    val supplierContact: String? = null,
    val totalAmount: Double = 0.0,
    val status: String = "Draft", // Draft | Awaiting Approval | Approved | Ordered | Delivered
    val deliveryDate: String? = null,
    val specialInstructions: String? = null,
    val createdBy: String? = null,
    val approvedBy: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["poId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("poId")]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val poId: String,
    val materialId: String? = null,
    val materialName: String,
    val quantity: Double,
    val unitPrice: Double,
    val unit: String = "units",
    val totalPrice: Double = quantity * unitPrice
)

@Entity(tableName = "weekly_payouts")
data class WeeklyPayoutRunEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val weekStartDate: String, // YYYY-MM-DD
    val weekEndDate: String,   // YYYY-MM-DD
    val totalAmount: Double = 0.0,
    val status: String = "draft", // draft | approved | paid
    val createdBy: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0
)

@Entity(
    tableName = "payout_items",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPayoutRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["payoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("payoutId"), Index("recipientId")]
)
data class PayoutItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val payoutId: String,
    val recipientType: String, // employee_salary | contractor_wage | vendor_payment | other
    val recipientId: String,
    val recipientName: String,
    val amountDue: Double,
    val amountPaid: Double,
    val status: String = "pending", // pending | paid | held
    val projectId: String? = null,
    val referenceDetails: String? = null,
    val payoutClass: String = "nmr", // rate | nmr
    val notes: String? = null,
    val syncStatus: Int = 0
)

@Entity(tableName = "contractors", indices = [Index("companyId")])
data class ContractorEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val category: String? = null,
    val contactPerson: String? = null,
    val phone: String,
    val email: String? = null,
    val accountDetails: String? = null,
    val gst: String? = null,
    val syncStatus: Int = 0
)

@Entity(tableName = "salary_profiles", indices = [Index("contractorId"), Index("userId")])
data class SalaryProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val userId: String? = null,
    val contractorId: String? = null,
    val workerName: String? = null,
    val paymentType: String = "daily_wage", // monthly | daily_wage | hourly
    val defaultRate: Double = 0.0,
    val tradeRatesJson: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null,
    val syncStatus: Int = 0
)

@Entity(
    tableName = "expenses",
    indices = [Index("projectId"), Index("userId"), Index("expenseDate"), Index("syncStatus")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,
    val userId: String = "",
    val amount: Double,
    val category: String,
    val description: String,
    val expenseDate: String, // YYYY-MM-DD
    val paymentStatus: String = "pending", // pending | paid | overdue | cancelled
    val paymentMode: String = "cash",
    val notes: String? = null,
    val receiver: String? = null,
    val localReceiptUri: String? = null,
    val receiptUrl: String? = null,
    val createdAt: String = "",
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "work_prep_contractor_calls",
    indices = [Index("date"), Index("contractorId")]
)
data class WorkPrepCallEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,            // Tomorrow's Date YYYY-MM-DD
    val contractorId: String,
    val contractorName: String,
    val phone: String,
    val callStatus: String = "pending", // pending | called | confirmed | no-answer
    val confirmedWorkerCount: Int = 0,
    val notes: String? = null,
    val lastCallTimestamp: Long? = null,
    val syncStatus: Int = 0
)

@Entity(tableName = "documents", indices = [Index("projectId"), Index("category")])
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val uploaderId: String,
    val name: String,
    val remoteUrl: String,
    val localFilePath: String? = null,
    val size: Long,
    val type: String,
    val category: String = "General",
    val createdAt: String = "",
    val isDownloaded: Boolean = false
)

@Entity(tableName = "document_reads", primaryKeys = ["documentId", "userId"])
data class DocumentReadEntity(
    val documentId: String,
    val userId: String,
    val readAt: String = ""
)

@Entity(tableName = "personal_documents", indices = [Index("userId")])
data class PersonalDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val remoteUrl: String,
    val localFilePath: String? = null,
    val size: Long,
    val type: String,
    val category: String = "Identity",
    val createdAt: String = ""
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("projectId"), Index("createdAt"), Index("syncStatus")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val userId: String,
    val userName: String? = null,
    val userAvatar: String? = null,
    val message: String,
    val fileAttachmentsJson: String? = null,
    val createdAt: String = "",
    val isOutgoing: Boolean = false,
    val syncStatus: Int = 0,
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications", indices = [Index("userId"), Index("isRead")])
data class NotificationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val projectId: String? = null,
    val title: String,
    val description: String,
    val isRead: Boolean = false,
    val link: String? = null,
    val category: String = "team",
    val icon: String = "MessageSquare",
    val timestamp: String = ""
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val phone: String? = null,
    val photoUrl: String? = null,
    val role: String = "member",
    val companyId: String? = null,
    val projectIdsJson: String = "[]",
    val permissionsJson: String? = null
)

// Relations
data class WorklogWithDetails(
    @Embedded val worklog: DailyWorklogEntity,
    @Relation(
        entity = WorklogLaborEntryEntity::class,
        parentColumn = "id",
        entityColumn = "worklogId"
    )
    val laborEntries: List<LaborEntryWithCounts>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worklogId"
    )
    val materials: List<WorklogMaterialEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worklogId"
    )
    val photos: List<WorklogPhotoEntity>
)

data class LaborEntryWithCounts(
    @Embedded val laborEntry: WorklogLaborEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "laborEntryId"
    )
    val workerCounts: List<WorklogWorkerCountEntity>
)

data class PurchaseOrderWithItems(
    @Embedded val order: PurchaseOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "poId"
    )
    val items: List<PurchaseOrderItemEntity>
)

data class PayoutRunWithItems(
    @Embedded val run: WeeklyPayoutRunEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "payoutId"
    )
    val items: List<PayoutItemEntity>
)

@Entity(
    tableName = "company_material_prices",
    indices = [Index("companyId"), Index("category")]
)
data class CompanyMaterialPriceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
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

@Entity(
    tableName = "material_takeoff_reconciliations",
    indices = [Index("projectId"), Index("severity"), Index("syncStatus")]
)
data class MaterialTakeoffReconciliationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contractor_ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = ContractorEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contractorId"), Index("projectId"), Index("payoutClass")]
)
data class ContractorLedgerEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val contractorId: String,
    val projectId: String? = null,
    val transactionDate: String = "", // YYYY-MM-DD
    val entryType: String = "work",  // work | payment | advance | deduction
    val description: String = "",
    val amount: Double = 0.0,
    val status: String = "paid",     // pending | paid | held
    val payoutClass: String = "nmr", // rate | nmr
    val referenceDetails: String? = null,
    val syncStatus: Int = 0,
    val createdAt: String = ""
) {
    val ledgerType: String get() = payoutClass
    val date: String get() = transactionDate
}

@Entity(
    tableName = "client_milestones",
    indices = [Index("projectId"), Index("status"), Index("syncStatus")]
)
data class ClientMilestoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val lastUpdatedLocally: Long = System.currentTimeMillis()
)

// Scope Contract Typealiases
typealias WarehouseAssetEntity = InventoryAssetEntity
typealias PaydayRunEntity = WeeklyPayoutRunEntity
typealias PaydayItemEntity = PayoutItemEntity
typealias ContractorAccountEntity = ContractorEntity

