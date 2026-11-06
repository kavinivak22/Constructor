package com.example.constructorapp.tier4_workflows

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.ReminderEntity
import com.example.constructorapp.data.remote.*
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.MaterialConsumedInput
import com.example.constructorapp.data.repository.WorkerCountInput
import com.example.constructorapp.fakes.FakeAppDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RealWorldWorkflowsTest {

    @Test
    fun testWorkflow1_projectSetupAndMilestonePlanning() = runTest {
        val fakeDao = FakeAppDao()

        // 1. Create Project Entity
        val project = ProjectEntity(
            id = "proj-godrej",
            name = "Godrej Palm Grove Villa",
            description = "Luxury G+2 Villa with landscaped garden",
            startDate = "2026-11-01",
            endDate = "2027-10-31",
            status = "Planning",
            progress = 0,
            clientName = "Godrej Properties",
            clientPhone = "9845012345",
            location = "Kochi, Kerala",
            budget = 38_000_000.0, // ₹3.8 Crore
            companyId = "godrej-infra"
        )
        fakeDao.insertProjects(listOf(project))

        val storedProjects = fakeDao.getProjects().first()
        assertEquals(1, storedProjects.size)
        assertEquals("Godrej Palm Grove Villa", storedProjects[0].name)
        assertEquals(38_000_000.0, storedProjects[0].budget, 0.001)

        // 2. Assign Initial Critical Tasks
        val initialTasks = mutableListOf(
            TaskItemDto(id = "task-1", projectId = project.id, title = "Soil Bearing Capacity Test", priority = "High", status = "completed"),
            TaskItemDto(id = "task-2", projectId = project.id, title = "Local Municipality Plan Sanction", priority = "High", status = "completed"),
            TaskItemDto(id = "task-3", projectId = project.id, title = "Temporary Electrical & Water Borewell", priority = "Medium", status = "pending"),
            TaskItemDto(id = "task-4", projectId = project.id, title = "Excavation Boundary Layout Marking", priority = "High", status = "pending")
        )

        assertEquals(4, initialTasks.size)
        val completedCount = initialTasks.count { it.isCompleted }
        assertEquals(2, completedCount)

        // 3. Complete Task 3 and verify progress
        val t3Idx = initialTasks.indexOfFirst { it.id == "task-3" }
        initialTasks[t3Idx] = initialTasks[t3Idx].copy(status = "completed")

        assertEquals(3, initialTasks.count { it.isCompleted })
        val completionRate = initialTasks.count { it.isCompleted }.toFloat() / initialTasks.size
        assertEquals(0.75f, completionRate, 0.01f)
    }

    @Test
    fun testWorkflow2_dailyShiftExecutionAndMaterialDepletion() {
        val projectId = "proj-site-4"

        // 1. Initial Site Materials Inventory
        val inventory = mutableMapOf(
            "mat-cement" to ProjectMaterialDto(id = "mat-cement", projectId = projectId, name = "OPC 53 Cement", quantity = 300.0, unit = "bags"),
            "mat-sand" to ProjectMaterialDto(id = "mat-sand", projectId = projectId, name = "M-Sand", quantity = 600.0, unit = "cft"),
            "mat-bricks" to ProjectMaterialDto(id = "mat-bricks", projectId = projectId, name = "Wirecut Red Bricks", quantity = 5000.0, unit = "nos")
        )

        // 2. Record Daily Labor Shift (NMR + Rate)
        val nmrLabor = LaborEntryInput(
            contractorName = "Shanmugam Daily Gang",
            category = "Civil",
            workDescription = "Basement excavation and site clearing",
            paymentStatus = "Pending",
            payoutClass = "NMR",
            workerCounts = listOf(
                WorkerCountInput("Mason", 3),
                WorkerCountInput("Helper", 7),
                WorkerCountInput("Barbending", 1)
            )
        )

        val rateLabor = LaborEntryInput(
            contractorName = "Kannan Bricklayers",
            category = "Masonry",
            workDescription = "First floor perimeter wall 9 inch",
            paymentStatus = "Pending",
            payoutClass = "Rate",
            workType = "Brickwork 9 inch",
            workDoneQuantity = 1200.0,
            workDoneUnit = "sq.ft",
            payoutRate = 24.0,
            totalPayout = 1200.0 * 24.0 // ₹28,800
        )

        val consumedMaterials = listOf(
            MaterialConsumedInput("mat-cement", "OPC 53 Cement", 45.0, "bags"),
            MaterialConsumedInput("mat-sand", "M-Sand", 150.0, "cft"),
            MaterialConsumedInput("mat-bricks", "Wirecut Red Bricks", 2500.0, "nos")
        )

        // 3. Verify Stock Depletion
        for (consumed in consumedMaterials) {
            val stock = inventory[consumed.projectMaterialId]
            assertNotNull(stock)
            assertTrue(stock!!.quantity >= consumed.quantityConsumed)
            inventory[consumed.projectMaterialId!!] = stock.copy(
                quantity = stock.quantity - consumed.quantityConsumed
            )
        }

        assertEquals(255.0, inventory["mat-cement"]!!.quantity, 0.001) // 300 - 45
        assertEquals(450.0, inventory["mat-sand"]!!.quantity, 0.001)   // 600 - 150
        assertEquals(2500.0, inventory["mat-bricks"]!!.quantity, 0.001) // 5000 - 2500

        // 4. Calculate Total Shift Cost Liability
        val rateCost = rateLabor.totalPayout ?: 0.0
        val wageRates = mapOf("Mason" to 950.0, "Helper" to 650.0, "Barbending" to 1100.0)
        val nmrCost = nmrLabor.workerCounts.sumOf { it.count * (wageRates[it.workerType] ?: 600.0) }
        // (3 * 950) + (7 * 650) + (1 * 1100) = 2850 + 4550 + 1100 = 8500
        assertEquals(8500.0, nmrCost, 0.001)
        assertEquals(28800.0, rateCost, 0.001)
        assertEquals(37300.0, rateCost + nmrCost, 0.001)
    }

    @Test
    fun testWorkflow3_estimationProcurementAndPettyCashDisbursement() {
        val projectId = "proj-villa-12"

        // 1. Civil Estimation: 30ft x 20ft x 0.5ft slab = 300 cft wet volume
        val wetVolumeCft = 30.0 * 20.0 * 0.5 // 300 cft
        val dryVolumeCft = wetVolumeCft * 1.54 // 462 cft
        val totalPartsM20 = 5.5 // 1:1.5:3
        val requiredCementBags = (dryVolumeCft * (1.0 / totalPartsM20)) / 1.226 // ~68.5 bags
        val requiredSandCft = dryVolumeCft * (1.5 / totalPartsM20) // 126 cft
        val requiredAggCft = dryVolumeCft * (3.0 / totalPartsM20) // 252 cft

        assertEquals(68.5, requiredCementBags, 0.1)
        assertEquals(126.0, requiredSandCft, 0.1)
        assertEquals(252.0, requiredAggCft, 0.1)

        // 2. Generate Purchase Order for Procurement
        val poItems = listOf(
            PurchaseOrderItemDto("OPC 53 Grade Cement", 70.0, 420.0, "bags"), // 29400
            PurchaseOrderItemDto("Screened River Sand", 130.0, 55.0, "cft"),  // 7150
            PurchaseOrderItemDto("20mm Blue Metal Stone", 260.0, 48.0, "cft") // 12480
        )
        val poTotal = poItems.sumOf { it.totalPrice }
        assertEquals(49030.0, poTotal, 0.001)

        val po = PurchaseOrderDto(
            id = "po-slab-01",
            projectId = projectId,
            supplierName = "Sri Murugan Hardwares",
            status = "Draft",
            totalAmount = poTotal,
            items = poItems
        )
        assertEquals("Draft", po.status)

        // 3. Approve PO
        val approvedPo = po.copy(status = "Approved")
        assertEquals("Approved", approvedPo.status)

        // 4. Pay Urgent Delivery & Fuel from Site Petty Cash
        val startingPettyCash = 12000.0
        val deliveryExpense = PouchTransactionDto(
            projectId = projectId,
            amount = 1200.0,
            category = "Fuel & Diesel",
            description = "Diesel for concrete mixer & vibrator engine",
            transactionType = "Expense",
            date = "2026-11-01"
        )
        val endingPettyCash = startingPettyCash - deliveryExpense.amount
        assertEquals(10800.0, endingPettyCash, 0.001)
    }

    @Test
    fun testWorkflow4_offlinePersonalPouchToCloudSync() = runTest {
        val fakeDao = FakeAppDao()
        val userId = "engineer-kavin"

        // 1. Create offline note with Priority, Location, Due Date
        val note = ReminderEntity(
            id = "rem-offline-1",
            userId = userId,
            title = "Inspect column C4 tie stirrups spacing before concrete pour",
            description = "Ensure 100mm c/c spacing at beam-column junction",
            priority = "High",
            location = "Column Grid C4 (9.9816, 76.2999)",
            dueDate = "2026-11-02",
            isCompleted = false,
            isSyncedWithSupabase = false,
            isSyncedWithSheets = false,
            createdAt = "2026-11-01 08:00:00"
        )

        fakeDao.insertReminder(note)

        // Verify saved offline
        val unsyncedSupabase = fakeDao.getUnsyncedReminders()
        val unsyncedSheets = fakeDao.getUnsyncedSheetsReminders()
        assertEquals(1, unsyncedSupabase.size)
        assertEquals(1, unsyncedSheets.size)

        // 2. Engineer completes task on site
        val completedNote = note.copy(isCompleted = true)
        fakeDao.insertReminder(completedNote)

        val stored = fakeDao.getReminderById(note.id)
        assertNotNull(stored)
        assertTrue(stored!!.isCompleted)

        // 3. Simulate connectivity restored & Supabase sync
        fakeDao.insertReminder(completedNote.copy(isSyncedWithSupabase = true))
        assertEquals(0, fakeDao.getUnsyncedReminders().size)
        assertEquals(1, fakeDao.getUnsyncedSheetsReminders().size)

        // 4. Simulate Google Sheets automated backup
        fakeDao.insertReminder(completedNote.copy(isSyncedWithSupabase = true, isSyncedWithSheets = true))
        assertEquals(0, fakeDao.getUnsyncedReminders().size)
        assertEquals(0, fakeDao.getUnsyncedSheetsReminders().size)

        val finalNote = fakeDao.getReminderById(note.id)
        assertTrue(finalNote!!.isSyncedWithSupabase)
        assertTrue(finalNote.isSyncedWithSheets)
        assertTrue(finalNote.isCompleted)
    }
}
