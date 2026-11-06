package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.remote.ContractorDto
import com.example.constructorapp.data.remote.LaborEntryDto
import com.example.constructorapp.data.remote.PayoutRunDto
import com.example.constructorapp.data.remote.WorkerCountDto
import org.junit.Assert.*
import org.junit.Test

class ContractorPaydayModelTest {

    @Test
    fun testContractorClassification_rateVsNmr() {
        val rateContractor = ContractorDto(
            id = "c-1",
            name = "Ramesh Kumar Masonry",
            trade = "Masonry",
            payoutType = "Rate",
            defaultRate = 18.0
        )

        val nmrContractor = ContractorDto(
            id = "c-2",
            name = "Murugan Daily Labour Gang",
            trade = "Civil",
            payoutType = "NMR",
            defaultRate = 650.0 // Daily wage
        )

        assertTrue(rateContractor.payoutType.equals("Rate", ignoreCase = true))
        assertFalse(rateContractor.payoutType.equals("NMR", ignoreCase = true))

        assertTrue(nmrContractor.payoutType.equals("NMR", ignoreCase = true))
        assertFalse(nmrContractor.payoutType.equals("Rate", ignoreCase = true))
    }

    @Test
    fun testRateWorkPayoutCalculation() {
        val laborEntry = LaborEntryDto(
            worklogId = "wl-100",
            contractorName = "Ramesh Kumar",
            payoutClass = "Rate",
            workType = "Brickwork 9 inch",
            workDoneQuantity = 2500.0,
            workDoneUnit = "sq.ft",
            payoutRate = 22.0,
            totalPayout = 2500.0 * 22.0
        )

        assertEquals("Rate", laborEntry.payoutClass)
        assertEquals(55000.0, laborEntry.totalPayout!!, 0.001)
    }

    @Test
    fun testNmrLaborWorkerCountAggregation() {
        val laborEntryId = "le-200"
        val workerCounts = listOf(
            WorkerCountDto(laborEntryId = laborEntryId, workerType = "Mason", count = 4),
            WorkerCountDto(laborEntryId = laborEntryId, workerType = "Helper", count = 6),
            WorkerCountDto(laborEntryId = laborEntryId, workerType = "Supervisor", count = 1)
        )

        val totalWorkers = workerCounts.sumOf { it.count }
        assertEquals(11, totalWorkers)

        val masons = workerCounts.find { it.workerType == "Mason" }?.count ?: 0
        val helpers = workerCounts.find { it.workerType == "Helper" }?.count ?: 0
        assertEquals(4, masons)
        assertEquals(6, helpers)
    }

    @Test
    fun testPayoutRun_statusTransition() {
        val run = PayoutRunDto(
            id = "run-1",
            projectId = "p-1",
            contractorName = "Ramesh Kumar",
            totalAmount = 55000.0,
            payoutClass = "Rate",
            status = "Pending",
            runDate = "2026-10-31"
        )

        assertEquals("Pending", run.status)
        val approvedRun = run.copy(status = "Approved")
        assertEquals("Approved", approvedRun.status)
        assertEquals(run.totalAmount, approvedRun.totalAmount, 0.001)
    }

    @Test
    fun testPayoutRun_weeklyAggregation() {
        val contractorRuns = listOf(
            PayoutRunDto(projectId = "p-1", contractorName = "Contractor A", totalAmount = 25000.0, payoutClass = "Rate", runDate = "2026-10-31"),
            PayoutRunDto(projectId = "p-1", contractorName = "Contractor B", totalAmount = 18000.0, payoutClass = "NMR", runDate = "2026-10-31"),
            PayoutRunDto(projectId = "p-1", contractorName = "Contractor C", totalAmount = 32000.0, payoutClass = "Rate", runDate = "2026-10-31")
        )

        val totalPayout = contractorRuns.sumOf { it.totalAmount }
        val ratePayout = contractorRuns.filter { it.payoutClass == "Rate" }.sumOf { it.totalAmount }
        val nmrPayout = contractorRuns.filter { it.payoutClass == "NMR" }.sumOf { it.totalAmount }

        assertEquals(75000.0, totalPayout, 0.001)
        assertEquals(57000.0, ratePayout, 0.001)
        assertEquals(18000.0, nmrPayout, 0.001)
    }
}
