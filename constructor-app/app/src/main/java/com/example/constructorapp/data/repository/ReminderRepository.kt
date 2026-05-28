package com.example.constructorapp.data.repository

import android.util.Log
import com.example.constructorapp.data.local.AppDao
import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.remote.GoogleSheetsService
import com.example.constructorapp.data.remote.PersonalNoteDto
import com.example.constructorapp.data.remote.SupabaseClient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReminderRepository(private val appDao: AppDao) {
    private val TAG = "ReminderRepository"

    fun getReminders(userId: String): Flow<List<ReminderEntity>> {
        return appDao.getReminders(userId)
    }

    suspend fun getReminderById(id: String): ReminderEntity? {
        return appDao.getReminderById(id)
    }

    suspend fun saveReminder(reminder: ReminderEntity): Result<Unit> {
        // Save locally first
        appDao.insertReminder(reminder)

        val authHeader = SupabaseClient.authHeader
        val userId = SupabaseClient.userId
        if (authHeader.isEmpty() || userId == null) {
            Log.w(TAG, "Not authenticated in Supabase, reminder saved offline-only")
            return Result.success(Unit)
        }

        return try {
            // Attempt to upload/sync to Supabase
            val noteDto = PersonalNoteDto(
                id = reminder.id,
                userId = userId,
                title = reminder.title,
                description = reminder.description,
                priority = reminder.priority,
                location = reminder.location,
                dueDate = reminder.dueDate,
                isCompleted = reminder.isCompleted,
                createdAt = reminder.createdAt.ifEmpty { null }
            )

            // Let's check if the note exists locally and was previously synced.
            // If it is unsynced, we try to create it or update it. Since Supabase Postgrest supports upsert via POST with header,
            // or we can patch/insert. Let's try inserting first. If it fails due to conflict, we update.
            // To do a proper sync, we can use createPersonalNote, and if it fails, we fall back to update.
            val insertResponse = SupabaseClient.api.createPersonalNote(authHeader, noteDto)
            if (insertResponse.isSuccessful) {
                // Success! Mark synced
                appDao.insertReminder(reminder.copy(isSyncedWithSupabase = true))
                Result.success(Unit)
            } else {
                // Conflict or failure? Try updating
                val updateMap = mapOf(
                    "title" to reminder.title,
                    "description" to (reminder.description ?: ""),
                    "priority" to reminder.priority,
                    "location" to (reminder.location ?: ""),
                    "due_date" to (reminder.dueDate ?: ""),
                    "is_completed" to reminder.isCompleted
                )
                val updateResponse = SupabaseClient.api.updatePersonalNote(authHeader, "eq.${reminder.id}", updateMap)
                if (updateResponse.isSuccessful) {
                    appDao.insertReminder(reminder.copy(isSyncedWithSupabase = true))
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Failed to sync to Supabase: ${insertResponse.code()} / ${updateResponse.code()}")
                    Result.success(Unit) // Still return success since we wrote locally
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure syncing reminder, saved offline", e)
            Result.success(Unit) // Offline success
        }
    }

    suspend fun deleteReminder(id: String): Result<Unit> {
        // Delete locally
        appDao.deleteReminderById(id)

        val authHeader = SupabaseClient.authHeader
        if (authHeader.isEmpty()) {
            return Result.success(Unit)
        }

        return try {
            val response = SupabaseClient.api.deletePersonalNote(authHeader, "eq.$id")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to delete reminder from Supabase: ${response.code()}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure deleting reminder from Supabase", e)
            Result.success(Unit)
        }
    }

    suspend fun syncWithSupabase(): Result<Unit> {
        val authHeader = SupabaseClient.authHeader
        val userId = SupabaseClient.userId
        if (authHeader.isEmpty() || userId == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        return try {
            // 1. Pull from Supabase
            val response = SupabaseClient.api.getPersonalNotes(authHeader, "eq.$userId")
            if (response.isSuccessful) {
                val notes = response.body() ?: emptyList()
                appDao.insertReminders(notes.map {
                    ReminderEntity(
                        id = it.id ?: UUID.randomUUID().toString(),
                        userId = it.userId,
                        title = it.title,
                        description = it.description,
                        priority = it.priority,
                        location = it.location,
                        dueDate = it.dueDate,
                        isCompleted = it.isCompleted,
                        isSyncedWithSupabase = true,
                        createdAt = it.createdAt ?: ""
                    )
                })

                // 2. Push unsynced local reminders
                val unsynced = appDao.getUnsyncedReminders()
                for (reminder in unsynced) {
                    saveReminder(reminder)
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to pull from Supabase: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithGoogleSheets(spreadsheetId: String, accessToken: String?): Result<Unit> {
        val userId = SupabaseClient.userId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Get unsynced sheets reminders or all of them? Exporting all or new ones is best.
            // Let's sync all reminders to ensure the sheet is fully up to date.
            val localReminders = appDao.getUnsyncedSheetsReminders()
            if (localReminders.isEmpty()) {
                return Result.success(Unit)
            }

            val success = GoogleSheetsService.syncToGoogleSheets(spreadsheetId, accessToken, localReminders)
            if (success) {
                // Mark synced in database
                for (reminder in localReminders) {
                    appDao.insertReminder(reminder.copy(isSyncedWithSheets = true))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to upload to Google Sheets"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
