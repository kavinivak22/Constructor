package com.example.constructorapp

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.navigation.Screen
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AlertFlipper
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.components.sampleAlerts
import com.example.constructorapp.ui.screens.*

@Composable
fun App() {
    ConstructorSharedTheme {
        // Full navigation backstack supporting all 31 screens
        val navigationStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
        val currentScreen = navigationStack.lastOrNull() ?: Screen.Dashboard
        var isDrawerOpen by remember { mutableStateOf(false) }

        val navigateTo: (Screen) -> Unit = { screen ->
            navigationStack.add(screen)
        }

        val navigateBack: () -> Unit = {
            if (navigationStack.size > 1) {
                navigationStack.removeLast()
            }
        }

        val selectedTab = when (currentScreen) {
            is Screen.Dashboard -> 0
            is Screen.Projects -> 1
            is Screen.PurchaseOrders -> 2
            is Screen.TeamHub, is Screen.AddEditReminder -> 3
            else -> -1
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBackground)
        ) {
            // Layer 1: Ambient Drifting Blobs Canvas Background
            AmbientBlobBackground()

            // Layer 2: Complete Navigation Router (All 31 Screens Connected)
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (currentScreen) {
                    is Screen.Dashboard -> DashboardScreen(
                        onNavigateToProjects = { navigateTo(Screen.Projects) },
                        onNavigateToWorklog = { navigateTo(Screen.Worklog()) },
                        onNavigateToExpenses = { navigateTo(Screen.Expenses()) },
                        onNavigateToMaterials = { navigateTo(Screen.MaterialEstimation()) },
                        onNavigateToWorkPrep = { navigateTo(Screen.WorkPrep()) },
                        onProjectClick = { proj ->
                            navigateTo(Screen.ProjectDetail(proj.id, proj.name))
                        }
                    )
                    is Screen.Projects -> ProjectsScreen(
                        onProjectClick = { proj ->
                            navigateTo(Screen.ProjectDetail(proj.id, proj.name))
                        }
                    )
                    is Screen.ProjectDetail -> ProjectDetailScreen(
                        projectId = currentScreen.projectId,
                        projectName = currentScreen.projectName,
                        onBackClick = navigateBack,
                        onNavigateToMaterials = { pId, pName -> navigateTo(Screen.ProjectMaterials(pId, pName)) },
                        onNavigateToPouch = { pId, pName -> navigateTo(Screen.ProjectPouch(pId, pName)) },
                        onNavigateToWorklog = { pId, pName -> navigateTo(Screen.Worklog(pId, pName)) },
                        onNavigateToBuildingPlan = { pId, pName -> navigateTo(Screen.BuildingPlan(pId, pName)) },
                        onNavigateToExpenses = { pId -> navigateTo(Screen.Expenses(pId)) },
                        onNavigateToWorkPrep = { pId -> navigateTo(Screen.WorkPrep(pId)) }
                    )
                    is Screen.ProjectMaterials -> ProjectMaterialsScreen(
                        projectId = currentScreen.projectId,
                        projectName = currentScreen.projectName,
                        onBackClick = navigateBack
                    )
                    is Screen.ProjectPouch -> ProjectPouchScreen(
                        projectId = currentScreen.projectId,
                        projectName = currentScreen.projectName,
                        onBackClick = navigateBack
                    )
                    is Screen.Worklog -> WorklogScreen(
                        projectId = currentScreen.projectId,
                        projectName = currentScreen.projectName,
                        onBackClick = navigateBack
                    )
                    is Screen.WorkPrep -> WorkPrepScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.MaterialEstimation -> MaterialEstimationScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.MaterialReconciliation -> MaterialReconciliationScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.PurchaseOrders -> PurchaseOrdersScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.Inventory -> InventoryScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.CompanyMaterials -> CompanyMaterialsScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.BuildingPlan -> BuildingPlanScreen(
                        projectId = currentScreen.projectId,
                        projectName = currentScreen.projectName,
                        onBackClick = navigateBack
                    )
                    is Screen.ClientMilestones -> ClientMilestonesScreen(
                        projectId = currentScreen.projectId,
                        onBackClick = navigateBack
                    )
                    is Screen.Expenses -> ExpensesScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.Payday -> PaydayScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.Contractors -> ContractorAccountsScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.Financials -> FinancialsScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.TeamHub -> RemindersScreen(
                        onBackClick = navigateBack,
                        onAddReminderClick = { navigateTo(Screen.AddEditReminder()) }
                    )
                    is Screen.AddEditReminder -> AddEditReminderScreen(
                        reminderId = currentScreen.reminderId,
                        onBackClick = navigateBack
                    )
                    is Screen.Notifications -> NotificationsScreen(
                        onBackClick = navigateBack
                    )
                    is Screen.Profile -> ProfileScreen(
                        onBackClick = navigateBack,
                        onLogout = {
                            navigationStack.clear()
                            navigationStack.add(Screen.Login)
                        }
                    )
                    is Screen.Login -> LoginScreen(
                        onLoginSuccess = {
                            navigationStack.clear()
                            navigationStack.add(Screen.Dashboard)
                        }
                    )
                }
            }

            // Layer 3: Floating Bottom Navigation Bar
            FloatingBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { tabIndex ->
                    navigationStack.clear()
                    when (tabIndex) {
                        0 -> navigationStack.add(Screen.Dashboard)
                        1 -> navigationStack.add(Screen.Projects)
                        2 -> navigationStack.add(Screen.PurchaseOrders())
                        3 -> navigationStack.add(Screen.TeamHub())
                    }
                },
                onMoreClick = { isDrawerOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            // More / Drawer Menu with all app destinations
            if (isDrawerOpen) {
                MoreMenuDialog(
                    onDismiss = { isDrawerOpen = false },
                    onSelect = { route ->
                        isDrawerOpen = false
                        when (route) {
                            "Dashboard" -> { navigationStack.clear(); navigationStack.add(Screen.Dashboard) }
                            "Projects" -> { navigationStack.clear(); navigationStack.add(Screen.Projects) }
                            "Purchase Orders" -> navigateTo(Screen.PurchaseOrders())
                            "Daily Worklogs" -> navigateTo(Screen.Worklog())
                            "Tomorrow's Prep" -> navigateTo(Screen.WorkPrep())
                            "Material Estimation" -> navigateTo(Screen.MaterialEstimation())
                            "Inventory & Stock" -> navigateTo(Screen.Inventory())
                            "Expenses Ledger" -> navigateTo(Screen.Expenses())
                            "Weekly Payday" -> navigateTo(Screen.Payday())
                            "Contractor Accounts" -> navigateTo(Screen.Contractors())
                            "Team Hub & Reminders" -> navigateTo(Screen.TeamHub())
                            "Notifications" -> navigateTo(Screen.Notifications)
                            "Profile & Settings" -> navigateTo(Screen.Profile)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToWorklog: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToWorkPrep: () -> Unit,
    onProjectClick: (SharedProject) -> Unit
) {
    var alertsList by remember { mutableStateOf(sampleAlerts) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Alert Flipper / Sentinel Mascot (Split-Flap ticker)
        item {
            AlertFlipper(
                alerts = alertsList,
                onAlertDismiss = { alertId ->
                    alertsList = alertsList.filter { it.id != alertId }
                }
            )
        }

        // 2. Quick Actions Row (Exact Android Dashboard: 38.dp height compact pills)
        item {
            QuickActionsRow(
                onWorklogClick = onNavigateToWorklog,
                onExpenseClick = onNavigateToExpenses,
                onMaterialsClick = onNavigateToMaterials
            )
        }

        // 3. Tomorrow's Work Prep Highlight Banner (Opens authentic WorkPrepScreen)
        item {
            TomorrowPrepBanner(
                scheduledTasksCount = 3,
                dateLabel = "Tomorrow",
                onPrepClick = onNavigateToWorkPrep
            )
        }

        // 4. Active Projects Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Projects",
                    fontFamily = OutfitFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "View All",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { onNavigateToProjects() }
                )
            }
        }

        // Projects List (Clicking opens ProjectDetailScreen)
        items(sharedProjects) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) }
            )
        }
    }
}

