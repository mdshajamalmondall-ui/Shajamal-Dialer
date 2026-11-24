package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallAnalyticsSummary
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.GroupedCallLogItem
import com.example.data.model.PermissionSnapshot
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.CallLogFilter
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.CallOutgoingBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectFilter: (CallLogFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCallChooseSim: (String) -> Unit,
    onCallWithSim: (String, Int) -> Unit,
    onEditInDialer: (String) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onDeleteMultiple: (Set<Long>) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val pictureTheme = LocalPictureTheme.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedGroupKeys by remember { mutableStateOf(setOf<String>()) }
    var showAnalysisSheet by remember { mutableStateOf(false) }

    var activeHistoryGroupKey by remember { mutableStateOf<String?>(null) }
    var activeProfileGroupKey by remember { mutableStateOf<String?>(null) }
    var noteDialogNumber by remember { mutableStateOf<String?>(null) }

    val activeHistoryGroup = remember(activeHistoryGroupKey, groupedLogs) {
        groupedLogs.firstOrNull { it.groupKey == activeHistoryGroupKey }
    }
    val activeProfileGroup = remember(activeProfileGroupKey, groupedLogs) {
        groupedLogs.firstOrNull { it.groupKey == activeProfileGroupKey }
    }

    fun isNumberBlocked(number: String): Boolean {
        val canonical = PhoneNumberInfoHelper.canonicalKey(number)
        if (canonical.isBlank()) return false
        return blockedNumbers.any { PhoneNumberInfoHelper.canonicalKey(it.number) == canonical }
    }

    fun openContactOrInsert(group: GroupedCallLogItem) {
        if (group.contactId != null) {
            val contactUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                group.contactId.toString()
            )
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, contactUri)) }
        } else {
            val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, group.number)
            }
            runCatching { context.startActivity(insertIntent) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar matching the 3 screenshots: Edit | [All / Missed] | Analysis + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        isEditMode = !isEditMode
                        if (!isEditMode) selectedGroupKeys = emptySet()
                    }
                    .testTag("recents_edit_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Rounded.Done else Icons.Rounded.Edit,
                        contentDescription = strings.edit,
                        tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isEditMode) strings.save else strings.edit,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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

        // Batch Edit Bar
        AnimatedVisibility(visible = isEditMode && groupedLogs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        selectedGroupKeys = if (selectedGroupKeys.size == groupedLogs.size) {
                            emptySet()
                        } else {
                            groupedLogs.map { it.groupKey }.toSet()
                        }
                    }
                ) {
                    Text(strings.selectAll)
                }
                val idsToDelete = remember(selectedGroupKeys, groupedLogs) {
                    groupedLogs.filter { it.groupKey in selectedGroupKeys }
                        .flatMap { it.allIds }
                        .toSet()
                }
                Button(
                    onClick = {
                        onDeleteMultiple(idsToDelete)
                        selectedGroupKeys = emptySet()
                        isEditMode = false
                    },
                    enabled = idsToDelete.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = CallMissedRed)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.deleteSelected} (${idsToDelete.size})")
                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
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
                items(groupedLogs, key = { it.groupKey }) { group ->
                    val isChecked = group.groupKey in selectedGroupKeys
                    val canonical = PhoneNumberInfoHelper.canonicalKey(group.number)
                    val savedNote = if (canonical.isNotBlank()) numberNotes[canonical] else null
                    val blocked = isNumberBlocked(group.number)

                    if (isEditMode) {
                        GroupedRecentCardContent(
                            group = group,
                            savedNote = savedNote,
                            isBlocked = blocked,
                            editMode = true,
                            checked = isChecked,
                            onCardClick = {
                                selectedGroupKeys = if (isChecked) {
                                    selectedGroupKeys - group.groupKey
                                } else {
                                    selectedGroupKeys + group.groupKey
                                }
                            },
                            onQuickCall = { onCallChooseSim(group.number) },
                            onOpenProfile = { activeProfileGroupKey = group.groupKey },
                            onOpenHistory = { activeHistoryGroupKey = group.groupKey },
                            onTagCallerId = { noteDialogNumber = group.number },
                            onEditInKeypad = { onEditInDialer(group.number) },
                            onToggleBlock = { onToggleBlock(group.number, blocked) },
                            onDeleteGroup = { onDeleteMultiple(group.allIds) }
                        )
                    } else {
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
                                savedNote = savedNote,
                                isBlocked = blocked,
                                editMode = false,
                                checked = false,
                                onCardClick = { activeHistoryGroupKey = group.groupKey },
                                onQuickCall = { onCallChooseSim(group.number) },
                                onOpenProfile = { activeProfileGroupKey = group.groupKey },
                                onOpenHistory = { activeHistoryGroupKey = group.groupKey },
                                onTagCallerId = { noteDialogNumber = group.number },
                                onEditInKeypad = { onEditInDialer(group.number) },
                                onToggleBlock = { onToggleBlock(group.number, blocked) },
                                onDeleteGroup = { onDeleteMultiple(group.allIds) }
                            )
                        }
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
        ModalBottomSheet(
            onDismissRequest = { activeHistoryGroupKey = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GroupedHistoryDetailSheet(
                group = group,
                savedNote = note,
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
                    val key = group.groupKey
                    activeHistoryGroupKey = null
                    activeProfileGroupKey = key
                },
                onDeleteEntry = onDeleteEntry
            )
        }
    }

    // Modal Bottom Sheet: Full Caller Profile, Notes & Reminder
    activeProfileGroup?.let { group ->
        val blocked = isNumberBlocked(group.number)
        val canonical = PhoneNumberInfoHelper.canonicalKey(group.number)
        val note = if (canonical.isNotBlank()) numberNotes[canonical] else null
        ModalBottomSheet(
            onDismissRequest = { activeProfileGroupKey = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FullCallerProfileSheet(
                group = group,
                savedNote = note,
                isBlocked = blocked,
                sim1Accent = pictureTheme.sim1Accent,
                sim2Accent = pictureTheme.sim2Accent,
                onCallSim1 = {
                    activeProfileGroupKey = null
                    onCallWithSim(group.number, 0)
                },
                onCallSim2 = {
                    activeProfileGroupKey = null
                    onCallWithSim(group.number, 1)
                },
                onEditTag = { noteDialogNumber = group.number },
                onOpenWhatsApp = {
                    val url = PhoneNumberInfoHelper.buildWhatsAppUrl(group.number)
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                onSetReminder = {
                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, "Call ${group.displayTitle}")
                        putExtra(CalendarContract.Events.DESCRIPTION, "Follow-up call: ${group.number}")
                    }
                    runCatching { context.startActivity(calIntent) }
                },
                onOpenContact = { openContactOrInsert(group) },
                onToggleBlock = { onToggleBlock(group.number, blocked) }
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

    // Call Analysis Sheet
    if (showAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CallAnalysisSheet(analytics = analytics)
        }
    }
}

@Composable
private fun GroupedRecentCardContent(
    group: GroupedCallLogItem,
    savedNote: String?,
    isBlocked: Boolean,
    editMode: Boolean,
    checked: Boolean,
    onCardClick: () -> Unit,
    onQuickCall: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onTagCallerId: () -> Unit,
    onEditInKeypad: () -> Unit,
    onToggleBlock: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

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

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isBlocked) CallMissedRed.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
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
            if (editMode) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onCardClick() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            } else {
                CallerAvatar(
                    title = group.displayTitle,
                    photoUri = group.photoUri,
                    size = 44.dp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = group.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isBlocked || group.latestDirection == CallDirection.MISSED -> CallMissedRed
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

                if (!savedNote.isNullOrBlank()) {
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

            if (!editMode) {
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
}

@Composable
private fun GroupedHistoryDetailSheet(
    group: GroupedCallLogItem,
    savedNote: String?,
    isBlocked: Boolean,
    onCallNow: () -> Unit,
    onEditNumber: () -> Unit,
    onToggleBlock: () -> Unit,
    onOpenFullProfile: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val formattedNumber = remember(group.number) {
        PhoneNumberInfoHelper.formatDisplayNumber(group.number)
    }
    val carrier = remember(group.number) {
        PhoneNumberInfoHelper.detectCarrierAndRegion(group.number).substringBefore(" ·")
    }
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
            CallerAvatar(title = group.displayTitle, photoUri = group.photoUri, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.displayTitle,
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
private fun FullCallerProfileSheet(
    group: GroupedCallLogItem,
    savedNote: String?,
    isBlocked: Boolean,
    sim1Accent: Color,
    sim2Accent: Color,
    onCallSim1: () -> Unit,
    onCallSim2: () -> Unit,
    onEditTag: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onSetReminder: () -> Unit,
    onOpenContact: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val strings = LocalAppStrings.current
    val carrierAndRegion = remember(group.number) {
        PhoneNumberInfoHelper.detectCarrierAndRegion(group.number)
    }
    val totalTalkTime = remember(group.entries) {
        group.entries.sumOf { it.durationSeconds }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CallerAvatar(title = group.displayTitle, photoUri = group.photoUri, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = PhoneNumberInfoHelper.formatDisplayNumber(group.number),
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = carrierAndRegion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = strings.totalCalls,
                value = group.callCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = strings.totalDuration,
                value = formatShortDuration(totalTalkTime),
                accent = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
        }

        // Saved Note / Tag card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onEditTag)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.tagCallerId,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = savedNote?.takeIf { it.isNotBlank() } ?: strings.addNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Rounded.Edit, contentDescription = strings.edit, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        // Direct SIM 1 & SIM 2 call buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCallSim1,
                colors = ButtonDefaults.buttonColors(containerColor = sim1Accent),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.callSim1, color = Color.White)
            }
            Button(
                onClick = onCallSim2,
                colors = ButtonDefaults.buttonColors(containerColor = sim2Accent),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.callSim2, color = Color.White)
            }
        }

        // Reminder, WhatsApp, Contact, Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onSetReminder, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.setCallReminder, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onOpenWhatsApp, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Chat, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.whatsApp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onOpenContact, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (group.contactId != null) Icons.Rounded.Person else Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (group.contactId != null) strings.openSystemContact else strings.saveContact,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(onClick = onToggleBlock, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = CallMissedRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isBlocked) strings.unblock else strings.block,
                    color = CallMissedRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            AsyncImage(
                model = photoUri,
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
