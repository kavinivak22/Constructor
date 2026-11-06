package com.example.constructorapp.tier1_coverage

import org.junit.Assert.*
import org.junit.Test

class CivilMixRatioCalculatorTest {

    data class ConcreteEstimate(
        val wetVolumeCft: Double,
        val dryVolumeCft: Double,
        val cementBags: Double,
        val sandCft: Double,
        val aggregateCft: Double,
        val waterLiters: Double
    )

    private fun calculateConcrete(
        lengthFt: Double,
        widthFt: Double,
        depthFt: Double,
        grade: String
    ): ConcreteEstimate {
        val wetVolumeCft = lengthFt * widthFt * depthFt
        val dryVolumeCft = wetVolumeCft * 1.54 // 54% bulk factor for dry mortar/concrete

        val (cementRatio, sandRatio, aggRatio) = when (grade.uppercase()) {
            "M15" -> Triple(1.0, 2.0, 4.0)
            "M20" -> Triple(1.0, 1.5, 3.0)
            "M25" -> Triple(1.0, 1.0, 2.0)
            "M10" -> Triple(1.0, 3.0, 6.0)
            "M7.5" -> Triple(1.0, 4.0, 8.0)
            else -> Triple(1.0, 1.5, 3.0)
        }
        val totalParts = cementRatio + sandRatio + aggRatio
        val cementBags = if (totalParts > 0) (dryVolumeCft * (cementRatio / totalParts)) / 1.226 else 0.0
        val sandCft = if (totalParts > 0) dryVolumeCft * (sandRatio / totalParts) else 0.0
        val aggregateCft = if (totalParts > 0) dryVolumeCft * (aggRatio / totalParts) else 0.0
        val waterLiters = cementBags * 28.0

        return ConcreteEstimate(
            wetVolumeCft = wetVolumeCft,
            dryVolumeCft = dryVolumeCft,
            cementBags = cementBags,
            sandCft = sandCft,
            aggregateCft = aggregateCft,
            waterLiters = waterLiters
        )
    }

    @Test
    fun testM20ConcreteCalculation_standardSlab() {
        // 20ft x 15ft slab with 0.5ft (6 inch) thickness
        val result = calculateConcrete(20.0, 15.0, 0.5, "M20")
        assertEquals(150.0, result.wetVolumeCft, 0.001)
        assertEquals(231.0, result.dryVolumeCft, 0.001) // 150 * 1.54

        // M20 total parts = 1 + 1.5 + 3 = 5.5
        // Cement volume = 231 / 5.5 = 42.0 cft
        // Cement bags = 42.0 / 1.226 = 34.2577 bags
        assertEquals(34.26, result.cementBags, 0.05)

        // Sand cft = 231 * (1.5 / 5.5) = 63.0 cft
        assertEquals(63.0, result.sandCft, 0.01)

        // Aggregate cft = 231 * (3.0 / 5.5) = 126.0 cft
        assertEquals(126.0, result.aggregateCft, 0.01)

        // Water liters = 34.2577 * 28.0 = ~959.2 liters
        assertEquals(959.2, result.waterLiters, 1.0)
    }

    @Test
    fun testM15ConcreteCalculation_footingLevel() {
        // 10ft x 10ft x 1ft = 100 cft wet volume
        val result = calculateConcrete(10.0, 10.0, 1.0, "M15")
        assertEquals(100.0, result.wetVolumeCft, 0.001)
        assertEquals(154.0, result.dryVolumeCft, 0.001)

        // M15 total parts = 1 + 2 + 4 = 7.0
        // Cement = 154 / 7 / 1.226 = 17.94 bags
        assertEquals(17.94, result.cementBags, 0.05)
        // Sand = 154 * 2 / 7 = 44.0 cft
        assertEquals(44.0, result.sandCft, 0.01)
        // Agg = 154 * 4 / 7 = 88.0 cft
        assertEquals(88.0, result.aggregateCft, 0.01)
    }

    @Test
    fun testM25ConcreteCalculation_heavyStructuralColumn() {
        // 5ft x 5ft x 10ft = 250 cft wet volume
        val result = calculateConcrete(5.0, 5.0, 10.0, "M25")
        assertEquals(250.0, result.wetVolumeCft, 0.001)
        assertEquals(385.0, result.dryVolumeCft, 0.001)

        // M25 total parts = 1 + 1 + 2 = 4.0
        // Cement = 385 / 4 / 1.226 = 78.507 bags
        assertEquals(78.51, result.cementBags, 0.05)
        // Sand = 385 * 1 / 4 = 96.25 cft
        assertEquals(96.25, result.sandCft, 0.01)
        // Agg = 385 * 2 / 4 = 192.5 cft
        assertEquals(192.5, result.aggregateCft, 0.01)
    }

    @Test
    fun testLeanMixConcrete_m10AndM75() {
        val resultM10 = calculateConcrete(10.0, 10.0, 1.0, "M10")
        // Parts: 1 + 3 + 6 = 10. Dry = 154 cft.
        // Cement = 15.4 / 1.226 = 12.56 bags
        assertEquals(12.56, resultM10.cementBags, 0.05)
        assertEquals(46.2, resultM10.sandCft, 0.01)
        assertEquals(92.4, resultM10.aggregateCft, 0.01)

        val resultM75 = calculateConcrete(10.0, 10.0, 1.0, "M7.5")
        // Parts: 1 + 4 + 8 = 13.
        assertTrue(resultM75.cementBags < resultM10.cementBags)
    }

    @Test
    fun testMetricEquivalents() {
        val volumeCft = 100.0
        val volumeM3 = volumeCft * 0.0283168
        assertEquals(2.83168, volumeM3, 0.001)

        val sandCft = 100.0
        val sandTons = sandCft * 0.045
        assertEquals(4.5, sandTons, 0.001)

        val aggCft = 100.0
        val aggTons = aggCft * 0.048
        assertEquals(4.8, aggTons, 0.001)
    }
}
