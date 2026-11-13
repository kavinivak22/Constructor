package com.example.constructorapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.local.entity.ProjectEntity
import com.example.constructorapp.data.remote.DocumentDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.*
import com.example.constructorapp.util.Toast
import kotlin.math.round

// ─── Data & Helpers matching Webapp ─────────────────────────────────────────

val NOTE_CATEGORIES = listOf(
    "General",
    "Site Utilities",
    "Engineering Specs",
    "Access & Codes",
    "Client Memos",
    "Site Measurements",
    "Other"
)

val DOCUMENT_CATEGORIES = listOf(
    "General",
    "Contracts",
    "Blueprints",
    "Invoices",
    "Reports"
)

data class ParsedNote(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val pinned: Boolean,
    val createdAt: String?,
    val rawDoc: DocumentDto
)

fun parseNoteData(doc: DocumentDto): ParsedNote {
    var content = doc.url
    var pinned = false
    try {
        val trimmed = doc.url.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val contentMatch = Regex("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(trimmed)
            if (contentMatch != null) {
                content = contentMatch.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
            }
            val pinnedMatch = Regex("\"pinned\"\\s*:\\s*(true|false)").find(trimmed)
            if (pinnedMatch != null) {
                pinned = pinnedMatch.groupValues[1] == "true"
            }
        }
    } catch (_: Exception) {
        content = doc.url
    }
    return ParsedNote(
        id = doc.id ?: "",
        title = doc.name,
        content = content,
        category = doc.category.ifBlank { "General" },
        pinned = pinned,
        createdAt = doc.createdAt,
        rawDoc = doc
    )
}

fun serializeNotePayload(content: String, pinned: Boolean): String {
    val escaped = content.trim().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    return "{\"content\":\"$escaped\",\"pinned\":$pinned}"
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val k = 1024.0
    val sizes = listOf("B", "KB", "MB", "GB")
    var b = bytes.toDouble()
    var i = 0
    while (b >= k && i < sizes.size - 1) {
        b /= k
        i++
    }
    val rounded = (round(b * 10) / 10.0).toString()
    return "$rounded ${sizes[i]}"
}

fun formatPouchDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Recent"
    val clean = dateStr.substringBefore("T")
    val parts = clean.split("-")
    if (parts.size == 3) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthIdx = parts[1].toIntOrNull()?.minus(1)
        if (monthIdx != null && monthIdx in 0..11) {
            val day = parts[2].toIntOrNull() ?: parts[2]
            return "${monthNames[monthIdx]} $day, ${parts[0]}"
        }
    }
    return clean
}

// ─── Main Screen Composable ─────────────────────────────────────────────────

