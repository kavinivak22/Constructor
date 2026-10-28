package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.constructorapp.data.remote.*
import com.example.constructorapp.theme.*

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onSave: (EmployeeDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Site Engineer") }

    // 8 categories permissions state
    var permissions by remember { mutableStateOf(getPresetPermissions("Site Engineer")) }

    fun updateRole(newRole: String) {
        selectedRole = newRole
        permissions = getPresetPermissions(newRole)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurfaceSubtle)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add Team Member",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Configure role & granular module permissions",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = SlateBorder)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic Info
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )

                    // Role Presets
                    Text(
                        text = "Role Preset",
                        fontSize = 14.sp,
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
                                    .background(if (isSelected) BrandPrimary.copy(alpha = 0.2f) else SlateSurfaceSubtle)
                                    .border(1.dp, if (isSelected) BrandPrimary else SlateBorder, RoundedCornerShape(10.dp))
                                    .clickable { updateRole(role) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Granular Module Permissions",
                        fontSize = 16.sp,
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

                HorizontalDivider(color = SlateBorder)

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurfaceSubtle)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Member", fontWeight = FontWeight.Bold, color = Color.White)
                    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
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

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = SlateBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = BrandPrimary
)

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
