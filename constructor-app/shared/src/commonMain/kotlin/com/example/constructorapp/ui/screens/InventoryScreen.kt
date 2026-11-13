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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.toInrFull

private val defaultInventoryAssets = listOf(
    InventoryAssetEntity(
        id = "inv-1",
        name = "OPC 53 Cement Bags",
        category = "Cement",
        assetType = "material",
        unit = "Bags",
        currentStock = 180.0,
        minStockLevel = 100.0,
        unitCost = 395.0,
        supplierName = "Dalmia Agencies"
    ),
    InventoryAssetEntity(
        id = "inv-2",
        name = "Fe 550D TMT Rebar (12mm)",
        category = "Steel",
        assetType = "material",
        unit = "Kg",
        currentStock = 450.0,
        minStockLevel = 500.0,
        unitCost = 68.0,
        supplierName = "Tata Tiscon"
    ),
    InventoryAssetEntity(
        id = "inv-3",
        name = "Total Station Leica TS07",
        category = "Surveying",
        assetType = "tool",
        unit = "Unit",
        currentStock = 2.0,
        minStockLevel = 1.0,
        unitCost = 450000.0,
        supplierName = "Leica Geosystems"
    ),
    InventoryAssetEntity(
        id = "inv-4",
        name = "Concrete Needle Vibrator (2 HP)",
        category = "Compaction",
        assetType = "tool",
        unit = "Unit",
        currentStock = 4.0,
        minStockLevel = 2.0,
        unitCost = 14500.0,
        supplierName = "Jaypee Tools"
    ),
    InventoryAssetEntity(
        id = "inv-5",
        name = "JCB 3DX EcoXcellence Backhoe",
        category = "Earthmoving",
        assetType = "machinery",
        unit = "Unit",
        currentStock = 1.0,
        minStockLevel = 1.0,
        unitCost = 3200000.0,
        supplierName = "JCB India"
    ),
    InventoryAssetEntity(
        id = "inv-6",
        name = "Mini Transit Concrete Mixer (10/7)",
        category = "Batching",
        assetType = "machinery",
        unit = "Unit",
        currentStock = 3.0,
        minStockLevel = 2.0,
        unitCost = 165000.0,
        supplierName = "Universal Construction Equipment"
    )
)

private fun getStockStatusLevel(currentStock: Double, minStockLevel: Double): String {
    return when {
        currentStock <= minStockLevel -> "Low Stock"
        currentStock <= minStockLevel * 1.5 -> "Medium Stock"
        else -> "Normal Stock"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    siteId: String? = null,
    onBackClick: () -> Unit = {}
) {
    var assets by remember { mutableStateOf(defaultInventoryAssets) }
    var activeTab by remember { mutableStateOf("material") }
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All") }
    var isTransferDialogOpen by remember { mutableStateOf(false) }

    val tabAssets = remember(assets, activeTab) {
        assets.filter { it.assetType.equals(activeTab, ignoreCase = true) }
    }

    val availableCategories = remember(tabAssets) {
        listOf("All") + tabAssets.map { it.category }.distinct()
    }

    val filteredAssets = remember(tabAssets, searchQuery, categoryFilter) {
        tabAssets.filter { asset ->
            val matchQuery = searchQuery.isBlank() ||
                asset.name.contains(searchQuery, ignoreCase = true) ||
                asset.category.contains(searchQuery, ignoreCase = true) ||
                (asset.supplierName?.contains(searchQuery, ignoreCase = true) == true)
            val matchCategory = categoryFilter == "All" || asset.category.equals(categoryFilter, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    val lowStockCount = remember(assets) {
        assets.count { it.currentStock <= it.minStockLevel }
    }

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
                                if (lowStockCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = RoseDangerContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$lowStockCount Low",
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
                        IconButton(onClick = { isTransferDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Transfer Stock",
                                tint = BrandPrimary
                            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 3 Classification Tabs (Materials, Tools, Machinery)
                PrimaryTabRow(
                    selectedTabIndex = when (activeTab) {
                        "tool" -> 1
                        "machinery" -> 2
                        else -> 0
                    },
                    containerColor = Color.Transparent,
                    contentColor = BrandPrimary
                ) {
                    Tab(
                        selected = activeTab == "material",
                        onClick = {
                            activeTab = "material"
                            categoryFilter = "All"
                        },
                        text = { Text("Materials Stock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == "tool",
                        onClick = {
                            activeTab = "tool"
                            categoryFilter = "All"
                        },
                        text = { Text("Tools & Equipment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == "machinery",
                        onClick = {
                            activeTab = "machinery"
                            categoryFilter = "All"
                        },
                        text = { Text("Heavy Machinery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Search & Filter Bar
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    FluidGlossTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search ${activeTab}s...",
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        availableCategories.forEach { category ->
                            val isSelected = categoryFilter.equals(category, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { categoryFilter = category },
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
                if (filteredAssets.isEmpty()) {
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
                            Text("No $activeTab items found", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            FluidGlossButton(
                                onClick = { isTransferDialogOpen = true },
                                variant = FluidGlossButtonVariant.Outlined,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                }
                            ) {
                                Text("Transfer Asset into Yard", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAssets, key = { it.id }) { asset ->
                            InventoryAssetCard(asset = asset, onTransferClick = { isTransferDialogOpen = true })
                        }
                    }
                }
            }
        }

        // Stock Transfer Dialog
        if (isTransferDialogOpen) {
            StockTransferDialog(
                availableAssets = assets,
                onDismissRequest = { isTransferDialogOpen = false },
                onConfirmTransfer = { materialId, srcType, destType, fromSite, toSite, qty, unit, itemName, category, notes ->
                    // Perform in-memory stock update
                    assets = assets.map { item ->
                        if (item.id == materialId) {
                            val newStock = (item.currentStock - qty).coerceAtLeast(0.0)
                            item.copy(currentStock = newStock)
                        } else item
                    }
                    isTransferDialogOpen = false
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
    val level = getStockStatusLevel(asset.currentStock, asset.minStockLevel)
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
                    .background(Color(0x80FFFFFF))
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
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