@Composable
fun ProjectPouchScreen(
    projectId: String = "",
    projectName: String = "",
    onBackClick: () -> Unit = {},
    allProjects: List<ProjectEntity> = emptyList(),
    initialDocuments: List<DocumentDto> = emptyList(),
    onAddDocument: ((DocumentDto, (Boolean) -> Unit) -> Unit)? = null,
    onUpdateDocument: ((docId: String, projectId: String, updates: Map<String, Any?>, (Boolean) -> Unit) -> Unit)? = null,
    onDeleteDocument: ((docId: String, projectId: String, () -> Unit) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current

    var rawDocuments by remember { mutableStateOf(initialDocuments) }
    val isDocumentsLoading by remember { mutableStateOf(false) }

    var activeProjectId by remember(projectId) {
        mutableStateOf(projectId)
    }

    LaunchedEffect(allProjects, activeProjectId) {
        if (activeProjectId.isBlank() && allProjects.isNotEmpty()) {
            activeProjectId = allProjects.first().id
        }
    }

    val currentProject = remember(activeProjectId, allProjects) {
        allProjects.find { it.id == activeProjectId }
    }
    val currentProjectName = currentProject?.name ?: projectName.ifBlank { "All Projects" }

    // Tab State: 'documents' vs 'notes' matching webapp exactly
    var activeTab by remember { mutableStateOf<String>("documents") }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedDocCategory by remember { mutableStateOf("All") }
    var selectedNoteCategory by remember { mutableStateOf("All") }

    // Action menus and dialog state
    var isUniversalActionOpen by remember { mutableStateOf(false) }
    var isProjectPickerOpen by remember { mutableStateOf(false) }
    var isCreateNoteOpen by remember { mutableStateOf(false) }
    var isAddDocOpen by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<DocumentDto?>(null) }
    var editingNote by remember { mutableStateOf<ParsedNote?>(null) }
    var viewingNote by remember { mutableStateOf<ParsedNote?>(null) }
    var viewingPdfDoc by remember { mutableStateOf<DocumentDto?>(null) }

    // Pure real data: NO sample or demo fallback
    val fileDocuments = remember(rawDocuments) {
        rawDocuments.filter { it.type != "note" }
    }

    val noteDocuments = remember(rawDocuments) {
        rawDocuments.filter { it.type == "note" }
    }

    val filteredFileDocs = remember(fileDocuments, selectedDocCategory, searchQuery) {
        fileDocuments.filter { doc ->
            val matchCategory = selectedDocCategory == "All" || doc.category.equals(selectedDocCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    doc.name.contains(searchQuery, ignoreCase = true) ||
                    doc.category.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val filteredNoteDocs = remember(noteDocuments, selectedNoteCategory, searchQuery) {
        noteDocuments
            .map { parseNoteData(it) }
            .filter { note ->
                val matchCategory = selectedNoteCategory == "All" || note.category.equals(selectedNoteCategory, ignoreCase = true)
                val matchSearch = searchQuery.isBlank() ||
                        note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true) ||
                        note.category.contains(searchQuery, ignoreCase = true)
                matchCategory && matchSearch
            }
            .sortedWith(
                compareByDescending<ParsedNote> { it.pinned }
                    .thenByDescending { it.createdAt ?: "" }
            )
    }

    fun handlePinToggle(note: ParsedNote) {
        val newPinned = !note.pinned
        val updatedPayload = serializeNotePayload(note.content, newPinned)
        if (onUpdateDocument != null) {
            onUpdateDocument(
                note.id,
                activeProjectId.ifBlank { note.rawDoc.projectId ?: "" },
                mapOf("url" to updatedPayload)
            ) { success ->
                if (success) {
                    Toast.makeText(null, if (newPinned) "Note pinned to top" else "Note unpinned").show()
                }
            }
        } else {
            rawDocuments = rawDocuments.map { doc ->
                if (doc.id == note.id) doc.copy(url = updatedPayload) else doc
            }
            Toast.makeText(null, if (newPinned) "Note pinned to top" else "Note unpinned").show()
        }
    }

    fun handleDelete(docId: String, isNote: Boolean) {
        if (onDeleteDocument != null) {
            onDeleteDocument(docId, activeProjectId) {
                Toast.makeText(null, if (isNote) "Note removed" else "Document deleted").show()
            }
        } else {
            rawDocuments = rawDocuments.filter { it.id != docId }
            Toast.makeText(null, if (isNote) "Note removed" else "Document deleted").show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SlateBackground)) {
        // Living Ambient Drifting Blobs
        AmbientBlobBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ─── 1. FluidGloss Top Header Bar ──────────────────────────────────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp)
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

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Project Pouch",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = TextPrimary
                                )

                                // Clickable Project Badge to Switch Projects
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandPrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.30f)),
                                        modifier = Modifier.clickable { isProjectPickerOpen = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = currentProjectName,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowDropDown,
                                                contentDescription = null,
                                                tint = BrandPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    FluidGlossDropdownMenu(
                                        expanded = isProjectPickerOpen,
                                        onDismissRequest = { isProjectPickerOpen = false },
                                        modifier = Modifier.width(220.dp)
                                    ) {
                                        FluidGlossDropdownMenuItem(
                                            text = "All Projects Vault",
                                            selected = activeProjectId.isBlank(),
                                            onClick = {
                                                activeProjectId = ""
                                                isProjectPickerOpen = false
                                            }
                                        )
                                        allProjects.forEach { p ->
                                            FluidGlossDropdownMenuItem(
                                                text = p.name,
                                                selected = activeProjectId == p.id,
                                                onClick = {
                                                    activeProjectId = p.id
                                                    isProjectPickerOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Vault for Blueprints, Site Specs & Field Memos",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Universal Add / Upload FluidGloss Dropdown Menu Button
                        Box {
                            FluidGlossButton(
                                text = "Add",
                                onClick = { isUniversalActionOpen = true },
                                variant = FluidGlossButtonVariant.Primary,
                                size = FluidGlossButtonSize.Small,
                                modifier = Modifier.height(36.dp)
                            )

                            FluidGlossDropdownMenu(
                                expanded = isUniversalActionOpen,
                                onDismissRequest = { isUniversalActionOpen = false },
                                modifier = Modifier.width(230.dp)
                            ) {
                                FluidGlossDropdownMenuItem(
                                    text = "Upload Document",
                                    subtitle = "PDF, Blueprint, Images",
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.UploadFile,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        isUniversalActionOpen = false
                                        isAddDocOpen = true
                                    }
                                )

                                FluidGlossDropdownMenuItem(
                                    text = "Create New Note",
                                    subtitle = "Specs, passcodes, memos",
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        isUniversalActionOpen = false
                                        isCreateNoteOpen = true
                                    }
                                )
                            }
                        }
                    }

                    // Quick Project Switcher Carousel
                    if (allProjects.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = activeProjectId.isBlank(),
                                    onClick = { activeProjectId = "" },
                                    label = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandPrimary,
                                        containerColor = Color.White.copy(alpha = 0.8f),
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = activeProjectId.isBlank(),
                                        borderColor = SlateBorder,
                                        selectedBorderColor = BrandPrimary
                                    )
                                )
                            }
                            items(allProjects) { p ->
                                val isSelected = activeProjectId == p.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { activeProjectId = p.id },
                                    label = { Text(p.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandPrimary,
                                        containerColor = Color.White.copy(alpha = 0.8f),
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = SlateBorder,
                                        selectedBorderColor = BrandPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── 2. Segmented Control Tabs (Documents vs Notes) ────────────────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Documents Tab Trigger
                    val isDocActive = activeTab == "documents"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDocActive) Color.White else Color.Transparent)
                            .clickable { activeTab = "documents" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderShared,
                                contentDescription = null,
                                tint = if (isDocActive) BrandPrimary else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Documents",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = if (isDocActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isDocActive) TextPrimary else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isDocActive) BrandPrimary.copy(alpha = 0.15f) else Color(0x180F172A)
                            ) {
                                Text(
                                    text = "${fileDocuments.size}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDocActive) BrandPrimary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Notes Tab Trigger
                    val isNoteActive = activeTab == "notes"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNoteActive) Color.White else Color.Transparent)
                            .clickable { activeTab = "notes" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = null,
                                tint = if (isNoteActive) Color(0xFFD97706) else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes & Specs",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = if (isNoteActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isNoteActive) TextPrimary else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isNoteActive) Color(0x33F59E0B) else Color(0x180F172A)
                            ) {
                                Text(
                                    text = "${noteDocuments.size}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNoteActive) Color(0xFFD97706) else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── 3. Search Bar & Contextual Category Filters ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // FluidGloss Search Bar
                FluidGlossTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (activeTab == "documents") "Search blueprints, drawings, contracts..." else "Search site notes & specs...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Filter Pills
                val currentCategoryLabel = if (activeTab == "documents") selectedDocCategory else selectedNoteCategory
                val currentCategoryList = if (activeTab == "documents") listOf("All") + DOCUMENT_CATEGORIES else listOf("All") + NOTE_CATEGORIES

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(currentCategoryList) { cat ->
                        val isSelected = cat == currentCategoryLabel
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (activeTab == "documents") selectedDocCategory = cat
                                else selectedNoteCategory = cat
                            },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (activeTab == "documents") BrandPrimary.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                selectedLabelColor = if (activeTab == "documents") BrandPrimary else Color(0xFFD97706),
                                containerColor = Color.White.copy(alpha = 0.8f),
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SlateBorder,
                                selectedBorderColor = if (activeTab == "documents") BrandPrimary else Color(0xFFD97706)
                            )
                        )
                    }
                }
            }

            // ─── 4. Content Body (Documents vs Notes) ───────────────────────────
            if (isDocumentsLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = BrandPrimary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Loading Vault...",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else if (activeTab == "documents") {
                if (filteredFileDocs.isEmpty()) {
                    FluidGlossEmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = if (fileDocuments.isEmpty()) "No Documents Uploaded" else "No Matching Documents",
                        description = if (fileDocuments.isEmpty())
                            "Upload architectural blueprints, structural drawings, contract PDFs, or site photos to store them securely."
                        else "Try adjusting your search query or category filter.",
                        actionText = if (fileDocuments.isEmpty()) "Upload Document" else null,
                        onAction = { isAddDocOpen = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredFileDocs, key = { it.id ?: it.url }) { doc ->
                            FluidGlossDocumentCard(
                                doc = doc,
                                onPreview = { viewingPdfDoc = doc },
                                onEdit = { editingDoc = doc },
                                onDelete = { doc.id?.let { handleDelete(it, false) } }
                            )
                        }
                    }
                }
            } else {
                if (filteredNoteDocs.isEmpty()) {
                    FluidGlossEmptyState(
                        icon = Icons.AutoMirrored.Outlined.StickyNote2,
                        title = if (noteDocuments.isEmpty()) "No Site Notes Yet" else "No Matching Notes",
                        description = if (noteDocuments.isEmpty())
                            "Record foundation specs, electricity meter codes, structural engineering memos, and client decisions."
                        else "No notes match your filter or search query.",
                        actionText = if (noteDocuments.isEmpty()) "Create Note" else null,
                        onAction = { isCreateNoteOpen = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNoteDocs, key = { it.id }) { note ->
                            FluidGlossNoteCard(
                                note = note,
                                onPinToggle = { handlePinToggle(note) },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(note.content))
                                    Toast.makeText(null, "Note copied to clipboard").show()
                                },
                                onEdit = { editingNote = note },
                                onDelete = { handleDelete(note.id, true) },
                                onClick = { viewingNote = note }
                            )
                        }
                    }
                }
            }
        }
    }

    // ─── High-End Modern PDF & Blueprint Viewer Dialog ──────────────────────────
    viewingPdfDoc?.let { doc: DocumentDto ->
        ModernPdfBlueprintViewerDialog(
            title = doc.name,
            urlOrPath = doc.url,
            category = doc.category.ifBlank { "Blueprint" },
            onDismiss = { viewingPdfDoc = null }
        )
    }

    // ─── Create Note Dialog ───────────────────────────────────────────────────
    if (isCreateNoteOpen) {
        FluidGlossCreateNoteDialog(
            activeProjectId = activeProjectId,
            onDismiss = { isCreateNoteOpen = false },
            onSave = { title, category, content, pinned ->
                val payload = serializeNotePayload(content, pinned)
                val newDoc = DocumentDto(
                    id = "note_${rawDocuments.size + 1}",
                    projectId = activeProjectId.ifBlank { allProjects.firstOrNull()?.id ?: "" },
                    name = title.trim(),
                    url = payload,
                    size = content.length.toLong(),
                    type = "note",
                    category = category
                )
                if (onAddDocument != null) {
                    onAddDocument(newDoc) {
                        Toast.makeText(null, "Note saved to pouch").show()
                        isCreateNoteOpen = false
                    }
                } else {
                    rawDocuments = listOf(newDoc) + rawDocuments
                    Toast.makeText(null, "Note saved to pouch").show()
                    isCreateNoteOpen = false
                }
            }
        )
    }

    // ─── Edit Note Dialog ─────────────────────────────────────────────────────
    editingNote?.let { note ->
        FluidGlossEditNoteDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { updatedTitle, updatedCategory, updatedContent, updatedPinned ->
                val payload = serializeNotePayload(updatedContent, updatedPinned)
                if (onUpdateDocument != null) {
                    onUpdateDocument(
                        note.id,
                        activeProjectId.ifBlank { note.rawDoc.projectId ?: "" },
                        mapOf(
                            "name" to updatedTitle.trim(),
                            "category" to updatedCategory,
                            "url" to payload
                        )
                    ) { success ->
                        if (success) {
                            Toast.makeText(null, "Note updated").show()
                            editingNote = null
                        } else {
                            Toast.makeText(null, "Failed to update note").show()
                        }
                    }
                } else {
                    rawDocuments = rawDocuments.map { doc ->
                        if (doc.id == note.id) doc.copy(
                            name = updatedTitle.trim(),
                            category = updatedCategory,
                            url = payload
                        ) else doc
                    }
                    Toast.makeText(null, "Note updated").show()
                    editingNote = null
                }
            }
        )
    }

    // ─── View Full Note Modal ─────────────────────────────────────────────────
    viewingNote?.let { note ->
        FluidGlossViewNoteModal(
            note = note,
            onDismiss = { viewingNote = null },
            onPinToggle = { handlePinToggle(note) },
            onEdit = {
                val current = note
                viewingNote = null
                editingNote = current
            },
            onDelete = {
                handleDelete(note.id, true)
                viewingNote = null
            }
        )
    }

    // ─── Add / Upload Document Dialog ─────────────────────────────────────────
    if (isAddDocOpen) {
        FluidGlossAddDocumentDialog(
            activeProjectId = activeProjectId,
            onDismiss = { isAddDocOpen = false },
            onSave = { name, category, url, sizeBytes, docType ->
                val newDoc = DocumentDto(
                    id = "doc_${rawDocuments.size + 1}",
                    projectId = activeProjectId.ifBlank { allProjects.firstOrNull()?.id ?: "" },
                    name = name.trim(),
                    url = url.trim(),
                    size = sizeBytes,
                    type = docType,
                    category = category
                )
                if (onAddDocument != null) {
                    onAddDocument(newDoc) {
                        Toast.makeText(null, "Document added to vault").show()
                        isAddDocOpen = false
                    }
                } else {
                    rawDocuments = listOf(newDoc) + rawDocuments
                    Toast.makeText(null, "Document added to vault").show()
                    isAddDocOpen = false
                }
            }
        )
    }

    // ─── Edit Document Dialog ─────────────────────────────────────────────────
    editingDoc?.let { doc ->
        FluidGlossEditDocumentDialog(
            doc = doc,
            onDismiss = { editingDoc = null },
            onSave = { updatedName, updatedCategory ->
                if (onUpdateDocument != null) {
                    onUpdateDocument(
                        doc.id ?: "",
                        activeProjectId.ifBlank { doc.projectId ?: "" },
                        mapOf(
                            "name" to updatedName.trim(),
                            "category" to updatedCategory
                        )
                    ) { success ->
                        if (success) {
                            Toast.makeText(null, "Document updated").show()
                            editingDoc = null
                        } else {
                            Toast.makeText(null, "Failed to update document").show()
                        }
                    }
                } else {
                    rawDocuments = rawDocuments.map { d ->
                        if (d.id == doc.id) d.copy(
                            name = updatedName.trim(),
                            category = updatedCategory
                        ) else d
                    }
                    Toast.makeText(null, "Document updated").show()
                    editingDoc = null
                }
            }
        )
    }
}

