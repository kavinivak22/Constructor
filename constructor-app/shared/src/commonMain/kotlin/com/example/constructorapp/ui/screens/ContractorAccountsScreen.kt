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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ContractorEntity
import com.example.constructorapp.data.model.ContractorLedgerEntryEntity
import com.example.constructorapp.data.model.ContractorsUiState
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.AmbientBlobBackground
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonSize
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.toInrFull

private val sampleContractorsData = listOf(
    ContractorEntity(
        id = "c1",
        name = "Murugan Masonry Works",
        phone = "+91 98421 11223",
        category = "Masonry",
        contactPerson = "Murugan",
        payoutType = "Rate",
        defaultRate = 18.5,
        accountDetails = "HDFC Bank • AC: 50100234123 • IFSC: HDFC0001234"
    ),
    ContractorEntity(
        id = "c2",
        name = "Selvam Bar Bending Team",
        phone = "+91 94432 44556",
        category = "Reinforcement",
        contactPerson = "Selvam",
        payoutType = "NMR",
        defaultRate = 850.0,
        accountDetails = "SBI • AC: 31234567890 • IFSC: SBIN0004567"
    ),
    ContractorEntity(
        id = "c3",
        name = "Karthik Electricals",
        phone = "+91 97890 88990",
        category = "Electrical",
        contactPerson = "Karthik",
        payoutType = "Rate",
        defaultRate = 24.0,
        accountDetails = "ICICI • AC: 00123456789 • IFSC: ICIC0000012"
    )
)

