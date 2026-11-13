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
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.constructorapp.navigation.Screen
import com.example.constructorapp.ui.components.AppDrawerContent
import kotlinx.coroutines.launch
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AlertFlipper
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.components.sampleAlerts
import com.example.constructorapp.ui.screens.*
import com.example.constructorapp.data.SharedDataRepository
import com.example.constructorapp.data.model.ProjectEntity
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.util.toInrCompact

@Composable
fun App() {
    ConstructorSharedTheme {
        // Full navigation backstack supporting all 31 screens
        val navigationStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
        val currentScreen = navigationStack.lastOrNull() ?: Screen.Dashboard
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

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

        val currentDrawerRoute = when (currentScreen) {
            is Screen.Dashboard -> "/"
            is Screen.Projects -> "/projects"
            is Screen.ProjectDetail, is Screen.ProjectMaterials, is Screen.ProjectPouch, is Screen.BuildingPlan -> "/projects"
            is Screen.PurchaseOrders -> "/purchase-orders"
            is Screen.Worklog -> "/worklog"
            is Screen.WorkPrep -> "/work-prep"
            is Screen.MaterialEstimation -> "/material-estimation"
            is Screen.Inventory -> "/inventory"
            is Screen.CompanyMaterials -> "/company-materials"
            is Screen.MaterialReconciliation -> "/materials/reconciliation"
            is Screen.Payday -> "/financials/payday"
            is Screen.Contractors -> "/financials/contractors"
            is Screen.Expenses -> "/expenses"
            is Screen.ClientMilestones -> "/projects/milestones"
            is Screen.TeamHub, is Screen.AddEditReminder -> "/team-hub"
            is Screen.Notifications -> "/notifications"
            is Screen.Profile -> "/profile"
            else -> ""
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentScreen !is Screen.Login,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            drawerContentColor = TextPrimary,
                            drawerShape = AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp, topRight = 0.dp, bottomRight = 0.dp),
                            modifier = Modifier.width(310.dp)
                        ) {
                            AppDrawerContent(
                                userEmail = "engineer@constructor.app",
                                userDisplayName = "Engineer",
                                userRole = "admin",
                                selectedRoute = currentDrawerRoute,
                                onNavigateRoute = { route ->
                                    coroutineScope.launch { drawerState.close() }
                                    when (route) {
                                        "/" -> { navigationStack.clear(); navigationStack.add(Screen.Dashboard) }
                                        "/projects" -> { navigationStack.clear(); navigationStack.add(Screen.Projects) }
                                        "/purchase-orders" -> { navigationStack.clear(); navigationStack.add(Screen.PurchaseOrders()) }
                                        "/team-hub" -> { navigationStack.clear(); navigationStack.add(Screen.TeamHub()) }
                                        "/notifications" -> navigateTo(Screen.Notifications)
                                        "/profile" -> navigateTo(Screen.Profile)
                                        "/worklog" -> navigateTo(Screen.Worklog())
                                        "/work-prep" -> navigateTo(Screen.WorkPrep())
                                        "/material-estimation" -> navigateTo(Screen.MaterialEstimation())
                                        "/inventory" -> navigateTo(Screen.Inventory())
                                        "/company-materials" -> navigateTo(Screen.CompanyMaterials)
                                        "/financials/payday", "/payday" -> navigateTo(Screen.Payday())
                                        "/financials/contractors", "/contractors" -> navigateTo(Screen.Contractors())
                                        "/expenses" -> navigateTo(Screen.Expenses())
                                        "/materials/reconciliation" -> navigateTo(Screen.MaterialReconciliation())
                                        "/projects/milestones" -> navigateTo(Screen.ClientMilestones())
                                        else -> {}
                                    }
                                },
                                onCloseDrawer = {
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onLogout = {
                                    coroutineScope.launch { drawerState.close() }
                                    navigationStack.clear()
                                    navigationStack.add(Screen.Login)
                                }
                            )
                        }
                    }
                }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                onMoreClick = {
                    coroutineScope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}
}
}
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: AUTHENTIC DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToWorklog: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToWorkPrep: () -> Unit,
    onProjectClick: (ProjectEntity) -> Unit
) {
    val projects by SharedDataRepository.projects.collectAsState()
    val worklogs by SharedDataRepository.worklogs.collectAsState()
    val tasks by SharedDataRepository.tasks.collectAsState()
    var alertsList by remember { mutableStateOf(sampleAlerts) }
    var isCreateProjectOpen by remember { mutableStateOf(false) }
    var isAddTaskOpen by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf("Medium") }

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
                scheduledTasksCount = tasks.count { it.status == "pending" },
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Projects",
                        fontFamily = OutfitFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${projects.size}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+ New",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        modifier = Modifier.clickable { isCreateProjectOpen = true }
                    )
                    Text(
                        text = "View All",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.clickable { onNavigateToProjects() }
                    )
                }
            }
        }

        // Projects List (Clicking opens ProjectDetailScreen)
        items(projects) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) }
            )
        }

        // 5. Recent Site Activities / Daily Worklogs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Site Worklogs",
                    fontFamily = OutfitFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "All Logs",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { onNavigateToWorklog() }
                )
            }
        }

        items(worklogs) { item ->
            WorklogCard(
                item = item,
                onClick = { onNavigateToWorklog() }
            )
        }

        // 6. Site Tasks Section with interactive checkmark
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Site Tasks",
                    fontFamily = OutfitFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "+ Add Task",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { isAddTaskOpen = true }
                )
            }
        }

        items(tasks) { task ->
            SiteTaskCard(
                task = task,
                onToggle = { SharedDataRepository.toggleTask(task.id) }
            )
        }
    }

    if (isCreateProjectOpen) {
        CreateProjectDialog(
            onDismiss = { isCreateProjectOpen = false },
            onSubmit = { created ->
                SharedDataRepository.addProject(created)
                isCreateProjectOpen = false
            }
        )
    }

    if (isAddTaskOpen) {
        AlertDialog(
            onDismissRequest = { isAddTaskOpen = false },
            title = { Text("Add Site Task", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            SharedDataRepository.addTask(newTaskTitle, newTaskPriority)
                            newTaskTitle = ""
                            isAddTaskOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddTaskOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
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
// SCREEN 2: AUTHENTIC PROJECTS CATALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectsScreen(
    onProjectClick: (ProjectEntity) -> Unit
) {
    val projects by SharedDataRepository.projects.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var isCreateProjectOpen by remember { mutableStateOf(false) }
    val statusOptions = listOf("All", "In Progress", "Completed", "On-Hold")

    val filtered = remember(projects, searchQuery, selectedStatus) {
        projects.filter { p ->
            val matchQ = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || (p.location?.contains(searchQuery, ignoreCase = true) == true)
            val matchS = when (selectedStatus) {
                "In Progress" -> p.status.equals("In Progress", ignoreCase = true) || p.status.equals("Active", ignoreCase = true)
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimary)
                        .clickable { isCreateProjectOpen = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ New Project",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
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

    if (isCreateProjectOpen) {
        CreateProjectDialog(
            onDismiss = { isCreateProjectOpen = false },
            onSubmit = { created ->
                SharedDataRepository.addProject(created)
                isCreateProjectOpen = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 3: AUTHENTIC PURCHASE ORDERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PurchaseOrdersScreen(
    onBackClick: () -> Unit
) {
    val purchaseOrders by SharedDataRepository.purchaseOrders.collectAsState()
    val projects by SharedDataRepository.projects.collectAsState()
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var isCreatePoOpen by remember { mutableStateOf(false) }
    val statusOptions = listOf("All", "Approved", "Delivered", "Pending")

    val filtered = remember(purchaseOrders, selectedStatus) {
        if (selectedStatus == "All") purchaseOrders
        else purchaseOrders.filter { it.status.equals(selectedStatus, ignoreCase = true) }
    }

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
                Column {
                    Text(
                        text = "Purchase Orders",
                        fontFamily = OutfitFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Material Procurement & Delivery Tracking",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimary)
                        .clickable { isCreatePoOpen = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ Create PO",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
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

        items(filtered) { po ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = po.id ?: "PO",
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (po.status == "Delivered") EmeraldSuccessContainer else SkyInfoContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = po.status,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (po.status == "Delivered") EmeraldSuccess else SkyInfo
                            )
                        }
                    }

                    Text(
                        text = po.supplierName,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    po.specialInstructions?.let {
                        Text(
                            text = it,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(color = SlateDivider)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delivery: ${po.deliveryDate ?: "Pending"}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${po.totalAmount.toInrCompact()}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }
    }

    if (isCreatePoOpen) {
        CreatePurchaseOrderDialog(
            projects = projects,
            onDismiss = { isCreatePoOpen = false },
            onSubmit = { created ->
                SharedDataRepository.addPurchaseOrder(created)
                isCreatePoOpen = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 4: AUTHENTIC TEAM HUB & SITE REMINDERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RemindersScreen(
    onBackClick: () -> Unit,
    onAddReminderClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Team Hub, 1 = Personal Pouch Reminders
    val chatMessages by SharedDataRepository.chatMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val tasks by SharedDataRepository.tasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Team Hub & Site Pouch",
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (selectedTab == 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .clickable { onAddReminderClick() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("+ Add Reminder", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }

        // Tab Selector (Team Hub vs Personal Reminders)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) SurfaceWhite else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💬 Site Team Chat",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == 0) BrandPrimary else TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) SurfaceWhite else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📝 Personal Reminders",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == 1) BrandPrimary else TextSecondary
                )
            }
        }

        if (selectedTab == 0) {
            // Live Site Chat Thread
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (msg.isSelf) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = "${msg.sender} • ${msg.time}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (msg.isSelf) BrandPrimary else SurfaceWhite)
                                .border(1.dp, if (msg.isSelf) BrandPrimary else SlateBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg.message,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                color = if (msg.isSelf) SurfaceWhite else TextPrimary
                            )
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextPrimary),
                    decorationBox = { inner ->
                        if (messageText.isEmpty()) {
                            Text("Post update to LVA 24A team...", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextSecondary)
                        }
                        inner()
                    }
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            SharedDataRepository.sendChatMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = BrandPrimary, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Personal Reminders
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks) { task ->
                    SiteTaskCard(
                        task = task,
                        onToggle = { SharedDataRepository.toggleTask(task.id) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTHENTIC REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectCard(
    project: ProjectEntity,
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
                    fontSize = 16.sp,
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

            project.description?.let {
                Text(
                    text = it,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(13.dp))
                Text(
                    text = project.location ?: "Site Location",
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
                    Text("${project.progress}%", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                LinearProgressIndicator(
                    progress = { project.progress / 100f },
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
                Text("Budget: ₹${project.budget.toInrCompact()}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, color = TextSecondary)
                Text("Spent: ₹${project.spent.toInrCompact()}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
        }
    }
}

@Composable
private fun WorklogCard(
    item: WorklogWithDetails,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.worklog.title, fontFamily = OutfitFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(item.worklog.date, fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
            }
            Text("LVA 24A • ${item.worklog.createdBy}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(color = SlateDivider)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val totalWorkers = item.laborEntries.flatMap { it.workerCounts }.sumOf { it.count }
                Text("👷 $totalWorkers Workers", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                Text("🧱 ${item.materials.size} Materials", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                Text("✅ Verified", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SiteTaskCard(
    task: SharedDataRepository.SiteTaskItem,
    onToggle: () -> Unit
) {
    val isDone = task.status == "completed"
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) EmeraldSuccess else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDone) TextSecondary else TextPrimary,
                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                task.dueDate?.let {
                    Text(
                        text = "Due: $it",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (task.priority) {
                            "High" -> RoseDangerContainer
                            "Medium" -> AmberWarningContainer
                            else -> SlateBorder
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = task.priority,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (task.priority) {
                        "High" -> RoseDanger
                        "Medium" -> AmberWarning
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}
