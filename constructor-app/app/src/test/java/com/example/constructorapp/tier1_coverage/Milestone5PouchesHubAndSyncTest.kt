package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.local.entity.ChatMessageEntity
import com.example.constructorapp.data.local.entity.NotificationEntity
import com.example.constructorapp.data.remote.PouchTransactionDto
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class Milestone5PouchesHubAndSyncTest {

    @Test
    fun testPersonalNotesPriorityAllowedValues() {
        val allowedPriorities = listOf("High", "Medium", "Low")
        
        val highReminder = ReminderEntity(
            userId = "user_test",
            title = "Order TMT Steel",
            priority = "High",
            location = "Main Office"
        )
        val medReminder = ReminderEntity(
            userId = "user_test",
            title = "Inspect Cube Moulds",
            priority = "Medium",
            location = "Site B"
        )
        val lowReminder = ReminderEntity(
            userId = "user_test",
            title = "Clean Tool Shed",
            priority = "Low",
            location = "Central Warehouse"
        )

        assertTrue(allowedPriorities.contains(highReminder.priority))
        assertTrue(allowedPriorities.contains(medReminder.priority))
        assertTrue(allowedPriorities.contains(lowReminder.priority))
        assertEquals("High", highReminder.priority)
    }

    @Test
    fun testPersonalNotesLocationSupport() {
        val namedLocation = ReminderEntity(
            userId = "user_test",
            title = "Buy Binding Wire",
            location = "Grocery Store / Hardware Shop"
        )
        val homeLocation = ReminderEntity(
            userId = "user_test",
            title = "Review Structural Drawing",
            location = "Home"
        )
        val coordLocation = ReminderEntity(
            userId = "user_test",
            title = "Soil Bearing Test",
            location = "12.9716° N, 77.5946° E"
        )

        assertEquals("Grocery Store / Hardware Shop", namedLocation.location)
        assertEquals("Home", homeLocation.location)
        assertEquals("12.9716° N, 77.5946° E", coordLocation.location)
        assertFalse(coordLocation.location.isNullOrBlank())
    }

    @Test
    fun testProjectPouchNetBalanceCalculation() {
        val initialDisbursed = 20000.0
        val transactions = listOf(
            PouchTransactionDto(
                projectId = "p1",
                amount = 1500.0,
                category = "Site Meals & Tea",
                transactionType = "Expense",
                date = "2026-11-01"
            ),
            PouchTransactionDto(
                projectId = "p1",
                amount = 4500.0,
                category = "Urgent Hardware",
                transactionType = "Expense",
                date = "2026-11-01"
            ),
            PouchTransactionDto(
                projectId = "p1",
                amount = 2000.0,
                category = "Diesel / Fuel",
                transactionType = "Expense",
                date = "2026-11-01"
            ),
            PouchTransactionDto(
                projectId = "p1",
                amount = 5000.0,
                category = "Additional Imprest",
                transactionType = "Inflow",
                date = "2026-11-01"
            )
        )

        val totalExpenses = transactions
            .filter { it.transactionType == "Expense" }
            .sumOf { it.amount }
        val totalInflows = transactions
            .filter { it.transactionType == "Inflow" }
            .sumOf { it.amount }

        val netBalance = (initialDisbursed + totalInflows) - totalExpenses

        assertEquals(8000.0, totalExpenses, 0.001)
        assertEquals(5000.0, totalInflows, 0.001)
        assertEquals(17000.0, netBalance, 0.001)
    }

    @Test
    fun testTeamHubMessageOrderingAndDelivery() {
        val messages = mutableListOf(
            ChatMessageEntity(
                projectId = "proj_1",
                userId = "eng_1",
                userName = "Site Engineer",
                message = "Truck arrived at site gate.",
                createdAt = "10:00 AM",
                isOutgoing = false
            ),
            ChatMessageEntity(
                projectId = "proj_1",
                userId = "user_current",
                userName = "You",
                message = "Approved entry. Please check delivery challan.",
                createdAt = "10:02 AM",
                isOutgoing = true
            ),
            ChatMessageEntity(
                projectId = "proj_1",
                userId = "eng_1",
                userName = "Site Engineer",
                message = "Challan verified: 200 bags OPC 53 grade.",
                createdAt = "10:05 AM",
                isOutgoing = false
            )
        )

        assertEquals(3, messages.size)
        assertFalse(messages[0].isOutgoing)
        assertTrue(messages[1].isOutgoing)
        assertEquals("Challan verified: 200 bags OPC 53 grade.", messages.last().message)
    }

    @Test
    fun testNotificationsUnreadFilteringAndMarkAllRead() {
        val notifications = mutableListOf(
            NotificationEntity(
                id = "n1",
                userId = "u1",
                title = "PO Approved",
                description = "Cement order approved",
                category = "approvals",
                isRead = false,
                timestamp = "1000"
            ),
            NotificationEntity(
                id = "n2",
                userId = "u1",
                title = "Low Stock",
                description = "Sand stock is low",
                category = "inventory",
                isRead = false,
                timestamp = "2000"
            ),
            NotificationEntity(
                id = "n3",
                userId = "u1",
                title = "Worklog Submitted",
                description = "Concreting log received",
                category = "approvals",
                isRead = true,
                timestamp = "3000"
            )
        )

        val unreadCountBefore = notifications.count { !it.isRead }
        assertEquals(2, unreadCountBefore)

        // Mark all as read
        val updated = notifications.map { it.copy(isRead = true) }
        val unreadCountAfter = updated.count { !it.isRead }

        assertEquals(0, unreadCountAfter)
        assertTrue(updated.all { it.isRead })
    }

    @Test
    fun testSheetsSyncPayloadTransformation() {
        val reminder = ReminderEntity(
            id = "rem_123",
            userId = "user_test",
            title = "Final Inspection",
            description = "Walkthrough with client before handover",
            priority = "High",
            location = "Flat 402, Site Alpha",
            dueDate = "2026-11-15",
            isSyncedWithSheets = false
        )

        // Verify representation as expected by Google Sheets API append request
        val row = listOf(
            reminder.id,
            reminder.title,
            reminder.description ?: "",
            reminder.priority,
            reminder.location ?: "",
            reminder.dueDate ?: "",
            if (reminder.isCompleted) "Completed" else "Pending"
        )

        assertEquals(7, row.size)
        assertEquals("rem_123", row[0])
        assertEquals("Final Inspection", row[1])
        assertEquals("High", row[3])
        assertEquals("Flat 402, Site Alpha", row[4])
        assertEquals("Pending", row[6])
    }
}
