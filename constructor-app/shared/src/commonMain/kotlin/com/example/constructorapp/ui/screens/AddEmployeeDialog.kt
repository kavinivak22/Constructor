package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.*
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onSave: (EmployeeDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Site Engineer") }

    var permissions by remember { mutableStateOf(getPresetPermissions("Site Engineer")) }

    fun updateRole(newRole: String) {
        selectedRole = newRole
        permissions = getPresetPermissions(newRole)
    }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Add Team Member",
        subtitle = "Configure role & granular module permissions",
        icon = Icons.Default.Shield,
        confirmButton = {
            FluidGlossButton(
                text = "Add Member",
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onSave(
                            EmployeeDto(
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim().ifEmpty { null },
                                role = selectedRole,
                                permissions = permissions
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank(),
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Basic Info
            FluidGlossTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name *",
                placeholder = "e.g. Rahul Sharma",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address *",
                placeholder = "e.g. rahul@example.com",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                placeholder = "e.g. +91 98765 43210",
                modifier = Modifier.fillMaxWidth()
            )

            // Role Presets
            Text(
                text = "Role Preset",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Admin", "Manager", "Site Engineer", "Accountant").forEach { role ->
                    val isSelected = selectedRole == role
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandPrimaryContainer else SlateSurfaceSubtle)
                            .border(1.dp, if (isSelected) BrandPrimary else SlateBorder, RoundedCornerShape(10.dp))
                            .clickable { updateRole(role) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = role,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Granular Module Permissions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Category 1: Company
            PermissionCategoryCard(
                title = "Company Management",
                description = "Manage company profile, employees, view financials, salaries",
                isAllChecked = permissions.company.manageCompany && permissions.company.manageEmployees &&
                        permissions.company.viewFinancials && permissions.company.manageSalaries,
                onMasterToggle = { checked ->
                    permissions = permissions.copy(
                        company = CompanyPermissions(checked, checked, checked, checked)
                    )
                }
            ) {
                PermissionRow("Manage Company Info", permissions.company.manageCompany) {
                    permissions = permissions.copy(company = permissions.company.copy(manageCompany = it))
                }
                PermissionRow("Manage Employees & Roles", permissions.company.manageEmployees) {
                    permissions = permissions.copy(company = permissions.company.copy(manageEmployees = it))
                }
                PermissionRow("View Company Financials", permissions.company.viewFinancials) {
                    permissions = permissions.copy(company = permissions.company.copy(viewFinancials = it))
                }
                PermissionRow("Manage Employee Salaries", permissions.company.manageSalaries) {
                    permissions = permissions.copy(company = permissions.company.copy(manageSalaries = it))
                }
            }

            // Category 2: Projects
            PermissionCategoryCard(
                title = "Projects Module",
                description = "Create, edit, view, delete projects and team members",
                isAllChecked = permissions.projects.viewAllProjects && permissions.projects.createProjects &&
                        permissions.projects.editProjects && permissions.projects.deleteProjects &&
                        permissions.projects.manageProjectMembers,
                onMasterToggle = { checked ->
                    permissions = permissions.copy(
                        projects = ProjectsPermissions(checked, checked, checked, checked, checked)
                    )
                }
            ) {
                PermissionRow("View All Projects", permissions.projects.viewAllProjects) {
                    permissions = permissions.copy(projects = permissions.projects.copy(viewAllProjects = it))
                }
                PermissionRow("Create Projects", permissions.projects.createProjects) {
                    permissions = permissions.copy(projects = permissions.projects.copy(createProjects = it))
                }
                PermissionRow("Edit Project Details", permissions.projects.editProjects) {
                    permissions = permissions.copy(projects = permissions.projects.copy(editProjects = it))
                }
                PermissionRow("Delete Projects", permissions.projects.deleteProjects) {
                    permissions = permissions.copy(projects = permissions.projects.copy(deleteProjects = it))
                }
                PermissionRow("Manage Project Members", permissions.projects.manageProjectMembers) {
                    permissions = permissions.copy(projects = permissions.projects.copy(manageProjectMembers = it))
                }
            }

            // Category 3: Worklogs
            PermissionCategoryCard(
                title = "Worklogs & Daily Reporting",
                description = "Rate & NMR entries, worker counts, material logs",
                isAllChecked = permissions.worklogs.viewWorklogs && permissions.worklogs.createWorklogs &&
                        permissions.worklogs.approveWorklogs && permissions.worklogs.exportWorklogs,
                onMasterToggle = { checked ->
                    permissions = permissions.copy(
                        worklogs = WorklogsPermissions(checked, checked, checked, checked)
                    )
                }
            ) {
                PermissionRow("View Worklogs", permissions.worklogs.viewWorklogs) {
                    permissions = permissions.copy(worklogs = permissions.worklogs.copy(viewWorklogs = it))
                }
                PermissionRow("Create Daily Worklogs", permissions.worklogs.createWorklogs) {
                    permissions = permissions.copy(worklogs = permissions.worklogs.copy(createWorklogs = it))
                }
                PermissionRow("Approve Worklogs", permissions.worklogs.approveWorklogs) {
                    permissions = permissions.copy(worklogs = permissions.worklogs.copy(approveWorklogs = it))
                }
                PermissionRow("Export Worklogs to PDF/Excel", permissions.worklogs.exportWorklogs) {
                    permissions = permissions.copy(worklogs = permissions.worklogs.copy(exportWorklogs = it))
                }
            }

            // Category 4: Expenses & Financials
            PermissionCategoryCard(
                title = "Expenses & Contractor Payouts",
                description = "Track expenses, batch payouts, verify invoices",
                isAllChecked = permissions.expenses.viewExpenses && permissions.expenses.createExpenses &&
                        permissions.expenses.approveExpenses && permissions.expenses.deleteExpenses,
                onMasterToggle = { checked ->
                    permissions = permissions.copy(
                        expenses = ExpensesPermissions(checked, checked, checked, checked, checked)
                    )
                }
            ) {
                PermissionRow("View Expenses", permissions.expenses.viewExpenses) {
                    permissions = permissions.copy(expenses = permissions.expenses.copy(viewExpenses = it))
                }
                PermissionRow("Create Expenses & Payday Runs", permissions.expenses.createExpenses) {
                    permissions = permissions.copy(expenses = permissions.expenses.copy(createExpenses = it))
                }
                PermissionRow("Approve Contractor Payouts", permissions.expenses.approveExpenses) {
                    permissions = permissions.copy(expenses = permissions.expenses.copy(approveExpenses = it))
                }
                PermissionRow("Export Financial Reports", permissions.expenses.exportExpenses) {
                    permissions = permissions.copy(expenses = permissions.expenses.copy(exportExpenses = it))
                }
            }

            // Category 5: Inventory & Material Estimation
            PermissionCategoryCard(
                title = "Inventory & Estimation",
                description = "Stock tracking, material usage logging, estimations",
                isAllChecked = permissions.inventory.viewInventory && permissions.inventory.manageInventory &&
                        permissions.inventory.logMaterialUsage && permissions.inventory.materialEstimation,
                onMasterToggle = { checked ->
                    permissions = permissions.copy(
                        inventory = InventoryPermissions(checked, checked, checked, checked, checked)
                    )
                }
            ) {
                PermissionRow("View Inventory & Stock", permissions.inventory.viewInventory) {
                    permissions = permissions.copy(inventory = permissions.inventory.copy(viewInventory = it))
                }
                PermissionRow("Manage Suppliers & Orders", permissions.inventory.manageInventory) {
                    permissions = permissions.copy(inventory = permissions.inventory.copy(manageInventory = it))
                }
                PermissionRow("Log Material Consumption", permissions.inventory.logMaterialUsage) {
                    permissions = permissions.copy(inventory = permissions.inventory.copy(logMaterialUsage = it))
                }
                PermissionRow("Material Estimation Calculator", permissions.inventory.materialEstimation) {
                    permissions = permissions.copy(inventory = permissions.inventory.copy(materialEstimation = it))
                }
            }
        }
    }
}

@Composable
fun PermissionCategoryCard(
    title: String,
    description: String,
    isAllChecked: Boolean,
    onMasterToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = description, fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = isAllChecked,
                    onCheckedChange = onMasterToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandPrimary,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SlateSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateDivider)
            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun PermissionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = if (checked) TextPrimary else TextSecondary)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BrandPrimary,
                checkmarkColor = Color.White,
                uncheckedColor = TextSecondary
            )
        )
    }
}