private val sampleLedgerEntriesData = listOf(
    ContractorLedgerEntryEntity(
        id = "l1",
        contractorId = "c1",
        transactionDate = "2026-11-10",
        description = "First Floor Brickwork Plastering (1200 Sq.Ft)",
        amount = 22200.0,
        status = "paid",
        payoutClass = "rate"
    ),
    ContractorLedgerEntryEntity(
        id = "l2",
        contractorId = "c1",
        transactionDate = "2026-11-12",
        description = "Parapet Wall Plastering (350 Sq.Ft)",
        amount = 6475.0,
        status = "pending",
        payoutClass = "rate"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorAccountsScreen(
    initialContractorId: String? = null,
    onBackClick: () -> Unit = {},
    onDialPhone: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTradeFilter by remember { mutableStateOf("All") }
    var selectedLedgerTab by remember { mutableStateOf("combined") }
    var isAddContractorDialogOpen by remember { mutableStateOf(false) }
    var isAddLedgerEntryDialogOpen by remember { mutableStateOf(false) }

    val contractors = remember { mutableStateListOf(*sampleContractorsData.toTypedArray()) }
    val ledgerEntries = remember { mutableStateListOf(*sampleLedgerEntriesData.toTypedArray()) }

    var selectedContractorId by remember { mutableStateOf(initialContractorId) }
    val selectedContractor = contractors.find { it.id == selectedContractorId }

    val availableTrades = remember(contractors) {
        listOf("All") + contractors.mapNotNull { it.category }.distinct()
    }

    val filteredContractors = remember(contractors, searchQuery, selectedTradeFilter) {
        contractors.filter { c ->
            val matchSearch = searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    (c.category?.contains(searchQuery, ignoreCase = true) == true)
            val matchTrade = selectedTradeFilter == "All" ||
                    c.category.equals(selectedTradeFilter, ignoreCase = true)
            matchSearch && matchTrade
        }
    }

    val rateDue = remember(ledgerEntries) {
        ledgerEntries.filter { it.payoutClass == "rate" && it.status == "pending" }.sumOf { it.amount }
    }
    val rateCleared = remember(ledgerEntries) {
        ledgerEntries.filter { it.payoutClass == "rate" && it.status == "paid" }.sumOf { it.amount }
    }
    val nmrDue = remember(ledgerEntries) {
        ledgerEntries.filter { it.payoutClass == "nmr" && it.status == "pending" }.sumOf { it.amount }
    }
    val nmrCleared = remember(ledgerEntries) {
        ledgerEntries.filter { it.payoutClass == "nmr" && it.status == "paid" }.sumOf { it.amount }
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
                        IconButton(onClick = { isAddContractorDialogOpen = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contractor", tint = BrandPrimary)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
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
                                Text("Due: ${rateDue.toInrFull()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                                Text("Cleared: ${rateCleared.toInrFull()}", fontSize = 11.sp, color = EmeraldSuccess)
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
                                Text("Due: ${nmrDue.toInrFull()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                Text("Cleared: ${nmrCleared.toInrFull()}", fontSize = 11.sp, color = EmeraldSuccess)
                            }
                        }
                    }
                }

                // Search & Trade Filter Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FluidGlossTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search contractor by name or trade...",
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Trade chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTrades.forEach { trade ->
                                val isSelected = selectedTradeFilter.equals(trade, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTradeFilter = trade },
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
                if (selectedContractor != null) {
                    item {
                        ContractorDetailCard(
                            contractor = selectedContractor,
                            onDial = { onDialPhone(selectedContractor.phone) },
                            onAddEntry = { isAddLedgerEntryDialogOpen = true }
                        )
                    }

                    // Statement sub-account tabs (Combined, Rate, NMR)
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = when (selectedLedgerTab) {
                                "rate" -> 1
                                "nmr" -> 2
                                else -> 0
                            },
                            containerColor = Color.Transparent,
                            contentColor = BrandPrimary
                        ) {
                            Tab(
                                selected = selectedLedgerTab == "combined",
                                onClick = { selectedLedgerTab = "combined" },
                                text = { Text("Combined Statement", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedLedgerTab == "rate",
                                onClick = { selectedLedgerTab = "rate" },
                                text = { Text("Rate Account", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedLedgerTab == "nmr",
                                onClick = { selectedLedgerTab = "nmr" },
                                text = { Text("NMR Account", fontSize = 12.sp) }
                            )
                        }
                    }

                    val activeLedgerEntries = when (selectedLedgerTab) {
                        "rate" -> ledgerEntries.filter { it.contractorId == selectedContractor.id && it.payoutClass == "rate" }
                        "nmr" -> ledgerEntries.filter { it.contractorId == selectedContractor.id && it.payoutClass == "nmr" }
                        else -> ledgerEntries.filter { it.contractorId == selectedContractor.id }
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
                    items(filteredContractors, key = { it.id }) { c ->
                        ContractorDirectoryCard(
                            contractor = c,
                            onClick = { selectedContractorId = c.id },
                            onDial = { onDialPhone(c.phone) }
                        )
                    }
                }
            }
        }

        // Add Contractor Dialog
        if (isAddContractorDialogOpen) {
            AddContractorDialog(
                onDismiss = { isAddContractorDialogOpen = false },
                onConfirm = { name, cat, contact, phone, email, acc, gst ->
                    val newC = ContractorEntity(
                        id = "c_${contractors.size + 1}",
                        name = name,
                        category = cat,
                        contactPerson = contact,
                        phone = phone,
                        email = email,
                        accountDetails = acc,
                        gstNumber = gst
                    )
                    contractors.add(newC)
                    isAddContractorDialogOpen = false
                }
            )
        }

        // Add Ledger Entry Dialog
        if (isAddLedgerEntryDialogOpen && selectedContractor != null) {
            AddLedgerEntryDialog(
                contractorName = selectedContractor.name,
                onDismiss = { isAddLedgerEntryDialogOpen = false },
                onConfirm = { type, desc, amt, status, pClass, ref ->
                    val newEntry = ContractorLedgerEntryEntity(
                        id = "l_${ledgerEntries.size + 1}",
                        contractorId = selectedContractor.id,
                        transactionDate = "2026-11-13",
                        entryType = type,
                        description = desc,
                        amount = amt,
                        status = status,
                        payoutClass = pClass,
                        referenceDetails = ref
                    )
                    ledgerEntries.add(newEntry)
                    isAddLedgerEntryDialogOpen = false
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
                    FluidGlossButton(
                        text = "Add Entry",
                        onClick = onAddEntry,
                        variant = FluidGlossButtonVariant.Primary,
                        size = FluidGlossButtonSize.Small,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
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

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Add New Contractor",
        icon = Icons.Default.PersonAdd,
        confirmButton = {
            FluidGlossButton(
                text = "Save Contractor",
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        error = "Name and Phone are required"
                        return@FluidGlossButton
                    }
                    onConfirm(
                        name,
                        category.takeIf { it.isNotBlank() },
                        contact.takeIf { it.isNotBlank() },
                        phone,
                        email.takeIf { it.isNotBlank() },
                        bank.takeIf { it.isNotBlank() },
                        gst.takeIf { it.isNotBlank() }
                    )
                },
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        FluidGlossTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = "Contractor / Agency Name",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = category,
            onValueChange = { category = it },
            label = "Trade / Category (e.g. Masonry, Electrical)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = contact,
            onValueChange = { contact = it },
            label = "Contact Person",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = "Phone Number",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = bank,
            onValueChange = { bank = it },
            label = "Bank & Account Details",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(error!!, color = RoseDanger, fontSize = 12.sp)
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

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Add Ledger Entry",
        subtitle = "Contractor: $contractorName",
        icon = Icons.Default.ReceiptLong,
        confirmButton = {
            FluidGlossButton(
                text = "Add Transaction",
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (description.isBlank() || amt == null || amt <= 0.0) {
                        error = "Description and valid amount are required"
                        return@FluidGlossButton
                    }
                    onConfirm(entryType, description, amt, status, payoutClass, reference.takeIf { it.isNotBlank() })
                },
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Text("Ledger Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
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
        FluidGlossTextField(
            value = description,
            onValueChange = { description = it; error = null },
            label = "Description (e.g. 500 Sq.Ft Plastering)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        FluidGlossTextField(
            value = amountText,
            onValueChange = { amountText = it; error = null },
            label = "Amount (₹)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

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
    }
}
