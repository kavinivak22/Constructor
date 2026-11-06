package com.example.constructorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.util.CurrencyFormatter

@Composable
fun EditProjectDialog(
    project: ProjectDto,
    onDismiss: () -> Unit,
    onSubmit: (ProjectDto) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var clientName by remember { mutableStateOf(project.clientName ?: "") }
    var clientPhone by remember { mutableStateOf(project.clientPhone ?: "") }
    var location by remember { mutableStateOf(project.location ?: "") }
    var description by remember { mutableStateOf(project.description ?: "") }
    var status by remember { mutableStateOf(project.status ?: "active") }
    var progressSlider by remember { mutableFloatStateOf(project.progress.toFloat()) }
    var budgetStr by remember { mutableStateOf(if (project.budget > 0) project.budget.toLong().toString() else "") }
    var startDate by remember { mutableStateOf(project.startDate ?: "") }
    var endDate by remember { mutableStateOf(project.endDate ?: "") }

    val parsedBudget = remember(budgetStr) { CurrencyFormatter.parseInr(budgetStr) ?: project.budget }
    val isFormValid = name.trim().length >= 2 && parsedBudget >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Project Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Client Contact", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                // Status Chips
                Text("Project Status", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("active", "on-hold", "completed").forEach { st ->
                        FilterChip(
                            selected = status.equals(st, ignoreCase = true),
                            onClick = { status = st },
                            label = { Text(st.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }

                // Progress Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress", fontSize = 12.sp, color = TextSecondary)
                        Text("${progressSlider.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                    Slider(
                        value = progressSlider,
                        onValueChange = { progressSlider = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandPrimary,
                            activeTrackColor = BrandPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Budget (₹) *", color = TextSecondary, fontSize = 12.sp) },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                    supportingText = {
                        if (parsedBudget > 0.0) {
                            Text(
                                "Preview: ${CurrencyFormatter.formatCompact(parsedBudget)} (${CurrencyFormatter.formatFull(parsedBudget)})",
                                color = EmeraldSuccess,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = calculatorFieldColors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = calculatorFieldColors()
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = calculatorFieldColors()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes", color = TextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = calculatorFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        onSubmit(
                            project.copy(
                                name = name.trim(),
                                description = description.ifBlank { null },
                                clientName = clientName.ifBlank { null },
                                clientPhone = clientPhone.ifBlank { null },
                                location = location.ifBlank { null },
                                status = status,
                                progress = progressSlider.toInt(),
                                budget = parsedBudget,
                                startDate = startDate.ifBlank { null },
                                endDate = endDate.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Changes")
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
