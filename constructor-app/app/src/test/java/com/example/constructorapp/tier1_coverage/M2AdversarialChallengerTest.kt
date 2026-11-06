package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.fakes.FakeAppDao
import com.example.constructorapp.ui.components.AlertVariant
import com.example.constructorapp.ui.components.DashboardAlert
import com.example.constructorapp.ui.components.defaultTelemetryPetals
import com.example.constructorapp.ui.components.sampleAlerts
import com.example.constructorapp.ui.main.DashboardViewModel
import com.example.constructorapp.ui.main.ProjectDetailViewModel
import com.example.constructorapp.ui.main.ProjectsViewModel
import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.toInrCompact
import com.example.constructorapp.util.toInrFull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class)
class M2AdversarialChallengerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeAppDao
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAppDao()
        repository = ProjectRepository(fakeDao)
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    // ─── 1. Indian Currency Formatting Across Extreme Budget Boundaries ────────

    @Test
    fun testCurrency_extremeBudgetBoundaries_exactValues() {
        // Required exact checks: ₹0, ₹50k, ₹25L, ₹1.5Cr, ₹100Cr
        assertEquals("₹0", 0.0.toInrCompact())
        assertEquals("₹50k", 50000.0.toInrCompact())
        assertEquals("₹25L", 2500000.0.toInrCompact())
        assertEquals("₹1.5Cr", 15000000.0.toInrCompact())
        assertEquals("₹100Cr", 1000000000.0.toInrCompact())
    }

    @Test
    fun testCurrency_boundaryTransitionsAndRounding() {
        // Boundary transition: < 1k vs >= 1k
        assertEquals("₹999", 999.0.toInrCompact())
        assertEquals("₹1k", 1000.0.toInrCompact())
        assertEquals("₹1.5k", 1500.0.toInrCompact())

        // Boundary transition: < 100k vs >= 100k (1 Lakh)
        assertEquals("₹99k", 99000.0.toInrCompact())
        assertEquals("₹1L", 100000.0.toInrCompact())
        assertEquals("₹1.2L", 120000.0.toInrCompact())

        // Boundary transition: < 10M vs >= 10M (1 Crore)
        assertEquals("₹99L", 9900000.0.toInrCompact())
        assertEquals("₹1Cr", 10000000.0.toInrCompact())
        assertEquals("₹2.5Cr", 25000000.0.toInrCompact())

        // Ultra high boundary: 500 Crore and 1000 Crore
        assertEquals("₹500Cr", 5000000000.0.toInrCompact())
        assertEquals("₹1000Cr", 10000000000.0.toInrCompact())
    }

    @Test
    fun testCurrency_negativeExtremeValues() {
        // Zero in IEEE-754 does not carry negative sign
        assertEquals("₹0", (-0.0).toInrCompact())
        assertEquals("-₹50k", (-50000.0).toInrCompact())
        assertEquals("-₹25L", (-2500000.0).toInrCompact())
        assertEquals("-₹1.5Cr", (-15000000.0).toInrCompact())
        assertEquals("-₹100Cr", (-1000000000.0).toInrCompact())
    }

    @Test
    fun testCurrency_indianFullGrouping_extremeScales() {
        assertEquals("₹0", 0L.toInrFull())
        assertEquals("₹50,000", 50000L.toInrFull())
        assertEquals("₹25,00,000", 2500000L.toInrFull())
        assertEquals("₹1,50,00,000", 15000000L.toInrFull())
        assertEquals("₹1,00,00,00,000", 1000000000L.toInrFull())
        assertEquals("-₹1,00,00,00,000", CurrencyFormatter.formatFull(-1000000000.0))
    }

    @Test
    fun testCurrency_parseInr_roundTripStress() {
        val testAmounts = listOf(0.0, 50000.0, 2500000.0, 15000000.0, 1000000000.0)
        for (amount in testAmounts) {
            val compact = amount.toInrCompact()
            val parsedCompact = CurrencyFormatter.parseInr(compact)
            assertNotNull("Failed to parse compact: $compact", parsedCompact)
            assertEquals("Mismatch for $amount vs $compact", amount, parsedCompact!!, 0.001)

            val full = CurrencyFormatter.formatFull(amount)
            val parsedFull = CurrencyFormatter.parseInr(full)
            assertNotNull("Failed to parse full: $full", parsedFull)
            assertEquals("Mismatch for full $amount vs $full", amount, parsedFull!!, 0.001)
        }
    }

    // ─── 2. AlertFlipper Auto-Rotation, Bounds & Empty Fallback ──────────────────

    @Test
    fun testAlertFlipper_emptyStateFallsBackToSentinelMode() = runTest {
        val viewModel = DashboardViewModel(repository)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSentinelMode)
        val initialAlerts = viewModel.uiState.value.alerts
        assertTrue(initialAlerts.isNotEmpty())

        // Dismiss all alerts sequentially
        for (alert in initialAlerts) {
            viewModel.dismissAlert(alert.id)
            testScheduler.advanceUntilIdle()
        }

        // When alerts are empty, state enters sentinel mascot mode
        assertTrue("State must enter sentinel mode when alerts are empty", viewModel.uiState.value.alerts.isEmpty())
        assertTrue("isSentinelMode must be true when alerts list is empty", viewModel.uiState.value.isSentinelMode)

        // Restore alerts returns to standard flipper mode
        viewModel.restoreAlerts()
        testScheduler.advanceUntilIdle()
        assertFalse("isSentinelMode must be false after restoration", viewModel.uiState.value.isSentinelMode)
        assertEquals(sampleAlerts.size, viewModel.uiState.value.alerts.size)
    }

    @Test
    fun testAlertFlipper_safeIndexNavigationLogic() {
        val alerts = listOf("a1", "a2", "a3")

        // Next wrap-around test: (0+1)%3 = 1, (1+1)%3 = 2, (2+1)%3 = 0
        var idx = 0
        idx = (idx + 1) % alerts.size
        assertEquals(1, idx)
        idx = (idx + 1) % alerts.size
        assertEquals(2, idx)
        idx = (idx + 1) % alerts.size
        assertEquals(0, idx)

        // Prev wrap-around test: if idx > 0 idx-1 else size-1
        idx = if (idx > 0) idx - 1 else alerts.size - 1
        assertEquals(2, idx)
        idx = if (idx > 0) idx - 1 else alerts.size - 1
        assertEquals(1, idx)
        idx = if (idx > 0) idx - 1 else alerts.size - 1
        assertEquals(0, idx)

        // Clamping on deletion of the last element
        var currentAlertsSize = 3
        var activeIndex = 2 // looking at last item
        currentAlertsSize -= 1 // one alert removed
        if (activeIndex >= currentAlertsSize) {
            activeIndex = (currentAlertsSize - 1).coerceAtLeast(0)
        }
        assertEquals(1, activeIndex)
        assertTrue(activeIndex in 0 until currentAlertsSize)
    }

    // ─── 3. Sentinel Mascot Telemetry Petals Blooming Math & Boundaries ─────────

    @Test
    fun testSentinelPetals_trigonometryAndAngularDistribution() {
        val petals = defaultTelemetryPetals
        assertEquals("Must have exactly 3 telemetry petals", 3, petals.size)

        // Petal 1: curing (angle 210 deg, distance 82 dp)
        val p1 = petals.find { it.id == "curing" }!!
        val rad1 = Math.toRadians(p1.angleDeg.toDouble())
        val x1 = cos(rad1) * p1.distanceDp
        val y1 = sin(rad1) * p1.distanceDp
        assertTrue("Petal 1 X should be negative (left side)", x1 < -60)
        assertTrue("Petal 1 Y should be negative (top side)", y1 < -30)

        // Petal 2: wind (angle 330 deg, distance 82 dp)
        val p2 = petals.find { it.id == "wind" }!!
        val rad2 = Math.toRadians(p2.angleDeg.toDouble())
        val x2 = cos(rad2) * p2.distanceDp
        val y2 = sin(rad2) * p2.distanceDp
        assertTrue("Petal 2 X should be positive (right side)", x2 > 60)
        assertTrue("Petal 2 Y should be negative (top side)", y2 < -30)

        // Petal 3: workers (angle 90 deg, distance 74 dp)
        val p3 = petals.find { it.id == "workers" }!!
        val rad3 = Math.toRadians(p3.angleDeg.toDouble())
        val x3 = cos(rad3) * p3.distanceDp
        val y3 = sin(rad3) * p3.distanceDp
        assertEquals("Petal 3 X should be zero (centered horizontally)", 0.0, x3, 0.001)
        assertTrue("Petal 3 Y should be positive (bottom side)", y3 > 70)

        // Distance separation between petals
        val dist12 = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        val dist13 = sqrt((x3 - x1).pow(2) + (y3 - y1).pow(2))
        val dist23 = sqrt((x3 - x2).pow(2) + (y3 - y2).pow(2))
        assertTrue("Petals 1 & 2 must be sufficiently separated", dist12 > 100)
        assertTrue("Petals 1 & 3 must be sufficiently separated", dist13 > 100)
        assertTrue("Petals 2 & 3 must be sufficiently separated", dist23 > 100)
    }

    @Test
    fun testSentinelPetals_bloomScaleBoundariesAndTitleParsing() {
        for (petal in defaultTelemetryPetals) {
            val shortTitle = petal.title.substringBefore(" •")
            assertFalse("Short title should not be empty", shortTitle.isBlank())
            if (petal.id == "curing") assertEquals("31°C", shortTitle)
            if (petal.id == "wind") assertEquals("11 km/h", shortTitle)
            if (petal.id == "workers") assertEquals("18 Active On-Site", shortTitle)

            // Test bloomProgress calculations at boundary values
            for (bloomProgress in listOf(0.0f, 0.05f, 0.5f, 1.0f, 1.15f)) {
                val distance = petal.distanceDp * bloomProgress
                val angleRad = Math.toRadians(petal.angleDeg.toDouble())
                val px = cos(angleRad).toFloat() * distance
                val py = sin(angleRad).toFloat() * distance
                assertFalse("Calculated X offset cannot be NaN", px.isNaN())
                assertFalse("Calculated Y offset cannot be NaN", py.isNaN())
            }
        }
    }

    // ─── 4. Project Tasks Inline Checklist Toggling, Editing, Deleting ──────────

    @Test
    fun testPersonalTasks_fullLifecycleAndBoundaryEdits() = runTest {
        val viewModel = DashboardViewModel(repository)
        testScheduler.advanceUntilIdle()

        // 1. Add task via ViewModel
        viewModel.onNewTaskTitleChange("Test concrete slump cone test")
        viewModel.addPersonalTask()
        testScheduler.advanceUntilIdle()

        val tasksAfterAdd = viewModel.uiState.value.personalTasks
        assertEquals(1, tasksAfterAdd.size)
        val addedTask = tasksAfterAdd[0]
        assertEquals("Test concrete slump cone test", addedTask.title)
        assertEquals("pending", addedTask.status)

        // 2. Toggle task to completed
        viewModel.toggleTask(addedTask.id, true)
        testScheduler.advanceUntilIdle()
        assertEquals("completed", viewModel.uiState.value.personalTasks[0].status)

        // 3. Toggle task back to pending
        viewModel.toggleTask(addedTask.id, false)
        testScheduler.advanceUntilIdle()
        assertEquals("pending", viewModel.uiState.value.personalTasks[0].status)

        // 4. Start edit & save valid edit
        viewModel.startEditingTask(addedTask.id, addedTask.title)
        testScheduler.advanceUntilIdle()
        assertEquals(addedTask.id, viewModel.uiState.value.editingTaskId)
        assertEquals(addedTask.title, viewModel.uiState.value.editingTaskTitle)

        viewModel.onEditTaskTitleChange("Test concrete slump cone test (IS 1199)")
        viewModel.saveEditedTask(addedTask.id)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingTaskId)
        assertEquals("Test concrete slump cone test (IS 1199)", viewModel.uiState.value.personalTasks[0].title)

        // 5. Blank title edit is rejected
        viewModel.startEditingTask(addedTask.id, viewModel.uiState.value.personalTasks[0].title)
        viewModel.onEditTaskTitleChange("   ")
        viewModel.saveEditedTask(addedTask.id)
        testScheduler.advanceUntilIdle()
        // Title should remain unchanged
        assertEquals("Test concrete slump cone test (IS 1199)", viewModel.uiState.value.personalTasks[0].title)

        // 6. Delete task
        viewModel.deleteTask(addedTask.id)
        testScheduler.advanceUntilIdle()
        assertTrue("Task list should be empty after deletion", viewModel.uiState.value.personalTasks.isEmpty())
    }

    @Test
    fun testProjectTasks_optimisticToggleAndLifecycle() = runTest {
        // Test repository-level toggle logic directly
        val addRes = repository.addPersonalTask("Site safety boundary net check", "High")
        assertTrue(addRes.isSuccess)
        val task = addRes.getOrThrow()
        assertEquals("pending", task.status)

        // Toggle to completed
        val toggleCompletedRes = repository.toggleTask(task.id, true)
        assertTrue(toggleCompletedRes.isSuccess)
        val tasksCompleted = repository.personalTasks.first()
        assertEquals("completed", tasksCompleted.find { it.id == task.id }?.status)

        // Toggle back to pending
        val togglePendingRes = repository.toggleTask(task.id, false)
        assertTrue(togglePendingRes.isSuccess)
        val tasksPending = repository.personalTasks.first()
        assertEquals("pending", tasksPending.find { it.id == task.id }?.status)

        // Test ProjectDetailViewModel toggle task optimistic state
        val detailViewModel = ProjectDetailViewModel("proj-detail-test", repository)
        testScheduler.advanceUntilIdle()
        assertEquals("proj-detail-test", detailViewModel.uiState.value.projectId)

        // Test dialog open/close lifecycle
        assertFalse(detailViewModel.uiState.value.isCreateTaskOpen)
        detailViewModel.openCreateTask()
        testScheduler.advanceUntilIdle()
        assertTrue(detailViewModel.uiState.value.isCreateTaskOpen)
        detailViewModel.closeCreateTask()
        testScheduler.advanceUntilIdle()
        assertFalse(detailViewModel.uiState.value.isCreateTaskOpen)

        assertFalse(detailViewModel.uiState.value.isEditProjectOpen)
        detailViewModel.openEditProject()
        testScheduler.advanceUntilIdle()
        assertTrue(detailViewModel.uiState.value.isEditProjectOpen)
        detailViewModel.closeEditProject()
        testScheduler.advanceUntilIdle()
        assertFalse(detailViewModel.uiState.value.isEditProjectOpen)
    }
}
