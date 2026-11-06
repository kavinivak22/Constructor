package com.example.constructorapp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*

@Composable
fun App() {
    ConstructorSharedTheme {
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            containerColor = SlateBackground,
            topBar = {
                NativeHeader()
            },
            bottomBar = {
                NativeBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab()
                    1 -> ProjectsTab()
                    2 -> WorklogsTab()
                    3 -> MaterialsTab()
                    4 -> MoreTab()
                }
            }
        }
    }
}

@Composable
private fun NativeHeader() {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏗️", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "CONSTRUCTOR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Site Construction Manager",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmeraldSuccessContainer)
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldSuccess))
                    Text("Cloud Synced", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
            }
        }
    }
}

@Composable
private fun NativeBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        Triple(0, "Dashboard", "📊"),
        Triple(1, "Projects", "📁"),
        Triple(2, "Worklogs", "📋"),
        Triple(3, "Materials", "🧱"),
        Triple(4, "Settings", "⚙️")
    )

    Surface(
        color = SurfaceWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, title, emoji) ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BrandPrimaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = if (isSelected) 18.sp else 16.sp)
                    }
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Welcome Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Site Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Today's verified site logs, materials, and labor active", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPill(modifier = Modifier.weight(1f), label = "Active Sites", value = "4", color = BrandPrimary)
                        StatPill(modifier = Modifier.weight(1f), label = "Labor On-Duty", value = "28", color = SkyInfo)
                        StatPill(modifier = Modifier.weight(1f), label = "Stock Alert", value = "2 Low", color = AmberWarning)
                    }
                }
            }
        }

        item {
            Text("Recent Site Projects", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(demoProjects) { project ->
            ProjectCard(project)
        }
    }
}

@Composable
private fun ProjectsTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("All Projects (${demoProjects.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(demoProjects) { project ->
            ProjectCard(project)
        }
    }
}

@Composable
private fun WorklogsTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Daily Worklogs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("+ Add Worklog", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }
        }

        items(demoWorklogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(log.date, fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(log.project, fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    Text(log.notes, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Divider(color = SlateDivider)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("👷 ${log.laborCount} Workers", fontSize = 11.sp, color = TextSecondary)
                        Text("🧱 ${log.materialsCount} Materials", fontSize = 11.sp, color = TextSecondary)
                        Text("📸 ${log.photosCount} Photos", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialsTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Inventory & Stock Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(demoMaterials) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Unit: ${item.unit} • ₹${item.unitCost}", fontSize = 11.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.stock} ${item.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess)
                        Text(if (item.stock < item.minStock) "⚠️ Low Stock" else "In Stock", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (item.stock < item.minStock) AmberWarning else EmeraldSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Constructor Native App", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Compose Multiplatform unified codebase for Android & iOS.", fontSize = 12.sp, color = TextSecondary)
                    Divider(color = SlateDivider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Framework", fontSize = 12.sp, color = TextSecondary)
                        Text("Jetpack Compose Multiplatform", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Database Engine", fontSize = 12.sp, color = TextSecondary)
                        Text("Supabase Cloud + Local Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Platform Target", fontSize = 12.sp, color = TextSecondary)
                        Text("Native iOS & Android", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ProjectCard(project: DemoProject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(project.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(project.location, fontSize = 11.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (project.progress > 70) EmeraldSuccessContainer else SkyInfoContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(project.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (project.progress > 70) EmeraldSuccess else SkyInfo)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", fontSize = 11.sp, color = TextSecondary)
                    Text("${project.progress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                LinearProgressIndicator(
                    progress = { project.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = BrandPrimary,
                    trackColor = SlateSurfaceSubtle
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budget: ₹${project.budget}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text("Spent: ₹${project.spent}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
            }
        }
    }
}

// Data Models
data class DemoProject(val name: String, val location: String, val status: String, val progress: Int, val budget: String, val spent: String)
data class DemoWorklog(val title: String, val project: String, val date: String, val notes: String, val laborCount: Int, val materialsCount: Int, val photosCount: Int)
data class DemoMaterial(val name: String, val unit: String, val stock: Int, val minStock: Int, val unitCost: String)

val demoProjects = listOf(
    DemoProject("Greenfield Villa Project", "Thillai Nagar, Trichy", "In Progress", 65, "45,00,000", "28,50,000"),
    DemoProject("Royal Residency Commercial", "Kallukuzhi, Trichy", "Active Foundation", 35, "85,00,000", "32,10,000"),
    DemoProject("Modern Living Duplex", "Karur Bypass, Trichy", "Finishing", 85, "32,00,000", "29,40,000")
)

val demoWorklogs = listOf(
    DemoWorklog("First Floor Slab Casting", "Greenfield Villa Project", "Today, 4:30 PM", "M20 grade concrete poured for roof slab with 12 masons and helpers.", 12, 4, 3),
    DemoWorklog("Brickwork & Plastering Phase 2", "Royal Residency Commercial", "Yesterday", "Completed outer wall perimeter bricklaying with cement mortar 1:5 ratio.", 8, 2, 2)
)

val demoMaterials = listOf(
    DemoMaterial("OPC 53 Grade Cement", "Bags", 140, 50, "395"),
    DemoMaterial("River Sand (Fine)", "CFT", 450, 200, "75"),
    DemoMaterial("TMT Steel 16mm (Fe 550D)", "Kg", 850, 300, "68"),
    DemoMaterial("Coarse Aggregate (20mm)", "CFT", 35, 100, "52")
)
