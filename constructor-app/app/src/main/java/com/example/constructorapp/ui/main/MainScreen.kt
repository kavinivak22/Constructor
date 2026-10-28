package com.example.constructorapp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.AddEditReminder
import com.example.constructorapp.ProjectDetail
import com.example.constructorapp.Worklog
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.screens.DashboardScreen
import com.example.constructorapp.ui.screens.EmployeesScreen
import com.example.constructorapp.ui.screens.FinancialsScreen
import com.example.constructorapp.ui.screens.RemindersScreen
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    projectViewModel: ProjectViewModel,
    reminderViewModel: ReminderViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                contentColor = BrandPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Business, contentDescription = "Projects", modifier = Modifier.size(20.dp)) },
                    label = { Text("Projects", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Team", modifier = Modifier.size(20.dp)) },
                    label = { Text("Team", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Financials", modifier = Modifier.size(20.dp)) },
                    label = { Text("Financials", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Reminders", modifier = Modifier.size(20.dp)) },
                    label = { Text("Reminders", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = SlateBackground,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = projectViewModel,
                    onProjectClick = { project ->
                        onItemClick(ProjectDetail(projectId = project.id, projectName = project.name))
                    },
                    onNavigate = { navKey -> onItemClick(navKey) },
                    onLogout = onLogout
                )
                1 -> EmployeesScreen(
                    viewModel = projectViewModel
                )
                2 -> FinancialsScreen(
                    viewModel = projectViewModel
                )
                3 -> RemindersScreen(
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
