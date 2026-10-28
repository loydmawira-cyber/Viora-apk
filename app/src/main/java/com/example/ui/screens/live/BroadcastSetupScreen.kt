package com.example.ui.screens.live

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.model.Country
import com.example.model.LiveStream
import com.example.model.POPULAR_COUNTRIES
import com.example.model.StreamCategory
import com.example.ui.components.LiveBadge
import com.example.ui.theme.*

@Composable
fun BroadcastSetupScreen(
    onBroadcastStarted: (LiveStream) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCancel() }

    var title by remember { mutableStateOf("Evening Chill & Acoustic Vibes 🎶") }
    var selectedCategory by remember { mutableStateOf(StreamCategory.MUSIC) }
    var selectedCountry by remember { mutableStateOf(POPULAR_COUNTRIES[1]) } // Default to US
    var isCameraFront by remember { mutableStateOf(true) }
    var isMicEnabled by remember { mutableStateOf(true) }
    var isCoinGated by remember { mutableStateOf(false) }
    var entryCoinPrice by remember { mutableLongStateOf(25L) }

    // Permissions state
    var hasCameraPermission by remember { mutableStateOf(true) }
    var hasAudioPermission by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: true
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] ?: true
    }

    val scrollState = rememberScrollState()
    val countryScrollState = rememberScrollState()

    val previewSnapshotUrl = remember(selectedCategory) {
        when (selectedCategory) {
            StreamCategory.MUSIC -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800"
            StreamCategory.GAMING -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800"
            StreamCategory.TALENT -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800"
            StreamCategory.FITNESS -> "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"
            StreamCategory.CREATIVE -> "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800"
            else -> "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
                    .testTag("cancel_setup_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }

            Text(
                text = "Broadcast Studio",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            IconButton(
                onClick = { isCameraFront = !isCameraFront },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = VioraElectricPurple)
            }
        }

        // Camera Preview Live Snapshot Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = previewSnapshotUrl,
                contentDescription = "Live camera snapshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay for camera HUD contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = VioraElectricPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isCameraFront) "Front Camera (1080p 60fps)" else "Rear Camera (1080p 60fps)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Region: ${selectedCountry.flagEmoji} ${selectedCountry.name}",
                        color = VioraWarmGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top overlay badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                LiveBadge(text = "PREVIEW")
            }

            // Mic indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (isMicEnabled) VioraSuccessGreen else VioraErrorRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isMicEnabled) "Mic Active" else "Muted",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Stream Title") },
            placeholder = { Text("Give your broadcast an exciting title...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stream_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VioraElectricPurple,
                unfocusedBorderColor = VioraBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Broadcast Country Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Broadcast Region & Country",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${selectedCountry.flagEmoji} ${selectedCountry.name}",
                    color = VioraWarmGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(countryScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                POPULAR_COUNTRIES.filter { it.code != "ALL" }.forEach { country ->
                    val isSelected = selectedCountry.code == country.code
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) VioraCoral else VioraCardDark)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) VioraWarmGold else VioraBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCountry = country }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(country.flagEmoji, fontSize = 14.sp)
                        Text(
                            text = country.code,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Category Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Select Category",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreamCategory.values().take(4).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) VioraElectricPurple else VioraCardDark)
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.title,
                            color = if (isSelected) Color.White else VioraMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreamCategory.values().drop(4).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) VioraElectricPurple else VioraCardDark)
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.title,
                            color = if (isSelected) Color.White else VioraMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Permissions Check Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VioraSuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("Broadcaster Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Camera & Mic Access Granted", color = VioraMutedText, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Verify", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Coin-Gated Live Stream Restriction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isCoinGated) VioraDeepViolet else VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isCoinGated) VioraWarmGold else VioraSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCoinGated) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isCoinGated) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Coin-Gated VIP Stream",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isCoinGated) "Viewers must unlock stream with coins" else "Public stream (Free for all viewers)",
                                color = if (isCoinGated) VioraWarmGold else VioraMutedText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isCoinGated,
                        onCheckedChange = { isCoinGated = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VioraCoral
                        ),
                        modifier = Modifier.testTag("coin_gate_switch")
                    )
                }

                if (isCoinGated) {
                    Text(
                        text = "Set Entry Ticket Price (Coins go to your earnings):",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10L, 25L, 50L, 100L, 200L).forEach { price ->
                            val isSelected = entryCoinPrice == price
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) VioraWarmGold else VioraSurfaceVariant)
                                    .clickable { entryCoinPrice = price }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$price 🪙",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "⭐ You will receive 70% of all ticket coins (${(entryCoinPrice * 0.70).toLong()} Diamonds) per viewer directly to your Creator Wallet.",
                        color = VioraWarmGold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start Live Button
        Button(
            onClick = {
                val newStream = VioraRepository.createLiveRoom(
                    title = title,
                    category = selectedCategory,
                    country = selectedCountry,
                    isCoinGated = isCoinGated,
                    entryCoinPrice = entryCoinPrice,
                    snapshotUrl = previewSnapshotUrl
                )
                onBroadcastStarted(newStream)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_broadcast_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioraCoral
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GO LIVE NOW",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
