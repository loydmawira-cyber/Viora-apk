package com.example.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.*
import com.example.ui.components.AvatarView
import com.example.ui.components.StreamCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectStream: (LiveStream) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveStreams by VioraRepository.liveStreams.collectAsState()
    val currentUser by VioraRepository.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Now, 1: Following
    var selectedCategory by remember { mutableStateOf(StreamCategory.ALL) }
    var selectedCountry by remember { mutableStateOf(POPULAR_COUNTRIES.first()) } // "ALL" / Global
    var selectedSortOrder by remember { mutableStateOf(StreamSortOrder.POPULAR) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var streamToUnlock by remember { mutableStateOf<LiveStream?>(null) }

    val handleStreamClick: (LiveStream) -> Unit = { stream ->
        val isHost = stream.hostId == currentUser.id
        val isAlreadyUnlocked = stream.unlockedByUserIds.contains(currentUser.id)
        if (stream.isCoinGated && !isHost && !isAlreadyUnlocked) {
            streamToUnlock = stream
        } else {
            onSelectStream(stream)
        }
    }

    val categoryScrollState = rememberScrollState()

    val filteredStreams = remember(liveStreams, selectedTab, selectedCategory, selectedCountry, selectedSortOrder, searchQuery) {
        val list = liveStreams.filter { stream ->
            stream.status == StreamStatus.LIVE &&
            (selectedTab == 0 || stream.isFollowing) &&
            (selectedCategory == StreamCategory.ALL || stream.category == selectedCategory) &&
            (selectedCountry.code == "ALL" || stream.countryCode.equals(selectedCountry.code, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
             stream.title.contains(searchQuery, ignoreCase = true) ||
             stream.hostName.contains(searchQuery, ignoreCase = true) ||
             stream.countryName.contains(searchQuery, ignoreCase = true) ||
             stream.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }

        when (selectedSortOrder) {
            StreamSortOrder.POPULAR -> list.sortedByDescending { it.viewerCount }
            StreamSortOrder.TRENDING -> list.sortedByDescending { it.likeCount }
            StreamSortOrder.NEWEST -> list.sortedByDescending { it.startedAtEpochMs }
            StreamSortOrder.DIAMONDS -> list.sortedByDescending { it.diamondCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraAppBg)
            .statusBarsPadding()
    ) {
        // Top Header Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(VioraPlumAccentBg, VioraSecondaryBg, VioraAppBg)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                VioraNeonPurple.copy(alpha = 0.72f),
                                VioraGlassBorder,
                                VioraPrimaryGold.copy(alpha = 0.42f),
                                VioraGlassBorder
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                // Top Header Content Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Viora app icon next to bold white App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(VioraNeonPurple, Color(0xFF3B1D5A))
                                    )
                                )
                                .border(
                                    width = 1.8.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(VioraNeonPurple, VioraNeonPurpleLight, VioraPrimaryGold, VioraNeonPurple)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.viora_icon),
                                contentDescription = "Viora app icon",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "Viora",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Right side: Coin balance pill + Search + Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pill-shaped coin balance badge (gold border, gold coin icon)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(VioraGlassSurface)
                                .border(1.dp, VioraGoldBorder, RoundedCornerShape(20.dp))
                                .clickable { onNavigateToWallet() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(VioraGoldHighlight, VioraPrimaryGold, VioraDarkGold)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "${currentUser.coinBalance}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Circular glassmorphism search icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VioraStrongGlassSurface)
                                .border(1.dp, VioraTranslucentPurple, CircleShape)
                                .clickable { isSearchActive = !isSearchActive },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Circular user avatar with green online dot
                        Box(
                            modifier = Modifier.clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.8.dp, VioraPrimaryGold, CircleShape)
                            ) {
                                AvatarView(
                                    imageUrl = currentUser.avatarUrl,
                                    displayName = currentUser.displayName,
                                    size = 42.dp,
                                    showBorder = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(VioraOnlineGreen)
                                    .border(1.5.dp, VioraAppBg, CircleShape)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    VioraNeonPurple.copy(alpha = 0.55f),
                                    VioraGlassBorder,
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Sub-Header Tabs: "Live Now (7)" in bold white, "Following" in gray, Far right glassmorphism pill dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clickable { selectedTab = 0 }
                                .testTag("tab_live_now")
                        ) {
                            Text(
                                text = "Live Now (${filteredStreams.size})",
                                color = if (selectedTab == 0) Color.White else VioraSecondaryText,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(if (selectedTab == 0) 46.dp else 0.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(VioraNeonPurple, VioraNeonPurpleLight)
                                        )
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clickable { selectedTab = 1 }
                                .testTag("tab_following")
                        ) {
                            Text(
                                text = "Following",
                                color = if (selectedTab == 1) Color.White else VioraSecondaryText,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(if (selectedTab == 1) 46.dp else 0.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(VioraNeonPurple, VioraNeonPurpleLight)
                                        )
                                    )
                            )
                        }
                    }

                    // Glassmorphism pill dropdown for "All Regions" with globe icon
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(VioraGlassSurface)
                            .border(1.dp, VioraGlassBorder, RoundedCornerShape(16.dp))
                            .clickable { showCountryPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("country_top_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(selectedCountry.flagEmoji, fontSize = 14.sp)
                        Text(
                            text = if (selectedCountry.code == "ALL") "All Regions" else selectedCountry.name,
                            color = VioraPrimaryGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Country",
                            tint = VioraSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Filter Bar: Horizontally scrollable pill-shaped chips.
        // Active chip ("All") has solid gold gradient background with black text. Inactive chips have dark glassmorphism background with white text.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreamCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    listOf(VioraPrimaryGold, VioraDarkGold)
                                )
                            } else {
                                Brush.linearGradient(listOf(VioraGlassSurface, VioraGlassSurface))
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) VioraGoldHighlight else VioraGlassBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("category_pill_${category.name}")
                ) {
                    Text(
                        text = category.title,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }

        // Stream Feed List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            if (filteredStreams.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = VioraLiveBadgeStart,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (selectedCountry.code != "ALL") "No live streams in ${selectedCountry.name} ${selectedCountry.flagEmoji}" else "No live broadcasts found",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Try switching to Global 🌍 or selecting a different category",
                            color = VioraSecondaryText,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                selectedCountry = POPULAR_COUNTRIES.first()
                                selectedCategory = StreamCategory.ALL
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryGold),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Filters", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredStreams, key = { it.id }) { stream ->
                    StreamCard(
                        stream = stream,
                        onClick = { handleStreamClick(stream) },
                        onToggleFollow = { VioraRepository.toggleFollowStream(stream.id) }
                    )
                }
            }
        }
    }

    // Direct Country / Region Picker Dialog
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = {
                Text("Select Region / Country", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(POPULAR_COUNTRIES) { country ->
                            val isSelected = selectedCountry.code == country.code
                            val streamCount = if (country.code == "ALL") {
                                liveStreams.count { it.status == StreamStatus.LIVE }
                            } else {
                                liveStreams.count { it.status == StreamStatus.LIVE && it.countryCode.equals(country.code, ignoreCase = true) }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) VioraPrimaryGold else VioraGlassSurface)
                                    .border(1.dp, if (isSelected) VioraGoldHighlight else VioraGlassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedCountry = country
                                        showCountryPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("country_picker_option_${country.code}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(country.flagEmoji, fontSize = 20.sp)
                                    Text(
                                        text = country.name,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }

                                if (streamCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color.Black.copy(alpha = 0.20f) else VioraCardDark)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$streamCount live",
                                            color = if (isSelected) Color.Black else VioraPrimaryGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("Close", color = VioraSecondaryText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // Coin-Gated VIP Stream Unlock Dialog
    if (streamToUnlock != null) {
        val targetStream = streamToUnlock!!
        AlertDialog(
            onDismissRequest = { streamToUnlock = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔒", fontSize = 20.sp)
                    Text(
                        text = "VIP Live Stream",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Host info & Stream Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarView(
                            imageUrl = targetStream.hostAvatar,
                            displayName = targetStream.hostName,
                            size = 46.dp,
                            showBorder = true,
                            borderColor = VioraPrimaryGold
                        )
                        Column {
                            Text(
                                text = targetStream.hostName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = targetStream.title,
                                color = VioraSecondaryText,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }

                    // Ticket Price Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VioraTranslucentPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Entry Ticket", color = VioraSecondaryText, fontSize = 11.sp)
                                Text("${targetStream.entryCoinPrice} Coins", color = VioraPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your Balance", color = VioraSecondaryText, fontSize = 11.sp)
                                Text("${currentUser.coinBalance} Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // Earnings info
                    Text(
                        text = "💎 Broadcaster receives 70% in Diamonds (${(targetStream.entryCoinPrice * 0.70).toLong()} Diamonds) as creator earnings. Unlocking grants you full access to this live room.",
                        color = VioraSecondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (currentUser.coinBalance < targetStream.entryCoinPrice) {
                        Text(
                            text = "⚠️ Insufficient coins (Need ${targetStream.entryCoinPrice - currentUser.coinBalance} more coins)",
                            color = VioraLiveBadgeStart,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                if (currentUser.coinBalance >= targetStream.entryCoinPrice) {
                    Button(
                        onClick = {
                            val result = VioraRepository.unlockLiveStream(targetStream.id)
                            streamToUnlock = null
                            if (result.isSuccess) {
                                onSelectStream(result.getOrThrow())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryGold),
                        modifier = Modifier.testTag("unlock_stream_button")
                    ) {
                        Text("Unlock & Enter (${targetStream.entryCoinPrice} Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            streamToUnlock = null
                            onNavigateToWallet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraLiveBadgeStart),
                        modifier = Modifier.testTag("recharge_coins_button")
                    ) {
                        Text("Recharge Coins", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { streamToUnlock = null }) {
                    Text("Cancel", color = VioraSecondaryText)
                }
            },
            containerColor = VioraCardDark
        )
    }
}
