package com.example.constructorapp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.AddEditReminder
import com.example.constructorapp.Worklog
import com.example.constructorapp.ui.screens.DashboardScreen
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
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Business, contentDescription = "Projects") },
                    label = { Text("Projects", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Reminders") },
                    label = { Text("Reminders", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        },
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
                        onItemClick(Worklog(projectId = project.id, projectName = project.name))
                    },
                    onLogout = onLogout
                )
                1 -> RemindersScreen(
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
