package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.constructorapp.data.local.entity.PayoutItemEntity
import com.example.constructorapp.data.local.entity.PayoutRunWithItems
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.PaydayUiState
import com.example.constructorapp.ui.viewmodel.PaydayViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaydayScreen(
    initialPayoutId: String? = null,
    viewModel: PaydayViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialPayoutId) {
        if (!initialPayoutId.isNullOrBlank()) {
            viewModel.selectRun(initialPayoutId)
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
                                text = "Weekly Pay-Day Runs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Wage processing & contractor payouts",
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
                        IconButton(onClick = { viewModel.setGenerateRunDialogOpen(true) }) {
                            Icon(Icons.Default.Add, contentDescription = "New Run", tint = BrandPrimary)
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
                // Active Run Overview Card
                item {
                    val currentRun = state.selectedRun
                    if (currentRun != null) {
                        PaydayRunHeaderCard(
                            runWithItems = currentRun,
                            onApprove = { viewModel.approvePayoutRun(currentRun.run.id) },
                            onDisburse = { viewModel.disbursePayoutRun(currentRun.run.id) }
                        )
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No Pay-Day Runs Created", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Generate a weekly run to compile muster attendance & payouts.", color = TextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.setGenerateRunDialogOpen(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Generate Weekly Run", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section Header: Recipient Payout Items
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payout Recipients",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.selectedRun?.items?.size ?: 0} recipients",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Recipients items list
                val items = state.selectedRun?.items ?: emptyList()
                if (items.isEmpty() && state.selectedRun != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recipient line items in this run", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        PayoutRecipientCard(
                            item = item,
                            onStatusChange = { newStatus, amt ->
                                viewModel.updatePayoutItemStatus(item.id, newStatus, amt)
                            },
                            onSplitClick = {
                                viewModel.setSplitItemDialogOpen(true, item)
                            }
                        )
                    }
                }
            }
        }

        // Generate Run Dialog
        if (state.isGenerateRunDialogOpen) {
            GeneratePaydayRunDialog(
                weekStart = state.weekStartDate,
                weekEnd = state.weekEndDate,
                calcMethod = state.calcMethod,
                includeUnpaid = state.includeUnpaid,
                onDismiss = { viewModel.setGenerateRunDialogOpen(false) },
                onConfirm = { start, end, method, unpaid, sampleItems ->
                    viewModel.setDateRange(start, end)
                    viewModel.setCalcMethod(method)
                    viewModel.setIncludeUnpaid(unpaid)
                    viewModel.generatePayoutRun(
                        startDate = start,
                        endDate = end,
                        method = method,
                        includePreviousUnpaid = unpaid,
                        customItems = sampleItems
                    )
                }
            )
        }

        // Split Item Dialog
        if (state.isSplitItemDialogOpen && state.splitTargetItem != null) {
            SplitPayoutItemDialog(
                targetItem = state.splitTargetItem!!,
                onDismiss = { viewModel.setSplitItemDialogOpen(false) },
                onConfirm = { rateAmt, nmrAmt ->
                    viewModel.splitPayoutItem(state.splitTargetItem!!, rateAmt, nmrAmt)
                }
            )
        }
    }
}

