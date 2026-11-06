package com.example.constructorapp.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
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
import com.example.constructorapp.data.local.entity.ContractorEntity
import com.example.constructorapp.data.local.entity.ContractorLedgerEntryEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ContractorsUiState
import com.example.constructorapp.ui.viewmodel.ContractorsViewModel
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import com.example.constructorapp.util.toInrFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorAccountsScreen(
    initialContractorId: String? = null,
    viewModel: ContractorsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialContractorId) {
        if (!initialContractorId.isNullOrBlank()) {
            viewModel.selectContractor(initialContractorId)
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
                                text = "Contractor Accounts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Rate contracts & NMR daily wage ledgers",
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
                        IconButton(onClick = { viewModel.setAddContractorDialogOpen(true) }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contractor", tint = BrandPrimary)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2-Tile KPI Summary Strip (Rate vs NMR)
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Rate Account Tile (Purple accent)
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rate (Sq.Ft) Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Due: ${state.rateDue.toInrFull()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                                Text("Cleared: ${state.rateCleared.toInrFull()}", fontSize = 11.sp, color = EmeraldSuccess)
                            }
                        }

                        // NMR Account Tile (Teal/Amber accent)
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("NMR (Labor) Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2DD4BF))
                                    Icon(Icons.Default.Engineering, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Due: ${state.nmrDue.toInrFull()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                Text("Cleared: ${state.nmrCleared.toInrFull()}", fontSize = 11.sp, color = EmeraldSuccess)
                            }
                        }
                    }
                }

                // Search & Trade Filter Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search contractor by name or trade...", fontSize = 13.sp, color = TextSecondary) },
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

                        // Trade chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.availableTrades.forEach { trade ->
                                val isSelected = state.selectedTradeFilter.equals(trade, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTradeFilter(trade) },
                                    label = { Text(trade, fontSize = 11.sp) },
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
                }

                // Selected Contractor Statement Details OR Contractor Directory List
                val selected = state.selectedContractor
                if (selected != null) {
                    item {
                        ContractorDetailCard(
                            contractor = selected,
                            onDial = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${selected.phone}")
                                }
                                context.startActivity(intent)
                            },
                            onAddEntry = { viewModel.setAddLedgerEntryDialogOpen(true) }
                        )
                    }

                    // Statement sub-account tabs (Combined, Rate, NMR)
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = when (state.selectedLedgerTab) {
                                "rate" -> 1
                                "nmr" -> 2
                                else -> 0
                            },
                            containerColor = SlateSurface.copy(alpha = 0.7f),
                            contentColor = BrandPrimary
                        ) {
                            Tab(
                                selected = state.selectedLedgerTab == "combined",
                                onClick = { viewModel.selectLedgerTab("combined") },
                                text = { Text("Combined Statement", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = state.selectedLedgerTab == "rate",
                                onClick = { viewModel.selectLedgerTab("rate") },
                                text = { Text("Rate Account", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = state.selectedLedgerTab == "nmr",
                                onClick = { viewModel.selectLedgerTab("nmr") },
                                text = { Text("NMR Account", fontSize = 12.sp) }
                            )
                        }
                    }

                    val activeLedgerEntries = when (state.selectedLedgerTab) {
                        "rate" -> state.rateLedgerEntries
                        "nmr" -> state.nmrLedgerEntries
                        else -> state.allLedgerEntries
                    }

                    if (activeLedgerEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions recorded in this ledger", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(activeLedgerEntries, key = { it.id }) { entry ->
                            LedgerEntryRow(entry = entry)
                        }
                    }
                } else {
                    // Contractor Directory Cards
                    items(state.filteredContractors, key = { it.id }) { c ->
                        ContractorDirectoryCard(
                            contractor = c,
                            onClick = { viewModel.selectContractor(c.id) },
                            onDial = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${c.phone}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Add Contractor Dialog
        if (state.isAddContractorDialogOpen) {
            AddContractorDialog(
                onDismiss = { viewModel.setAddContractorDialogOpen(false) },
                onConfirm = { name, cat, contact, phone, email, acc, gst ->
                    viewModel.createContractor(name, cat, contact, phone, email, acc, gst)
                }
            )
        }

        // Add Ledger Entry Dialog
        if (state.isAddLedgerEntryDialogOpen && state.selectedContractor != null) {
            AddLedgerEntryDialog(
                contractorName = state.selectedContractor!!.name,
                onDismiss = { viewModel.setAddLedgerEntryDialogOpen(false) },
                onConfirm = { type, desc, amt, status, pClass, ref ->
                    viewModel.addLedgerEntry(
                        contractorId = state.selectedContractor!!.id,
                        entryType = type,
                        description = desc,
                        amount = amt,
                        status = status,
                        payoutClass = pClass,
                        referenceDetails = ref
                    )
                }
            )
        }
    }
}

@Composable
fun ContractorDetailCard(
    contractor: ContractorEntity,
    onDial: () -> Unit,
    onAddEntry: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contractor.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = (contractor.category ?: "Contractor").uppercase(),
                        fontSize = 11.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!contractor.contactPerson.isNullOrBlank()) {
                        Text(
                            text = "Contact: ${contractor.contactPerson}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDial,
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandPrimaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Dial", tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = onAddEntry,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!contractor.accountDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = SlateSurfaceSubtle.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bank: ${contractor.accountDetails}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContractorDirectoryCard(
    contractor: ContractorEntity,
    onClick: () -> Unit,
    onDial: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contractor.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = contractor.category ?: "Contractor",
                    fontSize = 11.sp,
                    color = BrandPrimary
                )
                Text(
                    text = "Phone: ${contractor.phone}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onDial,
                modifier = Modifier
                    .size(36.dp)
                    .background(BrandPrimaryContainer, CircleShape)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Dial", tint = BrandPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun LedgerEntryRow(entry: ContractorLedgerEntryEntity) {
    val isPaid = entry.status.equals("paid", ignoreCase = true)
    val isRate = entry.payoutClass.equals("rate", ignoreCase = true)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isRate) Color(0x268B5CF6) else Color(0x2614B8A6),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isRate) "RATE" else "NMR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRate) Color(0xFFA78BFA) else Color(0xFF2DD4BF),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Date: ${entry.transactionDate}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.amount.toInrFull(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) EmeraldSuccess else RoseDanger
                )
                Surface(
                    color = if (isPaid) EmeraldSuccessContainer else AmberWarningContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isPaid) "Settled" else "Pending Due",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaid) EmeraldSuccess else AmberWarning,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddContractorDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: String?,
        contactPerson: String?,
        phone: String,
        email: String?,
        accountDetails: String?,
        gst: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Masonry") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
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
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add New Contractor", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contractor / Agency Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Trade / Category (e.g. Masonry, Electrical)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Bank & Account Details") },
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
                            if (name.isBlank() || phone.isBlank()) {
                                error = "Name and Phone are required"
                                return@Button
                            }
                            onConfirm(name, category.takeIf { it.isNotBlank() }, contact.takeIf { it.isNotBlank() }, phone, email.takeIf { it.isNotBlank() }, bank.takeIf { it.isNotBlank() }, gst.takeIf { it.isNotBlank() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Contractor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddLedgerEntryDialog(
    contractorName: String,
    onDismiss: () -> Unit,
    onConfirm: (
        entryType: String,
        description: String,
        amount: Double,
        status: String,
        payoutClass: String,
        referenceDetails: String?
    ) -> Unit
) {
    var entryType by remember { mutableStateOf("work") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("pending") }
    var payoutClass by remember { mutableStateOf("rate") }
    var reference by remember { mutableStateOf("") }
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
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add Ledger Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Contractor: $contractorName", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                // Account classification selector
                Text("Ledger Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("rate" to "Rate Contract", "nmr" to "NMR Daily Labor").forEach { (pc, label) ->
                        FilterChip(
                            selected = payoutClass == pc,
                            onClick = { payoutClass = pc },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceSubtle,
                                labelColor = TextSecondary,
                                selectedContainerColor = if (pc == "rate") Color(0xFF8B5CF6) else Color(0xFF14B8A6),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. 500 Sq.Ft Plastering)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pending" to "Pending Due", "paid" to "Settled / Paid").forEach { (st, label) ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceSubtle,
                                labelColor = TextSecondary,
                                selectedContainerColor = if (st == "paid") EmeraldSuccess else AmberWarning,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

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
                            val amt = amountText.toDoubleOrNull()
                            if (description.isBlank() || amt == null || amt <= 0.0) {
                                error = "Description and valid amount are required"
                                return@Button
                            }
                            onConfirm(entryType, description, amt, status, payoutClass, reference.takeIf { it.isNotBlank() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for navigation wiring from ProjectViewModel
@Composable
fun ContractorAccountsScreen(
    initialContractorId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val contractorsViewModel: ContractorsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContractorsViewModel(database) as T
            }
        }
    )

    ContractorAccountsScreen(
        initialContractorId = initialContractorId,
        viewModel = contractorsViewModel,
        onBackClick = onBackClick
    )
}
