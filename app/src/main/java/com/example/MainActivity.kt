package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.Conversation
import com.example.model.LiveStream
import com.example.model.UserRole
import com.example.ui.screens.admin.AdminConsoleScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AppealScreen
import com.example.ui.screens.moderation.ModeratorHubScreen
import kotlinx.coroutines.delay
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.inbox.DirectChatScreen
import com.example.ui.screens.inbox.InboxScreen
import com.example.ui.screens.live.BroadcastSetupScreen
import com.example.ui.screens.live.LiveRoomScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.*

enum class AppScreen {
    HOME,
    INBOX,
    DIRECT_CHAT,
    NOTIFICATIONS,
    PROFILE,
    LIVE_ROOM,
    BROADCAST_SETUP,
    WALLET,
    ADMIN_DASHBOARD,
    SETTINGS,
    MODERATOR_HUB,
    ADMIN_CONSOLE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VioraRepository.init(applicationContext)
        setContent {
            VioraTheme(darkTheme = true) {
                VioraAppRoot()
            }
        }
    }
}

@Composable
fun VioraAppRoot() {
    val isAuthenticated by VioraRepository.isAuthenticated.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var activeLiveStream by remember { mutableStateOf<LiveStream?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    val currentUser by VioraRepository.currentUser.collectAsState()
    val platformSettings by VioraRepository.settings.collectAsState()
    val allStreams by VioraRepository.liveStreams.collectAsState()
    var notice by remember { mutableStateOf<String?>(null) }
    var showCreatorApply by remember { mutableStateOf(false) }
    var silentStreamId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2800)
            notice = null
        }
    }

    val restoring by VioraRepository.isRestoring.collectAsState()
    if (restoring) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VioraAppBg),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = VioraPrimaryChampagneGold)
        }
        return
    }

    if (!isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { currentScreen = AppScreen.HOME },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (currentUser.isBanned) {
        AppealScreen(
            onSignOut = { VioraRepository.logout() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Go Live is allowed for approved creators and staff only, and never during a kill switch
    val tryGoLive: () -> Unit = {
        if (platformSettings.streamsPaused) {
            notice = "Live streaming is temporarily paused by Viora"
        } else if (currentUser.role == UserRole.VIEWER) {
            showCreatorApply = true
        } else {
            currentScreen = AppScreen.BROADCAST_SETUP
        }
    }

    val showBottomNav = currentScreen in listOf(
        AppScreen.HOME,
        AppScreen.INBOX,
        AppScreen.NOTIFICATIONS,
        AppScreen.PROFILE
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VioraAppBg)
    ) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        // Main Screen View
        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    onSelectStream = { stream ->
                        activeLiveStream = stream
                        currentScreen = AppScreen.LIVE_ROOM
                    },
                    onNavigateToWallet = { currentScreen = AppScreen.WALLET },
                    onNavigateToProfile = { currentScreen = AppScreen.PROFILE },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.INBOX -> {
                InboxScreen(
                    onOpenConversation = { conv ->
                        activeConversation = conv
                        currentScreen = AppScreen.DIRECT_CHAT
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.DIRECT_CHAT -> {
                activeConversation?.let { conv ->
                    DirectChatScreen(
                        conversation = conv,
                        onBack = { currentScreen = AppScreen.INBOX },
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    currentScreen = AppScreen.INBOX
                }
            }
            AppScreen.NOTIFICATIONS -> {
                NotificationsScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.PROFILE -> {
                ProfileScreen(
                    onNavigateToWallet = { currentScreen = AppScreen.WALLET },
                    onNavigateToAdmin = { currentScreen = AppScreen.ADMIN_CONSOLE },
                    onNavigateToModeratorHub = { currentScreen = AppScreen.MODERATOR_HUB },
                    onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                    onNavigateToBroadcastSetup = { tryGoLive() },
                    onLogout = { VioraRepository.logout() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.LIVE_ROOM -> {
                activeLiveStream?.let { stream ->
                    LiveRoomScreen(
                        stream = stream,
                        onLeave = {
                            silentStreamId = null
                            activeLiveStream = null
                            currentScreen = AppScreen.HOME
                        },
                        onNavigateToWallet = { currentScreen = AppScreen.WALLET },
                        modifier = Modifier.fillMaxSize(),
                        isSilentModerator = silentStreamId == stream.id &&
                            (currentUser.role == UserRole.MODERATOR || currentUser.role == UserRole.ADMIN)
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }
            AppScreen.BROADCAST_SETUP -> {
                BroadcastSetupScreen(
                    onBroadcastStarted = { newStream ->
                        activeLiveStream = newStream
                        currentScreen = AppScreen.LIVE_ROOM
                    },
                    onCancel = { currentScreen = AppScreen.HOME },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.WALLET -> {
                WalletScreen(
                    onBack = { currentScreen = AppScreen.HOME },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.MODERATOR_HUB -> {
                ModeratorHubScreen(
                    onBack = { currentScreen = AppScreen.PROFILE },
                    onViewStream = { streamId ->
                        val target = allStreams.find { it.id == streamId }
                        if (target != null && target.status == com.example.model.StreamStatus.LIVE) {
                            silentStreamId = target.id
                            activeLiveStream = target
                            currentScreen = AppScreen.LIVE_ROOM
                        } else {
                            notice = "That stream is no longer live"
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.ADMIN_CONSOLE -> {
                AdminConsoleScreen(
                    onBack = { currentScreen = AppScreen.PROFILE },
                    onOpenSafetyDashboard = { currentScreen = AppScreen.ADMIN_DASHBOARD },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.ADMIN_DASHBOARD -> {
                AdminDashboardScreen(
                    onBack = { currentScreen = AppScreen.ADMIN_CONSOLE },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    onBack = { currentScreen = AppScreen.PROFILE },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        notice?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = VioraCardElevated,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp
            ) {
                Text(msg, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
      }

        // Bottom Navigation fixed below the content (primary tabs only)
        if (showBottomNav) {
            VioraBottomNavigationBar(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen },
                onGoLive = { tryGoLive() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showCreatorApply) {
        var pitch by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatorApply = false },
            title = { Text("Apply to go live", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Viora approves creators before they can broadcast. Tell us what you will stream.", color = VioraMutedText, fontSize = 12.sp)
                    OutlinedTextField(
                        value = pitch,
                        onValueChange = { pitch = it },
                        label = { Text("What will you stream?") },
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
                    enabled = pitch.isNotBlank(),
                    onClick = {
                        VioraRepository.applyForCreator(pitch)
                        showCreatorApply = false
                        notice = "Application sent. We will review it soon."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple)
                ) { Text("Send application", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showCreatorApply = false }) { Text("Cancel", color = VioraMutedText) } },
            containerColor = VioraCardDark
        )
    }
}

@Composable
fun VioraBottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by VioraRepository.conversations.collectAsState()
    val unreadMessageCount = conversations.sumOf { it.unreadCount }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(VioraGoldBorderSubtle, Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            ),
        color = VioraSecondaryBg,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            BottomNavItem(
                icon = if (currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                isSelected = currentScreen == AppScreen.HOME,
                onClick = { onNavigate(AppScreen.HOME) },
                testTag = "nav_home"
            )

            // Inbox Tab with Unread Badge
            BottomNavItem(
                icon = if (currentScreen == AppScreen.INBOX) Icons.Filled.Forum else Icons.Outlined.Forum,
                label = "Inbox",
                isSelected = currentScreen == AppScreen.INBOX,
                badgeCount = unreadMessageCount,
                onClick = { onNavigate(AppScreen.INBOX) },
                testTag = "nav_inbox"
            )

            // Prominent "Go Live" action in center with Champagne Gold & Violet holographic glow ring
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(VioraPrimaryChampagneGold, VioraSubtleViolet, VioraGoldHighlight, VioraPrimaryChampagneGold)
                        ),
                        shape = CircleShape
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VioraCardDark, VioraDeepViolet)
                        )
                    )
                    .clickable { onGoLive() }
                    .testTag("nav_go_live"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = "Go Live",
                    tint = VioraPrimaryChampagneGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notifications Tab
            BottomNavItem(
                icon = if (currentScreen == AppScreen.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                label = "Alerts",
                isSelected = currentScreen == AppScreen.NOTIFICATIONS,
                onClick = { onNavigate(AppScreen.NOTIFICATIONS) },
                testTag = "nav_notifications"
            )

            // Profile Tab
            BottomNavItem(
                icon = if (currentScreen == AppScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                label = "Profile",
                isSelected = currentScreen == AppScreen.PROFILE,
                onClick = { onNavigate(AppScreen.PROFILE) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) VioraPrimaryChampagneGold else VioraMutedText,
                modifier = Modifier.size(24.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(VioraLiveCoralPink)
                        .border(1.5.dp, VioraCardDark, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = label,
            color = if (isSelected) VioraPrimaryText else VioraMutedText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
