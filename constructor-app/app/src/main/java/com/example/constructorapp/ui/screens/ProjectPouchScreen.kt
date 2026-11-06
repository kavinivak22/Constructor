package com.example.constructorapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.remote.DocumentDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectPouchScreen(
    projectId: String,
    projectName: String,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val rawDocuments by viewModel.documents.collectAsState()
    val isDocumentsLoading by viewModel.isDocumentsLoading.collectAsState()
    val allProjects by viewModel.projects.collectAsState()

    var activeProjectId by remember(projectId, allProjects) {
        mutableStateOf(
            if (projectId.isNotBlank()) projectId
            else allProjects.firstOrNull()?.id ?: ""
        )
    }

    val currentProjectName = remember(activeProjectId, allProjects) {
        allProjects.find { it.id == activeProjectId }?.name ?: projectName.ifBlank { "Project Pouch" }
    }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isAddItemOpen by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<DocumentDto?>(null) }

    LaunchedEffect(activeProjectId) {
        if (activeProjectId.isNotBlank()) {
            viewModel.loadDocuments(activeProjectId)
        }
    }

    // Default starter vault items for initial project setup
    val starterTemplates = remember(activeProjectId) {
        listOf(
            DocumentDto(
                id = "demo-bp-1",
                projectId = activeProjectId,
                name = "Architectural Floor Plan Rev 3",
                url = "https://drive.google.com/open?id=architectural-blueprint-final",
                size = 3450000L,
                type = "pdf",
                category = "Blueprints",
                createdAt = "2026-10-25T10:00:00Z"
            ),
            DocumentDto(
                id = "demo-des-1",
                projectId = activeProjectId,
                name = "3D Exterior Elevation & Facade Render",
                url = "https://drive.google.com/open?id=exterior-elevation-render",
                size = 8200000L,
                type = "image",
                category = "Designs",
                createdAt = "2026-10-22T14:30:00Z"
            ),
            DocumentDto(
                id = "demo-note-1",
                projectId = activeProjectId,
                name = "Site Benchmark Dimensions & Foundation Depth",
                url = "Foundation footings depth: 5ft 6in below natural ground. Column center spacing: 12ft 4in C/C. Bearing capacity verified at 180 kN/sq.m.",
                size = 1200L,
                type = "note",
                category = "Notes",
                createdAt = "2026-10-20T08:15:00Z"
            ),
            DocumentDto(
                id = "demo-spec-1",
                projectId = activeProjectId,
                name = "Steel Rebar & Concrete Mix Grade Specifications",
                url = "Slab: M25 (1:1:2). Columns & Beams: M25 with Fe550D TMT bars. Min lap length: 48d. 28-day target cube strength: 31.6 N/mm².",
                size = 2400L,
                type = "note",
                category = "Specifications",
                createdAt = "2026-10-18T16:00:00Z"
            )
        )
    }

    val displayDocuments = remember(rawDocuments, starterTemplates) {
        if (rawDocuments.isNotEmpty()) rawDocuments else starterTemplates
    }

    val categories = listOf("All", "Blueprints", "Designs", "Notes", "Specifications", "Important Details")

    val filteredList = remember(displayDocuments, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") displayDocuments
        else displayDocuments.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0x150F172A)),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Project Pouch",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$currentProjectName • Storage & Blueprints",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { isAddItemOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Store Item",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Project Switcher if multiple projects exist
                    if (allProjects.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(allProjects) { p ->
                                val isSelected = p.id == activeProjectId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { activeProjectId = p.id },
                                    label = {
                                        Text(
                                            text = p.name,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandPrimary,
                                        containerColor = Color.White.copy(alpha = 0.8f),
                                        labelColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) BrandPrimary.copy(alpha = 0.5f) else Color(0x150F172A))
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    val count = if (cat == "All") displayDocuments.size else displayDocuments.count { it.category.equals(cat, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat },
                        label = {
                            Text(
                                text = "$cat ($count)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) BrandPrimary else Color(0x200F172A))
                    )
                }
            }

            // Storage Overview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StorageStatPill(
                    title = "Blueprints",
                    count = displayDocuments.count { it.category.equals("Blueprints", ignoreCase = true) },
                    icon = Icons.Outlined.Architecture,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
                StorageStatPill(
                    title = "Designs & 3D",
                    count = displayDocuments.count { it.category.equals("Designs", ignoreCase = true) },
                    icon = Icons.Outlined.DesignServices,
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
                StorageStatPill(
                    title = "Site Notes",
                    count = displayDocuments.count { it.category.equals("Notes", ignoreCase = true) || it.category.equals("Specifications", ignoreCase = true) },
                    icon = Icons.Outlined.StickyNote2,
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Document List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0x150F172A))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderZip,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No storage items found in this section",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Store Item' to upload blueprints, designs, notes or site measurements.",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList) { doc ->
                        PouchItemCard(
                            document = doc,
                            onClick = { viewingDocument = doc },
                            onDelete = {
                                doc.id?.let { id ->
                                    viewModel.deleteDocument(id, activeProjectId) {
                                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Add New Storage Item
    if (isAddItemOpen) {
        AddPouchItemDialog(
            projectId = activeProjectId,
            projectName = currentProjectName,
            onDismiss = { isAddItemOpen = false },
            onSubmit = { newDoc ->
                viewModel.addDocument(newDoc) {
                    Toast.makeText(context, "Saved to Project Pouch", Toast.LENGTH_SHORT).show()
                }
                isAddItemOpen = false
            }
        )
    }

    // Modal: Inspect Item Details / Blueprints
    if (viewingDocument != null) {
        val doc = viewingDocument!!
        InspectPouchItemDialog(
            document = doc,
            onDismiss = { viewingDocument = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(doc.url))
                Toast.makeText(context, "Details copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onOpenLink = {
                if (doc.url.startsWith("http://") || doc.url.startsWith("https://")) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.url))
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Could not open external link", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun StorageStatPill(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x150F172A)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "$count",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = title,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PouchItemCard(
    document: DocumentDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (catColor, catIcon) = when (document.category.lowercase()) {
        "blueprints" -> Pair(Color(0xFF2563EB), Icons.Outlined.Architecture)
        "designs" -> Pair(Color(0xFF7C3AED), Icons.Outlined.DesignServices)
        "notes" -> Pair(EmeraldSuccess, Icons.Outlined.StickyNote2)
        "specifications" -> Pair(AmberWarning, Icons.Outlined.Description)
        else -> Pair(BrandPrimary, Icons.Outlined.FolderShared)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0x180F172A)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = document.name,
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(catColor.copy(alpha = 0.10f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = document.category.uppercase(),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = document.url,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (document.createdAt.isNullOrBlank()) "Saved in Pouch" else document.createdAt.take(10),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                    if (document.size > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${(document.size / 1024)} KB",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddPouchItemDialog(
    projectId: String,
    projectName: String,
    onDismiss: () -> Unit,
    onSubmit: (DocumentDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Notes") }
    var detailsOrUrl by remember { mutableStateOf("") }

    val categories = listOf("Notes", "Blueprints", "Designs", "Specifications", "Important Details")
    val isValid = name.trim().isNotEmpty() && detailsOrUrl.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Store in Project Pouch",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    text = projectName,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Title / Item Name *", color = TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. Electrical Layout or Foundation Notes", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lightDialogTextFieldColors()
                )

                Text(
                    text = "Category",
                    fontFamily = PlusJakartaSansFontFamily,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = detailsOrUrl,
                    onValueChange = { detailsOrUrl = it },
                    label = { Text("Details, Notes or Link / URL *", color = TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("Type structural notes, site measurements, or paste Google Drive / Dropbox link...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = lightDialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val isUrl = detailsOrUrl.startsWith("http://") || detailsOrUrl.startsWith("https://")
                        val itemType = if (isUrl) {
                            if (detailsOrUrl.endsWith(".pdf", ignoreCase = true)) "pdf"
                            else if (detailsOrUrl.endsWith(".png", ignoreCase = true) || detailsOrUrl.endsWith(".jpg", ignoreCase = true)) "image"
                            else "link"
                        } else "note"

                        onSubmit(
                            DocumentDto(
                                id = UUID.randomUUID().toString(),
                                projectId = projectId,
                                uploaderId = SupabaseClient.userId,
                                name = name.trim(),
                                url = detailsOrUrl.trim(),
                                size = detailsOrUrl.length.toLong(),
                                type = itemType,
                                category = category,
                                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                            )
                        )
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Item", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun InspectPouchItemDialog(
    document: DocumentDto,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpenLink: () -> Unit
) {
    val isUrl = document.url.startsWith("http://") || document.url.startsWith("https://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = document.name,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = document.category.uppercase(),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Text(
                        text = document.url,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }

                if (isUrl) {
                    Text(
                        text = "This item links to an external cloud resource or blueprint document.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isUrl) {
                    Button(
                        onClick = onOpenLink,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", color = Color.White, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
