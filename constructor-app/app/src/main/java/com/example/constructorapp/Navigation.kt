package com.example.constructorapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.ui.main.MainScreen
import com.example.constructorapp.ui.screens.*
import com.example.constructorapp.ui.screens.workprep.WorkPrepScreen
import com.example.constructorapp.ui.viewmodel.AuthViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel
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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Login> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        backStack.removeLastOrNull()
                        backStack.add(Main)
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
                        backStack.removeLastOrNull()
                    },
                    onBackClick = { backStack.removeLastOrNull() },
                    onLogout = {
                        backStack.removeLastOrNull()
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
                        backStack.removeLastOrNull()
                    },
                    onBackClick = { backStack.removeLastOrNull() },
                    onLogout = {
                        backStack.removeLastOrNull()
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
                        backStack.clear()
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
    )
}
