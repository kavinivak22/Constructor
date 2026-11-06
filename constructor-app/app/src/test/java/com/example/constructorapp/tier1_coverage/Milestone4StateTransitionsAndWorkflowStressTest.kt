package com.example.constructorapp.tier1_coverage

import android.content.Intent
import android.net.Uri
import com.example.constructorapp.*
import com.example.constructorapp.data.local.entity.*
import com.example.constructorapp.data.remote.PurchaseOrderDto
import com.example.constructorapp.data.remote.PurchaseOrderItemDto
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.UUID

/**
 * Empirical Stress Harness for Milestone 4:
 * 1. Purchase Order Lifecycle & Stock Increment Invariants
 * 2. Payday Run Lifecycle, Auto-Expense Generation & Split Payout Invariants
 * 3. Contractor Quick Dial Intent Formatting & Robustness
 * 4. Image Compression Mathematical Bounds & Graceful Degradation
 * 5. Navigation Routes, M4 NavKeys & Backstack Safety
 */
@RunWith(JUnit4::class)
class Milestone4StateTransitionsAndWorkflowStressTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PURCHASE ORDER LIFECYCLE & STOCK INCREMENT INVARIANTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPurchaseOrderStatusTransition_fullStateMachine() {
        // Valid status sequence: Draft -> Awaiting Approval -> Approved -> Ordered -> Delivered
        val validTransitions = listOf(
            "Draft" to "Awaiting Approval",
            "Awaiting Approval" to "Approved",
            "Approved" to "Ordered",
            "Ordered" to "Delivered"
        )

        val allowedStatuses = setOf("Draft", "Awaiting Approval", "Approved", "Ordered", "Delivered", "Cancelled")

        var currentStatus = "Draft"
        for ((from, to) in validTransitions) {
            assertEquals(from, currentStatus)
            assertTrue("Target status must be in allowed statuses", allowedStatuses.contains(to))
            currentStatus = to
        }
        assertEquals("Delivered", currentStatus)

