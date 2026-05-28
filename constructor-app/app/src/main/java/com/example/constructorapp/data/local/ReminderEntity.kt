package com.example.constructorapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val description: String? = null,
    val priority: String = "Medium", // High, Medium, Low
    val location: String? = null,    // Accept name or lat/lng
    val dueDate: String? = null,     // Format YYYY-MM-DD
    val isCompleted: Boolean = false,
    val isSyncedWithSheets: Boolean = false,
    val isSyncedWithSupabase: Boolean = false,
    val createdAt: String = ""
)
