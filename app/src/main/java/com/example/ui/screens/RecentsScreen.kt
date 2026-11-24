package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallAnalyticsSummary
import com.example.data.model.CallDirection
import com.example.data.model.CallerIdentityReport
import com.example.data.model.CallerTrustStatus
import com.example.data.model.CommunitySpamReport
import com.example.data.model.DialerSettings
import com.example.data.model.GroupedCallLogItem
import com.example.data.model.PermissionSnapshot
import com.example.data.model.PrivateContactEntry
import com.example.data.model.SimBalanceInfo
import com.example.data.model.SpamCategory
import com.example.data.telephony.CallerIntelligenceEngine
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.CallLogFilter
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.CallOutgoingBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecentsScreen(
    groupedLogs: List<GroupedCallLogItem>,
    analytics: CallAnalyticsSummary,
    todayCallCount: Int,
    activeFilter: CallLogFilter,
    searchQuery: String,
    blockedNumbers: List<BlockedNumberEntry>,
    numberNotes: Map<String, String>,
    permissions: PermissionSnapshot,
    settings: DialerSettings = DialerSettings(),
    communityReports: Map<String, CommunitySpamReport> = emptyMap(),
    privateContacts: List<PrivateContactEntry> = emptyList(),
    simBalances: List<SimBalanceInfo> = emptyList(),
    blockedCanonicalKeys: Set<String> = emptySet(),
    callerReports: Map<String, CallerIdentityReport> = emptyMap(),
    onSelectFilter: (CallLogFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCallChooseSim: (String) -> Unit,
    onCallWithSim: (String, Int) -> Unit,
    onEditInDialer: (String) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onSubmitSpamReport: (String, SpamCategory, String, Boolean) -> Unit = { _, _, _, _ -> },
    onRefreshSimBalance: (Int) -> Unit = {},
    onUpdateSimUssdCode: (Int, String) -> Unit = { _, _ -> },
    onDialSimUssd: (Int) -> Unit = {},
    onDeleteEntry: (Long) -> Unit,
    onDeleteMultiple: (Set<Long>) -> Unit,
    onOpenFullProfile: (GroupedCallLogItem) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit = {},
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    var showSimBalanceSheet by remember { mutableStateOf(false) }
    var showAnalysisSheet by remember { mutableStateOf(false) }

    var activeHistoryGroupKey by remember { mutableStateOf<String?>(null) }
    var noteDialogNumber by remember { mutableStateOf<String?>(null) }
    var reportDialogNumber by remember { mutableStateOf<String?>(null) }

    val activeHistoryGroup = remember(activeHistoryGroupKey, groupedLogs) {
        groupedLogs.firstOrNull { it.groupKey == activeHistoryGroupKey }
    }

    val effectiveBlockedKeys = remember(blockedCanonicalKeys, blockedNumbers) {
        blockedCanonicalKeys.ifEmpty {
            blockedNumbers.mapNotNull {
                PhoneNumberInfoHelper.canonicalKey(it.number).takeIf { k -> k.isNotBlank() }
            }.toSet()
        }
    }

    fun isNumberBlocked(number: String): Boolean {
        val canonical = PhoneNumberInfoHelper.canonicalKey(number)
        return canonical.isNotBlank() && canonical in effectiveBlockedKeys
    }

    val anyRefreshingBalance = remember(simBalances) { simBalances.any { it.isRefreshing } }
    val balancePillText = remember(simBalances, strings) {
        val withBalance = simBalances.filter { it.balanceShort.isNotBlank() }
        val activeCount = simBalances.count { it.isActive }.coerceAtLeast(1)
        when {
            withBalance.size >= 2 -> "${withBalance[0].balanceShort} • ${withBalance[1].balanceShort}"
            withBalance.size == 1 -> {
                val entry = withBalance.first()
                if (activeCount > 1) "S${entry.slotIndex + 1} ${entry.balanceShort}" else entry.balanceShort
            }
            anyRefreshingBalance -> strings.checkingBalanceLabel
            else -> strings.simBalancePillDefault
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar: Always-visible SIM Balance | [All / Missed] | Analysis + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF16A34A).copy(alpha = 0.13f),
                border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.40f)),
                modifier = Modifier
                    .widthIn(max = 148.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { showSimBalanceSheet = true }
                    .testTag("recents_sim_balance_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (anyRefreshingBalance) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = strings.simBalanceTitle,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = balancePillText,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Segmented [All | Missed] pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentedTopPill(
                        label = strings.filterAll,
                        selected = activeFilter == CallLogFilter.ALL,
                        onClick = { onSelectFilter(CallLogFilter.ALL) }
                    )
                    SegmentedTopPill(
                        label = strings.filterMissed,
                        selected = activeFilter == CallLogFilter.MISSED,
                        onClick = { onSelectFilter(CallLogFilter.MISSED) }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarCircleAction(
                    icon = Icons.Rounded.Analytics,
                    contentDescription = strings.analysisTitle,
                    containerColor = Color(0xFF16A34A).copy(alpha = 0.16f),
                    iconTint = Color(0xFF15803D),
                    onClick = { showAnalysisSheet = true },
                    modifier = Modifier.testTag("recents_analysis_button")
                )
                TopBarCircleAction(
                    icon = Icons.Rounded.Settings,
                    contentDescription = strings.settingsTitle,
                    containerColor = Color(0xFFD97706).copy(alpha = 0.16f),
                    iconTint = Color(0xFFB45309),
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("recents_settings_button")
                )
            }
        }

        // Search Recents Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(strings.searchRecentsHint) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("recents_search_input")
        )

        // Filter Chips Row (All, Missed, Incoming, Outgoing, SIM 1, SIM 2)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(CallLogFilter.entries) { filter ->
                val selected = filter == activeFilter
                val label = when (filter) {
                    CallLogFilter.ALL -> strings.filterAll
                    CallLogFilter.MISSED -> strings.filterMissed
                    CallLogFilter.INCOMING -> strings.filterIncoming
                    CallLogFilter.OUTGOING -> strings.filterOutgoing
                    CallLogFilter.SIM1 -> "SIM 1"
                    CallLogFilter.SIM2 -> "SIM 2"
                }
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // Info Strip: Today Count + Swipe Hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenPrivacyDashboard)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = strings.privacyDashboardTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${strings.todayCallsPrefix}: $todayCallCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = strings.swipeHintText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!permissions.readCallLog) {
            EmptyStateCard(
                icon = Icons.Rounded.History,
                title = strings.callLogPermTitle,
                subtitle = strings.callLogPermSub,
                actionLabel = strings.grantPermission,
                onAction = onRequestPermissions
            )
        } else if (groupedLogs.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.History,
                title = strings.noCallHistoryTitle,
                subtitle = strings.noCallHistorySub,
                actionLabel = null,
                onAction = {}
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupedLogs, key = { it.groupKey }, contentType = { "recent_call_card" }) { group ->
                    val canonical = remember(group.number) { PhoneNumberInfoHelper.canonicalKey(group.number) }
                    val savedNote = if (canonical.isNotBlank()) numberNotes[canonical] else null
                    val blocked = canonical.isNotBlank() && canonical in effectiveBlockedKeys
                    val callerReport = callerReports[group.groupKey] ?: remember(
                        group.number,
                        group.displayTitle,
                        group.photoUri,
                        savedNote,
                        blocked,
                        communityReports,
                        privateContacts,
                        settings
                    ) {
                        val contactHint = group.displayTitle.takeIf {
                            it != PhoneNumberInfoHelper.formatDisplayNumber(group.number) && it != group.number
                        }
                        CallerIntelligenceEngine.evaluate(
                            rawNumber = group.number,
                            contactName = contactHint,
                            photoUri = group.photoUri,
                            note = savedNote,
                            blockedNumbers = blockedNumbers,
                            communityReports = communityReports,
                            privateContacts = privateContacts,
                            settings = settings
                        )
                    }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onCallChooseSim(group.number)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onDeleteMultiple(group.allIds)
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val dir = dismissState.dismissDirection
                            val isCallSwipe = dir == SwipeToDismissBoxValue.StartToEnd
                            val bgColor = when (dir) {
                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF16A34A)
                                SwipeToDismissBoxValue.EndToStart -> CallMissedRed
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = bgColor,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isCallSwipe) Arrangement.Start else Arrangement.End
                                ) {
                                    if (dir != SwipeToDismissBoxValue.Settled) {
                                        Icon(
                                            imageVector = if (isCallSwipe) Icons.Rounded.Call else Icons.Rounded.DeleteOutline,
                                            contentDescription = if (isCallSwipe) strings.callButton else strings.delete,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isCallSwipe) strings.callButton else strings.delete,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        GroupedRecentCardContent(
                            group = group,
                            callerReport = callerReport,
                            showCallerIdBadge = settings.callerIdEnabled,
                            savedNote = savedNote,
                            isBlocked = blocked,
                            onCardClick = { activeHistoryGroupKey = group.groupKey },
                            onQuickCall = { onCallChooseSim(group.number) },
                            onOpenProfile = { onOpenFullProfile(group) },
                            onOpenHistory = { activeHistoryGroupKey = group.groupKey },
                            onTagCallerId = { noteDialogNumber = group.number },
                            onReportSpam = { reportDialogNumber = group.number },
                            onEditInKeypad = { onEditInDialer(group.number) },
                            onToggleBlock = { onToggleBlock(group.number, blocked) },
                            onDeleteGroup = { onDeleteMultiple(group.allIds) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Grouped Call History Detail (matches Screenshot 3)
    activeHistoryGroup?.let { group ->
        val blocked = isNumberBlocked(group.number)
        val canonical = PhoneNumberInfoHelper.canonicalKey(group.number)
        val note = if (canonical.isNotBlank()) numberNotes[canonical] else null
        val contactHint = group.displayTitle.takeIf {
            it != PhoneNumberInfoHelper.formatDisplayNumber(group.number) && it != group.number
        }
        val report = CallerIntelligenceEngine.evaluate(
            rawNumber = group.number,
            contactName = contactHint,
            photoUri = group.photoUri,
            note = note,
            blockedNumbers = blockedNumbers,
            communityReports = communityReports,
            privateContacts = privateContacts,
            settings = settings
        )
        ModalBottomSheet(
            onDismissRequest = { activeHistoryGroupKey = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GroupedHistoryDetailSheet(
                group = group,
                callerReport = report,
                savedNote = if (report.isSensitiveMasked) null else note,
                isBlocked = blocked,
                onCallNow = {
                    activeHistoryGroupKey = null
                    onCallChooseSim(group.number)
                },
                onEditNumber = {
                    activeHistoryGroupKey = null
                    onEditInDialer(group.number)
                },
                onToggleBlock = { onToggleBlock(group.number, blocked) },
                onOpenFullProfile = {
                    activeHistoryGroupKey = null
                    onOpenFullProfile(group)
                },
                onDeleteEntry = onDeleteEntry
            )
        }
    }

    // Tag / Caller ID Note Dialog
    noteDialogNumber?.let { targetNumber ->
        val canonical = PhoneNumberInfoHelper.canonicalKey(targetNumber)
        val existingNote = if (canonical.isNotBlank()) numberNotes[canonical].orEmpty() else ""
        var noteInput by remember(targetNumber, existingNote) { mutableStateOf(existingNote) }

        AlertDialog(
            onDismissRequest = { noteDialogNumber = null },
            title = { Text(strings.noteDialogTitle) },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text(strings.notePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(targetNumber, noteInput)
                        noteDialogNumber = null
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogNumber = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Community Spam / Business Report Dialog
    reportDialogNumber?.let { targetNumber ->
        val isBn = settings.language == com.example.data.model.AppLanguage.BENGALI
        var selectedCategory by remember { mutableStateOf(SpamCategory.TELEMARKETING) }
        var noteInput by remember { mutableStateOf("") }
        val selectable = remember {
            listOf(
                SpamCategory.SCAM_FRAUD,
                SpamCategory.TELEMARKETING,
                SpamCategory.LOAN_DEBT,
                SpamCategory.ROBOCALL,
                SpamCategory.HARASSMENT,
                SpamCategory.VERIFIED_BUSINESS
            )
        }
        AlertDialog(
            onDismissRequest = { reportDialogNumber = null },
            title = { Text(strings.reportNumberTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = PhoneNumberInfoHelper.formatDisplayNumber(targetNumber),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = CallIncomingGreen
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectable.forEach { cat ->
                            val selected = cat == selectedCategory
                            val label = if (isBn) cat.labelBn else cat.labelEn
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text(strings.notePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitSpamReport(
                            targetNumber,
                            selectedCategory,
                            noteInput,
                            selectedCategory == SpamCategory.SCAM_FRAUD || selectedCategory == SpamCategory.HARASSMENT
                        )
                        reportDialogNumber = null
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportDialogNumber = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Call Analysis Sheet
    if (showAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CallAnalysisSheet(analytics = analytics)
        }
    }

    // SIM Balance Sheet
    if (showSimBalanceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSimBalanceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SimBalanceSheet(
                simBalances = simBalances,
                onRefreshSim = onRefreshSimBalance,
                onUpdateUssdCode = onUpdateSimUssdCode,
                onDialUssd = onDialSimUssd
            )
        }
    }
}

@Composable
private fun GroupedRecentCardContent(
    group: GroupedCallLogItem,
    callerReport: CallerIdentityReport,
    showCallerIdBadge: Boolean,
    savedNote: String?,
    isBlocked: Boolean,
    onCardClick: () -> Unit,
    onQuickCall: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onTagCallerId: () -> Unit,
    onReportSpam: () -> Unit,
    onEditInKeypad: () -> Unit,
    onToggleBlock: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    val displayTitle = if (callerReport.isSensitiveMasked) {
        callerReport.displayName
    } else if (callerReport.isBusinessVerified && group.contactId == null) {
        callerReport.displayName
    } else {
        group.displayTitle
    }
    val displayPhoto = if (callerReport.isSensitiveMasked) null else group.photoUri

    val (dirIcon, dirColor) = when (group.latestDirection) {
        CallDirection.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived to CallIncomingGreen
        CallDirection.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade to Color(0xFF0284C7)
        CallDirection.MISSED -> Icons.AutoMirrored.Rounded.CallMissed to CallMissedRed
        CallDirection.REJECTED, CallDirection.BLOCKED -> Icons.Rounded.Block to CallMissedRed
        CallDirection.VOICEMAIL -> Icons.Rounded.Call to MaterialTheme.colorScheme.tertiary
    }

    val relativeTime = remember(group.latestDateMillis) {
        DateUtils.getRelativeTimeSpanString(
            group.latestDateMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    val cardBorderColor = when {
        isBlocked || callerReport.status == CallerTrustStatus.FRAUD_WARNING -> CallMissedRed.copy(alpha = 0.55f)
        callerReport.status == CallerTrustStatus.SPAM_SUSPECTED -> Color(0xFFD97706).copy(alpha = 0.55f)
        callerReport.status == CallerTrustStatus.SENSITIVE_PROTECTED -> Color(0xFF6366F1).copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(width = 1.dp, color = cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("grouped_recent_card_${group.groupKey}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallerAvatar(
                title = displayTitle,
                photoUri = displayPhoto,
                size = 44.dp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isBlocked ||
                                callerReport.status == CallerTrustStatus.FRAUD_WARNING ||
                                group.latestDirection == CallDirection.MISSED -> CallMissedRed
                            callerReport.status == CallerTrustStatus.VERIFIED_BUSINESS -> Color(0xFF15803D)
                            callerReport.status == CallerTrustStatus.SENSITIVE_PROTECTED -> Color(0xFF4F46E5)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (group.callCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFD97706).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.38f))
                        ) {
                            Text(
                                text = "(${group.callCount})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = null,
                        tint = dirColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${group.callCount} ${strings.callsWord} • $relativeTime",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (group.latestSimSlotIndex != null) {
                        val isSim1 = group.latestSimSlotIndex == 0
                        val simColor = if (isSim1) Color(0xFF16A34A) else Color(0xFF2563EB)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = simColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, simColor.copy(alpha = 0.38f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SimCard,
                                    contentDescription = null,
                                    tint = simColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "SIM ${group.latestSimSlotIndex + 1}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = simColor
                                )
                            }
                        }
                    }
                }

                val showSpecialStatusBadge = callerReport.status in setOf(
                    CallerTrustStatus.FRAUD_WARNING,
                    CallerTrustStatus.SPAM_SUSPECTED,
                    CallerTrustStatus.VERIFIED_BUSINESS,
                    CallerTrustStatus.SENSITIVE_PROTECTED,
                    CallerTrustStatus.BLOCKED
                ) || (showCallerIdBadge && callerReport.status == CallerTrustStatus.UNKNOWN_CLEAN)

                if (showSpecialStatusBadge) {
                    val chipColor = when (callerReport.status) {
                        CallerTrustStatus.FRAUD_WARNING, CallerTrustStatus.BLOCKED -> CallMissedRed
                        CallerTrustStatus.SPAM_SUSPECTED -> Color(0xFFD97706)
                        CallerTrustStatus.VERIFIED_BUSINESS -> Color(0xFF15803D)
                        CallerTrustStatus.SENSITIVE_PROTECTED -> Color(0xFF4F46E5)
                        else -> Color(0xFF0284C7)
                    }
                    val badgeLabel = if (callerReport.status == CallerTrustStatus.UNKNOWN_CLEAN) {
                        "${callerReport.statusBadge} • ${callerReport.carrierRegion.substringBefore(" ·")}"
                    } else {
                        callerReport.statusBadge
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = chipColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!savedNote.isNullOrBlank() && !callerReport.isSensitiveMasked) {
                    Text(
                        text = savedNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = Color(0xFF16A34A).copy(alpha = 0.13f),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onQuickCall)
                    .testTag("recent_call_button_${group.groupKey}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = strings.callButton,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("recent_more_button_${group.groupKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.contactProfile) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF16A34A))
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.callHistory) },
                            leadingIcon = {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenHistory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.tagCallerId) },
                            leadingIcon = {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color(0xFF2563EB))
                            },
                            onClick = {
                                menuExpanded = false
                                onTagCallerId()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.editInKeypad) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            onClick = {
                                menuExpanded = false
                                onEditInKeypad()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.reportNumberTitle) },
                            leadingIcon = {
                                Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = Color(0xFFD97706))
                            },
                            onClick = {
                                menuExpanded = false
                                onReportSpam()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBlocked) strings.unblock else strings.markSpamAndBlock,
                                    color = CallMissedRed
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Block, contentDescription = null, tint = CallMissedRed)
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleBlock()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.deleteHistory, color = CallMissedRed) },
                            leadingIcon = {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = CallMissedRed)
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteGroup()
                            }
                        )
                    }
                }
            }
        }
    }

