package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.ProjectEntity
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.fakes.FakeAppDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProjectRepositoryLogicTest {

    private lateinit var fakeDao: FakeAppDao
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        fakeDao = FakeAppDao()
        repository = ProjectRepository(fakeDao)
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    @After
    fun tearDown() {
        SupabaseClient.authToken = null
        SupabaseClient.userId = null
    }

    @Test
    fun testProjectsFlow_mapsEntitiesToDtos() = runTest {
        val entity = ProjectEntity(
            id = "proj-1",
            name = "Commercial Complex",
            description = "Retail plaza with basement parking",
            startDate = "2026-02-01",
            endDate = "2026-12-31",
            status = "In Progress",
            progress = 40,
            clientName = "Apex Properties",
            clientPhone = "9123456780",
            location = "OMR IT Corridor, Chennai",
            budget = 120000000.0,
            companyId = "apex-corp"
        )

        fakeDao.insertProjects(listOf(entity))

        val list = repository.projects.first()
        assertEquals(1, list.size)
        val dto = list[0]
        assertEquals("proj-1", dto.id)
        assertEquals("Commercial Complex", dto.name)
        assertEquals("Retail plaza with basement parking", dto.description)
        assertEquals(40, dto.progress)
        assertEquals(120000000.0, dto.budget, 0.001)
        assertEquals("Apex Properties", dto.clientName)
        assertEquals("9123456780", dto.clientPhone)
    }

    @Test
    fun testProjectsFlow_emptyState() = runTest {
        val list = repository.projects.first()
        assertTrue("Initial projects list should be empty", list.isEmpty())
    }

    @Test
    fun testRefreshProjects_unauthenticatedFails() = runTest {
        SupabaseClient.authToken = null
        val result = repository.refreshProjects()
        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun testGetDailyWorklogs_unauthenticatedFails() = runTest {
        SupabaseClient.authToken = null
        val result = repository.getDailyWorklogs("proj-1")
        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun testCreateWorklog_unauthenticatedFails() = runTest {
        SupabaseClient.authToken = null
        val result = repository.createWorklog("proj-1", "2026-11-01", emptyList())
        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun testGetEmployees_unauthenticatedFails() = runTest {
        SupabaseClient.authToken = null
        val result = repository.getEmployees()
        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun testGetPurchaseOrders_unauthenticatedFails() = runTest {
        SupabaseClient.authToken = null
        val result = repository.getPurchaseOrders("proj-1")
        assertTrue(result.isFailure)
        assertEquals("Not authenticated", result.exceptionOrNull()?.message)
    }
}
