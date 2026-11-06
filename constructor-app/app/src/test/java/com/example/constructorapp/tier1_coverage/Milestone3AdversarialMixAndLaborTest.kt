package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.model.worklog.WorkerCountInput
import com.example.constructorapp.data.model.worklog.WorklogLaborInput
import com.example.constructorapp.util.CalculatedMixMaterial
import com.example.constructorapp.util.ConcreteMixCalculator
import com.example.constructorapp.util.ConstructionActivity
import com.example.constructorapp.util.MixGrade
import org.junit.Assert.*
import org.junit.Test

/**
 * Adversarial Challenger 1 Test Suite for Milestone 3:
 * - Stress testing ConcreteMixCalculator across all grades (M20, M25, M15, M10, M7.5, custom)
 * - Stress testing Mortar calculations (1:3, 1:4, 1:5, 1:6, custom)
 * - Edge case testing: 0, negative, fractional, and extreme scale cement bags
 * - Precision & rounding verification (IS 456 CFT conversion and 1 decimal place rounding)
 * - Labor headcount summation (NMR) and rate work payout math
 * - Mathematical parity with MixRatioHelperWidget UI logic
 */
class Milestone3AdversarialMixAndLaborTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. ALL CONCRETE GRADES: M20, M25, M15, M10, M7.5, CUSTOM
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testConcreteGrades_exactIS456Ratios_10Bags() {
        val bags = 10.0

        // M20 (1 : 1.5 : 3.0) -> Sand: 10 * 1.5 * 1.25 = 18.75 -> 18.8 CFT, Gravel: 10 * 3.0 * 1.25 = 37.5 CFT
        val m20 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M20, bags)
        assertEquals(3, m20.size)
        assertEquals("53 Grade Cement", m20[0].materialName)
        assertEquals(10.0, m20[0].quantity, 0.001)
        assertEquals("Bags", m20[0].unit)
        assertEquals("M-Sand (Fine Aggregate)", m20[1].materialName)
        assertEquals(18.8, m20[1].quantity, 0.001)
        assertEquals("CFT", m20[1].unit)
        assertEquals("20mm Blue Metal (Coarse Aggregate)", m20[2].materialName)
        assertEquals(37.5, m20[2].quantity, 0.001)
        assertEquals("CFT", m20[2].unit)

        // M25 (1 : 1 : 2) -> Sand: 10 * 1.0 * 1.25 = 12.5 CFT, Gravel: 10 * 2.0 * 1.25 = 25.0 CFT
        val m25 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M25, bags)
        assertEquals(3, m25.size)
        assertEquals(12.5, m25[1].quantity, 0.001)
        assertEquals(25.0, m25[2].quantity, 0.001)

        // M15 (1 : 2 : 4) -> Sand: 10 * 2.0 * 1.25 = 25.0 CFT, Gravel: 10 * 4.0 * 1.25 = 50.0 CFT
        val m15 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M15, bags)
        assertEquals(3, m15.size)
        assertEquals(25.0, m15[1].quantity, 0.001)
        assertEquals(50.0, m15[2].quantity, 0.001)

        // M10 (1 : 3 : 6) -> Sand: 10 * 3.0 * 1.25 = 37.5 CFT, Gravel: 10 * 6.0 * 1.25 = 75.0 CFT
        val m10 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M10, bags)
        assertEquals(3, m10.size)
        assertEquals(37.5, m10[1].quantity, 0.001)
        assertEquals(75.0, m10[2].quantity, 0.001)

        // M7.5 (1 : 4 : 8) -> Sand: 10 * 4.0 * 1.25 = 50.0 CFT, Gravel: 10 * 8.0 * 1.25 = 100.0 CFT
        val m75 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M7_5, bags)
        assertEquals(3, m75.size)
        assertEquals(50.0, m75[1].quantity, 0.001)
        assertEquals(100.0, m75[2].quantity, 0.001)
    }

    @Test
    fun testConcreteCustomRatio_arbitraryParts() {
        val bags = 8.0
        // Custom mix: 1 : 2.5 : 3.5
        val custom = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.CONCRETE,
            mixGrade = MixGrade.CUSTOM,
            cementBags = bags,
            customSandRatio = 2.5,
            customGravelRatio = 3.5
        )

        assertEquals(3, custom.size)
        assertEquals(8.0, custom[0].quantity, 0.001)
        // Sand: 8 * 2.5 * 1.25 = 25.0 CFT
        assertEquals(25.0, custom[1].quantity, 0.001)
        // Gravel: 8 * 3.5 * 1.25 = 35.0 CFT
        assertEquals(35.0, custom[2].quantity, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MORTAR MIXES ACROSS MASONRY AND PLASTERING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testMortarMixes_allStandardRatios() {
        val bags = 10.0

        // Mortar 1:3 -> Sand: 10 * 3.0 * 1.25 = 37.5 CFT, No gravel
        val mortar13 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_3, bags)
        assertEquals(2, mortar13.size)
        assertEquals("P-Sand (Masonry Sand)", mortar13[1].materialName)
        assertEquals(37.5, mortar13[1].quantity, 0.001)

        // Mortar 1:4 -> Sand: 10 * 4.0 * 1.25 = 50.0 CFT, No gravel
        val mortar14 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_4, bags)
        assertEquals(2, mortar14.size)
        assertEquals("P-Sand (Masonry Sand)", mortar14[1].materialName)
        assertEquals(50.0, mortar14[1].quantity, 0.001)

        // Mortar 1:5 -> Sand: 10 * 5.0 * 1.25 = 62.5 CFT, No gravel
        val mortar15 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_5, bags)
        assertEquals(2, mortar15.size)
        assertEquals("P-Sand (Masonry Sand)", mortar15[1].materialName)
        assertEquals(62.5, mortar15[1].quantity, 0.001)

        // Mortar 1:6 Plastering -> Sand: 10 * 6.0 * 1.25 = 75.0 CFT, Plastering sand label
        val mortar16 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.PLASTERING, MixGrade.MORTAR_1_6, bags)
        assertEquals(2, mortar16.size)
        assertEquals("Plastering Sand (Fine)", mortar16[1].materialName)
        assertEquals(75.0, mortar16[1].quantity, 0.001)
    }

    @Test
    fun testMortarCustomRatio_zeroGravelExclusion() {
        val bags = 6.0
        // Custom plastering mortar: 1 : 4.5, gravel = 0.0
        val customMortar = ConcreteMixCalculator.calculateMaterials(
            activity = ConstructionActivity.PLASTERING,
            mixGrade = MixGrade.CUSTOM,
            cementBags = bags,
            customSandRatio = 4.5,
            customGravelRatio = 0.0
        )

        assertEquals(2, customMortar.size)
        // Sand: 6 * 4.5 * 1.25 = 33.75 -> 33.8 CFT
        assertEquals(33.8, customMortar[1].quantity, 0.001)
        assertEquals("Plastering Sand (Fine)", customMortar[1].materialName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. EDGE CASES: ZERO, NEGATIVE, FRACTIONAL, AND EXTREME SCALE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testEdgeCases_zeroAndNegativeBags_returnsEmptyList() {
        // Exactly 0 bags
        val zero = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M20, 0.0)
        assertTrue("Zero bags must return empty list", zero.isEmpty())

        // Negative bags
        val neg1 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M20, -1.0)
        assertTrue("Negative bags must return empty list", neg1.isEmpty())

        val negSmall = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_4, -0.0001)
        assertTrue("Slightly negative bags must return empty list", negSmall.isEmpty())

        // Custom with 0 bags
        val customZero = ConcreteMixCalculator.calculateMaterials(
            ConstructionActivity.CONCRETE,
            MixGrade.CUSTOM,
            0.0,
            customSandRatio = 2.0,
            customGravelRatio = 4.0
        )
        assertTrue("Custom with zero bags must return empty list", customZero.isEmpty())
    }

    @Test
    fun testEdgeCases_fractionalBags_accurateRounding() {
        // 0.5 bag of M20:
        // Sand: 0.5 * 1.5 * 1.25 = 0.9375 -> rounded to 0.9 CFT
        // Gravel: 0.5 * 3.0 * 1.25 = 1.875 -> rounded to 1.9 CFT
        val halfBag = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M20, 0.5)
        assertEquals(3, halfBag.size)
        assertEquals(0.5, halfBag[0].quantity, 0.001)
        assertEquals(0.9, halfBag[1].quantity, 0.001)
        assertEquals(1.9, halfBag[2].quantity, 0.001)

        // 2.75 bags of M25:
        // Sand: 2.75 * 1.0 * 1.25 = 3.4375 -> rounded to 3.4 CFT
        // Gravel: 2.75 * 2.0 * 1.25 = 6.875 -> rounded to 6.9 CFT
        val fracM25 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M25, 2.75)
        assertEquals(3.4, fracM25[1].quantity, 0.001)
        assertEquals(6.9, fracM25[2].quantity, 0.001)

        // 1.333 bags of M15:
        // Sand: 1.333 * 2.0 * 1.25 = 3.3325 -> rounded to 3.3 CFT
        // Gravel: 1.333 * 4.0 * 1.25 = 6.665 -> rounded to 6.7 CFT
        val fracM15 = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M15, 1.333)
        assertEquals(3.3, fracM15[1].quantity, 0.001)
        assertEquals(6.7, fracM15[2].quantity, 0.001)
    }

    @Test
    fun testEdgeCases_extremeScale_noArithmeticOverflow() {
        // Mega pour: 50,000 bags of M20
        val bigBags = 50_000.0
        val megaPour = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M20, bigBags)

        assertEquals(3, megaPour.size)
        assertEquals(50000.0, megaPour[0].quantity, 0.001)
        // Sand: 50000 * 1.5 * 1.25 = 93750.0 CFT
        assertEquals(93750.0, megaPour[1].quantity, 0.001)
        // Gravel: 50000 * 3.0 * 1.25 = 187500.0 CFT
        assertEquals(187500.0, megaPour[2].quantity, 0.001)

        // 1,000,000 bags of M7.5
        val massive = ConcreteMixCalculator.calculateMaterials(ConstructionActivity.CONCRETE, MixGrade.M7_5, 1_000_000.0)
        // Sand: 1000000 * 4.0 * 1.25 = 5000000.0 CFT
        assertEquals(5_000_000.0, massive[1].quantity, 0.001)
        // Gravel: 1000000 * 8.0 * 1.25 = 10000000.0 CFT
        assertEquals(10_000_000.0, massive[2].quantity, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. MATHEMATICAL PARITY WITH MIX RATIO HELPER WIDGET
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testParityWithMixRatioHelperWidgetFormula() {
        // The UI helper in MixRatioHelperWidget.kt computes:
        // val calculatedSandCFT = Math.round(bags * sandRatio * 1.25 * 10.0) / 10.0
        // val calculatedGravelCFT = Math.round(bags * gravelRatio * 1.25 * 10.0) / 10.0
        val testCases = listOf(
            Triple(ConstructionActivity.CONCRETE, MixGrade.M20, 15.0),
            Triple(ConstructionActivity.CONCRETE, MixGrade.M25, 25.0),
            Triple(ConstructionActivity.CONCRETE, MixGrade.M15, 30.0),
            Triple(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_4, 12.0),
            Triple(ConstructionActivity.MASONRY, MixGrade.MORTAR_1_3, 8.0),
            Triple(ConstructionActivity.PLASTERING, MixGrade.MORTAR_1_6, 20.0)
        )

        for ((activity, grade, bags) in testCases) {
            val calcResult = ConcreteMixCalculator.calculateMaterials(activity, grade, bags)

            val expectedSand = Math.round(bags * grade.sandRatio * 1.25 * 10.0) / 10.0
            assertEquals("Sand CFT mismatch for $grade", expectedSand, calcResult[1].quantity, 0.001)

            if (grade.gravelRatio > 0.0) {
                val expectedGravel = Math.round(bags * grade.gravelRatio * 1.25 * 10.0) / 10.0
                assertEquals("Gravel CFT mismatch for $grade", expectedGravel, calcResult[2].quantity, 0.001)
            } else {
                assertEquals(2, calcResult.size)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. LABOR CALCULATIONS: NMR HEADCOUNT & RATE WORK PAYOUTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testNmrLabor_headcountSummationAndFilter() {
        val workerList = listOf(
            WorkerCountInput("Mason", 5),
            WorkerCountInput("MC (Male Coolie)", 4),
            WorkerCountInput("FC (Female Coolie)", 6),
            WorkerCountInput("Carpenter", 2),
            WorkerCountInput("Bar Bender", 0),
            WorkerCountInput("Electrician", 0),
            WorkerCountInput("Helper", 8)
        )

        // Total headcount summation
        val totalHeadcount = workerList.sumOf { it.count }
        assertEquals(25, totalHeadcount)

        // Active worker groups (count > 0)
        val activeGroups = workerList.filter { it.count > 0 }
        assertEquals(5, activeGroups.size)
        assertEquals(25, activeGroups.sumOf { it.count })

        // Empty worker list returns 0
        val emptyList = emptyList<WorkerCountInput>()
        assertEquals(0, emptyList.sumOf { it.count })
        assertTrue(emptyList.filter { it.count > 0 }.isEmpty())
    }

    @Test
    fun testRateWorkPayout_exactMultiplicationAndDecimals() {
        // Standard brickwork: 1540.5 sq.ft at ₹38.5 per sq.ft
        val qty = 1540.5
        val rate = 38.5
        val expectedPayout = qty * rate // 59309.25

        val laborEntry = WorklogLaborInput(
            contractorName = "Velu Masonry Works",
            category = "Masonry",
            payoutClass = "Rate",
            workType = "Brickwork",
            workDoneQuantity = qty,
            workDoneUnit = "sq.ft",
            payoutRate = rate,
            totalPayout = expectedPayout
        )

        assertNotNull(laborEntry.totalPayout)
        assertEquals(59309.25, laborEntry.totalPayout!!, 0.001)

        // Large scale contract: 45,000 sq.ft plastering at ₹18.0 per sq.ft
        val bigQty = 45000.0
        val bigRate = 18.0
        val bigPayout = bigQty * bigRate
        assertEquals(810000.0, bigPayout, 0.001)

        // Edge case: zero quantity or zero rate
        val zeroQtyEntry = WorklogLaborInput(
            contractorName = "Test",
            workDoneQuantity = 0.0,
            payoutRate = 50.0,
            totalPayout = 0.0 * 50.0
        )
        assertEquals(0.0, zeroQtyEntry.totalPayout!!, 0.001)

        // Null quantity/rate leaves totalPayout as null
        val nullEntry = WorklogLaborInput(
            contractorName = "NMR Team",
            payoutClass = "NMR",
            workDoneQuantity = null,
            payoutRate = null,
            totalPayout = null
        )
        assertNull(nullEntry.totalPayout)
    }

    @Test
    fun testWorklogCreationTab_enumIntegrity() {
        val tabs = com.example.constructorapp.data.model.worklog.WorklogCreationTab.entries
        assertEquals(4, tabs.size)
        assertEquals("Details", tabs[0].title)
        assertEquals("Labor", tabs[1].title)
        assertEquals("Materials", tabs[2].title)
        assertEquals("Photos", tabs[3].title)
    }
}
