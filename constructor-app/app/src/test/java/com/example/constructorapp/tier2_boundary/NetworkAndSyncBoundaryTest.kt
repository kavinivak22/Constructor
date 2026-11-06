package com.example.constructorapp.tier2_boundary

import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.LaborEntryDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.fakes.FakeAppDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NetworkAndSyncBoundaryTest {

    @Before
    fun setUp() {
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    @After
    fun tearDown() {
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    @Test
    fun testSupabaseAuthHeader_nullAndEmptyToken() {
        SupabaseClient.authToken = null
        assertEquals("", SupabaseClient.authHeader)

        SupabaseClient.authToken = ""
        assertEquals("Bearer ", SupabaseClient.authHeader)

        SupabaseClient.authToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        assertEquals("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", SupabaseClient.authHeader)
    }

    @Test
    fun testOfflineSyncFlags_filteringPermutations() = runTest {
        val fakeDao = FakeAppDao()

        val rBothUnsynced = ReminderEntity(id = "r1", userId = "u1", title = "T1", isSyncedWithSupabase = false, isSyncedWithSheets = false)
        val rSupabaseSynced = ReminderEntity(id = "r2", userId = "u1", title = "T2", isSyncedWithSupabase = true, isSyncedWithSheets = false)
        val rSheetsSynced = ReminderEntity(id = "r3", userId = "u1", title = "T3", isSyncedWithSupabase = false, isSyncedWithSheets = true)
        val rBothSynced = ReminderEntity(id = "r4", userId = "u1", title = "T4", isSyncedWithSupabase = true, isSyncedWithSheets = true)

        fakeDao.insertReminders(listOf(rBothUnsynced, rSupabaseSynced, rSheetsSynced, rBothSynced))

        val unsyncedSupabase = fakeDao.getUnsyncedReminders()
        assertEquals(2, unsyncedSupabase.size)
        assertTrue(unsyncedSupabase.any { it.id == "r1" })
        assertTrue(unsyncedSupabase.any { it.id == "r3" })
        assertFalse(unsyncedSupabase.any { it.id == "r2" })
        assertFalse(unsyncedSupabase.any { it.id == "r4" })

        val unsyncedSheets = fakeDao.getUnsyncedSheetsReminders()
        assertEquals(2, unsyncedSheets.size)
        assertTrue(unsyncedSheets.any { it.id == "r1" })
        assertTrue(unsyncedSheets.any { it.id == "r2" })
        assertFalse(unsyncedSheets.any { it.id == "r3" })
        assertFalse(unsyncedSheets.any { it.id == "r4" })
    }

    @Test
    fun testDtos_nullAndOptionalFieldsHandling() {
        val worklog = DailyWorklogDto(
            projectId = "p-1",
            date = "2026-11-01"
            // id and createdBy are null
        )
        assertNull(worklog.id)
        assertNull(worklog.createdBy)
        assertEquals("p-1", worklog.projectId)
        assertEquals("2026-11-01", worklog.date)

        val laborEntry = LaborEntryDto(
            worklogId = "wl-1",
            contractorName = "Murugan"
            // category, workDescription, workType, etc. are null/defaults
        )
        assertNull(laborEntry.id)
        assertNull(laborEntry.category)
        assertNull(laborEntry.workDescription)
        assertEquals("Pending", laborEntry.paymentStatus)
        assertEquals("NMR", laborEntry.payoutClass)

        val task = TaskItemDto(
            projectId = "p-1",
            title = "Site Safety Audit"
            // description, dueDate, assignedTo are null
        )
        assertNull(task.id)
        assertNull(task.description)
        assertNull(task.dueDate)
        assertNull(task.assignedTo)
        assertFalse(task.isCompleted)
        assertEquals("Medium", task.priority)
    }
}
