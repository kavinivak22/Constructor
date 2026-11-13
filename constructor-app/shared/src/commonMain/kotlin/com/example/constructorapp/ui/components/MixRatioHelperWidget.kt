package com.example.constructorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*

data class MixRatioMaterialItem(
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String
)

@Composable
fun MixRatioHelperWidget(
    onAddMaterials: (List<MixRatioMaterialItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var activity by remember { mutableStateOf("concrete") } // concrete | masonry | plaster
    var mixGrade by remember { mutableStateOf("m20") }
    var customSand by remember { mutableStateOf("2.5") }
    var customGravel by remember { mutableStateOf("3.5") }
    var cementBags by remember { mutableStateOf("") }
    var calculatedResult by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title + IS 456 Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Mix Calculator",
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mix Ratio Helper",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-calculate Sand & Gravel (1 bag = 1.25 CFT)",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "IS 456 SPEC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
            }

            // Activity Selector Chips: Concrete, Masonry, Plaster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "concrete" to "Concrete",
                    "masonry" to "Masonry",
                    "plaster" to "Plastering"
                ).forEach { (key, label) ->
                    val isSelected = activity == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandPrimary else Color.White)
                            .border(
                                1.dp,
                                if (isSelected) BrandPrimary else Color(0xFFE2E8F0),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                activity = key
                                mixGrade = when (key) {
                                    "concrete" -> "m20"
                                    "masonry" -> "1:4"
                                    else -> "1:6"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            // Mix Grade Options Header
            Text(
                text = "Mix Grade / Ratio",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            val grades = when (activity) {
                "concrete" -> listOf(
                    "m20" to "M20 (1:1.5:3)",
                    "m25" to "M25 (1:1:2)",
                    "m15" to "M15 (1:2:4)",
                    "custom" to "Custom"
                )
                "masonry" -> listOf(
                    "1:4" to "1:4 (Standard)",
                    "1:3" to "1:3 (Rich)",
                    "1:5" to "1:5 (Light)",
                    "custom" to "Custom"
                )
                else -> listOf(
                    "1:6" to "1:6 (Standard)",
                    "1:4" to "1:4 (Ceiling)",
                    "custom" to "Custom"
                )
            }

            // 2-Column Responsive Grid for Grade Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                grades.chunked(2).forEach { rowGrades ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGrades.forEach { (gKey, gLabel) ->
                            val isGradeSelected = mixGrade == gKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isGradeSelected) BrandPrimary.copy(alpha = 0.12f) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isGradeSelected) BrandPrimary else Color(0xFFE2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { mixGrade = gKey }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isGradeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isGradeSelected) BrandPrimary else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowGrades.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Custom inputs if custom selected
            if (mixGrade == "custom") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSand,
                        onValueChange = { customSand = it },
                        label = { Text("Sand Parts", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    if (activity == "concrete") {
                        OutlinedTextField(
                            value = customGravel,
                            onValueChange = { customGravel = it },
                            label = { Text("Gravel Parts", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Cement Bags Input & Calculate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cementBags,
                    onValueChange = {
                        cementBags = it
                        calculatedResult = null
                    },
                    label = { Text("Cement Bags", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 50", fontSize = 12.sp, color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val bags = cementBags.toDoubleOrNull() ?: 0.0
                        if (bags <= 0.0) return@Button

                        var sandRatio = 1.5
                        var gravelRatio = 3.0
                        var sandName = "M-Sand (Fine Aggregate)"
                        val gravelName = "20mm Blue Metal (Coarse Aggregate)"

                        if (activity == "concrete") {
                            when (mixGrade) {
                                "m20" -> { sandRatio = 1.5; gravelRatio = 3.0 }
                                "m25" -> { sandRatio = 1.0; gravelRatio = 2.0 }
                                "m15" -> { sandRatio = 2.0; gravelRatio = 4.0 }
                                "custom" -> {
                                    sandRatio = customSand.toDoubleOrNull() ?: 2.0
                                    gravelRatio = customGravel.toDoubleOrNull() ?: 4.0
                                }
                            }
                        } else if (activity == "masonry") {
                            sandName = "P-Sand (Masonry Sand)"
                            gravelRatio = 0.0
                            when (mixGrade) {
                                "1:4" -> sandRatio = 4.0
                                "1:3" -> sandRatio = 3.0
                                "1:5" -> sandRatio = 5.0
                                "custom" -> sandRatio = customSand.toDoubleOrNull() ?: 4.0
                            }
                        } else {
                            sandName = "Plastering Sand (Fine)"
                            gravelRatio = 0.0
                            when (mixGrade) {
                                "1:6" -> sandRatio = 6.0
                                "1:4" -> sandRatio = 4.0
                                "custom" -> sandRatio = customSand.toDoubleOrNull() ?: 6.0
                            }
                        }

                        val calculatedSandCFT = kotlin.math.round(bags * sandRatio * 1.25 * 10.0) / 10.0
                        val calculatedGravelCFT = kotlin.math.round(bags * gravelRatio * 1.25 * 10.0) / 10.0

                        val items = mutableListOf(
                            MixRatioMaterialItem("53 Grade Cement", bags, "Bags"),
                            MixRatioMaterialItem(sandName, calculatedSandCFT, "CFT")
                        )
                        if (gravelRatio > 0.0) {
                            items.add(MixRatioMaterialItem(gravelName, calculatedGravelCFT, "CFT"))
                        }

                        calculatedResult = "$calculatedSandCFT CFT Sand" + if (gravelRatio > 0.0) " • $calculatedGravelCFT CFT Gravel" else ""
                        onAddMaterials(items)
                    },
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = cementBags.toDoubleOrNull() != null && (cementBags.toDoubleOrNull() ?: 0.0) > 0.0,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Calculation Result Preview
            calculatedResult?.let { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.12f))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Added to materials: $res",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}
