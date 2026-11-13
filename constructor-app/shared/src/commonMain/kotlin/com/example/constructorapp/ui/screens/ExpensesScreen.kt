package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ExpenseEntity
import com.example.constructorapp.data.model.MaterialConsumptionItem
import com.example.constructorapp.data.model.ProjectEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossAlertDialog
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.DateTimeUtil
import com.example.constructorapp.util.toInrFull

private val sampleProjectsData = listOf(
    ProjectEntity(
        id = "proj-1",
        name = "Grand Horizon Towers",
        location = "Coimbatore, Tamil Nadu",
        status = "In Progress",
        budget = 45000000.0,
        spent = 18200000.0
    ),
    ProjectEntity(
        id = "proj-2",
        name = "Astra Tech Park Phase 2",
        location = "Saravanampatti",
        status = "In Progress",
        budget = 75000000.0,
        spent = 32000000.0
    ),
    ProjectEntity(
        id = "proj-3",
        name = "Emerald Villa Enclave",
        location = "Race Course Road",
        status = "Planning",
        budget = 22000000.0,
        spent = 4500000.0
    )
)

private val sampleExpensesData = listOf(
    ExpenseEntity(
        id = "exp-1",
        projectId = "proj-1",
        amount = 145000.0,
        category = "materials",
        description = "500 Bags UltraTech Cement 53 Grade",
        expenseDate = "2026-11-12",
        paymentMode = "bank_transfer",
        paymentStatus = "paid",
        receiver = "Ramco Cement Agencies",
        notes = "Delivered to Block B site directly",
        userId = "user-1"
    ),
    ExpenseEntity(
        id = "exp-2",
        projectId = "proj-1",
        amount = 48000.0,
        category = "labor",
        description = "Mason Team Weekly Muster Payout",
        expenseDate = "2026-11-10",
        paymentMode = "cash",
        paymentStatus = "paid",
        receiver = "Murugan Masonry Works",
        notes = "Dual ledger NMR component cleared",
        userId = "user-1"
    ),
    ExpenseEntity(
        id = "exp-3",
        projectId = "proj-2",
        amount = 85000.0,
        category = "equipment",
        description = "JCB Excavator 40 Hours Hire",
        expenseDate = "2026-11-09",
        paymentMode = "upi",
        paymentStatus = "paid",
        receiver = "Sri Balaji Earth Movers",
        notes = "Excavation and trenching for electrical conduits",
        userId = "user-2"
    ),
    ExpenseEntity(
        id = "exp-4",
        projectId = "proj-1",
        amount = 12000.0,
        category = "permits",
        description = "Local Municipal Water Connection Inspection Fee",
        expenseDate = "2026-11-08",
        paymentMode = "cheque",
        paymentStatus = "pending",
        receiver = "City Municipal Corporation",
        notes = "Pending inspection sign-off",
        userId = "user-1"
    ),
    ExpenseEntity(
        id = "exp-5",
        projectId = "proj-3",
        amount = 65000.0,
        category = "subcontractor",
        description = "Architectural 3D Elevation & BIM Renderings",
        expenseDate = "2026-11-05",
        paymentMode = "bank_transfer",
        paymentStatus = "paid",
        receiver = "Studio Arch Design",
        notes = "Final approval client package",
        userId = "user-1"
    )
)

