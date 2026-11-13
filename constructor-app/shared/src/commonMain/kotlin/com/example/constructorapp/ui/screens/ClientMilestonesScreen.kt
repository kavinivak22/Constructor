package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.local.entity.ClientMilestoneEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.toInrFull

private fun createStandardMilestones(projectId: String, contractValue: Double): List<ClientMilestoneEntity> {
    return listOf(
        ClientMilestoneEntity(
            id = "ms-1",
            projectId = projectId,
            stageNumber = 1,
            stageName = "Foundation & Plinth Beam Completion",
            stageNameTa = "அஸ்திவாரம் மற்றும் பிளின்த் பீம்",
            pctShare = 15.0,
            expectedAmount = contractValue * 0.15,
            collectedAmount = contractValue * 0.15,
            status = "collected",
            paymentMode = "bank_transfer",
            paymentDate = "2026-10-15"
        ),
        ClientMilestoneEntity(
            id = "ms-2",
            projectId = projectId,
            stageNumber = 2,
            stageName = "Ground Floor Slab Casting",
            stageNameTa = "தரைத்தள கான்கிரீட் தளம்",
            pctShare = 20.0,
            expectedAmount = contractValue * 0.20,
            collectedAmount = contractValue * 0.20,
            status = "collected",
            paymentMode = "cheque",
            paymentDate = "2026-11-02"
        ),
        ClientMilestoneEntity(
            id = "ms-3",
            projectId = projectId,
            stageNumber = 3,
            stageName = "First Floor Roof Slab Casting",
            stageNameTa = "முதல் தள கூரை தளம்",
            pctShare = 20.0,
            expectedAmount = contractValue * 0.20,
            collectedAmount = contractValue * 0.10,
            status = "pending",
            paymentMode = "upi",
            dueDate = "2026-11-20"
        ),
        ClientMilestoneEntity(
            id = "ms-4",
            projectId = projectId,
            stageNumber = 4,
            stageName = "Brickwork & Internal Plastering",
            stageNameTa = "செங்கல் கட்டுமானம் மற்றும் பூச்சு",
            pctShare = 20.0,
            expectedAmount = contractValue * 0.20,
            collectedAmount = 0.0,
            status = "upcoming",
            dueDate = "2026-12-15"
        ),
        ClientMilestoneEntity(
            id = "ms-5",
            projectId = projectId,
            stageNumber = 5,
            stageName = "Flooring, Electrical & Plumbing",
            stageNameTa = "தரை ஓடுகள், வயரிங் & குழாய்",
            pctShare = 15.0,
            expectedAmount = contractValue * 0.15,
            collectedAmount = 0.0,
            status = "upcoming",
            dueDate = "2027-01-10"
        ),
        ClientMilestoneEntity(
            id = "ms-6",
            projectId = projectId,
            stageNumber = 6,
            stageName = "Painting & Key Handover",
            stageNameTa = "வர்ணம் பூசுதல் & சாவி ஒப்படைப்பு",
            pctShare = 10.0,
            expectedAmount = contractValue * 0.10,
            collectedAmount = 0.0,
            status = "upcoming",
            dueDate = "2027-02-01"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMilestonesScreen(
    projectId: String = "",
    onBackClick: () -> Unit = {}
) {
    val activeProjectId = projectId.ifBlank { "project-default" }
    var milestones by remember { mutableStateOf(createStandardMilestones(activeProjectId, 4500000.0)) }
    var isRecordPaymentDialogOpen by remember { mutableStateOf(false) }
    var selectedMilestoneForPayment by remember { mutableStateOf<ClientMilestoneEntity?>(null) }

    val totalContractValue = remember(milestones) { milestones.sumOf { it.expectedAmount } }
    val totalCollected = remember(milestones) { milestones.sumOf { it.collectedAmount } }
    val totalPending = (totalContractValue - totalCollected).coerceAtLeast(0.0)
    val overallProgressPct = if (totalContractValue > 0) ((totalCollected / totalContractValue) * 100).toInt() else 0

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
                        if (milestones.isEmpty()) {
                            TextButton(
                                onClick = {
                                    milestones = createStandardMilestones(activeProjectId, 4500000.0)
                                }
                            ) {
                                Text("Load Standard Stages", color = BrandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3 KPI Cards Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MilestoneKpiCard(
                                title = "Contract Value",
                                value = totalContractValue.toInrFull(),
                                icon = Icons.Default.AccountBalance,
                                iconTint = SkyInfo,
                                modifier = Modifier.weight(1f)
                            )
                            MilestoneKpiCard(
                                title = "Collected Income",
                                value = totalCollected.toInrFull(),
                                icon = Icons.Default.CheckCircle,
                                iconTint = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            MilestoneKpiCard(
                                title = "Pending Due",
                                value = totalPending.toInrFull(),
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
                                    Text("$overallProgressPct%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (overallProgressPct / 100f).coerceIn(0f, 1f) },
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
                if (milestones.isEmpty()) {
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
                                FluidGlossButton(
                                    text = "Initialize 6 Standard Construction Stages",
                                    onClick = {
                                        milestones = createStandardMilestones(activeProjectId, 4500000.0)
                                    },
                                    variant = FluidGlossButtonVariant.Primary,
                                    size = FluidGlossButtonSize.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(milestones, key = { it.id }) { milestone ->
                        ClientMilestoneCard(
                            milestone = milestone,
                            onRecordPayment = {
                                selectedMilestoneForPayment = milestone
                                isRecordPaymentDialogOpen = true
                            }
                        )
                    }
                }
            }
        }

        // Record Payment Dialog
        if (isRecordPaymentDialogOpen && selectedMilestoneForPayment != null) {
            val targetMilestone = selectedMilestoneForPayment!!
            RecordClientPaymentDialog(
                milestone = targetMilestone,
                onDismiss = {
                    isRecordPaymentDialogOpen = false
                    selectedMilestoneForPayment = null
                },
                onConfirm = { amount, mode, date ->
                    milestones = milestones.map { m ->
                        if (m.id == targetMilestone.id) {
                            val newCollected = m.collectedAmount + amount
                            val newStatus = if (newCollected >= m.expectedAmount) "collected" else "pending"
                            m.copy(
                                collectedAmount = newCollected,
                                status = newStatus,
                                paymentMode = mode,
                                paymentDate = date
                            )
                        } else m
                    }
                    isRecordPaymentDialogOpen = false
                    selectedMilestoneForPayment = null
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
                    FluidGlossButton(
                        text = "Record Collection",
                        onClick = onRecordPayment,
                        variant = FluidGlossButtonVariant.Primary,
                        size = FluidGlossButtonSize.Small,
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
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
    var paymentDate by remember { mutableStateOf("2026-11-13") }
    var error by remember { mutableStateOf<String?>(null) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Record Client Payment",
        subtitle = "Stage: ${milestone.stageName} (Due: ${remainingBalance.toInrFull()})",
        icon = Icons.Default.Paid,
        confirmButton = {
            FluidGlossButton(
                text = "Record Payment",
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        error = "Please enter a valid amount"
                        return@FluidGlossButton
                    }
                    onConfirm(amt, paymentMode, paymentDate)
                },
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        FluidGlossTextField(
            value = amountText,
            onValueChange = { amountText = it; error = null },
            label = "Collected Amount (₹)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
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

        FluidGlossTextField(
            value = paymentDate,
            onValueChange = { paymentDate = it; error = null },
            label = "Payment Date",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(error!!, color = RoseDanger, fontSize = 12.sp)
        }
    }
}
