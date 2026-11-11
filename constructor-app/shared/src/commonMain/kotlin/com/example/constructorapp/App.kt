package com.example.constructorapp

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AlertFlipper
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.components.sampleAlerts

@Composable
fun App() {
    ConstructorSharedTheme {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var isDrawerOpen by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Safe-area compliant)
                NativeHeader()

                // Active View Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> DashboardScreen(
                            onNavigateToProjects = { selectedTab = 1 },
                            onNavigateToOrders = { selectedTab = 2 },
                            onNavigateToMessages = { selectedTab = 3 }
                        )
                        1 -> ProjectsScreen()
                        2 -> PurchaseOrdersScreen()
                        3 -> RemindersScreen()
                        else -> DashboardScreen(
                            onNavigateToProjects = { selectedTab = 1 },
                            onNavigateToOrders = { selectedTab = 2 },
                            onNavigateToMessages = { selectedTab = 3 }
                        )
                    }
                }
            }

            // Floating Bottom Navigation Bar (Exact 1:1 match with Android App)
            FloatingBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onMoreClick = { isDrawerOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            // More / Drawer Bottom Sheet
            if (isDrawerOpen) {
                MoreMenuDialog(
                    onDismiss = { isDrawerOpen = false },
                    onSelect = { tabIndex ->
                        selectedTab = tabIndex
                        isDrawerOpen = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NativeHeader() {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏗️", fontSize = 20.sp)
                }
                Column {
                    Text(
                        text = "CONSTRUCTOR",
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Site Construction Manager",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmeraldSuccessContainer)
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldSuccess))
                    Text(
                        "Cloud Synced",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: DASHBOARD (Authentic 1:1 Android Architecture)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToMessages: () -> Unit
) {
    var alertsList by remember { mutableStateOf(sampleAlerts) }
    var tasks by remember {
        mutableStateOf(
            listOf(
                SharedTask("task-1", "Verify steel delivery manifest for Foundation", false, "High"),
                SharedTask("task-2", "Call electrical contractor for slab conduit approval", true, "Medium"),
                SharedTask("task-3", "Review cement cube compression test lab report", false, "Low")
            )
        )
    }
    var newTaskTitle by remember { mutableStateOf("") }
    var isExpenseDialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Alert Flipper / Sentinel Mascot (Split-Flap ticker)
        item {
            AlertFlipper(
                alerts = alertsList,
                onAlertDismiss = { alertId ->
                    alertsList = alertsList.filter { it.id != alertId }
                }
            )
        }

        // 2. Quick Actions Row (Exact Android Dashboard: 38.dp height pills)
        item {
            QuickActionsRow(
                onWorklogClick = onNavigateToProjects,
                onExpenseClick = { isExpenseDialogOpen = true },
                onMaterialsClick = onNavigateToOrders
            )
        }

        // 3. Tomorrow's Work Prep Highlight Banner (Exact Android Dashboard Surface)
        item {
            TomorrowPrepBanner(
                scheduledTasksCount = 0,
                dateLabel = "Tomorrow",
                onPrepClick = onNavigateToProjects
            )
        }

        // 4. Active Projects Header
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
                    modifier = Modifier.clickable { onNavigateToProjects() }
                )
            }
        }

        // Projects List
        items(sharedProjects.take(2)) { project ->
            ProjectCard(project)
        }

        // 5. My Tasks Section (Interactive Checklist)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Site Tasks",
                            fontFamily = OutfitFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${tasks.count { it.completed }}/${tasks.size} Done",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandPrimary
                        )
                    }

                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    tasks = tasks.map {
                                        if (it.id == task.id) it.copy(completed = !it.completed) else it
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (task.completed) EmeraldSuccess else SlateBackground)
                                    .border(1.5.dp, if (task.completed) EmeraldSuccess else SlateBorder, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.completed) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(14.dp))
                                }
                            }

                            Text(
                                text = task.title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                color = if (task.completed) TextSecondary else TextPrimary,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            PriorityBadge(task.priority)
                        }
                    }

                    // Add Task Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextPrimary),
                            decorationBox = { innerTextField ->
                                if (newTaskTitle.isEmpty()) {
                                    Text("+ Add a new task...", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextSecondary)
                                }
                                innerTextField()
                            }
                        )
                        if (newTaskTitle.isNotBlank()) {
                            Text(
                                text = "Save",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.clickable {
                                    tasks = tasks + SharedTask("task-${tasks.size + 1}", newTaskTitle, false, "Medium")
                                    newTaskTitle = ""
                                }
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Site Activity
        item {
            Text(
                text = "Recent Site Activity",
                fontFamily = OutfitFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(sharedRecentActivities) { activity ->
            RecentActivityCard(activity)
        }
    }

    if (isExpenseDialogOpen) {
        ExpenseQuickDialog(onDismiss = { isExpenseDialogOpen = false })
    }
}

// ─── 1. Quick Actions Row (Android 1:1 Implementation) ─────────────────────────
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

// ─── 2. Tomorrow's Work Prep Banner (Android 1:1 Implementation) ───────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2: PROJECTS (Android 1:1 Implementation)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectsScreen() {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    val statusOptions = listOf("All", "Active", "Completed", "On-Hold")

    val filtered = remember(searchQuery, selectedStatus) {
        sharedProjects.filter { p ->
            val matchQ = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.location.contains(searchQuery, ignoreCase = true)
            val matchS = when (selectedStatus) {
                "Active" -> p.status.equals("In Progress", ignoreCase = true) || p.status.equals("Active Foundation", ignoreCase = true)
                "Completed" -> p.status.equals("Completed", ignoreCase = true)
                "On-Hold" -> p.status.equals("On-Hold", ignoreCase = true)
                else -> true
            }
            matchQ && matchS
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projects (${filtered.size})",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("+ New Project", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextPrimary),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search project name, client, location...", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp, color = TextSecondary)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                    )
                }
            }
        }

        // Status Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusOptions) { status ->
                    val isSel = selectedStatus == status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) BrandPrimary else SurfaceWhite)
                            .border(1.dp, if (isSel) BrandPrimary else SlateBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = status,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) SurfaceWhite else TextSecondary
                        )
                    }
                }
            }
        }

        items(filtered) { project ->
            ProjectCard(project)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 3: PURCHASE ORDERS (Android 1:1 Implementation)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PurchaseOrdersScreen() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Purchase Orders",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("+ New Order", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }

        items(sharedMaterials) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontFamily = OutfitFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Unit: ${item.unit} • ₹${item.unitCost}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${item.stock} ${item.unit}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess
                        )
                        Text(
                            if (item.stock < item.minStock) "⚠️ Low Stock" else "In Stock",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 4: REMINDERS & TEAM HUB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RemindersScreen() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Team Hub & Reminders",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("+ Add Reminder", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }

        items(sharedWorklogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.title, fontFamily = OutfitFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(log.date, fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(log.project, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    Text(log.notes, fontFamily = PlusJakartaSansFontFamily, fontSize = 12.5.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider(color = SlateDivider)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("👷 ${log.laborCount} Workers", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                        Text("🧱 ${log.materialsCount} Materials", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                        Text("📸 ${log.photosCount} Photos", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORE DRAWER / MENU DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoreMenuDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Menu Navigation", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Dashboard", "Projects", "Purchase Orders", "Team Hub & Reminders").forEachIndexed { idx, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(idx) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontFamily = PlusJakartaSansFontFamily, color = BrandPrimary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK EXPENSE DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExpenseQuickDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Expense logging synced to Supabase database.",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)) {
                Text("Done")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PriorityBadge(priority: String) {
    val (bgColor, textColor) = when (priority.lowercase()) {
        "high" -> RoseDangerContainer to RoseDanger
        "medium" -> AmberWarningContainer to AmberWarning
        else -> SkyInfoContainer to SkyInfo
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(priority, fontFamily = PlusJakartaSansFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun ProjectCard(project: SharedProject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SkyInfoContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = project.status,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyInfo
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(13.dp))
                Text(
                    text = project.location,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, color = TextSecondary)
                    Text("${(project.progress * 100).toInt()}%", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                LinearProgressIndicator(
                    progress = { project.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BrandPrimary,
                    trackColor = SlateBorder
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Budget: ₹${project.budget}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, color = TextSecondary)
                Text("Spent: ₹${project.spent}", fontFamily = PlusJakartaSansFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(activity: SharedRecentActivity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(activity.icon, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${activity.project}: ${activity.description}",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(activity.timeAgo, fontFamily = PlusJakartaSansFontFamily, fontSize = 10.5.sp, color = TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS & SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────
private data class SharedTask(val id: String, val title: String, val completed: Boolean, val priority: String)
private data class SharedProject(val name: String, val location: String, val status: String, val progress: Float, val budget: String, val spent: String)
private data class SharedWorklog(val title: String, val project: String, val notes: String, val date: String, val laborCount: Int, val materialsCount: Int, val photosCount: Int)
private data class SharedMaterial(val name: String, val unit: String, val stock: Int, val minStock: Int, val unitCost: Int)
private data class SharedRecentActivity(val project: String, val description: String, val timeAgo: String, val icon: String)

private val sharedProjects = listOf(
    SharedProject("Greenfield Villa Project", "Thillai Nagar, Trichy", "In Progress", 0.65f, "45,00,000", "28,50,000"),
    SharedProject("Royal Residency Commercial", "Kallukuzhi, Trichy", "Active Foundation", 0.35f, "85,00,000", "32,10,000"),
    SharedProject("Modern Living Duplex", "Karur Bypass, Trichy", "Finishing", 0.85f, "32,00,000", "29,40,000")
)

private val sharedWorklogs = listOf(
    SharedWorklog("First Floor Slab Casting", "Greenfield Villa Project", "M20 grade concrete poured for roof slab with 12 masons and helpers.", "Today, 4:30 PM", 12, 4, 3),
    SharedWorklog("Brickwork & Plastering Phase 2", "Royal Residency Commercial", "Completed outer wall perimeter bricklaying with cement mortar 1:5 ratio.", "Yesterday", 8, 2, 2)
)

private val sharedMaterials = listOf(
    SharedMaterial("OPC 53 Grade Cement", "Bags", 140, 50, 395),
    SharedMaterial("River Sand (Fine)", "CFT", 450, 100, 75),
    SharedMaterial("TMT Steel 16mm (Fe 550D)", "Kg", 850, 200, 68),
    SharedMaterial("Coarse Aggregate (20mm)", "CFT", 35, 80, 52)
)

private val sharedRecentActivities = listOf(
    SharedRecentActivity("Greenfield Villa", "12 workers on site for first floor slab casting", "2h ago", "👷"),
    SharedRecentActivity("Royal Residency", "Consumed 40 bags of OPC 53 Grade Cement", "4h ago", "🧱"),
    SharedRecentActivity("Greenfield Villa", "3 site photo(s) captured & verified", "5h ago", "📸")
)
