package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.DialogProperties
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.PurchaseOrderDto
import com.example.constructorapp.data.remote.PurchaseOrderItemDto
import com.example.constructorapp.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseOrderDialog(
    projects: List<ProjectDto>,
    initialProjectId: String?,
    onDismiss: () -> Unit,
    onSubmit: (PurchaseOrderDto) -> Unit
) {
    val context = LocalContext.current
    var selectedProjectId by remember {
        mutableStateOf(initialProjectId ?: projects.firstOrNull()?.id ?: "")
    }

    var supplierName by remember { mutableStateOf("") }
    var supplierContact by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }

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
                                onValueChange = { supplierName = it },
                                label = { Text("Supplier / Vendor Name", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.2f),
                                colors = calculatorFieldColors()
                            )
                            OutlinedTextField(
                                value = supplierContact,
                                onValueChange = { supplierContact = it },
                                label = { Text("Contact Phone", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = calculatorFieldColors()
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
                                colors = calculatorFieldColors()
                            )
                            OutlinedTextField(
                                value = specialInstructions,
                                onValueChange = { specialInstructions = it },
                                label = { Text("Site Delivery Note", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = calculatorFieldColors()
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
                                        colors = calculatorFieldColors()
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
                                        colors = calculatorFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = item.unit,
                                        onValueChange = { u ->
                                            items[index] = item.copy(unit = u)
                                        },
                                        label = { Text("Unit", fontSize = 10.sp) },
                                        modifier = Modifier.weight(0.9f),
                                        colors = calculatorFieldColors()
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
                                        colors = calculatorFieldColors()
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateDivider)

                // Total and Action Buttons
                val grandTotal = items.sumOf { it.quantity * it.unitPrice }
                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grand Total:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = currencyFormatter.format(grandTotal),
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
                                    Toast.makeText(context, "Supplier name is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedProjectId.isBlank()) {
                                    Toast.makeText(context, "Please select a project", Toast.LENGTH_SHORT).show()
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
