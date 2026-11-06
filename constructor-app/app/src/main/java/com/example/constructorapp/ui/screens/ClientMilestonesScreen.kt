package com.example.constructorapp.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.ClientMilestoneEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ClientMilestonesUiState
import com.example.constructorapp.ui.viewmodel.ClientMilestonesViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMilestonesScreen(
    initialProjectId: String? = null,
    viewModel: ClientMilestonesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialProjectId) {
        if (!initialProjectId.isNullOrBlank()) {
            viewModel.filterByProject(initialProjectId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Client Milestones",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stage-wise progress payments from client",
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
                                tint = BrandPrimary
                            )
                        }
                    },
                    actions = {
                        if (state.milestones.isEmpty()) {
                            TextButton(
                                onClick = {
                                    val projId = initialProjectId ?: "project-default"
                                    viewModel.initializeDefaultMilestones(projId, 4500000.0) // Sample 45 Lakhs contract
                                }
                            ) {
                                Text("Load Standard Stages", color = BrandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateSurface.copy(alpha = 0.85f),
                        titleContentColor = TextPrimary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3 KPI Cards Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MilestoneKpiCard(
                                title = "Contract Value",
                                value = state.totalContractValue.toInrFull(),
                                icon = Icons.Default.AccountBalance,
                                iconTint = SkyInfo,
                                modifier = Modifier.weight(1f)
                            )
                            MilestoneKpiCard(
                                title = "Collected Income",
                                value = state.totalCollected.toInrFull(),
                                icon = Icons.Default.CheckCircle,
                                iconTint = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            MilestoneKpiCard(
                                title = "Pending Due",
                                value = state.totalPending.toInrFull(),
                                icon = Icons.Default.PendingActions,
                                iconTint = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Overall Progress Bar Card
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Overall Collection Progress", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("${state.overallProgressPct}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (state.overallProgressPct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = BrandPrimary,
                                    trackColor = SlateSurfaceSubtle
                                )
                            }
                        }
                    }
                }

                // Section Header
                item {
                    Text(
                        text = "Construction Payment Stages",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Milestone Items
                if (state.milestones.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No milestones setup for this project", color = TextSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val projId = initialProjectId ?: "project-default"
                                        viewModel.initializeDefaultMilestones(projId, 4500000.0)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Initialize 6 Standard Construction Stages", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(state.milestones, key = { it.id }) { milestone ->
                        ClientMilestoneCard(
                            milestone = milestone,
                            onRecordPayment = { viewModel.setRecordPaymentDialogOpen(true, milestone) }
                        )
                    }
                }
            }
        }

        // Record Payment Dialog
        if (state.isRecordPaymentDialogOpen && state.selectedMilestoneForPayment != null) {
            RecordClientPaymentDialog(
                milestone = state.selectedMilestoneForPayment!!,
                onDismiss = { viewModel.setRecordPaymentDialogOpen(false) },
                onConfirm = { amount, mode, date ->
                    viewModel.recordPayment(
                        milestoneId = state.selectedMilestoneForPayment!!.id,
                        amount = amount,
                        paymentMode = mode,
                        paymentDate = date
                    )
                }
            )
        }
    }
}

@Composable
fun MilestoneKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
            Text(title, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun ClientMilestoneCard(
    milestone: ClientMilestoneEntity,
    onRecordPayment: () -> Unit
) {
    val balance = (milestone.expectedAmount - milestone.collectedAmount).coerceAtLeast(0.0)
    val isCollected = milestone.status.equals("collected", ignoreCase = true) || balance <= 0.0

    val (badgeText, badgeBg, badgeTextColor) = when {
        isCollected -> Triple("Fully Collected", EmeraldSuccessContainer, EmeraldSuccess)
        milestone.collectedAmount > 0.0 -> Triple("Partially Due", AmberWarningContainer, AmberWarning)
        else -> Triple("Upcoming Stage", SkyInfoContainer, SkyInfo)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = SlateSurfaceSubtle,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${milestone.stageNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = milestone.stageName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!milestone.stageNameTa.isNullOrBlank()) {
                            Text(
                                text = milestone.stageNameTa,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress breakdown numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceSubtle.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Expected (${milestone.pctShare.toInt()}%)", fontSize = 10.sp, color = TextSecondary)
                    Text(milestone.expectedAmount.toInrFull(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("Collected", fontSize = 10.sp, color = TextSecondary)
                    Text(milestone.collectedAmount.toInrFull(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Column {
                    Text("Balance Due", fontSize = 10.sp, color = TextSecondary)
                    Text(balance.toInrFull(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (balance > 0) AmberWarning else TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCollected) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Collection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fully Cleared", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordClientPaymentDialog(
    milestone: ClientMilestoneEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, paymentMode: String, paymentDate: String) -> Unit
) {
    val remainingBalance = (milestone.expectedAmount - milestone.collectedAmount).coerceAtLeast(0.0)
    var amountText by remember { mutableStateOf(remainingBalance.toInt().toString()) }
    var paymentMode by remember { mutableStateOf("bank_transfer") }
    var paymentDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Record Client Payment", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stage: ${milestone.stageName} (Due: ${remainingBalance.toInrFull()})",
                    fontSize = 12.sp,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Collected Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("bank_transfer" to "Bank", "cheque" to "Cheque", "upi" to "UPI", "cash" to "Cash").forEach { (m, label) ->
                        FilterChip(
                            selected = paymentMode == m,
                            onClick = { paymentMode = m },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceSubtle,
                                labelColor = TextSecondary,
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("Payment Date") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = RoseDanger, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                error = "Please enter a valid amount"
                                return@Button
                            }
                            onConfirm(amt, paymentMode, paymentDate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Record Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun ClientMilestonesScreen(
    initialProjectId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val milestonesViewModel: ClientMilestonesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ClientMilestonesViewModel(database) as T
            }
        }
    )

    ClientMilestonesScreen(
        initialProjectId = initialProjectId,
        viewModel = milestonesViewModel,
        onBackClick = onBackClick
    )
}
