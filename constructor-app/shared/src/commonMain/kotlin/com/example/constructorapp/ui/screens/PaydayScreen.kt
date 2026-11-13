package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.PayoutItemEntity
import com.example.constructorapp.data.model.PayoutRunWithItems
import com.example.constructorapp.data.model.WeeklyPayoutRunEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.DateTimeUtil
import com.example.constructorapp.util.toInrFull

private val samplePayoutRuns = listOf(
    PayoutRunWithItems(
        run = WeeklyPayoutRunEntity(
            id = "run-2026-w45",
            companyId = "company-1",
            weekStartDate = "2026-11-03",
            weekEndDate = "2026-11-09",
            totalAmount = 58700.0,
            status = "approved",
            createdAt = "2026-11-10"
        ),
        items = listOf(
            PayoutItemEntity(
                id = "item-1",
                payoutId = "run-2026-w45",
                recipientType = "labor_wage",
                recipientId = "mason-team-1",
                recipientName = "Periyasamy Mason Team",
                amountDue = 14500.0,
                amountPaid = 0.0,
                status = "pending",
                payoutClass = "rate",
                projectId = "proj-1",
                notes = "Brickwork contract 2nd floor"
            ),
            PayoutItemEntity(
                id = "item-2",
                payoutId = "run-2026-w45",
                recipientType = "labor_wage",
                recipientId = "nmr-gang-2",
                recipientName = "Kumar Daily Labor Gang",
                amountDue = 9200.0,
                amountPaid = 0.0,
                status = "pending",
                payoutClass = "nmr",
                projectId = "proj-1",
                notes = "Concrete curing & earth handling"
            ),
            PayoutItemEntity(
                id = "item-3",
                payoutId = "run-2026-w45",
                recipientType = "vendor_payment",
                recipientId = "vendor-steel-1",
                recipientName = "Sri Murugan Steels",
                amountDue = 35000.0,
                amountPaid = 35000.0,
                status = "paid",
                payoutClass = "rate",
                projectId = "proj-1",
                notes = "TMT bar delivery PO-102"
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaydayScreen(
    initialPayoutId: String? = null,
    onBackClick: () -> Unit = {}
) {
    val runs = remember { mutableStateListOf(*samplePayoutRuns.toTypedArray()) }
    var selectedRunId by remember { mutableStateOf(initialPayoutId ?: runs.firstOrNull()?.run?.id) }

    var isGenerateRunDialogOpen by remember { mutableStateOf(false) }
    var isSplitItemDialogOpen by remember { mutableStateOf(false) }
    var splitTargetItem by remember { mutableStateOf<PayoutItemEntity?>(null) }
    var weekStartDate by remember { mutableStateOf("2026-11-10") }
    var weekEndDate by remember { mutableStateOf("2026-11-16") }
    var calcMethod by remember { mutableStateOf("auto") }
    var includeUnpaid by remember { mutableStateOf(true) }

    val selectedRunIndex = runs.indexOfFirst { it.run.id == selectedRunId }
    val currentRun = if (selectedRunIndex >= 0) runs[selectedRunIndex] else null

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
                        IconButton(onClick = { isGenerateRunDialogOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Run", tint = BrandPrimary)
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
                // Active Run Overview Card
                item {
                    if (currentRun != null) {
                        PaydayRunHeaderCard(
                            runWithItems = currentRun,
                            onApprove = {
                                val updatedRun = currentRun.run.copy(status = "approved")
                                runs[selectedRunIndex] = currentRun.copy(run = updatedRun)
                            },
                            onDisburse = {
                                val updatedRun = currentRun.run.copy(status = "paid")
                                val updatedItems = currentRun.items.map { it.copy(status = "paid", amountPaid = it.amountDue) }
                                runs[selectedRunIndex] = currentRun.copy(run = updatedRun, items = updatedItems)
                            }
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
                                FluidGlossButton(
                                    onClick = { isGenerateRunDialogOpen = true },
                                    variant = FluidGlossButtonVariant.Primary,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
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
                            text = "${currentRun?.items?.size ?: 0} recipients",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Recipients items list
                val items = currentRun?.items ?: emptyList()
                if (items.isEmpty() && currentRun != null) {
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
                                if (selectedRunIndex >= 0) {
                                    val currentItems = runs[selectedRunIndex].items
                                    val idx = currentItems.indexOfFirst { it.id == item.id }
                                    if (idx >= 0) {
                                        val updated = currentItems[idx].copy(status = newStatus, amountPaid = amt)
                                        val newItems = currentItems.toMutableList().apply { set(idx, updated) }
                                        runs[selectedRunIndex] = runs[selectedRunIndex].copy(items = newItems)
                                    }
                                }
                            },
                            onSplitClick = {
                                splitTargetItem = item
                                isSplitItemDialogOpen = true
                            }
                        )
                    }
                }
            }
        }

        // Generate Run Dialog
        if (isGenerateRunDialogOpen) {
            GeneratePaydayRunDialog(
                weekStart = weekStartDate,
                weekEnd = weekEndDate,
                calcMethod = calcMethod,
                includeUnpaid = includeUnpaid,
                onDismiss = { isGenerateRunDialogOpen = false },
                onConfirm = { start, end, method, unpaid, sampleItems ->
                    weekStartDate = start
                    weekEndDate = end
                    calcMethod = method
                    includeUnpaid = unpaid
                    val newRunId = "run-${DateTimeUtil.randomUuid().take(8)}"
                    val newRun = PayoutRunWithItems(
                        run = WeeklyPayoutRunEntity(
                            id = newRunId,
                            weekStartDate = start,
                            weekEndDate = end,
                            totalAmount = sampleItems.sumOf { it.amountDue },
                            status = "draft"
                        ),
                        items = sampleItems.map { it.copy(payoutId = newRunId) }
                    )
                    runs.add(0, newRun)
                    selectedRunId = newRunId
                    isGenerateRunDialogOpen = false
                }
            )
        }

        // Split Item Dialog
        if (isSplitItemDialogOpen && splitTargetItem != null) {
            val target = splitTargetItem!!
            SplitPayoutItemDialog(
                targetItem = target,
                onDismiss = {
                    isSplitItemDialogOpen = false
                    splitTargetItem = null
                },
                onConfirm = { rateAmt, nmrAmt ->
                    if (selectedRunIndex >= 0) {
                        val currentItems = runs[selectedRunIndex].items
                        val targetIndex = currentItems.indexOfFirst { it.id == target.id }
                        if (targetIndex >= 0) {
                            val rateItem = target.copy(
                                id = DateTimeUtil.randomUuid(),
                                recipientName = "${target.recipientName} (Rate)",
                                amountDue = rateAmt,
                                amountPaid = 0.0,
                                payoutClass = "rate",
                                notes = "Split Rate Component • ${target.notes ?: ""}".trim()
                            )
                            val nmrItem = target.copy(
                                id = DateTimeUtil.randomUuid(),
                                recipientName = "${target.recipientName} (NMR)",
                                amountDue = nmrAmt,
                                amountPaid = 0.0,
                                payoutClass = "nmr",
                                notes = "Split NMR Component • ${target.notes ?: ""}".trim()
                            )
                            val newItems = currentItems.toMutableList().apply {
                                removeAt(targetIndex)
                                add(targetIndex, rateItem)
                                add(targetIndex + 1, nmrItem)
                            }
                            runs[selectedRunIndex] = runs[selectedRunIndex].copy(items = newItems)
                        }
                    }
                    isSplitItemDialogOpen = false
                    splitTargetItem = null
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
                    FluidGlossButton(
                        onClick = onApprove,
                        variant = FluidGlossButtonVariant.Primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
                        Text("Approve Weekly Pay-Day Run", fontWeight = FontWeight.Bold)
                    }
                }
                "approved" -> {
                    FluidGlossButton(
                        onClick = onDisburse,
                        variant = FluidGlossButtonVariant.Success,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
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
                    FluidGlossButton(
                        onClick = onSplitClick,
                        variant = FluidGlossButtonVariant.Outlined,
                        size = FluidGlossButtonSize.Small,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Split Rate/NMR", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FluidGlossButton(
                        onClick = {
                            val newStat = if (isPaid) "pending" else "paid"
                            val amt = if (isPaid) 0.0 else item.amountDue
                            onStatusChange(newStat, amt)
                        },
                        variant = if (isPaid) FluidGlossButtonVariant.Success else FluidGlossButtonVariant.Secondary,
                        size = FluidGlossButtonSize.Small,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPaid) "Paid" else "Mark Paid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Generate Weekly Pay-Day Run",
        subtitle = "Compile attendance muster and calculate contractor disbursements",
        icon = Icons.Default.Payments,
        iconTint = BrandPrimary,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val dummyItems = listOf(
                        PayoutItemEntity(
                            id = DateTimeUtil.randomUuid(),
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
                            id = DateTimeUtil.randomUuid(),
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
                            id = DateTimeUtil.randomUuid(),
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
                variant = FluidGlossButtonVariant.Primary
            ) {
                Text("Generate Run", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FluidGlossButton(
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary
            ) {
                Text("Cancel")
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = start,
                onValueChange = { start = it },
                label = "Week Start Date",
                placeholder = "YYYY-MM-DD",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = end,
                onValueChange = { end = it },
                label = "Week End Date",
                placeholder = "YYYY-MM-DD",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
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

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Split Rate & NMR Accounts",
        subtitle = "Recipient: ${targetItem.recipientName} • Total: ${targetItem.amountDue.toInrFull()}",
        icon = Icons.Default.CallSplit,
        iconTint = BrandPrimary,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val r = rateText.toDoubleOrNull() ?: 0.0
                    val n = nmrText.toDoubleOrNull() ?: 0.0
                    if (r + n <= 0.0) {
                        error = "Sum must be greater than zero"
                        return@FluidGlossButton
                    }
                    onConfirm(r, n)
                },
                variant = FluidGlossButtonVariant.Primary
            ) {
                Text("Confirm Split", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FluidGlossButton(
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary
            ) {
                Text("Cancel")
            }
        }
    ) {
        FluidGlossTextField(
            value = rateText,
            onValueChange = { rateText = it; error = null },
            label = "Rate Contract Share (₹)",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        FluidGlossTextField(
            value = nmrText,
            onValueChange = { nmrText = it; error = null },
            label = "NMR Labor Share (₹)",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = RoseDanger, fontSize = 12.sp)
        }
    }
}
