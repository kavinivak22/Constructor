package com.example.constructorapp.tier3_combinations

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.remote.*
import com.example.constructorapp.data.repository.LaborEntryInput
import com.example.constructorapp.data.repository.MaterialConsumedInput
import com.example.constructorapp.data.repository.WorkerCountInput
import org.junit.Assert.*
import org.junit.Test

class CrossFeatureCombinationsTest {

    @Test
    fun testProjectTaskChecklistCombination() {
        val project1Id = "proj-alpha"
        val project2Id = "proj-beta"

        val allTasks = mutableListOf(
            TaskItemDto(id = "t1", projectId = project1Id, title = "Soil Bearing Test", status = "completed", priority = "High"),
            TaskItemDto(id = "t2", projectId = project1Id, title = "Foundation Excavation", status = "completed", priority = "High"),
            TaskItemDto(id = "t3", projectId = project1Id, title = "Plinth Beam Casting", status = "pending", priority = "Medium"),
            TaskItemDto(id = "t4", projectId = project2Id, title = "Site Survey", status = "pending", priority = "Low")
        )

        // Filter for Project 1
        val proj1Tasks = allTasks.filter { it.projectId == project1Id }
        val proj1Completed = proj1Tasks.count { it.isCompleted }
        assertEquals(3, proj1Tasks.size)
        assertEquals(2, proj1Completed)

        // Toggle t3 in Project 1
        val index = allTasks.indexOfFirst { it.id == "t3" }
        allTasks[index] = allTasks[index].copy(status = "completed")

        val updatedCompleted = allTasks.filter { it.projectId == project1Id }.count { it.isCompleted }
        assertEquals(3, updatedCompleted)

        // Verify Project 2 was untouched
        val proj2Tasks = allTasks.filter { it.projectId == project2Id }
        assertEquals(1, proj2Tasks.size)
        assertFalse(proj2Tasks[0].isCompleted)
    }

    @Test
    fun testDailyWorklog_hybridLaborComposition() {
        // Daily worklog with 1 NMR entry and 1 Rate entry
        val nmrEntry = LaborEntryInput(
            contractorName = "Murugan Daily Gang",
            category = "Civil",
            workDescription = "Excavation and debris clearing",
            paymentStatus = "Pending",
            payoutClass = "NMR",
            workerCounts = listOf(
                WorkerCountInput("Mason", 2),
                WorkerCountInput("Helper", 6),
                WorkerCountInput("Supervisor", 1)
            )
        )

        val rateEntry = LaborEntryInput(
            contractorName = "Ramesh Kumar Plasterers",
            category = "Plastering",
            workDescription = "Ground floor ceiling plastering",
            paymentStatus = "Pending",
            payoutClass = "Rate",
            workType = "Plastering 15mm",
            workDoneQuantity = 800.0,
            workDoneUnit = "sq.ft",
            payoutRate = 18.0,
            totalPayout = 800.0 * 18.0 // 14400.0
        )

        val laborEntries = listOf(nmrEntry, rateEntry)

        // Compute total NMR workers
        val totalNmrWorkers = laborEntries
            .filter { it.payoutClass == "NMR" }
            .flatMap { it.workerCounts }
            .sumOf { it.count }
        assertEquals(9, totalNmrWorkers) // 2 + 6 + 1

        // Compute total Rate work liability
        val totalRatePayout = laborEntries
            .filter { it.payoutClass == "Rate" }
            .sumOf { it.totalPayout ?: 0.0 }
        assertEquals(14400.0, totalRatePayout, 0.001)

        // Compute daily labor wage for NMR (assuming standard rates: Mason ₹900, Helper ₹600, Supervisor ₹1200)
        val wageMap = mapOf("Mason" to 900.0, "Helper" to 600.0, "Supervisor" to 1200.0)
        val nmrWageLiability = nmrEntry.workerCounts.sumOf {
            it.count * (wageMap[it.workerType] ?: 500.0)
        }
        // (2 * 900) + (6 * 600) + (1 * 1200) = 1800 + 3600 + 1200 = 6600
        assertEquals(6600.0, nmrWageLiability, 0.001)

        val totalDailyLaborCost = totalRatePayout + nmrWageLiability
        assertEquals(21000.0, totalDailyLaborCost, 0.001)
    }

