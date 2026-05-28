package com.example.constructorapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Reminders Operations
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isSyncedWithSupabase = 0")
    suspend fun getUnsyncedReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isSyncedWithSheets = 0")
    suspend fun getUnsyncedSheetsReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    // Projects Operations
    @Query("SELECT * FROM projects")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects")
    suspend fun clearProjects()
}
