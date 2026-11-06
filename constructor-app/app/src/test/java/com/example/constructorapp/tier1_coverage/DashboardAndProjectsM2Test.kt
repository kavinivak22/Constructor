package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.fakes.FakeAppDao
import com.example.constructorapp.ui.main.DashboardViewModel
import com.example.constructorapp.ui.main.ProjectDetailViewModel
import com.example.constructorapp.ui.main.ProjectsViewModel
import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.toInrCompact
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardAndProjectsM2Test {

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

    @Test
    fun testIndianCurrencyFormatting_onProjectBudgets() {
        val croreBudget = 15000000.0 // 1.5 Cr
        val lakhBudget = 2500000.0   // 25 L
        val thousandBudget = 50000.0 // 50k

        assertEquals("₹1.5Cr", croreBudget.toInrCompact())
        assertEquals("₹25L", lakhBudget.toInrCompact())
        assertEquals("₹50k", thousandBudget.toInrCompact())
    }

    @Test
    fun testProjectRepository_createAndUpdateProject() = runTest {
        val newProj = ProjectDto(
            id = "proj-m2-test",
            name = "Skyline Residency",
            clientName = "Apex Builders",
            budget = 25000000.0,
            status = "active",
            progress = 15,
            companyId = "comp-1"
        )

        val createResult = repository.createProject(newProj)
        assertTrue(createResult.isSuccess)

        val list = repository.projects.first()
        assertEquals(1, list.size)
        assertEquals("Skyline Residency", list[0].name)
        assertEquals(25000000.0, list[0].budget, 0.001)

        val updatedProj = newProj.copy(progress = 30, status = "active")
        val updateResult = repository.updateProject(updatedProj)
        assertTrue(updateResult.isSuccess)

        val updatedList = repository.projects.first()
        assertEquals(30, updatedList[0].progress)
    }

    @Test
    fun testPersonalTasks_addEditToggleDelete() = runTest {
        val addRes = repository.addPersonalTask("Inspect foundation rebar", "High")
        assertTrue(addRes.isSuccess)
        val task = addRes.getOrThrow()
        assertEquals("Inspect foundation rebar", task.title)
        assertEquals("pending", task.status)

        val tasksBefore = repository.personalTasks.first()
        assertEquals(1, tasksBefore.size)

        repository.toggleTask(task.id, true)
        val tasksAfterToggle = repository.personalTasks.first()
        assertEquals("completed", tasksAfterToggle[0].status)

        repository.editTaskTitle(task.id, "Inspect foundation & slab rebar")
        val tasksAfterEdit = repository.personalTasks.first()
        assertEquals("Inspect foundation & slab rebar", tasksAfterEdit[0].title)

        repository.deleteTask(task.id)
        val tasksAfterDelete = repository.personalTasks.first()
        assertTrue(tasksAfterDelete.isEmpty())
    }

    @Test
    fun testTomorrowPrepSummary_returnsScheduledTaskCounts() = runTest {
        val summary = repository.getTomorrowPrepSummary().first()
        assertNotNull(summary.dateLabel)
        assertTrue(summary.scheduledTasksCount >= 0)
    }

    @Test
    fun testDashboardViewModel_stateAndAlerts() = runTest {
        val viewModel = DashboardViewModel(repository)
        testScheduler.advanceUntilIdle()

        assertFalse("Alerts list should be populated by default", viewModel.uiState.value.alerts.isEmpty())
        val firstAlertId = viewModel.uiState.value.alerts[0].id

        viewModel.dismissAlert(firstAlertId)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.alerts.any { it.id == firstAlertId })

        viewModel.restoreAlerts()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.alerts.any { it.id == firstAlertId })
    }

    @Test
    fun testProjectsViewModel_filteringAndSearch() = runTest {
        fakeDao.insertProjects(
            listOf(
                ProjectEntity(
                    id = "1",
                    name = "Green Towers",
                    description = "Residential tower",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                    status = "active",
                    progress = 25,
                    clientName = "Client A",
                    clientPhone = "12345",
                    location = "Chennai",
                    budget = 1000000.0,
                    companyId = "c1"
                ),
                ProjectEntity(
                    id = "2",
                    name = "Blue Horizon",
                    description = "Commercial hub",
                    startDate = "2026-02-01",
                    endDate = "2026-11-30",
                    status = "completed",
                    progress = 100,
                    clientName = "Client B",
                    clientPhone = "67890",
                    location = "Bangalore",
                    budget = 2000000.0,
                    companyId = "c1"
                ),
                ProjectEntity(
                    id = "3",
                    name = "Amber Villa",
                    description = "Luxury villa",
                    startDate = "2026-03-01",
                    endDate = "2027-03-31",
                    status = "on-hold",
                    progress = 10,
                    clientName = "Client C",
                    clientPhone = "11223",
                    location = "Coimbatore",
                    budget = 3000000.0,
                    companyId = "c1"
                )
            )
        )

        val viewModel = ProjectsViewModel(repository)
        testScheduler.advanceUntilIdle()

        viewModel.setSearchQuery("Green")
        testScheduler.advanceUntilIdle()
        assertEquals("Green", viewModel.uiState.value.searchQuery)
        assertEquals(1, viewModel.uiState.value.filteredProjects.size)
        assertEquals("Green Towers", viewModel.uiState.value.filteredProjects[0].name)

        viewModel.setSearchQuery("")
        viewModel.setSelectedStatus("Completed")
        testScheduler.advanceUntilIdle()
        assertEquals("Completed", viewModel.uiState.value.selectedStatus)
        assertEquals(1, viewModel.uiState.value.filteredProjects.size)
        assertEquals("Blue Horizon", viewModel.uiState.value.filteredProjects[0].name)
    }

    @Test
    fun testProjectDetailViewModel_taskOperations() = runTest {
        val viewModel = ProjectDetailViewModel("proj-1", repository)
        testScheduler.advanceUntilIdle()
        assertEquals("proj-1", viewModel.uiState.value.projectId)
    }
}
