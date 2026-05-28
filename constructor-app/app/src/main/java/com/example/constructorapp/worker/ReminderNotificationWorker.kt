package com.example.constructorapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.constructorapp.MainActivity
import com.example.constructorapp.data.local.AppDatabase

class ReminderNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "ReminderNotificationWorker"
    private val CHANNEL_ID = "reminder_notifications"

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminder_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Reminder"
        val description = inputData.getString("description") ?: ""

        Log.i(TAG, "Triggering notification for reminder $reminderId: $title")

        // Double check in database if the reminder is still active and not deleted/completed
        val db = AppDatabase.getDatabase(applicationContext)
        val reminder = db.appDao.getReminderById(reminderId)
        if (reminder == null || reminder.isCompleted) {
            Log.i(TAG, "Reminder is completed or deleted. Skipping notification.")
            return Result.success()
        }

        sendNotification(title, description, reminderId)

        return Result.success()
    }

    private fun sendNotification(title: String, message: String, reminderId: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Constructor Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers notifications for personal reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action opens MainActivity
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Constructor Premium Visual Theme (Vivid Coral logo tint color if needed)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System fallback alarm icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFF6B35.toInt()) // Constructor Vivid Coral Primary Color
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
    }
}
