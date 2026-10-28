package com.example.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VioraRepository
import com.example.model.MediaType
import com.example.model.ProfileMedia
import com.example.model.UserRole
import com.example.ui.components.AvatarView
import com.example.ui.theme.*

private val PRESET_MEDIA_SAMPLES = listOf(
    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600",
    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600",
    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600"
)

@Composable
fun ProfileScreen(
    onNavigateToWallet: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBroadcastSetup: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by VioraRepository.currentUser.collectAsState()
    val profileMediaList by VioraRepository.profileMedia.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedMediaForPreview by remember { mutableStateOf<ProfileMedia?>(null) }
    var mediaToUnlock by remember { mutableStateOf<ProfileMedia?>(null) }
    var mediaFilter by remember { mutableIntStateOf(0) } // 0: All, 1: My Posts, 2: Locked VIP

    // Upload draft state
    var uploadMediaType by remember { mutableStateOf(MediaType.PHOTO) }
    var uploadMediaUrl by remember { mutableStateOf(PRESET_MEDIA_SAMPLES.first()) }
    var uploadCaption by remember { mutableStateOf("") }
    var uploadIsLocked by remember { mutableStateOf(true) }
    var uploadCoinPrice by remember { mutableLongStateOf(30L) }

    // Android Zero-Permission Photo Picker
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadMediaUrl = uri.toString()
            showUploadDialog = true
        }
    }

    val filteredMedia = remember(profileMediaList, mediaFilter, currentUser.id) {
        when (mediaFilter) {
            1 -> profileMediaList.filter { it.userId == currentUser.id }
            2 -> profileMediaList.filter { it.isLocked }
            else -> profileMediaList
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
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
            Text(
                text = "My Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
                    .testTag("profile_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    imageUrl = currentUser.avatarUrl,
                    displayName = currentUser.displayName,
                    size = 76.dp,
                    showBorder = true,
                    isVerified = currentUser.isVerified
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentUser.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "@${currentUser.username}",
                        color = VioraCoral,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (currentUser.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser.bio,
                            color = VioraMutedText,
                            fontSize = 12.sp
                        )
                    }
                }

                // Level & Role Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VioraElectricPurple)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Level ${currentUser.level}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (currentUser.role) {
                                    UserRole.ADMIN -> VioraWarmGold
                                    UserRole.MODERATOR -> VioraCoral
                                    UserRole.BROADCASTER -> VioraElectricPurple
                                    UserRole.VIEWER -> VioraSurfaceVariant
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = currentUser.role.name,
                            color = if (currentUser.role == UserRole.ADMIN) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stats: Followers / Following / Coins / Diamonds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(label = "Followers", value = "${currentUser.followersCount}")
                    StatColumn(label = "Following", value = "${currentUser.followingCount}")
                    StatColumn(label = "Coins", value = "${currentUser.coinBalance}")
                    StatColumn(label = "Diamonds", value = "${currentUser.diamondBalance}")
                }

                // Edit Profile Button
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile Details", fontSize = 12.sp)
                }
            }
        }

        // Live Role Switcher (For convenient MVP testing of all role requirements)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraDeepViolet),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Switch Active Role (Testing)",
                    color = VioraWarmGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Test the app experience as a Viewer, Broadcaster, Moderator, or Admin:",
                    color = VioraMutedText,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.values().forEach { role ->
                        val isSelected = currentUser.role == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VioraCoral else VioraCardDark)
                                .clickable { VioraRepository.switchRole(role) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role.name.take(4),
                                color = if (isSelected) Color.White else VioraMutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // EXCLUSIVE MEDIA & POSTS SECTION (Photo/Video Uploads & Coin Unlock)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section Header + Upload Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Exclusive Media Vault",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text("🔒", fontSize = 14.sp)
                        }
                        Text(
                            text = "Coin-locked photos & videos earn diamonds",
                            color = VioraMutedText,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { showUploadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("upload_media_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Filter Tabs: All / My Posts / Locked VIP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "All (${profileMediaList.size})",
                        "My Posts (${profileMediaList.count { it.userId == currentUser.id }})",
                        "Locked VIP (${profileMediaList.count { it.isLocked }})"
                    )
                    tabs.forEachIndexed { index, label ->
                        val isSelected = mediaFilter == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VioraCoral else VioraSurfaceVariant)
                                .clickable { mediaFilter = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else VioraMutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Media Items Grid (2 Columns)
                if (filteredMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No media posts yet in this category.",
                            color = VioraMutedText,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Chunk list into pairs of 2 for grid layout
                    val chunkedList = filteredMedia.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunkedList.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { media ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        MediaItemCard(
                                            media = media,
                                            currentUserId = currentUser.id,
                                            onUnlock = { mediaToUnlock = media },
                                            onPreview = { selectedMediaForPreview = media }
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Actions List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Admin Dashboard Link (Always accessible if Admin)
            if (currentUser.role == UserRole.ADMIN) {
                ProfileMenuRow(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Dashboard & Moderation Queue",
                    subtitle = "Review reports, platform metrics & rooms",
                    tint = VioraWarmGold,
                    onClick = onNavigateToAdmin,
                    testTag = "admin_dashboard_menu"
                )
            }

            ProfileMenuRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Wallet & Creator Earnings",
                subtitle = "Recharge coins & cash out earnings",
                tint = VioraWarmGold,
                onClick = onNavigateToWallet,
                testTag = "wallet_menu"
            )

            ProfileMenuRow(
                icon = Icons.Default.Podcasts,
                title = "Broadcaster Studio",
                subtitle = "Configure camera, microphone & start stream",
                tint = VioraCoral,
                onClick = onNavigateToBroadcastSetup,
                testTag = "broadcast_studio_menu"
            )

            ProfileMenuRow(
                icon = Icons.Default.Logout,
                title = "Sign Out",
                subtitle = "End your active session",
                tint = VioraErrorRed,
                onClick = onLogout,
                testTag = "logout_menu"
            )
        }
    }

    // 1. Edit Profile Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(currentUser.displayName) }
        var editBio by remember { mutableStateOf(currentUser.bio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3,
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
                        VioraRepository.updateProfile(editName, editBio)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // 2. Upload Profile Media Dialog
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📸", fontSize = 20.sp)
                    Text("Publish Exclusive Media", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type selector: Photo or Video
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(MediaType.PHOTO to "Photo 📷", MediaType.VIDEO to "Video 🎥").forEach { (type, label) ->
                            val isSelected = uploadMediaType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) VioraElectricPurple else VioraSurfaceVariant)
                                    .clickable { uploadMediaType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else VioraMutedText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Device Photo Picker trigger
                    OutlinedButton(
                        onClick = {
                            pickMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_device_media_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VioraCoral),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Device Gallery", fontSize = 12.sp)
                    }

                    // Media Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraDeepViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uploadMediaUrl,
                            contentDescription = "Selected media preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (uploadMediaType == MediaType.VIDEO) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    // Caption input
                    OutlinedTextField(
                        value = uploadCaption,
                        onValueChange = { uploadCaption = it },
                        label = { Text("Caption / Title") },
                        placeholder = { Text("e.g. VIP Soundcheck rehearsal...") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caption_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Coin Lock Switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VioraDeepViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Lock with Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Viewers spend coins to view", color = VioraMutedText, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = uploadIsLocked,
                                    onCheckedChange = { uploadIsLocked = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = VioraWarmGold
                                    ),
                                    modifier = Modifier.testTag("lock_switch")
                                )
                            }

                            if (uploadIsLocked) {
                                Text("Select Entry Price:", color = VioraWarmGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(15L, 30L, 50L, 100L).forEach { price ->
                                        val isSelected = uploadCoinPrice == price
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) VioraWarmGold else VioraCardDark)
                                                .clickable { uploadCoinPrice = price }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$price🪙",
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "💎 Creator Share: 70% converts to Diamonds for your earnings (${(uploadCoinPrice * 0.70).toLong()} Diamonds per unlock).",
                                    color = VioraMutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val caption = uploadCaption.ifBlank {
                            if (uploadMediaType == MediaType.VIDEO) "Exclusive VIP Video Clip 🎥" else "Exclusive VIP Photo Post 📸"
                        }
                        VioraRepository.uploadProfileMedia(
                            mediaType = uploadMediaType,
                            mediaUrl = uploadMediaUrl,
                            caption = caption,
                            isLocked = uploadIsLocked,
                            coinPrice = if (uploadIsLocked) uploadCoinPrice else 0L
                        )
                        showUploadDialog = false
                        uploadCaption = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                    modifier = Modifier.testTag("publish_media_button")
                ) {
                    Text("Publish to Profile", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // 3. Unlock Exclusive Media Dialog
    if (mediaToUnlock != null) {
        val targetMedia = mediaToUnlock!!
        AlertDialog(
            onDismissRequest = { mediaToUnlock = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💎", fontSize = 20.sp)
                    Text("Unlock Exclusive Media", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarView(
                            imageUrl = targetMedia.authorAvatar,
                            displayName = targetMedia.authorName,
                            size = 44.dp,
                            showBorder = true
                        )
                        Column {
                            Text(targetMedia.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = targetMedia.caption,
                                color = VioraMutedText,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }

                    // Price Card
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
                                Text("Unlock Cost", color = VioraMutedText, fontSize = 11.sp)
                                Text("${targetMedia.coinPrice} Coins", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your Balance", color = VioraMutedText, fontSize = 11.sp)
                                Text("${currentUser.coinBalance} Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    Text(
                        text = "💎 70% of coins (${(targetMedia.coinPrice * 0.70).toLong()} Diamonds) is credited directly to ${targetMedia.authorName}'s creator earnings.",
                        color = VioraMutedText,
                        fontSize = 11.sp
                    )

                    if (currentUser.coinBalance < targetMedia.coinPrice) {
                        Text(
                            text = "⚠️ Insufficient coins (Need ${targetMedia.coinPrice - currentUser.coinBalance} more coins)",
                            color = VioraCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                if (currentUser.coinBalance >= targetMedia.coinPrice) {
                    Button(
                        onClick = {
                            val res = VioraRepository.unlockProfileMedia(targetMedia.id)
                            mediaToUnlock = null
                            if (res.isSuccess) {
                                selectedMediaForPreview = res.getOrThrow()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraWarmGold),
                        modifier = Modifier.testTag("unlock_media_confirm_button")
                    ) {
                        Text("Unlock Now (${targetMedia.coinPrice} Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            mediaToUnlock = null
                            onNavigateToWallet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraCoral),
                        modifier = Modifier.testTag("recharge_for_media_button")
                    ) {
                        Text("Recharge Coins", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToUnlock = null }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }

    // 4. Full-Screen Media Viewer Dialog
    if (selectedMediaForPreview != null) {
        val media = selectedMediaForPreview!!
        var isPlayingVideo by remember { mutableStateOf(true) }
        var likesCount by remember { mutableIntStateOf(media.likesCount) }
        var hasLiked by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedMediaForPreview = null },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Author Header
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
                                imageUrl = media.authorAvatar,
                                displayName = media.authorName,
                                size = 36.dp,
                                showBorder = true
                            )
                            Column {
                                Text(media.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (media.mediaType == MediaType.VIDEO) "Video Post" else "Photo Post", color = VioraMutedText, fontSize = 10.sp)
                            }
                        }

                        if (media.isLocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VioraWarmGold)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("🔓 Unlocked VIP", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Media Display Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = media.mediaUrl,
                            contentDescription = media.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (media.mediaType == MediaType.VIDEO) {
                            IconButton(
                                onClick = { isPlayingVideo = !isPlayingVideo },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = if (isPlayingVideo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingVideo) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Video timeline bar
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("0:18", color = Color.White, fontSize = 10.sp)
                                LinearProgressIndicator(
                                    progress = { 0.45f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .height(4.dp),
                                    color = VioraCoral,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                )
                                Text("0:42", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }

                    // Caption & Likes
                    Text(
                        text = media.caption,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                if (!hasLiked) {
                                    likesCount++
                                    hasLiked = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (hasLiked) VioraCoral else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("$likesCount likes", color = Color.White, fontSize = 12.sp)
                        }

                        if (media.userId == currentUser.id && media.isLocked) {
                            Text(
                                text = "Earned: ${(media.coinPrice * 0.70).toLong() * media.unlockedByUserIds.size} 💎",
                                color = VioraWarmGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedMediaForPreview = null },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple)
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun MediaItemCard(
    media: ProfileMedia,
    currentUserId: String,
    onUnlock: () -> Unit,
    onPreview: () -> Unit
) {
    val isOwner = media.userId == currentUserId
    val isUnlocked = media.unlockedByUserIds.contains(currentUserId)
    val shouldLock = media.isLocked && !isOwner && !isUnlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (shouldLock) {
                    onUnlock()
                } else {
                    onPreview()
                }
            }
            .testTag("media_card_${media.id}"),
        colors = CardDefaults.cardColors(containerColor = VioraSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Image
            AsyncImage(
                model = media.thumbnailUrl,
                contentDescription = media.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (media.mediaType == MediaType.VIDEO) "🎥 Video" else "📷 Photo",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Lock / Unlock Badge
                if (media.isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    shouldLock -> VioraWarmGold
                                    isUnlocked -> VioraSuccessGreen
                                    else -> VioraElectricPurple
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                shouldLock -> "🔒 ${media.coinPrice}🪙"
                                isUnlocked -> "🔓 Unlocked"
                                else -> "👑 Mine (${media.coinPrice}🪙)"
                            },
                            color = if (shouldLock) Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Locked Scrim Overlay if locked
            if (shouldLock) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = VioraWarmGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Tap to Unlock",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${media.coinPrice} Coins",
                            color = VioraWarmGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (media.mediaType == MediaType.VIDEO) {
                // Video Play Icon overlay when playable
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Bottom Caption snippet
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = media.caption,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = VioraCoral, modifier = Modifier.size(10.dp))
                        Text(text = "${media.likesCount}", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = VioraMutedText, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = VioraCardDark),
        shape = RoundedCornerShape(14.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtitle, color = VioraMutedText, fontSize = 11.sp)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VioraMutedText, modifier = Modifier.size(20.dp))
        }
    }
}
