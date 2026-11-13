package com.example.constructorapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.TextPrimary
import com.example.constructorapp.ui.components.AppDrawerContent
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.main.MainScreen
import com.example.constructorapp.ui.screens.*
import com.example.constructorapp.ui.screens.workprep.WorkPrepScreen
import com.example.constructorapp.ui.viewmodel.AuthViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object ProjectsCatalog : NavKey

@Composable
fun MainNavigation(
    authViewModel: AuthViewModel,
    projectViewModel: ProjectViewModel,
    reminderViewModel: ReminderViewModel
) {
    val startDestination = if (SupabaseClient.isLoggedIn()) Main else Login
    val backStack = rememberNavBackStack(startDestination)
    var currentMainTab by rememberSaveable { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentKey = backStack.lastOrNull()
    val isAppAuthenticated = currentKey !is Login && currentKey !is RegisterCompany

    val selectedNavTab = when (currentKey) {
        is Main -> currentMainTab
        is ProjectsCatalog, is ProjectDetail, is ProjectMaterials, is BuildingPlan, is ProjectPouch -> 1
        is PurchaseOrders -> 2
        is TeamHub, is PersonalPouchNav, is AddEditReminder -> 3
        else -> -1
    }

    val currentDrawerRoute = when (currentKey) {
        is Main -> when (currentMainTab) {
            0 -> "/"
            1 -> "/projects"
            2 -> "/purchase-orders"
            3 -> "/team-hub"
            else -> "/"
        }
        is ProjectsCatalog, is ProjectDetail, is ProjectMaterials -> "/projects"
        is PurchaseOrders -> "/purchase-orders"
        is TeamHub -> "/team-hub"
        is Worklog -> "/worklog"
        is WorkPrep -> "/work-prep"
        is MaterialEstimation -> "/material-estimation"
        is Inventory -> "/inventory"
        is CompanyMaterials -> "/company-materials"
        is MaterialReconciliation -> "/materials/reconciliation"
        is Payday -> "/financials/payday"
        is Contractors -> "/financials/contractors"
        is Expenses -> "/expenses"
        is ClientMilestones -> "/projects/milestones"
        is BuildingPlan -> "/employees"
        is ProjectPouch -> "/project-pouch"
        is PersonalPouchNav -> "/personal-pouch"
        is Notifications -> "/notifications"
        is Profile -> "/profile"
        else -> ""
    }

    val appEntryProvider = entryProvider {
        entry<Login> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = {
                                        backStack.removeLastOrNull()
                                        backStack.add(Main)
                                    },
                                    onNavigateToRegisterCompany = { userId, email, displayName ->
                                        backStack.add(RegisterCompany(userId = userId, email = email, displayName = displayName))
                                    }
                                )
                            }
                            entry<RegisterCompany> { key ->
                                com.example.constructorapp.ui.screens.RegisterCompanyScreen(
                                    viewModel = authViewModel,
                                    initialDisplayName = key.displayName,
                                    onRegistrationSuccess = {
                                        backStack.removeLastOrNull()
                                        backStack.add(Main)
                                    },
                                    onBackToLogin = {
                                        backStack.removeLastOrNull()
                                        backStack.add(Login)
                                    }
                                )
                            }
                            entry<Main> {
                                MainScreen(
                                    selectedTab = currentMainTab,
                                    onTabSelected = { currentMainTab = it },
                                    onItemClick = { navKey -> backStack.add(navKey) },
                                    projectViewModel = projectViewModel,
                                    reminderViewModel = reminderViewModel,
                                    onLogout = {
                                        backStack.removeLastOrNull()
                                        backStack.add(Login)
                                    }
                                )
                            }
                            entry<ProjectsCatalog> {
                                ProjectsScreen(
                                    viewModel = projectViewModel,
                                    onProjectClick = { project ->
                                        backStack.add(ProjectDetail(projectId = project.id, projectName = project.name))
                                    },
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<ProjectDetail> { key ->
                                ProjectDetailScreen(
                                    projectId = key.projectId,
                                    projectName = key.projectName,
                                    viewModel = projectViewModel,
                                    onNavigate = { navKey -> backStack.add(navKey) },
                                    onNavigateToTab = { tabIndex ->
                                        currentMainTab = tabIndex
                                        while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                            backStack.removeLastOrNull()
                                        }
                                        if (backStack.lastOrNull() !is Main) {
                                            backStack.add(Main)
                                        }
                                    },
                                    onBackClick = { backStack.removeLastOrNull() },
                                    onLogout = {
                                        while (backStack.isNotEmpty()) {
                                            backStack.removeLastOrNull()
                                        }
                                        backStack.add(Login)
                                    }
                                )
                            }
                            entry<Worklog> { key ->
                                WorklogScreen(
                                    projectId = key.projectId,
                                    projectName = key.projectName,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<WorkPrep> { key ->
                                WorkPrepScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<ProjectMaterials> { key ->
                                ProjectMaterialsScreen(
                                    projectId = key.projectId,
                                    projectName = key.projectName,
                                    viewModel = projectViewModel,
                                    onNavigateToTab = { tabIndex ->
                                        currentMainTab = tabIndex
                                        while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                            backStack.removeLastOrNull()
                                        }
                                        if (backStack.lastOrNull() !is Main) {
                                            backStack.add(Main)
                                        }
                                    },
                                    onBackClick = { backStack.removeLastOrNull() },
                                    onLogout = {
                                        while (backStack.isNotEmpty()) {
                                            backStack.removeLastOrNull()
                                        }
                                        backStack.add(Login)
                                    }
                                )
                            }
                            entry<MaterialEstimation> { key ->
                                MaterialEstimationScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<PurchaseOrders> { key ->
                                PurchaseOrdersScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<BuildingPlan> { key ->
                                BuildingPlanScreen(
                                    projectId = key.projectId,
                                    projectName = key.projectName,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<ProjectPouch> { key ->
                                ProjectPouchScreen(
                                    projectId = key.projectId,
                                    projectName = key.projectName,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<AddEditReminder> { key ->
                                AddEditReminderScreen(
                                    reminderId = key.reminderId,
                                    viewModel = reminderViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Inventory> { key ->
                                InventoryScreen(
                                    initialSiteId = key.siteId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<CompanyMaterials> {
                                CompanyMaterialsScreen(
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<MaterialReconciliation> { key ->
                                MaterialReconciliationScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Payday> { key ->
                                PaydayScreen(
                                    initialPayoutId = key.payoutId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Contractors> { key ->
                                ContractorAccountsScreen(
                                    initialContractorId = key.contractorId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Expenses> { key ->
                                ExpensesScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<ClientMilestones> { key ->
                                ClientMilestonesScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<TeamHub> { key ->
                                TeamHubScreen(
                                    initialProjectId = key.projectId,
                                    viewModel = projectViewModel,
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Notifications> {
                                NotificationsScreen(
                                    onBackClick = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Profile> {
                                ProfileScreen(
                                    onBackClick = { backStack.removeLastOrNull() },
                                    onLogout = {
                                        while (backStack.isNotEmpty()) {
                                            backStack.removeLastOrNull()
                                        }
                                        backStack.add(Login)
                                    }
                                )
                            }
                            entry<PersonalPouchNav> {
                                RemindersScreen(
                                    viewModel = reminderViewModel,
                                    onAddReminderClick = {
                                        backStack.add(AddEditReminder(reminderId = null))
                                    },
                                    onEditReminderClick = { id ->
                                        backStack.add(AddEditReminder(reminderId = id))
                                    }
                                )
                            }
    }

    if (!isAppAuthenticated) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = appEntryProvider
            )
        }
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            drawerContentColor = TextPrimary,
                            drawerShape = AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp, topRight = 0.dp, bottomRight = 0.dp),
                            modifier = Modifier.width(310.dp)
                        ) {
                            AppDrawerContent(
                                userEmail = SupabaseClient.userEmail,
                                userDisplayName = SupabaseClient.userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User",
                                userRole = "admin",
                                selectedRoute = currentDrawerRoute,
                                onNavigateRoute = { route ->
                                    coroutineScope.launch { drawerState.close() }
                                    when (route) {
                                        "/" -> {
                                            currentMainTab = 0
                                            while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                                backStack.removeLastOrNull()
                                            }
                                            if (backStack.lastOrNull() !is Main) backStack.add(Main)
                                        }
                                        "/projects" -> {
                                            currentMainTab = 1
                                            while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                                backStack.removeLastOrNull()
                                            }
                                            if (backStack.lastOrNull() !is Main) backStack.add(Main)
                                        }
                                        "/purchase-orders" -> {
                                            currentMainTab = 2
                                            while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                                backStack.removeLastOrNull()
                                            }
                                            if (backStack.lastOrNull() !is Main) backStack.add(Main)
                                        }
                                        "/personal-pouch" -> {
                                            currentMainTab = 3
                                            while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                                backStack.removeLastOrNull()
                                            }
                                            if (backStack.lastOrNull() !is Main) backStack.add(Main)
                                        }
                                        "/team-hub" -> backStack.add(TeamHub())
                                        "/notifications" -> backStack.add(Notifications)
                                        "/profile" -> backStack.add(Profile)
                                        "/worklog" -> backStack.add(Worklog(projectId = "", projectName = "All Projects"))
                                        "/work-prep" -> backStack.add(WorkPrep(projectId = null))
                                        "/material-estimation" -> backStack.add(MaterialEstimation())
                                        "/project-pouch" -> backStack.add(ProjectPouch(projectId = "", projectName = "Project Pouch"))
                                        "/employees" -> backStack.add(BuildingPlan(projectId = "", projectName = "Employees"))
                                        "/inventory" -> backStack.add(Inventory())
                                        "/financials/payday", "/payday" -> backStack.add(Payday())
                                        "/financials/contractors", "/contractors" -> backStack.add(Contractors())
                                        "/expenses" -> backStack.add(Expenses())
                                        "/materials/reconciliation" -> backStack.add(MaterialReconciliation())
                                        "/projects/milestones" -> backStack.add(ClientMilestones())
                                        "/company-materials" -> backStack.add(CompanyMaterials)
                                        else -> {}
                                    }
                                },
                                onCloseDrawer = {
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onLogout = {
                                    coroutineScope.launch { drawerState.close() }
                                    SupabaseClient.authToken = null
                                    SupabaseClient.userId = null
                                    SupabaseClient.userEmail = null
                                    while (backStack.isNotEmpty()) {
                                        backStack.removeLastOrNull()
                                    }
                                    backStack.add(Login)
                                }
                            )
                        }
                    }
                }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryProvider = appEntryProvider
                        )

                        FloatingBottomNav(
                            selectedTab = selectedNavTab,
                            onTabSelected = { index ->
                                currentMainTab = index
                                while (backStack.size > 1 && backStack.lastOrNull() !is Main) {
                                    backStack.removeLastOrNull()
                                }
                                if (backStack.lastOrNull() !is Main) {
                                    backStack.add(Main)
                                }
                            },
                            onMoreClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
