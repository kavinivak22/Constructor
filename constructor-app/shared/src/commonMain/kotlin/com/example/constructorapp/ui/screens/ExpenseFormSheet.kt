package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.constructorapp.data.model.ExpenseEntity
import com.example.constructorapp.data.model.ProjectEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    initialProjectId: String? = null,
    expenseToEdit: ExpenseEntity? = null,
    projects: List<ProjectEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (
        id: String?,
        projectId: String?,
        amount: Double,
        category: String,
        description: String,
        expenseDate: String,
        paymentMode: String,
        paymentStatus: String,
        notes: String?,
        receiver: String?,
        localReceiptUri: String?
    ) -> Unit
) {
    var selectedProjectId by remember(expenseToEdit, initialProjectId) {
        mutableStateOf(expenseToEdit?.projectId ?: initialProjectId)
    }
    var amountText by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var selectedCategory by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.category ?: "materials")
    }
    var description by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.description ?: "")
    }
    var expenseDate by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.expenseDate?.takeIf { it.isNotBlank() } ?: "2026-11-13")
    }
    var paymentMode by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.paymentMode ?: "cash")
    }
    var paymentStatus by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.paymentStatus ?: "paid")
    }
    var notes by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.notes ?: "")
    }
    var receiver by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.receiver ?: "")
    }
    var selectedImageUri by remember { mutableStateOf<String?>(expenseToEdit?.receiptUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = expenseToEdit != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlassCardSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isEditing) "Edit Expense" else "Log Project Expense",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEditing) "Update cash outflow details" else "Record site expenditure, bills & payouts",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Project Selector (if projects available)
            if (projects.isNotEmpty()) {
                Text("Select Project Site", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    projects.forEach { proj ->
                        val isSelected = selectedProjectId == proj.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedProjectId = proj.id },
                            label = { Text(proj.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceSubtle,
                                labelColor = TextSecondary,
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Amount Input with INR prefix
            FluidGlossTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (₹)",
                placeholder = "0.00",
                leadingIcon = {
                    Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess, modifier = Modifier.padding(start = 12.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Selector
            Text("Expense Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "materials" to "Materials",
                    "labor" to "Labor",
                    "equipment" to "Equipment",
                    "permits" to "Permits",
                    "subcontractor" to "Subcontractor",
                    "other" to "Other"
                ).forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategory.equals(catKey, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catKey },
                        label = { Text(catLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurfaceSubtle,
                            labelColor = TextSecondary,
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            FluidGlossTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description (e.g. 50 bags sand delivery)",
                placeholder = "Enter expense description",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date and Payment Mode Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluidGlossTextField(
                    value = expenseDate,
                    onValueChange = { expenseDate = it },
                    label = "Expense Date",
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )

                FluidGlossTextField(
                    value = paymentMode,
                    onValueChange = { paymentMode = it },
                    label = "Mode (cash/upi/bank)",
                    placeholder = "Mode",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Receiver / Vendor Name and Payment Status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluidGlossTextField(
                    value = receiver,
                    onValueChange = { receiver = it },
                    label = "Paid To / Vendor Name",
                    placeholder = "Vendor",
                    modifier = Modifier.weight(1.3f)
                )

                FluidGlossTextField(
                    value = paymentStatus,
                    onValueChange = { paymentStatus = it },
                    label = "Status (paid/pending)",
                    placeholder = "Status",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / References
            FluidGlossTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes & Reference Details (Optional)",
                placeholder = "Optional notes",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Receipt Photo Attachment
            Text("Receipt / Bill Attachment", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { selectedImageUri = "receipt_attached" }
                    .background(SlateSurfaceSubtle.copy(alpha = 0.5f))
                    .padding(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedImageUri != null || expenseToEdit?.receiptUrl != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = if (selectedImageUri != null || expenseToEdit?.receiptUrl != null) EmeraldSuccess else BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedImageUri != null) "Receipt Photo Selected"
                            else if (expenseToEdit?.receiptUrl != null) "Receipt Attached on File"
                            else "Attach Bill / Receipt Photo",
                            fontSize = 12.sp,
                            color = if (selectedImageUri != null || expenseToEdit?.receiptUrl != null) EmeraldSuccess else TextPrimary
                        )
                    }

                    if (selectedImageUri != null) {
                        IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = RoseDanger, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            FluidGlossButton(
                text = if (isEditing) "Update Expense" else "Record Expense",
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid amount"
                        return@FluidGlossButton
                    }
                    if (description.isBlank()) {
                        errorMessage = "Please provide an expense description"
                        return@FluidGlossButton
                    }

                    onSubmit(
                        expenseToEdit?.id,
                        selectedProjectId,
                        amount,
                        selectedCategory,
                        description,
                        expenseDate,
                        paymentMode,
                        paymentStatus,
                        notes.takeIf { it.isNotBlank() },
                        receiver.takeIf { it.isNotBlank() },
                        selectedImageUri
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                variant = FluidGlossButtonVariant.Primary
            )
        }
    }
}
