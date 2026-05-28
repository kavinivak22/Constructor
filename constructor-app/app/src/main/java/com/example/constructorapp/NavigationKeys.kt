package com.example.constructorapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object Main : NavKey
@Serializable data class Worklog(val projectId: String, val projectName: String) : NavKey
@Serializable data class AddEditReminder(val reminderId: String? = null) : NavKey

