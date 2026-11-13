package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
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
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossExposedDropdown
import com.example.constructorapp.ui.components.FluidGlossTextField

@Composable
fun StockTransferDialog(
    availableAssets: List<InventoryAssetEntity>,
    onDismissRequest: () -> Unit,
    onConfirmTransfer: (
        materialId: String,
        sourceType: String,
        destinationType: String,
        fromSiteId: String,
        toSiteId: String,
        quantity: Double,
        unit: String,
        itemName: String,
        category: String,
        notes: String?
    ) -> Unit
) {
    var direction by remember { mutableStateOf("wh_to_proj") } // wh_to_proj | proj_to_wh | proj_to_proj
    var assetType by remember { mutableStateOf("material") } // material | tool | machinery
    var selectedAsset by remember { mutableStateOf<InventoryAssetEntity?>(null) }
    var customItemName by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var unitText by remember { mutableStateOf("Units") }
    var fromSiteId by remember { mutableStateOf("Central Warehouse") }
    var toSiteId by remember { mutableStateOf("Project Site Alpha") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredAssets = remember(availableAssets, assetType) {
        availableAssets.filter { it.assetType.equals(assetType, ignoreCase = true) }
    }

    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null) {
            unitText = selectedAsset?.unit ?: "Units"
        }
    }

    FluidGlossDialog(
        onDismissRequest = onDismissRequest,
        title = "Transfer Stock / Asset",
        icon = Icons.Default.LocalShipping,
        confirmButton = {
            FluidGlossButton(
                text = "Transfer",
                onClick = {
                    val qty = quantityText.toDoubleOrNull()
                    if (qty == null || qty <= 0.0) {
                        errorMessage = "Please enter a valid positive quantity"
                        return@FluidGlossButton
                    }
                    val itemName = selectedAsset?.name ?: customItemName
                    if (itemName.isBlank()) {
                        errorMessage = "Please select or name the asset"
                        return@FluidGlossButton
                    }
                    if (selectedAsset != null && qty > selectedAsset!!.currentStock) {
                        errorMessage = "Quantity exceeds current stock (${selectedAsset!!.currentStock})"
                        return@FluidGlossButton
                    }

                    val (srcType, destType) = when (direction) {
                        "wh_to_proj" -> "warehouse" to "project"
                        "proj_to_wh" -> "project" to "warehouse"
                        else -> "project" to "project"
                    }

                    onConfirmTransfer(
                        selectedAsset?.id ?: "",
                        srcType,
                        destType,
                        fromSiteId,
                        toSiteId,
                        qty,
                        unitText,
                        itemName,
                        selectedAsset?.category ?: "material",
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Direction Route Selector
            Text("Transfer Direction", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val routes = listOf(
                    "wh_to_proj" to "🏢 Yard ➔ 🏗️ Site",
                    "proj_to_wh" to "🏗️ Site ➔ 🏢 Yard",
                    "proj_to_proj" to "🏗️ Site ➔ 🏗️ Site"
                )
                routes.forEach { (key, label) ->
                    val isSelected = direction == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandPrimaryContainer else SlateSurfaceSubtle)
                            .border(1.dp, if (isSelected) BrandPrimary else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                direction = key
                                when (key) {
                                    "wh_to_proj" -> {
                                        fromSiteId = "Central Warehouse"
                                        toSiteId = "Project Site Alpha"
                                    }
                                    "proj_to_wh" -> {
                                        fromSiteId = "Project Site Alpha"
                                        toSiteId = "Central Warehouse"
                                    }
                                    "proj_to_proj" -> {
                                        fromSiteId = "Project Site Alpha"
                                        toSiteId = "Project Site Beta"
                                    }
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandPrimary else TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Asset Type Selector
            Text("Asset Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("material" to "Materials", "tool" to "Tools", "machinery" to "Machinery").forEach { (type, label) ->
                    val isSel = assetType == type
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            assetType = type
                            selectedAsset = null
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurfaceSubtle,
                            labelColor = TextSecondary,
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Select Existing Asset or Enter Custom using FluidGlossExposedDropdown
            Text("Select Inventory Item", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            val dropdownItems = remember(filteredAssets) {
                filteredAssets + listOf<InventoryAssetEntity?>(null)
            }
            FluidGlossExposedDropdown(
                items = dropdownItems,
                selectedItem = selectedAsset,
                onItemSelected = { asset ->
                    selectedAsset = asset
                    if (asset != null) {
                        customItemName = asset.name
                        unitText = asset.unit
                    } else {
                        customItemName = ""
                    }
                },
                itemLabel = { asset ->
                    asset?.let { "${it.name} (${it.currentStock} ${it.unit} available)" } ?: "+ Custom / Unlisted item"
                },
                label = "Inventory Asset",
                placeholder = "Select inventory asset...",
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedAsset == null) {
                FluidGlossTextField(
                    value = customItemName,
                    onValueChange = { customItemName = it },
                    label = "Custom Item Name",
                    placeholder = "e.g. Scaffolding Clamps",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quantity and Unit Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluidGlossTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = "Quantity",
                    placeholder = "0",
                    modifier = Modifier.weight(1.5f)
                )

                FluidGlossTextField(
                    value = unitText,
                    onValueChange = { unitText = it },
                    label = "Unit",
                    placeholder = "Units",
                    modifier = Modifier.weight(1f)
                )
            }

            // Notes / Rationale
            FluidGlossTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Transfer Reason / Voucher Notes",
                placeholder = "Voucher reference, transit details",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = RoseDanger, fontSize = 12.sp)
            }
        }
    }
}
