package com.example.constructorapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.repository.ReminderRepository

class SheetsSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "SheetsSyncWorker"

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic Google Sheets synchronization...")

        val sharedPref = applicationContext.getSharedPreferences("constructor_prefs", Context.MODE_PRIVATE)
        val spreadsheetId = sharedPref.getString("spreadsheet_id", "") ?: ""
        val googleToken = sharedPref.getString("google_access_token", "") ?: ""

        if (spreadsheetId.isBlank()) {
            Log.w(TAG, "Sync skipped: No Spreadsheet ID configured")
            return Result.success()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = ReminderRepository(db.appDao)

        val result = repository.syncWithGoogleSheets(
            spreadsheetId = spreadsheetId,
            accessToken = googleToken.ifEmpty { null }
        )

        return if (result.isSuccess) {
            Log.i(TAG, "Periodic Google Sheets sync completed successfully")
            Result.success()
        } else {
            Log.e(TAG, "Periodic Google Sheets sync failed: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }
}
