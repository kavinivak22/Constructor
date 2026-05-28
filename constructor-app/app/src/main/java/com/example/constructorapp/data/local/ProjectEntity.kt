package com.example.constructorapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val status: String?,
    val progress: Int,
    val clientName: String?,
    val location: String?,
    val budget: Double,
    val companyId: String
)
