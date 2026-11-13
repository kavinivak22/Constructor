package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.constructorapp.data.model.ProjectEntity
import com.example.constructorapp.data.model.PurchaseOrderDto
import com.example.constructorapp.data.model.PurchaseOrderItemDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.util.CurrencyFormatter

@Composable
fun CreatePurchaseOrderDialog(
    initialProjectId: String? = null,
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSubmit: (PurchaseOrderDto) -> Unit
) {
    var selectedProjectId by remember {
        mutableStateOf(initialProjectId ?: projects.firstOrNull()?.id ?: "")
    }

    var supplierName by remember { mutableStateOf("") }
    var supplierContact by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val items = remember {
        mutableStateListOf(
            PurchaseOrderItemDto(
                materialName = "Ultratech Cement (OPC 53)",
                quantity = 100.0,
                unitPrice = 410.0,
                unit = "bags",
                totalPrice = 41000.0
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create Purchase Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Itemized supplier requisition",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateDivider)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Project selector
                    item {
                        Text("TARGET PROJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        var expanded by remember { mutableStateOf(false) }
                        val currentProject = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SlateBackground,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currentProject?.name ?: "Select Project", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandPrimary)
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(SlateSurface)
                            ) {
                                projects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = { Text(proj.name, color = TextPrimary) },
                                        onClick = {
                                            selectedProjectId = proj.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Supplier info
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = supplierName,
                                onValueChange = {
                                    supplierName = it
                                    errorMessage = null
                                },
                                label = { Text("Supplier / Vendor Name", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.2f),
                                colors = poFieldColors()
                            )
                            OutlinedTextField(
                                value = supplierContact,
                                onValueChange = { supplierContact = it },
                                label = { Text("Contact Phone", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = poFieldColors()
                            )
                        }
                    }

                    // Delivery Date & Notes
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = deliveryDate,
                                onValueChange = { deliveryDate = it },
                                label = { Text("Target Delivery (YYYY-MM-DD)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("2026-11-05", color = TextSecondary) },
                                colors = poFieldColors()
                            )
                            OutlinedTextField(
                                value = specialInstructions,
                                onValueChange = { specialInstructions = it },
                                label = { Text("Site Delivery Note", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = poFieldColors()
                            )
                        }
                    }

                    // PO Items Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PURCHASE ORDER ITEMS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                            FilledTonalButton(
                                onClick = {
                                    items.add(
                                        PurchaseOrderItemDto(
                                            materialName = "",
                                            quantity = 1.0,
                                            unitPrice = 0.0,
                                            unit = "units",
                                            totalPrice = 0.0
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SlateSurfaceSubtle,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item", fontSize = 11.sp)
                            }
                        }
                    }

                    itemsIndexed(items) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = item.materialName,
                                        onValueChange = { name ->
                                            items[index] = item.copy(materialName = name)
                                        },
                                        label = { Text("Material Name", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = poFieldColors()
                                    )
                                    IconButton(
                                        onClick = { if (items.size > 1) items.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseDanger, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item.quantity.toString(),
                                        onValueChange = { qStr ->
                                            val q = qStr.toDoubleOrNull() ?: item.quantity
                                            items[index] = item.copy(quantity = q, totalPrice = q * item.unitPrice)
                                        },
                                        label = { Text("Qty", fontSize = 10.sp) },
                                        modifier = Modifier.weight(0.9f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = poFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = item.unit,
                                        onValueChange = { u ->
                                            items[index] = item.copy(unit = u)
                                        },
                                        label = { Text("Unit", fontSize = 10.sp) },
                                        modifier = Modifier.weight(0.9f),
                                        colors = poFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = item.unitPrice.toString(),
                                        onValueChange = { pStr ->
                                            val p = pStr.toDoubleOrNull() ?: item.unitPrice
                                            items[index] = item.copy(unitPrice = p, totalPrice = item.quantity * p)
                                        },
                                        label = { Text("Unit Rate (₹)", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1.2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = poFieldColors()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Subtotal: ₹${(item.quantity * item.unitPrice).toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = RoseDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateDivider)

                // Total and Action Buttons
                val grandTotal = items.sumOf { it.quantity * it.unitPrice }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grand Total:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatFull(grandTotal),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (supplierName.isBlank()) {
                                    errorMessage = "Supplier name is required"
                                    return@Button
                                }
                                if (selectedProjectId.isBlank()) {
                                    errorMessage = "Please select a project"
                                    return@Button
                                }

                                val po = PurchaseOrderDto(
                                    projectId = selectedProjectId,
                                    supplierName = supplierName.trim(),
                                    supplierContact = supplierContact.ifBlank { null },
                                    deliveryDate = deliveryDate.ifBlank { null },
                                    specialInstructions = specialInstructions.ifBlank { null },
                                    status = "Draft",
                                    totalAmount = grandTotal,
                                    items = items.toList()
                                )
                                onSubmit(po)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Create PO")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun poFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = SlateBorder,
    focusedLabelColor = BrandPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = SlateBackground,
    unfocusedContainerColor = SlateBackground
)
