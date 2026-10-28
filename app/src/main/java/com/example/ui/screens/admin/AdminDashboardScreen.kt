package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.ReportStatus
import com.example.model.StreamStatus
import com.example.ui.components.LiveBadge
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val reports by VioraRepository.reports.collectAsState()
    val liveStreams by VioraRepository.liveStreams.collectAsState()
    val withdrawals by VioraRepository.withdrawals.collectAsState()

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: Reports Queue, 1: Active Streams, 2: Metrics

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Top App Bar
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
                    .testTag("admin_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = VioraWarmGold, modifier = Modifier.size(20.dp))
                Text(
                    text = "Viora Admin & Safety Center",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VioraWarmGold)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("ADMIN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High Level Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard("Live Streams", "${liveStreams.count { it.status == StreamStatus.LIVE }}", VioraElectricPurple, Modifier.weight(1f))
            MetricCard("Reports", "${reports.count { it.status == ReportStatus.PENDING_REVIEW }}", VioraErrorRed, Modifier.weight(1f))
            MetricCard("Withdrawals", "${withdrawals.size}", VioraWarmGold, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VioraCardDark)
                .padding(4.dp)
        ) {
            listOf("Safety Queue", "Active Streams", "Platform Stats").forEachIndexed { index, title ->
                val isSelected = selectedAdminTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VioraElectricPurple else Color.Transparent)
                        .clickable { selectedAdminTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else VioraMutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedAdminTab) {
            0 -> SafetyReportsQueue(reports = reports)
            1 -> ActiveStreamsManager(streams = liveStreams.filter { it.status == StreamStatus.LIVE })
            2 -> PlatformStatsView(streamsCount = liveStreams.size, reportsCount = reports.size)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VioraCardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = VioraMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SafetyReportsQueue(reports: List<com.example.model.ModerationReport>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (reports.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Safety queue clear! No pending reports.", color = VioraSuccessGreen, fontSize = 13.sp)
                }
            }
        } else {
            items(reports) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VioraCoral)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(rep.category.displayName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Target: ${rep.targetName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Text(
                                text = rep.status.name,
                                color = if (rep.status == ReportStatus.PENDING_REVIEW) VioraWarmGold else VioraSuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (rep.notes.isNotBlank()) {
                            Text("Note: \"${rep.notes}\"", color = VioraMutedText, fontSize = 12.sp)
                        }

                        if (rep.status == ReportStatus.PENDING_REVIEW) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { VioraRepository.resolveReport(rep.id, "DISMISS") },
                                    colors = ButtonDefaults.buttonColors(containerColor = VioraSurfaceVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss", color = Color.White, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        VioraRepository.resolveReport(rep.id, "ACTION_TAKEN")
                                        if (rep.targetType == "STREAM") {
                                            VioraRepository.adminTerminateStream(rep.targetId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VioraErrorRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Enforce & Ban", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveStreamsManager(streams: List<com.example.model.LiveStream>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (streams.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No broadcasts currently active.", color = VioraMutedText, fontSize = 13.sp)
                }
            }
        } else {
            items(streams) { stream ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LiveBadge()
                                Text(stream.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Host: ${stream.hostName} • ${stream.viewerCount} Viewers", color = VioraMutedText, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { VioraRepository.adminTerminateStream(stream.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = VioraErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Kill Room", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformStatsView(streamsCount: Int, reportsCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Platform Health & Architecture", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Video Engine: Agora RTC v4.x Low Latency Network (Token-Gated)", color = Color.White, fontSize = 12.sp)
                Text("• Backend: Supabase PostgreSQL with Row Level Security", color = Color.White, fontSize = 12.sp)
                Text("• Realtime: Chat, Viewers count, Reaction hearts & Gifts", color = Color.White, fontSize = 12.sp)
                Text("• Creator Split: 70% Creator / 30% Platform Pool", color = Color.White, fontSize = 12.sp)
                Text("• Safety: Zero-trust moderator actions with audit trail", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
