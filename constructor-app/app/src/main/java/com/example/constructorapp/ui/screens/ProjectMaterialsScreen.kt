package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavKey
import com.example.constructorapp.*
import com.example.constructorapp.data.remote.MaterialLogDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.AppDrawerContent
import com.example.constructorapp.ui.components.FloatingBottomNav
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMaterialsScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit,
    onNavigateToTab: (Int) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val projects by viewModel.projects.collectAsState()
    val project = projects.find { it.id == projectId } ?: ProjectDto(
        id = projectId,
        name = projectName,
        companyId = ""
    )

    val materials by viewModel.materials.collectAsState()
    val isMaterialsLoading by viewModel.isMaterialsLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf("all") } // all | low | out | in

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var materialToUpdateStock by remember { mutableStateOf<ProjectMaterialDto?>(null) }
    var updateStockIsAdd by remember { mutableStateOf(true) }
    var materialToEdit by remember { mutableStateOf<ProjectMaterialDto?>(null) }
    var materialForHistory by remember { mutableStateOf<ProjectMaterialDto?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadProjectMaterials(projectId)
    }

    val filteredMaterials = remember(materials, searchQuery, stockFilter) {
        materials.filter { mat ->
            val matchesSearch = mat.name.contains(searchQuery, ignoreCase = true) ||
                    (mat.category?.contains(searchQuery, ignoreCase = true) == true)

            val stock = mat.effectiveQuantity
            val minStock = mat.effectiveMinQuantity
            val matchesFilter = when (stockFilter) {
                "low" -> stock <= minStock
                "out" -> stock <= 0.0
                "in" -> stock > minStock
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBackground)
        ) {
            // Layer 1: Ambient Fluid Glass Background
            AmbientBlobBackground()

            // Layer 2: Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ─── Header ──────────────────────────────────────────────
                Surface(
                    color = Color.White.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Project Inventory",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = project.name.ifBlank { "Project Materials" },
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FluidGlossButton(
                            onClick = { showAddDialog = true },
                            variant = FluidGlossButtonVariant.Primary,
                            size = FluidGlossButtonSize.Small,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        ) {
                            Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ─── Search & Stock Filters ───────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluidGlossTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search materials or categories...",
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stock Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            "all" to "All Items (${materials.size})",
                            "low" to "Low Stock (${materials.count { it.effectiveQuantity <= it.effectiveMinQuantity }})",
                            "out" to "Out of Stock (${materials.count { it.effectiveQuantity <= 0.0 }})",
                            "in" to "In Stock (${materials.count { it.effectiveQuantity > it.effectiveMinQuantity }})"
                        )

                        items(filters) { (key, label) ->
                            val isSelected = stockFilter == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.75f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandPrimary else Color(0x1F0F172A)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable { stockFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // ─── Materials List ───────────────────────────────────────
                if (isMaterialsLoading && materials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(36.dp))
                    }
                } else if (filteredMaterials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Materials Found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (materials.isEmpty()) "No inventory items added to this project yet." else "No materials match your current filter.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                if (materials.isEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FluidGlossButton(
                                        onClick = { showAddDialog = true },
                                        variant = FluidGlossButtonVariant.Primary,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Add Your First Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMaterials, key = { it.id ?: it.name }) { material ->
                            MaterialInventoryCard(
                                material = material,
                                onCardClick = { materialForHistory = material },
                                onAddStock = {
                                    materialToUpdateStock = material
                                    updateStockIsAdd = true
                                },
                                onUseStock = {
                                    materialToUpdateStock = material
                                    updateStockIsAdd = false
                                },
                                onEdit = { materialToEdit = material }
                            )
                        }
                    }
                }
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp)
            )

        }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    // 1. Add Material Dialog
    if (showAddDialog) {
        AddMaterialDialog(
            projectId = projectId,
            siteId = project.siteId,
            onDismiss = { showAddDialog = false },
            onAdd = { name, category, initialQty, minQty, unit, supplier, cost, logExpense, totalCost, expenseDate, paymentStatus, invoiceNo ->
                viewModel.addMaterial(
                    projectId = projectId,
                    siteId = project.siteId,
                    name = name,
                    category = category,
                    initialStock = initialQty,
                    minStock = minQty,
                    unit = unit,
                    supplier = supplier,
                    cost = cost,
                    logExpense = logExpense,
                    totalCost = totalCost,
                    expenseDate = expenseDate,
                    paymentStatus = paymentStatus,
                    invoiceNumber = invoiceNo
                ) { success, errorMsg ->
                    if (success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Material '$name' added to inventory.")
                        }
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, errorMsg ?: "Failed to add material", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 2. Stock Update Dialog (Add or Use)
    materialToUpdateStock?.let { mat ->
        StockUpdateDialog(
            material = mat,
            isAdd = updateStockIsAdd,
            onDismiss = { materialToUpdateStock = null },
            onConfirm = { amount, unitRate, updateActivePrice, date, supplier, purpose, logExpense, restockAmount, paymentStatus, invoiceNo ->
                viewModel.updateStock(
                    projectId = projectId,
                    material = mat,
                    amount = amount,
                    isAdd = updateStockIsAdd,
                    unitRate = unitRate,
                    updateActivePrice = updateActivePrice,
                    date = date,
                    supplier = supplier,
                    purpose = purpose,
                    logExpense = logExpense,
                    restockAmount = restockAmount,
                    paymentStatus = paymentStatus,
                    invoiceNumber = invoiceNo
                ) { success, errorMsg ->
                    if (success) {
                        val action = if (updateStockIsAdd) "Added" else "Used"
                        scope.launch {
                            snackbarHostState.showSnackbar("$action $amount ${mat.effectiveUnit} for ${mat.name}.")
                        }
                        materialToUpdateStock = null
                    } else {
                        Toast.makeText(context, errorMsg ?: "Failed to update stock", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 3. Edit Material Dialog
    materialToEdit?.let { mat ->
        EditMaterialDialog(
            material = mat,
            onDismiss = { materialToEdit = null },
            onSave = { name, category, currentStock, minStock, unit, supplier, cost ->
                viewModel.editMaterial(
                    projectId = projectId,
                    materialId = mat.id ?: "",
                    name = name,
                    category = category,
                    currentStock = currentStock,
                    minStock = minStock,
                    unit = unit,
                    supplier = supplier,
                    cost = cost
                ) { success, errorMsg ->
                    if (success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Material '$name' updated.")
                        }
                        materialToEdit = null
                    } else {
                        Toast.makeText(context, errorMsg ?: "Failed to edit material", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = {
                viewModel.deleteMaterial(projectId, mat.id ?: "") { success ->
                    if (success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Material deleted.")
                        }
                        materialToEdit = null
                    } else {
                        Toast.makeText(context, "Failed to delete material", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 4. Material Logs Dialog
    materialForHistory?.let { mat ->
        MaterialHistoryDialog(
            material = mat,
            viewModel = viewModel,
            onDismiss = { materialForHistory = null }
        )
    }
}

// ─── Material Card ────────────────────────────────────────────────────────────

@Composable
fun MaterialInventoryCard(
    material: ProjectMaterialDto,
    onCardClick: () -> Unit,
    onAddStock: () -> Unit,
    onUseStock: () -> Unit,
    onEdit: () -> Unit
) {
    val isLowStock = material.effectiveQuantity <= material.effectiveMinQuantity

    GlassCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Name, Category, Low Stock badge, Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0x100F172A))
                    ) {
                        Text(
                            text = material.category?.takeIf { it.isNotBlank() } ?: "Uncategorized",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Low Stock",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0x120F172A)), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Split Quantity Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0x120F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Stock
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT STOCK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", material.effectiveQuantity).removeSuffix(".0"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) Color(0xFFEF4444) else TextPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = material.effectiveUnit,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0x150F172A))
                    )

                    // Min Qty
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MIN ALERT QTY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", material.effectiveMinQuantity).removeSuffix(".0"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = material.effectiveUnit,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: Add Stock / Use Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluidGlossButton(
                    onClick = onAddStock,
                    variant = FluidGlossButtonVariant.Success,
                    size = FluidGlossButtonSize.Small,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                ) {
                    Text("Add Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FluidGlossButton(
                    onClick = onUseStock,
                    enabled = material.effectiveQuantity > 0,
                    variant = FluidGlossButtonVariant.Destructive,
                    size = FluidGlossButtonSize.Small,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                ) {
                    Text("Use Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Footer: Supplier & Unit Rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (material.effectiveSupplier.isNotBlank()) "Supplier: ${material.effectiveSupplier}" else "No Supplier",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${CurrencyFormatter.formatFull(material.effectiveCost)} / ${material.effectiveUnit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

// ─── Add Material Dialog ──────────────────────────────────────────────────────

@Composable
fun AddMaterialDialog(
    projectId: String,
    siteId: String?,
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        category: String,
        initialQty: Double,
        minQty: Double,
        unit: String,
        supplier: String,
        cost: Double,
        logExpense: Boolean,
        totalCost: Double?,
        expenseDate: String,
        paymentStatus: String,
        invoiceNo: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var initialQtyStr by remember { mutableStateOf("") }
    var minQtyStr by remember { mutableStateOf("5") }
    var unit by remember { mutableStateOf("bags") }
    var supplier by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }

    var logExpense by remember { mutableStateOf(true) }
    var totalCostStr by remember { mutableStateOf("") }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var expenseDate by remember { mutableStateOf(todayDate) }
    var paymentStatus by remember { mutableStateOf("paid") }
    var invoiceNo by remember { mutableStateOf("") }

    // Auto calculate total cost when qty or unit cost changes
    LaunchedEffect(initialQtyStr, costStr) {
        val q = initialQtyStr.toDoubleOrNull() ?: 0.0
        val c = costStr.toDoubleOrNull() ?: 0.0
        if (q > 0 && c > 0) {
            totalCostStr = String.format(Locale.getDefault(), "%.2f", q * c)
        }
    }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Add New Item",
        subtitle = "Add a new material to track in your project inventory",
        icon = Icons.Default.Inventory,
        iconTint = BrandPrimary,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val initQty = initialQtyStr.toDoubleOrNull() ?: 0.0
                    val minQty = minQtyStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val totalCost = totalCostStr.toDoubleOrNull()
                    if (name.isBlank()) return@FluidGlossButton

                    onAdd(
                        name.trim(),
                        category.trim(),
                        initQty,
                        minQty,
                        unit.trim().ifBlank { "units" },
                        supplier.trim(),
                        cost,
                        logExpense,
                        totalCost,
                        expenseDate,
                        paymentStatus,
                        invoiceNo.trim().ifBlank { null }
                    )
                },
                variant = FluidGlossButtonVariant.Primary,
                enabled = name.isNotBlank() && initialQtyStr.isNotBlank()
            ) {
                Text("Add Item to Inventory", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Text("ITEM DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, letterSpacing = 1.sp)
        FluidGlossTextField(
            value = name,
            onValueChange = { name = it },
            label = "Item Name *",
            placeholder = "e.g. Maha PPC Cement, Steel 12mm",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FluidGlossTextField(
            value = category,
            onValueChange = { category = it },
            label = "Category",
            placeholder = "e.g. Cement, Steel, Structural",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("INVENTORY CONTROL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = initialQtyStr,
                onValueChange = { initialQtyStr = it },
                label = "Initial Stock *",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = minQtyStr,
                onValueChange = { minQtyStr = it },
                label = "Min Alert *",
                placeholder = "5",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = unit,
                onValueChange = { unit = it },
                label = "Unit *",
                placeholder = "bags, kg",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("PROCUREMENT & SUPPLIER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, letterSpacing = 1.sp)
        FluidGlossTextField(
            value = supplier,
            onValueChange = { supplier = it },
            label = "Supplier Name",
            placeholder = "e.g. Palanivel Steels",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FluidGlossTextField(
            value = costStr,
            onValueChange = { costStr = it },
            label = "Cost per Unit (₹)",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))
        // Expense Logging Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Record as Project Expense", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Auto-creates an expense record for this initial batch", fontSize = 10.sp, color = TextSecondary)
            }
            Switch(
                checked = logExpense,
                onCheckedChange = { logExpense = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandPrimary
                )
            )
        }

        AnimatedVisibility(visible = logExpense) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FluidGlossTextField(
                    value = totalCostStr,
                    onValueChange = { totalCostStr = it },
                    label = "Total Purchase Cost (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FluidGlossTextField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        label = "Purchase Date",
                        placeholder = "YYYY-MM-DD",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Payment Status
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payment Status", fontSize = 11.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.dp, Color(0x150F172A)), RoundedCornerShape(10.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                    .background(if (paymentStatus == "paid") BrandPrimary else Color.Transparent)
                                    .clickable { paymentStatus = "paid" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (paymentStatus == "paid") Color.White else TextSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                    .background(if (paymentStatus == "pending") Color(0xFFF59E0B) else Color.Transparent)
                                    .clickable { paymentStatus = "pending" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (paymentStatus == "pending") Color.White else TextSecondary)
                            }
                        }
                    }
                }

                FluidGlossTextField(
                    value = invoiceNo,
                    onValueChange = { invoiceNo = it },
                    label = "Bill / Invoice # (Optional)",
                    placeholder = "e.g. INV-2026-001",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── Stock Update Dialog (Add or Use) ─────────────────────────────────────────

@Composable
fun StockUpdateDialog(
    material: ProjectMaterialDto,
    isAdd: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        unitRate: Double?,
        updateActivePrice: Boolean,
        date: String,
        supplier: String?,
        purpose: String,
        logExpense: Boolean,
        restockAmount: Double?,
        paymentStatus: String,
        invoiceNo: String?
    ) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var unitRateStr by remember { mutableStateOf(if (material.effectiveCost > 0) material.effectiveCost.toString() else "") }
    var updateActivePrice by remember { mutableStateOf(true) }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(todayDate) }
    var supplier by remember { mutableStateOf(material.effectiveSupplier) }
    var purpose by remember { mutableStateOf(if (isAdd) "Shipment restock" else "Site consumption") }

    var logExpense by remember { mutableStateOf(isAdd) }
    var restockAmountStr by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("paid") }
    var invoiceNo by remember { mutableStateOf("") }

    // Auto-calculate restock cost
    LaunchedEffect(amountStr, unitRateStr) {
        if (isAdd) {
            val a = amountStr.toDoubleOrNull() ?: 0.0
            val r = unitRateStr.toDoubleOrNull() ?: material.effectiveCost
            if (a > 0 && r > 0) {
                restockAmountStr = String.format(Locale.getDefault(), "%.2f", a * r)
            }
        }
    }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = if (isAdd) "Add Stock: ${material.name}" else "Use Stock: ${material.name}",
        subtitle = if (isAdd) "Add new inventory to stock and log purchase rate" else "Record material consumption for site execution",
        icon = if (isAdd) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
        iconTint = if (isAdd) EmeraldSuccess else RoseDanger,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) return@FluidGlossButton
                    val rate = unitRateStr.toDoubleOrNull()
                    val restockAmt = restockAmountStr.toDoubleOrNull()
                    onConfirm(
                        amt,
                        rate,
                        updateActivePrice,
                        date,
                        supplier.trim().ifBlank { null },
                        purpose.trim(),
                        logExpense,
                        restockAmt,
                        paymentStatus,
                        invoiceNo.trim().ifBlank { null }
                    )
                },
                variant = if (isAdd) FluidGlossButtonVariant.Success else FluidGlossButtonVariant.Destructive,
                enabled = amountStr.isNotBlank() && purpose.isNotBlank()
            ) {
                Text(
                    text = if (isAdd) "Confirm Add Stock" else "Confirm Use Stock",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Quantity (${material.effectiveUnit}) *",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (isAdd) {
                FluidGlossTextField(
                    value = unitRateStr,
                    onValueChange = { unitRateStr = it },
                    label = "Unit Rate (₹)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = date,
                onValueChange = { date = it },
                label = if (isAdd) "Purchase Date" else "Usage Date",
                placeholder = "YYYY-MM-DD",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (isAdd) {
                FluidGlossTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = "Supplier",
                    placeholder = "Supplier name",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        FluidGlossTextField(
            value = purpose,
            onValueChange = { purpose = it },
            label = "Purpose / Reason *",
            placeholder = if (isAdd) "e.g. New delivery for slab" else "e.g. Ground floor columns casting",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isAdd) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Update active inventory price to this rate", fontSize = 11.sp, color = TextSecondary)
                Switch(
                    checked = updateActivePrice,
                    onCheckedChange = { updateActivePrice = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Record Purchase as Expense", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Log purchase total to project expenses", fontSize = 10.sp, color = TextSecondary)
                }
                Switch(
                    checked = logExpense,
                    onCheckedChange = { logExpense = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandPrimary
                    )
                )
            }

            AnimatedVisibility(visible = logExpense) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FluidGlossTextField(
                        value = restockAmountStr,
                        onValueChange = { restockAmountStr = it },
                        label = "Total Bill (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Payment Status toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.dp, Color(0x150F172A)), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                        .background(if (paymentStatus == "paid") BrandPrimary else Color.Transparent)
                                        .clickable { paymentStatus = "paid" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (paymentStatus == "paid") Color.White else TextSecondary)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                        .background(if (paymentStatus == "pending") Color(0xFFF59E0B) else Color.Transparent)
                                        .clickable { paymentStatus = "pending" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (paymentStatus == "pending") Color.White else TextSecondary)
                                }
                            }
                        }

                        FluidGlossTextField(
                            value = invoiceNo,
                            onValueChange = { invoiceNo = it },
                            label = "Bill / Inv #",
                            placeholder = "INV-102",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Edit Material Dialog ─────────────────────────────────────────────────────

@Composable
fun EditMaterialDialog(
    material: ProjectMaterialDto,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        currentStock: Double,
        minStock: Double,
        unit: String,
        supplier: String,
        cost: Double
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(material.name) }
    var category by remember { mutableStateOf(material.category ?: "") }
    var stockStr by remember { mutableStateOf(material.effectiveQuantity.toString()) }
    var minStockStr by remember { mutableStateOf(material.effectiveMinQuantity.toString()) }
    var unit by remember { mutableStateOf(material.effectiveUnit) }
    var supplier by remember { mutableStateOf(material.effectiveSupplier) }
    var costStr by remember { mutableStateOf(material.effectiveCost.toString()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Edit Material",
        subtitle = "Update specifications, thresholds, and unit costs",
        icon = Icons.Default.Edit,
        iconTint = BrandPrimary,
        confirmButton = {
            FluidGlossButton(
                onClick = {
                    val s = stockStr.toDoubleOrNull() ?: material.effectiveQuantity
                    val m = minStockStr.toDoubleOrNull() ?: material.effectiveMinQuantity
                    val c = costStr.toDoubleOrNull() ?: material.effectiveCost
                    onSave(name.trim(), category.trim(), s, m, unit.trim(), supplier.trim(), c)
                },
                variant = FluidGlossButtonVariant.Primary,
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            onValueChange = { name = it },
            label = "Name",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = category,
            onValueChange = { category = it },
            label = "Category",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = stockStr,
                onValueChange = { stockStr = it },
                label = "Stock",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = minStockStr,
                onValueChange = { minStockStr = it },
                label = "Min Stock",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = unit,
                onValueChange = { unit = it },
                label = "Unit",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidGlossTextField(
                value = supplier,
                onValueChange = { supplier = it },
                label = "Supplier",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FluidGlossTextField(
                value = costStr,
                onValueChange = { costStr = it },
                label = "Unit Cost (₹)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!showDeleteConfirm) {
            FluidGlossButton(
                onClick = { showDeleteConfirm = true },
                variant = FluidGlossButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            ) {
                Text("Delete Material", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluidGlossButton(
                    onClick = { showDeleteConfirm = false },
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 11.sp)
                }

                FluidGlossButton(
                    onClick = onDelete,
                    variant = FluidGlossButtonVariant.Destructive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Material History Dialog ──────────────────────────────────────────────────

@Composable
fun MaterialHistoryDialog(
    material: ProjectMaterialDto,
    viewModel: ProjectViewModel,
    onDismiss: () -> Unit
) {
    var logs by remember { mutableStateOf<List<MaterialLogDto>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }

    LaunchedEffect(material.id) {
        material.id?.let { matId ->
            viewModel.fetchMaterialLogs(matId) { fetchedLogs ->
                logs = fetchedLogs
                isLoadingLogs = false
            }
        } ?: run {
            isLoadingLogs = false
        }
    }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Stock History",
        subtitle = material.name,
        icon = Icons.Default.History,
        iconTint = BrandPrimary,
        dismissButton = {
            FluidGlossButton(
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    ) {
        // Summary Banner wrapped in GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACTIVE RATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyFormatter.formatFull(material.effectiveCost),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT STOCK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${material.effectiveQuantity} ${material.effectiveUnit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MIN LEVEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${material.effectiveMinQuantity} ${material.effectiveUnit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Logs List
        if (isLoadingLogs) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(32.dp))
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No stock logs found for this item.", fontSize = 12.sp, color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                logs.forEach { log ->
                    val isAdd = log.changeAmount > 0
                    val parsed = remember(log.purpose, isAdd) { parseLogPurpose(log.purpose, isAdd) }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isAdd) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, if (isAdd) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = if (isAdd) "Restock" else "Usage",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdd) Color(0xFF059669) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = parsed.displayNote,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (parsed.supplier != null || parsed.unitCost != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        parsed.supplier?.let { supp ->
                                            Text(
                                                text = "Supplier: $supp",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        parsed.unitCost?.let { cost ->
                                            Text(
                                                text = "@ ${CurrencyFormatter.formatFull(cost)}/unit",
                                                fontSize = 10.sp,
                                                color = BrandPrimary
                                            )
                                        }
                                    }
                                }

                                log.createdAt?.let { dateStr ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateStr.take(16).replace("T", " "),
                                        fontSize = 10.sp,
                                        color = TextSecondary.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Text(
                                text = "${if (isAdd) "+" else ""}${log.changeAmount} ${material.effectiveUnit}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdd) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ParsedLogPurpose(
    val displayNote: String,
    val supplier: String? = null,
    val unitCost: Double? = null,
    val totalCost: Double? = null
)

private fun parseLogPurpose(raw: String?, isAdd: Boolean): ParsedLogPurpose {
    if (raw.isNullOrBlank()) {
        return ParsedLogPurpose(displayNote = if (isAdd) "Stock replenishment" else "Site usage")
    }
    val trimmed = raw.trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            val json = org.json.JSONObject(trimmed)
            val note = json.optString("note").takeIf { it.isNotBlank() }
            val supplier = json.optString("supplier").takeIf { it.isNotBlank() }
            val unitCost = if (json.has("unitCost")) json.optDouble("unitCost").takeIf { !it.isNaN() } else null
            val totalCost = if (json.has("totalCost")) json.optDouble("totalCost").takeIf { !it.isNaN() } else null
            val action = json.optString("action")

            val title = note ?: when {
                !supplier.isNullOrBlank() -> "Purchase from $supplier"
                action == "purchase" -> "Stock purchase"
                else -> if (isAdd) "Stock replenishment" else "Site usage"
            }
            return ParsedLogPurpose(
                displayNote = title,
                supplier = supplier,
                unitCost = unitCost,
                totalCost = totalCost
            )
        } catch (_: Exception) {
            // fall through
        }
    }
    return ParsedLogPurpose(displayNote = raw)
}

@Composable
fun lightDialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = Color(0x1F0F172A),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = BrandPrimary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = BrandPrimary
)
