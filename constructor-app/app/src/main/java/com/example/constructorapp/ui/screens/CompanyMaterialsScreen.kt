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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.entity.CompanyMaterialPriceEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.MaterialsUiState
import com.example.constructorapp.ui.viewmodel.MaterialsViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyMaterialsScreen(
    viewModel: MaterialsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(state.companyPrices) {
        listOf("All") + state.companyPrices.map { it.category }.distinct()
    }

    val filteredPrices = remember(state.companyPrices, selectedCategory, state.searchQuery) {
        state.companyPrices.filter { price ->
            val matchesCategory = selectedCategory == "All" || price.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = state.searchQuery.isBlank() ||
                price.materialName.contains(state.searchQuery, ignoreCase = true) ||
                (price.materialNameTa?.contains(state.searchQuery, ignoreCase = true) == true) ||
                (price.supplierName?.contains(state.searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesQuery
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
                                text = "Company Materials Catalog",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Standard master price rates & supplier directory",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateSurface.copy(alpha = 0.85f),
                        titleContentColor = TextPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.setAddCatalogDialogOpen(true) },
                    containerColor = BrandPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Material")
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search materials by name or supplier...", fontSize = 13.sp, color = TextSecondary) },
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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

                // Category Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceSubtle,
                                labelColor = TextSecondary,
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Materials List
                if (filteredPrices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No materials in catalog", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.setAddCatalogDialogOpen(true) },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Text("+ Add First Material", color = BrandPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPrices, key = { it.id }) { price ->
                            CompanyMaterialPriceCard(
                                price = price,
                                onDelete = { viewModel.deleteCompanyPrice(price.id) }
                            )
                        }
                    }
                }
            }
        }

        // Add Catalog Dialog
        if (state.isAddCatalogDialogOpen) {
            AddCompanyMaterialDialog(
                onDismiss = { viewModel.setAddCatalogDialogOpen(false) },
                onConfirm = { name, nameTa, cat, unit, rate, supplier ->
                    viewModel.addCompanyPrice(
                        materialName = name,
                        materialNameTa = nameTa,
                        category = cat,
                        unit = unit,
                        standardRate = rate,
                        supplierName = supplier
                    )
                }
            )
        }
    }
}

@Composable
fun CompanyMaterialPriceCard(
    price: CompanyMaterialPriceEntity,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = price.materialName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (!price.materialNameTa.isNullOrBlank()) {
                    Text(
                        text = price.materialNameTa,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = SlateSurfaceSubtle,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = price.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (!price.supplierName.isNullOrBlank()) {
                        Text(
                            text = "Supplier: ${price.supplierName}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price.standardRate.toInrFull(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
                Text(
                    text = "per ${price.unit}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddCompanyMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        nameTa: String?,
        category: String,
        unit: String,
        standardRate: Double,
        supplierName: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameTa by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("material") }
    var unit by remember { mutableStateOf("bag") }
    var rateText by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            color = SlateSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Catalog Material", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name (e.g. OPC 53 Grade Cement)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameTa,
                    onValueChange = { nameTa = it },
                    label = { Text("Tamil Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Standard Rate (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Preferred Supplier (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = RoseDanger, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rate = rateText.toDoubleOrNull()
                            if (name.isBlank()) {
                                error = "Please enter material name"
                                return@Button
                            }
                            if (rate == null || rate <= 0.0) {
                                error = "Please enter valid rate"
                                return@Button
                            }
                            onConfirm(name, nameTa.takeIf { it.isNotBlank() }, category, unit, rate, supplier.takeIf { it.isNotBlank() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Catalog", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun CompanyMaterialsScreen(
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

    CompanyMaterialsScreen(
        viewModel = materialsViewModel,
        onBackClick = onBackClick
    )
}
