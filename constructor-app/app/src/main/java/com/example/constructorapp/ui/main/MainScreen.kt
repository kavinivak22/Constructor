package com.example.constructorapp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.screens.DashboardScreen
import com.example.constructorapp.ui.screens.ProjectsScreen
import com.example.constructorapp.ui.screens.PurchaseOrdersScreen
import com.example.constructorapp.ui.screens.RemindersScreen
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel

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
    }
}
