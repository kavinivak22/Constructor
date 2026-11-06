package com.example.constructorapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.example.constructorapp.BuildingPlan
import com.example.constructorapp.Expenses
import com.example.constructorapp.MaterialEstimation
import com.example.constructorapp.PurchaseOrders
import com.example.constructorapp.WorkPrep
import com.example.constructorapp.Worklog
import com.example.constructorapp.data.local.entity.TaskEntity
import com.example.constructorapp.data.remote.PouchTransactionDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AlertFlipper
import com.example.constructorapp.ui.components.DashboardAlert
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.components.sampleAlerts
import com.example.constructorapp.ui.main.ActivityType
import com.example.constructorapp.ui.main.DashboardViewModel
import com.example.constructorapp.ui.main.RecentActivityItem
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.CurrencyFormatter
import com.example.constructorapp.util.toInrCompact
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel,
    dashboardViewModel: DashboardViewModel? = null,
    onProjectClick: (ProjectDto) -> Unit,
    onViewAllProjects: () -> Unit = {},
    onNavigate: (NavKey) -> Unit = {},
    onLogout: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val realWorklogs by viewModel.worklogsWithDetails.collectAsState()
    val isLoading by viewModel.loading.collectAsState()

    var alertsList by remember { mutableStateOf(sampleAlerts) }
    var isExpenseDialogOpen by remember { mutableStateOf(false) }
    var isCreateProjectOpen by remember { mutableStateOf(false) }

    // My Tasks Local State
    var personalTasks by remember {
        mutableStateOf(
            listOf(
                TaskEntity(
                    id = "task-local-1",
                    title = "Verify steel delivery manifest for Foundation",
                    status = "pending",
                    priority = "High"
                ),
                TaskEntity(
                    id = "task-local-2",
                    title = "Call electrical contractor for slab conduit approval",
                    status = "completed",
                    priority = "Medium"
                ),
                TaskEntity(
                    id = "task-local-3",
                    title = "Review cement cube compression test lab report",
                    status = "pending",
                    priority = "Low"
                )
            )
        )
    }

    var newTaskInput by rememberSaveable { mutableStateOf("") }
    var editingTaskId by remember { mutableStateOf<String?>(null) }
    var editingTaskTitle by remember { mutableStateOf("") }

    val tomorrowDateLabel = remember {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.US).format(calendar.time)
    }

    // Live Recent Activity mapped directly from Supabase / Room Worklogs
    val recentActivities = remember(realWorklogs, projects) {
        realWorklogs.take(6).map { detail ->
            val worklog = detail.worklog
            val proj = projects.find { it.id == worklog.projectId }
            val projName = proj?.name ?: "LVA 24A"

            val labor = detail.laborEntries.firstOrNull()?.laborEntry
            val mat = detail.materials.firstOrNull()
            val photos = detail.photos

            val (desc, type) = when {
                labor != null -> {
                    val text = if (!labor.workDescription.isNullOrBlank()) {
                        "${labor.contractorName}: ${labor.workDescription}"
                    } else {
                        "${labor.contractorName} on site"
                    }
                    text to ActivityType.LABOR
                }
                mat != null -> {
                    "Consumed ${mat.quantityConsumed} ${mat.unit} of ${mat.materialName}" to ActivityType.MATERIAL
                }
                photos.isNotEmpty() -> {
                    "${photos.size} site photo(s) captured & verified" to ActivityType.PHOTO
                }
                else -> {
                    (if (!worklog.title.isNullOrBlank()) worklog.title else "Daily site log recorded") to ActivityType.GENERAL
                }
            }

            RecentActivityItem(
                id = worklog.id,
                projectName = projName,
                description = desc,
                timestampAgo = formatDashboardTimeAgo(worklog.date),
                type = type
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.syncRecentActivities()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Alert Flipper / Sentinel Mascot (Webapp Split-Flap)
            item {
                AlertFlipper(
                    alerts = alertsList,
                    onAlertDismiss = { alertId ->
                        if (alertId == "__SIMULATE_EMPTY__") {
                            alertsList = emptyList()
                        } else {
                            alertsList = alertsList.filter { it.id != alertId }
                        }
                    },
                    onRestoreAlerts = {
                        alertsList = sampleAlerts
                    }
                )
            }

            // 3. Quick Actions Row (3 columns: Worklog, Expense, Materials)
            item {
                QuickActionsRow(
                    onWorklogClick = {
                        onNavigate(Worklog(projectId = "", projectName = "All Projects"))
                    },
                    onExpenseClick = {
                        onNavigate(Expenses())
                    },
                    onMaterialsClick = {
                        onNavigate(MaterialEstimation())
                    }
                )
            }

            // 4. Tomorrow's Work Prep Highlight Banner
            item {
                TomorrowPrepBanner(
                    scheduledTasksCount = 0,
                    dateLabel = tomorrowDateLabel,
                    onPrepClick = {
                        onNavigate(WorkPrep(projectId = null))
                    }
                )
            }

            // 5. Active Projects Header & Cards
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
                        modifier = Modifier.clickable {
                            onViewAllProjects()
                        }
                    )
                }
            }

            if (isLoading && projects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }
                }
            } else if (projects.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No Projects Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Create your first construction project to track schedules, worklogs, and materials.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { isCreateProjectOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Project", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    DashboardProjectCard(
                        project = project,
                        onClick = { onProjectClick(project) }
                    )
                }
            }

            // 6. My Tasks Section (Personal Checklist with inline add/edit/toggle/delete)
            item {
                MyTasksSection(
                    tasks = personalTasks,
                    newTaskInput = newTaskInput,
                    onNewTaskInputChange = { newTaskInput = it },
                    onAddTask = {
                        val title = newTaskInput.trim()
                        if (title.isNotEmpty()) {
                            personalTasks = listOf(
                                TaskEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    status = "pending",
                                    priority = "Medium"
                                )
                            ) + personalTasks
                            newTaskInput = ""
                        }
                    },
                    onToggleTask = { taskId, isCompleted ->
                        personalTasks = personalTasks.map {
                            if (it.id == taskId) it.copy(status = if (isCompleted) "completed" else "pending") else it
                        }
                    },
                    editingTaskId = editingTaskId,
                    editingTaskTitle = editingTaskTitle,
                    onStartEditing = { id, title ->
                        editingTaskId = id
                        editingTaskTitle = title
                    },
                    onEditingTitleChange = { editingTaskTitle = it },
                    onSaveEdit = { id ->
                        val trimmed = editingTaskTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            personalTasks = personalTasks.map {
                                if (it.id == id) it.copy(title = trimmed) else it
                            }
                        }
                        editingTaskId = null
                        editingTaskTitle = ""
                    },
                    onCancelEdit = {
                        editingTaskId = null
                        editingTaskTitle = ""
                    },
                    onDeleteTask = { id ->
                        personalTasks = personalTasks.filter { it.id != id }
                    }
                )
            }

            // 7. Recent Activity Timeline
            item {
                ActivityTimelineSection(
                    activities = recentActivities,
                    onActivityClick = { act ->
                        val proj = projects.find { it.name.equals(act.projectName, ignoreCase = true) }
                        onNavigate(Worklog(projectId = proj?.id ?: "", projectName = act.projectName))
                    },
                    onRecordWorklogClick = {
                        onNavigate(Worklog(projectId = "", projectName = "All Projects"))
                    }
                )
            }
        }
    }

    // Expense Form Sheet / Dialog
    if (isExpenseDialogOpen) {
        ExpenseQuickDialog(
            projects = projects,
            onDismiss = { isExpenseDialogOpen = false },
            onSubmit = { amount, category, desc, projId ->
                viewModel.createPouchTransaction(
                    PouchTransactionDto(
                        projectId = projId,
                        amount = amount,
                        category = category,
                        description = desc,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    )
                ) {
                    isExpenseDialogOpen = false
                }
            }
        )
    }

    if (isCreateProjectOpen) {
        CreateProjectDialog(
            onDismiss = { isCreateProjectOpen = false },
            onSubmit = { newProject ->
                viewModel.createProject(newProject) { success ->
                    if (success) {
                        isCreateProjectOpen = false
                        viewModel.loadProjects()
                    }
                }
            }
        )
    }
}

