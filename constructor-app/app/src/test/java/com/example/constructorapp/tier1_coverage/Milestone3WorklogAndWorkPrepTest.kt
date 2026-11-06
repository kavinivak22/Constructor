package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.model.workprep.CallStatus
import com.example.constructorapp.data.model.worklog.WorkerCountInput
import com.example.constructorapp.data.model.worklog.WorklogLaborInput
import com.example.constructorapp.data.repository.WorkPrepRepository
import com.example.constructorapp.util.ConcreteMixCalculator
import com.example.constructorapp.util.ConstructionActivity
import com.example.constructorapp.util.MixGrade
import org.junit.Assert.*
import org.junit.Test

class Milestone3WorklogAndWorkPrepTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. READINESS SCORE FORMULA TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testReadinessScore_zeroTasks_returnsZero() {
        // When 0 tasks are planned, task weight is 0. Total defaults without tasks.
        val score = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = 0,
            assignedContractorTaskCount = 0,
            totalContactsCount = 0,
            confirmedContactsCount = 0,
            criticalDeficitMaterialCount = 0
        )
        // taskWeight = 0, contractorAssignedWeight = 20, contactConfirmationWeight = 30, materialScore = 20
        // Total = 70 (or with 0 tasks, taskWeight is 0)
        assertEquals(70, score)
    }

    @Test
    fun testReadinessScore_perfectReadiness_returns100() {
        val score = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = 4,
            assignedContractorTaskCount = 4,
            totalContactsCount = 3,
            confirmedContactsCount = 3,
            criticalDeficitMaterialCount = 0
        )
        // taskWeight: 30
        // contractorWeight: (4/4) * 20 = 20
        // contactWeight: (3/3) * 30 = 30
        // materialScore: 20 - 0 = 20
        // Total: 30 + 20 + 30 + 20 = 100
        assertEquals(100, score)
    }

    @Test
    fun testReadinessScore_partialContractorsAndShortage() {
        val score = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = 2,
            assignedContractorTaskCount = 1, // 50% = 10 pts
            totalContactsCount = 2,
            confirmedContactsCount = 1,      // 50% = 15 pts
            criticalDeficitMaterialCount = 1 // deduction = 15 -> materialScore = 5 pts
        )
        // taskWeight: 30
        // contractorWeight: 10
        // contactWeight: 15
        // materialScore: 5
        // Total = 30 + 10 + 15 + 5 = 60
        assertEquals(60, score)
    }

    @Test
    fun testReadinessScore_severeMaterialDeficits_floorsMaterialScoreAtZero() {
        val score = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = 2,
            assignedContractorTaskCount = 2, // 20 pts
            totalContactsCount = 2,
            confirmedContactsCount = 2,      // 30 pts
            criticalDeficitMaterialCount = 3 // 3 * 15 = 45 deduction -> materialScore = max(0, 20 - 45) = 0
        )
        // taskWeight: 30
        // contractorWeight: 20
        // contactWeight: 30
        // materialScore: 0
        // Total = 30 + 20 + 30 + 0 = 80
        assertEquals(80, score)
    }

    @Test
    fun testReadinessScore_unassignedTasksAndNoConfirmations() {
        val score = WorkPrepRepository.calculateReadinessScore(
            selectedTaskCount = 5,
            assignedContractorTaskCount = 0, // 0 pts
            totalContactsCount = 4,
            confirmedContactsCount = 0,      // 0 pts
            criticalDeficitMaterialCount = 2 // 2 * 15 = 30 -> materialScore = 0
        )
        // taskWeight: 30
        // contractorWeight: 0
        // contactWeight: 0
        // materialScore: 0
        // Total = 30
        assertEquals(30, score)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CONCRETE & MORTAR MIX RATIO CALCULATOR TESTS (IS 456)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testConcreteMixCalculator_m20Standard() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.CONCRETE,
            mixGrade = MixGrade.M20,
            cementBags = 10.0
        )

        // M20 ratio: 1 : 1.5 : 3.0
        // Cement: 10 Bags
        // Sand CFT: 10 * 1.5 * 1.25 = 18.75 -> 18.8 CFT
        // Gravel CFT: 10 * 3.0 * 1.25 = 37.5 CFT
        assertEquals(3, result.size)
        assertEquals("53 Grade Cement", result[0].materialName)
        assertEquals(10.0, result[0].quantity, 0.001)
        assertEquals("Bags", result[0].unit)

        assertEquals("M-Sand (Fine Aggregate)", result[1].materialName)
        assertEquals(18.8, result[1].quantity, 0.05)
        assertEquals("CFT", result[1].unit)

        assertEquals("20mm Blue Metal (Coarse Aggregate)", result[2].materialName)
        assertEquals(37.5, result[2].quantity, 0.05)
        assertEquals("CFT", result[2].unit)
    }

    @Test
    fun testConcreteMixCalculator_m25HeavyStructural() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.CONCRETE,
            mixGrade = MixGrade.M25,
            cementBags = 20.0
        )

        // M25 ratio: 1 : 1 : 2
        // Cement: 20 Bags
        // Sand: 20 * 1.0 * 1.25 = 25.0 CFT
        // Gravel: 20 * 2.0 * 1.25 = 50.0 CFT
        assertEquals(3, result.size)
        assertEquals(20.0, result[0].quantity, 0.001)
        assertEquals(25.0, result[1].quantity, 0.01)
        assertEquals(50.0, result[2].quantity, 0.01)
    }

    @Test
    fun testConcreteMixCalculator_m15FootingPCC() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.CONCRETE,
            mixGrade = MixGrade.M15,
            cementBags = 8.0
        )

        // M15 ratio: 1 : 2 : 4
        // Sand: 8 * 2.0 * 1.25 = 20.0 CFT
        // Gravel: 8 * 4.0 * 1.25 = 40.0 CFT
        assertEquals(20.0, result[1].quantity, 0.01)
        assertEquals(40.0, result[2].quantity, 0.01)
    }

    @Test
    fun testMortarMixCalculator_brickwork1to4() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.MASONRY,
            mixGrade = MixGrade.MORTAR_1_4,
            cementBags = 5.0
        )

        // Mortar 1:4:
        // Cement: 5 Bags
        // P-Sand: 5 * 4.0 * 1.25 = 25.0 CFT
        // Gravel: 0 (No coarse aggregate in masonry mortar)
        assertEquals(2, result.size)
        assertEquals("P-Sand (Masonry Sand)", result[1].materialName)
        assertEquals(25.0, result[1].quantity, 0.01)
    }

    @Test
    fun testMortarMixCalculator_plastering1to6() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.PLASTERING,
            mixGrade = MixGrade.MORTAR_1_6,
            cementBags = 4.0
        )

        // Mortar 1:6:
        // Cement: 4 Bags
        // Plastering Sand: 4 * 6.0 * 1.25 = 30.0 CFT
        assertEquals(2, result.size)
        assertEquals("Plastering Sand (Fine)", result[1].materialName)
        assertEquals(30.0, result[1].quantity, 0.01)
    }

    @Test
    fun testConcreteMixCalculator_zeroBagsReturnsEmpty() {
        val result = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.CONCRETE,
            mixGrade = MixGrade.M20,
            cementBags = 0.0
        )
        assertTrue(result.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. LABOR PAYOUT & WORKER HEADCOUNT LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testLaborEntry_rateWorkPayoutCalculation() {
        val qty = 1200.0 // 1200 sq.ft brickwork
        val rate = 35.0  // ₹35 per sq.ft
        val totalPayout = qty * rate

        val entry = WorklogLaborInput(
            contractorName = "Murugan Masonry Gang",
            category = "Masonry",
            payoutClass = "Rate",
            workType = "Brickwork",
            workDoneQuantity = qty,
            workDoneUnit = "sq.ft",
            payoutRate = rate,
            totalPayout = totalPayout
        )

        assertEquals(42000.0, entry.totalPayout!!, 0.001)
        assertEquals("Rate", entry.payoutClass)
    }

    @Test
    fun testLaborEntry_nmrWorkerHeadcountSum() {
        val workers = listOf(
            WorkerCountInput("Mason", 3),
            WorkerCountInput("Helper", 4),
            WorkerCountInput("Carpenter", 2),
            WorkerCountInput("Electrician", 0)
        )

        val totalHeadcount = workers.sumOf { it.count }
        assertEquals(9, totalHeadcount)

        val activeWorkers = workers.filter { it.count > 0 }
        assertEquals(3, activeWorkers.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CALL STATUS ENUM & PARSING TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testCallStatusEnum_parsingAndFallback() {
        assertEquals(CallStatus.PENDING, CallStatus.fromKey("pending"))
        assertEquals(CallStatus.CALLED, CallStatus.fromKey("called"))
        assertEquals(CallStatus.CONFIRMED, CallStatus.fromKey("confirmed"))
        assertEquals(CallStatus.NO_ANSWER, CallStatus.fromKey("no-answer"))

        // Case insensitivity
        assertEquals(CallStatus.CONFIRMED, CallStatus.fromKey("CONFIRMED"))
        assertEquals(CallStatus.NO_ANSWER, CallStatus.fromKey("No-Answer"))

        // Unknown fallback
        assertEquals(CallStatus.PENDING, CallStatus.fromKey("unknown-status"))
    }
}
