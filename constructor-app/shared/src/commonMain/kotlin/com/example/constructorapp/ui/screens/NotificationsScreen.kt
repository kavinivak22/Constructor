package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.model.NotificationEntity
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sample site notifications matching Constructor ecosystem
    val notifications = remember {
        mutableStateListOf(
            NotificationEntity(
                id = DateTimeUtil.randomUuid(),
                userId = "user_1",
                title = "Purchase Order Approved",
                description = "PO #1042 for 200 bags of 53 Grade Cement has been approved by management.",
                category = "approvals",
                isRead = false,
                timestamp = "30m ago"
            ),
            NotificationEntity(
                id = DateTimeUtil.randomUuid(),
                userId = "user_1",
                title = "Low Stock Alert: 20mm Blue Metal",
                description = "Central warehouse stock has dropped below minimum threshold (15 CFT remaining).",
                category = "inventory",
                isRead = false,
                timestamp = "2h ago"
            ),
            NotificationEntity(
                id = DateTimeUtil.randomUuid(),
                userId = "user_1",
                title = "Daily Worklog Verified",
                description = "Worklog for Ground Floor Slab Concreting was audited and verified.",
                category = "approvals",
                isRead = true,
                timestamp = "1d ago"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                val updated = notifications.map { it.copy(isRead = true) }
                                notifications.clear()
                                notifications.addAll(updated)
                            }
                        ) {
                            Text(
                                text = "Mark all as read",
                                fontSize = 12.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        containerColor = SlateBackground,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BrandPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All Caught Up!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You don't have any unread notifications right now.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { notif ->
                        val isUnread = !notif.isRead
                        val icon = when (notif.category) {
                            "inventory" -> Icons.Default.Inventory2
                            "approvals" -> Icons.Default.FactCheck
                            "team" -> Icons.AutoMirrored.Filled.Chat
                            else -> Icons.Default.Notifications
                        }
                        val iconTint = when (notif.category) {
                            "inventory" -> AmberWarning
                            "approvals" -> EmeraldSuccess
                            "team" -> SkyInfo
                            else -> BrandPrimary
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val index = notifications.indexOf(notif)
                                    if (index != -1) {
                                        notifications[index] = notif.copy(isRead = true)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            borderColor = if (isUnread) BrandPrimary.copy(alpha = 0.4f) else SlateBorder
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iconTint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        if (isUnread) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandPrimary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.description,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
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
