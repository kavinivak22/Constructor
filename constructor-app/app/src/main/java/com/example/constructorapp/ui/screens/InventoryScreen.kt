package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.InventoryUiState
import com.example.constructorapp.ui.viewmodel.InventoryViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    initialSiteId: String? = null,
    viewModel: InventoryViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBlobBackground()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Central Warehouse",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                if (state.lowStockCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = RoseDangerContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${state.lowStockCount} Low",
                                            color = RoseDanger,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Company yard, tool shed & machinery fleet",
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
                        IconButton(onClick = { viewModel.setTransferDialogOpen(true) }) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Transfer Stock",
                                tint = BrandPrimary
                            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 3 Classification Tabs (Materials, Tools, Machinery)
                PrimaryTabRow(
                    selectedTabIndex = when (state.activeTab) {
                        "tool" -> 1
                        "machinery" -> 2
                        else -> 0
                    },
                    containerColor = SlateSurface.copy(alpha = 0.7f),
                    contentColor = BrandPrimary
                ) {
                    Tab(
                        selected = state.activeTab == "material",
                        onClick = { viewModel.selectTab("material") },
                        text = { Text("Materials Stock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = state.activeTab == "tool",
                        onClick = { viewModel.selectTab("tool") },
                        text = { Text("Tools & Equipment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = state.activeTab == "machinery",
                        onClick = { viewModel.selectTab("machinery") },
                        text = { Text("Heavy Machinery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Search & Filter Bar
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search ${state.activeTab}s...", fontSize = 13.sp, color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SlateSurface.copy(alpha = 0.6f),
                            unfocusedContainerColor = SlateSurface.copy(alpha = 0.6f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Horizontal Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableCategories.forEach { category ->
                            val isSelected = state.categoryFilter.equals(category, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCategoryFilter(category) },
                                label = { Text(category, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SlateSurfaceSubtle,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Inventory Assets List
                if (state.filteredAssets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No ${state.activeTab} items found", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.setTransferDialogOpen(true) },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Text("Transfer Asset into Yard", color = BrandPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredAssets, key = { it.id }) { asset ->
                            InventoryAssetCard(asset = asset, onTransferClick = { viewModel.setTransferDialogOpen(true) })
                        }
                    }
                }
            }
        }

        // Stock Transfer Dialog
        if (state.isTransferDialogOpen) {
            StockTransferDialog(
                availableAssets = state.materials + state.tools + state.machinery,
                onDismissRequest = { viewModel.setTransferDialogOpen(false) },
                onConfirmTransfer = { materialId, srcType, destType, fromSite, toSite, qty, unit, itemName, category, notes ->
                    viewModel.executeStockTransfer(
                        materialId = materialId,
                        sourceType = srcType,
                        destinationType = destType,
                        fromSiteId = fromSite,
                        toSiteId = toSite,
                        quantity = qty,
                        unit = unit,
                        itemName = itemName,
                        category = category,
                        notes = notes
                    )
                }
            )
        }
    }
}

@Composable
fun InventoryAssetCard(
    asset: InventoryAssetEntity,
    onTransferClick: () -> Unit
) {
    val level = InventoryViewModel.getStockStatusLevel(asset.currentStock, asset.minStockLevel)
    val (badgeBg, badgeText) = when (level) {
        "Low Stock" -> RoseDangerContainer to RoseDanger
        "Medium Stock" -> AmberWarningContainer to AmberWarning
        else -> EmeraldSuccessContainer to EmeraldSuccess
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, Category & Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = asset.category.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = BrandPrimary
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeText.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = level,
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurfaceSubtle.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Current Stock", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${asset.currentStock} ${asset.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column {
                    Text("Min Threshold", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${asset.minStockLevel} ${asset.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                Column {
                    Text("Unit Cost", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = asset.unitCost.toInrFull(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }

            if (!asset.supplierName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Supplier: ${asset.supplierName}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun InventoryScreen(
    initialSiteId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InventoryViewModel(database) as T
            }
        }
    )

    InventoryScreen(
        initialSiteId = initialSiteId,
        viewModel = inventoryViewModel,
        onBackClick = onBackClick
    )
}
