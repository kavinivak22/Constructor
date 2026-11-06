package com.example.constructorapp.tier2_boundary

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.remote.PurchaseOrderItemDto
import org.junit.Assert.*
import org.junit.Test

class ExtremeNumericBoundariesTest {

    @Test
    fun testExtremeBudgets_zeroAndNegative() {
        val zeroBudgetProject = ProjectEntity(
            id = "p-zero",
            name = "Zero Budget Pro Bono Site",
            description = null,
            startDate = null,
            endDate = null,
            status = "Planning",
            progress = 0,
            clientName = null,
            location = null,
            budget = 0.0,
            companyId = "c1"
        )
        assertEquals(0.0, zeroBudgetProject.budget, 0.001)

        val negativeBudgetProject = zeroBudgetProject.copy(
            id = "p-neg",
            budget = -50000.0
        )
        assertEquals(-50000.0, negativeBudgetProject.budget, 0.001)
        assertTrue("Budget is negative", negativeBudgetProject.budget < 0.0)
    }

    @Test
    fun testExtremeBudgets_megaInfrastructureScale() {
        // 100 Crore = 1,000,000,000 INR
        val megaBudget = 1_000_000_000.0
        val megaProject = ProjectEntity(
            id = "p-mega",
            name = "Metro Rail Elevated Corridor",
            description = "10km viaduct and 8 stations",
            startDate = "2026-01-01",
            endDate = "2030-12-31",
            status = "Active",
            progress = 12,
            clientName = "Metro Rail Corp",
            location = "Chennai Phase 2",
            budget = megaBudget,
            companyId = "infra-corp"
        )

        assertEquals(1e9, megaProject.budget, 0.001)
        assertTrue(megaProject.budget > 10_000_000.0)
    }

    @Test
    fun testConcreteCalculation_zeroDimensions() {
        fun calculateVolume(l: Double, w: Double, d: Double): Double {
            if (l <= 0.0 || w <= 0.0 || d <= 0.0) return 0.0
            return l * w * d
        }

        assertEquals(0.0, calculateVolume(0.0, 15.0, 0.5), 0.001)
        assertEquals(0.0, calculateVolume(20.0, 0.0, 0.5), 0.001)
        assertEquals(0.0, calculateVolume(20.0, 15.0, 0.0), 0.001)
        assertEquals(0.0, calculateVolume(-10.0, 15.0, 0.5), 0.001)
        assertEquals(0.0, calculateVolume(20.0, -5.0, 0.5), 0.001)
    }

    @Test
    fun testConcreteCalculation_microDimensions() {
        // Small patch work: 0.1 ft x 0.1 ft x 0.1 ft = 0.001 cft
        val wetVolume = 0.1 * 0.1 * 0.1
        val dryVolume = wetVolume * 1.54
        val totalParts = 5.5 // M20
        val cementBags = (dryVolume * (1.0 / totalParts)) / 1.226

        assertTrue(wetVolume > 0.0)
        assertTrue(cementBags > 0.0)
        assertTrue(cementBags < 0.01) // Micro fractional bag
    }

    @Test
    fun testPurchaseOrderItem_zeroAndNegativeQuantities() {
        val zeroQtyItem = PurchaseOrderItemDto(
            materialName = "Wire Nails",
            quantity = 0.0,
            unitPrice = 120.0,
            unit = "kg"
        )
        assertEquals(0.0, zeroQtyItem.totalPrice, 0.001)

        val negativeQtyItem = PurchaseOrderItemDto(
            materialName = "Returned Cement Bags",
            quantity = -10.0,
            unitPrice = 410.0,
            unit = "bags"
        )
        assertEquals(-4100.0, negativeQtyItem.totalPrice, 0.001)
    }

    @Test
    fun testRebarSteel_zeroVolume() {
        val rccVolume = 0.0
        val steelDensity = 7850.0
        val percentage = 0.01 // 1%
        val totalSteelKg = rccVolume * percentage * steelDensity
        val bindingWire = totalSteelKg * 0.01

        assertEquals(0.0, totalSteelKg, 0.001)
        assertEquals(0.0, bindingWire, 0.001)
    }

    @Test
    fun testRateWork_extremeRatesAndQuantities() {
        fun computePayout(qty: Double, rate: Double): Double {
            if (qty < 0.0 || rate < 0.0) return 0.0
            return qty * rate
        }

        assertEquals(0.0, computePayout(0.0, 25.0), 0.001)
        assertEquals(0.0, computePayout(100.0, 0.0), 0.001)
        assertEquals(0.0, computePayout(-50.0, 25.0), 0.001)
        assertEquals(0.0, computePayout(50.0, -25.0), 0.001)
        assertEquals(2_500_000.0, computePayout(100_000.0, 25.0), 0.001)
    }
}
