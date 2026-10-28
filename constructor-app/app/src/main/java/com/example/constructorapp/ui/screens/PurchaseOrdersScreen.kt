package com.example.constructorapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.constructorapp.data.remote.PurchaseOrderDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrdersScreen(
    initialProjectId: String?,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    var selectedProjectId by remember { mutableStateOf(initialProjectId) }
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()

    var statusFilter by remember { mutableStateOf("All") }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (projects.isEmpty()) {
            viewModel.loadProjects()
        }
        viewModel.loadPurchaseOrders(selectedProjectId)
    }

    LaunchedEffect(selectedProjectId) {
        viewModel.loadPurchaseOrders(selectedProjectId)
    }

    val filteredOrders = remember(purchaseOrders, statusFilter) {
        if (statusFilter == "All") purchaseOrders
        else purchaseOrders.filter { it.status.equals(statusFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Purchase Orders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (selectedProjectId != null) {
                                projects.find { it.id == selectedProjectId }?.name ?: "Selected Site"
                            } else "All Construction Projects",
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
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    var projMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { projMenuExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Project", tint = BrandPrimary)
                    }
                    DropdownMenu(
                        expanded = projMenuExpanded,
                        onDismissRequest = { projMenuExpanded = false },
                        modifier = Modifier.background(SlateSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Projects", color = TextPrimary, fontWeight = if (selectedProjectId == null) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedProjectId = null
                                projMenuExpanded = false
                            }
                        )
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, color = TextPrimary, fontWeight = if (selectedProjectId == p.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedProjectId = p.id
                                    projMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreateDialogOpen = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Purchase Order")
            }
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Draft", "Approved", "Fulfilled").forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Purchase Orders Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to create your first procurement order.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredOrders) { po ->
                        PurchaseOrderCard(
                            po = po,
                            projectName = projects.find { it.id == po.projectId }?.name ?: "Site Order",
                            onApprove = { poId ->
                                viewModel.approvePurchaseOrder(poId, selectedProjectId)
                                Toast.makeText(context, "Purchase order approved!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (isCreateDialogOpen) {
        CreatePurchaseOrderDialog(
            projects = projects,
            initialProjectId = selectedProjectId,
            onDismiss = { isCreateDialogOpen = false },
            onSubmit = { newPo ->
                viewModel.createPurchaseOrder(newPo) {
                    isCreateDialogOpen = false
                    Toast.makeText(context, "Purchase Order created successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun PurchaseOrderCard(
    po: PurchaseOrderDto,
    projectName: String,
    onApprove: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val (statusBg, statusFg) = when (po.status.lowercase()) {
        "approved" -> EmeraldSuccess.copy(alpha = 0.2f) to EmeraldSuccess
        "fulfilled" -> SkyInfo.copy(alpha = 0.2f) to SkyInfo
        "cancelled" -> RoseDanger.copy(alpha = 0.2f) to RoseDanger
        else -> AmberWarning.copy(alpha = 0.2f) to AmberWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Supplier & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = po.supplierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = projectName,
                        fontSize = 11.sp,
                        color = SkyInfo
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = po.status.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = statusFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Date & Contact Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!po.deliveryDate.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delivery: ${po.deliveryDate}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (!po.supplierContact.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateSurfaceSubtle)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${po.supplierContact}"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = po.supplierContact,
                            fontSize = 11.sp,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!po.specialInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${po.specialInstructions}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SlateDivider)

            // Items Count & Expand Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${po.items.size} Material Items",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = currencyFormatter.format(po.totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EmeraldSuccess
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    po.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.materialName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${item.quantity} ${item.unit} @ ₹${item.unitPrice.toInt()}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "₹${item.totalPrice.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Action row for Draft POs
            if (po.status.equals("Draft", ignoreCase = true) && po.id != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onApprove(po.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve Purchase Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
