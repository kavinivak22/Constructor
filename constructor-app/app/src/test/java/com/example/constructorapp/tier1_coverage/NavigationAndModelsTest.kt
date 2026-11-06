package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.*
import com.example.constructorapp.data.remote.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NavigationAndModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testNavigationKeys_equalityAndProperties() {
        val detailKey1 = ProjectDetail("p-1", "Skyline Towers")
        val detailKey2 = ProjectDetail("p-1", "Skyline Towers")
        val detailKey3 = ProjectDetail("p-2", "Green Valley")

        assertEquals(detailKey1, detailKey2)
        assertNotEquals(detailKey1, detailKey3)
        assertEquals("p-1", detailKey1.projectId)
        assertEquals("Skyline Towers", detailKey1.projectName)

        val worklogKey = Worklog("p-1", "Skyline Towers")
        assertEquals("p-1", worklogKey.projectId)

        val materialKey = MaterialEstimation("p-1")
        assertEquals("p-1", materialKey.projectId)

        val poKey = PurchaseOrders("p-1")
        assertEquals("p-1", poKey.projectId)

        val planKey = BuildingPlan("p-1", "Skyline Towers")
        assertEquals("p-1", planKey.projectId)

        val pouchKey = ProjectPouch("p-1", "Skyline Towers")
        assertEquals("p-1", pouchKey.projectId)

        val reminderKey = AddEditReminder("rem-1")
        assertEquals("rem-1", reminderKey.reminderId)
    }

    @Test
    fun testProjectDto_serializationRoundtrip() {
        val project = ProjectDto(
            id = "proj-101",
            name = "Sobha City Phase 2",
            description = "G+14 Residential apartment block",
            startDate = "2026-01-15",
            endDate = "2027-06-30",
            status = "Active",
            progress = 35,
            clientName = "Sobha Developers Ltd",
            clientPhone = "9840123456",
            location = "Bangalore, KA",
            budget = 45000000.0,
            companyId = "comp-1"
        )

        val encoded = json.encodeToString(project)
        val decoded = json.decodeFromString<ProjectDto>(encoded)

        assertEquals(project.id, decoded.id)
        assertEquals(project.name, decoded.name)
        assertEquals(project.progress, decoded.progress)
        assertEquals(project.budget, decoded.budget, 0.001)
        assertEquals(project.clientPhone, decoded.clientPhone)
    }

    @Test
    fun testEmployeePermissions_defaultAndCustomValues() {
        val defaultPerms = EmployeePermissions()
        assertFalse(defaultPerms.company.manageCompany)
        assertFalse(defaultPerms.projects.createProjects)
        assertFalse(defaultPerms.expenses.approveExpenses)
        assertFalse(defaultPerms.inventory.manageInventory)

        val managerPerms = EmployeePermissions(
            company = CompanyPermissions(manageCompany = true, viewFinancials = true),
            projects = ProjectsPermissions(viewAllProjects = true, createProjects = true, editProjects = true),
            expenses = ExpensesPermissions(viewExpenses = true, approveExpenses = true),
            inventory = InventoryPermissions(viewInventory = true, manageInventory = true),
            purchaseOrders = PurchaseOrdersPermissions(viewPurchaseOrders = true, approvePurchaseOrders = true)
        )

        assertTrue(managerPerms.company.manageCompany)
        assertTrue(managerPerms.projects.createProjects)
        assertTrue(managerPerms.expenses.approveExpenses)
        assertFalse(managerPerms.projects.deleteProjects)
    }

    @Test
    fun testLoginRequestAndResponse_models() {
        val req = LoginRequest("engineer@constructor.app", "Secret123!")
        assertEquals("engineer@constructor.app", req.email)
        assertEquals("Secret123!", req.password)

        val res = LoginResponse(
            accessToken = "jwt-access-token-12345",
            refreshToken = "refresh-token-67890",
            user = SupabaseUser(id = "user-uuid-1", email = "engineer@constructor.app")
        )

        assertEquals("jwt-access-token-12345", res.accessToken)
        assertEquals("user-uuid-1", res.user.id)
        assertEquals("engineer@constructor.app", res.user.email)
    }
}
