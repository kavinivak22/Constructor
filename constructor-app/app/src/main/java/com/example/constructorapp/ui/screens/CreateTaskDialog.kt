package com.example.constructorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.remote.TaskItemDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.FluidGlossButton
import com.example.constructorapp.ui.components.FluidGlossButtonVariant
import com.example.constructorapp.ui.components.FluidGlossDialog
import com.example.constructorapp.ui.components.FluidGlossTextField
import java.util.UUID

@Composable
fun CreateTaskDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onSubmit: (TaskItemDto) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }

    val isFormValid = title.trim().isNotEmpty()

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Create Project Task",
        icon = Icons.AutoMirrored.Filled.Assignment,
        confirmButton = {
            FluidGlossButton(
                text = "Create Task",
                onClick = {
                    if (isFormValid) {
                        onSubmit(
                            TaskItemDto(
                                id = UUID.randomUUID().toString(),
                                projectId = projectId,
                                title = title.trim(),
                                description = description.ifBlank { null },
                                dueDate = dueDate.ifBlank { null },
                                priority = priority.lowercase(),
                                status = "pending",
                                createdBy = SupabaseClient.userId
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
                variant = FluidGlossButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FluidGlossTextField(
                value = title,
                onValueChange = { title = it },
                label = "Task Title *",
                placeholder = "e.g. Electrical Conduit Layout",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Details or checklist instructions",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )

            FluidGlossTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = "Due Date (YYYY-MM-DD)",
                placeholder = "e.g. 2026-11-15",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Priority",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("High", "Medium", "Low").forEach { p ->
                    val isSelected = priority.equals(p, ignoreCase = true)
                    val color = when (p) {
                        "High" -> RoseDanger
                        "Medium" -> AmberWarning
                        else -> EmeraldSuccess
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { priority = p },
                        label = { Text(p, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        )
                    )
                }
            }
        }
    }
}