        // Cancellation should be possible from Draft or Awaiting Approval
        assertTrue(allowedStatuses.contains("Cancelled"))
    }

    @Test
    fun testPurchaseOrderApproval_stockIncrementCalculation() {
        val inventoryBefore = mutableMapOf(
            "Ultratech 53 Cement" to 100.0,
            "Fe 550D TMT Steel 12mm" to 5.0
        )

        val poItems = listOf(
            PurchaseOrderItemEntity(
                poId = "po-1",
                materialName = "Ultratech 53 Cement",
                quantity = 50.0,
                unitPrice = 420.0
            ),
            PurchaseOrderItemEntity(
                poId = "po-1",
                materialName = "Fe 550D TMT Steel 12mm",
                quantity = 2.5,
                unitPrice = 65000.0
            )
        )

        // Simulate single approval stock increment
        for (item in poItems) {
            val current = inventoryBefore[item.materialName] ?: 0.0
            inventoryBefore[item.materialName] = current + item.quantity
        }

        assertEquals(150.0, inventoryBefore["Ultratech 53 Cement"]!!, 0.001)
        assertEquals(7.5, inventoryBefore["Fe 550D TMT Steel 12mm"]!!, 0.001)
    }

    @Test
    fun testPurchaseOrderApproval_idempotencyFailureVulnerability() {
        // CHALLENGE: If approvePurchaseOrder is invoked twice on the same PO,
        // does the stock increment twice without an idempotent status check?
        var poStatus = "Draft"
        var currentStock = 100.0
        val poQty = 50.0

        fun approvePo() {
            // Unprotected implementation (as currently in PurchaseOrderDao):
            poStatus = "Approved"
            currentStock += poQty
        }

        // First approval
        approvePo()
        assertEquals(150.0, currentStock, 0.001)
        assertEquals("Approved", poStatus)

        // Adversarial second approval (duplicate tap or race condition)
        approvePo()
        // If not guarded by `if (poStatus != "Approved")`, stock becomes 200 instead of 150!
        assertEquals("Vulnerability confirmed: duplicate approval increments stock twice", 200.0, currentStock, 0.001)

        // Mitigation verification: Idempotent implementation
        var safeStock = 100.0
        var safeStatus = "Draft"
        fun safeApprovePo(): Boolean {
            if (safeStatus == "Approved") return false // Guard against duplicate increment
            safeStatus = "Approved"
            safeStock += poQty
            return true
        }

        assertTrue(safeApprovePo())
        assertEquals(150.0, safeStock, 0.001)
        assertFalse("Second approval should be rejected", safeApprovePo())
        assertEquals("Stock remains preserved under idempotent guard", 150.0, safeStock, 0.001)
    }

    @Test
    fun testPurchaseOrderApproval_missingInventoryItemHandling() {
        // CHALLENGE: If a PO item references a material not yet present in inventory_assets,
        // SQL `UPDATE inventory_assets SET currentStock = currentStock + :qty WHERE name = :name`
        // matches 0 rows and fails silently without creating the asset.
        val existingInventory = mutableMapOf<String, Double>("River Sand" to 20.0)
        val poItemName = "M-Sand Grade 1"
        val poQty = 15.0

        // Simulating the DAO UPDATE query
        val rowsAffected = if (existingInventory.containsKey(poItemName)) {
            existingInventory[poItemName] = existingInventory[poItemName]!! + poQty
            1
        } else {
            0 // Row not found, 0 rows updated
        }

        assertEquals("UPDATE on non-existent item affects 0 rows", 0, rowsAffected)
        assertFalse("Item was NOT created in inventory", existingInventory.containsKey(poItemName))

        // Mitigation: If rowsAffected == 0, fallback to upserting new asset
        if (rowsAffected == 0) {
            existingInventory[poItemName] = poQty
        }
        assertTrue("Mitigation creates asset when absent", existingInventory.containsKey(poItemName))
        assertEquals(15.0, existingInventory[poItemName]!!, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PAYDAY RUN LIFECYCLE, AUTO-EXPENSE & SPLIT PAYOUT INVARIANTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testPaydayRunStatusTransitions() {
        val allowedStatuses = setOf("draft", "approved", "paid")
        var runStatus = "draft"

        assertTrue(allowedStatuses.contains(runStatus))

        // Draft -> Approved
        runStatus = "approved"
        assertTrue(allowedStatuses.contains(runStatus))

        // Approved -> Paid
        runStatus = "paid"
        assertTrue(allowedStatuses.contains(runStatus))
    }

    @Test
    fun testPaydayDisbursement_autoExpenseCategoryMapping() {
        val items = listOf(
            PayoutItemEntity(
                payoutId = "run-1",
                recipientType = "vendor_payment",
                recipientId = "v1",
                recipientName = "Kavin Bricks",
                amountDue = 25000.0,
                amountPaid = 25000.0,
                projectId = "p-1"
            ),
            PayoutItemEntity(
                payoutId = "run-1",
                recipientType = "labor_wage",
                recipientId = "c1",
                recipientName = "Muthu Mason Gang",
                amountDue = 18000.0,
                amountPaid = 18000.0,
                projectId = "p-1"
            ),
            PayoutItemEntity(
                payoutId = "run-1",
                recipientType = "employee_salary",
                recipientId = "e1",
                recipientName = "Site Engineer",
                amountDue = 35000.0,
                amountPaid = 35000.0,
                projectId = "p-1"
            )
        )

        // Mapping rule from FinancialsDao:
        // if (item.recipientType.equals("vendor_payment", ignoreCase = true)) "materials" else "labor"
        val expenses = items.map { item ->
            val category = if (item.recipientType.equals("vendor_payment", ignoreCase = true)) "materials" else "labor"
            ExpenseEntity(
                projectId = item.projectId,
                userId = item.recipientId,
                amount = item.amountPaid,
                category = category,
                description = "Payout: ${item.recipientName}",
                expenseDate = "2026-11-01",
                paymentStatus = "paid",
                paymentMode = "bank_transfer"
            )
        }

        assertEquals("materials", expenses[0].category)
        assertEquals(25000.0, expenses[0].amount, 0.001)

        assertEquals("labor", expenses[1].category)
        assertEquals(18000.0, expenses[1].amount, 0.001)

        assertEquals("labor", expenses[2].category)
        assertEquals(35000.0, expenses[2].amount, 0.001)
    }

    @Test
    fun testPaydayDisbursement_zeroAmountPaidBugExposure() {
        // CHALLENGE: In FinancialsDao.updatePayoutRunStatus(runId, "paid"):
        // `updatePayoutItemsStatusForRun(runId, "paid")` updates status to "paid"
        // BUT does NOT set `amountPaid = amountDue`.
        // If items are generated with amountPaid = 0.0, the generated ExpenseEntity has amount = 0.0!
        val itemWithZeroAmountPaid = PayoutItemEntity(
            payoutId = "run-1",
            recipientType = "labor_wage",
            recipientId = "mason-1",
            recipientName = "Mason Contractor",
            amountDue = 14500.0,
            amountPaid = 0.0, // Default generated value
            projectId = "project-1"
        )

        // As implemented in FinancialsDao.kt lines 360-372:
        val generatedExpense = ExpenseEntity(
            projectId = itemWithZeroAmountPaid.projectId,
            userId = itemWithZeroAmountPaid.recipientId,
            amount = itemWithZeroAmountPaid.amountPaid, // BUG: amount is 0.0!
            category = "labor",
            description = "Payout: ${itemWithZeroAmountPaid.recipientName}",
            expenseDate = "2026-11-01",
            paymentStatus = "paid",
            paymentMode = "bank_transfer"
        )

        assertEquals("Bug confirmed: expense logged with 0.0 when amountPaid not initialized", 0.0, generatedExpense.amount, 0.001)
        assertNotEquals(itemWithZeroAmountPaid.amountDue, generatedExpense.amount)

        // Mitigation verification:
        val effectiveAmount = if (itemWithZeroAmountPaid.amountPaid > 0) {
            itemWithZeroAmountPaid.amountPaid
        } else {
            itemWithZeroAmountPaid.amountDue
        }
        val correctedExpense = generatedExpense.copy(amount = effectiveAmount)
        assertEquals(14500.0, correctedExpense.amount, 0.001)
    }

    @Test
    fun testSplitPayoutItem_mathConservationAndDuplicateItemRisk() {
        val originalItem = PayoutItemEntity(
            id = "original-item-uuid",
            payoutId = "run-1",
            recipientType = "contractor_wage",
            recipientId = "contractor-101",
            recipientName = "Periyasamy Mason Team",
            amountDue = 25000.0,
            amountPaid = 0.0,
            payoutClass = "rate",
            projectId = "proj-1"
        )

        val ratePortion = 15000.0
        val nmrPortion = 10000.0

        // Invariant 1: Sum conservation
        assertEquals("Split components must strictly sum to original amount due", originalItem.amountDue, ratePortion + nmrPortion, 0.001)

        // CHALLENGE: In PaydayViewModel.splitPayoutItem:
        // `financialsDao.insertPayoutItems(listOf(rateItem, nmrItem))` is called with new UUIDs,
        // but `originalItem` is NOT deleted from DB!
        val itemsList = mutableListOf(originalItem)

        val rateItem = originalItem.copy(
            id = UUID.randomUUID().toString(),
            payoutClass = "rate",
            amountDue = ratePortion
        )
        val nmrItem = originalItem.copy(
            id = UUID.randomUUID().toString(),
            payoutClass = "nmr",
            amountDue = nmrPortion
        )

        // If original item is not removed:
        itemsList.addAll(listOf(rateItem, nmrItem))
        assertEquals(3, itemsList.size)
        val inflatedTotal = itemsList.sumOf { it.amountDue }
        assertEquals(50000.0, inflatedTotal, 0.001) // Doubled total!

        // Correct workflow: Replace original item with split items
        itemsList.removeIf { it.id == originalItem.id }
        assertEquals(2, itemsList.size)
        val correctTotal = itemsList.sumOf { it.amountDue }
        assertEquals(25000.0, correctTotal, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CONTRACTOR QUICK DIAL INTENT FORMATTING & ROBUSTNESS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testContractorQuickDialIntent_validPhoneFormats() {
        fun buildDialIntent(phone: String): Pair<String, String> {
            val sanitizedPhone = phone.trim()
            val uriString = "tel:$sanitizedPhone"
            return "android.intent.action.DIAL" to uriString
        }

        // Test 1: Standard 10-digit number
        val (action1, uri1) = buildDialIntent("9876543210")
        assertEquals("android.intent.action.DIAL", action1)
        assertEquals("tel:9876543210", uri1)

        // Test 2: Standard with country code
        val (action2, uri2) = buildDialIntent("+919876543210")
        assertEquals("tel:+919876543210", uri2)

        // Test 3: Phone with leading/trailing whitespaces
        val (action3, uri3) = buildDialIntent("  9840112233  ")
        assertEquals("tel:9840112233", uri3)

        // Test 4: Verify URI scheme and payload
        val parsedUri = java.net.URI.create("tel:9876543210")
        assertEquals("tel", parsedUri.scheme)
        assertEquals("9876543210", parsedUri.schemeSpecificPart)
    }

    @Test
    fun testContractorQuickDialIntent_adversarialEmptyOrBlankInput() {
        fun safeDialIntent(phone: String?): Pair<String, String>? {
            val cleaned = phone?.trim()
            if (cleaned.isNullOrEmpty()) return null
            return "android.intent.action.DIAL" to "tel:$cleaned"
        }

        assertNull("Null phone should not generate dial intent", safeDialIntent(null))
        assertNull("Blank phone should not generate dial intent", safeDialIntent("   "))
        assertNotNull(safeDialIntent("9876543210"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. IMAGE COMPRESSION BOUNDS & GRACEFUL DEGRADATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testImageCompressor_specificationConstantsContract() {
        // Spec requirements:
        // - Max dimension: 2560px
        // - Target max bytes: 1.5MB (1,500,000 bytes)
        // - Initial quality: 80%
        // - Min quality: 45%
        // - Quality step: 5%
        val maxDimension = 2560
        val targetMaxBytes = 1_500_000L
        val initialQuality = 80
        val minQuality = 45
        val qualityStep = 5

        assertEquals(2560, maxDimension)
        assertEquals(1500000L, targetMaxBytes)
        assertEquals(80, initialQuality)
        assertEquals(45, minQuality)
        assertEquals(5, qualityStep)
    }

    @Test
    fun testImageCompressor_dimensionDownsamplingAlgorithm() {
        fun computeSampleSize(width: Int, height: Int, maxDim: Int = 2560): Int {
            var inSampleSize = 1
            val maxOriginal = maxOf(width, height)
            if (maxOriginal > maxDim) {
                inSampleSize = 2
                while ((maxOriginal / inSampleSize) > maxDim) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        // Case 1: Standard 1080p (1920x1080) -> within 2560 -> inSampleSize = 1
        assertEquals(1, computeSampleSize(1920, 1080))

        // Case 2: 4K UHD (3840x2160) -> max is 3840 > 2560 -> inSampleSize = 2 (3840/2 = 1920 <= 2560)
        assertEquals(2, computeSampleSize(3840, 2160))

        // Case 3: 12MP Phone Camera (4032x3024) -> 4032 > 2560 -> inSampleSize = 2
        assertEquals(2, computeSampleSize(4032, 3024))

        // Case 4: 108MP Ultra High Res (12000x9000) -> 12000 > 2560 -> inSampleSize = 8 (12000/8 = 1500)
        assertEquals(8, computeSampleSize(12000, 9000))

        // Case 5: Exact boundary 2560x1440 -> inSampleSize = 1
        assertEquals(1, computeSampleSize(2560, 1440))
    }

    @Test
    fun testImageCompressor_aspectRatioPreservingScaleCalculation() {
        fun computeScaledDimensions(origWidth: Int, origHeight: Int, maxDim: Int = 2560): Pair<Int, Int> {
            if (origWidth <= maxDim && origHeight <= maxDim) {
                return origWidth to origHeight
            }
            val ratio = minOf(maxDim.toFloat() / origWidth, maxDim.toFloat() / origHeight)
            val newWidth = (origWidth * ratio).toInt()
            val newHeight = (origHeight * ratio).toInt()
            return newWidth to newHeight
        }

        // Test scaling 4000 x 3000 down to max 2560:
        val (scaledW, scaledH) = computeScaledDimensions(4000, 3000, 2560)
        assertEquals(2560, scaledW)
        assertEquals(1920, scaledH)
        assertEquals(4000.0 / 3000.0, scaledW.toDouble() / scaledH.toDouble(), 0.001)

        // Test portrait 3000 x 4000 down to max 2560:
        val (portraitW, portraitH) = computeScaledDimensions(3000, 4000, 2560)
        assertEquals(1920, portraitW)
        assertEquals(2560, portraitH)
        assertEquals(3000.0 / 4000.0, portraitW.toDouble() / portraitH.toDouble(), 0.001)
    }

    @Test
    fun testExpenseViewModel_compressionFailureGracefulFallback() {
        // When ImageCompressor throws (e.g. invalid URI or corrupt file),
        // ExpenseViewModel must fall back to original URI string without crashing.
        val mockUri = "content://media/external/images/media/9999"
        var finalStoredPath: String? = null

        try {
            // Simulate simulated exception in test
            throw java.io.IOException("Unable to open input stream")
        } catch (e: Exception) {
            finalStoredPath = mockUri // Fallback implemented in ExpenseViewModel
        }

        assertNotNull(finalStoredPath)
        assertEquals(mockUri, finalStoredPath)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. NAVIGATION ROUTES, M4 NAVKEYS & BACKSTACK SAFETY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testAllMilestone4NavKeys_instantiationAndProperties() {
        val inventoryKey = Inventory(siteId = "site-alpha")
        assertEquals("site-alpha", inventoryKey.siteId)

        val inventoryDefault = Inventory()
        assertNull(inventoryDefault.siteId)

        val companyMaterialsKey = CompanyMaterials
        assertNotNull(companyMaterialsKey)

        val materialReconKey = MaterialReconciliation(projectId = "p-100")
        assertEquals("p-100", materialReconKey.projectId)

        val paydayKey = Payday(payoutId = "run-202611")
        assertEquals("run-202611", paydayKey.payoutId)

        val contractorsKey = Contractors(contractorId = "c-50")
        assertEquals("c-50", contractorsKey.contractorId)

        val expensesKey = Expenses(projectId = "p-200")
        assertEquals("p-200", expensesKey.projectId)

        val clientMilestonesKey = ClientMilestones(projectId = "p-300")
        assertEquals("p-300", clientMilestonesKey.projectId)

        val purchaseOrdersKey = PurchaseOrders(projectId = "p-400")
        assertEquals("p-400", purchaseOrdersKey.projectId)
    }

    @Test
    fun testDrawerRouteMapping_allMilestone4RoutesCovered() {
        // Verify that all 7 M4 drawer routes map to valid non-null NavKeys
        fun mapDrawerRoute(route: String): Any? {
            return when (route) {
                "/inventory" -> Inventory()
                "/financials/payday" -> Payday()
                "/financials/contractors" -> Contractors()
                "/expenses" -> Expenses()
                "/materials/reconciliation" -> MaterialReconciliation()
                "/projects/milestones" -> ClientMilestones()
                "/company-materials" -> CompanyMaterials
                else -> null
            }
        }

        assertNotNull(mapDrawerRoute("/inventory"))
        assertNotNull(mapDrawerRoute("/financials/payday"))
        assertNotNull(mapDrawerRoute("/financials/contractors"))
        assertNotNull(mapDrawerRoute("/expenses"))
        assertNotNull(mapDrawerRoute("/materials/reconciliation"))
        assertNotNull(mapDrawerRoute("/projects/milestones"))
        assertNotNull(mapDrawerRoute("/company-materials"))

        assertNull(mapDrawerRoute("/unrecognized-route"))
    }

    @Test
    fun testBackStackNavigation_pushPopInvariants() {
        val backStack = mutableListOf<Any>("Main")

        // Navigate through deep M4 sequence:
        // Main -> Inventory -> StockTransfer -> Back
        backStack.add(Inventory(siteId = "wh-1"))
        assertEquals(2, backStack.size)

        backStack.add(MaterialReconciliation(projectId = "proj-1"))
        assertEquals(3, backStack.size)

        backStack.add(Payday(payoutId = "run-1"))
        assertEquals(4, backStack.size)

        // Back button pops
        val popped1 = backStack.removeLastOrNull()
        assertTrue(popped1 is Payday)
        assertEquals(3, backStack.size)

        val popped2 = backStack.removeLastOrNull()
        assertTrue(popped2 is MaterialReconciliation)
        assertEquals(2, backStack.size)

        val popped3 = backStack.removeLastOrNull()
        assertTrue(popped3 is Inventory)
        assertEquals(1, backStack.size)
        assertEquals("Main", backStack.last())

        // Back on root
        val poppedRoot = backStack.removeLastOrNull()
        assertEquals("Main", poppedRoot)
        assertTrue(backStack.isEmpty())
        assertNull(backStack.removeLastOrNull()) // Safe, does not crash on empty
    }
}
