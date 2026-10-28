package com.example.constructorapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*

data class StageCheckItem(
    val name: String,
    var isDone: Boolean
)

data class ConstructionStage(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val checks: List<StageCheckItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingPlanScreen(
    projectId: String,
    projectName: String,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Quality Stages, 1: Blueprint Viewer
    var selectedFloor by remember { mutableStateOf("Ground Floor Plan") }

    val stages = remember {
        mutableStateListOf(
            ConstructionStage(
                title = "1. Substructure & Foundation",
                icon = Icons.Default.Foundation,
                checks = listOf(
                    StageCheckItem("Soil excavation to firm stratum", true),
                    StageCheckItem("P.C.C. 1:4:8 leveling bed casting", true),
                    StageCheckItem("Footing rebar mat & column starter placement", true),
                    StageCheckItem("Concrete footing casting with mechanical needle vibration", true),
                    StageCheckItem("Backfilling & ramming in layers with water compaction", false)
                )
            ),
            ConstructionStage(
                title = "2. Plinth Beam & Damp-Proofing",
                icon = Icons.Default.LinearScale,
                checks = listOf(
                    StageCheckItem("Plinth beam shuttering & line alignment", true),
                    StageCheckItem("Plinth rebar cages with stirrups & 25mm cover blocks", true),
                    StageCheckItem("M20 plinth concrete casting", true),
                    StageCheckItem("Anti-termite chemical treatment along perimeter", false),
                    StageCheckItem("Damp-Proof Course (DPC) 50mm application", false)
                )
            ),
            ConstructionStage(
                title = "3. Superstructure Columns & Walls",
                icon = Icons.Default.ViewColumn,
                checks = listOf(
                    StageCheckItem("Column starter marking from center line grid", true),
                    StageCheckItem("Column vertical rebar tie & lap length verification", true),
                    StageCheckItem("Shuttering plumb bob check on all four faces", true),
                    StageCheckItem("Column pour up to beam bottom & 7 days wet gunny curing", false),
                    StageCheckItem("First class brick masonry with 1:6 cement mortar", false)
                )
            ),
            ConstructionStage(
                title = "4. Roof Slab RCC & Beams",
                icon = Icons.Default.Roofing,
                checks = listOf(
                    StageCheckItem("Prop staging & steel shuttering sheet leveling", false),
                    StageCheckItem("Shuttering oil application & gap tape sealing", false),
                    StageCheckItem("Beam reinforcement cages & top crank bars", false),
                    StageCheckItem("Electrical conduit box routing & fan hook tie", false),
                    StageCheckItem("M20 / M25 Ready Mix / Site Mix slab casting", false),
                    StageCheckItem("Ponding bund formation & 14 days curing", false)
                )
            ),
            ConstructionStage(
                title = "5. MEP & Plastering",
                icon = Icons.Default.Plumbing,
                checks = listOf(
                    StageCheckItem("Electrical wall wall chasing & conduit pipe fixing", false),
                    StageCheckItem("Concealed CPVC/UPVC plumbing line hydro-pressure test", false),
                    StageCheckItem("Internal ceiling & wall 12mm smooth cement plastering", false),
                    StageCheckItem("External 20mm double-coat sand-faced waterproof plastering", false)
                )
            ),
            ConstructionStage(
                title = "6. Finishing & Handover",
                icon = Icons.Default.CheckCircle,
                checks = listOf(
                    StageCheckItem("Bathroom sunken slab chemical waterproofing", false),
                    StageCheckItem("Vitrified tile / granite flooring & skirting", false),
                    StageCheckItem("2 coats wall putty + 2 coats premium emulsion paint", false),
                    StageCheckItem("Electrical switches, sanitaryware & doors fitting", false),
                    StageCheckItem("Final QA inspection & handover snag checklist", false)
                )
            )
        )
    }

    val totalChecks = stages.sumOf { it.checks.size }
    val completedChecks = stages.sumOf { it.checks.count { c -> c.isDone } }
    val progressPct = if (totalChecks > 0) (completedChecks.toFloat() / totalChecks) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Building Construction Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "$projectName • Quality & Stages",
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
        containerColor = SlateBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Construction Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SITE EXECUTION PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${(progressPct * 100).toInt()}% COMPLETE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progressPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldSuccess,
                            trackColor = SlateSurfaceSubtle
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$completedChecks of $totalChecks Quality Inspections Passed",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Current Stage: Superstructure",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SkyInfo
                            )
                        }
                    }
                }
            }

            // Tab Selector: Stages vs Blueprint Viewer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .padding(4.dp)
                ) {
                    TabPill(
                        selected = selectedTab == 0,
                        title = "Civil Stages & Quality",
                        icon = Icons.Default.Checklist,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabPill(
                        selected = selectedTab == 1,
                        title = "Architectural Blueprint",
                        icon = Icons.Default.Layers,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
            }

            if (selectedTab == 0) {
                // Civil Stages Accordion
                itemsIndexed(stages) { stageIdx, stage ->
                    var isExpanded by remember { mutableStateOf(stageIdx <= 2) }
                    val stageCompleted = stage.checks.count { it.isDone }
                    val stageTotal = stage.checks.size
                    val stagePct = stageCompleted.toFloat() / stageTotal

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (stagePct == 1f) EmeraldSuccess.copy(alpha = 0.2f) else BrandPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (stagePct == 1f) Icons.Default.CheckCircle else stage.icon,
                                        contentDescription = null,
                                        tint = if (stagePct == 1f) EmeraldSuccess else BrandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stage.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$stageCompleted / $stageTotal checks passed (${(stagePct * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    stage.checks.forEachIndexed { checkIdx, checkItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SlateBackground)
                                                .clickable {
                                                    val updatedChecks = stage.checks.toMutableList()
                                                    updatedChecks[checkIdx] = checkItem.copy(isDone = !checkItem.isDone)
                                                    stages[stageIdx] = stage.copy(checks = updatedChecks)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checkItem.isDone,
                                                onCheckedChange = { isChecked ->
                                                    val updatedChecks = stage.checks.toMutableList()
                                                    updatedChecks[checkIdx] = checkItem.copy(isDone = isChecked)
                                                    stages[stageIdx] = stage.copy(checks = updatedChecks)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = EmeraldSuccess,
                                                    uncheckedColor = TextSecondary,
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = checkItem.name,
                                                fontSize = 12.sp,
                                                color = if (checkItem.isDone) TextSecondary else TextPrimary,
                                                fontWeight = if (checkItem.isDone) FontWeight.Normal else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Blueprint Viewer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ARCHITECTURAL BLUEPRINT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                                Text(
                                    text = "Scale 1:100",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Floor Level Filter
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Ground Floor Plan", "First Floor Plan", "Terrace Plan").forEach { floor ->
                                    FilterChip(
                                        selected = selectedFloor == floor,
                                        onClick = { selectedFloor = floor },
                                        label = { Text(floor, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = SlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Blueprint Canvas Renderer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, SkyInfo.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height

                                    // Architectural Grid lines
                                    val gridSpacing = 40.dp.toPx()
                                    var x = 0f
                                    while (x < canvasWidth) {
                                        drawLine(
                                            color = Color(0x1A38BDF8),
                                            start = Offset(x, 0f),
                                            end = Offset(x, canvasHeight),
                                            strokeWidth = 1f
                                        )
                                        x += gridSpacing
                                    }
                                    var y = 0f
                                    while (y < canvasHeight) {
                                        drawLine(
                                            color = Color(0x1A38BDF8),
                                            start = Offset(0f, y),
                                            end = Offset(canvasWidth, y),
                                            strokeWidth = 1f
                                        )
                                        y += gridSpacing
                                    }

                                    // Outer Boundary Walls (Double line)
                                    val margin = 30.dp.toPx()
                                    val planW = canvasWidth - (margin * 2)
                                    val planH = canvasHeight - (margin * 2)

                                    drawRect(
                                        color = SkyInfo,
                                        topLeft = Offset(margin, margin),
                                        size = Size(planW, planH),
                                        style = Stroke(width = 4f)
                                    )

                                    // Internal Partitions
                                    val midX = margin + (planW * 0.55f)
                                    val midY = margin + (planH * 0.5f)

                                    // Living / Bedroom divider
                                    drawLine(
                                        color = SkyInfo,
                                        start = Offset(midX, margin),
                                        end = Offset(midX, margin + planH),
                                        strokeWidth = 3f
                                    )

                                    // Kitchen / Living horizontal divider
                                    drawLine(
                                        color = SkyInfo,
                                        start = Offset(margin, midY),
                                        end = Offset(midX, midY),
                                        strokeWidth = 3f
                                    )

                                    // Master Bedroom toilet block
                                    val toiletY = margin + (planH * 0.65f)
                                    drawLine(
                                        color = SkyInfo,
                                        start = Offset(midX, toiletY),
                                        end = Offset(margin + planW, toiletY),
                                        strokeWidth = 2.5f
                                    )

                                    // Column markers (Red concrete columns at junctions)
                                    val colSize = 10.dp.toPx()
                                    val colPoints = listOf(
                                        Offset(margin, margin),
                                        Offset(midX, margin),
                                        Offset(margin + planW, margin),
                                        Offset(margin, midY),
                                        Offset(midX, midY),
                                        Offset(margin, margin + planH),
                                        Offset(midX, margin + planH),
                                        Offset(margin + planW, margin + planH),
                                        Offset(margin + planW, toiletY)
                                    )
                                    colPoints.forEach { pt ->
                                        drawRect(
                                            color = RoseDanger,
                                            topLeft = Offset(pt.x - (colSize / 2), pt.y - (colSize / 2)),
                                            size = Size(colSize, colSize)
                                        )
                                    }

                                    // Door swing arc (dashed)
                                    drawArc(
                                        color = BrandPrimary,
                                        startAngle = 0f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        topLeft = Offset(margin + 20f, margin + 20f),
                                        size = Size(60f, 60f),
                                        style = Stroke(
                                            width = 2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }

                                // Overlay Room Labels
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(36.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                        // Living Hall
                                        Box(
                                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("LIVING ROOM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("16'0\" x 14'6\"", fontSize = 9.sp, color = SkyInfo)
                                            }
                                        }
                                        // Master Bedroom
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("MASTER BED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("14'0\" x 12'0\"", fontSize = 9.sp, color = SkyInfo)
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                        // Modular Kitchen
                                        Box(
                                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("KITCHEN & DINING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("12'0\" x 10'0\"", fontSize = 9.sp, color = SkyInfo)
                                            }
                                        }
                                        // Attached Bath & Utility
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("TOILET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("8'0\" x 5'0\"", fontSize = 8.sp, color = SkyInfo)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Legend Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                LegendItem(color = RoseDanger, label = "RCC Columns (C1)")
                                LegendItem(color = SkyInfo, label = "9\" Brick Walls")
                                LegendItem(color = BrandPrimary, label = "Door Swings (D1)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}
