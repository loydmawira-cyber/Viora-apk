package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class Section(val emoji: String, val title: String, val subtitle: String) {
    TASKS("📋", "Assign Tasks", "Give reports to moderators"),
    SAFETY("🛡️", "Escalations & Safety", "Escalated reports, kill rooms, stats"),
    USERS("👥", "Users & Roles", "Search, strikes, roles, bans"),
    PURCHASES("🪙", "Coin Purchases", "Refund or flag purchases"),
    CATALOG("🏷️", "Coin & Gift Catalog", "Edit packages, prices and gifts"),
    WITHDRAWALS("💸", "Withdrawals", "Approve or reject payouts"),
    REVENUE("📊", "Revenue Report", "Coins sold and payouts owed"),
    FRAUD("🚨", "Fraud Alerts", "Suspicious buying and gifting"),
    APPEALS("⚖️", "Ban Appeals", "Review user appeals"),
    MODS("🧑‍⚖️", "Moderator Shifts", "Who is online and performance"),
    CREATORS("🎤", "Creator Applications", "Approve who can go live"),
    FEATURED("⭐", "Featured Streams", "Pin streams on Home"),
    ANNOUNCE("📣", "Announcements", "Message all users"),
    KEYWORDS("🔤", "Chat Keyword Filter", "Words held for review"),
    KILL("🛑", "Emergency Kill Switch", "Pause streams or gifting"),
    AUDIT("🧾", "Audit Log", "Every staff action")
}

