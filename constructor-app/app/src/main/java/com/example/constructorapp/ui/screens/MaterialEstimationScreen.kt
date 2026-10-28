package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import java.text.NumberFormat
import java.util.Locale

data class EstimatedMaterialItem(
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val unitRate: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialEstimationScreen(
    initialProjectId: String?,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    var selectedProjectId by remember {
        mutableStateOf(initialProjectId ?: projects.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(Unit) {
        if (projects.isEmpty()) {
            viewModel.loadProjects()
        }
    }

    LaunchedEffect(projects) {
        if (selectedProjectId.isEmpty() && projects.isNotEmpty()) {
            selectedProjectId = projects.first().id
        }
    }

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Civil Calculators, 1: AI Estimator
    var calculatorTab by remember { mutableIntStateOf(0) } // 0: Concrete, 1: Brickwork, 2: Plastering, 3: Rebar Steel

    // Review & Import list
    val materialList = remember {
        mutableStateListOf(
            EstimatedMaterialItem("OPC 53 Cement", "Cement", 150.0, "bags", 410.0),
            EstimatedMaterialItem("River Sand / M-Sand", "Aggregate", 450.0, "cft", 55.0),
            EstimatedMaterialItem("20mm Blue Metal Stone", "Aggregate", 600.0, "cft", 48.0),
            EstimatedMaterialItem("Fe 550D TMT Rebar", "Steel", 1250.0, "kg", 68.0)
        )
    }

    var isImporting by remember { mutableStateOf(false) }

    // Concrete Calculator State
    var concLength by remember { mutableStateOf("20") }
    var concWidth by remember { mutableStateOf("15") }
    var concDepth by remember { mutableStateOf("0.5") }
    var concMix by remember { mutableStateOf("M20 (1:1.5:3)") } // M15, M20, M25

    // Brickwork Calculator State
    var brickLength by remember { mutableStateOf("30") }
    var brickHeight by remember { mutableStateOf("10") }
    var brickThickness by remember { mutableStateOf("9 inch") } // 9 inch or 4.5 inch
    var brickMortarRatio by remember { mutableStateOf("1:6") }

    // Plastering Calculator State
    var plasterArea by remember { mutableStateOf("500") }
    var plasterThickness by remember { mutableStateOf("12mm") } // 12mm, 15mm, 20mm
    var plasterRatio by remember { mutableStateOf("1:4") }

    // Steel Calculator State
    var steelRccVolume by remember { mutableStateOf("10") } // cu.m
    var steelMemberType by remember { mutableStateOf("Slab (1.0%)") } // Slab 1.0%, Beam 2.0%, Column 2.5%, Footing 0.8%

    // AI Estimator State
    var aiProjectType by remember { mutableStateOf("Residential 2BHK Villa") }
    var aiProjectSize by remember { mutableStateOf("1200") }
    var aiProjectLocation by remember { mutableStateOf("Chennai, TN") }
    var aiSpecificReqs by remember { mutableStateOf("Framed RCC structure with red brick masonry, teak wood doors, vitrified tile flooring.") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Material Estimation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Civil Calculation & AI Takeoff",
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
            // Target Project Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TARGET PROJECT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (projects.isEmpty()) {
                            Text(
                                text = "Loading projects or none found...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            val currentProject = projects.find { it.id == selectedProjectId } ?: projects.first()

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SlateBackground,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentProject.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandPrimary)
                                    }
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(SlateSurface)
                                ) {
                                    projects.forEach { proj ->
                                        DropdownMenuItem(
                                            text = { Text(proj.name, color = TextPrimary) },
                                            onClick = {
                                                selectedProjectId = proj.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mode Selector Tabs (Civil Calculators vs AI Estimator)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .padding(4.dp)
                ) {
                    TabPill(
                        selected = selectedMode == 0,
                        title = "Civil Calculators",
                        icon = Icons.Default.Calculate,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 0 }
                    )
                    TabPill(
                        selected = selectedMode == 1,
                        title = "AI Spec Estimator",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 1 }
                    )
                }
            }

            if (selectedMode == 0) {
                // Calculator Sub-Tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = calculatorTab,
                        containerColor = SlateBackground,
                        contentColor = BrandPrimary,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        val tabs = listOf("Concrete", "Brickwork", "Plastering", "Steel / Rebar")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = calculatorTab == index,
                                onClick = { calculatorTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (calculatorTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (calculatorTab == index) BrandPrimary else TextSecondary
                                    )
                                }
                            )
                        }
                    }
                }

                // Calculator Active View
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (calculatorTab) {
                                0 -> {
                                    // Concrete Calculator
                                    Text("Concrete Volume & Material Takeoff", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = concLength,
                                            onValueChange = { concLength = it },
                                            label = { Text("Length (ft)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = calculatorFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = concWidth,
                                            onValueChange = { concWidth = it },
                                            label = { Text("Width (ft)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = calculatorFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = concDepth,
                                            onValueChange = { concDepth = it },
                                            label = { Text("Depth (ft)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = calculatorFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Select Mix Ratio", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("M15 (1:2:4)", "M20 (1:1.5:3)", "M25 (1:1:2)").forEach { mix ->
                                            FilterChip(
                                                selected = concMix == mix,
                                                onClick = { concMix = mix },
                                                label = { Text(mix, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    // Calculations
                                    val l = concLength.toDoubleOrNull() ?: 0.0
                                    val w = concWidth.toDoubleOrNull() ?: 0.0
                                    val d = concDepth.toDoubleOrNull() ?: 0.0
                                    val wetVolumeCft = l * w * d
                                    val dryVolumeCft = wetVolumeCft * 1.54 // dry volume factor 1.54

                                    val (cementRatio, sandRatio, aggRatio) = when {
                                        concMix.startsWith("M15") -> Triple(1.0, 2.0, 4.0)
                                        concMix.startsWith("M20") -> Triple(1.0, 1.5, 3.0)
                                        else -> Triple(1.0, 1.0, 2.0)
                                    }
                                    val totalParts = cementRatio + sandRatio + aggRatio
                                    val cementBags = if (totalParts > 0) (dryVolumeCft * (cementRatio / totalParts)) / 1.226 else 0.0
                                    val sandCft = if (totalParts > 0) dryVolumeCft * (sandRatio / totalParts) else 0.0
                                    val aggCft = if (totalParts > 0) dryVolumeCft * (aggRatio / totalParts) else 0.0
                                    val waterLiters = cementBags * 28.0

                                    Spacer(modifier = Modifier.height(16.dp))
                                    ResultBox(
                                        title = "Estimated Concrete Output",
                                        items = listOf(
                                            "Wet Volume" to String.format(Locale.US, "%.1f cu.ft (%.2f m³)", wetVolumeCft, wetVolumeCft * 0.0283168),
                                            "Cement Required" to String.format(Locale.US, "%.1f Bags (50kg)", cementBags),
                                            "Sand (Fine Aggregate)" to String.format(Locale.US, "%.1f cu.ft (%.2f tons)", sandCft, sandCft * 0.045),
                                            "Blue Metal (20mm Aggregate)" to String.format(Locale.US, "%.1f cu.ft (%.2f tons)", aggCft, aggCft * 0.048),
                                            "Water" to String.format(Locale.US, "%.0f Liters", waterLiters)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (cementBags > 0) {
                                                materialList.add(EstimatedMaterialItem("Cement Bags (Concrete ${concMix.take(3)})", "Cement", Math.round(cementBags * 10) / 10.0, "bags", 410.0))
                                                materialList.add(EstimatedMaterialItem("Sand (Concrete ${concMix.take(3)})", "Aggregate", Math.round(sandCft * 10) / 10.0, "cft", 55.0))
                                                materialList.add(EstimatedMaterialItem("20mm Aggregate (Concrete ${concMix.take(3)})", "Aggregate", Math.round(aggCft * 10) / 10.0, "cft", 48.0))
                                                Toast.makeText(context, "Added concrete materials to import list", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push to Materials Import List")
                                    }
                                }
                                1 -> {
                                    // Brickwork Calculator
                                    Text("Brick Masonry Estimation", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = brickLength,
                                            onValueChange = { brickLength = it },
                                            label = { Text("Wall Length (ft)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = calculatorFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = brickHeight,
                                            onValueChange = { brickHeight = it },
                                            label = { Text("Wall Height (ft)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = calculatorFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("9 inch (Main)", "4.5 inch (Partition)").forEach { thick ->
                                            FilterChip(
                                                selected = brickThickness == thick,
                                                onClick = { brickThickness = thick },
                                                label = { Text(thick, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    val bl = brickLength.toDoubleOrNull() ?: 0.0
                                    val bh = brickHeight.toDoubleOrNull() ?: 0.0
                                    val wallArea = bl * bh
                                    val isNineInch = brickThickness.startsWith("9")
                                    val bricksCount = if (isNineInch) wallArea * 9.5 else wallArea * 4.75
                                    val cementBags = if (isNineInch) wallArea * 0.28 else wallArea * 0.14
                                    val sandCft = if (isNineInch) wallArea * 1.25 else wallArea * 0.62

                                    Spacer(modifier = Modifier.height(16.dp))
                                    ResultBox(
                                        title = "Estimated Brickwork Output",
                                        items = listOf(
                                            "Wall Face Area" to String.format(Locale.US, "%.1f sq.ft", wallArea),
                                            "Standard Modular Bricks" to String.format(Locale.US, "%.0f Nos (incl 5%% waste)", bricksCount * 1.05),
                                            "Cement for Mortar (1:6)" to String.format(Locale.US, "%.1f Bags", cementBags),
                                            "M-Sand for Mortar" to String.format(Locale.US, "%.1f cu.ft", sandCft)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (bricksCount > 0) {
                                                materialList.add(EstimatedMaterialItem("Standard Clay Bricks", "Masonry", Math.round(bricksCount * 1.05).toDouble(), "nos", 9.5))
                                                materialList.add(EstimatedMaterialItem("Cement (Brickwork Mortar)", "Cement", Math.round(cementBags * 10) / 10.0, "bags", 410.0))
                                                materialList.add(EstimatedMaterialItem("Sand (Brickwork Mortar)", "Aggregate", Math.round(sandCft * 10) / 10.0, "cft", 55.0))
                                                Toast.makeText(context, "Added brickwork materials to import list", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push to Materials Import List")
                                    }
                                }
                                2 -> {
                                    // Plastering Calculator
                                    Text("Plastering Mortar Estimation", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = plasterArea,
                                        onValueChange = { plasterArea = it },
                                        label = { Text("Plastering Surface Area (sq.ft)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = calculatorFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("12mm (Internal)", "15mm (Ceiling)", "20mm (External)").forEach { th ->
                                            FilterChip(
                                                selected = plasterThickness == th,
                                                onClick = { plasterThickness = th },
                                                label = { Text(th, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    val pArea = plasterArea.toDoubleOrNull() ?: 0.0
                                    val thicknessMultiplier = when {
                                        plasterThickness.startsWith("15") -> 1.25
                                        plasterThickness.startsWith("20") -> 1.66
                                        else -> 1.0
                                    }
                                    val pCementBags = (pArea * 0.09) * thicknessMultiplier
                                    val pSandCft = (pArea * 0.45) * thicknessMultiplier

                                    Spacer(modifier = Modifier.height(16.dp))
                                    ResultBox(
                                        title = "Estimated Plastering Output",
                                        items = listOf(
                                            "Surface Area" to String.format(Locale.US, "%.1f sq.ft", pArea),
                                            "Cement Required (1:4 Mortar)" to String.format(Locale.US, "%.1f Bags", pCementBags),
                                            "Plastering Sand (Screened)" to String.format(Locale.US, "%.1f cu.ft", pSandCft)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (pCementBags > 0) {
                                                materialList.add(EstimatedMaterialItem("Plastering Cement (${plasterThickness.take(4)})", "Cement", Math.round(pCementBags * 10) / 10.0, "bags", 410.0))
                                                materialList.add(EstimatedMaterialItem("Plastering Fine Sand", "Aggregate", Math.round(pSandCft * 10) / 10.0, "cft", 60.0))
                                                Toast.makeText(context, "Added plastering materials to import list", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push to Materials Import List")
                                    }
                                }
                                3 -> {
                                    // Steel / Rebar Calculator
                                    Text("RCC Steel & Rebar Weight Takeoff", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = steelRccVolume,
                                        onValueChange = { steelRccVolume = it },
                                        label = { Text("RCC Volume (cu.meters)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = calculatorFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Slab (1.0%)", "Beam (2.0%)", "Column (2.5%)", "Footing (0.8%)").forEach { mem ->
                                            FilterChip(
                                                selected = steelMemberType == mem,
                                                onClick = { steelMemberType = mem },
                                                label = { Text(mem, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BrandPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    val rccVol = steelRccVolume.toDoubleOrNull() ?: 0.0
                                    val steelDensity = 7850.0 // kg/m3
                                    val percentage = when {
                                        steelMemberType.startsWith("Beam") -> 0.02
                                        steelMemberType.startsWith("Column") -> 0.025
                                        steelMemberType.startsWith("Footing") -> 0.008
                                        else -> 0.01 // Slab
                                    }
                                    val totalSteelKg = rccVol * percentage * steelDensity
                                    val steelTonnes = totalSteelKg / 1000.0

                                    Spacer(modifier = Modifier.height(16.dp))
                                    ResultBox(
                                        title = "Estimated Rebar Output",
                                        items = listOf(
                                            "RCC Volume" to String.format(Locale.US, "%.2f m³", rccVol),
                                            "Steel Reinforcement Weight" to String.format(Locale.US, "%.1f kg", totalSteelKg),
                                            "Metric Tonnes" to String.format(Locale.US, "%.3f Tonnes", steelTonnes),
                                            "Binding Wire (approx 1%%)" to String.format(Locale.US, "%.1f kg", totalSteelKg * 0.01)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (totalSteelKg > 0) {
                                                materialList.add(EstimatedMaterialItem("Fe 550D TMT Steel (${steelMemberType.substringBefore(" ")})", "Steel", Math.round(totalSteelKg * 10) / 10.0, "kg", 68.0))
                                                materialList.add(EstimatedMaterialItem("Binding Wire 18G", "Steel", Math.round(totalSteelKg * 0.01 * 10) / 10.0, "kg", 85.0))
                                                Toast.makeText(context, "Added steel materials to import list", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push to Materials Import List")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // AI Spec Estimator Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Project Specifications", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = aiProjectType,
                                onValueChange = { aiProjectType = it },
                                label = { Text("Project Type / Building Classification", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = calculatorFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = aiProjectSize,
                                    onValueChange = { aiProjectSize = it },
                                    label = { Text("Built-up Area (sq.ft)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = calculatorFieldColors()
                                )
                                OutlinedTextField(
                                    value = aiProjectLocation,
                                    onValueChange = { aiProjectLocation = it },
                                    label = { Text("Site Location", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = calculatorFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = aiSpecificReqs,
                                onValueChange = { aiSpecificReqs = it },
                                label = { Text("Engineering & Material Requirements", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = calculatorFieldColors()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isGeneratingAi = true
                                    val size = aiProjectSize.toDoubleOrNull() ?: 1000.0
                                    // Generate standardized civil engineering benchmark items
                                    materialList.clear()
                                    materialList.addAll(
                                        listOf(
                                            EstimatedMaterialItem("OPC 53 Grade Cement", "Cement", size * 0.45, "bags", 415.0),
                                            EstimatedMaterialItem("River Sand / Clean M-Sand", "Aggregate", size * 1.8, "cft", 58.0),
                                            EstimatedMaterialItem("20mm Graded Granite Stones", "Aggregate", size * 1.35, "cft", 50.0),
                                            EstimatedMaterialItem("Fe 550D High Ductility Steel", "Steel", size * 4.2, "kg", 68.0),
                                            EstimatedMaterialItem("First Class Wire-cut Red Bricks", "Masonry", size * 18.0, "nos", 9.8),
                                            EstimatedMaterialItem("Vitrified Double-Charge Tiles 2x2", "Finishing", size * 1.1, "sq.ft", 65.0),
                                            EstimatedMaterialItem("Acrylic Emulsion Exterior Paint", "Paint", size * 0.08, "liters", 320.0)
                                        )
                                    )
                                    isGeneratingAi = false
                                    Toast.makeText(context, "Generated benchmark material takeoff for $size sq.ft", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isGeneratingAi
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating Takeoff...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate AI Materials Takeoff")
                                }
                            }
                        }
                    }
                }
            }

            // Structured Materials Table & Direct Import Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED MATERIALS INVENTORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${materialList.size} items ready for project import",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            materialList.add(
                                EstimatedMaterialItem(
                                    name = "Custom Material",
                                    category = "General",
                                    quantity = 10.0,
                                    unit = "units",
                                    unitRate = 100.0
                                )
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SlateSurfaceSubtle,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(materialList) { index, item ->
                MaterialRowCard(
                    item = item,
                    onDelete = { materialList.removeAt(index) },
                    onUpdate = { updated -> materialList[index] = updated }
                )
            }

            // Grand Total and Import Button
            item {
                val totalCost = materialList.sumOf { it.quantity * it.unitRate }
                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Total Cost:", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = currencyFormatter.format(totalCost),
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (selectedProjectId.isEmpty()) {
                                    Toast.makeText(context, "Please select a target project first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (materialList.isEmpty()) {
                                    Toast.makeText(context, "No materials to import", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isImporting = true
                                var count = 0
                                materialList.forEach { mat ->
                                    viewModel.saveProjectMaterial(
                                        ProjectMaterialDto(
                                            projectId = selectedProjectId,
                                            name = mat.name,
                                            quantity = mat.quantity,
                                            unit = mat.unit
                                        )
                                    ) { success ->
                                        if (success) count++
                                    }
                                }
                                isImporting = false
                                Toast.makeText(context, "Successfully queued ${materialList.size} items to project materials!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isImporting && materialList.isNotEmpty()
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importing to Project...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import to Project Inventory (${materialList.size} Items)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialRowCard(
    item: EstimatedMaterialItem,
    onDelete: () -> Unit,
    onUpdate: (EstimatedMaterialItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = item.category,
                        color = SkyInfo,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity.toString(),
                    onValueChange = {
                        val q = it.toDoubleOrNull() ?: item.quantity
                        onUpdate(item.copy(quantity = q))
                    },
                    label = { Text("Qty (${item.unit})", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = calculatorFieldColors()
                )

                OutlinedTextField(
                    value = item.unitRate.toString(),
                    onValueChange = {
                        val r = it.toDoubleOrNull() ?: item.unitRate
                        onUpdate(item.copy(unitRate = r))
                    },
                    label = { Text("Rate (₹)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = calculatorFieldColors()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Subtotal", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "₹${(item.quantity * item.unitRate).toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ResultBox(title: String, items: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 12.sp, color = TextSecondary)
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
fun TabPill(
    selected: Boolean,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrandPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
fun calculatorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = SlateBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = BrandPrimary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = BrandPrimary
)