private val sampleConsumptionLogsData = listOf(
    MaterialConsumptionItem(
        id = "con-1",
        worklogId = "wl-101",
        worklogTitle = "Floor 2 Slab Concrete Pouring",
        materialId = "mat-1",
        materialName = "53 Grade OPC Cement",
        quantityConsumed = 120.0,
        unit = "Bags",
        unitCost = 390.0,
        totalCost = 46800.0,
        date = "2026-11-12",
        projectId = "proj-1"
    ),
    MaterialConsumptionItem(
        id = "con-2",
        worklogId = "wl-102",
        worklogTitle = "Plinth Beam Reinforcement",
        materialId = "mat-2",
        materialName = "12mm Fe550D TMT Rebar",
        quantityConsumed = 2.4,
        unit = "MT",
        unitCost = 54000.0,
        totalCost = 129600.0,
        date = "2026-11-11",
        projectId = "proj-1"
    ),
    MaterialConsumptionItem(
        id = "con-3",
        worklogId = "wl-103",
        worklogTitle = "Masonry Wall Construction",
        materialId = "mat-3",
        materialName = "AAC Blocks 600x200x150",
        quantityConsumed = 850.0,
        unit = "Nos",
        unitCost = 65.0,
        totalCost = 55250.0,
        date = "2026-11-10",
        projectId = "proj-2"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    initialProjectId: String? = null,
    onBackClick: () -> Unit = {},
    onShareSummary: (String) -> Unit = {}
) {
    var selectedProjectId by remember { mutableStateOf(initialProjectId) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var dateFilterPreset by remember { mutableStateOf("all") }
    var userFilter by remember { mutableStateOf("all") }

    var isDatePickerDropdownOpen by remember { mutableStateOf(false) }
    var isFormSheetOpen by remember { mutableStateOf(false) }
    var isConsumptionSheetOpen by remember { mutableStateOf(false) }
    var viewingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val projects = remember { mutableStateListOf(*sampleProjectsData.toTypedArray()) }
    val expenses = remember { mutableStateListOf(*sampleExpensesData.toTypedArray()) }
    val consumptionLogs = remember { mutableStateListOf(*sampleConsumptionLogsData.toTypedArray()) }

    val currentProject = remember(projects, selectedProjectId) {
        projects.find { it.id == selectedProjectId }
    }

    val availableCategories = listOf("all", "materials", "labor", "equipment", "permits", "subcontractor", "other")

    val filteredExpenses = remember(expenses, selectedProjectId, searchQuery, selectedCategory, userFilter) {
        expenses.filter { exp ->
            val matchProj = selectedProjectId == null || selectedProjectId == "all" || exp.projectId == selectedProjectId
            val matchCat = selectedCategory == "all" || exp.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    exp.description.contains(searchQuery, ignoreCase = true) ||
                    (exp.receiver?.contains(searchQuery, ignoreCase = true) == true) ||
                    (exp.notes?.contains(searchQuery, ignoreCase = true) == true)
            val matchUser = userFilter != "me" || exp.userId == "user-1"
            matchProj && matchCat && matchSearch && matchUser
        }
    }

    val totalSpend = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    val totalConsumptionCost = remember(consumptionLogs, selectedProjectId) {
        val relevant = if (selectedProjectId == null || selectedProjectId == "all") consumptionLogs
        else consumptionLogs.filter { it.projectId == selectedProjectId }
        relevant.sumOf { it.totalCost }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Project Expenses",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = currentProject?.name ?: "All Projects (Company Overview)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (currentProject != null) BrandPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val summary = buildString {
                                    appendLine("=== CONSTRUCTOR PROJECT EXPENSES SUMMARY ===")
                                    appendLine("Site: ${currentProject?.name ?: "All Projects"}")
                                    appendLine("Total Spend: ${totalSpend.toInrFull()}")
                                    appendLine("Total Entries: ${filteredExpenses.size}")
                                    appendLine("Material Consumption Cost: ${totalConsumptionCost.toInrFull()}")
                                    appendLine("\nTop Recent Expenses:")
                                    filteredExpenses.take(10).forEach { e ->
                                        appendLine("- ${e.expenseDate}: ${e.description} (${e.amount.toInrFull()}) [${e.category.uppercase()}] - ${e.paymentStatus.uppercase()}")
                                    }
                                }
                                onShareSummary(summary)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export Summary", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingExpense = null
                        isFormSheetOpen = true
                    },
                    containerColor = BrandPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp, end = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Expense", modifier = Modifier.size(24.dp))
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Projects Filter Bar
                if (projects.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "PROJECT SITE FILTER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isAllSelected = selectedProjectId == null || selectedProjectId == "all"
                                FilterChip(
                                    selected = isAllSelected,
                                    onClick = { selectedProjectId = null },
                                    label = {
                                        Text(
                                            "All Projects (${expenses.size})",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = SlateSurfaceSubtle,
                                        labelColor = TextSecondary,
                                        selectedContainerColor = BrandPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )

                                projects.forEach { proj ->
                                    val isSelected = selectedProjectId == proj.id
                                    val count = expenses.count { it.projectId == proj.id }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedProjectId = proj.id },
                                        label = {
                                            Text(
                                                "${proj.name} ($count)",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = SlateSurfaceSubtle,
                                            labelColor = TextSecondary,
                                            selectedContainerColor = BrandPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Financial Summary Banner
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Cash Spent
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BrandPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CurrencyRupee,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cash Spent",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = totalSpend.toInrFull(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${filteredExpenses.size} expenses",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Card 2: Materials Consumed
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isConsumptionSheetOpen = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberWarningContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Materials",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AmberWarning.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("Log", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                        }
                                    }
                                    Text(
                                        text = totalConsumptionCost.toInrFull(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Site Log",
                                            fontSize = 10.sp,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Search and Responsive Filter Controls
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search expenses by description, vendor, notes...", fontSize = 12.sp, color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SlateSurface.copy(alpha = 0.6f),
                                unfocusedContainerColor = SlateSurface.copy(alpha = 0.6f)
                            ),
                            singleLine = true
                        )

                        // Filters Bar: Date Preset + My/All toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { isDatePickerDropdownOpen = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SlateSurfaceSubtle)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (dateFilterPreset) {
                                            "today" -> "Date: Today"
                                            "yesterday" -> "Date: Yesterday"
                                            "7days" -> "Date: Last 7 Days"
                                            "this_month" -> "Date: This Month"
                                            "last_month" -> "Date: Last Month"
                                            else -> "Date: All Time"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                DropdownMenu(
                                    expanded = isDatePickerDropdownOpen,
                                    onDismissRequest = { isDatePickerDropdownOpen = false }
                                ) {
                                    listOf(
                                        "all" to "All Time",
                                        "today" to "Today",
                                        "yesterday" to "Yesterday",
                                        "7days" to "Last 7 Days",
                                        "this_month" to "This Month",
                                        "last_month" to "Last Month"
                                    ).forEach { (presetKey, presetLabel) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    presetLabel,
                                                    fontWeight = if (dateFilterPreset == presetKey) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (dateFilterPreset == presetKey) BrandPrimary else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                dateFilterPreset = presetKey
                                                isDatePickerDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // My Expenses / All Expenses Switch Chip
                            FilterChip(
                                selected = userFilter == "me",
                                onClick = {
                                    userFilter = if (userFilter == "me") "all" else "me"
                                },
                                label = {
                                    Text(
                                        if (userFilter == "me") "My Expenses" else "All Crew",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SlateSurfaceSubtle,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.height(36.dp)
                            )
                        }

                        // Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableCategories.forEach { cat ->
                                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Text(
                                            cat.replaceFirstChar { it.uppercase() },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = SlateSurfaceSubtle,
                                        labelColor = TextSecondary,
                                        selectedContainerColor = BrandPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Expenses List
                if (filteredExpenses.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(SlateSurfaceSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Expenses Found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (expenses.isNotEmpty()) "No expenses match your current filters or date range." else "No site expenses logged yet. Tap Log Expense to record one.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        editingExpense = null
                                        isFormSheetOpen = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        val project = projects.find { it.id == expense.projectId }
                        WebappStyleExpenseCard(
                            expense = expense,
                            projectName = project?.name,
                            onClick = { viewingExpense = expense },
                            onEdit = {
                                editingExpense = expense
                                isFormSheetOpen = true
                            },
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }

        // Expense Form Bottom Sheet (Add or Edit)
        if (isFormSheetOpen) {
            ExpenseFormSheet(
                initialProjectId = selectedProjectId ?: initialProjectId,
                expenseToEdit = editingExpense,
                projects = projects,
                onDismiss = { isFormSheetOpen = false },
                onSubmit = { id, projId, amt, cat, desc, date, mode, stat, notes, receiver, uri ->
                    val index = expenses.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        expenses[index] = expenses[index].copy(
                            projectId = projId,
                            amount = amt,
                            category = cat,
                            description = desc,
                            expenseDate = date,
                            paymentMode = mode,
                            paymentStatus = stat,
                            notes = notes,
                            receiver = receiver,
                            localReceiptUri = uri
                        )
                    } else {
                        expenses.add(
                            0,
                            ExpenseEntity(
                                id = (id ?: "").ifBlank { DateTimeUtil.randomUuid() },
                                projectId = projId,
                                amount = amt,
                                category = cat,
                                description = desc,
                                expenseDate = date,
                                paymentMode = mode,
                                paymentStatus = stat,
                                notes = notes,
                                receiver = receiver,
                                localReceiptUri = uri,
                                userId = "user-1"
                            )
                        )
                    }
                    isFormSheetOpen = false
                }
            )
        }

        // Expense Details Dialog
        viewingExpense?.let { expense ->
            val project = projects.find { it.id == expense.projectId }
            ExpenseDetailsDialog(
                expense = expense,
                projectName = project?.name ?: "Current Site",
                onDismiss = { viewingExpense = null },
                onEdit = {
                    viewingExpense = null
                    editingExpense = expense
                    isFormSheetOpen = true
                },
                onDelete = {
                    viewingExpense = null
                    expenseToDelete = expense
                }
            )
        }

        // Delete Confirmation Alert Dialog
        if (expenseToDelete != null) {
            val toDelete = expenseToDelete!!
            FluidGlossAlertDialog(
                onDismissRequest = { expenseToDelete = null },
                title = "Delete Expense?",
                text = "This action cannot be undone. This will permanently delete the expense record for '${toDelete.description}'.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = {
                    expenses.removeAll { it.id == toDelete.id }
                    expenseToDelete = null
                },
                onDismiss = { expenseToDelete = null },
                isDestructive = true,
                icon = Icons.Default.Delete
            )
        }

        // Material Consumption Log Bottom Sheet
        if (isConsumptionSheetOpen) {
            val relevantLogs = if (selectedProjectId == null || selectedProjectId == "all") consumptionLogs
            else consumptionLogs.filter { it.projectId == selectedProjectId }
            MaterialConsumptionSheet(
                logs = relevantLogs,
                totalCost = totalConsumptionCost,
                projectName = currentProject?.name ?: "All Project Sites",
                onDismiss = { isConsumptionSheetOpen = false }
            )
        }
    }
}

@Composable
fun WebappStyleExpenseCard(
    expense: ExpenseEntity,
    projectName: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (catIcon, catColor) = when (expense.category.lowercase()) {
        "materials" -> Icons.Default.Inventory2 to BrandPrimary
        "labor" -> Icons.Default.Engineering to Color(0xFF14B8A6)
        "equipment" -> Icons.Default.LocalShipping to SkyInfo
        "permits" -> Icons.Default.Description to AmberWarning
        "subcontractor" -> Icons.Default.Handshake to Color(0xFF8B5CF6)
        else -> Icons.Default.ReceiptLong to TextSecondary
    }

    val cleanDesc = remember(expense.description) {
        expense.description
            .replace(Regex("Contractor Wages:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Mason Coolie\\)", RegexOption.IGNORE_CASE), "Mason")
            .replace(Regex("\\(Female Coolie\\)", RegexOption.IGNORE_CASE), "Female Coolie")
    }

    var isMenuOpen by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(catColor.copy(alpha = 0.12f))
                        .border(1.dp, catColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanDesc,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Metadata Row: Category badge • Date • Project
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SlateSurfaceSubtle)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = expense.category.replaceFirstChar { it.uppercase() },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }

                        Text("•", fontSize = 9.sp, color = TextSecondary)

                        Text(
                            text = expense.expenseDate,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )

                        if (!projectName.isNullOrBlank()) {
                            Text("•", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                text = projectName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!expense.receiver.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "To: ${expense.receiver}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Amount, Status badge & More Menu
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = expense.amount.toInrFull(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                val isPaid = expense.paymentStatus.equals("paid", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPaid) EmeraldSuccessContainer else AmberWarningContainer)
                        .border(1.dp, if (isPaid) EmeraldSuccess.copy(alpha = 0.3f) else AmberWarning.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = expense.paymentStatus.uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) EmeraldSuccess else AmberWarning
                    )
                }

                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Actions", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                isMenuOpen = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Expense", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                isMenuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Expense", fontSize = 12.sp, color = RoseDanger) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailsDialog(
    expense: ExpenseEntity,
    projectName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = expense.paymentStatus.equals("paid", ignoreCase = true)

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = expense.description,
        subtitle = "${expense.category.uppercase()} • ${expense.expenseDate}",
        icon = Icons.Default.ReceiptLong,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluidGlossButton(
                    text = "Edit",
                    onClick = onEdit,
                    variant = FluidGlossButtonVariant.Outlined,
                    size = FluidGlossButtonSize.Small,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp)) }
                )

                FluidGlossButton(
                    text = "Close",
                    onClick = onDismiss,
                    variant = FluidGlossButtonVariant.Primary,
                    size = FluidGlossButtonSize.Small
                )
            }
        },
        dismissButton = {
            FluidGlossButton(
                text = "Delete",
                onClick = onDelete,
                variant = FluidGlossButtonVariant.Destructive,
                size = FluidGlossButtonSize.Small
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Highlighted Amount Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimaryContainer.copy(alpha = 0.5f))
                    .border(1.dp, BrandPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Expense Amount", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        text = expense.amount.toInrFull(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                }
            }

            // 4-item Details Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurfaceSubtle)
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logged By", fontSize = 10.sp, color = TextSecondary)
                        Text("Site Supervisor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Project Site", fontSize = 10.sp, color = TextSecondary)
                        Text(projectName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payment Mode", fontSize = 10.sp, color = TextSecondary)
                        Text(expense.paymentMode.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            expense.paymentStatus.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) EmeraldSuccess else AmberWarning
                        )
                    }
                }
                if (!expense.receiver.isNullOrBlank()) {
                    Column {
                        Text("Paid To / Vendor", fontSize = 10.sp, color = TextSecondary)
                        Text(expense.receiver, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }

            // Notes & Reference Details
            if (!expense.notes.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notes & Reference Details", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateSurfaceSubtle)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(expense.notes, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            // Receipt Attachment Card
            if (!expense.localReceiptUri.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Receipt Document / Photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateSurfaceSubtle)
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Attached Receipt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = expense.localReceiptUri,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialConsumptionSheet(
    logs: List<MaterialConsumptionItem>,
    totalCost: Double,
    projectName: String,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter {
            it.materialName.contains(searchQuery, ignoreCase = true) ||
            it.worklogTitle.contains(searchQuery, ignoreCase = true) ||
            it.date.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlassCardSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberWarningContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Site Material Consumption", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(projectName, fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Consumption Financial Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberWarningContainer.copy(alpha = 0.4f))
                    .border(1.dp, AmberWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Estimated Material Cost", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        text = totalCost.toInrFull(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberWarning.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${logs.size} log entries", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            FluidGlossTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Filter by material or log title...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Items List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No material consumption records found.", fontSize = 12.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceSubtle)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.materialName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.date} • ${item.worklogTitle}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.quantityConsumed} ${item.unit} @ ₹${item.unitCost.toLong()}/${item.unit}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary
                                )
                            }
                            Text(
                                text = item.totalCost.toInrFull(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
