package com.example.constructorapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.TextSecondary

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    data object Dashboard : BottomNavItem("Dashboard", Icons.Outlined.GridView, "/")
    data object Projects : BottomNavItem("Projects", Icons.Outlined.Folder, "/projects")
    data object Orders : BottomNavItem("Orders", Icons.Outlined.ShoppingCart, "/purchase-orders")
    data object Messages : BottomNavItem("Messages", Icons.Outlined.ChatBubbleOutline, "/team-hub")
    data object More : BottomNavItem("More", Icons.Outlined.MoreHoriz, "more")
}

val NavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Projects,
    BottomNavItem.Orders,
    BottomNavItem.Messages,
    BottomNavItem.More
)

@Composable
fun FloatingBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(24.dp), // rounded-3xl matching webapp
        color = Color(0xEBFFFFFF),         // 92% frosted glass (lowered transparency for clear, crisp nav elements)
        border = BorderStroke(1.dp, Color(0x80FFFFFF)), // Crisp specular glass border
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top highlight reflection mimicking inset 0 1px 1px 0 rgba(255, 255, 255, 0.5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x80FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NavItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index && index != 4
                    val isMore = index == 4

                    val iconOffsetY by animateDpAsState(
                        targetValue = if (isSelected) (-2).dp else 0.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "iconOffset"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isMore) {
                                    onMoreClick()
                                } else {
                                    onTabSelected(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Active Indicator: Top Line (w-8 h-1 rounded-b-full bg-primary)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(32.dp)
                                    .height(3.dp)
                                    .background(
                                        color = BrandPrimary,
                                        shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            // Icon Container with active tint and background
                            Box(
                                modifier = Modifier
                                    .offset(y = iconOffsetY)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) BrandPrimary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) BrandPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Label
                            Text(
                                text = item.title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) BrandPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
