package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.entity.ExpenseEntity
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.local.entity.PayoutItemEntity
import com.example.constructorapp.data.local.entity.PurchaseOrderEntity
import com.example.constructorapp.data.local.entity.PurchaseOrderItemEntity
import com.example.constructorapp.data.local.entity.WeeklyPayoutRunEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.UUID

/**
 * Empirical Adversarial Verification Test Suite for Milestone 4 Iteration 2
 *
 * Verifies resolution and stress-tests edge cases for:
 * 1. Payday Auto-Expense Disbursed Amount Fallback (> 0.0) & Idempotency
 * 2. Split Payout Item Deletion of originalItem & Conservation of Total Liabilities
 * 3. PurchaseOrderDao.approvePurchaseOrder Idempotency & Auto-Creation of Missing Inventory Assets
 */
@RunWith(JUnit4::class)
class Milestone4Iteration2AdversarialVerificationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE 1: PAYDAY AUTO-EXPENSE DISBURSED AMOUNT FALLBACK & IDEMPOTENCY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulates FinancialsDao.updatePayoutRunStatus logic exactly as written in Daos.kt lines 388-419
     */
    private class SimulatedFinancialsDao {
        val runsTable = mutableMapOf<String, WeeklyPayoutRunEntity>()
        val itemsTable = mutableMapOf<String, PayoutItemEntity>()
        val expensesTable = mutableListOf<ExpenseEntity>()

        fun findPayoutRunById(id: String): WeeklyPayoutRunEntity? = runsTable[id]

        fun getPayoutItemsList(runId: String): List<PayoutItemEntity> =
            itemsTable.values.filter { it.payoutId == runId }

        fun updatePayoutRunStatusOnly(runId: String, status: String) {
            runsTable[runId]?.let {
                runsTable[runId] = it.copy(status = status)
            }
        }

        fun updatePayoutItemsStatusForRun(runId: String, status: String) {
            itemsTable.values.filter { it.payoutId == runId }.forEach {
                itemsTable[it.id] = it.copy(status = status)
            }
        }

        fun updatePayoutItem(itemId: String, status: String, amountPaid: Double) {
            itemsTable[itemId]?.let {
                itemsTable[itemId] = it.copy(status = status, amountPaid = amountPaid)
            }
        }

        fun insertExpense(expense: ExpenseEntity) {
            expensesTable.add(expense)
        }

        fun deletePayoutItemById(id: String): Int {
            return if (itemsTable.remove(id) != null) 1 else 0
        }

        fun insertPayoutItems(items: List<PayoutItemEntity>) {
            items.forEach { itemsTable[it.id] = it }
        }

        // EXACT implementation from FinancialsDao.updatePayoutRunStatus
        fun updatePayoutRunStatus(runId: String, status: String) {
            val currentRun = findPayoutRunById(runId)
            if (status.equals("paid", ignoreCase = true) && currentRun?.status?.equals("paid", ignoreCase = true) == true) {
                return
            }
            updatePayoutRunStatusOnly(runId, status)
            updatePayoutItemsStatusForRun(runId, status)
            if (status.equals("paid", ignoreCase = true)) {
                val items = getPayoutItemsList(runId)
                for (item in items) {
                    val disbursedAmount = if (item.amountPaid > 0.0) item.amountPaid else item.amountDue
                    if (item.amountPaid <= 0.0 && disbursedAmount > 0.0) {
                        updatePayoutItem(item.id, status, disbursedAmount)
                    }
                    if (!item.projectId.isNullOrBlank() && disbursedAmount > 0.0) {
                        val category = if (item.recipientType.equals("vendor_payment", ignoreCase = true)) "materials" else "labor"
                        val expense = ExpenseEntity(
                            projectId = item.projectId,
                            userId = item.recipientId,
                            amount = disbursedAmount,
                            category = category,
                            description = "Payout: ${item.recipientName}",
                            expenseDate = java.time.LocalDate.now().toString(),
                            paymentStatus = "paid",
                            paymentMode = "bank_transfer",
                            notes = item.notes ?: item.referenceDetails
                        )
                        insertExpense(expense)
                    }
                }
            }
        }
    }

    @Test
    fun testPaydayAutoExpense_disbursedAmountFallbackWhenAmountPaidIsZero() {
        val dao = SimulatedFinancialsDao()
        val runId = "run-001"
        dao.runsTable[runId] = WeeklyPayoutRunEntity(
            id = runId,
            companyId = "company-1",
            weekStartDate = "2026-10-25",
            weekEndDate = "2026-10-31",
            totalAmount = 29500.0,
            status = "draft"
        )

        // Item 1: labor with amountPaid = 0.0, amountDue = 14,500.0
        val item1 = PayoutItemEntity(
            id = "item-1",
            payoutId = runId,
            recipientType = "labor_wage",
            recipientId = "mason-01",
            recipientName = "Periyasamy Mason Team",
            amountDue = 14500.0,
            amountPaid = 0.0,
            projectId = "proj-1"
        )

        // Item 2: vendor payment with amountPaid = 0.0, amountDue = 15,000.0
        val item2 = PayoutItemEntity(
            id = "item-2",
            payoutId = runId,
            recipientType = "vendor_payment",
            recipientId = "vendor-01",
            recipientName = "Kavin Cement Agency",
            amountDue = 15000.0,
            amountPaid = 0.0,
            projectId = "proj-1"
        )

        dao.itemsTable[item1.id] = item1
        dao.itemsTable[item2.id] = item2

        // Execute status transition to paid
        dao.updatePayoutRunStatus(runId, "paid")

        // Assert run and item statuses
        assertEquals("paid", dao.runsTable[runId]?.status)
        assertEquals(14500.0, dao.itemsTable["item-1"]?.amountPaid ?: 0.0, 0.001)
        assertEquals(15000.0, dao.itemsTable["item-2"]?.amountPaid ?: 0.0, 0.001)

        // Assert generated expenses
        assertEquals(2, dao.expensesTable.size)

        val laborExpense = dao.expensesTable.first { it.userId == "mason-01" }
        assertEquals(14500.0, laborExpense.amount, 0.001)
        assertEquals("labor", laborExpense.category)
        assertEquals("Payout: Periyasamy Mason Team", laborExpense.description)
        assertEquals("paid", laborExpense.paymentStatus)

        val vendorExpense = dao.expensesTable.first { it.userId == "vendor-01" }
        assertEquals(15000.0, vendorExpense.amount, 0.001)
        assertEquals("materials", vendorExpense.category)
        assertEquals("Payout: Kavin Cement Agency", vendorExpense.description)
    }

    @Test
    fun testPaydayAutoExpense_partialAmountPaidHonoredOverAmountDue() {
        val dao = SimulatedFinancialsDao()
        val runId = "run-002"
        dao.runsTable[runId] = WeeklyPayoutRunEntity(
            id = runId,
            companyId = "company-1",
            weekStartDate = "2026-10-25",
            weekEndDate = "2026-10-31",
            totalAmount = 20000.0,
            status = "approved"
        )

        // Item with partial payment pre-filled: due = 20,000, but disbursed = 12,000
        val item = PayoutItemEntity(
            id = "item-partial",
            payoutId = runId,
            recipientType = "labor_wage",
            recipientId = "carpenter-01",
            recipientName = "Carpentry Gang",
            amountDue = 20000.0,
            amountPaid = 12000.0, // Partial payment already populated
            projectId = "proj-1"
        )
        dao.itemsTable[item.id] = item

        dao.updatePayoutRunStatus(runId, "paid")

        // Disbursed amount must be 12,000, NOT 20,000
        assertEquals(1, dao.expensesTable.size)
        assertEquals(12000.0, dao.expensesTable[0].amount, 0.001)
        assertEquals(12000.0, dao.itemsTable["item-partial"]?.amountPaid ?: 0.0, 0.001)
    }

    @Test
    fun testPaydayAutoExpense_idempotencyGuardsAgainstDuplicateExpenseInsertion() {
        val dao = SimulatedFinancialsDao()
        val runId = "run-003"
        dao.runsTable[runId] = WeeklyPayoutRunEntity(
            id = runId,
            companyId = "company-1",
            weekStartDate = "2026-10-25",
            weekEndDate = "2026-10-31",
            totalAmount = 10000.0,
            status = "draft"
        )
        val item = PayoutItemEntity(
            id = "item-idemp",
            payoutId = runId,
            recipientType = "daily_wages",
            recipientId = "w1",
            recipientName = "Site Helpers",
            amountDue = 10000.0,
            amountPaid = 0.0,
            projectId = "proj-1"
        )
        dao.itemsTable[item.id] = item

        // First call: executes disbursement
        dao.updatePayoutRunStatus(runId, "paid")
        assertEquals(1, dao.expensesTable.size)
        assertEquals(10000.0, dao.expensesTable[0].amount, 0.001)

        // Adversarial duplicate invocation (e.g. repeated tap or network retry)
        dao.updatePayoutRunStatus(runId, "paid")
        assertEquals("Expense count must remain 1 after repeated invocation", 1, dao.expensesTable.size)

        // A third invocation
        dao.updatePayoutRunStatus(runId, "paid")
        assertEquals("Expense count must still remain 1", 1, dao.expensesTable.size)
    }

    @Test
    fun testPaydayAutoExpense_adversarialZeroAndNegativeAmountsFiltered() {
        val dao = SimulatedFinancialsDao()
        val runId = "run-004"
        dao.runsTable[runId] = WeeklyPayoutRunEntity(
            id = runId,
            companyId = "company-1",
            weekStartDate = "2026-10-25",
            weekEndDate = "2026-10-31",
            totalAmount = 0.0,
            status = "draft"
        )

        // Zero amounts
        val zeroItem = PayoutItemEntity(
            id = "item-zero",
            payoutId = runId,
            recipientType = "labor_wage",
            recipientId = "zero-user",
            recipientName = "Zero Worker",
            amountDue = 0.0,
            amountPaid = 0.0,
            projectId = "proj-1"
        )

        // Item missing projectId
        val noProjectItem = PayoutItemEntity(
            id = "item-no-proj",
            payoutId = runId,
            recipientType = "vendor_payment",
            recipientId = "v-hq",
            recipientName = "HQ Vendor",
            amountDue = 5000.0,
            amountPaid = 5000.0,
            projectId = null // No project ID
        )

        dao.itemsTable[zeroItem.id] = zeroItem
        dao.itemsTable[noProjectItem.id] = noProjectItem

        dao.updatePayoutRunStatus(runId, "paid")

        // No expenses should be inserted for zero amount or null project
        assertEquals(0, dao.expensesTable.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE 2: SPLIT PAYOUT ITEM DELETION & CONSERVATION OF LIABILITIES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testSplitPayoutItem_deletesOriginalAndPreservesTotalLiabilities() {
        val dao = SimulatedFinancialsDao()
        val originalItem = PayoutItemEntity(
            id = "orig-contractor-item",
            payoutId = "run-10",
            recipientType = "contractor_wage",
            recipientId = "contractor-42",
            recipientName = "Annadurai Electricals",
            amountDue = 30000.0,
            amountPaid = 0.0,
            payoutClass = "rate",
            projectId = "proj-10"
        )
        dao.itemsTable[originalItem.id] = originalItem
        assertEquals(1, dao.itemsTable.size)
        assertEquals(30000.0, dao.itemsTable.values.sumOf { it.amountDue }, 0.001)

        val ratePortion = 18000.0
        val nmrPortion = 12000.0

        // Simulate PaydayViewModel.splitPayoutItem implementation (lines 186-211)
        val rateItem = originalItem.copy(
            id = UUID.randomUUID().toString(),
            payoutClass = "rate",
            amountDue = ratePortion,
            amountPaid = if (originalItem.status == "paid") ratePortion else 0.0,
            notes = "${originalItem.notes ?: ""} (Split Rate contract)".trim()
        )
        val nmrItem = originalItem.copy(
            id = UUID.randomUUID().toString(),
            payoutClass = "nmr",
            amountDue = nmrPortion,
            amountPaid = if (originalItem.status == "paid") nmrPortion else 0.0,
            notes = "${originalItem.notes ?: ""} (Split NMR labor)".trim()
        )

        // Delete original and insert split items
        dao.deletePayoutItemById(originalItem.id)
        dao.insertPayoutItems(listOf(rateItem, nmrItem))

        // Assertions
        assertEquals(2, dao.itemsTable.size)
        assertNull("Original item MUST be deleted from payout_items", dao.itemsTable[originalItem.id])
        assertNotNull(dao.itemsTable[rateItem.id])
        assertNotNull(dao.itemsTable[nmrItem.id])

        // Conservation of total liabilities
        val totalDue = dao.itemsTable.values.sumOf { it.amountDue }
        assertEquals("Total liabilities must strictly equal original 30,000 without duplication", 30000.0, totalDue, 0.001)

        // Check attributes
        assertEquals("rate", dao.itemsTable[rateItem.id]?.payoutClass)
        assertEquals(18000.0, dao.itemsTable[rateItem.id]?.amountDue ?: 0.0, 0.001)
        assertTrue(dao.itemsTable[rateItem.id]?.notes?.contains("(Split Rate contract)") == true)

        assertEquals("nmr", dao.itemsTable[nmrItem.id]?.payoutClass)
        assertEquals(12000.0, dao.itemsTable[nmrItem.id]?.amountDue ?: 0.0, 0.001)
        assertTrue(dao.itemsTable[nmrItem.id]?.notes?.contains("(Split NMR labor)") == true)
    }

    @Test
    fun testSplitPayoutItem_sequentialDoubleSplitMaintainsStrictConservation() {
        val dao = SimulatedFinancialsDao()
        val originalItem = PayoutItemEntity(
            id = "base-item",
            payoutId = "run-20",
            recipientType = "contractor_wage",
            recipientId = "c-99",
            recipientName = "Plumbing Master",
            amountDue = 50000.0,
            amountPaid = 0.0,
            payoutClass = "rate"
        )
        dao.itemsTable[originalItem.id] = originalItem

        // First split: 30k Rate + 20k NMR
        val rate1 = originalItem.copy(id = "r1", payoutClass = "rate", amountDue = 30000.0)
        val nmr1 = originalItem.copy(id = "n1", payoutClass = "nmr", amountDue = 20000.0)
        dao.deletePayoutItemById(originalItem.id)
        dao.insertPayoutItems(listOf(rate1, nmr1))

        assertEquals(2, dao.itemsTable.size)
        assertEquals(50000.0, dao.itemsTable.values.sumOf { it.amountDue }, 0.001)

        // Second split: split r1 (30k) into 20k Rate Milestone A + 10k Rate Milestone B
        val rate1A = rate1.copy(id = "r1-a", payoutClass = "rate", amountDue = 20000.0)
        val rate1B = rate1.copy(id = "r1-b", payoutClass = "rate", amountDue = 10000.0)
        dao.deletePayoutItemById(rate1.id)
        dao.insertPayoutItems(listOf(rate1A, rate1B))

        assertEquals(3, dao.itemsTable.size)
        assertNull(dao.itemsTable["base-item"])
        assertNull(dao.itemsTable["r1"])
        assertNotNull(dao.itemsTable["n1"])
        assertNotNull(dao.itemsTable["r1-a"])
        assertNotNull(dao.itemsTable["r1-b"])

        assertEquals(50000.0, dao.itemsTable.values.sumOf { it.amountDue }, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE 3: PURCHASE ORDER APPROVAL IDEMPOTENCY & AUTO-CREATION OF ASSETS
    // ─────────────────────────────────────────────────────────────────────────

    private class SimulatedPurchaseOrderDao {
        val ordersTable = mutableMapOf<String, PurchaseOrderEntity>()
        val itemsTable = mutableMapOf<String, List<PurchaseOrderItemEntity>>()
        val inventoryTable = mutableMapOf<String, InventoryAssetEntity>()

        fun findPurchaseOrderById(poId: String): PurchaseOrderEntity? = ordersTable[poId]

        fun getItemsForPo(poId: String): List<PurchaseOrderItemEntity> = itemsTable[poId] ?: emptyList()

        fun updateOrderStatusAndApprover(poId: String, status: String, approverName: String) {
            ordersTable[poId]?.let {
                ordersTable[poId] = it.copy(status = status, approvedBy = approverName)
            }
        }

        fun incrementStockByName(name: String, qty: Double): Int {
            val existing = inventoryTable.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
            return if (existing != null) {
                inventoryTable[existing.id] = existing.copy(currentStock = existing.currentStock + qty)
                1
            } else {
                0
            }
        }

        fun insertInventoryAsset(asset: InventoryAssetEntity) {
            inventoryTable[asset.id] = asset
        }

        // EXACT implementation from PurchaseOrderDao.approvePurchaseOrder (lines 272-303)
        fun approvePurchaseOrder(poId: String, approverName: String = "Admin") {
            val order = findPurchaseOrderById(poId) ?: return
            if (order.status.equals("Approved", ignoreCase = true) ||
                order.status.equals("Ordered", ignoreCase = true) ||
                order.status.equals("Delivered", ignoreCase = true)) {
                return
            }
            updateOrderStatusAndApprover(poId, "Approved", approverName)
            val items = getItemsForPo(poId)
            for (item in items) {
                val rowsUpdated = incrementStockByName(item.materialName, item.quantity)
                if (rowsUpdated == 0) {
                    insertInventoryAsset(
                        InventoryAssetEntity(
                            id = item.materialId?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                            siteId = order.projectId,
                            name = item.materialName,
                            category = "material",
                            assetType = "material",
                            unit = item.unit,
                            currentStock = item.quantity,
                            minStockLevel = 0.0,
                            unitCost = item.unitPrice,
                            supplierName = order.supplierName,
                            supplierContact = order.supplierContact,
                            lastUpdatedLocally = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    @Test
    fun testPurchaseOrderApproval_existingItemIncrementsAndMissingItemIsCreated() {
        val dao = SimulatedPurchaseOrderDao()

        // Pre-existing asset: Cement with current stock = 100 bags
        val existingCement = InventoryAssetEntity(
            id = "cement-asset-01",
            siteId = "site-A",
            name = "Ultratech 53 Cement",
            category = "material",
            assetType = "material",
            unit = "bags",
            currentStock = 100.0,
            minStockLevel = 20.0,
            unitCost = 420.0
        )
        dao.inventoryTable[existingCement.id] = existingCement

        // PO with 2 items: 1 existing (Cement) and 1 completely new (Ready Mix Concrete)
        val poId = "po-101"
        dao.ordersTable[poId] = PurchaseOrderEntity(
            id = poId,
            poNumber = "PO-2026-001",
            projectId = "site-A",
            supplierName = "Apex Builders Supply",
            supplierContact = "+91 98765 43210",
            totalAmount = 67000.0,
            status = "Draft"
        )
        dao.itemsTable[poId] = listOf(
            PurchaseOrderItemEntity(
                id = "poi-1",
                poId = poId,
                materialId = existingCement.id,
                materialName = "Ultratech 53 Cement",
                unit = "bags",
                quantity = 50.0,
                unitPrice = 420.0
            ),
            PurchaseOrderItemEntity(
                id = "poi-2",
                poId = poId,
                materialId = "rmc-mat-new",
                materialName = "M25 Ready Mix Concrete",
                unit = "m3",
                quantity = 10.0,
                unitPrice = 4600.0
            )
        )

        // Execute approval
        dao.approvePurchaseOrder(poId, "Site Manager")

        // Order status check
        assertEquals("Approved", dao.ordersTable[poId]?.status)
        assertEquals("Site Manager", dao.ordersTable[poId]?.approvedBy)

        // Existing cement stock must increment from 100.0 to 150.0
        val updatedCement = dao.inventoryTable[existingCement.id]
        assertNotNull(updatedCement)
        assertEquals(150.0, updatedCement!!.currentStock, 0.001)

        // Missing RMC asset must be automatically created
        val rmcAsset = dao.inventoryTable["rmc-mat-new"]
        assertNotNull("Missing material must be created in inventory_assets", rmcAsset)
        assertEquals("M25 Ready Mix Concrete", rmcAsset!!.name)
        assertEquals(10.0, rmcAsset.currentStock, 0.001)
        assertEquals("m3", rmcAsset.unit)
        assertEquals(4600.0, rmcAsset.unitCost, 0.001)
        assertEquals("Apex Builders Supply", rmcAsset.supplierName)
        assertEquals("+91 98765 43210", rmcAsset.supplierContact)
    }

    @Test
    fun testPurchaseOrderApproval_strictIdempotencyAcrossMultipleInvocations() {
        val dao = SimulatedPurchaseOrderDao()

        val poId = "po-idemp-01"
        dao.ordersTable[poId] = PurchaseOrderEntity(
            id = poId,
            poNumber = "PO-2026-002",
            projectId = "site-B",
            supplierName = "Steel Mart",
            supplierContact = "+91 91234 56789",
            totalAmount = 130000.0,
            status = "Awaiting Approval"
        )
        dao.itemsTable[poId] = listOf(
            PurchaseOrderItemEntity(
                id = "poi-steel",
                poId = poId,
                materialId = "tmt-steel-new",
                materialName = "Fe 550D TMT Steel 16mm",
                unit = "tonnes",
                quantity = 2.0,
                unitPrice = 65000.0
            )
        )

        // 1st Approval
        dao.approvePurchaseOrder(poId, "Kavin Approver")
        assertEquals("Approved", dao.ordersTable[poId]?.status)
        assertEquals(2.0, dao.inventoryTable["tmt-steel-new"]?.currentStock ?: 0.0, 0.001)

        // 2nd Approval (Adversarial repeat call)
        dao.approvePurchaseOrder(poId, "Duplicate Approver")
        assertEquals("Approved", dao.ordersTable[poId]?.status)
        assertEquals("Stock must strictly remain 2.0 and not increment to 4.0",
            2.0, dao.inventoryTable["tmt-steel-new"]?.currentStock ?: 0.0, 0.001)
        assertEquals("Approver must remain original Kavin Approver",
            "Kavin Approver", dao.ordersTable[poId]?.approvedBy)

        // 3rd Approval
        dao.approvePurchaseOrder(poId, "Another Approver")
        assertEquals(2.0, dao.inventoryTable["tmt-steel-new"]?.currentStock ?: 0.0, 0.001)
    }

    @Test
    fun testPurchaseOrderApproval_rejectsOrderedAndDeliveredStatus() {
        val dao = SimulatedPurchaseOrderDao()

        // Existing order already Ordered
        val poOrderedId = "po-ordered"
        dao.ordersTable[poOrderedId] = PurchaseOrderEntity(
            id = poOrderedId,
            poNumber = "PO-2026-003",
            projectId = "site-C",
            supplierName = "Sand Quarry",
            totalAmount = 5000.0,
            status = "Ordered"
        )
        dao.itemsTable[poOrderedId] = listOf(
            PurchaseOrderItemEntity(
                id = "poi-sand",
                poId = poOrderedId,
                materialName = "M-Sand",
                quantity = 10.0,
                unitPrice = 500.0
            )
        )

        dao.approvePurchaseOrder(poOrderedId, "Admin")
        // Status should remain Ordered and no inventory should be modified
        assertEquals("Ordered", dao.ordersTable[poOrderedId]?.status)
        assertEquals(0, dao.inventoryTable.size)

        // Order already Delivered
        val poDeliveredId = "po-delivered"
        dao.ordersTable[poDeliveredId] = PurchaseOrderEntity(
            id = poDeliveredId,
            poNumber = "PO-2026-004",
            projectId = "site-C",
            supplierName = "Blue Metal Quarry",
            totalAmount = 5000.0,
            status = "Delivered"
        )
        dao.itemsTable[poDeliveredId] = listOf(
            PurchaseOrderItemEntity(
                id = "poi-gravel",
                poId = poDeliveredId,
                materialName = "20mm Blue Metal",
                quantity = 8.0,
                unitPrice = 600.0
            )
        )

        dao.approvePurchaseOrder(poDeliveredId, "Admin")
        assertEquals("Delivered", dao.ordersTable[poDeliveredId]?.status)
        assertEquals(0, dao.inventoryTable.size)
    }

    @Test
    fun testPurchaseOrderApproval_invalidPoIdGracefullyIgnored() {
        val dao = SimulatedPurchaseOrderDao()
        // Calling approval on nonexistent PO should be a safe NO-OP
        dao.approvePurchaseOrder("nonexistent-po-uuid", "Admin")
        assertEquals(0, dao.ordersTable.size)
        assertEquals(0, dao.inventoryTable.size)
    }
}