    @Test
    fun testMaterialConsumption_inventoryDepletion() {
        val initialStock = mutableMapOf(
            "mat-cement" to ProjectMaterialDto(id = "mat-cement", projectId = "p-1", name = "OPC 53 Cement", quantity = 500.0, unit = "bags"),
            "mat-sand" to ProjectMaterialDto(id = "mat-sand", projectId = "p-1", name = "River Sand", quantity = 1000.0, unit = "cft")
        )

        val consumedMaterials = listOf(
            MaterialConsumedInput(projectMaterialId = "mat-cement", materialName = "OPC 53 Cement", quantityConsumed = 85.0, unit = "bags"),
            MaterialConsumedInput(projectMaterialId = "mat-sand", materialName = "River Sand", quantityConsumed = 210.0, unit = "cft")
        )

        // Deplete stock
        for (consumption in consumedMaterials) {
            val stock = initialStock[consumption.projectMaterialId]
            assertNotNull(stock)
            assertTrue("Sufficient stock should be available", stock!!.quantity >= consumption.quantityConsumed)
            initialStock[consumption.projectMaterialId!!] = stock.copy(
                quantity = stock.quantity - consumption.quantityConsumed
            )
        }

        assertEquals(415.0, initialStock["mat-cement"]!!.quantity, 0.001) // 500 - 85
        assertEquals(790.0, initialStock["mat-sand"]!!.quantity, 0.001)   // 1000 - 210
    }

    @Test
    fun testPurchaseOrderApproval_andPouchTransportExpense() {
        // Step 1: PO created in Draft
        val po = PurchaseOrderDto(
            id = "po-201",
            projectId = "p-1",
            supplierName = "Apex Ready Mix Concrete",
            status = "Draft",
            totalAmount = 85000.0,
            items = listOf(PurchaseOrderItemDto("M25 Concrete Transit Mix", 15.0, 5666.67, "m3"))
        )
        assertEquals("Draft", po.status)

        // Step 2: PO Approved
        val approvedPo = po.copy(status = "Approved")
        assertEquals("Approved", approvedPo.status)

        // Step 3: Site Engineer pays toll and truck unloading charges from Petty Cash
        val pouchBalanceBefore = 10000.0
        val unloadExpense = PouchTransactionDto(
            projectId = "p-1",
            amount = 1500.0,
            category = "Transport & Auto",
            description = "RMC pump truck site access & toll charge",
            transactionType = "Expense",
            date = "2026-11-01"
        )

        val pouchBalanceAfter = pouchBalanceBefore - unloadExpense.amount
        assertEquals(8500.0, pouchBalanceAfter, 0.001)
    }

    @Test
    fun testContractorDualLedgerReconciliation() {
        val contractors = listOf(
            ContractorDto("c1", "Velu Masonry", trade = "Masonry", payoutType = "Rate", defaultRate = 20.0),
            ContractorDto("c2", "Kumar Earthmovers", trade = "Excavation", payoutType = "Rate", defaultRate = 1200.0),
            ContractorDto("c3", "Chinna Labour Gang", trade = "Civil", payoutType = "NMR", defaultRate = 600.0),
            ContractorDto("c4", "Shankar Helpers", trade = "Civil", payoutType = "NMR", defaultRate = 550.0)
        )

        val rateLedger = contractors.filter { it.payoutType.equals("Rate", ignoreCase = true) }
        val nmrLedger = contractors.filter { it.payoutType.equals("NMR", ignoreCase = true) }

        assertEquals(2, rateLedger.size)
        assertEquals(2, nmrLedger.size)

        // Generate Rate Payout Run
        val rateRun = PayoutRunDto(
            id = "pr-1",
            projectId = "p-1",
            contractorName = rateLedger[0].name,
            totalAmount = 45000.0,
            payoutClass = "Rate",
            status = "Pending",
            runDate = "2026-11-01"
        )
        assertEquals("Rate", rateRun.payoutClass)
        assertEquals("Pending", rateRun.status)

        // Approve Payout Run
        val approvedRateRun = rateRun.copy(status = "Approved")
        assertEquals("Approved", approvedRateRun.status)
    }
}
