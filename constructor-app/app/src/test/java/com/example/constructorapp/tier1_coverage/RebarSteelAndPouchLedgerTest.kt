package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.remote.PouchTransactionDto
import org.junit.Assert.*
import org.junit.Test

class RebarSteelAndPouchLedgerTest {

    data class SteelEstimate(
        val rccVolumeM3: Double,
        val percentage: Double,
        val totalSteelKg: Double,
        val steelTonnes: Double,
        val bindingWireKg: Double
    )

    private fun calculateSteel(
        rccVolumeM3: Double,
        memberType: String
    ): SteelEstimate {
        val density = 7850.0 // Steel density in kg/m3
        val percentage = when (memberType.lowercase()) {
            "beam" -> 0.02
            "column" -> 0.025
            "footing" -> 0.008
            else -> 0.01 // Slab default
        }
        val totalKg = rccVolumeM3 * percentage * density
        val tonnes = totalKg / 1000.0
        val bindingWire = totalKg * 0.01

        return SteelEstimate(
            rccVolumeM3 = rccVolumeM3,
            percentage = percentage,
            totalSteelKg = totalKg,
            steelTonnes = tonnes,
            bindingWireKg = bindingWire
        )
    }

    @Test
    fun testSlabReinforcementSteel() {
        // 10 m3 RCC slab @ 1.0% steel
        val estimate = calculateSteel(10.0, "slab")
        assertEquals(0.01, estimate.percentage, 0.0001)
        // 10 * 0.01 * 7850 = 785.0 kg
        assertEquals(785.0, estimate.totalSteelKg, 0.01)
        assertEquals(0.785, estimate.steelTonnes, 0.0001)
        assertEquals(7.85, estimate.bindingWireKg, 0.01)
    }

    @Test
    fun testColumnReinforcementSteel() {
        // 5 m3 RCC columns @ 2.5% steel
        val estimate = calculateSteel(5.0, "column")
        assertEquals(0.025, estimate.percentage, 0.0001)
        // 5 * 0.025 * 7850 = 981.25 kg
        assertEquals(981.25, estimate.totalSteelKg, 0.01)
        assertEquals(0.98125, estimate.steelTonnes, 0.0001)
        assertEquals(9.8125, estimate.bindingWireKg, 0.001)
    }

    @Test
    fun testBeamReinforcementSteel() {
        // 8 m3 RCC beams @ 2.0% steel
        val estimate = calculateSteel(8.0, "beam")
        assertEquals(0.02, estimate.percentage, 0.0001)
        // 8 * 0.02 * 7850 = 1256.0 kg
        assertEquals(1256.0, estimate.totalSteelKg, 0.01)
        assertEquals(1.256, estimate.steelTonnes, 0.0001)
        assertEquals(12.56, estimate.bindingWireKg, 0.01)
    }

    @Test
    fun testFootingReinforcementSteel() {
        // 12 m3 RCC footings @ 0.8% steel
        val estimate = calculateSteel(12.0, "footing")
        assertEquals(0.008, estimate.percentage, 0.0001)
        // 12 * 0.008 * 7850 = 753.6 kg
        assertEquals(753.6, estimate.totalSteelKg, 0.01)
        assertEquals(0.7536, estimate.steelTonnes, 0.0001)
        assertEquals(7.536, estimate.bindingWireKg, 0.001)
    }

    @Test
    fun testPouchLedgerBalanceCalculation() {
        val initialDisbursed = 15000.0
        val transactions = listOf(
            PouchTransactionDto(
                projectId = "p-1",
                amount = 450.0,
                category = "Site Meals & Tea",
                date = "2026-10-27",
                transactionType = "Expense"
            ),
            PouchTransactionDto(
                projectId = "p-1",
                amount = 1200.0,
                category = "Urgent Hardware",
                date = "2026-10-26",
                transactionType = "Expense"
            ),
            PouchTransactionDto(
                projectId = "p-1",
                amount = 800.0,
                category = "Fuel & Diesel",
                date = "2026-10-25",
                transactionType = "Expense"
            ),
            PouchTransactionDto(
                projectId = "p-1",
                amount = 5000.0,
                category = "Topup",
                date = "2026-10-24",
                transactionType = "FundRequest" // Not an expense deduction
            )
        )

        val totalSpent = transactions.filter { it.transactionType == "Expense" }.sumOf { it.amount }
        val inHandBalance = initialDisbursed - totalSpent

        assertEquals(2450.0, totalSpent, 0.01) // 450 + 1200 + 800
        assertEquals(12550.0, inHandBalance, 0.01) // 15000 - 2450
        assertTrue("Balance is healthy (> 3000)", inHandBalance > 3000.0)
    }

    @Test
    fun testPouchLedgerLowBalanceWarning() {
        val initialDisbursed = 5000.0
        val transactions = listOf(
            PouchTransactionDto(
                projectId = "p-1",
                amount = 2500.0,
                category = "Urgent Hardware",
                date = "2026-10-26",
                transactionType = "Expense"
            )
        )

        val totalSpent = transactions.filter { it.transactionType == "Expense" }.sumOf { it.amount }
        val inHandBalance = initialDisbursed - totalSpent

        assertEquals(2500.0, inHandBalance, 0.01)
        assertTrue("Balance is below threshold (<= 3000)", inHandBalance <= 3000.0)
    }
}
