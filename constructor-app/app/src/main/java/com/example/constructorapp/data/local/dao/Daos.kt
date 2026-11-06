package com.example.constructorapp.data.local.dao

import androidx.room.*
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsList(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE status = :status")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM projects")
    suspend fun clearProjects()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isUpcoming = 0 ORDER BY createdAt DESC")
    fun getProjectTasks(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId IS NULL ORDER BY createdAt DESC")
    fun getPersonalTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date OR (projectId = :projectId AND isUpcoming = 1)")
    fun getTasksForWorkPrep(date: String, projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date OR isUpcoming = 1 ORDER BY createdAt DESC")
    fun getAllTasksForWorkPrep(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, syncStatus = 2 WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: String?)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM tasks WHERE syncStatus != 0")
    suspend fun getPendingSyncTasks(): List<TaskEntity>
}

@Dao
interface WorklogDao {
    @Transaction
    @Query("""
        SELECT * FROM daily_worklogs 
        WHERE (:projectId = 'all' OR :projectId = '' OR projectId = :projectId)
          AND (:dateFilter = '' OR date = :dateFilter)
        ORDER BY date DESC, createdAt DESC
    """)
    fun getFilteredWorklogs(projectId: String, dateFilter: String): Flow<List<WorklogWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_worklogs WHERE projectId = :projectId ORDER BY date DESC, createdAt DESC")
    fun getWorklogsWithDetails(projectId: String): Flow<List<WorklogWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_worklogs ORDER BY date DESC, createdAt DESC")
    fun getAllWorklogsWithDetails(): Flow<List<WorklogWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_worklogs WHERE id = :worklogId")
    fun getWorklogDetailsById(worklogId: String): Flow<WorklogWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorklog(worklog: DailyWorklogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaborEntries(entries: List<WorklogLaborEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerCounts(counts: List<WorklogWorkerCountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorklogMaterials(materials: List<WorklogMaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorklogPhotos(photos: List<WorklogPhotoEntity>)

    @Transaction
    suspend fun insertFullWorklog(
        worklog: DailyWorklogEntity,
        laborEntries: List<WorklogLaborEntryEntity>,
        workerCounts: List<WorklogWorkerCountEntity>,
        materials: List<WorklogMaterialEntity>,
        photos: List<WorklogPhotoEntity>
    ) {
        insertWorklog(worklog)
        if (laborEntries.isNotEmpty()) insertLaborEntries(laborEntries)
        if (workerCounts.isNotEmpty()) insertWorkerCounts(workerCounts)
        if (materials.isNotEmpty()) insertWorklogMaterials(materials)
        if (photos.isNotEmpty()) insertWorklogPhotos(photos)
    }

    @Query("SELECT * FROM daily_worklogs WHERE syncStatus != 0")
    suspend fun getPendingSyncWorklogs(): List<DailyWorklogEntity>

    @Query("SELECT * FROM worklog_photos WHERE isUploaded = 0")
    suspend fun getPendingPhotoUploads(): List<WorklogPhotoEntity>

    @Query("UPDATE daily_worklogs SET syncStatus = 0 WHERE id = :id")
    suspend fun markWorklogSynced(id: String)

    @Query("UPDATE worklog_photos SET isUploaded = 1, remotePhotoUrl = :remoteUrl WHERE id = :id")
    suspend fun markPhotoUploaded(id: String, remoteUrl: String)

    @Query("DELETE FROM daily_worklogs WHERE id = :worklogId")
    suspend fun deleteWorklogById(worklogId: String)

    @Query("DELETE FROM worklog_labor_entries WHERE worklogId = :worklogId")
    suspend fun deleteLaborEntriesByWorklogId(worklogId: String)

    @Query("DELETE FROM worklog_materials WHERE worklogId = :worklogId")
    suspend fun deleteMaterialsByWorklogId(worklogId: String)

    @Query("DELETE FROM worklog_photos WHERE worklogId = :worklogId")
    suspend fun deletePhotosByWorklogId(worklogId: String)

    @Transaction
    suspend fun updateFullWorklog(
        worklog: DailyWorklogEntity,
        laborEntries: List<WorklogLaborEntryEntity>,
        workerCounts: List<WorklogWorkerCountEntity>,
        materials: List<WorklogMaterialEntity>,
        photos: List<WorklogPhotoEntity>
    ) {
        insertWorklog(worklog)
        deleteLaborEntriesByWorklogId(worklog.id)
        deleteMaterialsByWorklogId(worklog.id)
        deletePhotosByWorklogId(worklog.id)
        if (laborEntries.isNotEmpty()) insertLaborEntries(laborEntries)
        if (workerCounts.isNotEmpty()) insertWorkerCounts(workerCounts)
        if (materials.isNotEmpty()) insertWorklogMaterials(materials)
        if (photos.isNotEmpty()) insertWorklogPhotos(photos)
    }

    @Query("DELETE FROM daily_worklogs WHERE projectId NOT IN (:allowedProjectIds)")
    suspend fun purgeForeignWorklogs(allowedProjectIds: List<String>)

    @Query("DELETE FROM daily_worklogs")
    suspend fun clearAllWorklogs()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_assets ORDER BY name ASC")
    fun getAllAssets(): Flow<List<InventoryAssetEntity>>

    @Query("SELECT * FROM inventory_assets WHERE assetType = :assetType ORDER BY name ASC")
    fun getAssetsByType(assetType: String): Flow<List<InventoryAssetEntity>>

    @Query("SELECT * FROM inventory_assets WHERE category = :category ORDER BY name ASC")
    fun getAssetsByCategory(category: String): Flow<List<InventoryAssetEntity>>

    @Query("SELECT * FROM inventory_assets WHERE currentStock <= minStockLevel")
    fun getLowStockAssets(): Flow<List<InventoryAssetEntity>>

    @Query("SELECT COUNT(*) FROM inventory_assets WHERE currentStock <= minStockLevel")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT * FROM inventory_assets WHERE id = :id LIMIT 1")
    fun getAssetById(id: String): Flow<InventoryAssetEntity?>

    @Query("SELECT * FROM inventory_assets WHERE id = :id LIMIT 1")
    suspend fun findAssetById(id: String): InventoryAssetEntity?

    @Query("SELECT * FROM inventory_assets WHERE name = :name AND (siteId = :siteId OR (:siteId IS NULL AND siteId IS NULL)) LIMIT 1")
    suspend fun findAssetByNameAndSite(name: String, siteId: String?): InventoryAssetEntity?

    @Query("SELECT * FROM stock_transfers ORDER BY transferDate DESC")
    fun getAllTransfers(): Flow<List<StockTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: InventoryAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssets(assets: List<InventoryAssetEntity>)

    @Query("DELETE FROM inventory_assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: StockTransferEntity)

    @Query("UPDATE inventory_assets SET currentStock = currentStock - :qty WHERE id = :id")
    suspend fun deductStock(id: String, qty: Double)

    @Query("UPDATE inventory_assets SET currentStock = currentStock + :qty WHERE id = :id")
    suspend fun addStock(id: String, qty: Double)

    @Transaction
    suspend fun executeTransferTransaction(
        transfer: StockTransferEntity,
        sourceAssetId: String? = null,
        destinationAssetId: String? = null
    ) {
        insertTransfer(transfer)
        val srcId = sourceAssetId ?: transfer.materialId
        val srcAsset = findAssetById(srcId)
        if (srcAsset != null) {
            deductStock(srcAsset.id, transfer.quantity)
        }

        if (!destinationAssetId.isNullOrBlank()) {
            addStock(destinationAssetId, transfer.quantity)
        } else if (srcAsset != null) {
            val destAsset = findAssetByNameAndSite(srcAsset.name, transfer.toSiteId)
            if (destAsset != null) {
                addStock(destAsset.id, transfer.quantity)
            } else {
                upsertAsset(
                    srcAsset.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        siteId = transfer.toSiteId,
                        currentStock = transfer.quantity,
                        lastUpdatedLocally = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

@Dao
interface PurchaseOrderDao {
    @Transaction
    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun getPurchaseOrdersWithItems(): Flow<List<PurchaseOrderWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE id = :poId LIMIT 1")
    fun getPurchaseOrderById(poId: String): Flow<PurchaseOrderWithItems?>

    @Query("SELECT * FROM purchase_orders WHERE id = :poId LIMIT 1")
    suspend fun findPurchaseOrderById(poId: String): PurchaseOrderEntity?

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE status = :status ORDER BY createdAt DESC")
    fun getPurchaseOrdersByStatus(status: String): Flow<List<PurchaseOrderWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getPurchaseOrdersByProject(projectId: String): Flow<List<PurchaseOrderWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE projectId = :projectId AND status = :status ORDER BY createdAt DESC")
    fun getPurchaseOrdersByProjectAndStatus(projectId: String, status: String): Flow<List<PurchaseOrderWithItems>>

    @Query("SELECT * FROM purchase_order_items WHERE poId = :poId")
    suspend fun getItemsForPo(poId: String): List<PurchaseOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrder(order: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrderItems(items: List<PurchaseOrderItemEntity>)

    @Query("UPDATE purchase_orders SET status = :status WHERE id = :poId")
    suspend fun updateOrderStatus(poId: String, status: String)

    @Query("UPDATE purchase_orders SET status = :status, approvedBy = :approverName WHERE id = :poId")
    suspend fun updateOrderStatusAndApprover(poId: String, status: String, approverName: String)

    @Query("UPDATE inventory_assets SET currentStock = currentStock + :qty WHERE name = :name")
    suspend fun incrementStockByName(name: String, qty: Double): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryAsset(asset: InventoryAssetEntity)

    @Transaction
    suspend fun insertFullPurchaseOrder(
        order: PurchaseOrderEntity,
        items: List<PurchaseOrderItemEntity>
    ) {
        insertPurchaseOrder(order)
        if (items.isNotEmpty()) {
            insertPurchaseOrderItems(items)
        }
    }

    @Transaction
    suspend fun approvePurchaseOrder(poId: String, approverName: String = "Admin") {
        val order = findPurchaseOrderById(poId) ?: return
        if (order.status.equals("Approved", ignoreCase = true) ||
            order.status.equals("Ordered", ignoreCase = true) ||
            order.status.equals("Delivered", ignoreCase = true)) {
            return
        }
        updateOrderStatusAndApprover(poId, "Approved", approverName)
        val items = getItemsForPo(poId)
        for (item in items) {
            val rowsUpdated = incrementStockByName(item.materialName, item.quantity)
            if (rowsUpdated == 0) {
                insertInventoryAsset(
                    InventoryAssetEntity(
                        id = item.materialId?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                        siteId = order.projectId,
                        name = item.materialName,
                        category = "material",
                        assetType = "material",
                        unit = item.unit,
                        currentStock = item.quantity,
                        minStockLevel = 0.0,
                        unitCost = item.unitPrice,
                        supplierName = order.supplierName,
                        supplierContact = order.supplierContact,
                        lastUpdatedLocally = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

@Dao
interface FinancialsDao {
    @Query("SELECT * FROM weekly_payouts ORDER BY weekStartDate DESC")
    fun getWeeklyPayouts(): Flow<List<WeeklyPayoutRunEntity>>

    @Transaction
    @Query("SELECT * FROM weekly_payouts ORDER BY weekStartDate DESC")
    fun getAllPayoutRunsWithItems(): Flow<List<PayoutRunWithItems>>

    @Transaction
    @Query("SELECT * FROM weekly_payouts WHERE id = :id LIMIT 1")
    fun getPayoutRunWithItems(id: String): Flow<PayoutRunWithItems?>

    @Query("SELECT * FROM weekly_payouts WHERE id = :id LIMIT 1")
    suspend fun findPayoutRunById(id: String): WeeklyPayoutRunEntity?

    @Query("DELETE FROM payout_items WHERE id = :id")
    suspend fun deletePayoutItemById(id: String): Int

    @Query("SELECT * FROM payout_items WHERE payoutId = :payoutId")
    fun getPayoutItems(payoutId: String): Flow<List<PayoutItemEntity>>

    @Query("SELECT * FROM payout_items WHERE payoutId = :payoutId")
    suspend fun getPayoutItemsList(payoutId: String): List<PayoutItemEntity>

    @Query("SELECT * FROM contractors ORDER BY name ASC")
    fun getContractors(): Flow<List<ContractorEntity>>

    @Query("SELECT * FROM contractors WHERE id = :id LIMIT 1")
    fun getContractorById(id: String): Flow<ContractorEntity?>

    @Query("SELECT * FROM payout_items WHERE recipientId = :contractorId")
    fun getContractorLedgerItems(contractorId: String): Flow<List<PayoutItemEntity>>

    @Query("SELECT * FROM contractor_ledger_entries WHERE contractorId = :contractorId ORDER BY transactionDate DESC")
    fun getAllContractorLedgerEntries(contractorId: String): Flow<List<ContractorLedgerEntryEntity>>

    @Query("SELECT * FROM contractor_ledger_entries WHERE contractorId = :contractorId AND payoutClass = :payoutClass ORDER BY transactionDate DESC")
    fun getContractorLedgerByClass(contractorId: String, payoutClass: String): Flow<List<ContractorLedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContractor(contractor: ContractorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalaryProfile(profile: SalaryProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutRun(run: WeeklyPayoutRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutItems(items: List<PayoutItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractorLedgerEntry(entry: ContractorLedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractorLedgerEntries(entries: List<ContractorLedgerEntryEntity>)

    @Query("UPDATE weekly_payouts SET status = :status WHERE id = :runId")
    suspend fun updatePayoutRunStatusOnly(runId: String, status: String)

    @Query("UPDATE payout_items SET status = :status WHERE payoutId = :runId")
    suspend fun updatePayoutItemsStatusForRun(runId: String, status: String)

    @Query("UPDATE payout_items SET status = :status, amountPaid = :amountPaid WHERE id = :itemId")
    suspend fun updatePayoutItem(itemId: String, status: String, amountPaid: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Transaction
    suspend fun insertFullPayoutRun(
        run: WeeklyPayoutRunEntity,
        items: List<PayoutItemEntity>
    ) {
        insertPayoutRun(run)
        if (items.isNotEmpty()) {
            insertPayoutItems(items)
        }
    }

    @Transaction
    suspend fun updatePayoutRunStatus(runId: String, status: String) {
        val currentRun = findPayoutRunById(runId)
        if (status.equals("paid", ignoreCase = true) && currentRun?.status?.equals("paid", ignoreCase = true) == true) {
            return
        }
        updatePayoutRunStatusOnly(runId, status)
        updatePayoutItemsStatusForRun(runId, status)
        if (status.equals("paid", ignoreCase = true)) {
            val items = getPayoutItemsList(runId)
            for (item in items) {
                val disbursedAmount = if (item.amountPaid > 0.0) item.amountPaid else item.amountDue
                if (item.amountPaid <= 0.0 && disbursedAmount > 0.0) {
                    updatePayoutItem(item.id, status, disbursedAmount)
                }
                if (!item.projectId.isNullOrBlank() && disbursedAmount > 0.0) {
                    val category = if (item.recipientType.equals("vendor_payment", ignoreCase = true)) "materials" else "labor"
                    val expense = ExpenseEntity(
                        projectId = item.projectId,
                        userId = item.recipientId,
                        amount = disbursedAmount,
                        category = category,
                        description = "Payout: ${item.recipientName}",
                        expenseDate = java.time.LocalDate.now().toString(),
                        paymentStatus = "paid",
                        paymentMode = "bank_transfer",
                        notes = item.notes ?: item.referenceDetails
                    )
                    insertExpense(expense)
                }
            }
        }
    }

    // Company Material Prices Master Catalog
    @Query("SELECT * FROM company_material_prices ORDER BY materialName ASC")
    fun getCompanyMaterialPrices(): Flow<List<CompanyMaterialPriceEntity>>

    @Query("SELECT * FROM company_material_prices WHERE category = :category ORDER BY materialName ASC")
    fun getCompanyMaterialPricesByCategory(category: String): Flow<List<CompanyMaterialPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompanyMaterialPrice(price: CompanyMaterialPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompanyMaterialPrices(prices: List<CompanyMaterialPriceEntity>)

    @Query("DELETE FROM company_material_prices WHERE id = :id")
    suspend fun deleteCompanyMaterialPrice(id: String)

    // Material Takeoff & Consumption Reconciliation
    @Query("SELECT * FROM material_takeoff_reconciliations WHERE projectId = :projectId ORDER BY materialName ASC")
    fun getReconciliationItems(projectId: String): Flow<List<MaterialTakeoffReconciliationEntity>>

    @Query("SELECT * FROM material_takeoff_reconciliations ORDER BY materialName ASC")
    fun getAllReconciliationItems(): Flow<List<MaterialTakeoffReconciliationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReconciliationItem(item: MaterialTakeoffReconciliationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReconciliationItems(items: List<MaterialTakeoffReconciliationEntity>)

    @Query("DELETE FROM material_takeoff_reconciliations WHERE id = :id")
    suspend fun deleteReconciliationItem(id: String)

    // Client Milestones Tracking
    @Query("SELECT * FROM client_milestones WHERE projectId = :projectId ORDER BY stageNumber ASC, dueDate ASC")
    fun getMilestonesForProject(projectId: String): Flow<List<ClientMilestoneEntity>>

    @Query("SELECT * FROM client_milestones ORDER BY stageNumber ASC, dueDate ASC")
    fun getAllMilestones(): Flow<List<ClientMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: ClientMilestoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestones(milestones: List<ClientMilestoneEntity>)

    @Query("UPDATE client_milestones SET collectedAmount = :collectedAmount, status = :status, paymentDate = :paymentDate, paymentMode = :paymentMode WHERE id = :id")
    suspend fun recordMilestonePayment(id: String, collectedAmount: Double, status: String, paymentDate: String, paymentMode: String)

    @Query("DELETE FROM client_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY expenseDate DESC")
    fun getProjectExpenses(projectId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAllCompanyExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY expenseDate DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND category = :category ORDER BY expenseDate DESC")
    fun getExpensesByProjectAndCategory(projectId: String, category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE syncStatus != 0")
    suspend fun getPendingExpenses(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("UPDATE expenses SET paymentStatus = :status WHERE id = :id")
    suspend fun updateExpensePaymentStatus(id: String, status: String)

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId")
    fun getTotalProjectExpenses(projectId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalCompanyExpenses(): Flow<Double?>
}

@Dao
interface WorkPrepDao {
    @Query("SELECT * FROM work_prep_contractor_calls WHERE date = :date ORDER BY contractorName ASC")
    fun getCallsForDate(date: String): Flow<List<WorkPrepCallEntity>>

    @Query("SELECT * FROM work_prep_contractor_calls WHERE date = :date AND contractorId = :contractorId LIMIT 1")
    suspend fun getCall(date: String, contractorId: String): WorkPrepCallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCall(call: WorkPrepCallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalls(calls: List<WorkPrepCallEntity>)

    @Query("UPDATE work_prep_contractor_calls SET callStatus = :status, lastCallTimestamp = :timestamp WHERE id = :id")
    suspend fun updateCallStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE work_prep_contractor_calls SET confirmedWorkerCount = :count WHERE id = :id")
    suspend fun updateConfirmedWorkers(id: String, count: Int)

    @Query("DELETE FROM work_prep_contractor_calls WHERE date = :date")
    suspend fun clearCallsForDate(date: String)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getProjectDocuments(projectId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM personal_documents WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPersonalDocuments(userId: String): Flow<List<PersonalDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalDocument(doc: PersonalDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordDocumentRead(read: DocumentReadEntity)
}

@Dao
interface PersonalPouchDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPersonalNotes(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isSyncedWithSupabase = 0")
    suspend fun getUnsyncedSupabaseNotes(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isSyncedWithSheets = 0")
    suspend fun getUnsyncedSheetsNotes(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteNote(id: String)
}

@Dao
interface TeamHubDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getChatMessages(projectId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE syncStatus = 1")
    suspend fun getPendingChatMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)
}
