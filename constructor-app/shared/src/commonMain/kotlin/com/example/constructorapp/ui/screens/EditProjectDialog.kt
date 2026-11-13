package com.example.constructorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.ProjectDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
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

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Edit Project Details",
        icon = Icons.Default.Edit,
        confirmButton = {
            FluidGlossButton(
                text = "Save Changes",
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
                variant = FluidGlossButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FluidGlossButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = FluidGlossButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FluidGlossTextField(
                value = name,
                onValueChange = { name = it },
                label = "Project Name *",
                placeholder = "Project Name",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = "Client Name",
                placeholder = "Client Name",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = "Client Contact",
                placeholder = "Client Phone / Contact",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = location,
                onValueChange = { location = it },
                label = "Location",
                placeholder = "Site Address or Coordinates",
                modifier = Modifier.fillMaxWidth()
            )

            // Status Chips
            Text("Project Status", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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
                    Text("Progress", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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

            Column(modifier = Modifier.fillMaxWidth()) {
                FluidGlossTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = "Budget (₹) *",
                    placeholder = "Total Budget in INR",
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (parsedBudget > 0.0) {
                    Text(
                        text = "Preview: ${CurrencyFormatter.formatCompact(parsedBudget)} (${CurrencyFormatter.formatFull(parsedBudget)})",
                        color = EmeraldSuccess,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluidGlossTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
                FluidGlossTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "End Date",
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
            }

            FluidGlossTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description / Notes",
                placeholder = "Scope of work, milestone notes",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
        }
    }
}