// ─── FluidGloss Document Card ───────────────────────────────────────────────

@Composable
fun FluidGlossDocumentCard(
    doc: DocumentDto,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPdf = doc.type.contains("pdf", ignoreCase = true) || doc.url.contains(".pdf", ignoreCase = true)
    val isImage = doc.type.startsWith("image", ignoreCase = true) ||
            doc.url.contains(".png", ignoreCase = true) ||
            doc.url.contains(".jpg", ignoreCase = true) ||
            doc.url.contains(".jpeg", ignoreCase = true)

    val (iconBg, iconTint, icon) = when {
        isPdf -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Outlined.PictureAsPdf)
        isImage -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Outlined.Image)
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.AutoMirrored.Outlined.InsertDriveFile)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Badge Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = doc.category.ifBlank { "General" },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isPdf) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = "PDF",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = doc.name,
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatPouchDate(doc.createdAt),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (doc.size > 0) {
                            Text(
                                text = "• ${formatFileSize(doc.size)}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Preview", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─── FluidGloss Note Card ───────────────────────────────────────────────────

@Composable
fun FluidGlossNoteCard(
    note: ParsedNote,
    onPinToggle: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val borderColor = if (note.pinned) Color(0xFFF59E0B) else GlassCardDefaultBorder
    val borderWidth = if (note.pinned) 1.5.dp else 1.dp

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = borderColor,
        borderWidth = borderWidth,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = note.category,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    if (note.pinned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PINNED",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                            tint = if (note.pinned) Color(0xFFD97706) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Recorded ${formatPouchDate(note.createdAt)}",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

// ─── FluidGloss Empty State ─────────────────────────────────────────────────

@Composable
fun FluidGlossEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                FluidGlossButton(
                    text = actionText,
                    onClick = onAction,
                    variant = FluidGlossButtonVariant.Primary,
                    size = FluidGlossButtonSize.Medium
                )
            }
        }
    }
}

