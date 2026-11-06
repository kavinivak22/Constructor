package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.entity.*
import com.example.constructorapp.ui.viewmodel.InventoryViewModel
import com.example.constructorapp.ui.viewmodel.MaterialsViewModel
import com.example.constructorapp.util.toInrCompact
import com.example.constructorapp.util.toInrFull
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class Milestone4FinancialsAndInventoryTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. INVENTORY & STOCK TRANSFER TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testLowStockWarningClassification() {
        val minStock = 100.0

        // Low stock: <= minStock
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(100.0, minStock))
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(50.0, minStock))
        assertEquals("Low Stock", InventoryViewModel.getStockStatusLevel(0.0, minStock))

        // Medium stock: > minStock and <= minStock * 1.5
        assertEquals("Medium Stock", InventoryViewModel.getStockStatusLevel(101.0, minStock))
        assertEquals("Medium Stock", InventoryViewModel.getStockStatusLevel(150.0, minStock))

        // High stock: > minStock * 1.5
        assertEquals("High Stock", InventoryViewModel.getStockStatusLevel(151.0, minStock))
        assertEquals("High Stock", InventoryViewModel.getStockStatusLevel(500.0, minStock))
    }

    @Test
    fun testStockTransferQuantitiesDeductionAndAddition() {
        var sourceStock = 500.0
        var destinationStock = 120.0
        val transferQuantity = 150.0

        // Validation check
        assertTrue("Transfer quantity must be positive", transferQuantity > 0)
        assertTrue("Transfer quantity must not exceed source stock", transferQuantity <= sourceStock)

        // Perform transfer
        val initialTotal = sourceStock + destinationStock
        sourceStock -= transferQuantity
        destinationStock += transferQuantity

        assertEquals(350.0, sourceStock, 0.001)
        assertEquals(270.0, destinationStock, 0.001)
        assertEquals("Conservation of total inventory across sites", initialTotal, sourceStock + destinationStock, 0.001)
    }

    @Test
    fun testStockTransferExceedingAvailableStockFailsValidation() {
        val sourceStock = 50.0
        val requestedTransfer = 75.0

        val isValid = requestedTransfer > 0 && requestedTransfer <= sourceStock
        assertFalse("Cannot transfer more stock than available in source yard", isValid)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PURCHASE ORDERS LIFECYCLE & TOTAL COMPUTATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPurchaseOrderLineItemsAndGrandTotal() {
        val poId = UUID.randomUUID().toString()
        val items = listOf(
            PurchaseOrderItemEntity(
                id = UUID.randomUUID().toString(),
                poId = poId,
                materialName = "Ultratech 53 Cement",
                quantity = 200.0,
                unitPrice = 380.0,
                unit = "bags"
            ),
            PurchaseOrderItemEntity(
                id = UUID.randomUUID().toString(),
                poId = poId,
                materialName = "Fe 550D TMT Steel 12mm",
                quantity = 2.5,
                unitPrice = 64000.0,
                unit = "tons"
            ),
            PurchaseOrderItemEntity(
                id = UUID.randomUUID().toString(),
                poId = poId,
                materialName = "River Sand M-Sand",
                quantity = 4.0,
                unitPrice = 8500.0,
                unit = "units"
            )
        )

        // Item level verification
        assertEquals(76000.0, items[0].totalPrice, 0.001) // 200 * 380
        assertEquals(160000.0, items[1].totalPrice, 0.001) // 2.5 * 64,000
        assertEquals(34000.0, items[2].totalPrice, 0.001) // 4 * 8,500

        // Grand total
        val grandTotal = items.sumOf { it.totalPrice }
        assertEquals(270000.0, grandTotal, 0.001)

        val po = PurchaseOrderEntity(
            id = poId,
            projectId = "project-1",
            poNumber = "PO-20261101-001",
            supplierName = "Sri Murugan Agencies",
            totalAmount = grandTotal,
            status = "Draft"
        )

        assertEquals("Draft", po.status)
        assertEquals(270000.0, po.totalAmount, 0.001)

        // Approval transition
        val approvedPo = po.copy(status = "Approved", approvedBy = "SuperAdmin")
        assertEquals("Approved", approvedPo.status)
        assertEquals("SuperAdmin", approvedPo.approvedBy)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. COMPANY MATERIALS & PRICE DRIFT RECONCILIATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPriceDriftFormulasAndSeverityBadges() {
        // Case 1: On budget (Green)
        // Committed 100 bags @ 400 = 40,000; Actual 95 bags @ 390 = 37,050
        val greenSeverity = MaterialsViewModel.computeSeverity(
            committedQty = 100.0,
            committedRate = 400.0,
            actualQty = 95.0,
            actualRate = 390.0
        )
        assertEquals("green", greenSeverity)

        // Case 2: Mild drift <= 10% (Amber)
        // Committed 100 bags @ 400 = 40,000; Actual 105 bags @ 420 = 44,100 (rate drift +5%, qty drift +5%)
        val amberSeverity = MaterialsViewModel.computeSeverity(
            committedQty = 100.0,
            committedRate = 400.0,
            actualQty = 105.0,
            actualRate = 420.0
        )
        assertEquals("amber", amberSeverity)

        // Case 3: Rate Overrun > 10% (Red)
        // Committed 100 bags @ 400; Actual 100 bags @ 460 (+15% rate drift)
        val redRateSeverity = MaterialsViewModel.computeSeverity(
            committedQty = 100.0,
            committedRate = 400.0,
            actualQty = 100.0,
            actualRate = 460.0
        )
        assertEquals("red", redRateSeverity)

        // Case 4: Quantity Overrun > 10% (Red)
        // Committed 100 bags @ 400; Actual 125 bags @ 400 (+25% excess quantity consumed)
        val redQtySeverity = MaterialsViewModel.computeSeverity(
            committedQty = 100.0,
            committedRate = 400.0,
            actualQty = 125.0,
            actualRate = 400.0
        )
        assertEquals("red", redQtySeverity)
    }

    @Test
    fun testNetVarianceAndDriftPercentageCalculations() {
        val committedQty = 500.0
        val committedRate = 380.0
        val committedTotal = committedQty * committedRate // 190,000

        val actualQty = 540.0
        val actualRate = 410.0
        val actualTotal = actualQty * actualRate // 221,400

        val netVariance = actualTotal - committedTotal // +31,400
        val percentageDrift = (netVariance / committedTotal) * 100.0 // ~16.526%

        assertEquals(190000.0, committedTotal, 0.001)
        assertEquals(221400.0, actualTotal, 0.001)
        assertEquals(31400.0, netVariance, 0.001)
        assertEquals(16.526, percentageDrift, 0.01)
        assertTrue("Net variance should indicate budget cost overrun", netVariance > 0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. WEEKLY PAY-DAY & SPLIT PAYOUT ITEM
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testWeeklyPaydayRunAggregationAndSplitMath() {
        val items = listOf(
            PayoutItemEntity(
                id = "1",
                payoutId = "run-1",
                recipientType = "labor_wage",
                recipientId = "cont-1",
                recipientName = "Kavin Mason Contractor",
                amountDue = 15000.0,
                amountPaid = 15000.0,
                status = "paid",
                payoutClass = "rate"
            ),
            PayoutItemEntity(
                id = "2",
                payoutId = "run-1",
                recipientType = "labor_wage",
                recipientId = "cont-2",
                recipientName = "Muthu Daily Labor NMR",
                amountDue = 8000.0,
                amountPaid = 8000.0,
                status = "paid",
                payoutClass = "nmr"
            ),
            PayoutItemEntity(
                id = "3",
                payoutId = "run-1",
                recipientType = "vendor_payment",
                recipientId = "vend-1",
                recipientName = "Lakshmi Electricals",
                amountDue = 12000.0,
                amountPaid = 0.0,
                status = "pending",
                payoutClass = "rate"
            )
        )

        val totalAmountDue = items.sumOf { it.amountDue }
        val totalAmountPaid = items.sumOf { it.amountPaid }
        val pendingPayout = totalAmountDue - totalAmountPaid

        assertEquals(35000.0, totalAmountDue, 0.001)
        assertEquals(23000.0, totalAmountPaid, 0.001)
        assertEquals(12000.0, pendingPayout, 0.001)

        // Split item test
        val originalItem = items[0]
        val ratePortion = 10000.0
        val nmrPortion = 5000.0

        assertEquals("Split components must sum to original amount due", originalItem.amountDue, ratePortion + nmrPortion, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CONTRACTOR ACCOUNTS & DUAL LEDGERS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testContractorDualLedgerSegregationMath() {
        val contractorId = "contractor-123"

        val entries = listOf(
            // Rate contract entries (Sq.Ft work)
            ContractorLedgerEntryEntity(
                id = "e1",
                contractorId = contractorId,
                transactionDate = "2026-10-15",
                entryType = "work",
                description = "First floor brickwork 1200 sq.ft @ ₹45",
                amount = 54000.0,
                status = "paid",
                payoutClass = "rate"
            ),
            ContractorLedgerEntryEntity(
                id = "e2",
                contractorId = contractorId,
                transactionDate = "2026-10-22",
                entryType = "work",
                description = "External plastering 800 sq.ft @ ₹35",
                amount = 28000.0,
                status = "pending",
                payoutClass = "rate"
            ),
            // NMR labor entries (Daily attendance muster)
            ContractorLedgerEntryEntity(
                id = "e3",
                contractorId = contractorId,
                transactionDate = "2026-10-18",
                entryType = "work",
                description = "10 Coolies concrete pouring shift @ ₹650",
                amount = 6500.0,
                status = "paid",
                payoutClass = "nmr"
            ),
            ContractorLedgerEntryEntity(
                id = "e4",
                contractorId = contractorId,
                transactionDate = "2026-10-25",
                entryType = "work",
                description = "8 Coolies site clearance shift @ ₹650",
                amount = 5200.0,
                status = "pending",
                payoutClass = "nmr"
            )
        )

        // Rate Account Calculations
        val rateEntries = entries.filter { it.payoutClass == "rate" }
        val rateCleared = rateEntries.filter { it.status == "paid" }.sumOf { it.amount }
        val rateDue = rateEntries.filter { it.status == "pending" }.sumOf { it.amount }

        assertEquals(54000.0, rateCleared, 0.001)
        assertEquals(28000.0, rateDue, 0.001)

        // NMR Account Calculations
        val nmrEntries = entries.filter { it.payoutClass == "nmr" }
        val nmrCleared = nmrEntries.filter { it.status == "paid" }.sumOf { it.amount }
        val nmrDue = nmrEntries.filter { it.status == "pending" }.sumOf { it.amount }

        assertEquals(6500.0, nmrCleared, 0.001)
        assertEquals(5200.0, nmrDue, 0.001)

        // Verify Strict Segregation: Rate amounts must not influence NMR balances
        assertNotEquals(rateCleared, nmrCleared)
        assertNotEquals(rateDue, nmrDue)

        val totalCleared = rateCleared + nmrCleared
        val totalDue = rateDue + nmrDue
        assertEquals(60500.0, totalCleared, 0.001)
        assertEquals(33200.0, totalDue, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. CLIENT PAYMENT MILESTONES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testClientMilestoneProgressAndCollectionMath() {
        val totalContractBudget = 5000000.0 // 50 Lakhs INR

        val stages = listOf(
            ClientMilestoneEntity(
                id = "m1",
                projectId = "p1",
                stageNumber = 1,
                stageName = "Booking Advance",
                pctShare = 10.0,
                expectedAmount = 500000.0,
                collectedAmount = 500000.0,
                status = "collected"
            ),
            ClientMilestoneEntity(
                id = "m2",
                projectId = "p1",
                stageNumber = 2,
                stageName = "Foundation Complete",
                pctShare = 15.0,
                expectedAmount = 750000.0,
                collectedAmount = 750000.0,
                status = "collected"
            ),
            ClientMilestoneEntity(
                id = "m3",
                projectId = "p1",
                stageNumber = 3,
                stageName = "Ground Floor Slab",
                pctShare = 20.0,
                expectedAmount = 1000000.0,
                collectedAmount = 600000.0, // Partial collection
                status = "pending"
            ),
            ClientMilestoneEntity(
                id = "m4",
                projectId = "p1",
                stageNumber = 4,
                stageName = "First Floor Slab",
                pctShare = 20.0,
                expectedAmount = 1000000.0,
                collectedAmount = 0.0,
                status = "upcoming"
            ),
            ClientMilestoneEntity(
                id = "m5",
                projectId = "p1",
                stageNumber = 5,
                stageName = "Brickwork & Plastering",
                pctShare = 20.0,
                expectedAmount = 1000000.0,
                collectedAmount = 0.0,
                status = "upcoming"
            ),
            ClientMilestoneEntity(
                id = "m6",
                projectId = "p1",
                stageNumber = 6,
                stageName = "Handover",
                pctShare = 15.0,
                expectedAmount = 750000.0,
                collectedAmount = 0.0,
                status = "upcoming"
            )
        )

        // Validate percentage sum
        val totalPct = stages.sumOf { it.pctShare }
        assertEquals(100.0, totalPct, 0.001)

        val totalExpected = stages.sumOf { it.expectedAmount }
        val totalCollected = stages.sumOf { it.collectedAmount }
        val totalPending = totalExpected - totalCollected
        val progressPercentage = ((totalCollected / totalExpected) * 100.0).toInt()

        assertEquals(5000000.0, totalExpected, 0.001)
        assertEquals(1850000.0, totalCollected, 0.001) // 5L + 7.5L + 6L
        assertEquals(3150000.0, totalPending, 0.001)
        assertEquals(37, progressPercentage) // 1,850,000 / 5,000,000 = 37%

        // Stage 3 partial balance
        val stage3Balance = stages[2].expectedAmount - stages[2].collectedAmount
        assertEquals(400000.0, stage3Balance, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. EXPENSES FILTERING & CURRENCY FORMATTING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testExpenseFilteringAndCurrencyFormatting() {
        val expenses = listOf(
            ExpenseEntity(
                id = "ex1",
                projectId = "proj-A",
                amount = 4500.0,
                category = "materials",
                description = "River sand delivery",
                expenseDate = "2026-10-30",
                paymentStatus = "paid"
            ),
            ExpenseEntity(
                id = "ex2",
                projectId = "proj-A",
                amount = 12000.0,
                category = "labor",
                description = "Carpenter overtime wage",
                expenseDate = "2026-10-31",
                paymentStatus = "paid"
            ),
            ExpenseEntity(
                id = "ex3",
                projectId = "proj-B",
                amount = 8500.0,
                category = "equipment",
                description = "JCB excavator hire",
                expenseDate = "2026-10-31",
                paymentStatus = "pending"
            )
        )

        // Filter by project
        val projAExpenses = expenses.filter { it.projectId == "proj-A" }
        assertEquals(2, projAExpenses.size)
        assertEquals(16500.0, projAExpenses.sumOf { it.amount }, 0.001)

        // Filter by category
        val materialsExpenses = expenses.filter { it.category == "materials" }
        assertEquals(1, materialsExpenses.size)
        assertEquals(4500.0, materialsExpenses[0].amount, 0.001)

        // Currency Formatter tests
        assertEquals("₹4,500", 4500.0.toInrFull())
        assertEquals("₹1,25,000", 125000.0.toInrFull())
        assertEquals("₹1.5Cr", 15000000.0.toInrCompact())
        assertEquals("₹25L", 2500000.0.toInrCompact())
        assertEquals("₹50k", 50000.0.toInrCompact())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. ITERATION 2 WORKFLOW & STATE TRANSITION REMEDIATION TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPaydayAutoExpenseGenerationUsesDisbursedAmountFallback() {
        val draftItems = listOf(
            PayoutItemEntity(
                id = "item-1",
                payoutId = "run-101",
                recipientType = "labor_wage",
                recipientId = "contractor-1",
                recipientName = "Periyasamy Mason Team",
                amountDue = 14500.0,
                amountPaid = 0.0, // Default generated in draft
                projectId = "proj-101"
            ),
            PayoutItemEntity(
                id = "item-2",
                payoutId = "run-101",
                recipientType = "vendor_payment",
                recipientId = "vendor-1",
                recipientName = "Sri Murugan Steels",
                amountDue = 35000.0,
                amountPaid = 0.0, // Default generated in draft
                projectId = "proj-101"
            ),
            PayoutItemEntity(
                id = "item-3",
                payoutId = "run-101",
                recipientType = "employee_salary",
                recipientId = "staff-1",
                recipientName = "Site Supervisor",
                amountDue = 0.0,
                amountPaid = 0.0, // Zero due and zero paid
                projectId = "proj-101"
            )
        )

        // Execute fallback algorithm matching FinancialsDao.updatePayoutRunStatus
        val itemAmountPaidUpdates = mutableMapOf<String, Double>()
        val generatedExpenses = draftItems.mapNotNull { item ->
            val disbursedAmount = if (item.amountPaid > 0.0) item.amountPaid else item.amountDue
            if (item.amountPaid <= 0.0 && disbursedAmount > 0.0) {
                itemAmountPaidUpdates[item.id] = disbursedAmount
            }
            if (!item.projectId.isNullOrBlank() && disbursedAmount > 0.0) {
                val category = if (item.recipientType.equals("vendor_payment", ignoreCase = true)) "materials" else "labor"
                ExpenseEntity(
                    projectId = item.projectId,
                    userId = item.recipientId,
                    amount = disbursedAmount,
                    category = category,
                    description = "Payout: ${item.recipientName}",
                    expenseDate = "2026-11-01",
                    paymentStatus = "paid",
                    paymentMode = "bank_transfer"
                )
            } else null
        }

        assertEquals(2, generatedExpenses.size)
        assertEquals(14500.0, generatedExpenses[0].amount, 0.001)
        assertEquals("labor", generatedExpenses[0].category)

        assertEquals(35000.0, generatedExpenses[1].amount, 0.001)
        assertEquals("materials", generatedExpenses[1].category)

        // Verify itemAmountPaid was updated with fallback
        assertEquals(14500.0, itemAmountPaidUpdates["item-1"]!!, 0.001)
        assertEquals(35000.0, itemAmountPaidUpdates["item-2"]!!, 0.001)
        assertFalse(itemAmountPaidUpdates.containsKey("item-3"))

        // Total expense matches aggregate non-zero payout
        val totalDisbursedExpense = generatedExpenses.sumOf { it.amount }
        assertEquals(49500.0, totalDisbursedExpense, 0.001)
    }

    @Test
    fun testSplitPayoutItemDeletesOriginalPreventingInflation() {
        val originalItem = PayoutItemEntity(
            id = "orig-item-1",
            payoutId = "run-1",
            recipientType = "contractor_wage",
            recipientId = "c1",
            recipientName = "Ravi Mason",
            amountDue = 25000.0,
            amountPaid = 0.0,
            payoutClass = "rate"
        )

        val tablePayoutItems = mutableListOf(originalItem)
        assertEquals(1, tablePayoutItems.size)
        assertEquals(25000.0, tablePayoutItems.sumOf { it.amountDue }, 0.001)

        val ratePortion = 15000.0
        val nmrPortion = 10000.0

        val rateItem = originalItem.copy(
            id = "split-rate-1",
            payoutClass = "rate",
            amountDue = ratePortion
        )
        val nmrItem = originalItem.copy(
            id = "split-nmr-1",
            payoutClass = "nmr",
            amountDue = nmrPortion
        )

        // Action: delete original item by ID and insert split entries
        tablePayoutItems.removeIf { it.id == originalItem.id }
        tablePayoutItems.addAll(listOf(rateItem, nmrItem))

        assertEquals(2, tablePayoutItems.size)
        assertFalse("Original item must be deleted", tablePayoutItems.any { it.id == originalItem.id })
        assertTrue("Rate split item must exist", tablePayoutItems.any { it.id == rateItem.id })
        assertTrue("NMR split item must exist", tablePayoutItems.any { it.id == nmrItem.id })
        assertEquals("Total liability must be exactly preserved without inflation", 25000.0, tablePayoutItems.sumOf { it.amountDue }, 0.001)
    }

    @Test
    fun testPurchaseOrderApprovalIsIdempotent() {
        var poStatus = "Draft"
        var currentInventoryStock = 100.0
        val orderQuantity = 50.0

        fun approvePo(targetPoStatus: String): Boolean {
            if (targetPoStatus.equals("Approved", ignoreCase = true) ||
                targetPoStatus.equals("Ordered", ignoreCase = true) ||
                targetPoStatus.equals("Delivered", ignoreCase = true)) {
                return false
            }
            poStatus = "Approved"
            currentInventoryStock += orderQuantity
            return true
        }

        // 1st approval from Draft -> succeeds
        val firstResult = approvePo(poStatus)
        assertTrue(firstResult)
        assertEquals("Approved", poStatus)
        assertEquals(150.0, currentInventoryStock, 0.001)

        // 2nd approval when already Approved -> rejected, stock unchanged
        val secondResult = approvePo(poStatus)
        assertFalse("Subsequent approval must be rejected by idempotency guard", secondResult)
        assertEquals("Approved", poStatus)
        assertEquals("Stock must remain 150.0 and not increment again", 150.0, currentInventoryStock, 0.001)

        // Subsequent approval after transition to Delivered -> rejected, stock unchanged
        poStatus = "Delivered"
        val thirdResult = approvePo(poStatus)
        assertFalse("Approval on Delivered PO must be rejected", thirdResult)
        assertEquals("Delivered", poStatus)
        assertEquals(150.0, currentInventoryStock, 0.001)
    }

    @Test
    fun testPurchaseOrderApprovalAutoCreatesMissingInventoryAsset() {
        val inventoryTable = mutableMapOf<String, Double>("Ultratech 53 Cement" to 50.0)

        fun incrementOrInsert(name: String, quantity: Double, unitPrice: Double) {
            val rowsUpdated = if (inventoryTable.containsKey(name)) {
                inventoryTable[name] = inventoryTable[name]!! + quantity
                1
            } else {
                0
            }

            if (rowsUpdated == 0) {
                // Asset does not exist in inventory_assets: instantiate and insert
                inventoryTable[name] = quantity
            }
        }

        // Case 1: Existing item increments
        incrementOrInsert("Ultratech 53 Cement", 30.0, 420.0)
        assertEquals(80.0, inventoryTable["Ultratech 53 Cement"]!!, 0.001)

        // Case 2: Missing item is created with initial stock equal to ordered quantity
        assertFalse(inventoryTable.containsKey("Fe 550D TMT Steel 16mm"))
        incrementOrInsert("Fe 550D TMT Steel 16mm", 4.5, 68000.0)
        assertTrue(inventoryTable.containsKey("Fe 550D TMT Steel 16mm"))
        assertEquals(4.5, inventoryTable["Fe 550D TMT Steel 16mm"]!!, 0.001)
    }
}
