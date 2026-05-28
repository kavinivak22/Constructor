package com.example.constructorapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.repository.ReminderRepository
import com.example.constructorapp.worker.ReminderNotificationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val TAG = "ReminderViewModel"

    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val reminders: StateFlow<List<ReminderEntity>> = _reminders.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    fun loadReminders() {
        val userId = SupabaseClient.userId ?: return
        viewModelScope.launch {
            repository.getReminders(userId).collect {
                _reminders.value = it
            }
        }
    }

    fun syncData() {
        _syncState.value = SyncUiState.Syncing
        viewModelScope.launch {
            val result = repository.syncWithSupabase()
            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success("Synced with Supabase successfully")
            } else {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Supabase Sync failed")
            }
        }
    }

    fun saveReminder(
        id: String? = null,
        title: String,
        description: String,
        priority: String,
        location: String,
        dueDate: String
    ) {
        val userId = SupabaseClient.userId ?: "offline_user"
        viewModelScope.launch {
            val reminder = ReminderEntity(
                id = id ?: UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                description = description.ifEmpty { null },
                priority = priority,
                location = location.ifEmpty { null },
                dueDate = dueDate.ifEmpty { null },
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            val result = repository.saveReminder(reminder)
            if (result.isSuccess) {
                // If reminder has a due date, schedule notification
                if (!dueDate.isNullOrBlank()) {
                    scheduleReminderNotification(reminder)
                }
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            cancelReminderNotification(id)
        }
    }

    fun toggleReminderCompletion(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted, isSyncedWithSupabase = false)
            repository.saveReminder(updated)
            if (updated.isCompleted) {
                cancelReminderNotification(updated.id)
            } else if (!updated.dueDate.isNullOrBlank()) {
                scheduleReminderNotification(updated)
            }
        }
    }

    fun syncGoogleSheets(context: Context, spreadsheetId: String, token: String?) {
        _syncState.value = SyncUiState.Syncing
        viewModelScope.launch {
            // Store credentials in shared preferences for worker usage
            val sharedPref = context.getSharedPreferences("constructor_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putString("spreadsheet_id", spreadsheetId)
                putString("google_access_token", token ?: "")
                apply()
            }

            val result = repository.syncWithGoogleSheets(spreadsheetId, token)
            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success("Synced with Google Sheets successfully")
            } else {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Sheets Sync failed")
            }
        }
    }

    private fun scheduleReminderNotification(reminder: ReminderEntity) {
        if (reminder.dueDate.isNullOrBlank()) return

        try {
            // Parse YYYY-MM-DD and set time to 9:00 AM on that day
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(reminder.dueDate) ?: return

            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            val triggerTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            val delay = triggerTime - currentTime

            if (delay > 0) {
                val data = Data.Builder()
                    .putString("reminder_id", reminder.id)
                    .putString("title", reminder.title)
                    .putString("description", reminder.description ?: "")
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("reminder_${reminder.id}")
                    .build()

                workManager.enqueue(workRequest)
                Log.i(TAG, "Scheduled reminder notification in ${delay / 1000} seconds")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification", e)
        }
    }

    private fun cancelReminderNotification(reminderId: String) {
        workManager.cancelAllWorkByTag("reminder_$reminderId")
        Log.i(TAG, "Cancelled notification for reminder $reminderId")
    }

    fun clearState() {
        _syncState.value = SyncUiState.Idle
    }
}

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}
