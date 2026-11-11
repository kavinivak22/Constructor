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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.components.sampleAlerts

@Composable
fun App() {
    ConstructorSharedTheme {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            containerColor = SlateBackground,
            topBar = {
                NativeHeader()
            },
            bottomBar = {
                NativeBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        onNavigateToProjects = { selectedTab = 1 },
                        onNavigateToWorklogs = { selectedTab = 2 },
                        onNavigateToMaterials = { selectedTab = 3 }
                    )
                    1 -> ProjectsScreen()
                    2 -> WorklogsScreen()
                    3 -> MaterialsScreen()
                    4 -> SettingsScreen()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NATIVE HEADER (Webapp & Android App Exact Match)
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Site Construction Manager",
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
                    Text("Cloud Synced", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAVIGATION BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NativeBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        Triple(0, "Dashboard", "📊"),
        Triple(1, "Projects", "📁"),
        Triple(2, "Worklogs", "📋"),
        Triple(3, "Materials", "🧱"),
        Triple(4, "Settings", "⚙️")
    )

    Surface(
        color = SurfaceWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, title, emoji) ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BrandPrimaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = if (isSelected) 18.sp else 16.sp)
                    }
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: DASHBOARD (Authentic Android UI Match)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToWorklogs: () -> Unit,
    onNavigateToMaterials: () -> Unit
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Alert Flipper / Sentinel Mascot (Webapp & Android Exact Split-Flap)
        item {
            AlertFlipper(
                alerts = alertsList,
                onAlertDismiss = { alertId ->
                    alertsList = alertsList.filter { it.id != alertId }
                }
            )
        }

        // 2. Quick Actions Row (Worklog, Expense, Materials)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "+ Worklog",
                    subtitle = "Daily Log",
                    icon = Icons.Outlined.EditCalendar,
                    color = BrandPrimary,
                    onClick = onNavigateToWorklogs
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "+ Expense",
                    subtitle = "Record Cost",
                    icon = Icons.Outlined.ReceiptLong,
                    color = EmeraldSuccess,
                    onClick = onNavigateToWorklogs
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "+ Materials",
                    subtitle = "Stock In/Out",
                    icon = Icons.Outlined.Inventory2,
                    color = SkyInfo,
                    onClick = onNavigateToMaterials
                )
            }
        }

        // 3. Tomorrow's Work Prep Highlight Banner
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Tomorrow's Work Prep", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("3 tasks scheduled • Next day labor & materials ready", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 4. Active Projects Header & Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active Projects", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { onNavigateToProjects() }
                )
            }
        }

        items(sharedProjects.take(2)) { project ->
            ProjectCard(project)
        }

        // 5. My Tasks Section (Interactive Checklist)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Tasks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${tasks.count { it.completed }}/${tasks.size} Done", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { checked ->
                                    tasks = tasks.map {
                                        if (it.id == task.id) it.copy(completed = checked) else it
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    color = if (task.completed) TextSecondary else TextPrimary,
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                            PriorityBadge(task.priority)
                        }
                    }

                    // Inline Add Task Input
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
                            textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                            decorationBox = { innerTextField ->
                                if (newTaskTitle.isEmpty()) {
                                    Text("+ Add a new task...", fontSize = 13.sp, color = TextSecondary)
                                }
                                innerTextField()
                            }
                        )
                        if (newTaskTitle.isNotBlank()) {
                            Text(
                                text = "Save",
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
            Text("Recent Site Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(sharedRecentActivities) { activity ->
            RecentActivityCard(activity)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2: PROJECTS (Authentic Android UI Match)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Projects (${filtered.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("+ New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search project name, client, location...", fontSize = 13.sp, color = TextSecondary)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
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
// SCREEN 3: DAILY WORKLOGS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WorklogsScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Worklogs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("+ Add Worklog", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
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
                        Text(log.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(log.date, fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(log.project, fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    Text(log.notes, fontSize = 12.5.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Divider(color = SlateDivider)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("👷 ${log.laborCount} Workers", fontSize = 11.sp, color = TextSecondary)
                        Text("🧱 ${log.materialsCount} Materials", fontSize = 11.sp, color = TextSecondary)
                        Text("📸 ${log.photosCount} Photos", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 4: INVENTORY & MATERIALS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MaterialsScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Inventory & Materials", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("+ Stock In", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
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
                        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Unit: ${item.unit} • ₹${item.unitCost}", fontSize = 11.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${item.stock} ${item.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess
                        )
                        Text(
                            if (item.stock < item.minStock) "⚠️ Low Stock" else "In Stock",
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
// SCREEN 5: SETTINGS & TEAM HUB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Constructor Platform Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Compose Multiplatform shared architecture active for Android & iOS.", fontSize = 12.sp, color = TextSecondary)
                    Divider(color = SlateDivider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Framework", fontSize = 12.sp, color = TextSecondary)
                        Text("Jetpack Compose Multiplatform", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cloud Database", fontSize = 12.sp, color = TextSecondary)
                        Text("Supabase + Room Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Multi-Arch Target", fontSize = 12.sp, color = TextSecondary)
                        Text("Universal x86_64 & ARM64", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyInfo)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE SUB-COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

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
        Text(priority, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("📍 ${project.location}", fontSize = 11.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (project.progress > 70) EmeraldSuccessContainer else SkyInfoContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        project.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.progress > 70) EmeraldSuccess else SkyInfo
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", fontSize = 11.sp, color = TextSecondary)
                    Text("${project.progress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                LinearProgressIndicator(
                    progress = { project.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = BrandPrimary,
                    trackColor = SlateSurfaceSubtle
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budget: ₹${project.budget}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text("Spent: ₹${project.spent}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(item: SharedRecentActivity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(item.description, fontSize = 11.5.sp, color = TextSecondary)
            }
            Text(item.time, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
data class SharedProject(
    val name: String,
    val location: String,
    val status: String,
    val progress: Int,
    val budget: String,
    val spent: String
)

data class SharedTask(
    val id: String,
    val title: String,
    val completed: Boolean,
    val priority: String
)

data class SharedWorklog(
    val title: String,
    val project: String,
    val date: String,
    val notes: String,
    val laborCount: Int,
    val materialsCount: Int,
    val photosCount: Int
)

data class SharedMaterial(
    val name: String,
    val unit: String,
    val stock: Int,
    val minStock: Int,
    val unitCost: String
)

data class SharedRecentActivity(
    val emoji: String,
    val title: String,
    val description: String,
    val time: String
)

val sharedProjects = listOf(
    SharedProject("Greenfield Villa Project", "Thillai Nagar, Trichy", "In Progress", 65, "45,00,000", "28,50,000"),
    SharedProject("Royal Residency Commercial", "Kallukuzhi, Trichy", "Active Foundation", 35, "85,00,000", "32,10,000"),
    SharedProject("Modern Living Duplex", "Karur Bypass, Trichy", "Finishing", 85, "32,00,000", "29,40,000")
)

val sharedWorklogs = listOf(
    SharedWorklog("First Floor Slab Casting", "Greenfield Villa Project", "Today, 4:30 PM", "M20 grade concrete poured for roof slab with 12 masons and helpers.", 12, 4, 3),
    SharedWorklog("Brickwork & Plastering Phase 2", "Royal Residency Commercial", "Yesterday", "Completed outer wall perimeter bricklaying with cement mortar 1:5 ratio.", 8, 2, 2)
)

val sharedMaterials = listOf(
    SharedMaterial("OPC 53 Grade Cement", "Bags", 140, 50, "395"),
    SharedMaterial("River Sand (Fine)", "CFT", 450, 200, "75"),
    SharedMaterial("TMT Steel 16mm (Fe 550D)", "Kg", 850, 300, "68"),
    SharedMaterial("Coarse Aggregate (20mm)", "CFT", 35, 100, "52")
)

val sharedRecentActivities = listOf(
    SharedRecentActivity("👷", "Masonry Team on Site", "8 masons & helpers completed outer wall perimeter", "2h ago"),
    SharedRecentActivity("🧱", "Cement Consumption", "Consumed 45 bags of OPC 53 Grade Cement", "4h ago"),
    SharedRecentActivity("📸", "Site Photos Verified", "3 progress photos attached to roof slab worklog", "Yesterday")
)
