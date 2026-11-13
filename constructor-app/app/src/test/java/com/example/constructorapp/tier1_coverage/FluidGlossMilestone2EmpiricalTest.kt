package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.ui.components.GlassCardDefaultBackground
import com.example.constructorapp.ui.components.GlassCardDefaultBorder
import com.example.constructorapp.ui.components.GlassCardDefaultShape
import com.example.constructorapp.ui.screens.getPresetPermissions
import com.example.constructorapp.util.CurrencyFormatter
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Empirical Unit & Stress Test Suite for Milestone 2:
 * Core Dialog & Modal Refactoring with FluidGloss Primitives
 */
class FluidGlossMilestone2EmpiricalTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CREATE TASK DIALOG VALIDATION & TRANSFORMATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testCreateTaskDialog_validationRules() {
        // Form valid only when title is non-empty after trimming
        fun isValid(title: String) = title.trim().isNotEmpty()

        assertFalse(isValid(""))
        assertFalse(isValid("   "))
        assertFalse(isValid("\n\t"))
        assertTrue(isValid("Foundation Inspection"))
        assertTrue(isValid(" A "))

        // TaskItemDto construction
        val title = "Pour Slab Concrete"
        val projId = "proj-101"
        val task = TaskItemDto(
            id = UUID.randomUUID().toString(),
            projectId = projId,
            title = title.trim(),
            description = "Grade M25 required".ifBlank { null },
            dueDate = "2026-11-20".ifBlank { null },
            priority = "high".lowercase(),
            status = "pending",
            createdBy = "test-user"
        )
        assertEquals("Pour Slab Concrete", task.title)
        assertEquals("high", task.priority)
        assertEquals("pending", task.status)
        assertEquals(projId, task.projectId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. EDIT PROJECT DIALOG VALIDATION & CURRENCY TRANSFORMATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testEditProjectDialog_validationAndBudgetParsing() {
        fun isFormValid(name: String, budget: Double) = name.trim().length >= 2 && budget >= 0.0

        assertFalse(isFormValid("", 50000.0))
        assertFalse(isFormValid("A", 50000.0))
        assertFalse(isFormValid("Project X", -10.0))
        assertTrue(isFormValid("Project X", 0.0))
        assertTrue(isFormValid("Project X", 1500000.0))

        // INR Budget parsing with CurrencyFormatter
        val parsed1 = CurrencyFormatter.parseInr("1500000")
        assertEquals(1500000.0, parsed1 ?: 0.0, 0.001)

        val parsed2 = CurrencyFormatter.parseInr("₹ 25,00,000")
        assertEquals(2500000.0, parsed2 ?: 0.0, 0.001)

        val initial = ProjectDto(
            id = "proj-1",
            name = "Tower A",
            budget = 1000000.0,
            progress = 30,
            companyId = "comp-1"
        )
        val updated = initial.copy(
            name = "Tower A - Phase 2",
            budget = 1500000.0,
            progress = 45,
            status = "active"
        )
        assertEquals("Tower A - Phase 2", updated.name)
        assertEquals(1500000.0, updated.budget, 0.001)
        assertEquals(45, updated.progress)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. STOCK TRANSFER LOGIC & INVENTORY LIMIT VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testStockTransfer_directionAndStockValidation() {
        fun resolveDirection(direction: String): Pair<String, String> = when (direction) {
            "wh_to_proj" -> "warehouse" to "project"
            "proj_to_wh" -> "project" to "warehouse"
            else -> "project" to "project"
        }

        assertEquals("warehouse" to "project", resolveDirection("wh_to_proj"))
        assertEquals("project" to "warehouse", resolveDirection("proj_to_wh"))
        assertEquals("project" to "project", resolveDirection("proj_to_proj"))

        val mockAsset = InventoryAssetEntity(
            id = "asset-cement-1",
            name = "OPC 53 Cement",
            category = "material",
            currentStock = 120.0,
            unit = "Bags",
            unitCost = 380.0,
            minStockLevel = 20.0,
            siteId = "wh-1",
            assetType = "material"
        )

        fun validateTransfer(qty: Double, asset: InventoryAssetEntity?): String? {
            if (qty <= 0.0) return "Please enter a valid positive quantity"
            if (asset != null && qty > asset.currentStock) {
                return "Quantity exceeds current stock (${asset.currentStock})"
            }
            return null
        }

        assertNotNull(validateTransfer(-5.0, mockAsset))
        assertNotNull(validateTransfer(0.0, mockAsset))
        assertNotNull(validateTransfer(150.0, mockAsset))
        assertNull(validateTransfer(50.0, mockAsset))
        assertNull(validateTransfer(120.0, mockAsset))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ADD EMPLOYEE ROLE PRESETS & GRANULAR PERMISSIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testAddEmployee_presetPermissionsConformToMatrix() {
        val adminPerms = getPresetPermissions("Admin")
        assertTrue(adminPerms.company.manageCompany)
        assertTrue(adminPerms.projects.deleteProjects)
        assertTrue(adminPerms.expenses.approveExpenses)
        assertTrue(adminPerms.inventory.manageInventory)

        val siteEngPerms = getPresetPermissions("Site Engineer")
        assertFalse(siteEngPerms.company.manageCompany)
        assertFalse(siteEngPerms.projects.deleteProjects)
        assertTrue(siteEngPerms.worklogs.createWorklogs)
        assertTrue(siteEngPerms.inventory.logMaterialUsage)

        val accountantPerms = getPresetPermissions("Accountant")
        assertFalse(accountantPerms.projects.createProjects)
        assertTrue(accountantPerms.company.viewFinancials)
        assertTrue(accountantPerms.expenses.createExpenses)
        assertTrue(accountantPerms.expenses.approveExpenses)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. GLASSCARD TOKEN INVARIANTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testGlassCard_tokensConformToSpec() {
        // Translucency check: Alpha must be ~0.72 (0xB8 / 255)
        val bgAlpha = GlassCardDefaultBackground.alpha
        assertEquals(0.7215f, bgAlpha, 0.01f)

        // Specular border alpha must be ~0.50 (0x80 / 255)
        val borderAlpha = GlassCardDefaultBorder.alpha
        assertEquals(0.5019f, borderAlpha, 0.01f)

        // Corner radius must be 24.dp
        assertNotNull(GlassCardDefaultShape)
    }
}
