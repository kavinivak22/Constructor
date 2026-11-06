package com.example.constructorapp.ui.screens.worklog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorklogDetailDialog(
    worklogWithDetails: WorklogWithDetails,
    projectName: String,
    creatorName: String? = null,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: ((WorklogWithDetails) -> Unit)? = null
) {
    val context = LocalContext.current
    val worklog = worklogWithDetails.worklog
    val laborEntries = worklogWithDetails.laborEntries
    val materials = worklogWithDetails.materials
    val photos = worklogWithDetails.photos

    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }

    // Resolve creator display name
    val displayCreatorName = remember(creatorName, worklog.createdBy) {
        if (!creatorName.isNullOrBlank()) {
            creatorName
        } else {
            val createdBy = worklog.createdBy.trim()
            val currentUserId = SupabaseClient.userId
            val currentEmail = SupabaseClient.userEmail
            if (createdBy.isBlank() || createdBy == "offline-user" || (currentUserId != null && createdBy == currentUserId)) {
                currentEmail?.substringBefore("@")
                    ?.replace(".", " ")
                    ?.replace("_", " ")
                    ?.split(" ")
                    ?.filter { it.isNotBlank() }
                    ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } ?: "You"
            } else if (createdBy.contains("@")) {
                createdBy.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            } else {
                val isUuid = createdBy.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
                if (!isUuid && createdBy.isNotBlank()) {
                    createdBy
                } else {
                    currentEmail?.substringBefore("@")
                        ?.replace(".", " ")
                        ?.replace("_", " ")
                        ?.split(" ")
                        ?.filter { it.isNotBlank() }
                        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } ?: "Site User"
                }
            }
        }
    }

    // Date parsing for Month ("FEB") & Day ("05")
    val (monthStr, dayStr, fullDateStr) = remember(worklog.date) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(worklog.date.substringBefore("T"))
            if (date != null) {
                Triple(
                    SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase(),
                    SimpleDateFormat("dd", Locale.getDefault()).format(date),
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                )
            } else {
                Triple("LOG", "--", worklog.date)
            }
        } catch (_: Exception) {
            Triple("LOG", "--", worklog.date)
        }
    }

    // Dynamic Title & Description matching webapp
    val title = remember(worklog.title, laborEntries, photos) {
        if (worklog.title.isNotBlank() &&
            worklog.title != "Daily Log" &&
            worklog.title != "Daily Site Log" &&
            worklog.title != "Daily Site Worklog"
        ) {
            worklog.title
        } else {
            val categories = laborEntries.mapNotNull { it.laborEntry.category?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (categories.isNotEmpty()) {
                categories.take(2).joinToString(" & ") + (if (categories.size > 2) "..." else "") + " Work"
            } else if (!photos.firstOrNull()?.caption.isNullOrBlank()) {
                photos.first().caption!!
            } else {
                "Daily Site Worklog"
            }
        }
    }

    val description = remember(laborEntries) {
        val raw = laborEntries.mapNotNull {
            val desc = it.laborEntry.workDescription?.trim()
            val cName = it.laborEntry.contractorName.trim()
            if (!desc.isNullOrBlank()) "$cName: $desc" else null
        }.joinToString(". ")
        if (raw.isNotBlank()) raw else "No detailed description provided for this day."
    }

    // Total worker headcount
    val totalWorkers = remember(laborEntries) {
        laborEntries.sumOf { entry -> entry.workerCounts.sumOf { it.count } }
    }

    // Role wage helper matching standard construction market rates
    fun getRoleRate(role: String): Double {
        val r = role.lowercase().trim()
        return when {
            r.contains("head mason") || r == "mason" || r.contains("masonry") -> 850.0
            r.contains("helper") || r.contains("coolie") || r == "mc" -> 600.0
            r == "fc" || r.contains("female") -> 500.0
            r.contains("carpenter") || r.contains("shuttering") -> 900.0
            r.contains("bar") || r.contains("steel") || r.contains("bender") -> 850.0
            r.contains("paint") -> 800.0
            r.contains("plumb") || r.contains("electr") -> 850.0
            r.contains("supervisor") || r.contains("foreman") -> 1000.0
            else -> 650.0
        }
    }

    // Material unit cost helper
    fun getMaterialRate(name: String): Double {
        val n = name.lowercase().trim()
        return when {
            n.contains("cement") -> 380.0
            n.contains("sand") || n.contains("m-sand") -> 55.0
            n.contains("gravel") || n.contains("aggregate") || n.contains("jelly") -> 45.0
            n.contains("steel") || n.contains("tmt") || n.contains("rebar") -> 68.0
            n.contains("brick") || n.contains("block") -> 35.0
            n.contains("paint") || n.contains("primer") -> 350.0
            n.contains("wood") || n.contains("plywood") -> 120.0
            else -> 100.0
        }
    }

    // Cost calculations matching webapp job-costing
    val laborCostBreakdown = remember(laborEntries) {
        var total = 0.0
        val items = laborEntries.map { entry ->
            val l = entry.laborEntry
            val isRate = l.payoutClass.equals("Rate", ignoreCase = true) || l.workType.equals("rate", ignoreCase = true)
            val entryTotal: Double = if (isRate) {
                l.totalPayout ?: ((l.workDoneQuantity ?: 0.0) * (l.payoutRate ?: 0.0))
            } else {
                entry.workerCounts.sumOf { wc -> wc.count * getRoleRate(wc.workerType) }
            }
            total += entryTotal
            entry to entryTotal
        }
        total to items
    }
    val totalLaborCost = laborCostBreakdown.first

    val materialsCostBreakdown = remember(materials) {
        var total = 0.0
        val items = materials.map { mat ->
            val rate = getMaterialRate(mat.materialName)
            val subtotal = mat.quantityConsumed * rate
            total += subtotal
            Triple(mat, rate, subtotal)
        }
        total to items
    }
    val totalMaterialsCost = materialsCostBreakdown.first

    val estimatedTotalCost = totalLaborCost + totalMaterialsCost

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(26.dp)),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Scrollable Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 1. Hero Image Section (Aspect 4:3) with Overlaid Badges
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            if (photos.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { photos.size })
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val photo = photos[page]
                                    val photoUrl = photo.remotePhotoUrl.ifBlank { photo.localFilePath ?: "" }
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(if (!photo.localFilePath.isNullOrBlank()) File(photo.localFilePath) else photoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = photo.caption ?: "Site Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                fullscreenPhotoIndex = page
                                            }
                                    )
                                }

                                // Multi-photo counter pill (e.g. "1/3")
                                if (photos.size > 1) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp),
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            } else {
                                // Empty state placeholder
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.Image,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "No site photos attached",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }

                            // Overlaid Date Pill (Top-Left)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shadowElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = monthStr,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = dayStr,
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            // Overlaid Close Button (Top-Right)
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                    .border(1.dp, Color(0x22000000), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 2. Content Details Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Meta Badges Row
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Project Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandPrimaryContainer,
                                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = projectName,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Workers Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "$totalWorkers Workers",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                // Materials Badge
                                if (materials.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = "${materials.size} Mats",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }

                                // Sync Status Badge (shown only if pending offline sync)
                                if (worklog.syncStatus != 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberWarning.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Sync,
                                                contentDescription = null,
                                                tint = AmberWarning,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "PENDING SYNC",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberWarning
                                            )
                                        }
                                    }
                                }
                            }

                            // Title & Description
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = title,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    lineHeight = 26.sp
                                )
                                Text(
                                    text = description,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.5.sp,
                                    color = TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }

                            // 3. Job-Costing "Cost of Work" Banner (Webapp Exact Match)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Rupee currency icon badge
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "₹",
                                                        fontFamily = OutfitFontFamily,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669)
                                                    )
                                                }
                                            }

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "JOB COST OF WORK",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857),
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Text(
                                                        text = "• Wage Profile Linked",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF059669)
                                                    )
                                                }
                                                Text(
                                                    text = "₹${"%,d".format(estimatedTotalCost.toLong())}",
                                                    fontFamily = OutfitFontFamily,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669),
                                                    lineHeight = 24.sp
                                                )
                                            }
                                        }
                                    }

                                    // Breakdown row pills
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.9f),
                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(Icons.Outlined.People, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(13.dp))
                                                Text(
                                                    text = "Labour:",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "₹${"%,d".format(totalLaborCost.toLong())}",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.9f),
                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                                                Text(
                                                    text = "Materials:",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "₹${"%,d".format(totalMaterialsCost.toLong())}",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 4. Labor & Activity Section
                            if (laborEntries.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.People, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "LABOR & ACTIVITY",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPrimary,
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        if (totalLaborCost > 0) {
                                            Text(
                                                text = "Total: ₹${"%,d".format(totalLaborCost.toLong())}",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }

                                    laborCostBreakdown.second.forEach { (laborWithCounts, entryCost) ->
                                        val entry = laborWithCounts.laborEntry
                                        val isRate = entry.payoutClass.equals("Rate", ignoreCase = true) || entry.workType.equals("rate", ignoreCase = true)

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Contractor & Badges
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = entry.contractorName,
                                                            fontFamily = OutfitFontFamily,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "(Wage Profile)",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        // Rate vs NMR Badge
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isRate) Color(0xFFECFDF5) else Color(0xFFF0F9FF),
                                                            border = BorderStroke(1.dp, if (isRate) Color(0xFFA7F3D0) else Color(0xFFBAE6FD))
                                                        ) {
                                                            Text(
                                                                text = if (isRate) "Rate Work" else "NMR Work",
                                                                fontFamily = PlusJakartaSansFontFamily,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isRate) Color(0xFF059669) else Color(0xFF0284C7),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        // Category Badge
                                                        if (!entry.category.isNullOrBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = BrandPrimaryContainer
                                                            ) {
                                                                Text(
                                                                    text = entry.category,
                                                                    fontFamily = PlusJakartaSansFontFamily,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = BrandPrimary,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Description
                                                if (!entry.workDescription.isNullOrBlank()) {
                                                    Text(
                                                        text = entry.workDescription,
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 12.5.sp,
                                                        color = TextPrimary,
                                                        lineHeight = 18.sp
                                                    )
                                                }

                                                // Work Done Quantity Highlight
                                                if (entry.workDoneQuantity != null && entry.workDoneQuantity > 0.0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = BrandPrimary.copy(alpha = 0.08f),
                                                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Work Done:",
                                                                fontFamily = PlusJakartaSansFontFamily,
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                            Text(
                                                                text = "${entry.workDoneQuantity} ${entry.workDoneUnit ?: "sq.ft"}",
                                                                fontFamily = PlusJakartaSansFontFamily,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BrandPrimary
                                                            )
                                                            if (entry.payoutRate != null && entry.payoutRate > 0.0) {
                                                                Text(
                                                                    text = "@ ₹${entry.payoutRate}",
                                                                    fontFamily = PlusJakartaSansFontFamily,
                                                                    fontSize = 11.sp,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Worker Attendance Pills
                                                val activeWorkers = laborWithCounts.workerCounts.filter { it.count > 0 }
                                                if (activeWorkers.isNotEmpty()) {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        activeWorkers.forEach { wc ->
                                                            val rate = getRoleRate(wc.workerType)
                                                            val sub = wc.count * rate
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color.White,
                                                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${wc.workerType}:",
                                                                        fontFamily = PlusJakartaSansFontFamily,
                                                                        fontSize = 11.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                    Text(
                                                                        text = "${wc.count}",
                                                                        fontFamily = PlusJakartaSansFontFamily,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = TextPrimary
                                                                    )
                                                                    Text(
                                                                        text = "(@ ₹${rate.toInt()} = ₹${"%,d".format(sub.toLong())})",
                                                                        fontFamily = PlusJakartaSansFontFamily,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = Color(0xFF059669)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Team Daily Wages Footer
                                                if (entryCost > 0) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        Text(
                                                            text = "Team Daily Wages: ",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                        Text(
                                                            text = "₹${"%,d".format(entryCost.toLong())}",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 5. Materials Consumed Section
                            if (materials.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "MATERIALS CONSUMED",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706),
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        if (totalMaterialsCost > 0) {
                                            Text(
                                                text = "Total: ₹${"%,d".format(totalMaterialsCost.toLong())}",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            materialsCostBreakdown.second.forEachIndexed { idx, (mat, rate, itemTotal) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = mat.materialName,
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "₹${rate.toInt()} per ${mat.unit ?: "unit"}",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 10.5.sp,
                                                            color = TextSecondary
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "${mat.quantityConsumed} ${mat.unit ?: ""}",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "₹${"%,d".format(itemTotal.toLong())}",
                                                            fontFamily = PlusJakartaSansFontFamily,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }
                                                }

                                                if (idx < materials.size - 1) {
                                                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Photo Attachments Thumbnail Strip (if multiple photos)
                            if (photos.size > 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "SITE PHOTO PROOF (${photos.size})",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemsIndexed(photos) { idx, p ->
                                            val pUrl = p.remotePhotoUrl.ifBlank { p.localFilePath ?: "" }
                                            Column(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .clickable {
                                                        fullscreenPhotoIndex = idx
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(if (!p.localFilePath.isNullOrBlank()) File(p.localFilePath) else pUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = p.caption,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(110.dp, 80.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                                )
                                                if (!p.caption.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = p.caption,
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 7. Footer Posted Timestamp & Creator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Posted on $fullDateStr",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (displayCreatorName.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "By $displayCreatorName",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button (Icon only)
                    IconButton(
                        onClick = { onDelete(worklog.id) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(RoseDanger.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = RoseDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit Worklog Button (Icon only)
                        if (onEdit != null) {
                            IconButton(
                                onClick = { onEdit(worklogWithDetails) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Close Button
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(
                                text = "Close",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Modern Photo Lightbox
    if (fullscreenPhotoIndex != null && photos.isNotEmpty()) {
        ModernWorklogPhotoViewer(
            photos = photos,
            initialIndex = fullscreenPhotoIndex!!.coerceIn(0, photos.lastIndex),
            title = title,
            dateLabel = fullDateStr,
            onDismiss = { fullscreenPhotoIndex = null }
        )
    }
}
