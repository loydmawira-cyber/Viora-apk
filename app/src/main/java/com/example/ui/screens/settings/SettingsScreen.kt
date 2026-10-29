package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var notifGoLive by remember { mutableStateOf(true) }
    var notifGifts by remember { mutableStateOf(true) }
    var incognitoMode by remember { mutableStateOf(false) }
    var allowDirectMessages by remember { mutableStateOf(true) }
    var showGuidelinesDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val me by com.example.data.VioraRepository.currentUser.collectAsState()
    val deleteScope = rememberCoroutineScope()
    val deleteContext = androidx.compose.ui.platform.LocalContext.current

    val scrollState = rememberScrollState()

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
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
                    .testTag("settings_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Settings & Privacy",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        // Push Notifications section
        SettingsSectionHeader("Push Notifications")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggleRow("Followed Creator Goes Live", "Alerts when creators you follow begin streaming", notifGoLive) { notifGoLive = it }
                HorizontalDivider(color = VioraBorder)
                SettingsToggleRow("Virtual Gift Alerts", "Alerts when you receive diamonds or gifts", notifGifts) { notifGifts = it }
            }
        }

        // Privacy & Safety section
        SettingsSectionHeader("Privacy & Stream Safety")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggleRow("Incognito Stream Viewer", "Hide your username from viewer presence lists", incognitoMode) { incognitoMode = it }
                HorizontalDivider(color = VioraBorder)
                SettingsToggleRow("Direct Messages", "Allow viewers to send you private inbox messages", allowDirectMessages) { allowDirectMessages = it }
            }
        }

        // Legal & Community Guidelines
        SettingsSectionHeader("Legal & Safety Compliance")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGuidelinesDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Community Guidelines", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Anti-harassment, safety policies & rules", color = VioraMutedText, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VioraMutedText)
                }

                HorizontalDivider(color = VioraBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Terms of Service & Privacy Policy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Data protection and user rights", color = VioraMutedText, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VioraMutedText)
                }
            }
        }

        // Account
        SettingsSectionHeader("Account")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Delete my account", color = VioraErrorRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Permanently remove your account and data", color = VioraMutedText, fontSize = 11.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VioraMutedText)
            }
        }

        // Debug-only preview (not present in release builds)
        if (com.example.BuildConfig.DEBUG) {
            SettingsSectionHeader("Debug preview (debug builds only)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preview screens for a role. Real roles come from the database.", color = VioraMutedText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.model.UserRole.values().forEach { r ->
                            com.example.ui.components.OpsSelectChip(r.name.take(4), me.role == r) {
                                com.example.data.VioraRepository.debugPreviewRole(r)
                            }
                        }
                    }
                    com.example.ui.components.OpsButton("Preview banned screen", color = VioraErrorRed) {
                        com.example.data.VioraRepository.debugPreviewBanned(true)
                    }
                }
            }
        }

        // Version Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Viora for Android • Version 1.0.0 (Build 2026.10)", color = VioraSubtleText, fontSize = 11.sp)
            Text("Powered by Agora RTC & Supabase Cloud", color = VioraSubtleText, fontSize = 10.sp)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete your account?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This permanently removes your profile, wallet and content. Unspent coins cannot be refunded.", color = VioraMutedText, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        deleteScope.launch {
                            com.example.data.VioraRepository.deleteAccount().onFailure {
                                android.widget.Toast.makeText(
                                    deleteContext,
                                    it.message ?: "Could not delete your account. Try again.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraErrorRed)
                ) { Text("Delete forever", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = VioraMutedText) } },
            containerColor = VioraCardDark
        )
    }

    if (showGuidelinesDialog) {
        AlertDialog(
            onDismissRequest = { showGuidelinesDialog = false },
            title = { Text("Viora Community Guidelines", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Respect Creators & Viewers: No bullying, hate speech, or harassment.", color = Color.White, fontSize = 12.sp)
                    Text("2. Safe Broadcasts: Zero tolerance for illicit goods, violence, or dangerous stunts.", color = Color.White, fontSize = 12.sp)
                    Text("3. Age Requirement: Broadcasters must be at least 18 years of age.", color = Color.White, fontSize = 12.sp)
                    Text("4. Fair Virtual Gifting: No fraudulent refund abuse or coin exploitation.", color = Color.White, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuidelinesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple)
                ) {
                    Text("I Agree", color = Color.White)
                }
            },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = VioraMutedText, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VioraCoral)
        )
    }
}