// ─── 1. Quick Actions Row ───────────────────────────────────────────────────

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

// ─── 2. Tomorrow's Work Prep Banner ─────────────────────────────────────────

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

// ─── 3. Project Card with Indian Currency ───────────────────────────────────

@Composable
fun DashboardProjectCard(
    project: ProjectDto,
    onClick: () -> Unit
) {
    val budgetDisplay = when {
        project.budget >= 10_000_000 -> "₹${String.format(Locale.US, "%.1f", project.budget / 10_000_000.0)}Cr"
        project.budget >= 100_000 -> "₹${String.format(Locale.US, "%.1f", project.budget / 100_000.0)}L"
        project.budget > 0 -> "₹${(project.budget / 1_000).toInt()}k"
        else -> null
    }
    val statusText = (project.status ?: "active").replaceFirstChar { it.uppercase() }
    val isCompletedOrActive = statusText.equals("active", ignoreCase = true) || statusText.equals("completed", ignoreCase = true)
    val statusColor = if (isCompletedOrActive) EmeraldSuccess else AmberWarning
    val statusContainer = if (isCompletedOrActive) EmeraldSuccess.copy(alpha = 0.12f) else AmberWarning.copy(alpha = 0.12f)
    val statusBorder = if (isCompletedOrActive) EmeraldSuccess.copy(alpha = 0.30f) else AmberWarning.copy(alpha = 0.30f)

    val formattedDate = remember(project.startDate) {
        val sDate = project.startDate
        if (sDate.isNullOrBlank()) "Aug 24"
        else {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = parser.parse(sDate.take(10))
                if (date != null) {
                    SimpleDateFormat("MMM d", Locale.US).format(date)
                } else sDate.take(10)
            } catch (e: Exception) {
                sDate.take(10)
            }
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Name, Client, Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = project.name,
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!project.clientName.isNullOrBlank()) {
                        Text(
                            text = project.clientName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusContainer)
                        .border(BorderStroke(1.dp, statusBorder), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            val projectImageUrl = if (!project.thumbnailUrl.isNullOrBlank()) {
                project.thumbnailUrl
            } else when {
                project.name.contains("PKGN", ignoreCase = true) -> "https://images.unsplash.com/photo-1612935089040-89195ef54677?auto=format&fit=crop&w=600&q=80"
                project.name.contains("LVA", ignoreCase = true) -> "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=600&q=80"
                project.name.contains("qwas", ignoreCase = true) -> "https://images.unsplash.com/photo-1634250878303-0c059dc54c4b?auto=format&fit=crop&w=600&q=80"
                project.name.contains("1234", ignoreCase = true) -> "https://images.unsplash.com/photo-1646608220368-c604d8e8130f?auto=format&fit=crop&w=600&q=80"
                else -> "https://images.unsplash.com/photo-1603287696448-61d5b81cb720?auto=format&fit=crop&w=600&q=80"
            }

            // Hero Image: EDGE-TO-EDGE across card (matches webapp relative h-36 w-full)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .background(SlateSurfaceSubtle)
            ) {
                AsyncImage(
                    model = projectImageUrl,
                    contentDescription = project.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Body: Progress & Indian Currency
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Text("${project.progress}%", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { project.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BrandPrimary,
                    trackColor = SlateSurfaceSubtle
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SlateDivider.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = BrandPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formattedDate,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }

                    if (budgetDisplay != null) {
                        Text(
                            text = budgetDisplay,
                            fontFamily = OutfitFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ─── 4. My Tasks Checklist ──────────────────────────────────────────────────

@Composable
fun MyTasksSection(
    tasks: List<TaskEntity>,
    newTaskInput: String,
    onNewTaskInputChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    editingTaskId: String?,
    editingTaskTitle: String,
    onStartEditing: (String, String) -> Unit,
    onEditingTitleChange: (String) -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteTask: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Tasks",
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${tasks.count { it.status == "completed" }}/${tasks.size} Done",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick-add input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newTaskInput,
                    onValueChange = onNewTaskInputChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (newTaskInput.isEmpty()) {
                                Text(
                                    "Add personal to-do...",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddTask,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimary)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tasks.forEach { task ->
                    val isCompleted = task.status == "completed"
                    val isEditing = editingTaskId == task.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateSurfaceSubtle)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { isChecked ->
                                onToggleTask(task.id, isChecked)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldSuccess,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editingTaskTitle,
                                onValueChange = onEditingTitleChange,
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                colors = calculatorFieldColors()
                            )
                            IconButton(onClick = { onSaveEdit(task.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onCancelEdit, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = RoseDanger, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Text(
                                text = task.title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                color = if (isCompleted) TextSecondary else TextPrimary,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(
                                onClick = { onStartEditing(task.id, task.title) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(13.dp))
                            }
                            IconButton(
                                onClick = { onDeleteTask(task.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 5. Recent Activity Timeline ────────────────────────────────────────────

fun formatDashboardTimeAgo(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "RECENT"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString.substringBefore("T")) ?: return dateString.uppercase()
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { time = date }

        val nowDay = now.get(Calendar.DAY_OF_YEAR)
        val nowYear = now.get(Calendar.YEAR)
        val thenDay = then.get(Calendar.DAY_OF_YEAR)
        val thenYear = then.get(Calendar.YEAR)

        if (nowYear == thenYear) {
            when (val diff = nowDay - thenDay) {
                0 -> "TODAY"
                1 -> "YESTERDAY"
                in 2..6 -> "$diff DAYS AGO"
                in 7..27 -> "${diff / 7} WEEKS AGO"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date).uppercase()
            }
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date).uppercase()
        }
    } catch (_: Exception) {
        dateString.take(10).uppercase()
    }
}

@Composable
fun ActivityTimelineSection(
    activities: List<RecentActivityItem>,
    onActivityClick: ((RecentActivityItem) -> Unit)? = null,
    onRecordWorklogClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (activities.isNotEmpty() && onRecordWorklogClick != null) {
                    Text(
                        text = "+ Log Work",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary,
                        modifier = Modifier.clickable { onRecordWorklogClick() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "No recent activity logged yet",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Daily worklogs recorded on site will appear here",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }
                }
            } else {
                activities.forEachIndexed { index, activity ->
                    val icon = when (activity.type) {
                        ActivityType.LABOR -> Icons.Outlined.Build
                        ActivityType.MATERIAL -> Icons.Outlined.Inventory2
                        ActivityType.PHOTO -> Icons.Outlined.Image
                        ActivityType.GENERAL -> Icons.Outlined.Description
                    }
                    val iconColor = when (activity.type) {
                        ActivityType.LABOR -> AmberWarning
                        ActivityType.MATERIAL -> SkyInfo
                        ActivityType.PHOTO -> PurpleAccent
                        ActivityType.GENERAL -> EmeraldSuccess
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onActivityClick != null) {
                                onActivityClick?.invoke(activity)
                            }
                    ) {
                        // Left Node Column with Connecting Line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                            }
                            if (index < activities.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(SlateBorder)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right Activity Details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (index < activities.size - 1) 14.dp else 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activity.projectName,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•  ${activity.timestampAgo}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activity.description,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 6. Expense Quick Dialog ────────────────────────────────────────────────

@Composable
fun ExpenseQuickDialog(
    projects: List<ProjectDto>,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, category: String, description: String, projectId: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Urgent Hardware") }
    var description by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: "") }

    val parsedAmount = CurrencyFormatter.parseInr(amountStr) ?: 0.0
    val isFormValid = parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("+ Quick Site Expense", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("e.g. 1500") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                Text("Expense Category", fontSize = 11.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Urgent Hardware", "Transport", "Tea/Snacks").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Purpose") },
                    placeholder = { Text("e.g. Binding wire & nails for beam") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        onSubmit(parsedAmount, category, description, selectedProjectId)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Record Expense")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SlateSurface
    )
}