@Composable
private fun GroupedHistoryDetailSheet(
    group: GroupedCallLogItem,
    callerReport: CallerIdentityReport,
    savedNote: String?,
    isBlocked: Boolean,
    onCallNow: () -> Unit,
    onEditNumber: () -> Unit,
    onToggleBlock: () -> Unit,
    onOpenFullProfile: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val formattedNumber = callerReport.formattedNumber
    val carrier = callerReport.carrierRegion.substringBefore(" ·")
    val sheetTitle = if (callerReport.isSensitiveMasked) {
        callerReport.displayName
    } else if (callerReport.isBusinessVerified && group.contactId == null) {
        callerReport.displayName
    } else {
        group.displayTitle
    }
    val sheetPhoto = if (callerReport.isSensitiveMasked) null else group.photoUri
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: Avatar, Name + Formatted Number, Block button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallerAvatar(title = sheetTitle, photoUri = sheetPhoto, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sheetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedNumber,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF15803D)
                )
                Text(
                    text = "${callerReport.statusBadge} • ${callerReport.subStatusLine}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!savedNote.isNullOrBlank()) {
                    Text(
                        text = savedNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = onToggleBlock) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    tint = CallMissedRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isBlocked) strings.unblock else strings.block,
                    color = CallMissedRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Primary Actions: Call Now + Edit Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCallNow,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("history_sheet_call_now")
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.callNow, color = Color.White, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onEditNumber,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("history_sheet_edit_number")
            ) {
                Icon(Icons.Rounded.Dialpad, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.editNumber, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Full Profile, Notes & Reminder button
        OutlinedButton(
            onClick = onOpenFullProfile,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("history_sheet_open_profile")
        ) {
            Text(
                text = strings.openFullProfile,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "${strings.callHistory} (${group.callCount})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(group.entries, key = { it.id }) { entry ->
                val dirLabel = when (entry.direction) {
                    CallDirection.INCOMING -> strings.incomingDirection
                    CallDirection.OUTGOING -> strings.outgoingDirection
                    CallDirection.MISSED -> strings.missedDirection
                    CallDirection.REJECTED -> strings.rejectedDirection
                    CallDirection.BLOCKED -> strings.blockedDirection
                    CallDirection.VOICEMAIL -> strings.voicemailDirection
                }
                val dirColor = when (entry.direction) {
                    CallDirection.INCOMING -> CallIncomingGreen
                    CallDirection.OUTGOING -> Color(0xFF15803D)
                    CallDirection.MISSED, CallDirection.REJECTED, CallDirection.BLOCKED -> CallMissedRed
                    CallDirection.VOICEMAIL -> MaterialTheme.colorScheme.tertiary
                }
                val simText = entry.simSlotIndex?.let { "SIM ${it + 1}" } ?: "SIM"
                val metaLine = buildString {
                    append(dirLabel)
                    append(" • ")
                    append(simText)
                    if (carrier.isNotBlank()) {
                        append(" • ")
                        append(carrier)
                    }
                }
                val timeText = remember(entry.dateMillis) {
                    dateFormat.format(Date(entry.dateMillis))
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = dirColor
                            )
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatShortDuration(entry.durationSeconds),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        IconButton(
                            onClick = { onDeleteEntry(entry.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallerAvatar(
    title: String,
    photoUri: String?,
    size: Dp
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.size(size)
    ) {
        if (!photoUri.isNullOrBlank()) {
            val context = LocalContext.current
            val request = remember(photoUri) {
                ImageRequest.Builder(context).data(photoUri).size(128).crossfade(false).build()
            }
            AsyncImage(
                model = request,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SegmentedTopPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Color(0xFFD9B872).copy(alpha = 0.35f) else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF926E22) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TopBarCircleAction(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun CallAnalysisSheet(analytics: CallAnalyticsSummary) {
    val strings = LocalAppStrings.current
    val totalSim = (analytics.sim1Count + analytics.sim2Count).coerceAtLeast(1)
    val sim1Fraction = analytics.sim1Count.toFloat() / totalSim.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.analysisTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = strings.totalCalls,
                value = analytics.totalCalls.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = strings.totalDuration,
                value = formatShortDuration(analytics.totalDurationSeconds),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                label = strings.filterIncoming,
                value = analytics.incomingCount.toString(),
                accent = CallIncomingGreen,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = strings.filterOutgoing,
                value = analytics.outgoingCount.toString(),
                accent = CallOutgoingBlue,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = strings.filterMissed,
                value = analytics.missedCount.toString(),
                accent = CallMissedRed,
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.simBreakdown,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SIM 1: ${analytics.sim1Count}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "SIM 2: ${analytics.sim2Count}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF2563EB)
                    )
                }
                LinearProgressIndicator(
                    progress = { sim1Fraction },
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF2563EB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        if (analytics.topContacts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.topContacts,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                analytics.topContacts.forEach { (name, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$count×",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimBalanceSheet(
    simBalances: List<SimBalanceInfo>,
    onRefreshSim: (Int) -> Unit,
    onUpdateUssdCode: (Int, String) -> Unit,
    onDialUssd: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.simBalanceTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = strings.simBalanceSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        simBalances.forEach { info ->
            val accent = if (info.slotIndex == 0) Color(0xFF16A34A) else Color(0xFF2563EB)
            var editingCode by remember(info.slotIndex) { mutableStateOf(false) }
            var ussdInput by remember(info.slotIndex, info.ussdCode) { mutableStateOf(info.ussdCode) }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SimCard,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "SIM ${info.slotIndex + 1} · ${info.carrierName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = info.balanceShort.ifBlank { "--" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Text(
                        text = info.rawResponse.ifBlank { strings.balanceNotCheckedYet },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (info.updatedAtMillis > 0L) {
                        val rel = remember(info.updatedAtMillis) {
                            DateUtils.getRelativeTimeSpanString(
                                info.updatedAtMillis,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                            ).toString()
                        }
                        Text(
                            text = rel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (editingCode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ussdInput,
                                onValueChange = { ussdInput = it },
                                label = { Text(strings.ussdCodeLabel) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onUpdateUssdCode(info.slotIndex, ussdInput)
                                    editingCode = false
                                }
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = strings.save, tint = accent)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${strings.ussdCodeLabel}: ${info.ussdCode}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { editingCode = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = strings.edit,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onRefreshSim(info.slotIndex) },
                            enabled = info.isActive && !info.isRefreshing,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (info.isRefreshing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (info.isRefreshing) strings.checkingBalanceLabel else strings.checkBalanceButton,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { onDialUssd(info.slotIndex) },
                            enabled = info.isActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dialpad,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.dialUssdButton, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun formatShortDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