@Composable
fun AdminConsoleScreen(
    onBack: () -> Unit,
    onOpenSafetyDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf<Section?>(null) }
    BackHandler { if (section != null) section = null else onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraAppBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OpsHeader(
            title = section?.title ?: "Admin Console",
            badge = "ADMIN",
            badgeColor = VioraWarmGold,
            onBack = { if (section != null) section = null else onBack() }
        )

        val current = section
        if (current == null) {
            val reports by VioraRepository.reports.collectAsState()
            val appeals by VioraRepository.appeals.collectAsState()
            val apps by VioraRepository.applications.collectAsState()
            val fraud by VioraRepository.fraudAlerts.collectAsState()
            val tasks by VioraRepository.tasks.collectAsState()
            val counts = mapOf(
                Section.SAFETY to reports.count { it.escalated && it.status == ReportStatus.PENDING_REVIEW },
                Section.APPEALS to appeals.count { it.status == AppealStatus.PENDING },
                Section.CREATORS to apps.count { it.status == ApplicationStatus.PENDING },
                Section.FRAUD to fraud.count { !it.resolved },
                Section.TASKS to tasks.count { it.status != TaskStatus.DONE }
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(Section.values().toList()) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(VioraCardDark)
                            .clickable { section = s }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(s.emoji, fontSize = 22.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(s.subtitle, color = VioraMutedText, fontSize = 11.sp)
                        }
                        val n = counts[s] ?: 0
                        if (n > 0) OpsChip("$n", VioraCoral)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                when (current) {
                    Section.TASKS -> tasksSection()
                    Section.SAFETY -> safetySection(onOpenSafetyDashboard)
                    Section.USERS -> usersSection()
                    Section.PURCHASES -> purchasesSection()
                    Section.CATALOG -> catalogSection()
                    Section.WITHDRAWALS -> withdrawalsSection()
                    Section.REVENUE -> revenueSection()
                    Section.FRAUD -> fraudSection()
                    Section.APPEALS -> appealsSection()
                    Section.MODS -> modsSection()
                    Section.CREATORS -> creatorsSection()
                    Section.FEATURED -> featuredSection()
                    Section.ANNOUNCE -> announceSection()
                    Section.KEYWORDS -> keywordsSection()
                    Section.KILL -> killSection()
                    Section.AUDIT -> auditSection()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- TASKS
private fun androidx.compose.foundation.lazy.LazyListScope.tasksSection() {
    item {
        val mods by VioraRepository.moderators.collectAsState()
        val reports by VioraRepository.reports.collectAsState()
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var modId by remember { mutableStateOf("") }
        var reportId by remember { mutableStateOf<String?>(null) }
        var priority by remember { mutableStateOf(TaskPriority.NORMAL) }
        val pending = reports.filter { it.status == ReportStatus.PENDING_REVIEW }

        OpsCard {
            Text("New task", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OpsField(title, { title = it }, "Task title")
            OpsField(details, { details = it }, "Details / deadline", singleLine = false)
            Text("Assign to", color = VioraMutedText, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mods.forEach { m -> OpsSelectChip(m.name, modId == m.id) { modId = m.id } }
            }
            Text("Priority", color = VioraMutedText, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskPriority.values().forEach { p -> OpsSelectChip(p.name, priority == p) { priority = p } }
            }
            if (pending.isNotEmpty()) {
                Text("Attach a report (optional)", color = VioraMutedText, fontSize = 11.sp)
                pending.forEach { r ->
                    OpsSelectChip("${r.targetName} • ${r.category.displayName}", reportId == r.id) {
                        reportId = if (reportId == r.id) null else r.id
                    }
                }
            }
            OpsButton("Assign task", enabled = title.isNotBlank() && modId.isNotBlank()) {
                VioraRepository.assignTask(title, details, reportId, modId, priority)
                title = ""; details = ""; reportId = null
            }
        }
    }
    item {
        val tasks by VioraRepository.tasks.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("All tasks", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (tasks.isEmpty()) OpsEmpty("No tasks yet")
            tasks.forEach { t ->
                OpsCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpsChip(t.status.name.replace('_', ' '), if (t.status == TaskStatus.DONE) VioraSuccessGreen else VioraElectricPurple)
                        OpsChip(t.priority.name, if (t.priority == TaskPriority.URGENT) VioraErrorRed else VioraWarmGold)
                    }
                    Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Assigned to ${t.assignedToName} by ${t.assignedByName}", color = VioraMutedText, fontSize = 11.sp)
                    if (t.note.isNotBlank()) Text("Moderator note: ${t.note}", color = VioraMutedText, fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- SAFETY
private fun androidx.compose.foundation.lazy.LazyListScope.safetySection(onOpenDashboard: () -> Unit) {
    item {
        OpsButton("Open full Safety Queue and Stats", color = VioraElectricPurple, modifier = Modifier.fillMaxWidth()) { onOpenDashboard() }
    }
    item {
        val reports by VioraRepository.reports.collectAsState()
        val escalated = reports.filter { it.escalated && it.status == ReportStatus.PENDING_REVIEW }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Escalated by moderators", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (escalated.isEmpty()) OpsEmpty("No escalations")
            escalated.forEach { r ->
                OpsCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpsChip(r.targetType, VioraElectricPurple)
                        OpsChip(r.category.displayName, VioraWarmGold)
                    }
                    Text(r.targetName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Moderator note: ${r.escalationNote}", color = VioraMutedText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (r.targetType == "STREAM") {
                            OpsButton("Kill room", color = VioraErrorRed) { VioraRepository.adminTerminateStream(r.targetId) }
                        }
                        OpsButton("Action taken", color = VioraSuccessGreen) { VioraRepository.resolveReport(r.id, "ACTION_TAKEN") }
                        OpsButton("Dismiss", color = VioraSurfaceVariant) { VioraRepository.resolveReport(r.id, "DISMISS") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- USERS
private fun androidx.compose.foundation.lazy.LazyListScope.usersSection() {
    item {
        val users by VioraRepository.platformUsers.collectAsState()
        var query by remember { mutableStateOf("") }
        val list = users.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OpsField(query, { query = it }, "Search users")
            Text("Strikes: 1 warning, 2 = 24h mute, 3 = suspension, 4 = ban", color = VioraMutedText, fontSize = 11.sp)
            if (list.isEmpty()) OpsEmpty("No users found")
            list.forEach { u ->
                OpsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(u.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OpsChip(
                            u.status.name,
                            when (u.status) {
                                AccountStatus.ACTIVE -> VioraSuccessGreen
                                AccountStatus.MUTED -> VioraWarmGold
                                AccountStatus.SUSPENDED -> VioraCoral
                                AccountStatus.BANNED -> VioraErrorRed
                            }
                        )
                    }
                    Text("Strikes ${u.strikes} • Reports ${u.reportCount} • Coins ${u.coinBalance}", color = VioraMutedText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UserRole.values().forEach { r ->
                            OpsSelectChip(r.name.take(4), u.role == r) { VioraRepository.setUserRole(u.id, r) }
                        }
                    }
                    if (u.status == AccountStatus.BANNED || u.strikes > 0) {
                        OpsButton("Unban / reset strikes", color = VioraSuccessGreen) { VioraRepository.unbanUser(u.id) }
                    }
                    if (u.status != AccountStatus.BANNED) {
                        OpsButton("Ban user", color = VioraErrorRed) { VioraRepository.banUser(u.id) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- PURCHASES
private fun androidx.compose.foundation.lazy.LazyListScope.purchasesSection() {
    item {
        val purchases by VioraRepository.purchases.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (purchases.isEmpty()) OpsEmpty("No purchases")
            purchases.forEach { p ->
                OpsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OpsChip(
                            p.status.name,
                            when (p.status) {
                                PurchaseStatus.COMPLETED -> VioraSuccessGreen
                                PurchaseStatus.REFUNDED -> VioraMutedText
                                PurchaseStatus.FLAGGED -> VioraErrorRed
                            }
                        )
                    }
                    Text("${p.coins} coins • $%.2f • ${opsTimeAgo(p.timestampMs)}".format(p.usd), color = VioraMutedText, fontSize = 12.sp)
                    if (p.status == PurchaseStatus.COMPLETED || p.status == PurchaseStatus.FLAGGED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OpsButton("Refund", color = VioraWarmGold) { VioraRepository.refundPurchase(p.id) }
                            if (p.status != PurchaseStatus.FLAGGED) {
                                OpsButton("Flag fraud", color = VioraErrorRed) { VioraRepository.flagPurchase(p.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- WITHDRAWALS
private fun androidx.compose.foundation.lazy.LazyListScope.withdrawalsSection() {
    item {
        val mine by VioraRepository.withdrawals.collectAsState()
        val others by VioraRepository.otherWithdrawals.collectAsState()
        val users by VioraRepository.platformUsers.collectAsState()
        val me by VioraRepository.currentUser.collectAsState()
        var message by remember { mutableStateOf<String?>(null) }
        val all = mine + others

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Rules: min $MIN_WITHDRAWAL_DIAMONDS diamonds, $NEW_CREATOR_HOLD_DAYS-day hold for new creators, " +
                    "2 admins for $LARGE_WITHDRAWAL_DIAMONDS+ diamonds.",
                color = VioraMutedText, fontSize = 11.sp
            )
            message?.let { Text(it, color = VioraErrorRed, fontSize = 12.sp) }
            if (all.isEmpty()) OpsEmpty("No withdrawal requests")
            all.forEach { w ->
                val name = if (w.userId == me.id) me.displayName else users.find { it.id == w.userId }?.name ?: w.userId
                val needed = if (w.diamondsAmount >= LARGE_WITHDRAWAL_DIAMONDS) 2 else 1
                val holding = System.currentTimeMillis() < w.holdUntilMs
                OpsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OpsChip(
                            w.status.name,
                            when (w.status) {
                                TransactionStatus.COMPLETED -> VioraSuccessGreen
                                TransactionStatus.REJECTED, TransactionStatus.FAILED -> VioraErrorRed
                                TransactionStatus.PENDING -> VioraWarmGold
                            }
                        )
                    }
                    Text("${w.diamondsAmount} diamonds = ${w.usdValue} via ${w.payoutMethod}", color = VioraMutedText, fontSize = 12.sp)
                    Text(w.payoutDestination, color = VioraSubtleText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (needed == 2) OpsChip("Needs 2 admins (${w.approvals.size}/2)", VioraCoral)
                        if (holding) OpsChip("New-creator hold", VioraWarmGold)
                    }
                    if (w.status == TransactionStatus.PENDING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OpsButton("Approve", color = VioraSuccessGreen) { message = VioraRepository.approveWithdrawal(w.id) }
                            OpsButton("Reject", color = VioraErrorRed) { VioraRepository.rejectWithdrawal(w.id) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- REVENUE
private fun androidx.compose.foundation.lazy.LazyListScope.revenueSection() {
    item {
        val purchases by VioraRepository.purchases.collectAsState()
        val mine by VioraRepository.withdrawals.collectAsState()
        val others by VioraRepository.otherWithdrawals.collectAsState()
        var days by remember { mutableIntStateOf(1) } // 1 = last 24h, 7 = last 7 days, 0 = all time
        val cutoff = if (days == 0) 0L else System.currentTimeMillis() - days * 24L * 3600000L

        val completed = purchases.filter { it.status == PurchaseStatus.COMPLETED && it.timestampMs >= cutoff }
        val coinsSold = completed.sumOf { it.coins }
        val gross = completed.sumOf { it.usd }
        val refunded = purchases.filter { it.status == PurchaseStatus.REFUNDED && it.timestampMs >= cutoff }.sumOf { it.usd }
        val all = mine + others
        val owed = all.filter { it.status == TransactionStatus.PENDING }.sumOf { it.diamondsAmount } / 100.0
        val paid = all.filter { it.status == TransactionStatus.COMPLETED && it.requestedAtEpochMs >= cutoff }.sumOf { it.diamondsAmount } / 100.0

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OpsSelectChip("Today", days == 1) { days = 1 }
                OpsSelectChip("7 days", days == 7) { days = 7 }
                OpsSelectChip("All time", days == 0) { days = 0 }
            }
            OpsCard {
                Text("Revenue report (demo data)", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                StatLine("Coins sold", "$coinsSold")
                StatLine("Gross sales", "$%.2f".format(gross))
                StatLine("Refunded", "$%.2f".format(refunded))
                StatLine("Payouts paid", "$%.2f".format(paid))
                StatLine("Payouts owed (all pending)", "$%.2f".format(owed))
                HorizontalDivider(color = VioraBorder)
                StatLine("Estimated platform balance", "$%.2f".format(gross - refunded - owed - paid))
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = VioraMutedText, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------- FRAUD
private fun androidx.compose.foundation.lazy.LazyListScope.fraudSection() {
    item {
        val alerts by VioraRepository.fraudAlerts.collectAsState()
        val open = alerts.filter { !it.resolved }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (open.isEmpty()) OpsEmpty("No open fraud alerts")
            open.forEach { a ->
                OpsCard {
                    OpsChip(a.severity, if (a.severity == "HIGH") VioraErrorRed else VioraWarmGold)
                    Text(a.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(a.reason, color = VioraMutedText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpsButton("Ban user", color = VioraErrorRed) { VioraRepository.resolveFraudAlert(a.id, true) }
                        OpsButton("Dismiss", color = VioraSurfaceVariant) { VioraRepository.resolveFraudAlert(a.id, false) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- APPEALS
private fun androidx.compose.foundation.lazy.LazyListScope.appealsSection() {
    item {
        val appeals by VioraRepository.appeals.collectAsState()
        val pending = appeals.filter { it.status == AppealStatus.PENDING }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pending.isEmpty()) OpsEmpty("No pending appeals")
            pending.forEach { a ->
                OpsCard {
                    Text(a.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("\"${a.reason}\"", color = VioraMutedText, fontSize = 12.sp)
                    Text(opsTimeAgo(a.createdMs), color = VioraSubtleText, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpsButton("Approve (unban)", color = VioraSuccessGreen) { VioraRepository.decideAppeal(a.id, true) }
                        OpsButton("Reject", color = VioraErrorRed) { VioraRepository.decideAppeal(a.id, false) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- MODERATOR SHIFTS
private fun androidx.compose.foundation.lazy.LazyListScope.modsSection() {
    item {
        val mods by VioraRepository.moderators.collectAsState()
        val tasks by VioraRepository.tasks.collectAsState()
        val reports by VioraRepository.reports.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Numbers are calculated from reports resolved and tasks finished.", color = VioraMutedText, fontSize = 11.sp)
            mods.forEach { m ->
                val resolved = reports.filter { it.resolvedByName == m.name && it.resolvedMs > 0 }
                val avg = if (resolved.isEmpty()) "-" else "${resolved.map { (it.resolvedMs - it.timestampMs) / 60000 }.average().toInt()} min"
                val open = tasks.count { it.assignedToId == m.id && it.status != TaskStatus.DONE }
                val done = tasks.count { it.assignedToId == m.id && it.status == TaskStatus.DONE }
                OpsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(m.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OpsChip(if (m.isOnline) "ONLINE" else "OFFLINE", if (m.isOnline) VioraSuccessGreen else VioraMutedText)
                    }
                    StatLine("Reports cleared", "${resolved.size}")
                    StatLine("Avg response time", avg)
                    StatLine("Tasks done", "$done")
                    StatLine("Open tasks", "$open")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- CREATORS
private fun androidx.compose.foundation.lazy.LazyListScope.creatorsSection() {
    item {
        val apps by VioraRepository.applications.collectAsState()
        val pending = apps.filter { it.status == ApplicationStatus.PENDING }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pending.isEmpty()) OpsEmpty("No pending applications")
            pending.forEach { a ->
                OpsCard {
                    Text(a.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(a.pitch, color = VioraMutedText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpsButton("Approve + verify", color = VioraSuccessGreen) { VioraRepository.decideApplication(a.id, true) }
                        OpsButton("Reject", color = VioraErrorRed) { VioraRepository.decideApplication(a.id, false) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- FEATURED
private fun androidx.compose.foundation.lazy.LazyListScope.featuredSection() {
    item {
        val streams by VioraRepository.liveStreams.collectAsState()
        val featured by VioraRepository.featuredStreamIds.collectAsState()
        val live = streams.filter { it.status == StreamStatus.LIVE }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The featured stream appears at the top of Home.", color = VioraMutedText, fontSize = 11.sp)
            if (live.isEmpty()) OpsEmpty("No live streams right now")
            live.forEach { s ->
                OpsCard {
                    Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("by ${s.hostName}", color = VioraMutedText, fontSize = 11.sp)
                    val on = s.id in featured
                    OpsButton(if (on) "Unfeature" else "Feature on Home", color = if (on) VioraSurfaceVariant else VioraWarmGold) {
                        VioraRepository.toggleFeatured(s.id)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- ANNOUNCEMENTS
private fun androidx.compose.foundation.lazy.LazyListScope.announceSection() {
    item {
        val list by VioraRepository.announcements.collectAsState()
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OpsCard {
                OpsField(title, { title = it }, "Title")
                OpsField(body, { body = it }, "Message", singleLine = false)
                OpsButton("Post to all users", enabled = title.isNotBlank()) {
                    VioraRepository.postAnnouncement(title, body)
                    title = ""; body = ""
                }
            }
            list.forEach { a ->
                OpsCard {
                    Text(a.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(a.body, color = VioraMutedText, fontSize = 12.sp)
                    Text(opsTimeAgo(a.timestampMs), color = VioraSubtleText, fontSize = 10.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- KEYWORDS
private fun androidx.compose.foundation.lazy.LazyListScope.keywordsSection() {
    item {
        val words by VioraRepository.keywords.collectAsState()
        var word by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chat messages containing these words are held for moderator review.", color = VioraMutedText, fontSize = 11.sp)
            OpsCard {
                OpsField(word, { word = it }, "Add a word or phrase")
                OpsButton("Add keyword", enabled = word.isNotBlank()) {
                    VioraRepository.addKeyword(word)
                    word = ""
                }
            }
            words.forEach { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VioraCardDark)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(w, color = Color.White, fontSize = 13.sp)
                    Text("Remove", color = VioraErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { VioraRepository.removeKeyword(w) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- KILL SWITCH
private fun androidx.compose.foundation.lazy.LazyListScope.killSection() {
    item {
        val settings by VioraRepository.settings.collectAsState()
        var confirmStreams by remember { mutableStateOf(false) }
        var confirmGifts by remember { mutableStateOf(false) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OpsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause ALL live streams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ends every live room and blocks Go Live.", color = VioraMutedText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.streamsPaused,
                        onCheckedChange = { if (it) confirmStreams = true else VioraRepository.setStreamsPaused(false) },
                        colors = SwitchDefaults.colors(checkedTrackColor = VioraErrorRed)
                    )
                }
            }
            OpsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause ALL gifting", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Nobody can send virtual gifts.", color = VioraMutedText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.giftingPaused,
                        onCheckedChange = { if (it) confirmGifts = true else VioraRepository.setGiftingPaused(false) },
                        colors = SwitchDefaults.colors(checkedTrackColor = VioraErrorRed)
                    )
                }
            }
        }

        if (confirmStreams) {
            AlertDialog(
                onDismissRequest = { confirmStreams = false },
                title = { Text("Pause all live streams?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Every live room will end right now.", color = VioraMutedText) },
                confirmButton = { OpsButton("Pause streams", color = VioraErrorRed) { VioraRepository.setStreamsPaused(true); confirmStreams = false } },
                dismissButton = { TextButton(onClick = { confirmStreams = false }) { Text("Cancel", color = VioraMutedText) } },
                containerColor = VioraCardDark
            )
        }
        if (confirmGifts) {
            AlertDialog(
                onDismissRequest = { confirmGifts = false },
                title = { Text("Pause all gifting?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Gift sending is blocked until you turn this off.", color = VioraMutedText) },
                confirmButton = { OpsButton("Pause gifting", color = VioraErrorRed) { VioraRepository.setGiftingPaused(true); confirmGifts = false } },
                dismissButton = { TextButton(onClick = { confirmGifts = false }) { Text("Cancel", color = VioraMutedText) } },
                containerColor = VioraCardDark
            )
        }
    }
}

// ---------------------------------------------------------------- AUDIT
private fun androidx.compose.foundation.lazy.LazyListScope.auditSection() {
    item {
        val log by VioraRepository.auditLog.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (log.isEmpty()) OpsEmpty("No staff actions recorded yet")
            log.forEach { e ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VioraCardDark)
                        .padding(12.dp)
                ) {
                    Text(e.action, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${e.actorName} (${e.actorRole.name}) • ${e.target} • ${opsTimeAgo(e.timestampMs)}", color = VioraMutedText, fontSize = 10.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- CATALOG
private fun androidx.compose.foundation.lazy.LazyListScope.catalogSection() {
    item {
        val packages by VioraRepository.coinPackages.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Real-money prices are also set in Google Play Console. Changing a price here does not change what Google charges.",
                color = VioraMutedText, fontSize = 11.sp
            )
            Text("Coin packages", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            packages.forEach { p -> PackageEditor(p) }
        }
    }
    item {
        val gifts by VioraRepository.giftCatalog.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Virtual gifts", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            gifts.forEach { g -> GiftEditor(g) }
        }
    }
}

@Composable
private fun PackageEditor(p: CoinPackage) {
    var coins by remember(p.id, p.coins) { mutableStateOf(p.coins.toString()) }
    var bonus by remember(p.id, p.bonusCoins) { mutableStateOf(p.bonusCoins.toString()) }
    var price by remember(p.id, p.priceUsd) { mutableStateOf(p.priceUsd) }
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(p.id, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Switch(checked = p.isActive, onCheckedChange = { VioraRepository.setCoinPackageActive(p.id, it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OpsField(coins, { coins = it.filter { c -> c.isDigit() } }, "Coins", Modifier.weight(1f))
            OpsField(bonus, { bonus = it.filter { c -> c.isDigit() } }, "Bonus", Modifier.weight(1f))
        }
        OpsField(price, { price = it }, "Price (USD)")
        OpsButton("Save package") {
            VioraRepository.updateCoinPackage(p.id, coins.toLongOrNull() ?: 0L, bonus.toLongOrNull() ?: 0L, price)
        }
    }
}

@Composable
private fun GiftEditor(g: VirtualGift) {
    var cost by remember(g.id, g.coinCost) { mutableStateOf(g.coinCost.toString()) }
    OpsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${g.emoji} ${g.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Switch(checked = g.isActive, onCheckedChange = { VioraRepository.setGiftActive(g.id, it) })
        }
        OpsField(cost, { cost = it.filter { c -> c.isDigit() } }, "Coin cost")
        OpsButton("Save price") { VioraRepository.updateGiftCost(g.id, cost.toLongOrNull() ?: 0L) }
    }
}
