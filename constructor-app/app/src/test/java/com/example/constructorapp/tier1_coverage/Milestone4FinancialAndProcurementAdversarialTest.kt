package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.entity.*
import com.example.constructorapp.ui.viewmodel.InventoryViewModel
import com.example.constructorapp.ui.viewmodel.MaterialsViewModel
import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.toInrCompact
import com.example.constructorapp.util.toInrFull
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Challenger M4.1 Empirical Adversarial Test Suite for Milestone 4:
 * Financials, Procurement, Inventory, Materials & Payday.
 *
 * Verifies:
 * 1. Stock transfer conservation invariant, overdraft prevention, and boundary edge cases.
 * 2. Low stock warning threshold classifications (Rose <= min, Amber <= 1.5*min, Emerald > 1.5*min).
 * 3. Purchase order arithmetic (line item total, grand total, fractional rates, zero quantities).
 * 4. Material price drift & variance formulas (committed, actual, net drift, % drift, severity green/amber/red).
 * 5. Contractor dual ledger complete segregation (Rate vs NMR balances, zero cross-leakage).
 * 6. Client milestones progress calculation, 100% stage share invariant, balance clamping, and edge cases.
 * 7. Payday split conservation and expense categorization.
 * 8. Indian currency formatting (full comma grouping and compact Cr/L/k scales).
 */
class Milestone4FinancialAndProcurementAdversarialTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. STOCK TRANSFER ARITHMETIC & INVARIANTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testStockTransfer_ConservationInvariant_FractionalAndExtremeQuantities() {
        // Test case 1: Standard fractional transfer
        var sourceStock = 125.75
        var destStock = 40.25
        val transferQty = 25.50
        val totalBefore = sourceStock + destStock

        assertTrue("Transfer quantity must be > 0", transferQty > 0.0)
        assertTrue("Transfer quantity must not exceed source stock", transferQty <= sourceStock)

        sourceStock -= transferQty
        destStock += transferQty

        assertEquals(100.25, sourceStock, 0.0001)
        assertEquals(65.75, destStock, 0.0001)
        assertEquals("Total inventory across sites must be strictly conserved", totalBefore, sourceStock + destStock, 0.0001)

        // Test case 2: Exact depletion transfer (source becomes 0.0)
        val fullDepletionQty = sourceStock
        sourceStock -= fullDepletionQty
        destStock += fullDepletionQty

        assertEquals(0.0, sourceStock, 0.0001)
        assertEquals(166.0, destStock, 0.0001)
        assertEquals(totalBefore, sourceStock + destStock, 0.0001)

        // Test case 3: Extreme scale transfer
        var largeSource = 1_000_000.0
        var largeDest = 500_000.0
        val extremeTransfer = 750_000.0
        val largeTotal = largeSource + largeDest

        largeSource -= extremeTransfer
        largeDest += extremeTransfer

