package com.example.constructorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.data.local.entity.ChatMessageEntity
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.GlassCard
import com.example.constructorapp.ui.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamHubScreen(
    initialProjectId: String? = null,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    var selectedProjectId by remember { mutableStateOf(initialProjectId ?: "") }

    // If no project selected yet and projects loaded, pick the first
    LaunchedEffect(projects) {
        if (selectedProjectId.isEmpty() && projects.isNotEmpty()) {
            selectedProjectId = projects[0].id
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Mock initial messages for preview and Room persistence
    val sampleMessages = remember(selectedProjectId) {
        mutableStateListOf(
            ChatMessageEntity(
                projectId = selectedProjectId,
                userId = "user_1",
                userName = "Kavin (Site Engineer)",
                message = "Morning team! Ready-mix concrete truck scheduled to arrive at 10:30 AM for 2nd floor slab casting.",
                createdAt = "09:15 AM",
                isOutgoing = false
            ),
            ChatMessageEntity(
                projectId = selectedProjectId,
                userId = "user_current",
                userName = "You",
                message = "Thanks Kavin. Bar benders have completed the top mat inspection. Vibrator and cube moulds are staged.",
                createdAt = "09:22 AM",
                isOutgoing = true
            ),
            ChatMessageEntity(
                projectId = selectedProjectId,
                userId = "user_2",
                userName = "Ramesh (Supervisor)",
                message = "Electrical conduits and fan hooks checked as per drawings. We are good to pour.",
                createdAt = "09:40 AM",
                isOutgoing = false
            )
        )
    }

    var messageInput by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    val currentProjectName = projects.find { it.id == selectedProjectId }?.name ?: "All Construction Sites"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Team Communication Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { projectDropdownExpanded = true }
                        ) {
                            Text(
                                text = currentProjectName,
                                fontSize = 12.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Project",
                                tint = BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                    DropdownMenu(
                        expanded = projectDropdownExpanded,
                        onDismissRequest = { projectDropdownExpanded = false }
                    ) {
                        projects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.name) },
                                onClick = {
                                    selectedProjectId = proj.id
                                    projectDropdownExpanded = false
                                }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Project Status Badge Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurfaceSubtle)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "Site Active • 4 Engineers Online",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SITE CHANNEL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
            }

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleMessages) { msg ->
                    ChatBubble(msg)
                }
            }

            // Message Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Message team at $currentProjectName...", fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                sampleMessages.add(
                                    ChatMessageEntity(
                                        projectId = selectedProjectId,
                                        userId = "user_current",
                                        userName = "You",
                                        message = messageInput.trim(),
                                        createdAt = timeNow,
                                        isOutgoing = true
                                    )
                                )
                                messageInput = ""
                                coroutineScope.launch {
                                    listState.animateScrollToItem(sampleMessages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessageEntity) {
    val isOutgoing = msg.isOutgoing
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (!isOutgoing && msg.userName != null) {
            Text(
                text = msg.userName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 16.dp
                    )
                )
                .background(if (isOutgoing) BrandPrimary else SlateSurfaceSubtle)
                .border(
                    1.dp,
                    if (isOutgoing) BrandPrimary.copy(alpha = 0.5f) else SlateBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = msg.message,
                    fontSize = 14.sp,
                    color = if (isOutgoing) Color.White else TextPrimary,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.createdAt,
                    fontSize = 10.sp,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
