package com.example.constructorapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.ExpenseEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossAlertDialog
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ExpenseUiState
import com.example.constructorapp.ui.viewmodel.ExpenseViewModel
import com.example.constructorapp.ui.viewmodel.MaterialConsumptionItem
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    initialProjectId: String? = null,
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isDatePickerDropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialProjectId) {
        if (!initialProjectId.isNullOrBlank()) {
            viewModel.filterByProject(initialProjectId)
        }
    }

    val currentProject = remember(state.projects, state.selectedProjectId) {
        state.projects.find { it.id == state.selectedProjectId }
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
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = currentProject?.name ?: "All Projects (Company Overview)",
                                fontFamily = PlusJakartaSansFontFamily,
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
                                    appendLine("Total Spend: ${state.totalSpend.toInrFull()}")
                                    appendLine("Total Entries: ${state.filteredExpenses.size}")
                                    appendLine("Material Consumption Cost: ${state.totalConsumptionCost.toInrFull()}")
                                    appendLine("\nTop Recent Expenses:")
                                    state.filteredExpenses.take(10).forEach { e ->
                                        appendLine("- ${e.expenseDate}: ${e.description} (${e.amount.toInrFull()}) [${e.category.uppercase()}] - ${e.paymentStatus.uppercase()}")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Expenses Summary")
                                context.startActivity(shareIntent)
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
                    onClick = { viewModel.setFormSheetOpen(true) },
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
                // 1. Projects Filter Bar (Crucial Webapp Feature)
                if (state.projects.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "PROJECT SITE FILTER",
                                fontFamily = PlusJakartaSansFontFamily,
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
                                val isAllSelected = state.selectedProjectId == null || state.selectedProjectId == "all"
                                FilterChip(
                                    selected = isAllSelected,
                                    onClick = { viewModel.filterByProject(null) },
                                    label = {
                                        Text(
                                            "All Projects (${state.expenses.size})",
                                            fontFamily = PlusJakartaSansFontFamily,
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

                                state.projects.forEach { proj ->
                                    val isSelected = state.selectedProjectId == proj.id
                                    val count = state.expenses.count { it.projectId == proj.id }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.filterByProject(proj.id) },
                                        label = {
                                            Text(
                                                "${proj.name} ($count)",
                                                fontFamily = PlusJakartaSansFontFamily,
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

                // 2. Financial Summary Banner (Option 2 exact from Webapp)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Cash Spent (Outflows)
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
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = state.totalSpend.toInrFull(),
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${state.filteredExpenses.size} expenses",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Card 2: Materials Consumed (Site Consumption Log)
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setConsumptionSheetOpen(true) }
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
                                            fontFamily = PlusJakartaSansFontFamily,
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
                                        text = state.totalConsumptionCost.toInrFull(),
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Site Log",
                                            fontFamily = PlusJakartaSansFontFamily,
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
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search expenses by description, vendor, notes...", fontSize = 12.sp, color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (state.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
                            // Date Range Preset Filter Button
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
                                        text = when (state.dateFilterPreset) {
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
                                                    fontWeight = if (state.dateFilterPreset == presetKey) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (state.dateFilterPreset == presetKey) BrandPrimary else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.setDateFilterPreset(presetKey)
                                                isDatePickerDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // My Expenses / All Expenses Switch Chip
                            FilterChip(
                                selected = state.userFilter == "me",
                                onClick = {
                                    viewModel.setUserFilter(if (state.userFilter == "me") "all" else "me")
                                },
                                label = {
                                    Text(
                                        if (state.userFilter == "me") "My Expenses" else "All Crew",
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
                            state.availableCategories.forEach { cat ->
                                val isSelected = state.selectedCategory.equals(cat, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.filterByCategory(cat) },
                                    label = {
                                        Text(
                                            cat.replaceFirstChar { it.uppercase() },
                                            fontFamily = PlusJakartaSansFontFamily,
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
                if (state.filteredExpenses.isEmpty()) {
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
                                    if (state.expenses.isNotEmpty()) "No expenses match your current filters or date range." else "No site expenses logged yet. Tap Log Expense to record one.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.setFormSheetOpen(true) },
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
                    items(state.filteredExpenses, key = { it.id }) { expense ->
                        val project = state.projects.find { it.id == expense.projectId }
                        WebappStyleExpenseCard(
                            expense = expense,
                            projectName = project?.name,
                            onClick = { viewModel.setViewingExpense(expense) },
                            onEdit = { viewModel.setEditingExpense(expense) },
                            onDelete = { viewModel.requestDeleteExpense(expense) }
                        )
                    }
                }
            }
        }

        // Expense Form Bottom Sheet (Add or Edit)
        if (state.isFormSheetOpen) {
            ExpenseFormSheet(
                initialProjectId = state.selectedProjectId ?: initialProjectId,
                expenseToEdit = state.editingExpense,
                projects = state.projects,
                onDismiss = { viewModel.setFormSheetOpen(false) },
                onSubmit = { id, projId, amt, cat, desc, date, mode, stat, notes, receiver, uri ->
                    viewModel.saveExpense(
                        id = id,
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
                        context = context
                    )
                }
            )
        }

        // Expense Details Dialog (viewingExpense exact from Webapp)
        state.viewingExpense?.let { expense ->
            val project = state.projects.find { it.id == expense.projectId }
            ExpenseDetailsDialog(
                expense = expense,
                projectName = project?.name ?: "Current Site",
                onDismiss = { viewModel.setViewingExpense(null) },
                onEdit = {
                    viewModel.setViewingExpense(null)
                    viewModel.setEditingExpense(expense)
                },
                onDelete = {
                    viewModel.setViewingExpense(null)
                    viewModel.requestDeleteExpense(expense)
                }
            )
        }

        // Delete Confirmation Alert Dialog
        if (state.isDeleteAlertOpen) {
            FluidGlossAlertDialog(
                onDismissRequest = { viewModel.cancelDeleteExpense() },
                title = "Delete Expense?",
                text = "This action cannot be undone. This will permanently delete the expense record.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = { viewModel.confirmDeleteExpense() },
                onDismiss = { viewModel.cancelDeleteExpense() },
                isDestructive = true,
                icon = Icons.Default.Delete
            )
        }

        // Material Consumption Log Bottom Sheet (from Card 2)
        if (state.isConsumptionSheetOpen) {
            MaterialConsumptionSheet(
                logs = state.consumptionLogs,
                totalCost = state.totalConsumptionCost,
                projectName = currentProject?.name ?: "All Project Sites",
                onDismiss = { viewModel.setConsumptionSheetOpen(false) }
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

    // Clean up verbose descriptions like in webapp
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
                        fontFamily = PlusJakartaSansFontFamily,
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
                    fontFamily = OutfitFontFamily,
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
            // Highlighted Amount Banner (Exact from Webapp)
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
                        fontFamily = OutfitFontFamily,
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

            // Receipt Attachment Preview
            if (!expense.localReceiptUri.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Receipt Document / Photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = File(expense.localReceiptUri),
                            contentDescription = "Receipt Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
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
                        fontFamily = OutfitFontFamily,
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
                                fontFamily = OutfitFontFamily,
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

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun ExpensesScreen(
    initialProjectId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExpenseViewModel(database) as T
            }
        }
    )

    ExpensesScreen(
        initialProjectId = initialProjectId,
        viewModel = expenseViewModel,
        onBackClick = onBackClick
    )
}
