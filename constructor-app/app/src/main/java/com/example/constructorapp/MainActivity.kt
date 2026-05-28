package com.example.constructorapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.data.repository.ReminderRepository
import com.example.constructorapp.theme.ConstructorAppTheme
import com.example.constructorapp.ui.viewmodel.AuthViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.ui.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {

    // Lazy initialization of Database & Repositories
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val projectRepository by lazy { ProjectRepository(database.appDao) }
    private val reminderRepository by lazy { ReminderRepository(database.appDao) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    // ViewModel Injectors
    private val authViewModel: AuthViewModel by viewModels()
    
    @Suppress("UNCHECKED_CAST")
    private val projectViewModel: ProjectViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProjectViewModel(projectRepository) as T
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val reminderViewModel: ReminderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReminderViewModel(reminderRepository, workManager) as T
            }
        }
    }

    // Permission Launcher for Notifications and Location Services
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions dynamically
        requestPermissions()

        enableEdgeToEdge()
        setContent {
            ConstructorAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        authViewModel = authViewModel,
                        projectViewModel = projectViewModel,
                        reminderViewModel = reminderViewModel
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
