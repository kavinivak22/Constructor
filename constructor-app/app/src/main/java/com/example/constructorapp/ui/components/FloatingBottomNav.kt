package com.example.constructorapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.BrandPrimary
import com.example.constructorapp.theme.BrandPrimaryDark
import com.example.constructorapp.theme.PlusJakartaSansFontFamily
import com.example.constructorapp.theme.TextPrimary
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

/**
 * Ultra-Glossy Fluid FloatingBottomNav matching the webapp's frosted glassmorphism:
 * - 68%-85% translucent white gradient fill allowing background ambient blobs to refract
 * - Dual-layer specular border gradient (crisp 90% white top down to 40% white bottom)
 * - Inset specular reflection line mimicking 0 1px 2px rgba(255,255,255,0.85)
 * - Deep ambient indigo drop shadow (0 16px 36px rgba(31, 38, 135, 0.18))
 * - Tactile active-tab indicator pill with animated elevation and soft glow
 */
@Composable
fun FloatingBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navShape = RoundedCornerShape(26.dp)

    // Specular glass border: sharp top reflection fading smoothly to lower ambient edge
    val glassBorderBrush = Brush.verticalGradient(
        listOf(
            Color(0xEBFFFFFF), // 92% specular highlight rim
            Color(0x99FFFFFF), // 60% translucent mid-border
            Color(0x40FFFFFF)  // 25% lower refractive edge
        )
    )

    // Frosted glass body gradient: 80% white top descending to 62% white bottom for deep translucency
    val glassBodyBrush = Brush.verticalGradient(
        listOf(
            Color(0xD1FFFFFF), // 82% frosted white
            Color(0xA6FFFFFF)  // 65% translucent acrylic white
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = 16.dp,
                shape = navShape,
                spotColor = Color(0x331F2687),    // 20% indigo ambient spot
                ambientColor = Color(0x241F2687) // 14% soft ambient spread
            ),
        shape = navShape,
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, glassBorderBrush),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(navShape)
                .background(glassBodyBrush)
        ) {
            // Top specular reflection highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x40FFFFFF),
                                Color(0xF2FFFFFF),
                                Color(0x40FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner subtle glass ambient refraction glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x2EFFFFFF),
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
                        animationSpec = tween(durationMillis = 220),
                        label = "iconOffset"
                    )

                    val pillScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "pillScale"
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
                        // Active Top Line Indicator (with primary gradient & subtle glow)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(28.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFFA043),
                                                BrandPrimary,
                                                BrandPrimaryDark
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .scale(pillScale)
                                .padding(top = 2.dp)
                        ) {
                            // Icon capsule with frosted active background & specular outline
                            Box(
                                modifier = Modifier
                                    .offset(y = iconOffsetY)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) BrandPrimary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = BrandPrimary.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        } else Modifier
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) BrandPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Tab Label
                            Text(
                                text = item.title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
