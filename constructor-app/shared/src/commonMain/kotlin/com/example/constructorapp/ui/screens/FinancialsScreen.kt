package com.example.constructorapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ContractorDto
import com.example.constructorapp.theme.*

val sampleContractorsList = listOf(
    ContractorDto("c1", "Murugan Masonry Works", "+91 98421 11223", "Civil & Brickwork", "Rate", 18.5),
    ContractorDto("c2", "Selvam Bar Bending Team", "+91 94432 44556", "Reinforcement", "NMR", 850.0),
    ContractorDto("c3", "Karthik Electricals", "+91 97890 88990", "Electrical & Conduiting", "Rate", 24.0),
    ContractorDto("c4", "Anand Plumbing Crew", "+91 91234 56780", "Plumbing & Drainage", "NMR", 750.0)
)

@Composable
fun FinancialsScreen(
    contractors: List<ContractorDto> = sampleContractorsList,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                                text = "Financials & Contractors",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Rate vs NMR payouts & settlements",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Payday Ready",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        },
        containerColor = SlateBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialStatCard(
                    title = "Rate Contractors",
                    value = contractors.count { it.payoutType.equals("Rate", ignoreCase = true) }.toString(),
                    badgeColor = SkyInfo,
                    modifier = Modifier.weight(1f)
                )
                FinancialStatCard(
                    title = "NMR Contractors",
                    value = contractors.count { it.payoutType.equals("NMR", ignoreCase = true) }.toString(),
                    badgeColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Rate", "NMR").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BrandPrimary.copy(alpha = 0.2f) else SlateSurface)
                            .border(1.dp, if (isSelected) BrandPrimary else SlateBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredList = contractors.filter {
                if (selectedFilter == "All") true
                else it.payoutType.equals(selectedFilter, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Contractors Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Contractors added to worklogs will appear here.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { contractor ->
                        ContractorCard(contractor)
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialStatCard(
    title: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
        }
    }
}

@Composable
fun ContractorCard(contractor: ContractorDto) {
    val isRate = contractor.payoutType.equals("Rate", ignoreCase = true)
    val badgeColor = if (isRate) SkyInfo else EmeraldSuccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = contractor.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = contractor.trade ?: "General Contractor",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (!contractor.phone.isNullOrBlank()) {
                    Text(
                        text = "Phone: ${contractor.phone}",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isRate) "RATE WORK" else "NMR WORK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
