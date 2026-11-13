package com.example.constructorapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossAlertDialog
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.screens.worklog.ModernWorklogPhotoViewer
import com.example.constructorapp.ui.screens.worklog.WorklogDetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorklogScreen(
    projectId: String = "",
    projectName: String = "All Projects",
    onBackClick: () -> Unit = {},
    initialWorklogs: List<WorklogWithDetails> = emptyList(),
    allProjects: List<ProjectDto> = emptyList(),
    onCreateWorklogClick: (() -> Unit)? = null,
    onDeleteWorklog: ((String) -> Unit)? = null
) {
    var worklogsState by remember(initialWorklogs) { mutableStateOf(initialWorklogs) }
    var selectedProjectId by remember(projectId) { mutableStateOf(if (projectId.isBlank()) "all" else projectId) }
    var selectedDateFilter by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var customDateInput by remember { mutableStateOf("") }
    var selectedWorklogForModal by remember { mutableStateOf<WorklogWithDetails?>(null) }

    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var contractorDropdownExpanded by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    var selectedContractor by remember { mutableStateOf("all") }
    var viewMode by remember { mutableStateOf("cards") } // "cards" or "table"
    var deletingWorklogId by remember { mutableStateOf<String?>(null) }
    var lightboxWorklog by remember { mutableStateOf<WorklogWithDetails?>(null) }
    var lightboxInitialIndex by remember { mutableStateOf(0) }

    val currentProjects = if (allProjects.isNotEmpty()) allProjects else listOf(
        ProjectDto(id = projectId.ifBlank { "p1" }, name = projectName.ifBlank { "Greenfield Villa" }, companyId = "")
    )

    // Dynamic list of unique contractors in current worklogs
    val availableContractors = remember(worklogsState) {
        val names = mutableSetOf<String>()
        worklogsState.forEach { log ->
            log.laborEntries.forEach { entry ->
                val name = entry.laborEntry.contractorName.trim()
                if (name.isNotEmpty()) names.add(name)
            }
        }
        names.sorted()
    }

    // Filtered worklogs by Search Query, Contractor, Project, and Date
    val filteredWorklogs = remember(worklogsState, searchTerm, selectedContractor, selectedProjectId, selectedDateFilter) {
        var list = worklogsState
        if (selectedProjectId != "all") {
            list = list.filter { it.worklog.projectId == selectedProjectId }
        }
        if (selectedDateFilter.isNotBlank()) {
            list = list.filter { it.worklog.date.startsWith(selectedDateFilter) }
        }
        if (selectedContractor != "all") {
            list = list.filter { log ->
                log.laborEntries.any { it.laborEntry.contractorName.equals(selectedContractor, ignoreCase = true) }
            }
        }
        if (searchTerm.isNotBlank()) {
            val q = searchTerm.lowercase().trim()
            list = list.filter { log ->
                val title = log.worklog.title.lowercase()
                val laborText = log.laborEntries.joinToString(" ") {
                    "${it.laborEntry.contractorName} ${it.laborEntry.workDescription ?: ""}"
                }.lowercase()
                val matText = log.materials.joinToString(" ") { it.materialName }.lowercase()
                title.contains(q) || laborText.contains(q) || matText.contains(q)
            }
        }
        list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                // Transparent Header - Ambient blob canvas flows smoothly behind
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Clean Back Button (No circular backdrop) + "Worklogs" Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Worklogs",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        }

                        // Right: Compact Project Selector Dropdown
                        Box {
                            val currentProjName = if (selectedProjectId == "all" || selectedProjectId.isBlank()) {
                                "All Projects"
                            } else {
                                currentProjects.find { it.id == selectedProjectId }?.name ?: projectName
                            }

                            Surface(
                                onClick = { projectDropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = currentProjName,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = projectDropdownExpanded,
                                onDismissRequest = { projectDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "All Projects",
                                            fontWeight = if (selectedProjectId == "all") FontWeight.Bold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedProjectId = "all"
                                        projectDropdownExpanded = false
                                    }
                                )
                                currentProjects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                proj.name,
                                                fontWeight = if (selectedProjectId == proj.id) FontWeight.Bold else FontWeight.Normal,
                                                color = TextPrimary
                                            )
                                        },
                                        onClick = {
                                            selectedProjectId = proj.id
                                            projectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onCreateWorklogClick?.invoke() },
                    containerColor = BrandPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp, end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Work",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ─── Filters & View Toggle Bar (Webapp Exact Match) ───────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Search Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCCFFFFFF),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchTerm.isEmpty()) {
                                    Text(
                                        "Search logs, scope, contractors...",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        color = TextMutedDark
                                    )
                                }
                                BasicTextField(
                                    value = searchTerm,
                                    onValueChange = { searchTerm = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchTerm.isNotEmpty()) {
                                IconButton(onClick = { searchTerm = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // 2. Filter Row: Contractor Selector + Date Filter + View Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Contractor Filter Dropdown
                            Box {
                                Surface(
                                    onClick = { contractorDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedContractor != "all") BrandPrimaryContainer else Color(0xCCFFFFFF),
                                    border = BorderStroke(1.dp, if (selectedContractor != "all") BrandPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier.widthIn(max = 115.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (selectedContractor == "all") "All Contractors" else selectedContractor,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedContractor != "all") FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedContractor != "all") BrandPrimary else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = if (selectedContractor != "all") BrandPrimary else TextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = contractorDropdownExpanded,
                                    onDismissRequest = { contractorDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "All Contractors",
                                                fontWeight = if (selectedContractor == "all") FontWeight.Bold else FontWeight.Normal,
                                                color = TextPrimary
                                            )
                                        },
                                        onClick = {
                                            selectedContractor = "all"
                                            contractorDropdownExpanded = false
                                        }
                                    )
                                    availableContractors.forEach { cName ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    cName,
                                                    fontWeight = if (selectedContractor == cName) FontWeight.Bold else FontWeight.Normal,
                                                    color = TextPrimary
                                                )
                                            },
                                            onClick = {
                                                selectedContractor = cName
                                                contractorDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Date Filter Button
                            Surface(
                                onClick = { showDatePickerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedDateFilter.isNotBlank()) BrandPrimaryContainer else Color(0xCCFFFFFF),
                                border = BorderStroke(1.dp, if (selectedDateFilter.isNotBlank()) BrandPrimary else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = if (selectedDateFilter.isNotBlank()) BrandPrimary else TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = selectedDateFilter.ifBlank { "Date" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedDateFilter.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedDateFilter.isNotBlank()) BrandPrimary else TextPrimary
                                    )
                                }
                            }

                            // Clear Filters Button
                            if (searchTerm.isNotBlank() || selectedContractor != "all" || selectedDateFilter.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        searchTerm = ""
                                        selectedContractor = "all"
                                        selectedDateFilter = ""
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xCCFFFFFF),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Clear Filters",
                                            tint = RoseDanger,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Cards vs Table Segmented Toggle (Icon-only for compact fit)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x40FFFFFF),
                            border = BorderStroke(1.dp, Color(0x80FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Cards Button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewMode == "cards") BrandPrimary else Color.Transparent)
                                        .clickable { viewMode = "cards" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.GridView,
                                        contentDescription = "Cards View",
                                        tint = if (viewMode == "cards") Color.White else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Table Button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewMode == "table") BrandPrimary else Color.Transparent)
                                        .clickable { viewMode = "table" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.TableChart,
                                        contentDescription = "Table View",
                                        tint = if (viewMode == "table") Color.White else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Main Content Area ────────────────────────────────────────
                if (worklogsState.isEmpty()) {
                    // Empty State: No logs created yet
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No Daily Logs Yet",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start documenting site progress, contractor labor, material consumption, and photos.",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onCreateWorklogClick?.invoke() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Worklog", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else if (filteredWorklogs.isEmpty()) {
                    // Empty State: Filters returned no results
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No worklogs found matching your filters",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    searchTerm = ""
                                    selectedContractor = "all"
                                    selectedDateFilter = ""
                                }
                            ) {
                                Text("Clear all filters", color = BrandPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (viewMode == "cards") {
                    // Card Feed View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredWorklogs, key = { it.worklog.id }) { logWithDetails ->
                            val projName = currentProjects.find { it.id == logWithDetails.worklog.projectId }?.name ?: "Site"
                            WorklogFeedCard(
                                logWithDetails = logWithDetails,
                                projectName = projName,
                                onViewDetails = { selectedWorklogForModal = logWithDetails },
                                onEdit = { onCreateWorklogClick?.invoke() },
                                onDelete = { deletingWorklogId = logWithDetails.worklog.id },
                                onPhotoClick = { pageIndex ->
                                    lightboxWorklog = logWithDetails
                                    lightboxInitialIndex = pageIndex
                                }
                            )
                        }
                    }
                } else {
                    // Table Ledger View
                    WorklogTableView(
                        worklogs = filteredWorklogs,
                        projects = currentProjects,
                        onViewDetails = { selectedWorklogForModal = it },
                        onDelete = { deletingWorklogId = it.worklog.id }
                    )
                }
            }
        }

        // ─── Modern Fullscreen Photo Lightbox ──────────────────────────────────
        lightboxWorklog?.let { log ->
            if (log.photos.isNotEmpty()) {
                ModernWorklogPhotoViewer(
                    photos = log.photos,
                    initialIndex = lightboxInitialIndex.coerceIn(0, log.photos.lastIndex),
                    title = log.worklog.title.ifBlank { "Site Photos" },
                    dateLabel = log.worklog.date,
                    onDismiss = { lightboxWorklog = null }
                )
            }
        }

        // ─── Delete Confirmation Dialog ──────────────────────────────────────
        deletingWorklogId?.let { logId ->
            FluidGlossAlertDialog(
                onDismissRequest = { deletingWorklogId = null },
                title = "Delete Worklog?",
                text = "This action cannot be undone. This will permanently delete this daily log and all associated labor and material entries.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = {
                    val id = logId
                    deletingWorklogId = null
                    if (onDeleteWorklog != null) {
                        onDeleteWorklog(id)
                    } else {
                        worklogsState = worklogsState.filter { it.worklog.id != id }
                    }
                },
                onDismiss = { deletingWorklogId = null },
                isDestructive = true,
                icon = Icons.Outlined.Delete
            )
        }

        // ─── Worklog Detail Dialog ───────────────────────────────────────────
        selectedWorklogForModal?.let { selected ->
            val projName = currentProjects.find { it.id == selected.worklog.projectId }?.name ?: "Project"
            WorklogDetailDialog(
                worklogWithDetails = selected,
                projectName = projName,
                creatorName = selected.worklog.createdBy.ifBlank { "Site Supervisor" },
                onDismiss = { selectedWorklogForModal = null },
                onEdit = {
                    selectedWorklogForModal = null
                    onCreateWorklogClick?.invoke()
                },
                onDelete = { worklogId ->
                    if (onDeleteWorklog != null) {
                        onDeleteWorklog(worklogId)
                    } else {
                        worklogsState = worklogsState.filter { it.worklog.id != worklogId }
                    }
                    selectedWorklogForModal = null
                }
            )
        }

        // ─── Date Picker Filter Dialog ────────────────────────────────────────
        if (showDatePickerDialog) {
            FluidGlossDialog(
                onDismissRequest = { showDatePickerDialog = false },
                title = "Filter by Date",
                subtitle = "Select or enter a date filter (YYYY-MM-DD)",
                icon = Icons.Outlined.CalendarToday
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FluidGlossTextField(
                        value = customDateInput,
                        onValueChange = { customDateInput = it },
                        label = "Date (YYYY-MM-DD)",
                        placeholder = "e.g. 2026-11-12",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FluidGlossButton(
                            text = "Clear",
                            onClick = {
                                selectedDateFilter = ""
                                customDateInput = ""
                                showDatePickerDialog = false
                            },
                            variant = FluidGlossButtonVariant.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        FluidGlossButton(
                            text = "Apply",
                            onClick = {
                                selectedDateFilter = customDateInput.trim()
                                showDatePickerDialog = false
                            },
                            variant = FluidGlossButtonVariant.Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Worklog Feed Card (Webapp Exact Match) ──────────────────────────────────

@Composable
fun WorklogFeedCard(
    logWithDetails: WorklogWithDetails,
    projectName: String,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val worklog = logWithDetails.worklog
    val photos = logWithDetails.photos
    val labor = logWithDetails.laborEntries
    val materials = logWithDetails.materials

    var menuExpanded by remember { mutableStateOf(false) }

    // Aggregate worker counts
    val totalWorkers = remember(labor) {
        var count = 0
        labor.forEach { l ->
            count += l.workerCounts.sumOf { it.count }
        }
        if (count == 0 && labor.isNotEmpty()) labor.size * 3 else count
    }

    // Smart Title
    val title = remember(worklog.title, labor) {
        if (worklog.title.isNotBlank() && worklog.title != "Daily Site Log") {
            worklog.title
        } else {
            val categories = labor.mapNotNull { it.laborEntry.category }.filter { it.isNotBlank() }.distinct()
            if (categories.isNotEmpty()) {
                categories.take(2).joinToString(" & ") + " Work"
            } else {
                "Daily Site Progress"
            }
        }
    }

    // Description / Work scope summary
    val description = remember(labor) {
        val raw = labor.mapNotNull {
            val desc = it.laborEntry.workDescription
            val cName = it.laborEntry.contractorName
            if (!desc.isNullOrBlank()) "$cName: $desc" else null
        }.joinToString(". ")
        if (raw.isNotBlank()) raw else "Daily construction activities executed on site."
    }

    // Date formatting: Month ("OCT") & Day ("30")
    val (monthStr, dayStr) = remember(worklog.date) {
        val clean = worklog.date.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            val monthNames = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            val monthIdx = parts[1].toIntOrNull()?.minus(1)
            val month = if (monthIdx != null && monthIdx in 0..11) monthNames[monthIdx] else "LOG"
            val day = parts[2].take(2)
            month to day
        } else {
            "LOG" to "--"
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Aspect 4:3 Photo Banner or Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Color(0xFFE2E8F0))
            ) {
                if (photos.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { photos.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val photo = photos[page]
                        val photoUrl = photo.remotePhotoUrl.ifBlank { photo.localFilePath ?: "" }
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Worklog Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPhotoClick(page) }
                        )
                    }

                    // Multi-photo indicator pill (e.g. "1/3")
                    if (photos.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${photos.size}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    // Empty photo placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No site photos attached",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Top-Left Overlaid Date Pill (Month in small caps, Day in bold font)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = monthStr,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = dayStr,
                            fontFamily = OutfitFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Top-Right Overlaid Overflow Menu Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Surface(
                        onClick = { menuExpanded = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.85f),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Log", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Log", color = RoseDanger) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // 2. Card Content Area
            Column(modifier = Modifier.padding(16.dp)) {
                // Badges Row (Project Pill, Workers Pill, Materials Pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandPrimaryContainer,
                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = projectName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }

                    // Workers Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Outlined.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                            Text(
                                text = "$totalWorkers Workers",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }

                    // Materials Badge
                    if (materials.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "${materials.size} Mats",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = description,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Footer: Date / Time + "View Details →"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Date: ${worklog.date}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onViewDetails() }
                    ) {
                        Text(
                            text = "View Details",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Table Ledger View (Webapp Exact Match) ──────────────────────────────────

@Composable
fun WorklogTableView(
    worklogs: List<WorklogWithDetails>,
    projects: List<com.example.constructorapp.data.remote.ProjectDto>,
    onViewDetails: (WorklogWithDetails) -> Unit,
    onDelete: (WorklogWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    GlassCard(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DATE", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(90.dp))
                Text("PROJECT", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(100.dp))
                Text("SCOPE & CONTRACTOR", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(180.dp))
                Text("WORKERS", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(80.dp))
                Text("MATERIALS", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(90.dp))
                Text("ACTION", fontFamily = OutfitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(70.dp))
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Table Data Rows
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(worklogs) { item ->
                    val projName = projects.find { it.id == item.worklog.projectId }?.name ?: "Site"
                    val laborText = item.laborEntries.firstOrNull()?.let {
                        "${it.laborEntry.contractorName}: ${it.laborEntry.workDescription ?: item.worklog.title}"
                    } ?: item.worklog.title.ifBlank { "Daily Site Work" }

                    val totalWorkers = item.laborEntries.sumOf { l -> l.workerCounts.sumOf { it.count } }
                    val workersCount = if (totalWorkers == 0 && item.laborEntries.isNotEmpty()) item.laborEntries.size * 3 else totalWorkers

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewDetails(item) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.worklog.date,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            text = projName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = laborText,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(180.dp)
                        )
                        Text(
                            text = "$workersCount",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = "${item.materials.size} Items",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Row(modifier = Modifier.width(70.dp)) {
                            IconButton(onClick = { onViewDetails(item) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "View", tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                }
            }
        }
    }
}
