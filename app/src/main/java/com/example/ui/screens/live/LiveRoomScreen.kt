package com.example.ui.screens.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.data.AgoraSession
import com.example.data.AgoraState
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VioraRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveRoomScreen(
    stream: LiveStream,
    onLeave: () -> Unit,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier,
    isSilentModerator: Boolean = false
) {
    BackHandler { onLeave() }

    val currentUser by VioraRepository.currentUser.collectAsState()
    val chatMap by VioraRepository.chatMessagesMap.collectAsState()
    val activeBanner by VioraRepository.activeGiftBanner.collectAsState()
    val liveStreams by VioraRepository.liveStreams.collectAsState()

    // Up-to-date stream reference
    val currentStream = liveStreams.find { it.id == stream.id } ?: stream
    val isBroadcaster = currentStream.hostId == currentUser.id || currentUser.role == UserRole.BROADCASTER
    val isModerator = currentUser.role == UserRole.MODERATOR || currentUser.role == UserRole.ADMIN

    val chatMessages = chatMap[stream.id] ?: emptyList()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showGiftSheet by remember { mutableStateOf(false) }
    var showBroadcasterMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var reactionHeartCount by remember { mutableIntStateOf(0) }

    // Broadcaster hardware toggles
    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraFront by remember { mutableStateOf(true) }
    var isVideoPaused by remember { mutableStateOf(false) }

    // ---- Agora live video/audio ----
    // Only the room's real host publishes. Viewers and silent moderators join as audience.
    val context = LocalContext.current
    val isHost = currentStream.hostId == currentUser.id && !isSilentModerator
    val agora = remember(stream.id, isHost) { AgoraSession(context, stream.id, isHost) }
    var mediaPermissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.CAMERA] == true &&
            results[Manifest.permission.RECORD_AUDIO] == true
        mediaPermissionDenied = !granted
        if (granted) agora.start(coroutineScope)
    }

    LaunchedEffect(agora) {
        if (!agora.isAvailable) return@LaunchedEffect
        if (!isHost) {
            agora.start(coroutineScope)
        } else {
            val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            val allGranted = needed.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) agora.start(coroutineScope) else permissionLauncher.launch(needed)
        }
    }
    DisposableEffect(agora) { onDispose { agora.release() } }

    // Auto-scroll chat to latest message
    // Real backend: pull new chat messages every couple of seconds while the room is open.
    LaunchedEffect(stream.id) {
        while (true) {
            VioraRepository.refreshChat(stream.id)
            kotlinx.coroutines.delay(2500)
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Periodic viewer simulation to feel alive
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (!isSilentModerator) VioraRepository.addLike(stream.id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isSilentModerator) {
                            reactionHeartCount++
                            VioraRepository.addLike(stream.id)
                        }
                    }
                )
            }
    ) {
        // 1. Immersive Simulated Video Surface / Camera Background
        LiveVideoCanvas(
            stream = currentStream,
            isVideoPaused = isVideoPaused,
            isFrontCamera = isCameraFront
        )

        // Real Agora video sits above the placeholder (hidden while the host has paused video).
        if (agora.isAvailable && !(isHost && isVideoPaused)) {
            AgoraVideoSurface(session = agora, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
        }

        // Connection / permission status for the live video
        val agoraNotice: String? = when {
            isHost && mediaPermissionDenied -> "Camera and microphone access is needed to broadcast. Allow it in Settings."
            agora.state == AgoraState.FAILED -> agora.errorMessage ?: "Live video unavailable"
            agora.state == AgoraState.CONNECTING -> "Connecting to live video…"
            else -> null
        }
        if (agoraNotice != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(agoraNotice, color = Color.White, fontSize = 13.sp)
            }
        }

        if (isSilentModerator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("👁 Silent moderator view. You are not counted as a viewer.", color = Color.White, fontSize = 11.sp)
            }
        }

        // 2. Floating Reactions Animation Overlay
        FloatingReactionsOverlay(
            triggerHeartCount = reactionHeartCount,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top Controls Bar & Broadcaster Pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Host Profile Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarView(
                        imageUrl = currentStream.hostAvatar,
                        displayName = currentStream.hostName,
                        size = 34.dp,
                        showBorder = true,
                        isVerified = true
                    )

                    Column {
                        Text(
                            text = currentStream.hostName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = "💎", fontSize = 10.sp)
                            Text(
                                text = "${currentStream.diamondCount}",
                                color = VioraWarmGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isBroadcaster) {
                        Button(
                            onClick = { VioraRepository.toggleFollowStream(currentStream.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStream.isFollowing) VioraSurfaceVariant else VioraCoral
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (currentStream.isFollowing) "Following" else "+ Follow",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Top Stats: Agora Quality + Viewers + Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Agora Quality Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(VioraSuccessGreen)
                            )
                            Text(if (agora.state == AgoraState.CONNECTED) "Agora HD" else "Demo", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // VIP Coin-Gated Badge
                    if (currentStream.isCoinGated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(VioraWarmGold)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🔒 VIP ${currentStream.entryCoinPrice}🪙",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Viewers count pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Viewers",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${currentStream.viewerCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Close button
                    IconButton(
                        onClick = {
                            if (isBroadcaster) showEndConfirmDialog = true else onLeave()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("leave_stream_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Gift Banner Celebratory Announcement
            GiftBannerOverlay(
                activeBanner = activeBanner,
                onDismiss = { VioraRepository.clearGiftBanner() }
            )

            // Pinned comment banner if present
            currentStream.pinnedComment?.let { pinned ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VioraDeepViolet.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = VioraWarmGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = pinned,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Live Chat Stream & Controls Area (Bottom-Aligned)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Chat Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        isModOrHost = isBroadcaster || isModerator,
                        onDelete = { VioraRepository.deleteChatMessage(stream.id, msg.id) },
                        onPin = { VioraRepository.pinComment(stream.id, "${msg.senderName}: ${msg.content}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chat Input Field
                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text("Say something...", color = VioraMutedText, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        focusedBorderColor = VioraElectricPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (chatInputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    VioraRepository.sendChatMessage(stream.id, chatInputText)
                                    chatInputText = ""
                                },
                                modifier = Modifier.testTag("send_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = VioraCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )

                // Virtual Gift Button
                IconButton(
                    onClick = { showGiftSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(VioraWarmGold, VioraCoral))
                        )
                        .testTag("open_gift_sheet_button")
                ) {
                    Text(text = "🎁", fontSize = 22.sp)
                }

                // Heart Reaction Button
                IconButton(
                    onClick = {
                        reactionHeartCount++
                        VioraRepository.addLike(stream.id)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(VioraCoral)
                        .testTag("send_reaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Broadcaster / Moderator Tools Button
                IconButton(
                    onClick = {
                        if (isBroadcaster) showBroadcasterMenu = true else showReportDialog = true
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("stream_tools_button")
                ) {
                    Icon(
                        imageVector = if (isBroadcaster) Icons.Default.Tune else Icons.Default.Shield,
                        contentDescription = "Tools",
                        tint = if (isBroadcaster) VioraElectricPurple else VioraWarmGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Gift Bottom Sheet
    if (showGiftSheet) {
        GiftBottomSheet(
            userCoinBalance = currentUser.coinBalance,
            onSendGift = { gift, count ->
                VioraRepository.sendGift(stream.id, gift, count)
                reactionHeartCount += count
                showGiftSheet = false
            },
            onNavigateToRecharge = {
                showGiftSheet = false
                onNavigateToWallet()
            },
            onDismiss = { showGiftSheet = false }
        )
    }

    // Broadcaster Tools Menu Modal
    if (showBroadcasterMenu) {
        AlertDialog(
            onDismissRequest = { showBroadcasterMenu = false },
            title = { Text("Broadcaster Studio Tools", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Flip camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraSurfaceVariant)
                            .clickable {
                                isCameraFront = !isCameraFront
                                agora.switchCamera()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Camera Lens", color = Color.White)
                        Text(if (isCameraFront) "Front (Selfie)" else "Rear", color = VioraCoral, fontWeight = FontWeight.Bold)
                    }

                    // Mute Microphone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraSurfaceVariant)
                            .clickable {
                                isMicMuted = !isMicMuted
                                agora.setMicMuted(isMicMuted)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Microphone", color = Color.White)
                        Text(if (isMicMuted) "MUTED 🔇" else "ACTIVE 🎙️", color = if (isMicMuted) VioraErrorRed else VioraSuccessGreen, fontWeight = FontWeight.Bold)
                    }

                    // Pause Video
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraSurfaceVariant)
                            .clickable {
                                isVideoPaused = !isVideoPaused
                                agora.setVideoPaused(isVideoPaused)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Video Stream", color = Color.White)
                        Text(if (isVideoPaused) "PAUSED ⏸️" else "STREAMING 🟢", color = if (isVideoPaused) VioraWarmGold else VioraSuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBroadcasterMenu = false
                        showEndConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraErrorRed)
                ) {
                    Text("End Broadcast", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcasterMenu = false }) {
                    Text("Close", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // Report / Safety Dialog
    if (showReportDialog) {
        var reportNotes by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(ReportCategory.INAPPROPRIATE_CONTENT) }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Broadcast / User", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Help keep Viora safe. Select a reason:", color = VioraMutedText, fontSize = 12.sp)

                    ReportCategory.values().take(4).forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = cat }
                        ) {
                            RadioButton(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                colors = RadioButtonDefaults.colors(selectedColor = VioraCoral)
                            )
                            Text(cat.displayName, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = reportNotes,
                        onValueChange = { reportNotes = it },
                        label = { Text("Additional notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        VioraRepository.submitReport(
                            targetType = "STREAM",
                            targetId = currentStream.id,
                            targetName = currentStream.hostName,
                            category = selectedCategory,
                            notes = reportNotes
                        )
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraCoral)
                ) {
                    Text("Submit Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // End Broadcast Confirmation Dialog
    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = { Text("End Live Broadcast?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your viewers will be disconnected and stream analytics will be saved to your Creator Studio.",
                    color = VioraMutedText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        VioraRepository.endLiveRoom(currentStream.id)
                        showEndConfirmDialog = false
                        onLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraErrorRed)
                ) {
                    Text("End Broadcast Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Keep Streaming", color = Color.White)
                }
            },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun LiveVideoCanvas(
    stream: LiveStream,
    isVideoPaused: Boolean,
    isFrontCamera: Boolean
) {
    if (isVideoPaused) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PauseCircle, contentDescription = null, tint = VioraWarmGold, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Broadcast Paused by Host", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Please stand by...", color = VioraMutedText, fontSize = 13.sp)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (stream.snapshotUrl.isNotBlank()) {
                AsyncImage(
                    model = stream.snapshotUrl,
                    contentDescription = "Live stream video of ${stream.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF200B44),
                                    Color(0xFF14082B),
                                    Color(0xFF0D041A)
                                )
                            )
                        )
                )
            }

            // High-fidelity camera & studio ambient lighting vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isModOrHost: Boolean,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    var showModOptions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = isModOrHost) { showModOptions = !showModOptions }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Role Badge
        when {
            message.isBroadcaster -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VioraCoral)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("HOST", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            message.isModerator -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VioraElectricPurple)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("MOD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            message.type == MessageType.GIFT_ANNOUNCEMENT -> {
                Text("🎁", fontSize = 11.sp)
            }
        }

        // Sender name
        Text(
            text = message.senderName,
            color = if (message.isBroadcaster) VioraCoral else if (message.isModerator) VioraWarmGold else Color(0xFFD6C8F5),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        // Content
        Text(
            text = message.content,
            color = if (message.type == MessageType.GIFT_ANNOUNCEMENT) VioraWarmGold else Color.White,
            fontWeight = if (message.type == MessageType.GIFT_ANNOUNCEMENT) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        // Quick mod actions
        if (showModOptions && isModOrHost) {
            IconButton(onClick = onPin, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = VioraWarmGold, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VioraErrorRed, modifier = Modifier.size(14.dp))
            }
        }
    }
}
