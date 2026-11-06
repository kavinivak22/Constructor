package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.example.constructorapp.data.local.entity.InventoryAssetEntity
import com.example.constructorapp.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transfer Stock / Asset",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Direction Route Selector
                Text("Transfer Direction", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(14.dp))

                // Asset Type Selector
                Text("Asset Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(14.dp))

                // Select Existing Asset or Enter Custom
                Text("Select Inventory Item", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                var assetDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = assetDropdownExpanded,
                    onExpandedChange = { assetDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAsset?.let { "${it.name} (${it.currentStock} ${it.unit} available)" } ?: customItemName.ifBlank { "Select or Enter item" },
                        onValueChange = { customItemName = it },
                        readOnly = selectedAsset != null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = assetDropdownExpanded,
                        onDismissRequest = { assetDropdownExpanded = false }
                    ) {
                        filteredAssets.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text("${asset.name} - ${asset.currentStock} ${asset.unit}") },
                                onClick = {
                                    selectedAsset = asset
                                    customItemName = asset.name
                                    unitText = asset.unit
                                    assetDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Custom / Unlisted item") },
                            onClick = {
                                selectedAsset = null
                                customItemName = ""
                                assetDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity and Unit Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes / Rationale
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Transfer Reason / Voucher Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = RoseDanger, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantityText.toDoubleOrNull()
                            if (qty == null || qty <= 0.0) {
                                errorMessage = "Please enter a valid positive quantity"
                                return@Button
                            }
                            val itemName = selectedAsset?.name ?: customItemName
                            if (itemName.isBlank()) {
                                errorMessage = "Please select or name the asset"
                                return@Button
                            }
                            if (selectedAsset != null && qty > selectedAsset!!.currentStock) {
                                errorMessage = "Quantity exceeds current stock (${selectedAsset!!.currentStock})"
                                return@Button
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
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
