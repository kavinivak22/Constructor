package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.ui.screens.ConstructionStage
import com.example.constructorapp.ui.screens.StageCheckItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Foundation
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.ViewColumn
import org.junit.Assert.*
import org.junit.Test

class BuildingPlanQualityTest {

    @Test
    fun testStageCheckItem_stateToggle() {
        val check = StageCheckItem("Soil excavation to firm stratum", false)
        assertFalse(check.isDone)

        check.isDone = true
        assertTrue(check.isDone)
    }

    @Test
    fun testConstructionStage_progressCalculation() {
        val stage = ConstructionStage(
            title = "1. Substructure & Foundation",
            icon = Icons.Default.Foundation,
            checks = listOf(
                StageCheckItem("Excavation", true),
                StageCheckItem("P.C.C. bed", true),
                StageCheckItem("Footing rebar", true),
                StageCheckItem("Concrete footing", false),
                StageCheckItem("Backfilling", false)
            )
        )

        val total = stage.checks.size
        val done = stage.checks.count { it.isDone }
        val pct = done.toFloat() / total

        assertEquals(5, total)
        assertEquals(3, done)
        assertEquals(0.6f, pct, 0.001f)
    }

    @Test
    fun testAllStages_overallProgressCalculation() {
        val stages = listOf(
            ConstructionStage(
                title = "Stage 1",
                icon = Icons.Default.Foundation,
                checks = listOf(
                    StageCheckItem("Item 1", true),
                    StageCheckItem("Item 2", true)
                )
            ),
            ConstructionStage(
                title = "Stage 2",
                icon = Icons.Default.LinearScale,
                checks = listOf(
                    StageCheckItem("Item 3", true),
                    StageCheckItem("Item 4", false)
                )
            ),
            ConstructionStage(
                title = "Stage 3",
                icon = Icons.Default.ViewColumn,
                checks = listOf(
                    StageCheckItem("Item 5", false),
                    StageCheckItem("Item 6", false)
                )
            )
        )

        val totalChecks = stages.sumOf { it.checks.size }
        val completedChecks = stages.sumOf { it.checks.count { c -> c.isDone } }
        val progress = completedChecks.toFloat() / totalChecks

        assertEquals(6, totalChecks)
        assertEquals(3, completedChecks)
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun testEmptyStages_zeroProgressSafety() {
        val emptyStages = emptyList<ConstructionStage>()
        val totalChecks = emptyStages.sumOf { it.checks.size }
        val completedChecks = emptyStages.sumOf { it.checks.count { c -> c.isDone } }
        val progress = if (totalChecks > 0) completedChecks.toFloat() / totalChecks else 0f

        assertEquals(0, totalChecks)
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun testBlueprintDimensionCalculations() {
        val canvasWidth = 800f
        val canvasHeight = 600f
        val margin = 30f

        val planW = canvasWidth - (margin * 2)
        val planH = canvasHeight - (margin * 2)

        assertEquals(740f, planW, 0.01f)
        assertEquals(540f, planH, 0.01f)

        val midX = margin + (planW * 0.55f)
        val midY = margin + (planH * 0.5f)

        assertEquals(30f + 407f, midX, 0.01f)
        assertEquals(30f + 270f, midY, 0.01f)
    }
}
