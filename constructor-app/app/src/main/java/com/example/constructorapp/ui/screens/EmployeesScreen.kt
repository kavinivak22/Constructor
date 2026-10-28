package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.EmployeeDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel

@Composable
fun EmployeesScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEmployees()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Team & Permissions",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage staff access & roles",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Member", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = SlateBackground,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SlateSurfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Team Members Added",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Add Member' to invite staff and configure permissions.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(employees) { employee ->
                        EmployeeCard(
                            employee = employee,
                            onDelete = {
                                employee.id?.let { id -> viewModel.removeEmployee(id) }
                            }
                        )
                    }
                }
            }

            if (showAddDialog) {
                AddEmployeeDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { newEmp ->
                        viewModel.addEmployee(newEmp) {
                            showAddDialog = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EmployeeCard(
    employee: EmployeeDto,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f))
                            .border(1.dp, BrandPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.name.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = employee.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = employee.email,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (employee.role) {
                        "Admin" -> RoseDanger
                        "Manager" -> PurpleAccent
                        "Site Engineer" -> SkyInfo
                        "Accountant" -> EmeraldSuccess
                        else -> BrandPrimary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = employee.role,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!employee.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Phone: ${employee.phone}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