// ─── Create Note Dialog ─────────────────────────────────────────────────────

@Composable
fun FluidGlossCreateNoteDialog(
    activeProjectId: String,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, content: String, pinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var content by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Create New Note",
        subtitle = "Field memo, spec, access code, or measurement",
        icon = Icons.AutoMirrored.Outlined.StickyNote2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FluidGlossTextField(
                value = title,
                onValueChange = { title = it },
                label = "Note Title *",
                placeholder = "e.g. Column Centering Specs",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NOTE_CATEGORIES) { cat ->
                        val isSelected = cat == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFD97706)
                            )
                        )
                    }
                }
            }

            FluidGlossTextField(
                value = content,
                onValueChange = { content = it },
                label = "Note Content *",
                placeholder = "Enter site coordinates, concrete mixes, access codes...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pinned = !pinned }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pinned,
                    onCheckedChange = { pinned = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Pin note to top",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Keep this memo highlighted at the top of the pouch",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FluidGlossButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FluidGlossButton(
                    text = "Save Note",
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onSave(title, category, content, pinned)
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    variant = FluidGlossButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Edit Note Dialog ───────────────────────────────────────────────────────

@Composable
fun FluidGlossEditNoteDialog(
    note: ParsedNote,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, content: String, pinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var category by remember { mutableStateOf(note.category) }
    var content by remember { mutableStateOf(note.content) }
    var pinned by remember { mutableStateOf(note.pinned) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Edit Note",
        icon = Icons.Outlined.Edit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FluidGlossTextField(
                value = title,
                onValueChange = { title = it },
                label = "Note Title *",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NOTE_CATEGORIES) { cat ->
                        val isSelected = cat == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFD97706)
                            )
                        )
                    }
                }
            }

            FluidGlossTextField(
                value = content,
                onValueChange = { content = it },
                label = "Note Content *",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pinned = !pinned }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pinned,
                    onCheckedChange = { pinned = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pin note to top of list",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FluidGlossButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FluidGlossButton(
                    text = "Update Note",
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onSave(title, category, content, pinned)
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    variant = FluidGlossButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── View Full Note Modal ───────────────────────────────────────────────────

@Composable
fun FluidGlossViewNoteModal(
    note: ParsedNote,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = note.title,
        subtitle = "Created ${formatPouchDate(note.createdAt)}",
        icon = Icons.AutoMirrored.Outlined.StickyNote2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                ) {
                    Text(
                        text = note.category,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (note.pinned) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Text(
                            text = "PINNED",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = note.content,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluidGlossButton(
                    text = "Copy",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(note.content))
                        Toast.makeText(null, "Copied to clipboard").show()
                    },
                    variant = FluidGlossButtonVariant.Secondary,
                    size = FluidGlossButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )

                FluidGlossButton(
                    text = "Edit",
                    onClick = onEdit,
                    variant = FluidGlossButtonVariant.Primary,
                    size = FluidGlossButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )

                FluidGlossButton(
                    text = "Delete",
                    onClick = onDelete,
                    variant = FluidGlossButtonVariant.Destructive,
                    size = FluidGlossButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Add / Upload Document Dialog ───────────────────────────────────────────

@Composable
fun FluidGlossAddDocumentDialog(
    activeProjectId: String,
    onDismiss: () -> Unit,
    onPickFile: (() -> Unit)? = null,
    onSave: (name: String, category: String, url: String, sizeBytes: Long, docType: String) -> Unit
) {
    var docName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Blueprints") }
    var fileUriString by remember { mutableStateOf("") }
    var detectedSize by remember { mutableLongStateOf(0L) }
    var docType by remember { mutableStateOf("pdf") }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Upload Document",
        subtitle = "Add blueprints, CAD drawings, invoices or contracts",
        icon = Icons.Outlined.UploadFile
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Local File Picker Trigger
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickFile?.invoke() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (fileUriString.isNotBlank()) "File Selected" else "Tap to Select Local File",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (fileUriString.isNotBlank()) fileUriString.takeLast(30) else "PDF, DWG, PNG, JPG supported",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FluidGlossTextField(
                value = docName,
                onValueChange = { docName = it },
                label = "Document Title *",
                placeholder = "e.g. Architectural Elevation Rev 2",
                modifier = Modifier.fillMaxWidth()
            )

            FluidGlossTextField(
                value = fileUriString,
                onValueChange = { fileUriString = it },
                label = "Document URL / File Path *",
                placeholder = "https://... or select file above",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DOCUMENT_CATEGORIES) { cat ->
                        val isSelected = cat == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FluidGlossButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FluidGlossButton(
                    text = "Save Document",
                    onClick = {
                        if (docName.isNotBlank() && fileUriString.isNotBlank()) {
                            onSave(docName, category, fileUriString, detectedSize, docType)
                        }
                    },
                    enabled = docName.isNotBlank() && fileUriString.isNotBlank(),
                    variant = FluidGlossButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Edit Document Dialog ───────────────────────────────────────────────────

@Composable
fun FluidGlossEditDocumentDialog(
    doc: DocumentDto,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(doc.name) }
    var category by remember { mutableStateOf(doc.category.ifBlank { "General" }) }

    FluidGlossDialog(
        onDismissRequest = onDismiss,
        title = "Edit Document",
        icon = Icons.Outlined.Edit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FluidGlossTextField(
                value = name,
                onValueChange = { name = it },
                label = "Document Name *",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DOCUMENT_CATEGORIES) { cat ->
                        val isSelected = cat == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FluidGlossButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FluidGlossButton(
                    text = "Update",
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, category)
                        }
                    },
                    enabled = name.isNotBlank(),
                    variant = FluidGlossButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
