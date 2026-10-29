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
import com.example.ui.screens.admin.AdminConsoleScreen
import com.example.ui.screens.admin.AdminDashboardScreen
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
    ADMIN_CONSOLE,
    ADMIN_DASHBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VioraRepository.init(this)
        enableEdgeToEdge()
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
    val isRestoring by VioraRepository.isRestoring.collectAsState()
    val currentUser by VioraRepository.currentUser.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var activeLiveStream by remember { mutableStateOf<LiveStream?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var showCreatorApply by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun tryGoLive() {
        if (currentUser.role == com.example.model.UserRole.VIEWER) {
            showCreatorApply = true
        } else {
            currentScreen = AppScreen.BROADCAST_SETUP
        }
    }

    if (isRestoring) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VioraAppBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VioraPrimaryGold)
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
                    onNavigateToAdmin = { currentScreen = AppScreen.ADMIN_DASHBOARD },
                    onNavigateToModeratorHub = { currentScreen = AppScreen.ADMIN_DASHBOARD },
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
                            activeLiveStream = null
                            currentScreen = AppScreen.HOME
                        },
                        onNavigateToWallet = { currentScreen = AppScreen.WALLET },
                        modifier = Modifier.fillMaxSize()
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

        // Bottom Navigation fixed below the content
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
                    Text("Viora approves creators before they can broadcast. Tell us what you will stream.", color = VioraSecondaryText, fontSize = 12.sp)
                    OutlinedTextField(
                        value = pitch,
                        onValueChange = { pitch = it },
                        label = { Text("What will you stream?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraNeonPurple,
                            unfocusedBorderColor = VioraGlassBorder,
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
                    colors = ButtonDefaults.buttonColors(containerColor = VioraNeonPurple)
                ) { Text("Send application", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showCreatorApply = false }) { Text("Cancel", color = VioraSecondaryText) } },
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
                color = VioraGlassBorder,
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

            // Center Floating Action Button (FAB) slightly raised with strong purple neon glow and broadcast icon
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(VioraNeonPurple, VioraNeonPurpleLight, VioraPrimaryGold, VioraNeonPurple)
                        ),
                        shape = CircleShape
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VioraNeonPurple, Color(0xFF3B1D5A))
                        )
                    )
                    .clickable { onGoLive() }
                    .testTag("nav_go_live"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = "Go Live",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notifications Tab (Alerts)
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
                tint = if (isSelected) VioraPrimaryGold else VioraSecondaryText,
                modifier = Modifier.size(24.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(VioraLiveBadgeStart)
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
            color = if (isSelected) Color.White else VioraSecondaryText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
