package com.example.constructorapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*

data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int? = null
)

@Composable
fun AppDrawerContent(
    userEmail: String?,
    userDisplayName: String?,
    userRole: String? = "admin",
    selectedRoute: String,
    onNavigateRoute: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("en") }

    val coreOperations = listOf(
        DrawerItem("Dashboard", Icons.Outlined.GridView, "/"),
        DrawerItem("Projects", Icons.Outlined.Folder, "/projects"),
        DrawerItem("Warehouse", Icons.Outlined.Inventory2, "/inventory"),
        DrawerItem("Purchase Orders", Icons.Outlined.ShoppingCart, "/purchase-orders"),
        DrawerItem("Daily Worklog", Icons.Outlined.EditNote, "/worklog"),
        DrawerItem("Work Prep", Icons.Outlined.PhoneCallback, "/work-prep"),
        DrawerItem("Team Hub", Icons.AutoMirrored.Outlined.Chat, "/team-hub"),
        DrawerItem("Analytics", Icons.Outlined.Insights, "/analytics")
    )

    val aiTools = listOf(
        DrawerItem("AI Estimation", Icons.Outlined.AutoAwesome, "/material-estimation")
    )

    val pouchSection = listOf(
        DrawerItem("Notifications", Icons.Outlined.Notifications, "/notifications", badgeCount = 3),
        DrawerItem("Personal Pouch", Icons.Outlined.AccountBalanceWallet, "/personal-pouch"),
        DrawerItem("Project Pouch", Icons.Outlined.WorkOutline, "/project-pouch")
    )

    val financialsSection = listOf(
        DrawerItem("Weekly Pay-Day", Icons.Outlined.MonetizationOn, "/financials/payday"),
        DrawerItem("Contractor Accounts", Icons.Outlined.AccountBalance, "/financials/contractors"),
        DrawerItem("Project Expenses", Icons.Outlined.ReceiptLong, "/expenses"),
        DrawerItem("Material Reconciliation", Icons.Outlined.SwapHoriz, "/materials/reconciliation"),
        DrawerItem("Client Milestones", Icons.Outlined.Flag, "/projects/milestones")
    )

    val adminSection = listOf(
        DrawerItem("Employees", Icons.Outlined.PeopleOutline, "/employees")
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .clip(AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp, topRight = 0.dp, bottomRight = 0.dp))
            .background(Color(0xEBFFFFFF)) // 92% frosted glass (lowered transparency for clear, crisp drawer elements)
            .border(
                BorderStroke(1.dp, Color(0x80FFFFFF)),
                AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp, topRight = 0.dp, bottomRight = 0.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        // App Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BrandPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Apartment,
                        contentDescription = "Constructor Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Constructor",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            IconButton(onClick = onCloseDrawer) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Drawer",
                    tint = TextSecondary
                )
            }
        }

        HorizontalDivider(color = SlateDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Scrollable Menu List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Core Operations
            coreOperations.forEach { item ->
                DrawerItemRow(
                    item = item,
                    isSelected = selectedRoute == item.route,
                    onClick = { onNavigateRoute(item.route) }
                )
            }

            // AI Tools Section
            DrawerSectionHeader("AI TOOLS")
            aiTools.forEach { item ->
                DrawerItemRow(
                    item = item,
                    isSelected = selectedRoute == item.route,
                    onClick = { onNavigateRoute(item.route) }
                )
            }

            // Pouch Section
            DrawerSectionHeader("POUCH")
            pouchSection.forEach { item ->
                DrawerItemRow(
                    item = item,
                    isSelected = selectedRoute == item.route,
                    onClick = { onNavigateRoute(item.route) }
                )
            }

            // Financials Section (Admin & Manager)
            if (userRole == "admin" || userRole == "manager") {
                DrawerSectionHeader("FINANCIALS")
                financialsSection.forEach { item ->
                    DrawerItemRow(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigateRoute(item.route) }
                    )
                }
            }

            // Admin Section
            if (userRole == "admin") {
                DrawerSectionHeader("ADMIN")
                adminSection.forEach { item ->
                    DrawerItemRow(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigateRoute(item.route) }
                    )
                }
            }
        }

        HorizontalDivider(color = SlateDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Footer: Language Switcher Pill
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0x33FFFFFF),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // EN Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedLanguage == "en") BrandPrimary else Color.Transparent)
                        .clickable { selectedLanguage = "en" }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EN",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedLanguage == "en") Color.White else TextSecondary
                    )
                }
                // Tamil Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedLanguage == "ta") BrandPrimary else Color.Transparent)
                        .clickable { selectedLanguage = "ta" }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "தமிழ்",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedLanguage == "ta") Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer: User Profile & Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x40FFFFFF))
                .border(BorderStroke(1.dp, SlateBorder), RoundedCornerShape(12.dp))
                .clickable { onNavigateRoute("/profile") }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(BrandPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (userDisplayName?.firstOrNull() ?: 'U').uppercase(),
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userDisplayName ?: "Constructor User",
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = userEmail ?: "user@constructor.app",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Log out",
                    tint = RoseDanger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = OutfitFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
        letterSpacing = 1.sp
    )
}

@Composable
private fun DrawerItemRow(
    item: DrawerItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandPrimary.copy(alpha = 0.12f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) BrandPrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = item.title,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) BrandPrimary else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (item.badgeCount != null && item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .background(RoseDanger, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    FluidGlossAlertDialog(
        onDismissRequest = onDismiss,
        title = "Log Out?",
        text = "Are you sure you want to log out?",
        confirmText = "Log Out",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
        icon = Icons.AutoMirrored.Filled.Logout
    )
}
