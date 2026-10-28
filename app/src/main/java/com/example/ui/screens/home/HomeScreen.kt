package com.example.ui.screens.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VioraRepository
import com.example.model.*
import com.example.ui.components.AvatarView
import com.example.ui.components.LiveBadge
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

    val featuredStream = remember(liveStreams, selectedCountry) {
        liveStreams.firstOrNull {
            it.status == StreamStatus.LIVE &&
            (selectedCountry.code == "ALL" || it.countryCode.equals(selectedCountry.code, ignoreCase = true))
        } ?: liveStreams.firstOrNull { it.status == StreamStatus.LIVE }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VioraElectricPurple, VioraCoral)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = "Viora",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Go live. Find your people.",
                        color = VioraCoral,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right Action Items: Coins pill + Filter icon + Search + Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Quick Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(VioraCardDark)
                        .clickable { onNavigateToWallet() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = VioraWarmGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${currentUser.coinBalance}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search toggle
                IconButton(
                    onClick = { isSearchActive = !isSearchActive },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VioraCardDark)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AvatarView(
                    imageUrl = currentUser.avatarUrl,
                    displayName = currentUser.displayName,
                    size = 36.dp,
                    showBorder = true,
                    modifier = Modifier.clickable { onNavigateToProfile() }
                )
            }
        }

        // Optional Search Bar
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search creators, country, tags, or topics...", color = VioraMutedText, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VioraElectricPurple) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioraElectricPurple,
                    unfocusedBorderColor = VioraBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Live Now vs Following Feed Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Live Now (${filteredStreams.size})",
                    color = if (selectedTab == 0) Color.White else VioraMutedText,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { selectedTab = 0 }
                        .testTag("tab_live_now")
                )

                Text(
                    text = "Following",
                    color = if (selectedTab == 1) Color.White else VioraMutedText,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { selectedTab = 1 }
                        .testTag("tab_following")
                )
            }

            // Quick Active Country Selector on top
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(VioraCardDark)
                    .border(1.dp, VioraBorder, RoundedCornerShape(14.dp))
                    .clickable { showCountryPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("country_top_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(selectedCountry.flagEmoji, fontSize = 14.sp)
                Text(
                    text = if (selectedCountry.code == "ALL") "All Regions" else selectedCountry.name,
                    color = VioraWarmGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Country",
                    tint = VioraMutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // CATEGORY FILTER CHIPS (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StreamCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) VioraElectricPurple else VioraCardDark)
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("category_pill_${category.name}")
                ) {
                    Text(
                        text = category.title,
                        color = if (isSelected) Color.White else VioraMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Stream List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
        ) {
            // Featured Spotlight Hero on Live Now tab when All categories and blank search
            if (selectedTab == 0 && searchQuery.isBlank() && selectedCategory == StreamCategory.ALL && featuredStream != null) {
                item {
                    FeaturedHeroBanner(
                        stream = featuredStream,
                        onWatch = { handleStreamClick(featuredStream) }
                    )
                }
            }

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
                            tint = VioraCoral,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (selectedCountry.code != "ALL") "No live streams in ${selectedCountry.name} ${selectedCountry.flagEmoji}" else "No live broadcasts found",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Try switching to Global 🌍 or selecting a different country filter",
                            color = VioraMutedText,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                selectedCountry = POPULAR_COUNTRIES.first()
                                selectedCategory = StreamCategory.ALL
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Filter to All Countries", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // Direct Country Picker Dialog from top selector
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
                                    .background(if (isSelected) VioraCoral else VioraSurfaceVariant)
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
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }

                                if (streamCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else VioraCardDark)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$streamCount live",
                                            color = if (isSelected) Color.White else VioraWarmGold,
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
                    Text("Close", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // Coin-Gated Stream Unlock Dialog
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
                            showBorder = true
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
                                color = VioraMutedText,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }

                    // Ticket Price Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VioraDeepViolet),
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
                                Text("Entry Ticket", color = VioraMutedText, fontSize = 11.sp)
                                Text("${targetStream.entryCoinPrice} Coins", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your Balance", color = VioraMutedText, fontSize = 11.sp)
                                Text("${currentUser.coinBalance} Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // Earnings info
                    Text(
                        text = "💎 Broadcaster receives 70% in Diamonds (${(targetStream.entryCoinPrice * 0.70).toLong()} Diamonds) as creator earnings. Unlocking grants you full access to this live room.",
                        color = VioraMutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (currentUser.coinBalance < targetStream.entryCoinPrice) {
                        Text(
                            text = "⚠️ Insufficient coins (Need ${targetStream.entryCoinPrice - currentUser.coinBalance} more coins)",
                            color = VioraCoral,
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
                        colors = ButtonDefaults.buttonColors(containerColor = VioraWarmGold),
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
                        colors = ButtonDefaults.buttonColors(containerColor = VioraCoral),
                        modifier = Modifier.testTag("recharge_coins_button")
                    ) {
                        Text("Recharge Coins", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { streamToUnlock = null }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun FeaturedHeroBanner(
    stream: LiveStream,
    onWatch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onWatch() }
            .testTag("featured_hero_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VioraDeepViolet),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 175.dp)
        ) {
            // Live broadcast snapshot photo
            if (stream.snapshotUrl.isNotBlank()) {
                AsyncImage(
                    model = stream.snapshotUrl,
                    contentDescription = "Live snapshot of ${stream.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VioraDeepViolet, VioraElectricPurple, Color(0xFF2A0845))
                            )
                        )
                )
            }

            // High contrast gradient scrim for text readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveBadge(text = "FEATURED LIVE")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${stream.countryFlag} ${stream.countryName}",
                                color = VioraWarmGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Viewers",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${stream.viewerCount} watching",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = stream.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvatarView(
                            imageUrl = stream.hostAvatar,
                            displayName = stream.hostName,
                            size = 34.dp,
                            isVerified = true
                        )
                        Column {
                            Text(
                                text = stream.hostName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stream.category.title,
                                color = VioraWarmGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = onWatch,
                        colors = ButtonDefaults.buttonColors(containerColor = VioraCoral),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
