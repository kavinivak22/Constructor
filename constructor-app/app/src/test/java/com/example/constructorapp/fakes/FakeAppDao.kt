package com.example.constructorapp.fakes

import com.example.constructorapp.data.local.AppDao
import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppDao : AppDao {
    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())
    private val projectsFlow = MutableStateFlow<List<ProjectEntity>>(emptyList())

    override fun getReminders(userId: String): Flow<List<ReminderEntity>> {
        return remindersFlow.map { list ->
            list.filter { it.userId == userId }
        }
    }

    override suspend fun getReminderById(id: String): ReminderEntity? {
        return remindersFlow.value.find { it.id == id }
    }

    override suspend fun getUnsyncedReminders(): List<ReminderEntity> {
        return remindersFlow.value.filter { !it.isSyncedWithSupabase }
    }

    override suspend fun getUnsyncedSheetsReminders(): List<ReminderEntity> {
        return remindersFlow.value.filter { !it.isSyncedWithSheets }
    }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        val current = remindersFlow.value.toMutableList()
        current.removeAll { it.id == reminder.id }
        current.add(reminder)
        remindersFlow.value = current
    }

    override suspend fun insertReminders(reminders: List<ReminderEntity>) {
        val current = remindersFlow.value.toMutableList()
        val ids = reminders.map { it.id }.toSet()
        current.removeAll { it.id in ids }
        current.addAll(reminders)
        remindersFlow.value = current
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        val current = remindersFlow.value.toMutableList()
        current.removeAll { it.id == reminder.id }
        remindersFlow.value = current
    }

    override suspend fun deleteReminderById(id: String) {
        val current = remindersFlow.value.toMutableList()
        current.removeAll { it.id == id }
        remindersFlow.value = current
    }

    override fun getProjects(): Flow<List<ProjectEntity>> {
        return projectsFlow
    }

    override suspend fun getProjectsSync(): List<ProjectEntity> {
        return projectsFlow.value
    }

    override suspend fun insertProjects(projects: List<ProjectEntity>) {
        val current = projectsFlow.value.toMutableList()
        val ids = projects.map { it.id }.toSet()
        current.removeAll { it.id in ids }
        current.addAll(projects)
        projectsFlow.value = current
    }

    override suspend fun clearProjects() {
        projectsFlow.value = emptyList()
    }
}