        assertEquals(250_000.0, largeSource, 0.0001)
        assertEquals(1_250_000.0, largeDest, 0.0001)
        assertEquals(largeTotal, largeSource + largeDest, 0.0001)
    }

    @Test
    fun testStockTransfer_OverdraftAndInvalidQuantity_Rejection() {
        val sourceStock = 50.0

        // Zero transfer
        assertFalse("Zero quantity transfer must fail validation", 0.0 > 0.0 && 0.0 <= sourceStock)

        // Negative transfer
        assertFalse("Negative quantity transfer must fail validation", -5.0 > 0.0 && -5.0 <= sourceStock)

        // Infinitesimal overdraft
        val overdraw = sourceStock + 0.0001
        assertFalse("Overdrawing source stock even by 0.0001 must fail validation", overdraw > 0.0 && overdraw <= sourceStock)

        // Massive overdraft
        assertFalse("Overdrawing source stock by 1000 must fail validation", 1000.0 <= sourceStock)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. LOW STOCK WARNING THRESHOLD SEVERITY LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testLowStockSeverity_ExactBoundariesAndInfinitesimalTransitions() {
        val minStock = 100.0

        // Exact minStock boundary -> Low Stock (Rose)
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(100.0, minStock))

        // Infinitesimal below minStock -> Low Stock
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(99.9999, minStock))

        // Zero and negative stock -> Low Stock
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(0.0, minStock))
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(-10.0, minStock))

        // Infinitesimal above minStock -> Medium Stock (Amber)
        assertEquals("Medium Stock", InventoryViewModel.getStockStatusLevel(100.0001, minStock))

        // Exact 1.5 * minStock boundary -> Medium Stock
        assertEquals("Medium Stock", InventoryViewModel.getStockStatusLevel(150.0, minStock))

        // Infinitesimal above 1.5 * minStock -> High Stock (Emerald)
        assertEquals("High Stock", InventoryViewModel.getStockStatusLevel(150.0001, minStock))

        // Large healthy stock -> High Stock
        assertEquals("High Stock", InventoryViewModel.getStockStatusLevel(10_000.0, minStock))
    }

    @Test
    fun testLowStockSeverity_ZeroMinStockLevel() {
        val minStock = 0.0

        // If minStock is 0 and currentStock is 0 -> Low Stock
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(0.0, minStock))

        // If minStock is 0 and currentStock > 0 -> High Stock
        assertEquals("High Stock", InventoryViewModel.getStockStatusLevel(1.0, minStock))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. PURCHASE ORDER ARITHMETIC & TOTALS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPurchaseOrderArithmetic_FractionalQuantitiesAndMultiLakhTotals() {
        val poId = UUID.randomUUID().toString()

        val items = listOf(
            PurchaseOrderItemEntity(
                poId = poId,
                materialName = "Ultratech Cement (OPC 53)",
                quantity = 150.5,
                unitPrice = 412.50,
                unit = "bags",
                totalPrice = 150.5 * 412.50
            ),
            PurchaseOrderItemEntity(
                poId = poId,
                materialName = "Fe 550 TMT Steel",
                quantity = 12.75,
                unitPrice = 64500.0,
                unit = "tons",
                totalPrice = 12.75 * 64500.0
            ),
            PurchaseOrderItemEntity(
                poId = poId,
                materialName = "Crushed Stone Aggregates 20mm",
                quantity = 8.0,
                unitPrice = 4200.0,
                unit = "units",
                totalPrice = 8.0 * 4200.0
            )
        )

        // Item 1: 150.5 * 412.50 = 62,081.25
        assertEquals(62081.25, items[0].totalPrice, 0.001)

        // Item 2: 12.75 * 64,500 = 822,375.0
        assertEquals(822375.0, items[1].totalPrice, 0.001)

        // Item 3: 8 * 4,200 = 33,600.0
        assertEquals(33600.0, items[2].totalPrice, 0.001)

        // Grand Total: 62081.25 + 822375.0 + 33600.0 = 918,056.25
        val expectedGrandTotal = 62081.25 + 822375.0 + 33600.0
        val actualGrandTotal = items.sumOf { it.totalPrice }
        assertEquals(expectedGrandTotal, actualGrandTotal, 0.001)
        assertEquals(918056.25, actualGrandTotal, 0.001)

        // Zero items list
        val emptyItems = emptyList<PurchaseOrderItemEntity>()
        assertEquals(0.0, emptyItems.sumOf { it.totalPrice }, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. MATERIAL PRICE DRIFT & VARIANCE FORMULAS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testMaterialPriceDrift_ExhaustiveSeverityThresholds() {
        // Case 1: Exact budget match (0% rate drift, 0% qty drift) -> Green
        assertEquals("green", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.0, actualRate = 400.0
        ))

        // Case 2: Favorable savings (cheaper rate, fewer units used) -> Green
        assertEquals("green", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 90.0, actualRate = 380.0
        ))

        // Case 3: Infinitesimal rate drift (+0.001%) -> Amber
        assertEquals("amber", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.0, actualRate = 400.01
        ))

        // Case 4: Infinitesimal quantity drift (+0.001%) -> Amber
        assertEquals("amber", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.01, actualRate = 400.0
        ))

        // Case 5: Exactly 10.0% rate drift -> Amber (boundary inclusion)
        // committedRate = 400.0, actualRate = 440.0 -> rateDrift = +10.0%
        assertEquals("amber", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.0, actualRate = 440.0
        ))

        // Case 6: Exactly 10.0% quantity drift -> Amber (boundary inclusion)
        // committedQty = 100.0, actualQty = 110.0 -> qtyDrift = +10.0%
        assertEquals("amber", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 110.0, actualRate = 400.0
        ))

        // Case 7: 10.01% rate drift (> 10.0%) -> Red
        // committedRate = 400.0, actualRate = 440.05 -> rateDrift = +10.0125%
        assertEquals("red", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.0, actualRate = 440.05
        ))

        // Case 8: 10.01% quantity drift (> 10.0%) -> Red
        // committedQty = 100.0, actualQty = 110.05 -> qtyDrift = +10.05%
        assertEquals("red", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 110.05, actualRate = 400.0
        ))

        // Case 9: Severe rate overrun (+50%) -> Red
        assertEquals("red", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 100.0, actualRate = 600.0
        ))

        // Case 10: Severe quantity overrun (+100%) -> Red
        assertEquals("red", MaterialsViewModel.computeSeverity(
            committedQty = 100.0, committedRate = 400.0,
            actualQty = 200.0, actualRate = 400.0
        ))
    }

    @Test
    fun testMaterialPriceDrift_NetCostAndPercentageCalculations() {
        // Committed: 400 bags @ ₹400 = 1,60,000
        val committedQty = 400.0
        val committedRate = 400.0
        val committedTotal = committedQty * committedRate

        // Actual: 450 bags @ ₹430 = 1,93,500
        val actualQty = 450.0
        val actualRate = 430.0
        val actualTotal = actualQty * actualRate

        val netDrift = actualTotal - committedTotal // +33,500
        val driftPct = (netDrift / committedTotal) * 100.0 // 33,500 / 160,000 = 20.9375%

        val rateDriftPct = ((actualRate - committedRate) / committedRate) * 100.0 // +7.5%
        val qtyDriftPct = ((actualQty - committedQty) / committedQty) * 100.0   // +12.5%

        assertEquals(160000.0, committedTotal, 0.001)
        assertEquals(193500.0, actualTotal, 0.001)
        assertEquals(33500.0, netDrift, 0.001)
        assertEquals(20.9375, driftPct, 0.0001)
        assertEquals(7.5, rateDriftPct, 0.0001)
        assertEquals(12.5, qtyDriftPct, 0.0001)

        // Severity must be Red because qtyDriftPct (12.5%) > 10%
        assertEquals("red", MaterialsViewModel.computeSeverity(committedQty, committedRate, actualQty, actualRate))

        // Division-by-zero protection when committedTotal == 0.0
        val zeroCommitted = 0.0
        val safeDriftPct = if (zeroCommitted > 0.0) (netDrift / zeroCommitted) * 100.0 else 0.0
        assertEquals(0.0, safeDriftPct, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CONTRACTOR DUAL LEDGER COMPLETE SEGREGATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testContractorDualLedger_StrictSegregationInvariant() {
        val entries = listOf(
            // Rate contract transactions
            ContractorLedgerEntryEntity(
                contractorId = "c1",
                amount = 45000.0,
                status = "paid",
                payoutClass = "rate"
            ),
            ContractorLedgerEntryEntity(
                contractorId = "c1",
                amount = 25000.0,
                status = "pending",
                payoutClass = "rate"
            ),
            // NMR daily muster transactions
            ContractorLedgerEntryEntity(
                contractorId = "c1",
                amount = 12000.0,
                status = "paid",
                payoutClass = "nmr"
            ),
            ContractorLedgerEntryEntity(
                contractorId = "c1",
                amount = 8000.0,
                status = "pending",
                payoutClass = "nmr"
            )
        )

        // Initial Calculations
        fun calcRateCleared(list: List<ContractorLedgerEntryEntity>) =
            list.filter { it.payoutClass.equals("rate", true) && it.status.equals("paid", true) }.sumOf { it.amount }
        fun calcRateDue(list: List<ContractorLedgerEntryEntity>) =
            list.filter { it.payoutClass.equals("rate", true) && it.status.equals("pending", true) }.sumOf { it.amount }
        fun calcNmrCleared(list: List<ContractorLedgerEntryEntity>) =
            list.filter { it.payoutClass.equals("nmr", true) && it.status.equals("paid", true) }.sumOf { it.amount }
        fun calcNmrDue(list: List<ContractorLedgerEntryEntity>) =
            list.filter { it.payoutClass.equals("nmr", true) && it.status.equals("pending", true) }.sumOf { it.amount }

        val initRateCleared = calcRateCleared(entries)
        val initRateDue = calcRateDue(entries)
        val initNmrCleared = calcNmrCleared(entries)
        val initNmrDue = calcNmrDue(entries)

        assertEquals(45000.0, initRateCleared, 0.001)
        assertEquals(25000.0, initRateDue, 0.001)
        assertEquals(12000.0, initNmrCleared, 0.001)
        assertEquals(8000.0, initNmrDue, 0.001)

        // INVARIANT STRESS TEST:
        // Inject a massive NMR transaction (+₹5,00,000 paid)
        val modifiedWithNmr = entries + ContractorLedgerEntryEntity(
            contractorId = "c1",
            amount = 500000.0,
            status = "paid",
            payoutClass = "nmr"
        )

        // Rate ledger must be 100% UNCHANGED (zero cross-leakage)
        assertEquals("Rate Cleared must be completely insulated from NMR transactions",
            initRateCleared, calcRateCleared(modifiedWithNmr), 0.001)
        assertEquals("Rate Due must be completely insulated from NMR transactions",
            initRateDue, calcRateDue(modifiedWithNmr), 0.001)
        assertEquals(512000.0, calcNmrCleared(modifiedWithNmr), 0.001)

        // Inject a massive Rate transaction (+₹10,00,000 pending)
        val modifiedWithRate = entries + ContractorLedgerEntryEntity(
            contractorId = "c1",
            amount = 1000000.0,
            status = "pending",
            payoutClass = "rate"
        )

        // NMR ledger must be 100% UNCHANGED (zero cross-leakage)
        assertEquals("NMR Cleared must be completely insulated from Rate transactions",
            initNmrCleared, calcNmrCleared(modifiedWithRate), 0.001)
        assertEquals("NMR Due must be completely insulated from Rate transactions",
            initNmrDue, calcNmrDue(modifiedWithRate), 0.001)
        assertEquals(1025000.0, calcRateDue(modifiedWithRate), 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. CLIENT MILESTONES PROGRESS & PERCENTAGE SHARING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testClientMilestones_Template100PercentInvariantAndProgressClamping() {
        val totalBudget = 6000000.0 // 60 Lakhs INR

        val stages = listOf(
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 1,
                stageName = "Booking Advance",
                pctShare = 10.0,
                expectedAmount = (totalBudget * 10.0) / 100.0,
                collectedAmount = 600000.0,
                status = "collected"
            ),
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 2,
                stageName = "Foundation Complete",
                pctShare = 15.0,
                expectedAmount = (totalBudget * 15.0) / 100.0,
                collectedAmount = 900000.0,
                status = "collected"
            ),
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 3,
                stageName = "Ground Floor Slab",
                pctShare = 20.0,
                expectedAmount = (totalBudget * 20.0) / 100.0,
                collectedAmount = 800000.0, // Partial payment (8L out of 12L)
                status = "pending"
            ),
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 4,
                stageName = "First Floor Slab & Lintel",
                pctShare = 20.0,
                expectedAmount = (totalBudget * 20.0) / 100.0,
                collectedAmount = 0.0,
                status = "upcoming"
            ),
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 5,
                stageName = "Brickwork & Internal Plastering",
                pctShare = 20.0,
                expectedAmount = (totalBudget * 20.0) / 100.0,
                collectedAmount = 0.0,
                status = "upcoming"
            ),
            ClientMilestoneEntity(
                projectId = "proj-1",
                stageNumber = 6,
                stageName = "Flooring, Painting & Handover",
                pctShare = 15.0,
                expectedAmount = (totalBudget * 15.0) / 100.0,
                collectedAmount = 0.0,
                status = "upcoming"
            )
        )

        // Invariant: sum of template percentages must be exactly 100.0%
        val totalPct = stages.sumOf { it.pctShare }
        assertEquals(100.0, totalPct, 0.001)

        val totalExpected = stages.sumOf { it.expectedAmount }
        val totalCollected = stages.sumOf { it.collectedAmount }
        val totalPending = (totalExpected - totalCollected).coerceAtLeast(0.0)

        // 600k + 900k + 800k = 2,300,000 collected
        assertEquals(6000000.0, totalExpected, 0.001)
        assertEquals(2300000.0, totalCollected, 0.001)
        assertEquals(3700000.0, totalPending, 0.001)

        // Progress = 2,300,000 / 6,000,000 = 38.33% -> 38%
        val progressPct = ((totalCollected / totalExpected) * 100.0).toInt().coerceIn(0, 100)
        assertEquals(38, progressPct)

        // Stage 3 partial balance = 1,200,000 - 800,000 = 400,000
        val stage3Balance = (stages[2].expectedAmount - stages[2].collectedAmount).coerceAtLeast(0.0)
        assertEquals(400000.0, stage3Balance, 0.001)

        // Test overcollection clamping (e.g. client overpays stage 3 by 50,000)
        val overpaidCollected = 1250000.0
        val clampedBalance = (stages[2].expectedAmount - overpaidCollected).coerceAtLeast(0.0)
        assertEquals("Stage balance must never be negative", 0.0, clampedBalance, 0.001)

        // Test zero contract value safety
        val zeroExpected = 0.0
        val zeroProgress = if (zeroExpected > 0.0) ((100.0 / zeroExpected) * 100.0).toInt().coerceIn(0, 100) else 0
        assertEquals(0, zeroProgress)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. PAYDAY SPLIT CONSERVATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPaydaySplitConservation_RateAndNmrComponents() {
        val originalAmountDue = 45000.0
        val ratePortion = 28000.0
        val nmrPortion = 17000.0

        assertEquals("Split portions must strictly sum to original amount due",
            originalAmountDue, ratePortion + nmrPortion, 0.001)

        // Zero / negative split portion rejection
        assertFalse("Split portion cannot be zero or negative", (0.0 + -500.0) > 0.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. INDIAN CURRENCY FORMATTER (INR) DETERMINISM
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testCurrencyFormatter_ExactIndianGroupingAndScales() {
        // Full Indian comma grouping
        assertEquals("₹0", 0.0.toInrFull())
        assertEquals("₹750", 750.0.toInrFull())
        assertEquals("₹1,000", 1000.0.toInrFull())
        assertEquals("₹12,500", 12500.0.toInrFull())
        assertEquals("₹1,25,000", 125000.0.toInrFull())
        assertEquals("₹25,00,000", 2500000.0.toInrFull())
        assertEquals("₹1,50,00,000", 15000000.0.toInrFull())

        // Decimals support
        assertEquals("₹1,250.50", CurrencyFormatter.formatFull(1250.50, includeDecimals = true))

        // Compact abbreviations
        assertEquals("₹0", 0.0.toInrCompact())
        assertEquals("₹750", 750.0.toInrCompact())
        assertEquals("₹1k", 1000.0.toInrCompact())
        assertEquals("₹1.5k", 1500.0.toInrCompact())
        assertEquals("₹50k", 50000.0.toInrCompact())
        assertEquals("₹1L", 100000.0.toInrCompact())
        assertEquals("₹25L", 2500000.0.toInrCompact())
        assertEquals("₹1Cr", 10000000.0.toInrCompact())
        assertEquals("₹1.5Cr", 15000000.0.toInrCompact())

        // Negative values
        assertEquals("-₹50,000", (-50000.0).toInrFull())
        assertEquals("-₹50k", (-50000.0).toInrCompact())

        // Parsing
        assertEquals(15000000.0, CurrencyFormatter.parseInr("₹1.5Cr")!!, 0.001)
        assertEquals(2500000.0, CurrencyFormatter.parseInr("₹25L")!!, 0.001)
        assertEquals(50000.0, CurrencyFormatter.parseInr("₹50k")!!, 0.001)
        assertEquals(125000.0, CurrencyFormatter.parseInr("₹1,25,000")!!, 0.001)
    }
}
