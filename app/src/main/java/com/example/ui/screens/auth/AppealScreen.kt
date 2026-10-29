package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.AppealStatus
import com.example.ui.components.OpsButton
import com.example.ui.components.OpsField
import com.example.ui.theme.*

@Composable
fun AppealScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val me by VioraRepository.currentUser.collectAsState()
    val appeals by VioraRepository.appeals.collectAsState()
    val mine = appeals.filter { it.userId == me.id }
    val hasPending = mine.any { it.status == AppealStatus.PENDING }
    val wasRejected = mine.any { it.status == AppealStatus.REJECTED } && !hasPending
    var reason by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraAppBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Text("🚫", fontSize = 48.sp)
        Text("Your account is banned", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            "Your account broke the Viora Community Guidelines. If you think this was a mistake you can send one appeal for an admin to review.",
            color = VioraMutedText, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        when {
            hasPending -> Text("Your appeal was sent. We will review it soon.", color = VioraWarmGold, fontSize = 13.sp, textAlign = TextAlign.Center)
            wasRejected -> Text("Your appeal was reviewed and the ban stays in place.", color = VioraErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            else -> {
                OpsField(reason, { reason = it }, "Why should we lift the ban?", singleLine = false)
                OpsButton("Send appeal", enabled = reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    VioraRepository.submitAppeal(reason)
                }
            }
        }
        TextButton(onClick = { VioraRepository.refreshAccount() }) { Text("Check my appeal status", color = VioraWarmGold) }
        TextButton(onClick = onSignOut) { Text("Sign out", color = VioraMutedText) }
    }
}
