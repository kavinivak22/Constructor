package com.example.constructorapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.*
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.TextPrimary
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.AppDrawerContent
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.screens.DashboardScreen
import com.example.constructorapp.ui.screens.ProjectsScreen
import com.example.constructorapp.ui.screens.PurchaseOrdersScreen
import com.example.constructorapp.ui.screens.RemindersScreen
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    projectViewModel: ProjectViewModel,
    reminderViewModel: ReminderViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentRoute = when (selectedTab) {
        0 -> "/"
        1 -> "/projects"
        2 -> "/purchase-orders"
        3 -> "/team-hub"
        else -> "/"
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
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
                            selectedRoute = currentRoute,
                            onNavigateRoute = { route ->
                                coroutineScope.launch { drawerState.close() }
                                when (route) {
                                    "/" -> onTabSelected(0)
                                    "/projects" -> onTabSelected(1)
                                    "/purchase-orders" -> onTabSelected(2)
                                    "/team-hub" -> onItemClick(TeamHub())
                                    "/personal-pouch" -> onTabSelected(3)
                                    "/notifications" -> onItemClick(Notifications)
                                    "/profile" -> onItemClick(Profile)
                                    "/worklog" -> onItemClick(Worklog(projectId = "", projectName = "All Projects"))
                                    "/work-prep" -> onItemClick(WorkPrep(projectId = null))
                                    "/material-estimation" -> onItemClick(MaterialEstimation())
                                    "/project-pouch" -> onItemClick(ProjectPouch(projectId = "", projectName = "Project Pouch"))
                                    "/employees" -> onItemClick(BuildingPlan(projectId = "", projectName = "Employees"))
                                    "/inventory" -> onItemClick(Inventory())
                                    "/financials/payday" -> onItemClick(Payday())
                                    "/financials/contractors" -> onItemClick(Contractors())
                                    "/expenses" -> onItemClick(Expenses())
                                    "/materials/reconciliation" -> onItemClick(MaterialReconciliation())
                                    "/projects/milestones" -> onItemClick(ClientMilestones())
                                    "/company-materials" -> onItemClick(CompanyMaterials)
                                    else -> {
                                        // Additional routes mapped as screens are added in subsequent milestones
                                    }
                                }
                            },
                            onCloseDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            },
                            onLogout = {
                                SupabaseClient.authToken = null
                                SupabaseClient.userId = null
                                SupabaseClient.userEmail = null
                                onLogout()
                            }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = modifier.fillMaxSize()) {
                    // Layer 1: Ambient Drifting Blobs Canvas Background (Webapp exact ambient canvas)
                    AmbientBlobBackground()

                    // Layer 2: Main Screen Content with top status bar clearance
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                viewModel = projectViewModel,
                                onProjectClick = { project ->
                                    onItemClick(ProjectDetail(projectId = project.id, projectName = project.name))
                                },
                                onViewAllProjects = {
                                    onTabSelected(1)
                                },
                                onNavigate = { navKey -> onItemClick(navKey) },
                                onLogout = onLogout
                            )
                            1 -> {
                                // Projects catalog tab
                                ProjectsScreen(
                                    viewModel = projectViewModel,
                                    onProjectClick = { project ->
                                        onItemClick(ProjectDetail(projectId = project.id, projectName = project.name))
                                    },
                                    onBackClick = { onTabSelected(0) }
                                )
                            }
                            2 -> {
                                // Orders tab
                                PurchaseOrdersScreen(
                                    initialProjectId = null,
                                    viewModel = projectViewModel,
                                    onBackClick = { onTabSelected(0) }
                                )
                            }
                            3 -> {
                                // Messages / Reminders tab
                                RemindersScreen(
                                    viewModel = reminderViewModel,
                                    onAddReminderClick = {
                                        onItemClick(AddEditReminder(reminderId = null))
                                    },
                                    onEditReminderClick = { id ->
                                        onItemClick(AddEditReminder(reminderId = id))
                                    }
                                )
                            }
                        }
                    }

                    // Layer 3: Floating Bottom Navigation Bar (Truly floating over scrolling content)
                    FloatingBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { index ->
                            onTabSelected(index)
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