// ─── 1. Quick Actions Row ─────────────────────────────────────────────────────
@Composable
fun QuickActionsRow(
    onWorklogClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onMaterialsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            title = "Worklog",
            icon = Icons.Outlined.Add,
            modifier = Modifier.weight(1f),
            onClick = onWorklogClick
        )
        QuickActionButton(
            title = "Expense",
            icon = Icons.Outlined.Receipt,
            modifier = Modifier.weight(1f),
            onClick = onExpenseClick
        )
        QuickActionButton(
            title = "Materials",
            icon = Icons.Outlined.Inventory2,
            modifier = Modifier.weight(1f),
            onClick = onMaterialsClick
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── 2. Tomorrow's Work Prep Banner ───────────────────────────────────────────
@Composable
fun TomorrowPrepBanner(
    scheduledTasksCount: Int,
    dateLabel: String,
    onPrepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPrepClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditCalendar,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Tomorrow's Prep",
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BrandPrimary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = dateLabel,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (scheduledTasksCount > 0) "$scheduledTasksCount tasks scheduled • Tap to review" else "Plan labor, materials & site tasks",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Prep",
                    tint = BrandPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2: PROJECTS CATALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectsScreen(
    onProjectClick: (SharedProject) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    val statusOptions = listOf("All", "Active", "Completed", "On-Hold")

    val filtered = remember(searchQuery, selectedStatus) {
        sharedProjects.filter { p ->
            val matchQ = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.location.contains(searchQuery, ignoreCase = true)
            val matchS = when (selectedStatus) {
                "Active" -> p.status.equals("In Progress", ignoreCase = true) || p.status.equals("Active Foundation", ignoreCase = true)
                "Completed" -> p.status.equals("Completed", ignoreCase = true)
                "On-Hold" -> p.status.equals("On-Hold", ignoreCase = true)
                else -> true
            }
            matchQ && matchS
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projects (${filtered.size})",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // Search Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextPrimary),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search project name, client, location...", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextSecondary)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                    )
                }
            }
        }

        // Status Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusOptions) { status ->
                    val isSel = selectedStatus == status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) BrandPrimary else SurfaceWhite)
                            .border(1.dp, if (isSel) BrandPrimary else SlateBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = status,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) SurfaceWhite else TextSecondary
                        )
                    }
                }
            }
        }

        items(filtered) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 3: PURCHASE ORDERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PurchaseOrdersScreen(
    onBackClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Purchase Orders",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        items(sharedMaterials) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontFamily = OutfitFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Unit: ${item.unit} • ₹${item.unitCost}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${item.stock} ${item.unit}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess
                        )
                        Text(
                            if (item.stock < item.minStock) "⚠️ Low Stock" else "In Stock",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 4: REMINDERS & TEAM HUB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RemindersScreen(
    onBackClick: () -> Unit,
    onAddReminderClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminders & Sync",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .clickable { onAddReminderClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("+ Add Reminder", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }

        items(sharedWorklogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.title, fontFamily = OutfitFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(log.date, fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(log.project, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    Text(log.notes, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.5.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider(color = SlateDivider)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("👷 ${log.laborCount} Workers", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                        Text("🧱 ${log.materialsCount} Materials", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                        Text("📸 ${log.photosCount} Photos", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORE DRAWER / MENU DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoreMenuDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val menuItems = listOf(
        "Dashboard",
        "Projects",
        "Purchase Orders",
        "Daily Worklogs",
        "Tomorrow's Prep",
        "Material Estimation",
        "Inventory & Stock",
        "Expenses Ledger",
        "Weekly Payday",
        "Contractor Accounts",
        "Team Hub & Reminders",
        "Notifications",
        "Profile & Settings"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Menu Navigation", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(menuItems) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(name) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontFamily = PlusJakartaSansFontFamily, color = BrandPrimary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectCard(
    project: SharedProject,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SkyInfoContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = project.status,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyInfo
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(13.dp))
                Text(
                    text = project.location,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    Text("${(project.progress * 100).toInt()}%", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                LinearProgressIndicator(
                    progress = { project.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BrandPrimary,
                    trackColor = SlateBorder
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Budget: ₹${project.budget}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, color = TextSecondary)
                Text("Spent: ₹${project.spent}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS & SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────
data class SharedProject(val id: String, val name: String, val location: String, val status: String, val progress: Float, val budget: String, val spent: String)
data class SharedWorklog(val title: String, val project: String, val notes: String, val date: String, val laborCount: Int, val materialsCount: Int, val photosCount: Int)
data class SharedMaterial(val name: String, val unit: String, val stock: Int, val minStock: Int, val unitCost: Int)

val sharedProjects = listOf(
    SharedProject("proj-1", "Greenfield Villa Project", "Thillai Nagar, Trichy", "In Progress", 0.65f, "45,00,000", "28,50,000"),
    SharedProject("proj-2", "Royal Residency Commercial", "Kallukuzhi, Trichy", "Active Foundation", 0.35f, "85,00,000", "32,10,000"),
    SharedProject("proj-3", "Modern Living Duplex", "Karur Bypass, Trichy", "Finishing", 0.85f, "32,00,000", "29,40,000")
)

val sharedWorklogs = listOf(
    SharedWorklog("First Floor Slab Casting", "Greenfield Villa Project", "M20 grade concrete poured for roof slab with 12 masons and helpers.", "Today, 4:30 PM", 12, 4, 3),
    SharedWorklog("Brickwork & Plastering Phase 2", "Royal Residency Commercial", "Completed outer wall perimeter bricklaying with cement mortar 1:5 ratio.", "Yesterday", 8, 2, 2)
)

val sharedMaterials = listOf(
    SharedMaterial("OPC 53 Grade Cement", "Bags", 140, 50, 395),
    SharedMaterial("River Sand (Fine)", "CFT", 450, 100, 75),
    SharedMaterial("TMT Steel 16mm (Fe 550D)", "Kg", 850, 200, 68),
    SharedMaterial("Coarse Aggregate (20mm)", "CFT", 35, 80, 52)
)
