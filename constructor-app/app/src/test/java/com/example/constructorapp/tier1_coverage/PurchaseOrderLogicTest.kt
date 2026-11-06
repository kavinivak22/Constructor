package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.remote.PurchaseOrderDto
import com.example.constructorapp.data.remote.PurchaseOrderItemDto
import org.junit.Assert.*
import org.junit.Test

class PurchaseOrderLogicTest {

    @Test
    fun testPurchaseOrderItem_totalPriceCalculation() {
        val item = PurchaseOrderItemDto(
            materialName = "Ultratech Cement 53 Grade",
            quantity = 150.0,
            unitPrice = 420.0,
            unit = "bags"
        )

        assertEquals("Ultratech Cement 53 Grade", item.materialName)
        assertEquals(150.0, item.quantity, 0.001)
        assertEquals(420.0, item.unitPrice, 0.001)
        assertEquals(63000.0, item.totalPrice, 0.001) // 150 * 420
    }

    @Test
    fun testPurchaseOrder_totalAmountAggregation() {
        val items = listOf(
            PurchaseOrderItemDto("OPC 53 Cement", 200.0, 410.0, "bags"), // 82000
            PurchaseOrderItemDto("River Sand", 400.0, 55.0, "cft"),       // 22000
            PurchaseOrderItemDto("20mm Blue Metal", 500.0, 48.0, "cft"),  // 24000
            PurchaseOrderItemDto("Fe 550D TMT Steel", 1000.0, 68.0, "kg") // 68000
        )

        val totalCalculated = items.sumOf { it.totalPrice }
        assertEquals(196000.0, totalCalculated, 0.001)

        val po = PurchaseOrderDto(
            id = "po-101",
            projectId = "proj-1",
            supplierName = "Sri Lakshmi Building Supplies",
            supplierContact = "9876543210",
            status = "Draft",
            totalAmount = totalCalculated,
            deliveryDate = "2026-11-05",
            specialInstructions = "Deliver in morning before 9 AM",
            items = items
        )

        assertEquals(4, po.items.size)
        assertEquals(196000.0, po.totalAmount, 0.001)
        assertEquals("Draft", po.status)
    }

    @Test
    fun testPurchaseOrder_statusTransition_draftToApproved() {
        val po = PurchaseOrderDto(
            id = "po-102",
            projectId = "proj-1",
            supplierName = "Metal Syndicate",
            status = "Draft",
            totalAmount = 50000.0
        )

        // Simulate status update
        val approvedPo = po.copy(status = "Approved")
        assertEquals("Approved", approvedPo.status)
        assertEquals(po.id, approvedPo.id)
        assertEquals(po.totalAmount, approvedPo.totalAmount, 0.001)
    }

    @Test
    fun testPurchaseOrder_statusTransition_approvedToFulfilled() {
        val po = PurchaseOrderDto(
            id = "po-103",
            projectId = "proj-1",
            supplierName = "Steel Yard",
            status = "Approved",
            totalAmount = 85000.0
        )

        val fulfilledPo = po.copy(status = "Fulfilled")
        assertEquals("Fulfilled", fulfilledPo.status)
    }

    @Test
    fun testPurchaseOrder_statusTransition_cancelled() {
        val po = PurchaseOrderDto(
            id = "po-104",
            projectId = "proj-1",
            supplierName = "Bad Vendor",
            status = "Draft",
            totalAmount = 15000.0
        )

        val cancelledPo = po.copy(status = "Cancelled")
        assertEquals("Cancelled", cancelledPo.status)
    }
}
