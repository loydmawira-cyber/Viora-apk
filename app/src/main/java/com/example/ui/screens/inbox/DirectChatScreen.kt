package com.example.ui.screens.inbox

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.ui.components.AvatarView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DirectChatScreen(
    conversation: Conversation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val directMessagesMap by VioraRepository.directMessagesMap.collectAsState()
    val messages = directMessagesMap[conversation.id] ?: emptyList()

    var inputText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Mark conversation as read on entry
    LaunchedEffect(conversation.id) {
        VioraRepository.markConversationRead(conversation.id)
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Chat Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VioraCardDark,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("direct_chat_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarView(
                            imageUrl = conversation.participantAvatar,
                            displayName = conversation.participantName,
                            size = 38.dp,
                            showBorder = true,
                            isVerified = conversation.verifiedBadge
                        )
                        if (conversation.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(VioraSuccessGreen)
                                    .border(1.5.dp, VioraCardDark, CircleShape)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = conversation.participantName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (conversation.isOnline) "Active now" else "Offline",
                            color = if (conversation.isOnline) VioraSuccessGreen else VioraMutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { toastMessage = "Agora 1-on-1 Video Call simulation connected!" }
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = VioraElectricPurple)
                    }
                    IconButton(
                        onClick = { toastMessage = "Creator Profile viewed" }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = VioraMutedText)
                    }
                }
            }
        }

        // Call Simulation Toast if triggered
        toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                delay(3000)
                toastMessage = null
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VioraElectricPurple)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
        }

        // Quick Emoji Reaction Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("❤️", "🔥", "🌹", "🏎️", "👏", "✨").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(VioraCardDark)
                        .clickable {
                            VioraRepository.sendDirectMessage(conversation.id, emoji)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VioraCardDark,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Send a message...", color = VioraMutedText, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("direct_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioraElectricPurple,
                        unfocusedBorderColor = VioraBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = VioraDarkBg,
                        unfocusedContainerColor = VioraDarkBg
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            VioraRepository.sendDirectMessage(conversation.id, inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VioraElectricPurple)
                        .testTag("send_direct_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: DirectMessage) {
    val isMe = message.isFromMe
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestampMs) { timeFormat.format(Date(message.timestampMs)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) {
                            Brush.horizontalGradient(listOf(VioraElectricPurple, Color(0xFF904EFA)))
                        } else {
                            Brush.horizontalGradient(listOf(VioraCardElevated, VioraSurfaceVariant))
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = formattedTime,
                    color = VioraSubtleText,
                    fontSize = 10.sp
                )
                if (isMe) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = VioraCoral,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
