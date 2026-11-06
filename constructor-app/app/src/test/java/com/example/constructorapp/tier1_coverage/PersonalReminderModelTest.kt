package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.ReminderEntity
import org.junit.Assert.*
import org.junit.Test

class PersonalReminderModelTest {

    @Test
    fun testReminderCreation_defaults() {
        val reminder = ReminderEntity(
            id = "rem-1",
            userId = "user-101",
            title = "Check slab shuttering plumb bob"
        )

        assertEquals("rem-1", reminder.id)
        assertEquals("user-101", reminder.userId)
        assertEquals("Check slab shuttering plumb bob", reminder.title)
        assertNull(reminder.description)
        assertEquals("Medium", reminder.priority)
        assertNull(reminder.location)
        assertNull(reminder.dueDate)
        assertFalse(reminder.isCompleted)
        assertFalse(reminder.isSyncedWithSheets)
        assertFalse(reminder.isSyncedWithSupabase)
    }

    @Test
    fun testReminderWithExplicitFields() {
        val reminder = ReminderEntity(
            id = "rem-2",
            userId = "user-101",
            title = "Order 200 bags of OPC 53 cement",
            description = "Needed for 2nd floor roof slab casting on Thursday",
            priority = "High",
            location = "Central Warehouse, Guindy",
            dueDate = "2026-11-05",
            isCompleted = false,
            isSyncedWithSheets = false,
            isSyncedWithSupabase = true,
            createdAt = "2026-11-01 09:30:00"
        )

        assertEquals("High", reminder.priority)
        assertEquals("Central Warehouse, Guindy", reminder.location)
        assertEquals("2026-11-05", reminder.dueDate)
        assertTrue(reminder.isSyncedWithSupabase)
        assertFalse(reminder.isSyncedWithSheets)
    }

    @Test
    fun testToggleCompletion_updatesFlagAndInvalidatesSync() {
        val original = ReminderEntity(
            id = "rem-3",
            userId = "user-101",
            title = "Review contractor bills",
            isCompleted = false,
            isSyncedWithSupabase = true
        )

        val completed = original.copy(isCompleted = true, isSyncedWithSupabase = false)
        assertTrue(completed.isCompleted)
        assertFalse(completed.isSyncedWithSupabase) // Needs re-sync to backend
    }

    @Test
    fun testLocationTypeDifferentiating() {
        fun isCoordinatePair(loc: String?): Boolean {
            if (loc.isNullOrBlank()) return false
            val parts = loc.split(",")
            if (parts.size != 2) return false
            val lat = parts[0].trim().toDoubleOrNull()
            val lng = parts[1].trim().toDoubleOrNull()
            return lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0
        }

        assertTrue(isCoordinatePair("37.4220,-122.0841"))
        assertTrue(isCoordinatePair("13.0827, 80.2707"))
        assertTrue(isCoordinatePair("-33.8688, 151.2093"))
        assertFalse(isCoordinatePair("Grocery Store"))
        assertFalse(isCoordinatePair("Site Office Gate 3"))
        assertFalse(isCoordinatePair(""))
        assertFalse(isCoordinatePair(null))
        assertFalse(isCoordinatePair("999.0,180.0")) // Latitude out of range
    }

    @Test
    fun testPriorityFilteringLogic() {
        val reminders = listOf(
            ReminderEntity(id = "1", userId = "u1", title = "T1", priority = "High"),
            ReminderEntity(id = "2", userId = "u1", title = "T2", priority = "Medium"),
            ReminderEntity(id = "3", userId = "u1", title = "T3", priority = "Low"),
            ReminderEntity(id = "4", userId = "u1", title = "T4", priority = "High")
        )

        val highOnly = reminders.filter { it.priority.equals("High", ignoreCase = true) }
        val mediumOnly = reminders.filter { it.priority.equals("Medium", ignoreCase = true) }
        val lowOnly = reminders.filter { it.priority.equals("Low", ignoreCase = true) }

        assertEquals(2, highOnly.size)
        assertEquals(1, mediumOnly.size)
        assertEquals(1, lowOnly.size)
    }
}
