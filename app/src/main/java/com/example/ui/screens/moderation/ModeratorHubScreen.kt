package com.example.ui.screens.moderation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.VioraRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ModeratorHubScreen(
    onBack: () -> Unit,
    onViewStream: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val currentUser by VioraRepository.currentUser.collectAsState()
    val tasks by VioraRepository.tasks.collectAsState()
    val reports by VioraRepository.reports.collectAsState()
    val held by VioraRepository.heldMessages.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var warnTarget by remember { mutableStateOf<ModerationReport?>(null) }
    var escalateTarget by remember { mutableStateOf<ModerationReport?>(null) }

    val myTasks = tasks.filter { it.assignedToId == currentUser.id || currentUser.role == UserRole.ADMIN }
    val media by VioraRepository.profileMedia.collectAsState()
    val isAdmin = currentUser.role == UserRole.ADMIN
    var queueFilter by remember { mutableIntStateOf(0) } // 0 = mine, 1 = unassigned, 2 = all (admin)
    var previewMediaId by remember { mutableStateOf<String?>(null) }

    // Suspected-minor reports go straight to admins; moderators never see them.
    val allPending = reports.filter {
        it.status == ReportStatus.PENDING_REVIEW && (isAdmin || it.category != ReportCategory.UNDERAGE)
    }
    val mineCount = allPending.count { it.assignedToId == currentUser.id }
    val unassignedCount = allPending.count { it.assignedToId.isBlank() }
    val pending = when (queueFilter) {
        0 -> allPending.filter { it.assignedToId == currentUser.id }
        1 -> allPending.filter { it.assignedToId.isBlank() }
        else -> allPending
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraAppBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OpsHeader("Moderator Hub", "MOD", VioraCoral, onBack)
        OpsTabs(
            listOf("Tasks (${myTasks.count { it.status != TaskStatus.DONE }})", "Reports (${if (isAdmin) allPending.size else mineCount + unassignedCount})", "Held Chat (${held.size})"),
            tab
        ) { tab = it }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            when (tab) {
                0 -> {
                    if (myTasks.isEmpty()) item { OpsEmpty("No tasks assigned to you") }
                    items(myTasks, key = { it.id }) { task -> TaskCard(task) }
                }
                1 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OpsSelectChip("Assigned to me ($mineCount)", queueFilter == 0) { queueFilter = 0 }
                            OpsSelectChip("Unassigned ($unassignedCount)", queueFilter == 1) { queueFilter = 1 }
                            if (isAdmin) OpsSelectChip("All (${allPending.size})", queueFilter == 2) { queueFilter = 2 }
                        }
                    }
                    if (pending.isEmpty()) item { OpsEmpty(if (queueFilter == 0) "Nothing assigned to you" else "Queue is clear. Nice work!") }
                    items(pending, key = { it.id }) { rep ->
                        OpsCard {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OpsChip(rep.targetType, VioraElectricPurple)
                                OpsChip(rep.category.displayName, VioraWarmGold)
                                if (rep.escalated) OpsChip("ESCALATED", VioraErrorRed)
                            }
                            Text(rep.targetName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(rep.notes, color = VioraMutedText, fontSize = 12.sp)
                            Text(
                                "Reported by ${rep.reportedBy} • ${opsTimeAgo(rep.timestampMs)}" +
                                    if (rep.assignedToName.isNotBlank()) " • Assigned to ${rep.assignedToName}" else " • Unassigned",
                                color = VioraSubtleText, fontSize = 10.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (rep.assignedToId.isBlank()) {
                                    OpsButton("Claim", color = VioraSuccessGreen) { VioraRepository.claimReport(rep.id) }
                                }
                                if (rep.targetType == "STREAM") {
                                    OpsButton("Watch silently", color = VioraElectricPurple) { onViewStream(rep.targetId) }
                                }
                                if (rep.targetType == "MEDIA") {
                                    OpsButton("Preview", color = VioraElectricPurple) { previewMediaId = rep.targetId }
                                    OpsButton("Hide content", color = VioraCoral) { VioraRepository.hideReportedMedia(rep.targetId, rep.id) }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OpsButton("Warn", color = VioraWarmGold) { warnTarget = rep }
                                OpsButton("Dismiss", color = VioraSurfaceVariant) { VioraRepository.resolveReport(rep.id, "DISMISS") }
                                if (!rep.escalated) {
                                    OpsButton("Escalate", color = VioraErrorRed) { escalateTarget = rep }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (held.isEmpty()) item { OpsEmpty("No chat messages held for review") }
                    items(held, key = { it.id }) { h ->
                        OpsCard {
                            OpsChip("Matched: ${h.matchedKeyword}", VioraErrorRed)
                            Text(h.senderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("\"${h.content}\"", color = VioraMutedText, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OpsButton("Release", color = VioraSuccessGreen) { VioraRepository.decideHeldMessage(h.id, true) }
                                OpsButton("Reject + strike", color = VioraErrorRed) { VioraRepository.decideHeldMessage(h.id, false) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Preview of reported photo / video
    previewMediaId?.let { id ->
        val item = media.find { it.id == id }
        AlertDialog(
            onDismissRequest = { previewMediaId = null },
            title = { Text(if (item != null) "Reported ${item.mediaType.name.lowercase()}" else "Content unavailable", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (item == null) {
                    Text("This content was already hidden or removed.", color = VioraMutedText, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = item.thumbnailUrl.ifBlank { item.mediaUrl },
                            contentDescription = "Reported content",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Text("By ${item.authorName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(item.caption, color = VioraMutedText, fontSize = 12.sp)
                        if (item.mediaType == MediaType.VIDEO) {
                            Text("Video: showing its thumbnail. Full playback needs the real backend.", color = VioraSubtleText, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (item != null) {
                    OpsButton("Hide content", color = VioraCoral) {
                        VioraRepository.hideReportedMedia(item.id, reports.firstOrNull { it.targetId == item.id }?.id)
                        previewMediaId = null
                    }
                }
            },
            dismissButton = { TextButton(onClick = { previewMediaId = null }) { Text("Close", color = VioraMutedText) } },
            containerColor = VioraCardDark
        )
    }

    // Warn dialog with canned responses
    warnTarget?.let { rep ->
        AlertDialog(
            onDismissRequest = { warnTarget = null },
            title = { Text("Warn ${rep.targetName}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pick a response. This adds a strike to the account.", color = VioraMutedText, fontSize = 12.sp)
                    CANNED_RESPONSES.forEach { text ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VioraSurfaceVariant)
                                .clickable {
                                    VioraRepository.warnUser(rep.targetName, text, rep.id)
                                    warnTarget = null
                                }
                                .padding(10.dp)
                        ) {
                            Text(text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { warnTarget = null }) { Text("Cancel", color = VioraMutedText) } },
            containerColor = VioraCardDark
        )
    }

    // Escalate dialog
    escalateTarget?.let { rep ->
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { escalateTarget = null },
            title = { Text("Escalate to admin", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { OpsField(note, { note = it }, "Why does an admin need to see this?", singleLine = false) },
            confirmButton = {
                OpsButton("Send", color = VioraErrorRed, enabled = note.isNotBlank()) {
                    VioraRepository.escalateReport(rep.id, note.trim())
                    escalateTarget = null
                }
            },
            dismissButton = { TextButton(onClick = { escalateTarget = null }) { Text("Cancel", color = VioraMutedText) } },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun TaskCard(task: ModerationTask) {
    var note by remember(task.id) { mutableStateOf(task.note) }
    OpsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OpsChip(
                task.priority.name,
                when (task.priority) {
                    TaskPriority.URGENT -> VioraErrorRed
                    TaskPriority.NORMAL -> VioraWarmGold
                    TaskPriority.LOW -> VioraMutedText
                }
            )
            OpsChip(
                task.status.name.replace('_', ' '),
                if (task.status == TaskStatus.DONE) VioraSuccessGreen else VioraElectricPurple
            )
        }
        Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (task.details.isNotBlank()) Text(task.details, color = VioraMutedText, fontSize = 12.sp)
        Text("From ${task.assignedByName} • To ${task.assignedToName} • ${opsTimeAgo(task.createdMs)}", color = VioraSubtleText, fontSize = 10.sp)
        if (task.status != TaskStatus.DONE) {
            OpsField(note, { note = it }, "Notes for admin", singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.status == TaskStatus.OPEN) {
                    OpsButton("Start", color = VioraElectricPurple) { VioraRepository.updateTask(task.id, TaskStatus.IN_PROGRESS, note) }
                }
                OpsButton("Mark done", color = VioraSuccessGreen) { VioraRepository.updateTask(task.id, TaskStatus.DONE, note) }
            }
        } else if (task.note.isNotBlank()) {
            Text("Note: ${task.note}", color = VioraMutedText, fontSize = 12.sp)
        }
    }
}
