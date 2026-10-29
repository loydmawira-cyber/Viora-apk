package com.example.ui.screens.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val featuredIds by VioraRepository.featuredStreamIds.collectAsState()
    val featuredStream = remember(liveStreams, selectedCountry, featuredIds) {
        liveStreams.firstOrNull { it.status == StreamStatus.LIVE && it.id in featuredIds } ?: liveStreams.firstOrNull {
            it.status == StreamStatus.LIVE &&
            (selectedCountry.code == "ALL" || it.countryCode.equals(selectedCountry.code, ignoreCase = true))
        } ?: liveStreams.firstOrNull { it.status == StreamStatus.LIVE }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraAppBg)
            .statusBarsPadding()
    ) {
        // Futuristic Holographic Top Header Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            // Background Canvas drawing holographic gold tech contour lines
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp)
            ) {
                val width = size.width
                val height = size.height

                // Subtle champagne gold grid lines & nodes
                val strokeWidth = 1.2.dp.toPx()
                val goldColor = Color(0x3BD9B56D)
                val glowNodeColor = Color(0xFFD9B56D)
                val violetNodeColor = Color(0x607655FF)

                // Top curved tech line
                val path1 = Path().apply {
                    moveTo(0f, height * 0.25f)
                    lineTo(width * 0.15f, height * 0.25f)
                    lineTo(width * 0.22f, height * 0.85f)
                    lineTo(width * 0.65f, height * 0.85f)
                    lineTo(width * 0.72f, height * 0.20f)
                    lineTo(width, height * 0.20f)
                }
                drawPath(
                    path = path1,
                    color = goldColor,
                    style = Stroke(width = strokeWidth)
                )

                // Tech nodes (glowing dots on line junctions)
                drawCircle(color = glowNodeColor, radius = 3.dp.toPx(), center = Offset(width * 0.22f, height * 0.85f))
                drawCircle(color = glowNodeColor, radius = 3.dp.toPx(), center = Offset(width * 0.65f, height * 0.85f))
                drawCircle(color = violetNodeColor, radius = 4.dp.toPx(), center = Offset(width * 0.72f, height * 0.20f))
            }

            // Top Header Content Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Brand Monogram & Wordmark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular V Monogram Badge with Champagne Gold gradient ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        VioraPrimaryChampagneGold,
                                        VioraGoldHighlight,
                                        VioraDarkChampagneGold,
                                        VioraPrimaryChampagneGold
                                    )
                                ),
                                shape = CircleShape
                            )
                            .background(VioraCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = VioraPrimaryText,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Text(
                        text = "Viora",
                        color = VioraPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right Action Items: Coin balance pill + Search button + Profile avatar with gold ring
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Coin Balance Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(VioraCardDark)
                            .border(1.dp, VioraGoldBorder, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToWallet() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Round gold coin icon
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(VioraGoldHighlight, VioraPrimaryChampagneGold, VioraDarkChampagneGold)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "${currentUser.coinBalance}",
                            color = VioraPrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Glass Search button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VioraCardDark)
                            .border(1.dp, VioraGoldBorder, CircleShape)
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VioraPrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Profile Avatar with Gold Ring & Green Online Status Dot
                    Box(
                        modifier = Modifier.clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, VioraPrimaryChampagneGold, CircleShape)
                        ) {
                            AvatarView(
                                imageUrl = currentUser.avatarUrl,
                                displayName = currentUser.displayName,
                                size = 40.dp,
                                showBorder = false
                            )
                        }
                        // Green Online Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(VioraOnlineGreen)
                                .border(1.5.dp, VioraAppBg, CircleShape)
                        )
                    }
                }
            }
        }

        // Optional Search Bar Input
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search creators, country, tags, or topics...", color = VioraMutedText, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VioraPrimaryChampagneGold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioraPrimaryChampagneGold,
                    unfocusedBorderColor = VioraGoldBorder,
                    focusedTextColor = VioraPrimaryText,
                    unfocusedTextColor = VioraPrimaryText,
                    focusedContainerColor = VioraCardDark,
                    unfocusedContainerColor = VioraCardDark
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Live Now vs Following Feed Selector + Region Pill Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Live Now (${filteredStreams.size})",
                    color = if (selectedTab == 0) VioraPrimaryText else VioraMutedText,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .clickable { selectedTab = 0 }
                        .testTag("tab_live_now")
                )

                Text(
                    text = "Following",
                    color = if (selectedTab == 1) VioraPrimaryText else VioraMutedText,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .clickable { selectedTab = 1 }
                        .testTag("tab_following")
                )
            }

            // Region / Country selector pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(VioraCardDark)
                    .border(1.dp, VioraGoldBorder, RoundedCornerShape(16.dp))
                    .clickable { showCountryPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("country_top_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(selectedCountry.flagEmoji, fontSize = 14.sp)
                Text(
                    text = if (selectedCountry.code == "ALL") "All Regions" else selectedCountry.name,
                    color = VioraPrimaryChampagneGold,
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

        // CATEGORY FILTER CHIPS (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState)
                .padding(horizontal = 14.dp, vertical = 6.dp),
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
                                    listOf(VioraPrimaryChampagneGold, VioraDarkChampagneGold)
                                )
                            } else {
                                Brush.linearGradient(listOf(VioraCardDark, VioraCardDark))
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) VioraGoldHighlight else VioraGoldBorderSubtle,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("category_pill_${category.name}")
                ) {
                    Text(
                        text = category.title,
                        color = if (isSelected) Color.Black else VioraSecondaryText,
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
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
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
                            tint = VioraLiveCoralPink,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (selectedCountry.code != "ALL") "No live streams in ${selectedCountry.name} ${selectedCountry.flagEmoji}" else "No live broadcasts found",
                            color = VioraPrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Try switching to Global 🌍 or selecting a different category",
                            color = VioraMutedText,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                selectedCountry = POPULAR_COUNTRIES.first()
                                selectedCategory = StreamCategory.ALL
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryChampagneGold),
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
                Text("Select Region / Country", color = VioraPrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                    .background(if (isSelected) VioraPrimaryChampagneGold else VioraCardElevated)
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
                                        color = if (isSelected) Color.Black else VioraPrimaryText,
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
                                            color = if (isSelected) Color.Black else VioraPrimaryChampagneGold,
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
                        color = VioraPrimaryText,
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
                            borderColor = VioraPrimaryChampagneGold
                        )
                        Column {
                            Text(
                                text = targetStream.hostName,
                                color = VioraPrimaryText,
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
                                Text("${targetStream.entryCoinPrice} Coins", color = VioraPrimaryChampagneGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your Balance", color = VioraMutedText, fontSize = 11.sp)
                                Text("${currentUser.coinBalance} Coins", color = VioraPrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            color = VioraLiveCoralPink,
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
                        colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryChampagneGold),
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
                        colors = ButtonDefaults.buttonColors(containerColor = VioraLiveCoralPink),
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
            .border(1.dp, VioraGoldBorder, RoundedCornerShape(20.dp))
            .clickable { onWatch() }
            .testTag("featured_hero_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VioraCardDark),
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
                                colors = listOf(VioraDeepViolet, VioraCardElevated, VioraAppBg)
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
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.65f),
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
                                .background(Color.Black.copy(alpha = 0.60f))
                                .border(0.5.dp, VioraGoldBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${stream.countryFlag} ${stream.countryName}",
                                color = VioraPrimaryChampagneGold,
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
                            .background(Color.Black.copy(alpha = 0.60f))
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
                                color = VioraPrimaryChampagneGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = onWatch,
                        colors = ButtonDefaults.buttonColors(containerColor = VioraPrimaryChampagneGold),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch Live", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
