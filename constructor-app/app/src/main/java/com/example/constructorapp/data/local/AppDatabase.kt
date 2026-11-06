package com.example.constructorapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.constructorapp.data.local.dao.*
import com.example.constructorapp.data.local.entity.*

/**
 * Room Database Version 3 with destructive migration fallback.
 * Registers all core entities and provides modular DAO accessors.
 */
@Database(
    entities = [
        ReminderEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        DailyWorklogEntity::class,
        WorklogLaborEntryEntity::class,
        WorklogWorkerCountEntity::class,
        WorklogMaterialEntity::class,
        WorklogPhotoEntity::class,
        InventoryAssetEntity::class,
        StockTransferEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        WeeklyPayoutRunEntity::class,
        PayoutItemEntity::class,
        ContractorEntity::class,
        SalaryProfileEntity::class,
        ExpenseEntity::class,
        WorkPrepCallEntity::class,
        DocumentEntity::class,
        DocumentReadEntity::class,
        PersonalDocumentEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        UserEntity::class,
        CompanyMaterialPriceEntity::class,
        MaterialTakeoffReconciliationEntity::class,
        ContractorLedgerEntryEntity::class,
        ClientMilestoneEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Legacy DAO preserved for backward compatibility with MainActivity & existing repos
    abstract val appDao: AppDao

    // Domain Modular DAOs
    abstract val projectDao: ProjectDao
    abstract val taskDao: TaskDao
    abstract val worklogDao: WorklogDao
    abstract val inventoryDao: InventoryDao
    abstract val purchaseOrderDao: PurchaseOrderDao
    abstract val financialsDao: FinancialsDao
    abstract val expenseDao: ExpenseDao
    abstract val workPrepDao: WorkPrepDao
    abstract val documentDao: DocumentDao
    abstract val personalPouchDao: PersonalPouchDao
    abstract val teamHubDao: TeamHubDao
    abstract val notificationDao: NotificationDao
    abstract val userDao: UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "constructor_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
