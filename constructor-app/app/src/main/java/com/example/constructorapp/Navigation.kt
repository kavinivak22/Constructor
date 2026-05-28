package com.example.constructorapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.ui.main.MainScreen
import com.example.constructorapp.ui.screens.LoginScreen
import com.example.constructorapp.ui.screens.WorklogScreen
import com.example.constructorapp.ui.screens.AddEditReminderScreen
import com.example.constructorapp.ui.viewmodel.AuthViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel

@Composable
fun MainNavigation(
    authViewModel: AuthViewModel,
    projectViewModel: ProjectViewModel,
    reminderViewModel: ReminderViewModel
) {
    val startDestination = if (SupabaseClient.authToken != null) Main else Login
    val backStack = rememberNavBackStack(startDestination)

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
                    onItemClick = { navKey -> backStack.add(navKey) },
                    projectViewModel = projectViewModel,
                    reminderViewModel = reminderViewModel,
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
            entry<AddEditReminder> { key ->
                AddEditReminderScreen(
                    reminderId = key.reminderId,
                    viewModel = reminderViewModel,
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