@Composable
fun PaydayRunHeaderCard(
    runWithItems: PayoutRunWithItems,
    onApprove: () -> Unit,
    onDisburse: () -> Unit
) {
    val run = runWithItems.run
    val (statusLabel, statusBg, statusColor) = when (run.status.lowercase()) {
        "paid" -> Triple("Disbursed / Paid", EmeraldSuccessContainer, EmeraldSuccess)
        "approved" -> Triple("Approved for Payout", SkyInfoContainer, SkyInfo)
        else -> Triple("Draft Run", AmberWarningContainer, AmberWarning)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Week: ${run.weekStartDate} to ${run.weekEndDate}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total Payout: ${runWithItems.items.sumOf { it.amountDue }.toInrFull()}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons based on status
            when (run.status.lowercase()) {
                "draft" -> {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve Weekly Pay-Day Run", fontWeight = FontWeight.Bold)
                    }
                }
                "approved" -> {
                    Button(
                        onClick = onDisburse,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disburse & Log Project Expenses", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PayoutRecipientCard(
    item: PayoutItemEntity,
    onStatusChange: (newStatus: String, amountPaid: Double) -> Unit,
    onSplitClick: () -> Unit
) {
    val isPaid = item.status.equals("paid", ignoreCase = true)
    val isRate = item.payoutClass.equals("rate", ignoreCase = true)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.recipientName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = item.recipientType.replace("_", " ").uppercase(),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // Dual Ledger Classification Pill
                Surface(
                    color = if (isRate) Color(0x268B5CF6) else Color(0x2614B8A6),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isRate) Color(0x4D8B5CF6) else Color(0x4D14B8A6))
                ) {
                    Text(
                        text = if (isRate) "RATE ACCOUNT" else "NMR LABOR",
                        color = if (isRate) Color(0xFFA78BFA) else Color(0xFF2DD4BF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Amount Due", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = item.amountDue.toInrFull(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onSplitClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Split Rate/NMR", fontSize = 10.sp, color = BrandPrimary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            val newStat = if (isPaid) "pending" else "paid"
                            val amt = if (isPaid) 0.0 else item.amountDue
                            onStatusChange(newStat, amt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaid) EmeraldSuccess else SlateSurfaceSubtle
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (isPaid) "Paid" else "Mark Paid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) Color.White else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratePaydayRunDialog(
    weekStart: String,
    weekEnd: String,
    calcMethod: String,
    includeUnpaid: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        start: String,
        end: String,
        method: String,
        unpaid: Boolean,
        sampleItems: List<PayoutItemEntity>
    ) -> Unit
) {
    var start by remember { mutableStateOf(weekStart) }
    var end by remember { mutableStateOf(weekEnd) }
    var method by remember { mutableStateOf(calcMethod) }
    var unpaid by remember { mutableStateOf(includeUnpaid) }

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
                Text("Generate Weekly Pay-Day Run", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Week Start Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Week End Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Calculation Methodology", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    "auto" to "Auto-compile NMR & Rate from Worklogs",
                    "wage_wise" to "Wage-wise Daily Muster",
                    "work_wise" to "Work-wise Contract Rates"
                ).forEach { (m, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { method = m }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = method == m, onClick = { method = m })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 12.sp, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = unpaid, onCheckedChange = { unpaid = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Include backlogged unpaid wages", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dummyItems = listOf(
                                PayoutItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    payoutId = "",
                                    recipientType = "labor_wage",
                                    recipientId = "mason-team-1",
                                    recipientName = "Periyasamy Mason Team",
                                    amountDue = 14500.0,
                                    amountPaid = 0.0,
                                    payoutClass = "rate",
                                    projectId = "project-1",
                                    notes = "Brickwork contract 2nd floor"
                                ),
                                PayoutItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    payoutId = "",
                                    recipientType = "labor_wage",
                                    recipientId = "nmr-gang-2",
                                    recipientName = "Kumar Daily Labor Gang",
                                    amountDue = 9200.0,
                                    amountPaid = 0.0,
                                    payoutClass = "nmr",
                                    projectId = "project-1",
                                    notes = "Concrete curing & earth handling"
                                ),
                                PayoutItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    payoutId = "",
                                    recipientType = "vendor_payment",
                                    recipientId = "vendor-steel-1",
                                    recipientName = "Sri Murugan Steels",
                                    amountDue = 35000.0,
                                    amountPaid = 0.0,
                                    payoutClass = "rate",
                                    projectId = "project-1",
                                    notes = "TMT bar delivery PO-102"
                                )
                            )
                            onConfirm(start, end, method, unpaid, dummyItems)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Generate Run", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SplitPayoutItemDialog(
    targetItem: PayoutItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (rateAmount: Double, nmrAmount: Double) -> Unit
) {
    var rateText by remember { mutableStateOf((targetItem.amountDue * 0.6).toInt().toString()) }
    var nmrText by remember { mutableStateOf((targetItem.amountDue * 0.4).toInt().toString()) }
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
                Text("Split Rate & NMR Accounts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Recipient: ${targetItem.recipientName}", fontSize = 12.sp, color = TextSecondary)
                Text("Total Amount: ${targetItem.amountDue.toInrFull()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate Contract Share (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nmrText,
                    onValueChange = { nmrText = it },
                    label = { Text("NMR Labor Share (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = RoseDanger, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val r = rateText.toDoubleOrNull() ?: 0.0
                            val n = nmrText.toDoubleOrNull() ?: 0.0
                            if (r + n <= 0.0) {
                                error = "Sum must be greater than zero"
                                return@Button
                            }
                            onConfirm(r, n)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Split", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun PaydayScreen(
    initialPayoutId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val paydayViewModel: PaydayViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaydayViewModel(database) as T
            }
        }
    )

    PaydayScreen(
        initialPayoutId = initialPayoutId,
        viewModel = paydayViewModel,
        onBackClick = onBackClick
    )
}
