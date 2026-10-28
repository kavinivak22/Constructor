package com.example.constructorapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.*
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onNavigate: (NavKey) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val project = projects.find { it.id == projectId }
    val tasks by viewModel.tasks.collectAsState()

    var isCreateTaskOpen by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val budgetStr = project?.budget?.let { currencyFormatter.format(it) } ?: "₹0"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = project?.location ?: "Site Operations Hub",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Overview & Budget Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROJECT OVERVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = project?.status?.uppercase() ?: "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Total Contract Budget", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = budgetStr,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Site Status", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = "On Schedule",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SkyInfo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { 0.35f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BrandPrimary,
                            trackColor = SlateSurfaceSubtle
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Client Information
                        if (!project?.clientName.isNullOrBlank()) {
                            HorizontalDivider(color = SlateDivider)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = project?.clientName ?: "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }

                                if (!project?.clientPhone.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldSuccess.copy(alpha = 0.15f))
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${project?.clientPhone}"))
                                                context.startActivity(intent)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = project?.clientPhone ?: "",
                                            fontSize = 11.sp,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Operations Command Center
            item {
                Text(
                    text = "COMMAND CENTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HubActionCard(
                            title = "Daily Worklog",
                            subtitle = "Labor & NMR progress",
                            icon = Icons.Default.Assignment,
                            accentColor = BrandPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Worklog(projectId, projectName)) }
                        )
                        HubActionCard(
                            title = "Building Plan",
                            subtitle = "Stages & quality checklists",
                            icon = Icons.Default.Layers,
                            accentColor = SkyInfo,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(BuildingPlan(projectId, projectName)) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HubActionCard(
                            title = "Material Calc",
                            subtitle = "Civil takeoff & formulas",
                            icon = Icons.Default.Calculate,
                            accentColor = PurpleAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(MaterialEstimation(projectId)) }
                        )
                        HubActionCard(
                            title = "Purchase Orders",
                            subtitle = "Supplier procurement",
                            icon = Icons.Default.ShoppingCart,
                            accentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(PurchaseOrders(projectId)) }
                        )
                    }

                    HubActionCard(
                        title = "Site Cash & Pouch",
                        subtitle = "Disbursements, petty expenses & vouchers",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = AmberWarning,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(ProjectPouch(projectId, projectName)) }
                    )
                }
            }

            // Site Tasks & Quality Inspection Checklist
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SITE CHECKLIST & TASKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${tasks.count { it.isCompleted }} of ${tasks.size} completed",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = { isCreateTaskOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SlateSurfaceSubtle,
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Task", fontSize = 12.sp)
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pending site tasks", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("Tap 'Add Task' above to assign works for this site.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(tasks) { task ->
                    TaskRowCard(
                        task = task,
                        onToggle = { isChecked ->
                            task.id?.let { taskId ->
                                viewModel.toggleTask(taskId, isChecked, projectId)
                            }
                        }
                    )
                }
            }
        }
    }

    if (isCreateTaskOpen) {
        CreateTaskDialog(
            projectId = projectId,
            onDismiss = { isCreateTaskOpen = false },
            onSubmit = { newTask ->
                viewModel.createTask(newTask) {
                    isCreateTaskOpen = false
                    Toast.makeText(context, "Task created!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun HubActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TaskRowCard(
    task: TaskItemDto,
    onToggle: (Boolean) -> Unit
) {
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> RoseDanger
        "low" -> SkyInfo
        else -> AmberWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldSuccess,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }

                if (!task.dueDate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Due: ${task.dueDate}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(priorityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = task.priority.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor
                )
            }
        }
    }
}

@Composable
fun CreateTaskDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onSubmit: (TaskItemDto) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var dueDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Site Task", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description / Milestone", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Specific Details / Checklist", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = calculatorFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)", fontSize = 11.sp) },
                    placeholder = { Text("2026-11-10", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(
                            TaskItemDto(
                                projectId = projectId,
                                title = title.trim(),
                                description = description.ifBlank { null },
                                priority = priority,
                                dueDate = dueDate.ifBlank { null }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Add Task")
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
