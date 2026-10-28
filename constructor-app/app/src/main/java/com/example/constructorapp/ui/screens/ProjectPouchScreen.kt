package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.PouchTransactionDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPouchScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.pouchTransactions.collectAsState()
    var isAddExpenseOpen by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadPouchTransactions(projectId)
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Initial dummy data if db is empty for preview
    val displayTransactions = remember(transactions) {
        if (transactions.isNotEmpty()) transactions
        else listOf(
            PouchTransactionDto(
                projectId = projectId,
                amount = 450.0,
                category = "Site Meals & Tea",
                description = "Morning tea and snacks for 12 masons",
                date = "2026-10-27",
                transactionType = "Expense"
            ),
            PouchTransactionDto(
                projectId = projectId,
                amount = 1200.0,
                category = "Urgent Hardware",
                description = "Binding wire 10kg & 3-inch nails from local shop",
                date = "2026-10-26",
                transactionType = "Expense"
            ),
            PouchTransactionDto(
                projectId = projectId,
                amount = 800.0,
                category = "Fuel & Diesel",
                description = "5 liters diesel for concrete vibrator engine",
                date = "2026-10-25",
                transactionType = "Expense"
            )
        )
    }

    val totalSpent = displayTransactions.filter { it.transactionType == "Expense" }.sumOf { it.amount }
    val initialDisbursed = 15000.0
    val inHandBalance = initialDisbursed - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Site Cash & Pouch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "$projectName • Petty Cash Ledger",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddExpenseOpen = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Site Expense")
            }
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "AVAILABLE PETTY CASH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currencyFormatter.format(inHandBalance),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (inHandBalance > 3000) EmeraldSuccess else AmberWarning
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SlateDivider)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Disbursed by Office", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = currencyFormatter.format(initialDisbursed),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Site Spent", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = currencyFormatter.format(totalSpent),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoseDanger
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Overview
            item {
                Text(
                    text = "SPENDING BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryPill(
                        icon = Icons.Default.Fastfood,
                        label = "Tea & Snacks",
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryPill(
                        icon = Icons.Default.Build,
                        label = "Hardware",
                        color = SkyInfo,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryPill(
                        icon = Icons.Default.LocalGasStation,
                        label = "Fuel / Diesel",
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expense Ledger Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOUCHER & EXPENSE LEDGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${displayTransactions.size} records",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            items(displayTransactions) { tx ->
                PouchTransactionCard(tx = tx)
            }
        }
    }

    if (isAddExpenseOpen) {
        RecordExpenseDialog(
            projectId = projectId,
            onDismiss = { isAddExpenseOpen = false },
            onSubmit = { newTx ->
                viewModel.createPouchTransaction(newTx) {
                    isAddExpenseOpen = false
                    Toast.makeText(context, "Expense recorded in site pouch!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun CategoryPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
fun PouchTransactionCard(tx: PouchTransactionDto) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val (catBg, catColor) = when {
        tx.category.contains("Tea", ignoreCase = true) || tx.category.contains("Snack", ignoreCase = true) -> AmberWarning.copy(alpha = 0.15f) to AmberWarning
        tx.category.contains("Hardware", ignoreCase = true) || tx.category.contains("Fastener", ignoreCase = true) -> SkyInfo.copy(alpha = 0.15f) to SkyInfo
        tx.category.contains("Fuel", ignoreCase = true) || tx.category.contains("Diesel", ignoreCase = true) -> EmeraldSuccess.copy(alpha = 0.15f) to EmeraldSuccess
        else -> PurpleAccent.copy(alpha = 0.15f) to PurpleAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tx.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tx.description ?: "Site Disbursement",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tx.date,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "-${currencyFormatter.format(tx.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RoseDanger
            )
        }
    }
}

@Composable
fun RecordExpenseDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onSubmit: (PouchTransactionDto) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Site Meals & Tea") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Site Meals & Tea", "Urgent Hardware", "Fuel & Diesel", "Transport & Auto", "Misc Labour Refreshment")
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Site Cash Expense", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount Spent (₹)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = calculatorFieldColors()
                )

                Text("Expense Category", fontSize = 11.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.substringBefore(" & "), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Expense Details / Purpose", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Nails & binding wire for slab", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = calculatorFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && description.isNotBlank()) {
                        onSubmit(
                            PouchTransactionDto(
                                projectId = projectId,
                                amount = amt,
                                category = category,
                                description = description.trim(),
                                transactionType = "Expense",
                                status = "Approved",
                                date = today
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SlateSurface
    )
}
