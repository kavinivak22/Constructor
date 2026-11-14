package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.example.constructorapp.data.local.entity.ClockUtils
import com.example.constructorapp.data.model.EmployeeDto
import com.example.constructorapp.theme.*

@Composable
fun EmployeesScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var employees by remember {
        mutableStateOf(
            listOf(
                EmployeeDto(
                    id = "emp-1",
                    companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6",
                    name = "Kavin Bharathi",
                    email = "kavin@goldenkattumaanam.com",
                    role = "admin"
                ),
                EmployeeDto(
                    id = "emp-2",
                    companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6",
                    name = "Suresh Kumar",
                    email = "suresh.site@goldenkattumaanam.com",
                    role = "engineer"
                ),
                EmployeeDto(
                    id = "emp-3",
                    companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6",
                    name = "Muthu Vel",
                    email = "muthu.supervisor@goldenkattumaanam.com",
                    role = "supervisor"
                )
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column {
                            Text(
                                text = "Team & Permissions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Manage staff access & roles",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                employees = employees.filter { it.id != employee.id }
                            }
                        )
                    }
                }
            }

            if (showAddDialog) {
                AddEmployeeDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { newEmp ->
                        employees = employees + newEmp.copy(id = "emp-${ClockUtils.nowMillis()}")
                        showAddDialog = false
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = employee.email,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = employee.role.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = RoseDanger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