fun getPresetPermissions(role: String): EmployeePermissions {
    return when (role) {
        "Admin" -> EmployeePermissions(
            company = CompanyPermissions(true, true, true, true),
            projects = ProjectsPermissions(true, true, true, true, true),
            expenses = ExpensesPermissions(true, true, true, true, true),
            inventory = InventoryPermissions(true, true, true, true, true),
            purchaseOrders = PurchaseOrdersPermissions(true, true, true, true),
            worklogs = WorklogsPermissions(true, true, true, true),
            pouches = PouchesPermissions(true, true, true),
            documents = DocumentsPermissions(true, true, true)
        )
        "Manager" -> EmployeePermissions(
            company = CompanyPermissions(false, false, true, false),
            projects = ProjectsPermissions(true, true, true, false, true),
            expenses = ExpensesPermissions(true, true, true, false, true),
            inventory = InventoryPermissions(true, true, true, true, true),
            purchaseOrders = PurchaseOrdersPermissions(true, true, true, true),
            worklogs = WorklogsPermissions(true, true, true, true),
            pouches = PouchesPermissions(true, true, true),
            documents = DocumentsPermissions(true, true, true)
        )
        "Site Engineer" -> EmployeePermissions(
            company = CompanyPermissions(false, false, false, false),
            projects = ProjectsPermissions(false, false, false, false, false),
            expenses = ExpensesPermissions(true, true, false, false, false),
            inventory = InventoryPermissions(true, false, true, true, true),
            purchaseOrders = PurchaseOrdersPermissions(true, true, false, true),
            worklogs = WorklogsPermissions(true, true, true, true),
            pouches = PouchesPermissions(true, true, false),
            documents = DocumentsPermissions(true, true, false)
        )
        "Accountant" -> EmployeePermissions(
            company = CompanyPermissions(false, false, true, true),
            projects = ProjectsPermissions(true, false, false, false, false),
            expenses = ExpensesPermissions(true, true, true, true, true),
            inventory = InventoryPermissions(true, false, false, true, false),
            purchaseOrders = PurchaseOrdersPermissions(true, true, true, false),
            worklogs = WorklogsPermissions(true, false, false, true),
            pouches = PouchesPermissions(true, true, true),
            documents = DocumentsPermissions(true, true, false)
        )
        else -> EmployeePermissions()
    }
}
