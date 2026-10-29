package com.example.ui.screens.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.Conversation
import com.example.ui.components.AvatarView
import com.example.ui.theme.*

@Composable
fun InboxScreen(
    onOpenConversation: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by VioraRepository.conversations.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Unread, 2: Creators, 3: Official
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filterScrollState = rememberScrollState()
    val activeNowScrollState = rememberScrollState()

    val filteredConversations = remember(conversations, selectedFilter, searchQuery) {
        conversations.filter { conv ->
            val matchesFilter = when (selectedFilter) {
                1 -> conv.unreadCount > 0
                2 -> conv.isCreator
                3 -> !conv.isCreator
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                conv.participantName.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val totalUnread = conversations.sumOf { it.unreadCount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Inbox",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )

                if (totalUnread > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VioraCoral)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalUnread",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(
                onClick = { isSearchActive = !isSearchActive },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
                    .testTag("inbox_search_button")
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Input
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages or creators...", color = VioraMutedText, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VioraElectricPurple) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("inbox_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioraElectricPurple,
                    unfocusedBorderColor = VioraBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Active Now Online Creators Avatar Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Active Creators",
                color = VioraMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(activeNowScrollState)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                conversations.filter { it.isOnline }.forEach { conv ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onOpenConversation(conv) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarView(
                                imageUrl = conv.participantAvatar,
                                displayName = conv.participantName,
                                size = 46.dp,
                                showBorder = true,
                                isVerified = conv.verifiedBadge
                            )
                            // Green Online Indicator Dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(VioraSuccessGreen)
                                    .border(2.dp, VioraDarkBg, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conv.participantName.split(" ").firstOrNull() ?: conv.participantName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(filterScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All Messages", "Unread", "Creators", "Official").forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VioraElectricPurple else VioraCardDark)
                        .clickable { selectedFilter = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("inbox_filter_$index")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else VioraMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Conversation List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
        ) {
            if (filteredConversations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = VioraMutedText,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No messages found",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Send a virtual gift during live streams to connect with creators!",
                            color = VioraMutedText,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(filteredConversations, key = { it.id }) { conv ->
                    ConversationItemCard(
                        conversation = conv,
                        onClick = { onOpenConversation(conv) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItemCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("conversation_item_${conversation.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (conversation.unreadCount > 0) VioraCardElevated else VioraCardDark
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarView(
                    imageUrl = conversation.participantAvatar,
                    displayName = conversation.participantName,
                    size = 46.dp,
                    showBorder = conversation.unreadCount > 0,
                    isVerified = conversation.verifiedBadge
                )
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(VioraSuccessGreen)
                            .border(1.5.dp, VioraCardDark, CircleShape)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.participantName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = conversation.lastMessageTimestamp,
                        color = if (conversation.unreadCount > 0) VioraCoral else VioraSubtleText,
                        fontSize = 11.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        color = if (conversation.unreadCount > 0) Color.White else VioraMutedText,
                        fontSize = 12.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VioraCoral)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${conversation.unreadCount}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
