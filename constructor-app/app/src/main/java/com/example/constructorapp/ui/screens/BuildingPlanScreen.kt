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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.GlassCard

// ─── Legacy Data Classes (Kept for unit test compatibility) ───────────────────
data class StageCheckItem(
    val name: String,
    var isDone: Boolean
)

data class ConstructionStage(
    val title: String,
    val icon: ImageVector,
    val checks: List<StageCheckItem>
)

// ─── Modern Building Plan Domain Models (Matching Webapp AIProcess) ───────────
data class QualityCheckpoint(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

data class BuildingTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String = "pending", // "pending", "in-progress", "completed"
    val checklists: List<QualityCheckpoint>
) {
    val isCompleted: Boolean
        get() = status.equals("completed", ignoreCase = true)
}

data class BuildingStage(
    val id: String,
    val title: String,
    val weight: Int,
    val icon: ImageVector,
    val tasks: List<BuildingTask>
)

data class PlanTemplate(
    val id: String,
    val name: String,
    val description: String,
    val buildingType: String,
    val stages: List<BuildingStage>
)

// ─── Predefined Templates (Matching src/app/actions/ai-progress.ts) ──────────
val BuiltInPlanTemplates = listOf(
    PlanTemplate(
        id = "tmpl-framed-villa",
        name = "Residential Villa (Framed Structure)",
        description = "Standard RCC frame construction workflow with footings, plinth, columns, slabs, and MEP.",
        buildingType = "Residential Villa",
        stages = listOf(
            BuildingStage(
                id = "p1",
                title = "1. Substructure & Foundation",
                weight = 20,
                icon = Icons.Default.Foundation,
                tasks = listOf(
                    BuildingTask(
                        id = "t1_1",
                        title = "Site Clearance & Grid Marking",
                        description = "Survey boundary alignment, layout grid lines, and establish site zero benchmark.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("c1", "Verify site boundary & grid line offset pins", true),
                            QualityCheckpoint("c2", "Check excavation benchmark level against drawing", true)
                        )
                    ),
                    BuildingTask(
                        id = "t1_2",
                        title = "Earth Excavation & Footing Pit Digging",
                        description = "Excavate pit to firm hard stratum with proper slope shoring.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("c3", "Inspect hard strata soil bearing level", true),
                            QualityCheckpoint("c4", "Ensure pit shoring and anti-termite chemical spray", true)
                        )
                    ),
                    BuildingTask(
                        id = "t1_3",
                        title = "PCC Bed Concreting (1:4:8)",
                        description = "Cast 4-inch plain cement concrete leveling bed.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("c5", "Verify 4-inch PCC bed thickness and level", true),
                            QualityCheckpoint("c6", "Ensure water curing for at least 3 days", true)
                        )
                    ),
                    BuildingTask(
                        id = "t1_4",
                        title = "Footing Steel Rebar Mat & Column Starters",
                        description = "Tie high-yield deformed bar mesh and fix vertical column starter rebar.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("c7", "Verify mesh bar spacing (150mm c/c)", true),
                            QualityCheckpoint("c8", "Check 50mm cover block placement under mesh", true),
                            QualityCheckpoint("c9", "Check vertical column starter plumb alignment", true)
                        )
                    ),
                    BuildingTask(
                        id = "t1_5",
                        title = "Footing Concreting (M20) & Backfilling",
                        description = "Pour design concrete with needle vibrator compaction and backfill in layers.",
                        status = "in-progress",
                        checklists = listOf(
                            QualityCheckpoint("c10", "Vibrator compaction during concrete pour", true),
                            QualityCheckpoint("c11", "Soil backfilling in 150mm layers with compaction", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "p2",
                title = "2. Plinth Beam & Damp-Proofing",
                weight = 25,
                icon = Icons.Default.LinearScale,
                tasks = listOf(
                    BuildingTask(
                        id = "t2_1",
                        title = "Plinth Beam Shuttering & Rebar Cage",
                        description = "Fix beam shuttering, top/bottom steel, and stirrups at specified pitch.",
                        status = "in-progress",
                        checklists = listOf(
                            QualityCheckpoint("c12", "Verify top steel and stirrup spacing (100-150mm)", true),
                            QualityCheckpoint("c13", "Check 25mm concrete cover blocks on sides and bottom", true),
                            QualityCheckpoint("c14", "Damp-Proof Course (DPC) 50mm bitumen membrane", false)
                        )
                    ),
                    BuildingTask(
                        id = "t2_2",
                        title = "RCC Column Casting to Lintel Height",
                        description = "Erect column shuttering with plumb alignment and pour M20 concrete.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c15", "Check column shuttering plumb alignment using plumb-bob", false),
                            QualityCheckpoint("c16", "Verify 40mm column cover block placement", false),
                            QualityCheckpoint("c17", "7 days continuous wet gunny bag curing", false)
                        )
                    ),
                    BuildingTask(
                        id = "t2_3",
                        title = "Ground Floor Roof Slab & Beam Formwork",
                        description = "Erect steel prop staging, deck shuttering, and reinforcement mat.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c18", "Check props verticality & sole plate stability", false),
                            QualityCheckpoint("c19", "Inspect main & distribution bar bent-up chairs", false),
                            QualityCheckpoint("c20", "Electrical conduit pipes & fan box placement", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "p3",
                title = "3. Brick Masonry & Partition Walls",
                weight = 20,
                icon = Icons.Default.ViewColumn,
                tasks = listOf(
                    BuildingTask(
                        id = "t3_1",
                        title = "First Class Brick Masonry (1:6 Mortar)",
                        description = "Lay 9-inch external walls and 4.5-inch internal partition walls.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c21", "Check horizontal course alignment with string line", false),
                            QualityCheckpoint("c22", "Ensure daily height limit of 1.5m", false),
                            QualityCheckpoint("c23", "Water curing for minimum 7 days", false)
                        )
                    ),
                    BuildingTask(
                        id = "t3_2",
                        title = "RCC Lintel Beams & Sunshade Chajjas",
                        description = "Cast continuous lintel band over doors and window openings.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c24", "Verify 150mm bearing over wall jambs", false),
                            QualityCheckpoint("c25", "Shuttering de-shuttering period adherence (7 days)", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "p4",
                title = "4. MEP & Concealed Services",
                weight = 15,
                icon = Icons.Default.Plumbing,
                tasks = listOf(
                    BuildingTask(
                        id = "t4_1",
                        title = "Electrical Wall Chasing & Conduit Routing",
                        description = "Cut wall chases, install PVC conduit pipes, and metal modular switch boxes.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c26", "Verify switchboard heights from finished floor level", false),
                            QualityCheckpoint("c27", "Conduit pipe fixing with GI saddle clamps", false)
                        )
                    ),
                    BuildingTask(
                        id = "t4_2",
                        title = "Plumbing Concealed CPVC Lines & Testing",
                        description = "Run hot and cold concealed water lines and hydrostatic pressure test.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c28", "Hydrostatic pressure test at 10 kg/cm² for 2 hours", false),
                            QualityCheckpoint("c29", "Bathroom sunken core cut waterproofing", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "p5",
                title = "5. Plastering & Architectural Finishing",
                weight = 20,
                icon = Icons.Default.CheckCircle,
                tasks = listOf(
                    BuildingTask(
                        id = "t5_1",
                        title = "Internal & External Cement Plastering",
                        description = "Apply 12mm smooth internal plaster and 20mm sand-faced waterproof exterior plaster.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c30", "Chicken wire mesh over beam-masonry joints", false),
                            QualityCheckpoint("c31", "20mm sand-faced external waterproof plaster", false)
                        )
                    ),
                    BuildingTask(
                        id = "t5_2",
                        title = "Flooring, Putty & Painting",
                        description = "Install vitrified floor tiles, 2 coats wall putty, primer, and emulsion paint.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c32", "Vitrified tile level & 2mm spacer joint filling", false),
                            QualityCheckpoint("c33", "2 coats wall putty + primer + 2 coats emulsion paint", false)
                        )
                    ),
                    BuildingTask(
                        id = "t5_3",
                        title = "Final Snag Inspection & Client Handover",
                        description = "Test all sanitary fittings, electrical circuits, and hand over keys to client.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("c34", "Sanitary fixture flow test & leak check", false),
                            QualityCheckpoint("c35", "Electrical switchboard continuity & phase load test", false),
                            QualityCheckpoint("c36", "Keys and warranty document pack handover", false)
                        )
                    )
                )
            )
        )
    ),
    PlanTemplate(
        id = "tmpl-load-bearing",
        name = "Load-Bearing Brick House",
        description = "Traditional brick masonry load-bearing construction with strip footings and RCC lintel bands.",
        buildingType = "Load-Bearing House",
        stages = listOf(
            BuildingStage(
                id = "lb1",
                title = "1. Excavation & Stepped Brick Foundation",
                weight = 25,
                icon = Icons.Default.Foundation,
                tasks = listOf(
                    BuildingTask(
                        id = "lb_t1",
                        title = "Trench Excavation & Lime Concrete Bed",
                        description = "Continuous strip trench excavation for load-bearing walls.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("lbc1", "Trench bed compaction & firm soil verification", true),
                            QualityCheckpoint("lbc2", "1:4:8 cement/lime bed casting", true)
                        )
                    ),
                    BuildingTask(
                        id = "lb_t2",
                        title = "Stepped Brick Footing Masonry",
                        description = "First class burnt clay brick masonry in stepped offsets.",
                        status = "in-progress",
                        checklists = listOf(
                            QualityCheckpoint("lbc3", "Mortar ratio 1:4 with fine river sand", true),
                            QualityCheckpoint("lbc4", "Course alignment and offset dimensions", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "lb2",
                title = "2. Plinth Band & 9-inch Walls",
                weight = 30,
                icon = Icons.Default.ViewColumn,
                tasks = listOf(
                    BuildingTask(
                        id = "lb_t3",
                        title = "RCC Plinth Tie Band Casting",
                        description = "Perimeter earthquake resistant RCC plinth band.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("lbc5", "4-bar 10mm reinforcement cage", false),
                            QualityCheckpoint("lbc6", "Bitumen DPC 50mm damp-proof course", false)
                        )
                    ),
                    BuildingTask(
                        id = "lb_t4",
                        title = "Superstructure 9-inch Load-Bearing Masonry",
                        description = "English bond masonry up to roof bottom.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("lbc7", "Daily masonry lift restricted to 1.2m", false),
                            QualityCheckpoint("lbc8", "Continuous wet curing for 14 days", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "lb3",
                title = "3. RCC Roof Slab & Finishing",
                weight = 45,
                icon = Icons.Default.Roofing,
                tasks = listOf(
                    BuildingTask(
                        id = "lb_t5",
                        title = "RCC Roof Slab Casting",
                        description = "One-way / two-way reinforced concrete roof slab.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("lbc9", "Formwork propping and leveling", false),
                            QualityCheckpoint("lbc10", "Ponding water curing for 14 days", false)
                        )
                    ),
                    BuildingTask(
                        id = "lb_t6",
                        title = "Internal Finishing & Woodwork",
                        description = "Cement plastering, floor tiles, and teakwood doors.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("lbc11", "Smooth 12mm internal plaster", false),
                            QualityCheckpoint("lbc12", "Door frame plumb and holdfast grouting", false)
                        )
                    )
                )
            )
        )
    ),
    PlanTemplate(
        id = "tmpl-commercial",
        name = "Commercial Multi-Story Structure",
        description = "Heavy commercial construction with deep pile foundation, core shear walls, and post-tensioned slabs.",
        buildingType = "Commercial Complex",
        stages = listOf(
            BuildingStage(
                id = "cm1",
                title = "1. Piling & Deep Basement Works",
                weight = 30,
                icon = Icons.Default.Foundation,
                tasks = listOf(
                    BuildingTask(
                        id = "cm_t1",
                        title = "Bored Cast-in-Situ Piles",
                        description = "Drill 600mm/800mm diameter piles to hard rock socket.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("cmc1", "Borehole flushing & bentonite density check", true),
                            QualityCheckpoint("cmc2", "Tremie pipe concrete pour without air pockets", true)
                        )
                    ),
                    BuildingTask(
                        id = "cm_t2",
                        title = "Basement Raft & Retaining Walls",
                        description = "Waterproof reinforced concrete basement raft and retaining wall.",
                        status = "in-progress",
                        checklists = listOf(
                            QualityCheckpoint("cmc3", "Hydrophilic PVC waterstop at cold joints", true),
                            QualityCheckpoint("cmc4", "Heavy bottom & top mat reinforcement tie", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "cm2",
                title = "2. Core Shear Walls & PT Slabs",
                weight = 35,
                icon = Icons.Default.ViewColumn,
                tasks = listOf(
                    BuildingTask(
                        id = "cm_t3",
                        title = "Central Lift Core Shear Walls (M40)",
                        description = "Climbing formwork system for core elevator shaft walls.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("cmc5", "Shear wall rebar cage and boundary elements", false),
                            QualityCheckpoint("cmc6", "M40 concrete pump mix design slump verification", false)
                        )
                    ),
                    BuildingTask(
                        id = "cm_t4",
                        title = "Post-Tensioned (PT) Flat Slabs",
                        description = "Post-tensioned tendon profiling and hydraulic jack stressing.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("cmc7", "PT duct parabolic profile alignment check", false),
                            QualityCheckpoint("cmc8", "Hydraulic stress elongation measurement sign-off", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "cm3",
                title = "3. Curtain Wall Facade & Services",
                weight = 35,
                icon = Icons.Default.CheckCircle,
                tasks = listOf(
                    BuildingTask(
                        id = "cm_t5",
                        title = "Unitized Structural Glazing Facade",
                        description = "Double glazed low-E glass facade panels with aluminum subframe.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("cmc9", "Anchor bracket torque testing", false),
                            QualityCheckpoint("cmc10", "Silicone weather seal water spray test", false)
                        )
                    ),
                    BuildingTask(
                        id = "cm_t6",
                        title = "Central HVAC & Fire Sprinkler Grid",
                        description = "Chilled water piping, AHU ducts, and automatic fire suppression.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("cmc11", "Fire sprinkler hydro test at 15 bar", false),
                            QualityCheckpoint("cmc12", "Smoke extraction damper operation test", false)
                        )
                    )
                )
            )
        )
    ),
    PlanTemplate(
        id = "tmpl-renovation",
        name = "Interior Fit-out & Renovation",
        description = "Commercial interior fit-out, partition walls, false ceilings, electrical, and cabinetry.",
        buildingType = "Fit-out & Renovation",
        stages = listOf(
            BuildingStage(
                id = "rn1",
                title = "1. Demolition & Site Prep",
                weight = 20,
                icon = Icons.Default.Foundation,
                tasks = listOf(
                    BuildingTask(
                        id = "rn_t1",
                        title = "Partition Demolition & Floor Strip",
                        description = "Safely remove existing masonry partitions and floor finishes.",
                        status = "completed",
                        checklists = listOf(
                            QualityCheckpoint("rnc1", "Structural column safety & no load-bearing breach", true),
                            QualityCheckpoint("rnc2", "Debris bagging & green transport disposal", true)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "rn2",
                title = "2. Drywall Partitions & False Ceiling",
                weight = 40,
                icon = Icons.Default.ViewColumn,
                tasks = listOf(
                    BuildingTask(
                        id = "rn_t2",
                        title = "GI Stud Drywall Partitions with Acoustic Wool",
                        description = "Erect 75mm GI framing with acoustic insulation and gypsum boards.",
                        status = "in-progress",
                        checklists = listOf(
                            QualityCheckpoint("rnc3", "Stud center-to-center spacing 600mm", true),
                            QualityCheckpoint("rnc4", "Acoustic rockwool 50kg/m³ density insertion", false)
                        )
                    ),
                    BuildingTask(
                        id = "rn_t3",
                        title = "Gypsum False Ceiling Grid & Lighting Cutouts",
                        description = "Hang perimeter channels, main tees, and cutouts for LED panels.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("rnc5", "Ceiling level check with laser level", false),
                            QualityCheckpoint("rnc6", "Independent soffit hangers for heavy AC units", false)
                        )
                    )
                )
            ),
            BuildingStage(
                id = "rn3",
                title = "3. Joinery, Modular Furniture & Snagging",
                weight = 40,
                icon = Icons.Default.CheckCircle,
                tasks = listOf(
                    BuildingTask(
                        id = "rn_t4",
                        title = "Custom Cabinetry & Glass Partitions",
                        description = "Install factory finished modular cabinets and 12mm toughened glass.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("rnc7", "Soft-close hinge and drawer slider testing", false),
                            QualityCheckpoint("rnc8", "Glass patch fitting plumb and silicone bead", false)
                        )
                    ),
                    BuildingTask(
                        id = "rn_t5",
                        title = "Final Polish, Snagging & Deep Cleaning",
                        description = "PU wood polish, touch-up paint, and handover snag list rectification.",
                        status = "pending",
                        checklists = listOf(
                            QualityCheckpoint("rnc9", "Touch-up paint inspection under spot light", false),
                            QualityCheckpoint("rnc10", "Final client handover snag clearance sign-off", false)
                        )
                    )
                )
            )
        )
    )
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

    // Template state
    var selectedTemplateId by remember { mutableStateOf("tmpl-framed-villa") }
    var isTemplateDialogOpen by remember { mutableStateOf(false) }

    // Stages state
    val initialStages = remember(selectedTemplateId) {
        BuiltInPlanTemplates.find { it.id == selectedTemplateId }?.stages ?: BuiltInPlanTemplates.first().stages
    }
    var stages by remember(selectedTemplateId) { mutableStateOf(initialStages) }

    // Expanded accordion states
    val expandedStageIds = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(selectedTemplateId) {
        stages.forEachIndexed { idx, stage ->
            expandedStageIds[stage.id] = idx <= 1
        }
    }

    // Dynamic Calculations
    val totalQualityChecks = remember(stages) {
        stages.sumOf { s -> s.tasks.sumOf { t -> t.checklists.size } }
    }
    val totalCompletedChecks = remember(stages) {
        stages.sumOf { s -> s.tasks.sumOf { t -> t.checklists.count { it.isCompleted } } }
    }
    val overallProgressPct = remember(totalQualityChecks, totalCompletedChecks) {
        if (totalQualityChecks > 0) (totalCompletedChecks * 100) / totalQualityChecks else 0
    }

    // Find active stage
    val activeStage = remember(stages) {
        stages.find { stage -> stage.tasks.any { !it.isCompleted } } ?: stages.firstOrNull()
    }
    val activeStageIndex = remember(stages, activeStage) {
        val idx = stages.indexOf(activeStage)
        if (idx != -1) idx + 1 else 1
    }

    // Toggle Task Status
    fun toggleTask(stageId: String, taskId: String, currentCompleted: Boolean) {
        val newStatus = if (currentCompleted) "pending" else "completed"
        stages = stages.map { stage ->
            if (stage.id != stageId) stage
            else stage.copy(
                tasks = stage.tasks.map { task ->
                    if (task.id != taskId) task
                    else task.copy(
                        status = newStatus,
                        checklists = task.checklists.map { it.copy(isCompleted = newStatus == "completed") }
                    )
                }
            )
        }
    }

    // Toggle Checklist Item
    fun toggleChecklist(stageId: String, taskId: String, checkId: String) {
        stages = stages.map { stage ->
            if (stage.id != stageId) stage
            else stage.copy(
                tasks = stage.tasks.map { task ->
                    if (task.id != taskId) task
                    else {
                        val updatedChecks = task.checklists.map {
                            if (it.id == checkId) it.copy(isCompleted = !it.isCompleted) else it
                        }
                        val allChecked = updatedChecks.isNotEmpty() && updatedChecks.all { it.isCompleted }
                        val anyChecked = updatedChecks.any { it.isCompleted }
                        val newStatus = when {
                            allChecked -> "completed"
                            anyChecked -> "in-progress"
                            else -> "pending"
                        }
                        task.copy(status = newStatus, checklists = updatedChecks)
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Building Construction Plan & Quality Checks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                actions = {
                    OutlinedButton(
                        onClick = { isTemplateDialogOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(30.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── 1. Overall Construction Stages & Quality Card ───────────────────
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = BrandPrimary.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandPrimaryContainer)
                                    .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Construction Stages & Quality Checklists",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Stage-by-stage progress breakdown with quality control checklists.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = GlassBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Stats
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
                                text = "$overallProgressPct% COMPLETE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (overallProgressPct == 100) EmeraldSuccess else BrandPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { overallProgressPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (overallProgressPct == 100) EmeraldSuccess else BrandPrimary,
                            trackColor = SlateSurfaceSubtle
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$totalCompletedChecks of $totalQualityChecks Quality Inspections Passed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandPrimaryContainer)
                                    .border(1.dp, BrandPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Stage $activeStageIndex of ${stages.size} Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ─── 2. Tab Selector: Stages vs Blueprint Viewer ─────────────────────
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

            // ─── 3. Tab Content ──────────────────────────────────────────────────
            if (selectedTab == 0) {
                // Accordion of Stages
                itemsIndexed(stages) { stageIdx, stage ->
                    val isExpanded = expandedStageIds[stage.id] ?: (stageIdx <= 1)
                    val totalTasks = stage.tasks.size
                    val completedTasks = stage.tasks.count { it.isCompleted }
                    val totalChecks = stage.tasks.sumOf { it.checklists.size }
                    val completedChecks = stage.tasks.sumOf { it.checklists.count { c -> c.isCompleted } }
                    val stagePct = if (totalChecks > 0) (completedChecks * 100) / totalChecks else 0
                    val isStageDone = stagePct == 100

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        borderColor = if (isStageDone) EmeraldSuccess.copy(alpha = 0.4f) else GlassBorder
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Stage Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedStageIds[stage.id] = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isStageDone) EmeraldSuccessContainer else BrandPrimaryContainer)
                                        .border(1.dp, if (isStageDone) EmeraldSuccess.copy(alpha = 0.3f) else BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isStageDone) Icons.Default.CheckCircle else stage.icon,
                                        contentDescription = null,
                                        tint = if (isStageDone) EmeraldSuccess else BrandPrimary,
                                        modifier = Modifier.size(19.dp)
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Weight: ${stage.weight}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary
                                        )
                                        Text("•", fontSize = 11.sp, color = TextSecondary)
                                        Text(
                                            text = "$completedTasks/$totalTasks Tasks Done",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isStageDone) EmeraldSuccessContainer else BrandPrimaryContainer)
                                        .border(1.dp, if (isStageDone) EmeraldSuccess.copy(alpha = 0.3f) else BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$stagePct%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStageDone) EmeraldSuccess else BrandPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Expanded Stage Tasks
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stage.tasks.forEach { task ->
                                        val taskChecksTotal = task.checklists.size
                                        val taskChecksDone = task.checklists.count { it.isCompleted }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = SlateSurfaceSubtle),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Task Top Row: Checkbox, Title, and Status Badge
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Checkbox(
                                                        checked = task.isCompleted,
                                                        onCheckedChange = {
                                                            toggleTask(stage.id, task.id, task.isCompleted)
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = EmeraldSuccess,
                                                            checkmarkColor = Color.White
                                                        ),
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .padding(top = 1.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = task.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (task.isCompleted) TextSecondary else TextPrimary,
                                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                                        )
                                                        if (!task.description.isNullOrBlank()) {
                                                            Text(
                                                                text = task.description,
                                                                fontSize = 11.sp,
                                                                color = TextSecondary,
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // Status Badge
                                                    val (badgeBg, badgeBorder, badgeText, badgeColor) = when (task.status) {
                                                        "completed" -> Quad(EmeraldSuccessContainer, EmeraldSuccess.copy(alpha = 0.3f), "completed", EmeraldSuccess)
                                                        "in-progress" -> Quad(Color(0xFFFEF3C7).copy(alpha = 0.15f), Color(0xFFF59E0B).copy(alpha = 0.3f), "in-progress", Color(0xFFF59E0B))
                                                        else -> Quad(SlateSurfaceSubtle, GlassBorder, "pending", TextSecondary)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(badgeBg)
                                                            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeColor
                                                        )
                                                    }
                                                }

                                                // Quality Checkpoints
                                                if (task.checklists.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.CheckBox, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(13.dp))
                                                            Text("Quality Checkpoints", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                        }
                                                        Text(
                                                            text = "$taskChecksDone/$taskChecksTotal Verified",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandPrimary
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        task.checklists.forEach { check ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (check.isCompleted) EmeraldSuccess.copy(alpha = 0.1f) else SlateBackground)
                                                                    .border(1.dp, if (check.isCompleted) EmeraldSuccess.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(6.dp))
                                                                    .clickable {
                                                                        toggleChecklist(stage.id, task.id, check.id)
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Checkbox(
                                                                    checked = check.isCompleted,
                                                                    onCheckedChange = {
                                                                        toggleChecklist(stage.id, task.id, check.id)
                                                                    },
                                                                    colors = CheckboxDefaults.colors(
                                                                        checkedColor = EmeraldSuccess,
                                                                        checkmarkColor = Color.White
                                                                    ),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = check.title,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = if (check.isCompleted) TextSecondary else TextPrimary,
                                                                    textDecoration = if (check.isCompleted) TextDecoration.LineThrough else null,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ─── 4. Architectural Blueprint Tab ──────────────────────────────────
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

                            // Floor Level Filter Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Ground Floor Plan", "First Floor Plan", "Terrace Plan").forEach { floor ->
                                    FilterChip(
                                        selected = selectedFloor == floor,
                                        onClick = { selectedFloor = floor },
                                        label = { Text(floor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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

    // ─── 5. Template Selection Dialog ("Change Building Plan") ──────────────
    if (isTemplateDialogOpen) {
        AlertDialog(
            onDismissRequest = { isTemplateDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = BrandPrimary)
                    Text("Change Building Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select a master construction workflow template to track stage-by-stage civil execution and quality checklists.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    BuiltInPlanTemplates.forEach { template ->
                        val isSelected = template.id == selectedTemplateId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimaryContainer else SlateSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimary else GlassBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedTemplateId = template.id
                                    isTemplateDialogOpen = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedTemplateId = template.id
                                    isTemplateDialogOpen = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = template.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "${template.stages.size} Stages • ${template.buildingType}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isTemplateDialogOpen = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            },
            containerColor = SlateSurface
        )
    }
}

// Helper quadruple
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
