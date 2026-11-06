package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.main.ProjectsViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.CurrencyFormatter

@Composable
fun ProjectsScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (ProjectDto) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    val statusOptions = listOf("All", "Active", "Completed", "On-Hold")

    val filteredProjects = remember(projects, searchQuery, selectedStatus) {
        projects.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                (p.clientName?.contains(searchQuery, ignoreCase = true) == true) ||
                (p.location?.contains(searchQuery, ignoreCase = true) == true)

            val matchesStatus = when (selectedStatus) {
                "Active" -> p.status == null || p.status.equals("active", ignoreCase = true) || p.status.equals("in progress", ignoreCase = true)
                "Completed" -> p.status?.equals("completed", ignoreCase = true) == true
                "On-Hold" -> p.status?.equals("on-hold", ignoreCase = true) == true || p.status?.equals("planning", ignoreCase = true) == true
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 1. Inline Header with + New Project Button & optional back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Projects",
                    fontFamily = OutfitFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { isCreateDialogOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "New Project",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Search & Status Filter Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects by name, client, or site...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = calculatorFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusOptions) { status ->
                    val isSelected = selectedStatus == status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandPrimaryContainer else GlassSurfaceSubtle)
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = status,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) BrandPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Project Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLoading && projects.isEmpty()) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlateSurfaceSubtle)
                    )
                }
            } else if (filteredProjects.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Projects Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "No projects match your current filters. Try adjusting your search query or status filter.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    NativeProjectCard(
                        project = project,
                        onClick = { onProjectClick(project) }
                    )
                }
            }
        }
    }

    if (isCreateDialogOpen) {
        CreateProjectDialog(
            onDismiss = { isCreateDialogOpen = false },
            onSubmit = { newProject ->
                viewModel.createProject(newProject) { success ->
                    if (success) {
                        isCreateDialogOpen = false
                        viewModel.loadProjects()
                    }
                }
            }
        )
    }
}

// Overload taking dedicated ProjectsViewModel
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectClick: (ProjectDto) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Projects",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { viewModel.openCreateDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Project", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Search & Filter Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search projects...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = calculatorFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Active", "Completed", "On-Hold")) { status ->
                    val isSelected = state.selectedStatus == status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandPrimaryContainer else GlassSurfaceSubtle)
                            .clickable { viewModel.setSelectedStatus(status) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = status,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BrandPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(state.filteredProjects, key = { it.id }) { project ->
                NativeProjectCard(
                    project = project,
                    onClick = { onProjectClick(project) }
                )
            }
        }
    }

    if (state.isCreateDialogOpen) {
        CreateProjectDialog(
            onDismiss = { viewModel.closeCreateDialog() },
            onSubmit = { newProject ->
                viewModel.createProject(newProject)
            }
        )
    }
}

@Composable
fun NativeProjectCard(
    project: ProjectDto,
    onClick: () -> Unit
) {
    val imageUrl = if (!project.thumbnailUrl.isNullOrBlank()) {
        project.thumbnailUrl
    } else when {
        project.name.contains("PKGN", ignoreCase = true) -> "https://images.unsplash.com/photo-1612935089040-89195ef54677?auto=format&fit=crop&w=600&q=80"
        project.name.contains("LVA", ignoreCase = true) -> "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=600&q=80"
        project.name.contains("qwas", ignoreCase = true) -> "https://images.unsplash.com/photo-1634250878303-0c059dc54c4b?auto=format&fit=crop&w=600&q=80"
        project.name.contains("1234", ignoreCase = true) -> "https://images.unsplash.com/photo-1646608220368-c604d8e8130f?auto=format&fit=crop&w=600&q=80"
        else -> "https://images.unsplash.com/photo-1603287696448-61d5b81cb720?auto=format&fit=crop&w=600&q=80"
    }
    val budgetDisplay = when {
        project.budget >= 10_000_000 -> "₹${String.format(java.util.Locale.US, "%.1f", project.budget / 10_000_000.0)}Cr"
        project.budget >= 100_000 -> "₹${String.format(java.util.Locale.US, "%.1f", project.budget / 100_000.0)}L"
        project.budget > 0 -> "₹${(project.budget / 1_000).toInt()}k"
        else -> null
    }
    val statusText = project.status ?: "active"
    val isCompletedOrActive = statusText.equals("active", ignoreCase = true) || statusText.equals("completed", ignoreCase = true)
    val statusColor = if (isCompletedOrActive) EmeraldSuccess else AmberWarning
    val statusContainer = if (isCompletedOrActive) EmeraldSuccessContainer else AmberWarningContainer

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Name, Client, Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = project.name,
                        fontFamily = OutfitFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.clientName ?: "Internal Project",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Hero Image via Coil with padding and rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateSurfaceSubtle)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = project.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Body: Progress Bar & Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Text("${project.progress}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SlateDivider)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = project.startDate?.take(10) ?: "Scheduled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    if (budgetDisplay != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = budgetDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
