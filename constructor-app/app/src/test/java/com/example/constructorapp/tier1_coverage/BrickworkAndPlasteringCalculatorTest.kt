package com.example.constructorapp.tier1_coverage

import org.junit.Assert.*
import org.junit.Test

class BrickworkAndPlasteringCalculatorTest {

    data class BrickworkEstimate(
        val wallAreaSqFt: Double,
        val modularBricksNos: Double,
        val cementBags: Double,
        val sandCft: Double
    )

    private fun calculateBrickwork(
        lengthFt: Double,
        heightFt: Double,
        isNineInchWall: Boolean
    ): BrickworkEstimate {
        val wallArea = lengthFt * heightFt
        val bricksPerSqFt = if (isNineInchWall) 9.5 else 4.75
        val baseBricks = wallArea * bricksPerSqFt
        val totalBricksWithWaste = baseBricks * 1.05 // 5% site breakage / cutting allowance
        val cementBags = if (isNineInchWall) wallArea * 0.28 else wallArea * 0.14
        val sandCft = if (isNineInchWall) wallArea * 1.25 else wallArea * 0.62

        return BrickworkEstimate(
            wallAreaSqFt = wallArea,
            modularBricksNos = totalBricksWithWaste,
            cementBags = cementBags,
            sandCft = sandCft
        )
    }

    data class PlasteringEstimate(
        val surfaceAreaSqFt: Double,
        val cementBags: Double,
        val sandCft: Double
    )

    private fun calculatePlastering(
        surfaceAreaSqFt: Double,
        thicknessMm: Int
    ): PlasteringEstimate {
        val thicknessMultiplier = when (thicknessMm) {
            15 -> 1.25
            20 -> 1.66
            else -> 1.0 // 12mm standard internal
        }
        val cementBags = (surfaceAreaSqFt * 0.09) * thicknessMultiplier
        val sandCft = (surfaceAreaSqFt * 0.45) * thicknessMultiplier

        return PlasteringEstimate(
            surfaceAreaSqFt = surfaceAreaSqFt,
            cementBags = cementBags,
            sandCft = sandCft
        )
    }

    @Test
    fun testNineInchLoadBearingWall_brickwork() {
        // 30ft length x 10ft height = 300 sq.ft wall
        val estimate = calculateBrickwork(30.0, 10.0, isNineInchWall = true)
        assertEquals(300.0, estimate.wallAreaSqFt, 0.001)

        // 300 * 9.5 = 2850 bricks, with 5% waste = 2992.5 nos
        assertEquals(2992.5, estimate.modularBricksNos, 0.1)

        // 300 * 0.28 = 84 bags cement
        assertEquals(84.0, estimate.cementBags, 0.01)

        // 300 * 1.25 = 375 cft sand
        assertEquals(375.0, estimate.sandCft, 0.01)
    }

    @Test
    fun testFourAndHalfInchPartitionWall_brickwork() {
        // 20ft length x 10ft height = 200 sq.ft wall
        val estimate = calculateBrickwork(20.0, 10.0, isNineInchWall = false)
        assertEquals(200.0, estimate.wallAreaSqFt, 0.001)

        // 200 * 4.75 = 950 bricks, with 5% waste = 997.5 nos
        assertEquals(997.5, estimate.modularBricksNos, 0.1)

        // 200 * 0.14 = 28 bags cement
        assertEquals(28.0, estimate.cementBags, 0.01)

        // 200 * 0.62 = 124 cft sand
        assertEquals(124.0, estimate.sandCft, 0.01)
    }

    @Test
    fun testInternalPlastering12mm() {
        // 500 sq.ft surface
        val estimate = calculatePlastering(500.0, 12)
        assertEquals(500.0, estimate.surfaceAreaSqFt, 0.001)
        // 500 * 0.09 = 45 bags cement
        assertEquals(45.0, estimate.cementBags, 0.01)
        // 500 * 0.45 = 225 cft sand
        assertEquals(225.0, estimate.sandCft, 0.01)
    }

    @Test
    fun testCeilingPlastering15mm() {
        // 500 sq.ft surface with 1.25 factor
        val estimate = calculatePlastering(500.0, 15)
        assertEquals(56.25, estimate.cementBags, 0.01) // 45 * 1.25
        assertEquals(281.25, estimate.sandCft, 0.01) // 225 * 1.25
    }

    @Test
    fun testExternalWaterproofPlastering20mm() {
        // 1000 sq.ft external facade with 1.66 factor
        val estimate = calculatePlastering(1000.0, 20)
        assertEquals(149.4, estimate.cementBags, 0.05) // 1000 * 0.09 * 1.66
        assertEquals(747.0, estimate.sandCft, 0.05) // 1000 * 0.45 * 1.66
    }
}
