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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.MaterialTakeoffReconciliationEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.MaterialsUiState
import com.example.constructorapp.ui.viewmodel.MaterialsViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialReconciliationScreen(
    initialProjectId: String? = null,
    viewModel: MaterialsViewModel,
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
                                text = "Material Rate Reconciliation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Committed budget vs actual consumption drift",
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
                        IconButton(onClick = { viewModel.setAddDriftDialogOpen(true) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Track", tint = BrandPrimary)
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
                // 4 KPI Summary Cards Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReconciliationKpiCard(
                                title = "Total Committed",
                                value = state.totalCommitted.toInrFull(),
                                subtitle = "Budgeted estimate",
                                icon = Icons.Default.Assessment,
                                iconTint = SkyInfo,
                                modifier = Modifier.weight(1f)
                            )
                            ReconciliationKpiCard(
                                title = "Actual Spend",
                                value = state.totalActual.toInrFull(),
                                subtitle = "Delivered / Consumed",
                                icon = Icons.Default.ShoppingCart,
                                iconTint = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isOverrun = state.netDrift > 0
                            val driftColor = if (isOverrun) RoseDanger else EmeraldSuccess
                            val driftIcon = if (isOverrun) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                            ReconciliationKpiCard(
                                title = "Net Drift Variance",
                                value = (if (isOverrun) "+" else "") + state.netDrift.toInrFull(),
                                subtitle = if (isOverrun) "+${String.format("%.1f", state.driftPct)}% over budget" else "Within budget",
                                icon = driftIcon,
                                iconTint = driftColor,
                                modifier = Modifier.weight(1f)
                            )
                            ReconciliationKpiCard(
                                title = "Budget Health",
                                value = "${state.greenCount} / ${state.reconciliationItems.size}",
                                subtitle = "${state.redCount} overrun • ${state.amberCount} mild",
                                icon = Icons.Default.HealthAndSafety,
                                iconTint = if (state.redCount > 0) RoseDanger else EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Itemized Reconciliation",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.reconciliationItems.size} items",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Empty State
                if (state.reconciliationItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No material reconciliation items", color = TextSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                FluidGlossButton(
                                    onClick = { viewModel.setAddDriftDialogOpen(true) },
                                    variant = FluidGlossButtonVariant.Outlined,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                    }
                                ) {
                                    Text("Track First Material", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    items(state.reconciliationItems, key = { it.id }) { item ->
                        ReconciliationItemCard(
                            item = item,
                            onDelete = { viewModel.deleteReconciliationItem(item.id) }
                        )
                    }
                }
            }
        }

        // Add Drift Track Dialog
        if (state.isAddDriftDialogOpen) {
            AddMaterialDriftDialog(
                defaultProjectId = initialProjectId ?: "project-default",
                onDismiss = { viewModel.setAddDriftDialogOpen(false) },
                onConfirm = { name, nameTa, unit, cQty, cRate, aQty, aRate, notes ->
                    viewModel.addReconciliationItem(
                        projectId = initialProjectId ?: "project-default",
                        materialName = name,
                        materialNameTa = nameTa,
                        unit = unit,
                        committedQty = cQty,
                        committedRate = cRate,
                        actualQty = aQty,
                        actualRate = aRate,
                        notes = notes
                    )
                }
            )
        }
    }
}

@Composable
fun ReconciliationKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun ReconciliationItemCard(
    item: MaterialTakeoffReconciliationEntity,
    onDelete: () -> Unit
) {
    val committedCost = item.committedQty * item.committedRate
    val actualCost = item.actualQty * item.actualRate
    val diff = actualCost - committedCost
    val isOver = diff > 0

    val (badgeText, badgeBg, badgeTextColor) = when (item.severity.lowercase()) {
        "red" -> Triple("Cost Overrun", RoseDangerContainer, RoseDanger)
        "amber" -> Triple("Mild Drift", AmberWarningContainer, AmberWarning)
        else -> Triple("On Budget", EmeraldSuccessContainer, EmeraldSuccess)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.materialName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!item.materialNameTa.isNullOrBlank()) {
                        Text(
                            text = item.materialNameTa,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-column comparison: Committed vs Actual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurfaceSubtle.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Committed (Est.)", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${item.committedQty} ${item.unit} @ ₹${item.committedRate.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total: ${committedCost.toInrFull()}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(GlassBorder)
                )

                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Actual (Delivered)", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${item.actualQty} ${item.unit} @ ₹${item.actualRate.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total: ${actualCost.toInrFull()}",
                        fontSize = 11.sp,
                        color = if (isOver) RoseDanger else EmeraldSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Net Financial Variance footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Variance: " + (if (isOver) "+ " else "") + diff.toInrFull(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) RoseDanger else EmeraldSuccess
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddMaterialDriftDialog(
    defaultProjectId: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        nameTa: String?,
        unit: String,
        committedQty: Double,
        committedRate: Double,
        actualQty: Double,
        actualRate: Double,
        notes: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameTa by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("bag") }
    var cQtyText by remember { mutableStateOf("") }
    var cRateText by remember { mutableStateOf("") }
    var aQtyText by remember { mutableStateOf("") }
    var aRateText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Add Material Reconciliation Track",
        subtitle = "Track variance between committed budget and actual site deliveries",
        icon = Icons.Default.SwapHoriz,
        iconTint = BrandPrimary,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val cQty = cQtyText.toDoubleOrNull()
                    val cRate = cRateText.toDoubleOrNull()
                    val aQty = aQtyText.toDoubleOrNull()
                    val aRate = aRateText.toDoubleOrNull()

                    if (name.isBlank()) {
                        error = "Please enter material name"
                        return@FluidGlossButton
                    }
                    if (cQty == null || cRate == null || aQty == null || aRate == null) {
                        error = "Please enter valid quantities and rates"
                        return@FluidGlossButton
                    }

                    onConfirm(name, nameTa.takeIf { it.isNotBlank() }, unit, cQty, cRate, aQty, aRate, notes.takeIf { it.isNotBlank() })
                },
                variant = FluidGlossButtonVariant.Primary
            ) {
                Text("Save Track", fontWeight = FontWeight.Bold)
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
            value = name,
            onValueChange = { name = it; error = null },
            label = "Material Name",
            placeholder = "e.g. 53 Grade OPC Cement",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = nameTa,
            onValueChange = { nameTa = it },
            label = "Tamil Name (Optional)",
            placeholder = "e.g. சிமெண்ட்",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = unit,
            onValueChange = { unit = it },
            label = "Unit of Measure (bag, kg, ton, nos)",
            placeholder = "bag",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("Budgeted Estimate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyInfo)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = cQtyText,
                onValueChange = { cQtyText = it; error = null },
                label = "Committed Qty",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = cRateText,
                onValueChange = { cRateText = it; error = null },
                label = "Committed Rate (₹)",
                placeholder = "0.00",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Actual Purchased / Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = aQtyText,
                onValueChange = { aQtyText = it; error = null },
                label = "Actual Qty",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = aRateText,
                onValueChange = { aRateText = it; error = null },
                label = "Actual Rate (₹)",
                placeholder = "0.00",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = RoseDanger, fontSize = 12.sp)
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun MaterialReconciliationScreen(
    initialProjectId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val materialsViewModel: MaterialsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MaterialsViewModel(database) as T
            }
        }
    )

    MaterialReconciliationScreen(
        initialProjectId = initialProjectId,
        viewModel = materialsViewModel,
        onBackClick = onBackClick
    )
}
