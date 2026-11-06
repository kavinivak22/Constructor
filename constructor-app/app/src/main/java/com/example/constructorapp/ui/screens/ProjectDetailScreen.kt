package com.example.constructorapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.example.constructorapp.*
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.remote.DailyWorklogDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.AppDrawerContent
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.CurrencyFormatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ProjectDetailScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onNavigate: (NavKey) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToTab: (Int) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val projects by viewModel.projects.collectAsState()
    val project = projects.find { it.id == projectId } ?: ProjectDto(
        id = projectId,
        name = projectName,
        companyId = ""
    )
    val tasks by viewModel.tasks.collectAsState()
    val worklogs by viewModel.worklogs.collectAsState()
    val worklogsWithDetails by viewModel.worklogsWithDetails.collectAsState()

    var isEditProjectOpen by remember { mutableStateOf(false) }
    var isCreateTaskOpen by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
        viewModel.selectProject(projectId)
        viewModel.selectWorklogProject(projectId)
    }

    val budget = project.budget
    val spentEstimate = budget * (project.progress / 100.0)
    val remainingEstimate = (budget - spentEstimate).coerceAtLeast(0.0)
    val spentPercentage = if (budget > 0) ((spentEstimate / budget) * 100.0).coerceIn(0.0, 100.0) else 0.0

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerContentColor = TextPrimary,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(310.dp)
            ) {
                AppDrawerContent(
                    userEmail = SupabaseClient.userEmail,
                    userDisplayName = SupabaseClient.userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User",
                    userRole = "admin",
                    selectedRoute = "/projects",
                    onNavigateRoute = { route ->
                        coroutineScope.launch { drawerState.close() }
                        when (route) {
                            "/" -> onNavigateToTab(0)
                            "/projects" -> onNavigateToTab(1)
                            "/purchase-orders" -> onNavigateToTab(2)
                            "/team-hub" -> onNavigateToTab(3)
                            "/inventory" -> onNavigate(Inventory())
                            "/company-materials" -> onNavigate(CompanyMaterials)
                            "/contractors" -> onNavigate(Contractors())
                            "/payday" -> onNavigate(Payday())
                            "/expenses" -> onNavigate(Expenses(projectId = projectId))
                            "/notifications" -> onNavigate(Notifications)
                            "/profile" -> onNavigate(Profile)
                            "/personal-pouch" -> onNavigate(PersonalPouchNav)
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
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Ambient Drifting Blobs Canvas Background (Webapp exact ambient canvas)
            AmbientBlobBackground()

            // Layer 2: Main Screen Content with top status bar clearance
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    // 1. Mobile Inline Header with Status Bar Clearance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (project.clientName?.take(1) ?: "C").uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = project.clientName ?: "Internal Project",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { isEditProjectOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // 2. Scrollable Body with bottom clearance for floating navbar
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // A. Big Overall Progress Card
                        item {
                            val completedTasksCount = remember(tasks) { tasks.count { it.status == "completed" } }
                            OverallProgressCard(
                                project = project,
                                budget = budget,
                                memberCount = 12,
                                spentPercentage = spentPercentage,
                                completedTasks = completedTasksCount,
                                totalTasks = tasks.size
                            )
                        }

                        // B. Asymmetric 5-Column Mobile Action Grid
                        item {
                            AsymmetricActionGrid(
                                projectId = projectId,
                                projectName = project.name,
                                onNavigate = onNavigate
                            )
                        }

                        // C. Adaptive Next Task Widget
                        item {
                            AdaptiveNextTaskWidget(
                                projectId = projectId,
                                projectName = project.name,
                                tasks = tasks,
                                worklogs = worklogs,
                                worklogsWithDetails = worklogsWithDetails,
                                onTaskToggle = { taskId, isChecked ->
                                    viewModel.toggleTask(taskId, isChecked, projectId)
                                },
                                onNavigate = onNavigate
                            )
                        }

                        // D. Building Plan Summary Card
                        item {
                            BuildingPlanSummaryCard(
                                projectId = projectId,
                                projectName = project.name,
                                onNavigate = onNavigate
                            )
                        }

                        // E. Recent Updates Carousel (Daily Logs)
                        item {
                            RecentUpdatesCarousel(
                                projectId = projectId,
                                worklogs = worklogs,
                                onWorklogClick = { log ->
                                    onNavigate(Worklog(projectId, project.name))
                                }
                            )
                        }

                        // F. Project Tasks List
                        item {
                            ProjectTasksCard(
                                tasks = tasks,
                                onToggleTask = { taskId, isChecked ->
                                    viewModel.toggleTask(taskId, isChecked, projectId)
                                },
                                onAddTaskClick = { isCreateTaskOpen = true }
                            )
                        }

                        // G. Budget Overview Bar
                        item {
                            BudgetOverviewCard(
                                budget = budget,
                                spent = spentEstimate,
                                remaining = remainingEstimate,
                                spentPercentage = spentPercentage
                            )
                        }
                    }
                }
            }

            // Layer 3: Floating Bottom Navigation Bar (Truly floating over scrolling content)
            FloatingBottomNav(
                selectedTab = 1,
                onTabSelected = { index ->
                    onNavigateToTab(index)
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

    if (isEditProjectOpen) {
        EditProjectDialog(
            project = project,
            onDismiss = { isEditProjectOpen = false },
            onSubmit = { updatedProject ->
                viewModel.updateProject(updatedProject) { success ->
                    if (success) {
                        isEditProjectOpen = false
                        viewModel.loadProjects()
                    }
                }
            }
        )
    }

    if (isCreateTaskOpen) {
        CreateTaskDialog(
            projectId = projectId,
            onDismiss = { isCreateTaskOpen = false },
            onSubmit = { newTask: TaskItemDto ->
                viewModel.createTask(newTask) {
                    isCreateTaskOpen = false
                    viewModel.loadTasks(projectId)
                }
            }
        )
    }
}

// ─── A. Big Overall Progress Card ───────────────────────────────────────────

@Composable
fun OverallProgressCard(
    project: ProjectDto,
    budget: Double,
    memberCount: Int,
    spentPercentage: Double,
    completedTasks: Int = 0,
    totalTasks: Int = 0
) {
    val budgetDisplay = CurrencyFormatter.formatCompact(budget)
    val spentEstimate = budget * (project.progress / 100.0)
    val remainingEstimate = (budget - spentEstimate).coerceAtLeast(0.0)
    val remainingDisplay = CurrencyFormatter.formatCompact(remainingEstimate)
    val spentDisplay = CurrencyFormatter.formatCompact(spentEstimate)

    val targetProgress = (project.progress / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "overall_progress_anim"
    )

    val isComplete = project.progress >= 100
    val statusLabel = when {
        isComplete -> "Completed"
        !project.status.isNullOrBlank() -> project.status.replace("_", " ").replaceFirstChar { it.uppercase() }
        project.progress > 0 -> "In Progress"
        else -> "Planning"
    }
    val statusColor = when {
        isComplete -> EmeraldSuccess
        project.status?.contains("delay", ignoreCase = true) == true -> RoseDanger
        project.progress > 0 -> BrandPrimary
        else -> TextSecondary
    }
    val statusBg = when {
        isComplete -> EmeraldSuccessContainer
        project.status?.contains("delay", ignoreCase = true) == true -> RoseDangerContainer
        project.progress > 0 -> BrandPrimaryContainer
        else -> SlateSurfaceSubtle
    }

    val phaseTitle = when {
        project.progress >= 100 -> "Project Completed & Delivered"
        project.progress >= 75 -> "Phase 4 • Finishing & Handover"
        project.progress >= 50 -> "Phase 3 • MEP & Masonry Works"
        project.progress >= 25 -> "Phase 2 • Structure & Framing"
        project.progress > 0 -> "Phase 1 • Site Prep & Foundation"
        else -> "Initial Planning & Mobilization"
    }

    val daysRemainingText = remember(project.endDate) {
        project.endDate?.take(10)?.let { dateStr ->
            try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val endCal = java.util.Calendar.getInstance().apply {
                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    }
                    val nowCal = java.util.Calendar.getInstance()
                    val diffMillis = endCal.timeInMillis - nowCal.timeInMillis
                    val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        diffDays > 1 -> "$diffDays days left"
                        diffDays == 1 -> "Due tomorrow"
                        diffDays == 0 -> "Due today"
                        diffDays < 0 -> "${-diffDays}d overdue"
                        else -> dateStr
                    }
                } else dateStr
            } catch (_: Exception) {
                dateStr
            }
        } ?: "Scheduled"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Top Tracker Subtitle + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                    Text(
                        text = "OVERALL PROGRESS",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )
                }

                // Status Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero Metric: Huge Numerical Display + Phase Title + Radial Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${(animatedProgress * 100).roundToInt()}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 40.sp
                        )
                        Text(
                            text = "%",
                            fontFamily = OutfitFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phaseTitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Checklist,
                            contentDescription = null,
                            tint = if (isComplete) EmeraldSuccess else BrandPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (totalTasks > 0) "$completedTasks of $totalTasks milestones reached" else "Milestone tracking active",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Radial Progress Gauge
                RadialProgressGauge(
                    progress = targetProgress,
                    isCompleted = isComplete
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Multi-Stage Progress Bar with Milestone Segment Markers
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SlateSurfaceSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, Color(0xFFFF8533), Color(0xFFFBBF24))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Milestone Nodes (25%, 50%, 75%, 100%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val milestones = listOf(
                        25 to "Foundation",
                        50 to "Structure",
                        75 to "Finishing",
                        100 to "Handover"
                    )
                    milestones.forEach { (percent, title) ->
                        val isReached = project.progress >= percent
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isReached) BrandPrimary else SlateSurfaceSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$percent%",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 9.sp,
                                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Medium,
                                color = if (isReached) BrandPrimary else TextMutedDark
                            )
                            Text(
                                text = title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 8.5.sp,
                                color = if (isReached) TextSecondary else TextMutedDark
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0x140F172A),
                thickness = 1.dp
            )

            // 4. Bento-Style 4-Block Metrics Grid (2x2 on Mobile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Default.CalendarToday,
                    iconTint = BrandPrimary,
                    iconContainerColor = BrandPrimaryContainer,
                    label = "End Date",
                    value = project.endDate?.take(10) ?: "Scheduled",
                    subtext = daysRemainingText,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.CurrencyRupee,
                    iconTint = EmeraldSuccess,
                    iconContainerColor = EmeraldSuccessContainer,
                    label = "Budget",
                    value = budgetDisplay,
                    subtext = "$remainingDisplay balance",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Default.People,
                    iconTint = AmberWarning,
                    iconContainerColor = AmberWarningContainer,
                    label = "Team Size",
                    value = "$memberCount members",
                    subtext = "Active on site",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.PieChart,
                    iconTint = SkyInfo,
                    iconContainerColor = SkyInfoContainer,
                    label = "Spent",
                    value = "${spentPercentage.roundToInt()}%",
                    subtext = "$spentDisplay est. spend",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RadialProgressGauge(
    progress: Float,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "radial_progress_anim"
    )

    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val strokeWidth = 6.5.dp.toPx()
            val arcSize = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)

            // Background Track
            drawArc(
                color = SlateSurfaceSubtle,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Foreground Sweep Arc
            if (animatedProgress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to BrandPrimary,
                        0.7f to Color(0xFFFF9E44),
                        1.0f to BrandPrimary,
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Icon Container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) EmeraldSuccessContainer else BrandPrimaryContainer.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = if (isCompleted) EmeraldSuccess else BrandPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    iconContainerColor: Color = iconTint.copy(alpha = 0.12f)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC).copy(alpha = 0.85f))
            .border(1.dp, Color(0x100F172A), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 9.5.sp,
                        color = TextMutedDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── B. Asymmetric 5-Column Mobile Action Grid ───────────────────────────────

@Composable
fun AsymmetricActionGrid(
    projectId: String,
    projectName: String,
    onNavigate: (NavKey) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Materials (2 cols) + View Expenses (3 cols)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNavigate(ProjectMaterials(projectId, projectName)) },
                modifier = Modifier
                    .weight(2f)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Materials", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { onNavigate(Expenses(projectId = projectId)) },
                modifier = Modifier
                    .weight(3f)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Expenses", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Row 2: Tomorrow's Prep (3 cols) + Add Log (2 cols)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onNavigate(WorkPrep(projectId = projectId)) },
                modifier = Modifier
                    .weight(3f)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tomorrow's Prep", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = { onNavigate(Worklog(projectId, projectName)) },
                modifier = Modifier
                    .weight(2f)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Log", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Row 3: Project Pouch (5 cols full width)
        OutlinedButton(
            onClick = { onNavigate(ProjectPouch(projectId, projectName)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Filled.FolderShared, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Project Pouch (Vault & Storage)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── C. Adaptive Next Task Widget ───────────────────────────────────────────

data class TaskQualityCheck(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

data class SmartWorklogCandidate(
    val id: String,
    val title: String,
    val date: String,
    val workerCount: Int,
    val photoCount: Int,
    val materialsSummary: String,
    val laborDescription: String,
    val score: Int = 0,
    val matchReasons: List<String> = emptyList(),
    val confidenceBadge: String? = null,
    val isLinked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNextTaskWidget(
    projectId: String,
    projectName: String,
    tasks: List<TaskItemDto>,
    worklogs: List<DailyWorklogDto> = emptyList(),
    worklogsWithDetails: List<WorklogWithDetails> = emptyList(),
    onTaskToggle: (String, Boolean) -> Unit,
    onNavigate: (NavKey) -> Unit
) {
    // If no tasks exist, provide a realistic default next task
    val displayTasks = remember(tasks) {
        if (tasks.isNotEmpty()) tasks else listOf(
            TaskItemDto(
                id = "default_foundation_rebar",
                projectId = projectId,
                title = "Footing Steel Rebar Mat & Column Starters",
                description = "Verify mesh bar spacing (150mm c/c) and 50mm cover block placement under mesh.",
                status = "pending"
            )
        )
    }

    var taskIndex by remember { mutableIntStateOf(0) }
    val currentTask = displayTasks.getOrNull(taskIndex.coerceIn(0, displayTasks.size - 1)) ?: displayTasks.first()

    // Quality Checklists State Map (Task ID -> List of checks)
    val checklistsMap = remember {
        mutableStateMapOf<String, List<TaskQualityCheck>>()
    }

    // Default Quality Checkpoints generator based on task title keywords
    fun getOrCreateChecksForTask(task: TaskItemDto): List<TaskQualityCheck> {
        val taskId = task.id ?: "t_default"
        return checklistsMap.getOrPut(taskId) {
            val titleLower = task.title.lowercase()
            when {
                titleLower.contains("excavat") || titleLower.contains("soil") || titleLower.contains("pit") -> listOf(
                    TaskQualityCheck("c1", "Verify excavation benchmark depth & line pins", task.isCompleted),
                    TaskQualityCheck("c2", "Inspect soil bearing strata level & firm base", task.isCompleted),
                    TaskQualityCheck("c3", "Ensure pit shoring and anti-termite spray", task.isCompleted)
                )
                titleLower.contains("pcc") || titleLower.contains("bed") -> listOf(
                    TaskQualityCheck("c1", "Verify 4-inch PCC bed thickness and level", task.isCompleted),
                    TaskQualityCheck("c2", "Check concrete mix ratio 1:4:8 consistency", task.isCompleted),
                    TaskQualityCheck("c3", "Ensure wet curing for at least 3 days", task.isCompleted)
                )
                titleLower.contains("rebar") || titleLower.contains("footing") || titleLower.contains("column") -> listOf(
                    TaskQualityCheck("c1", "Verify mesh bar spacing (150mm c/c)", task.isCompleted),
                    TaskQualityCheck("c2", "Check 50mm cover block placement under mesh", task.isCompleted),
                    TaskQualityCheck("c3", "Check vertical column starter plumb alignment", task.isCompleted),
                    TaskQualityCheck("c4", "Inspect lap lengths and binding wire tightness", task.isCompleted)
                )
                titleLower.contains("slab") || titleLower.contains("roof") || titleLower.contains("beam") -> listOf(
                    TaskQualityCheck("c1", "Check props verticality & sole plate stability", task.isCompleted),
                    TaskQualityCheck("c2", "Inspect main & distribution bar bent-up chairs", task.isCompleted),
                    TaskQualityCheck("c3", "Electrical conduit pipes & fan box placement", task.isCompleted)
                )
                titleLower.contains("brick") || titleLower.contains("wall") || titleLower.contains("masonry") -> listOf(
                    TaskQualityCheck("c1", "Check horizontal course alignment with string line", task.isCompleted),
                    TaskQualityCheck("c2", "Verify 1:6 cement mortar mix ratio", task.isCompleted),
                    TaskQualityCheck("c3", "Check wall plumb bob alignment on both faces", task.isCompleted)
                )
                else -> listOf(
                    TaskQualityCheck("c1", "Verify material grade & specifications compliance", task.isCompleted),
                    TaskQualityCheck("c2", "Check dimensional accuracy against architectural drawing", task.isCompleted),
                    TaskQualityCheck("c3", "Site safety audit & supervisor sign-off", task.isCompleted)
                )
            }
        }
    }

    val currentChecks = getOrCreateChecksForTask(currentTask)
    val completedChecksCount = currentChecks.count { it.isCompleted }
    val totalChecksCount = currentChecks.size
    val checkProgressPct = if (totalChecksCount > 0) (completedChecksCount * 100) / totalChecksCount else 0

    // Linked Worklogs State (Task ID -> Set of linked worklog IDs)
    var linkedWorklogsMap by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    val currentTaskId = currentTask.id ?: "t_default"
    val currentTaskLinkedIds = linkedWorklogsMap[currentTaskId] ?: emptySet()

    fun linkWorklog(logId: String) {
        val existing = linkedWorklogsMap[currentTaskId] ?: emptySet()
        linkedWorklogsMap = linkedWorklogsMap + (currentTaskId to (existing + logId))
    }

    fun unlinkWorklog(logId: String) {
        val existing = linkedWorklogsMap[currentTaskId] ?: emptySet()
        linkedWorklogsMap = linkedWorklogsMap + (currentTaskId to (existing - logId))
    }

    // Raw candidate pool: map real worklogsWithDetails/worklogs, or use rich site logs if empty
    val rawCandidates = remember(worklogs, worklogsWithDetails) {
        if (worklogsWithDetails.isNotEmpty()) {
            worklogsWithDetails.map { detail ->
                val totalWorkers = detail.laborEntries.sumOf { e -> e.workerCounts.sumOf { it.count } }
                val matStr = detail.materials.joinToString(", ") { "${it.materialName} (${it.quantityConsumed} ${it.unit ?: ""})".trim() }
                val laborDesc = detail.laborEntries.mapNotNull { it.laborEntry.workDescription }.joinToString("; ")
                SmartWorklogCandidate(
                    id = detail.worklog.id,
                    title = detail.worklog.title ?: "Site Daily Log",
                    date = detail.worklog.date,
                    workerCount = totalWorkers,
                    photoCount = detail.photos.size,
                    materialsSummary = matStr,
                    laborDescription = laborDesc
                )
            }
        } else if (worklogs.isNotEmpty()) {
            worklogs.map { log ->
                SmartWorklogCandidate(
                    id = log.id ?: "",
                    title = log.title ?: "Site Daily Log",
                    date = log.date,
                    workerCount = 6,
                    photoCount = 1,
                    materialsSummary = "",
                    laborDescription = ""
                )
            }
        } else {
            // Realistic simulated site worklogs with rich context
            listOf(
                SmartWorklogCandidate(
                    id = "sample_log_rebar",
                    title = "Footing Steel Rebar Mat & Column Starter Binding",
                    date = "2026-11-05",
                    workerCount = 8,
                    photoCount = 3,
                    materialsSummary = "TMT Steel Fe550D (1.2 Tons), Binding wire (25 kg)",
                    laborDescription = "Rebar cutting, bending, footing mesh tie, and 50mm cover blocks fixing"
                ),
                SmartWorklogCandidate(
                    id = "sample_log_pcc",
                    title = "PCC Bed 1:4:8 Leveling & Pit Trench Curing",
                    date = "2026-11-04",
                    workerCount = 6,
                    photoCount = 2,
                    materialsSummary = "OPC 53 Cement (45 Bags), River Sand, 20mm Aggregate",
                    laborDescription = "Level bed plain concrete pour, compaction, and water ponding"
                ),
                SmartWorklogCandidate(
                    id = "sample_log_survey",
                    title = "Site Clearance, Grid Pin Marking & Benchmark Setup",
                    date = "2026-11-03",
                    workerCount = 4,
                    photoCount = 1,
                    materialsSummary = "Lime powder, Wooden offset pegs, String lines",
                    laborDescription = "Boundary survey verification, excavation center-lines, and level offset pins"
                ),
                SmartWorklogCandidate(
                    id = "sample_log_column",
                    title = "RCC Column Shuttering & Plumb Bob Verticality Inspection",
                    date = "2026-11-02",
                    workerCount = 7,
                    photoCount = 2,
                    materialsSummary = "Steel prop jacks, Shuttering oil, 40mm Cover blocks",
                    laborDescription = "Column box alignment with plumb-bob and bracing sole plates"
                ),
                SmartWorklogCandidate(
                    id = "sample_log_plinth",
                    title = "Plinth Beam Shuttering & DPC Bitumen Coating",
                    date = "2026-11-01",
                    workerCount = 5,
                    photoCount = 1,
                    materialsSummary = "DPC Bitumen Primer, Rebar Stirrups 8mm",
                    laborDescription = "Beam cage positioning, side shuttering, and anti-termite perimeter spray"
                )
            )
        }
    }

    // AI Smart Matching & Relevance Scoring against currentTask.title
    val scoredCandidates = remember(rawCandidates, currentTask.title, currentTaskLinkedIds) {
        val stopWords = setOf("and", "the", "for", "with", "task", "work", "site", "stage", "phase", "part", "check", "verify", "inspect", "placement", "marking", "level", "test")
        val taskTokens = currentTask.title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in stopWords }

        rawCandidates.map { candidate ->
            val isLinked = candidate.id in currentTaskLinkedIds
            var score = 0
            val reasons = mutableListOf<String>()

            val logTitleLower = candidate.title.lowercase()
            val laborLower = candidate.laborDescription.lowercase()
            val matLower = candidate.materialsSummary.lowercase()

            // 1. Exact or substring match
            if (logTitleLower == currentTask.title.lowercase().trim()) {
                score += 100
                reasons.add("Exact Title Match")
            } else if (logTitleLower.contains(currentTask.title.lowercase().trim())) {
                score += 75
                reasons.add("Title Match")
            }

            // 2. Keyword tokens in title (+35 each)
            val matchedTitleKeywords = taskTokens.filter { logTitleLower.contains(it) }
            if (matchedTitleKeywords.isNotEmpty()) {
                score += matchedTitleKeywords.size * 35
                reasons.add("Matches: ${matchedTitleKeywords.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}")
            }

            // 3. Keyword tokens in labor work description (+25 each)
            val matchedLaborKeywords = taskTokens.filter { it !in matchedTitleKeywords && laborLower.contains(it) }
            if (matchedLaborKeywords.isNotEmpty()) {
                score += matchedLaborKeywords.size * 25
                reasons.add("Labor match: ${matchedLaborKeywords.first().replaceFirstChar { c -> c.uppercase() }}")
            }

            // 4. Construction Material Correlation (+30)
            val hasRebarKeyword = taskTokens.any { it in listOf("rebar", "steel", "mesh", "starter", "bar") }
            val hasRebarMat = matLower.contains("steel") || matLower.contains("tmt") || matLower.contains("rod") || matLower.contains("wire")
            if (hasRebarKeyword && hasRebarMat) {
                score += 30
                reasons.add("Steel/TMT Verified")
            }

            val hasConcreteKeyword = taskTokens.any { it in listOf("concrete", "concreting", "pcc", "rcc", "pour", "footing", "slab", "beam", "column") }
            val hasConcreteMat = matLower.contains("cement") || matLower.contains("rmc") || matLower.contains("concrete") || matLower.contains("aggregate")
            if (hasConcreteKeyword && hasConcreteMat) {
                score += 30
                reasons.add("Cement/Concrete Verified")
            }

            val hasMasonryKeyword = taskTokens.any { it in listOf("brick", "masonry", "block", "wall", "plaster") }
            val hasMasonryMat = matLower.contains("brick") || matLower.contains("block") || matLower.contains("sand") || matLower.contains("mortar")
            if (hasMasonryKeyword && hasMasonryMat) {
                score += 30
                reasons.add("Masonry Verified")
            }

            val hasExcavationKeyword = taskTokens.any { it in listOf("excavat", "soil", "pit", "strata", "trench") }
            val hasExcavationMat = matLower.contains("lime") || matLower.contains("peg") || matLower.contains("anti-termite")
            if (hasExcavationKeyword && (hasExcavationMat || laborLower.contains("survey") || laborLower.contains("excavat"))) {
                score += 30
                reasons.add("Earthwork Verified")
            }

            // 5. Site Photo Evidence Bonus (+20)
            if (candidate.photoCount > 0) {
                score += 20
                reasons.add("${candidate.photoCount} site photo${if (candidate.photoCount > 1) "s" else ""}")
            }

            // 6. Active Workforce Bonus (+15)
            if (candidate.workerCount > 0) {
                score += 15
            }

            val confidenceBadge = when {
                score >= 80 -> "98% Match • Top Pick"
                score >= 50 -> "88% Match • Strong Fit"
                score >= 25 -> "Relevant Match"
                else -> null
            }

            candidate.copy(
                score = score,
                matchReasons = reasons,
                confidenceBadge = confidenceBadge,
                isLinked = isLinked
            )
        }
    }

    // Top smart suggestions: unlinked candidates with score >= 25
    val smartSuggestions = remember(scoredCandidates) {
        scoredCandidates.filter { !it.isLinked && it.score >= 25 }.sortedByDescending { it.score }
    }

    // Currently linked candidates
    val linkedWorklogs = remember(scoredCandidates) {
        scoredCandidates.filter { it.isLinked }
    }

    var isLinkDialogOpen by remember { mutableStateOf(false) }
    var linkSearchTerm by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("all") } // "all", "smart", "photos"
    var showEvidenceWarning by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        borderColor = BrandPrimary.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1. Merged Top Row: NEXT TASK badge + Stage Name + Manual Stepper Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val isCompleted = currentTask.isCompleted
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCompleted) EmeraldSuccessContainer else BrandPrimaryContainer)
                            .border(1.dp, if (isCompleted) EmeraldSuccess.copy(alpha = 0.3f) else BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isCompleted) EmeraldSuccess else BrandPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isCompleted) "COMPLETED" else "NEXT TASK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) EmeraldSuccess else BrandPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Text(
                        text = "CIVIL STAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                if (displayTasks.size > 1) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateSurfaceSubtle)
                            .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { taskIndex = (taskIndex - 1).coerceAtLeast(0) },
                            enabled = taskIndex > 0,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = if (taskIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = "${taskIndex + 1}/${displayTasks.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(
                            onClick = { taskIndex = (taskIndex + 1).coerceAtMost(displayTasks.size - 1) },
                            enabled = taskIndex < displayTasks.size - 1,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = if (taskIndex < displayTasks.size - 1) TextPrimary else TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Task Title and Quality Progress Badge on One Clean Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTask.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (totalChecksCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandPrimaryContainer)
                            .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$completedChecksCount/$totalChecksCount ($checkProgressPct%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }

            // 3. 2-Column Compact Grid of Checks
            if (currentChecks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(7.dp))
                val chunkedChecks = remember(currentChecks) { currentChecks.chunked(2) }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    chunkedChecks.forEach { rowChecks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowChecks.forEach { check ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (check.isCompleted) EmeraldSuccess.copy(alpha = 0.12f) else SlateSurfaceSubtle)
                                        .border(
                                            1.dp,
                                            if (check.isCompleted) EmeraldSuccess.copy(alpha = 0.35f) else GlassBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            val updated = currentChecks.map {
                                                if (it.id == check.id) it.copy(isCompleted = !it.isCompleted) else it
                                            }
                                            checklistsMap[currentTaskId] = updated
                                        }
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = check.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            val updated = currentChecks.map {
                                                if (it.id == check.id) it.copy(isCompleted = isChecked) else it
                                            }
                                            checklistsMap[currentTaskId] = updated
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = EmeraldSuccess,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = check.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (check.isCompleted) TextSecondary else TextPrimary,
                                        textDecoration = if (check.isCompleted) TextDecoration.LineThrough else null,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowChecks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Combined Footer Row: Proof Chip + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Proof of Work Chip
                if (linkedWorklogs.isNotEmpty()) {
                    val firstLog = linkedWorklogs.first()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldSuccessContainer.copy(alpha = 0.5f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(start = 7.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { isLinkDialogOpen = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (linkedWorklogs.size == 1) firstLog.title else "${firstLog.title} (+${linkedWorklogs.size - 1})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { unlinkWorklog(firstLog.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Unlink", tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                        }
                    }
                } else if (smartSuggestions.isNotEmpty()) {
                    // Smart match suggestion available pill: 1-Tap Link + browse button
                    val topMatch = smartSuggestions.first()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimaryContainer.copy(alpha = 0.55f))
                            .border(1.dp, BrandPrimary.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(start = 7.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { linkWorklog(topMatch.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✨ Link: ${topMatch.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { isLinkDialogOpen = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Browse all logs", tint = BrandPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { isLinkDialogOpen = true }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Link Proof",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            maxLines = 1
                        )
                    }
                }

                // Log Work Action
                OutlinedButton(
                    onClick = { onNavigate(Worklog(projectId, projectName)) },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Complete / Reopen Action
                if (currentTask.isCompleted) {
                    OutlinedButton(
                        onClick = { currentTask.id?.let { onTaskToggle(it, false) } },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Reopen", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (linkedWorklogs.isEmpty()) {
                                showEvidenceWarning = true
                            } else {
                                val allDone = currentChecks.map { it.copy(isCompleted = true) }
                                checklistsMap[currentTaskId] = allDone
                                currentTask.id?.let { onTaskToggle(it, true) }
                                if (taskIndex < displayTasks.size - 1) {
                                    taskIndex++
                                }
                            }
                        },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (linkedWorklogs.isNotEmpty()) EmeraldSuccess else SlateSurfaceSubtle,
                            contentColor = if (linkedWorklogs.isNotEmpty()) Color.White else TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Smart Link Proof of Work
    if (isLinkDialogOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isLinkDialogOpen = false },
            sheetState = sheetState,
            containerColor = SlateSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.35f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandPrimaryContainer)
                                .border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Link Proof of Work",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Attach site worklogs & photos as audit proof",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { isLinkDialogOpen = false },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SlateSurfaceSubtle)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Target Task Context Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurfaceSubtle)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET TASK:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentTask.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── HERO SECTION: TOP SMART MATCH ───────────────────────────
                if (smartSuggestions.isNotEmpty()) {
                    val topMatch = smartSuggestions.first()
                    val isTopMatchLinked = topMatch.id in currentTaskLinkedIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandPrimaryContainer.copy(alpha = 0.45f)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, BrandPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "AI BEST MATCH",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandPrimary,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSuccessContainer)
                                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = topMatch.confidenceBadge ?: "98% Match",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = topMatch.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Meta row: Date • Workers • Photos
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                    Text(text = topMatch.date, fontSize = 11.sp, color = TextSecondary)
                                }
                                Text("•", fontSize = 10.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                    Text(text = "${topMatch.workerCount} Workers", fontSize = 11.sp, color = TextSecondary)
                                }
                                if (topMatch.photoCount > 0) {
                                    Text("•", fontSize = 10.sp, color = TextSecondary)
                                    Text(text = "📸 ${topMatch.photoCount} Photos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
                                }
                            }

                            if (topMatch.matchReasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    topMatch.matchReasons.take(3).forEach { reason ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SlateSurfaceSubtle)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = reason,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            if (isTopMatchLinked) {
                                OutlinedButton(
                                    onClick = { unlinkWorklog(topMatch.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Linked as Proof ✓ (Tap to Unlink)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { linkWorklog(topMatch.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚡ Link This Worklog as Proof", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ─── SEARCH & FILTER BAR ──────────────────────────────────────
                OutlinedTextField(
                    value = linkSearchTerm,
                    onValueChange = { linkSearchTerm = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search logs by title, date, labor...", fontSize = 12.sp, color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (linkSearchTerm.isNotEmpty()) {
                            IconButton(onClick = { linkSearchTerm = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurfaceSubtle,
                        unfocusedContainerColor = SlateSurfaceSubtle,
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = GlassBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterTab == "all",
                        onClick = { selectedFilterTab = "all" },
                        label = { Text("All (${scoredCandidates.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedFilterTab == "smart",
                        onClick = { selectedFilterTab = "smart" },
                        label = { Text("✨ Suggested (${smartSuggestions.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedFilterTab == "photos",
                        onClick = { selectedFilterTab = "photos" },
                        label = { Text("📸 Photos (${scoredCandidates.count { it.photoCount > 0 }})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ─── CANDIDATE LIST (Ranked by Relevance) ────────────────────
                val filteredLogs = scoredCandidates
                    .filter { candidate ->
                        val matchesSearch = linkSearchTerm.isBlank() ||
                                candidate.title.contains(linkSearchTerm, ignoreCase = true) ||
                                candidate.date.contains(linkSearchTerm, ignoreCase = true) ||
                                candidate.laborDescription.contains(linkSearchTerm, ignoreCase = true)

                        val matchesFilter = when (selectedFilterTab) {
                            "smart" -> candidate.score >= 25
                            "photos" -> candidate.photoCount > 0
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }
                    .sortedWith(
                        compareByDescending<SmartWorklogCandidate> { it.id in currentTaskLinkedIds }
                            .thenByDescending { it.score }
                            .thenByDescending { it.date }
                    )

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching worklogs found.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { candidate ->
                            val isLinked = candidate.id in currentTaskLinkedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isLinked -> EmeraldSuccess.copy(alpha = 0.10f)
                                            candidate.score >= 50 -> BrandPrimaryContainer.copy(alpha = 0.25f)
                                            else -> SlateSurfaceSubtle
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isLinked -> EmeraldSuccess.copy(alpha = 0.45f)
                                            candidate.score >= 50 -> BrandPrimary.copy(alpha = 0.35f)
                                            else -> GlassBorder
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(
                                            text = candidate.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (candidate.score >= 40 && !isLinked) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(BrandPrimaryContainer)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("✨ Match", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${candidate.date} • ${candidate.workerCount} workers${if (candidate.photoCount > 0) " • 📸 ${candidate.photoCount} photos" else ""}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isLinked) {
                                    OutlinedButton(
                                        onClick = { unlinkWorklog(candidate.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                                    ) {
                                        Text("Linked ✓", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.Close, contentDescription = "Unlink", modifier = Modifier.size(11.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = { linkWorklog(candidate.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (candidate.score >= 50) BrandPrimary else SlateSurfaceSubtle,
                                            contentColor = if (candidate.score >= 50) Color.White else TextPrimary
                                        )
                                    ) {
                                        Text("+ Link", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action: Done Button
                Button(
                    onClick = { isLinkDialogOpen = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Dialog: Evidence Warning
    if (showEvidenceWarning) {
        AlertDialog(
            onDismissRequest = { showEvidenceWarning = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp)) },
            title = { Text("Site Evidence Required", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = "Please link or log at least one worklog for this task before completing it, as required by site quality audit standards.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEvidenceWarning = false
                        isLinkDialogOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Link Worklog", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvidenceWarning = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SlateSurface
        )
    }
}

// ─── D. Building Plan Summary Card ──────────────────────────────────────────

@Composable
fun BuildingPlanSummaryCard(
    projectId: String,
    projectName: String,
    onNavigate: (NavKey) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = BrandPrimary.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimaryContainer)
                        .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Building Plan & Quality Checks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandPrimaryContainer)
                                .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Stage 2 of 6",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active: Foundation & Columns",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "14/28 Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.50f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BrandPrimary,
                        trackColor = SlateSurfaceSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onNavigate(BuildingPlan(projectId, projectName)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("View Building Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// ─── E. Recent Updates Carousel ─────────────────────────────────────────────

@Composable
fun RecentUpdatesCarousel(
    projectId: String,
    worklogs: List<DailyWorklogDto>,
    onWorklogClick: (DailyWorklogDto) -> Unit
) {
    val projectLogs = worklogs.filter { it.projectId == projectId }.ifEmpty {
        listOf(
            DailyWorklogDto(
                id = "demo-1",
                projectId = projectId,
                date = "2026-11-01"
            ),
            DailyWorklogDto(
                id = "demo-2",
                projectId = projectId,
                date = "2026-10-31"
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Recent Updates", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(projectLogs) { log ->
                val fallbackImg = "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=600&auto=format&fit=crop"
                GlassCard(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { onWorklogClick(log) }
                ) {
                    Column {
                        AsyncImage(
                            model = fallbackImg,
                            contentDescription = log.date,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(log.date, fontSize = 10.sp, color = TextSecondary)
                            Text(
                                "Daily Site Progress",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Labor execution and structural updates recorded.",
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

// ─── F. Project Tasks Card ──────────────────────────────────────────────────

@Composable
fun ProjectTasksCard(
    tasks: List<TaskItemDto>,
    onToggleTask: (String, Boolean) -> Unit,
    onAddTaskClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Project Tasks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Button(
                    onClick = onAddTaskClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add Task", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No site tasks yet. Tap '+ Add Task' to add works for this project.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceSubtle)
                                .clickable { task.id?.let { onToggleTask(it, !task.isCompleted) } }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { isChecked ->
                                    task.id?.let { onToggleTask(it, isChecked) }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldSuccess,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (task.isCompleted) TextSecondary else TextPrimary,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!task.priority.isNullOrBlank()) {
                                    val prioColor = when (task.priority.lowercase()) {
                                        "high" -> RoseDanger
                                        "medium" -> AmberWarning
                                        else -> TextSecondary
                                    }
                                    Text(
                                        text = task.priority.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = prioColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── G. Budget Overview Bar ─────────────────────────────────────────────────

@Composable
fun BudgetOverviewCard(
    budget: Double,
    spent: Double,
    remaining: Double,
    spentPercentage: Double
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Budget Overview", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(CurrencyFormatter.formatFull(budget), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Amount Spent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(CurrencyFormatter.formatFull(spent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (spentPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BrandPrimary,
                trackColor = SlateSurfaceSubtle
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Remaining", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(CurrencyFormatter.formatFull(remaining), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun OverallProgressCardPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        OverallProgressCard(
            project = ProjectDto(
                id = "demo-1",
                name = "Skyline Heights Luxury Villa",
                progress = 68,
                budget = 2500000.0,
                endDate = "2026-12-15",
                status = "in_progress",
                companyId = "comp-1"
            ),
            budget = 2500000.0,
            memberCount = 14,
            spentPercentage = 62.0,
            completedTasks = 9,
            totalTasks = 13
        )
    }
}
