package com.example.constructorapp.ui.screens.worklog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.constructorapp.data.local.entity.WorklogPhotoEntity
import com.example.constructorapp.theme.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * Modern Fullscreen Photo Lightbox with:
 * - Swipe left/right between photos (HorizontalPager)
 * - Two-finger pinch-to-zoom (1x up to 5x)
 * - Panning when zoomed in
 * - Double-tap to quick-zoom / reset
 * - Floating top bar with photo counter & zoom controls
 * - Floating bottom sheet with captions, title, and interactive thumbnail gallery strip
 */
@Composable
fun ModernWorklogPhotoViewer(
    photos: List<WorklogPhotoEntity>,
    initialIndex: Int = 0,
    title: String? = null,
    dateLabel: String? = null,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitialIndex = initialIndex.coerceIn(0, photos.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { photos.size }
    )

    // Per-page scale & offset state to allow free zooming and panning per slide
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when active page changes
    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
        currentOffset = Offset.Zero
    }

    val thumbnailListState = rememberLazyListState()
    LaunchedEffect(pagerState.currentPage) {
        thumbnailListState.animateScrollToItem(pagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Horizontal Pager (Swiping is enabled only when unzoomed so user can pan when zoomed)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = (currentScale <= 1.05f),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                val photoModel = remember(photo) {
                    if (!photo.localFilePath.isNullOrBlank()) {
                        val file = File(photo.localFilePath)
                        if (file.exists()) file else photo.remotePhotoUrl
                    } else {
                        photo.remotePhotoUrl
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (currentScale > 1.05f) {
                                        currentScale = 1f
                                        currentOffset = Offset.Zero
                                    } else {
                                        currentScale = 2.5f
                                        val maxOffsetX = (size.width * 1.5f) / 2f
                                        val maxOffsetY = (size.height * 1.5f) / 2f
                                        val targetX = (size.width / 2f - tapOffset.x) * 1.5f
                                        val targetY = (size.height / 2f - tapOffset.y) * 1.5f
                                        currentOffset = Offset(
                                            x = targetX.coerceIn(-maxOffsetX, maxOffsetX),
                                            y = targetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    }
                                }
                            )
                        }
                        .pointerInput(page) {
                            detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                val oldScale = currentScale
                                val newScale = (oldScale * zoom).coerceIn(1f, 6f)
                                currentScale = newScale

                                if (newScale > 1.02f) {
                                    val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                    currentOffset = Offset(
                                        x = (currentOffset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (currentOffset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    currentOffset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = photo.caption ?: "Worklog Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 72.dp)
                            .graphicsLayer {
                                scaleX = if (pagerState.currentPage == page) currentScale else 1f
                                scaleY = if (pagerState.currentPage == page) currentScale else 1f
                                translationX = if (pagerState.currentPage == page) currentOffset.x else 0f
                                translationY = if (pagerState.currentPage == page) currentOffset.y else 0f
                            }
                    )
                }
            }

            // 2. Left & Right Floating Chevron Buttons (When unzoomed & multiple photos)
            if (photos.size > 1 && currentScale <= 1.05f) {
                // Prev button
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Next button
                AnimatedVisibility(
                    visible = pagerState.currentPage < photos.size - 1,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. Top Floating Glass Controls Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Photo Index Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${photos.size}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Zoom Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Reset Zoom button (visible when zoomed)
                        AnimatedVisibility(
                            visible = currentScale > 1.05f,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = {
                                    currentScale = 1f
                                    currentOffset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Outlined.RestartAlt,
                                    contentDescription = "Reset Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bottom Floating Glass Info & Thumbnail Tray
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activePhoto = photos.getOrNull(pagerState.currentPage)

                    // Caption
                    if (!activePhoto?.caption.isNullOrBlank()) {
                        Text(
                            text = activePhoto!!.caption!!,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Metadata Row: Title, Date, Gesture Tip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(title, dateLabel).joinToString(" • ").ifBlank { "Site Progress Photo" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Text(
                            text = if (currentScale > 1.05f) "${(currentScale * 100).toInt()}% • Drag to pan" else "Pinch / Double-tap to zoom",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            color = if (currentScale > 1.05f) BrandPrimary else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Thumbnail Strip for Instant Jump (When 2+ photos)
                    if (photos.size > 1) {
                        LazyRow(
                            state = thumbnailListState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            itemsIndexed(photos) { idx, thumbPhoto ->
                                val isSelected = idx == pagerState.currentPage
                                val thumbModel = remember(thumbPhoto) {
                                    if (!thumbPhoto.localFilePath.isNullOrBlank()) {
                                        val file = File(thumbPhoto.localFilePath)
                                        if (file.exists()) file else thumbPhoto.remotePhotoUrl
                                    } else {
                                        thumbPhoto.remotePhotoUrl
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
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
