package com.example.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.call.CallRepository
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallAnalyticsSummary
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.CallPlacementResult
import com.example.data.model.CallerIdentityReport
import com.example.data.model.CallerProfileTarget
import com.example.data.model.CommunitySpamReport
import com.example.data.model.ContactItem
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.data.model.DualSimMode
import com.example.data.model.FavoriteGroup
import com.example.data.model.GroupedCallLogItem
import com.example.data.model.HiddenCallEntry
import com.example.data.model.PermissionSnapshot
import com.example.data.model.PrivateContactEntry
import com.example.data.model.SimBalanceInfo
import com.example.data.model.SimSlotInfo
import com.example.data.model.SpamCategory
import com.example.data.preferences.PrivacyVaultDataStore
import com.example.data.preferences.SettingsDataStore
import com.example.data.telephony.CallerIntelligenceEngine
import com.example.data.telephony.DeviceDirectoryRepository
import com.example.data.telephony.DualSimCallManager
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.data.telephony.SimBalanceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class CallLogFilter {
    ALL,
    MISSED,
    INCOMING,
    OUTGOING,
    SIM1,
    SIM2
}

class ShajamalViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)
    private val privacyVaultDataStore = PrivacyVaultDataStore(context)
    private val dualSimCallManager = DualSimCallManager(context)
    private val simBalanceManager = SimBalanceManager(context)
    private val directoryRepository = DeviceDirectoryRepository(context, dualSimCallManager)

    private val initialSettings: DialerSettings = settingsDataStore.getFastSettings()

    val settings: StateFlow<DialerSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialSettings
    )

    val blockedNumbers: StateFlow<List<BlockedNumberEntry>> = settingsDataStore.blockedNumbersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val blockedCanonicalKeys: StateFlow<Set<String>> = blockedNumbers
        .map { list ->
            list.mapNotNull { entry ->
                PhoneNumberInfoHelper.canonicalKey(entry.number).takeIf { it.isNotBlank() }
            }.toSet()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val communityReports: StateFlow<Map<String, CommunitySpamReport>> =
        settingsDataStore.communityReportsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val privateContacts: StateFlow<List<PrivateContactEntry>> =
        privacyVaultDataStore.privateContactsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hiddenCalls: StateFlow<List<HiddenCallEntry>> =
        privacyVaultDataStore.hiddenCallsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _appUnlockedThisSession = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = combine(
        settings,
        _appUnlockedThisSession
    ) { currentSettings, unlocked ->
        currentSettings.appLockEnabled && currentSettings.hasSecurityPin && !unlocked
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialSettings.appLockEnabled && initialSettings.hasSecurityPin
    )

    private val _settingsUnlocked = MutableStateFlow(false)
    val isSettingsLockRequired: StateFlow<Boolean> = combine(
        settings,
        _settingsUnlocked
    ) { currentSettings, unlocked ->
        currentSettings.pinProtectedSettingsEnabled && currentSettings.hasSecurityPin && !unlocked
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val numberNotes: StateFlow<Map<String, String>> = settingsDataStore.numberNotesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val numberGroups: StateFlow<Map<String, FavoriteGroup>> = settingsDataStore.numberGroupsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val _dialField = MutableStateFlow(TextFieldValue(""))
    val dialField: StateFlow<TextFieldValue> = _dialField.asStateFlow()

    private val _simSlots = MutableStateFlow<List<SimSlotInfo>>(emptyList())
    val simSlots: StateFlow<List<SimSlotInfo>> = _simSlots.asStateFlow()

    private val _refreshingBalanceSlots = MutableStateFlow<Set<Int>>(emptySet())
    val simBalances: StateFlow<List<SimBalanceInfo>> = combine(
        _simSlots,
        settingsDataStore.simBalancesFlow,
        _refreshingBalanceSlots
    ) { slots, storedMap, refreshingSlots ->
        (0..1).map { slotIdx ->
            val slot = slots.firstOrNull { it.slotIndex == slotIdx }
            val stored = storedMap[slotIdx]
            val carrier = slot?.carrierName ?: "SIM ${slotIdx + 1}"
            val defaultCode = SimBalanceManager.defaultUssdCodeForCarrier(carrier)
            SimBalanceInfo(
                slotIndex = slotIdx,
                carrierName = carrier,
                isActive = slot?.isActive == true,
                ussdCode = stored?.ussdCode?.takeIf { it.isNotBlank() } ?: defaultCode,
                balanceShort = stored?.balanceShort.orEmpty(),
                rawResponse = stored?.rawResponse.orEmpty(),
                updatedAtMillis = stored?.updatedAtMillis ?: 0L,
                isRefreshing = slotIdx in refreshingSlots
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val initialPermissionSnapshot = readPermissionSnapshotSync(canFullScreen = true)
    private val initialCachedLogs: List<CallLogItem> = if (initialPermissionSnapshot.readCallLog) {
        settingsDataStore.getCachedCallLogsFast()
    } else {
        emptyList()
    }
    private val initialGroupedLogs: List<GroupedCallLogItem> = if (initialCachedLogs.isNotEmpty()) {
        groupAndFilterCallLogs(initialCachedLogs, emptyList(), CallLogFilter.ALL, "")
    } else {
        emptyList()
    }

    private val _permissions = MutableStateFlow(initialPermissionSnapshot)
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    private val _allCallLogs = MutableStateFlow<List<CallLogItem>>(initialCachedLogs)
    private val _callLogsFetchedOnce = MutableStateFlow(false)
    private val _callLogFilter = MutableStateFlow(CallLogFilter.ALL)
    val callLogFilter: StateFlow<CallLogFilter> = _callLogFilter.asStateFlow()

    private val _recentsSearchQuery = MutableStateFlow("")
    val recentsSearchQuery: StateFlow<String> = _recentsSearchQuery.asStateFlow()

    private val _allContacts = MutableStateFlow<List<ContactItem>>(emptyList())
    private val _contactsFetchedOnce = MutableStateFlow(false)
    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private val _recentsCombined: StateFlow<Pair<List<GroupedCallLogItem>, Boolean>> = combine(
        _allCallLogs,
        _allContacts,
        _callLogFilter,
        _recentsSearchQuery,
        _callLogsFetchedOnce
    ) { logs, contacts, filter, query, fetchedOnce ->
        val grouped = groupAndFilterCallLogs(logs, contacts, filter, query)
        val loading = !fetchedOnce && grouped.isEmpty()
        grouped to loading
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialGroupedLogs to (initialPermissionSnapshot.readCallLog && initialGroupedLogs.isEmpty())
        )

    val groupedCallLogs: StateFlow<List<GroupedCallLogItem>> = _recentsCombined
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialGroupedLogs)

    val isCallLogsLoading: StateFlow<Boolean> = combine(
        _permissions,
        _recentsCombined
    ) { perms, recents ->
        perms.readCallLog && recents.second
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialPermissionSnapshot.readCallLog && initialGroupedLogs.isEmpty()
    )

    val recentsCallerReports: StateFlow<Map<String, CallerIdentityReport>> = combine(
        groupedCallLogs,
        blockedNumbers,
        communityReports,
        privateContacts,
        numberNotes,
        settings
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val groups = arr[0] as List<GroupedCallLogItem>
        @Suppress("UNCHECKED_CAST")
        val blocked = arr[1] as List<BlockedNumberEntry>
        @Suppress("UNCHECKED_CAST")
        val reports = arr[2] as Map<String, CommunitySpamReport>
        @Suppress("UNCHECKED_CAST")
        val privates = arr[3] as List<PrivateContactEntry>
        @Suppress("UNCHECKED_CAST")
        val notes = arr[4] as Map<String, String>
        val currentSettings = arr[5] as DialerSettings

        buildMap(groups.size) {
            groups.forEach { group ->
                val canonical = PhoneNumberInfoHelper.canonicalKey(group.number)
                val savedNote = if (canonical.isNotBlank()) notes[canonical] else null
                val contactHint = group.displayTitle.takeIf {
                    it != PhoneNumberInfoHelper.formatDisplayNumber(group.number) && it != group.number
                }
                put(
                    group.groupKey,
                    CallerIntelligenceEngine.evaluate(
                        rawNumber = group.number,
                        contactName = contactHint,
                        photoUri = group.photoUri,
                        note = savedNote,
                        blockedNumbers = blocked,
                        communityReports = reports,
                        privateContacts = privates,
                        settings = currentSettings
                    )
                )
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val todayCallCount: StateFlow<Int> = _allCallLogs.map { logs ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        logs.count { it.dateMillis >= startOfDay }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val callAnalytics: StateFlow<CallAnalyticsSummary> = _allCallLogs.map { logs ->
        val incoming = logs.count { it.direction == CallDirection.INCOMING }
        val outgoing = logs.count { it.direction == CallDirection.OUTGOING }
        val missed = logs.count {
            it.direction == CallDirection.MISSED || it.direction == CallDirection.REJECTED
        }
        val sim1 = logs.count { it.simSlotIndex == 0 }
        val sim2 = logs.count { it.simSlotIndex == 1 }
        val totalDuration = logs.sumOf { it.durationSeconds }
        val top = logs.groupBy { it.displayTitle }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        CallAnalyticsSummary(
            totalCalls = logs.size,
            incomingCount = incoming,
            outgoingCount = outgoing,
            missedCount = missed,
            sim1Count = sim1,
            sim2Count = sim2,
            totalDurationSeconds = totalDuration,
            topContacts = top
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CallAnalyticsSummary(0, 0, 0, 0, 0, 0, 0L, emptyList())
        )

    private val _contactsCombined: StateFlow<Pair<List<ContactItem>, Boolean>> = combine(
        _allContacts,
        privateContacts,
        _contactSearchQuery,
        _contactsFetchedOnce
    ) { contacts, privates, query, fetchedOnce ->
        val privateKeys = privates.mapNotNull {
            PhoneNumberInfoHelper.canonicalKey(it.number).takeIf { k -> k.isNotBlank() }
        }.toSet()
        val publicContacts = if (privateKeys.isEmpty()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.phoneNumbers.none { PhoneNumberInfoHelper.canonicalKey(it) in privateKeys }
            }
        }
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            publicContacts
        } else {
            publicContacts.filter { contact ->
                contact.name.contains(trimmed, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.contains(trimmed, ignoreCase = true) }
            }
        }
        val loading = !fetchedOnce && filtered.isEmpty()
        filtered to loading
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<ContactItem>() to initialPermissionSnapshot.readContacts
        )

    val filteredContacts: StateFlow<List<ContactItem>> = _contactsCombined
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isContactsLoading: StateFlow<Boolean> = combine(
        _permissions,
        _contactsCombined
    ) { perms, combined ->
        perms.readContacts && combined.second
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialPermissionSnapshot.readContacts
    )

    val favoriteContacts: StateFlow<List<ContactItem>> = _allContacts.map { contacts ->
        contacts.filter { it.isStarred }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exactDialedContact: StateFlow<ContactItem?> = combine(
        _dialField,
        _allContacts
    ) { field, contacts ->
        val canonical = PhoneNumberInfoHelper.canonicalKey(field.text)
        if (canonical.length < 6) {
            null
        } else {
            contacts.firstOrNull { c ->
                c.phoneNumbers.any { PhoneNumberInfoHelper.canonicalKey(it) == canonical }
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _rawProfileTarget = MutableStateFlow<CallerProfileTarget?>(null)
    val activeProfileTarget: StateFlow<CallerProfileTarget?> = combine(
        _rawProfileTarget,
        _allContacts
    ) { target, contacts ->
        target?.let { current ->
            val key = PhoneNumberInfoHelper.canonicalKey(current.number)
            val matched = if (current.contactId != null) {
                contacts.firstOrNull { it.id == current.contactId }
            } else {
                contacts.firstOrNull { c ->
                    c.phoneNumbers.any { PhoneNumberInfoHelper.canonicalKey(it) == key && key.isNotBlank() }
                }
            }
            if (matched != null) {
                current.copy(
                    displayTitle = matched.name,
                    photoUri = matched.photoUri ?: current.photoUri,
                    contactId = matched.id,
                    isStarred = matched.isStarred
                )
            } else {
                current
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _simPickerNumber = MutableStateFlow<String?>(null)
    val simPickerNumber: StateFlow<String?> = _simPickerNumber.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _permissionPromptTrigger = MutableSharedFlow<Unit>()
    val permissionPromptTrigger: SharedFlow<Unit> = _permissionPromptTrigger.asSharedFlow()

    private var lastDeviceRefreshMillis = 0L
    private var autoCheckedBalanceThisSession = false

    init {
        refreshDeviceState(force = true)
    }

    fun refreshDeviceState(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastDeviceRefreshMillis < 1500L) return
        lastDeviceRefreshMillis = now

        viewModelScope.launch {
            launch(Dispatchers.IO) { runCatching { settingsDataStore.syncFastCallCache() } }

            var freshLogs: List<CallLogItem> = emptyList()
            var freshContacts: List<ContactItem> = emptyList()
            var resolvedSnapshot = _permissions.value
            var resolvedSlots: List<SimSlotInfo> = emptyList()

            coroutineScope {
                val permDeferred = async(Dispatchers.IO) {
                    val canFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
                    } else {
                        true
                    }
                    readPermissionSnapshotSync(canFullScreen) to dualSimCallManager.isDefaultDialer()
                }
                val slotsDeferred = async(Dispatchers.IO) {
                    dualSimCallManager.loadSimSlots()
                }
                val rawLogsDeferred = async(Dispatchers.IO) {
                    directoryRepository.loadCallLogs(emptyList(), limit = 120)
                }
                val contactsDeferred = async(Dispatchers.IO) {
                    directoryRepository.loadContacts()
                }

                val (permSnapshot, defaultDialer) = permDeferred.await()
                resolvedSnapshot = permSnapshot
                if (permSnapshot != _permissions.value) {
                    _permissions.value = permSnapshot
                }
                _isDefaultDialer.value = defaultDialer

                resolvedSlots = slotsDeferred.await()
                _simSlots.value = resolvedSlots

                val rawLogs = rawLogsDeferred.await()
                freshLogs = withContext(Dispatchers.Default) {
                    rawLogs.map { item ->
                        val slot = dualSimCallManager.resolveSimSlotForAccountId(item.phoneAccountId, resolvedSlots)
                        if (slot != item.simSlotIndex) item.copy(simSlotIndex = slot) else item
                    }
                }
                _allCallLogs.value = freshLogs
                _callLogsFetchedOnce.value = true

                freshContacts = contactsDeferred.await()
                _allContacts.value = freshContacts
                _contactsFetchedOnce.value = true
            }

            if (resolvedSnapshot.readCallLog) {
                launch(Dispatchers.IO) {
                    runCatching { settingsDataStore.cacheRecentCallLogs(freshLogs, freshContacts) }
                }
            }

            if (!autoCheckedBalanceThisSession && resolvedSnapshot.hasCoreDialerPermissions) {
                autoCheckedBalanceThisSession = true
                autoRefreshStaleSimBalances(resolvedSlots)
            }
        }
    }

    private fun readPermissionSnapshotSync(canFullScreen: Boolean): PermissionSnapshot {
        return PermissionSnapshot(
            callPhone = hasPermission(Manifest.permission.CALL_PHONE),
            readPhoneState = hasPermission(Manifest.permission.READ_PHONE_STATE),
            readCallLog = hasPermission(Manifest.permission.READ_CALL_LOG),
            writeCallLog = hasPermission(Manifest.permission.WRITE_CALL_LOG),
            readContacts = hasPermission(Manifest.permission.READ_CONTACTS),
            writeContacts = hasPermission(Manifest.permission.WRITE_CONTACTS),
            postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            },
            canUseFullScreenIntent = canFullScreen
        )
    }

    private fun autoRefreshStaleSimBalances(slots: List<SimSlotInfo>) {
        viewModelScope.launch {
            delay(2500L)
            val storedMap = settingsDataStore.simBalancesFlow.first()
            val now = System.currentTimeMillis()
            val staleThresholdMs = 30L * 60L * 1000L
            slots.filter { it.isActive }.forEach { slot ->
                val stored = storedMap[slot.slotIndex]
                val updatedAt = stored?.updatedAtMillis ?: 0L
                if (now - updatedAt > staleThresholdMs) {
                    refreshSimBalance(slot.slotIndex, silent = true)
                }
            }
        }
    }

    fun refreshSimBalance(slotIndex: Int, silent: Boolean = false) {
        val slot = _simSlots.value.firstOrNull { it.slotIndex == slotIndex }
        if (slot == null || !slot.isActive) {
            if (!silent) emitMessage("SIM ${slotIndex + 1} is not active")
            return
        }
        if (!_permissions.value.hasCoreDialerPermissions) {
            if (!silent) {
                emitMessage("Grant Phone permission to check SIM balance")
                viewModelScope.launch { _permissionPromptTrigger.emit(Unit) }
            }
            return
        }
        if (slotIndex in _refreshingBalanceSlots.value) return

        viewModelScope.launch {
            _refreshingBalanceSlots.value = _refreshingBalanceSlots.value + slotIndex
            try {
                val storedMap = settingsDataStore.simBalancesFlow.first()
                val ussdCode = storedMap[slotIndex]?.ussdCode?.takeIf { it.isNotBlank() }
                    ?: SimBalanceManager.defaultUssdCodeForCarrier(slot.carrierName)

                val result = withContext(Dispatchers.IO) {
                    simBalanceManager.fetchSimBalance(slot, ussdCode)
                }
                result.onSuccess { (shortText, rawText) ->
                    settingsDataStore.saveSimBalanceResult(slotIndex, shortText, rawText)
                    if (!silent) {
                        emitMessage("SIM ${slotIndex + 1}: ${shortText.ifBlank { rawText }}")
                    }
                }.onFailure { err ->
                    if (!silent) {
                        emitMessage(err.message ?: "Unable to fetch SIM ${slotIndex + 1} balance via USSD")
                    }
                }
            } finally {
                _refreshingBalanceSlots.value = _refreshingBalanceSlots.value - slotIndex
            }
        }
    }

    fun updateSimUssdCode(slotIndex: Int, code: String) {
        viewModelScope.launch {
            settingsDataStore.saveSimUssdCode(slotIndex, code)
            emitMessage("SIM ${slotIndex + 1} USSD code updated")
        }
    }

    fun dialSimUssd(slotIndex: Int) {
        val slot = _simSlots.value.firstOrNull { it.slotIndex == slotIndex }
        if (!_permissions.value.hasCoreDialerPermissions) {
            viewModelScope.launch { _permissionPromptTrigger.emit(Unit) }
            return
        }
        viewModelScope.launch {
            val storedMap = settingsDataStore.simBalancesFlow.first()
            val code = storedMap[slotIndex]?.ussdCode?.takeIf { it.isNotBlank() }
                ?: SimBalanceManager.defaultUssdCodeForCarrier(slot?.carrierName.orEmpty())
            val launched = simBalanceManager.dialUssdFallback(slot, code)
            if (!launched) {
                emitMessage("Unable to dial $code")
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun onDialFieldChange(newValue: TextFieldValue) {
        val textToUse = if (settings.value.autoCleanPastedNumber) {
            val cleaned = DualSimCallManager.sanitizeDialNumber(newValue.text)
            if (cleaned != newValue.text) {
                newValue.copy(
                    text = cleaned,
                    selection = TextRange(cleaned.length)
                )
            } else {
                newValue
            }
        } else {
            newValue
        }
        _dialField.value = textToUse
    }

    fun appendDialChar(ch: Char) {
        val current = _dialField.value
        val start = current.selection.min.coerceIn(0, current.text.length)
        val end = current.selection.max.coerceIn(0, current.text.length)
        val updatedText = buildString {
            append(current.text.substring(0, start))
            append(ch)
            append(current.text.substring(end))
        }
        val newCursor = start + 1
        _dialField.value = TextFieldValue(
            text = updatedText,
            selection = TextRange(newCursor)
        )
    }

    fun replaceLastCharWithPlus() {
        val current = _dialField.value
        if (current.text.endsWith("0")) {
            val replaced = current.text.dropLast(1) + "+"
            _dialField.value = TextFieldValue(replaced, TextRange(replaced.length))
        } else {
            appendDialChar('+')
        }
    }

    fun backspaceDialChar() {
        val current = _dialField.value
        if (current.text.isEmpty()) return
        val start = current.selection.min.coerceIn(0, current.text.length)
        val end = current.selection.max.coerceIn(0, current.text.length)

        if (start != end) {
            val updated = current.text.removeRange(start, end)
            _dialField.value = TextFieldValue(updated, TextRange(start))
        } else if (start > 0) {
            val updated = current.text.removeRange(start - 1, start)
            _dialField.value = TextFieldValue(updated, TextRange(start - 1))
        }
    }

    fun clearDialNumber() {
        _dialField.value = TextFieldValue("")
    }

    fun setDialNumber(rawNumber: String) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(rawNumber)
        _dialField.value = TextFieldValue(cleaned, TextRange(cleaned.length))
    }

    fun callCurrentNumberOnSlot(slotIndex: Int) {
        placeCall(_dialField.value.text, slotIndex)
    }

    fun requestAskSimForCurrentNumber() {
        val number = DualSimCallManager.sanitizeDialNumber(_dialField.value.text)
        if (number.isBlank()) {
            emitMessage("Enter or paste a phone number first.")
            return
        }
        openSimPickerForNumber(number)
    }

    fun openSimPickerForNumber(number: String) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number)
        if (cleaned.isBlank()) return

        val activeSlots = _simSlots.value.filter { it.isActive && it.phoneAccountHandle != null }
        when {
            activeSlots.size == 1 -> placeCall(cleaned, activeSlots.first().slotIndex)
            settings.value.dualSimMode == DualSimMode.DUAL_BUTTONS && activeSlots.isNotEmpty() -> {
                placeCall(cleaned, activeSlots.first().slotIndex)
            }
            else -> _simPickerNumber.value = cleaned
        }
    }

    fun dismissSimPicker() {
        _simPickerNumber.value = null
    }

    fun placeCall(number: String, slotIndex: Int) {
        _simPickerNumber.value = null
        val sanitized = DualSimCallManager.sanitizeDialNumber(number)
        if (sanitized.isBlank()) {
            emitMessage("Enter or paste a valid phone number.")
            return
        }

        if (!_permissions.value.hasCoreDialerPermissions) {
            emitMessage("Please grant Phone permissions to make calls.")
            viewModelScope.launch { _permissionPromptTrigger.emit(Unit) }
            return
        }

        val slot = _simSlots.value.firstOrNull { it.slotIndex == slotIndex }
        if (slot == null || !slot.isActive || slot.phoneAccountHandle == null) {
            val msg = if (slotIndex == 1) {
                "SIM 2 is not active or has no call-capable SIM card."
            } else {
                "SIM 1 is not active or has no call-capable SIM card."
            }
            emitMessage(msg)
            return
        }

        val canonical = PhoneNumberInfoHelper.canonicalKey(sanitized)
        val matchedContact = _allContacts.value.firstOrNull { contact ->
            contact.phoneNumbers.any {
                PhoneNumberInfoHelper.canonicalKey(it) == canonical && canonical.isNotBlank()
            }
        }

        CallRepository.setPendingOutgoingCaller(
            number = sanitized,
            callerName = matchedContact?.name,
            photoUri = matchedContact?.photoUri
        )
        val result = dualSimCallManager.placeCall(sanitized, slotIndex)
        when (result) {
            is CallPlacementResult.Success -> Unit
            is CallPlacementResult.SimUnavailable -> emitMessage(result.reason)
            is CallPlacementResult.PermissionRequired -> viewModelScope.launch { _permissionPromptTrigger.emit(Unit) }
            is CallPlacementResult.InvalidNumber -> emitMessage("Enter or paste a valid phone number.")
            is CallPlacementResult.Error -> emitMessage(result.message)
        }
    }

    fun setCallLogFilter(filter: CallLogFilter) {
        _callLogFilter.value = filter
    }

    fun setRecentsSearchQuery(query: String) {
        _recentsSearchQuery.value = query
    }

    fun deleteCallLogItem(id: Long) {
        viewModelScope.launch {
            val deleted = directoryRepository.deleteCallLogEntry(id)
            if (deleted) {
                _allCallLogs.value = _allCallLogs.value.filterNot { it.id == id }
                emitMessage("Call log entry deleted")
            } else {
                emitMessage("Unable to delete entry. Grant Call Log permission.")
            }
        }
    }

    fun deleteMultipleCallLogs(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            var deletedCount = 0
            ids.forEach { id ->
                if (directoryRepository.deleteCallLogEntry(id)) {
                    deletedCount++
                }
            }
            if (deletedCount > 0) {
                _allCallLogs.value = _allCallLogs.value.filterNot { it.id in ids }
                emitMessage("Deleted $deletedCount call history entries")
            } else {
                emitMessage("Unable to delete entries. Grant Call Log permission.")
            }
        }
    }

    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
    }

    fun createSetDefaultDialerIntent(): Intent? = dualSimCallManager.createSetDefaultDialerIntent()

    fun selectTheme(themeId: String) {
        viewModelScope.launch { settingsDataStore.selectTheme(themeId) }
    }

    fun selectDoodle(doodle: DoodleDesign) {
        viewModelScope.launch { settingsDataStore.selectDoodle(doodle) }
    }

    fun saveCustomPhotoTheme(uri: Uri) {
        viewModelScope.launch {
            val savedPath = settingsDataStore.saveCustomPhotoTheme(uri)
            if (savedPath != null) {
                emitMessage("Custom picture theme applied!")
            } else {
                emitMessage("Could not load selected picture.")
            }
        }
    }

    fun setOverlayDimAlpha(alpha: Float) {
        viewModelScope.launch { settingsDataStore.setOverlayDimAlpha(alpha) }
    }

    fun setDtmfTonesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDtmfTonesEnabled(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticFeedbackEnabled(enabled) }
    }

    fun setDualSimMode(mode: DualSimMode) {
        viewModelScope.launch { settingsDataStore.setDualSimMode(mode) }
    }

    fun setAutoCleanPastedNumber(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoCleanPastedNumber(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsDataStore.setLanguage(language) }
    }

    fun setDisplayScale(scale: Float) {
        viewModelScope.launch { settingsDataStore.setDisplayScale(scale) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsDataStore.setFontScale(scale) }
    }

    fun setSimRingtone(slotIndex: Int, uri: Uri?) {
        viewModelScope.launch {
            val savedName = settingsDataStore.setSimRingtone(slotIndex, uri)
            if (savedName != null) {
                emitMessage("SIM ${slotIndex + 1} ringtone set: $savedName")
            }
        }
    }

    fun clearSimRingtone(slotIndex: Int) {
        viewModelScope.launch {
            settingsDataStore.clearSimRingtone(slotIndex)
            emitMessage("SIM ${slotIndex + 1} ringtone reset to default")
        }
    }

    fun blockNumber(number: String, label: String = "") {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number)
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            settingsDataStore.blockNumber(cleaned, label)
            emitMessage("Blocked $cleaned")
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            settingsDataStore.unblockNumber(number)
            emitMessage("Unblocked $number")
        }
    }

    fun saveNumberNote(number: String, note: String) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number)
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            settingsDataStore.saveNumberNote(cleaned, note)
            emitMessage(if (note.isBlank()) "Note cleared" else "Note saved")
        }
    }

    fun openCallerProfile(target: CallerProfileTarget) {
        _rawProfileTarget.value = target
    }

    fun setNumberGroup(number: String, group: FavoriteGroup) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number).ifBlank { number }
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            settingsDataStore.setNumberGroup(cleaned, group)
        }
    }

    fun toggleContactFavorite(contactId: Long?, currentlyStarred: Boolean) {
        if (contactId == null) {
            emitMessage("Save this number as a contact to mark favorite.")
            return
        }
        viewModelScope.launch {
            val updated = directoryRepository.setContactStarred(contactId, !currentlyStarred)
            if (updated) {
                _allContacts.value = directoryRepository.loadContacts()
                emitMessage(if (!currentlyStarred) "Added to Favorites" else "Removed from Favorites")
            } else {
                emitMessage("Grant Contacts write permission to update favorite.")
            }
        }
    }

    fun setCallerIdEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setCallerIdEnabled(enabled) }
    }

    fun setSpamDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSpamDetectionEnabled(enabled) }
    }

    fun setFraudWarningEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setFraudWarningEnabled(enabled) }
    }

    fun setAutoSpamCategoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoSpamCategoryEnabled(enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled) _appUnlockedThisSession.value = true
        viewModelScope.launch { settingsDataStore.setAppLockEnabled(enabled) }
    }

    fun setPinProtectedSettingsEnabled(enabled: Boolean) {
        if (enabled) _settingsUnlocked.value = true
        viewModelScope.launch { settingsDataStore.setPinProtectedSettingsEnabled(enabled) }
    }

    fun setSensitiveContactProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSensitiveContactProtectionEnabled(enabled) }
    }

    fun setSecurityPin(pin: String) {
        viewModelScope.launch {
            settingsDataStore.setSecurityPin(pin)
            _appUnlockedThisSession.value = true
            _settingsUnlocked.value = true
            emitMessage("Security PIN saved")
        }
    }

    fun removeSecurityPin() {
        viewModelScope.launch {
            settingsDataStore.clearSecurityPin()
            _appUnlockedThisSession.value = true
            _settingsUnlocked.value = true
            emitMessage("Security PIN removed")
        }
    }

    suspend fun verifyPin(pin: String): Boolean = settingsDataStore.verifySecurityPin(pin)

    fun unlockApp() {
        _appUnlockedThisSession.value = true
    }

    fun unlockSettings() {
        _settingsUnlocked.value = true
    }

    fun lockSettingsSession() {
        _settingsUnlocked.value = false
    }

    fun reportSpamNumber(
        number: String,
        category: SpamCategory,
        note: String = "",
        alsoBlock: Boolean = false
    ) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number).ifBlank { number.trim() }
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            settingsDataStore.reportSpamNumber(cleaned, category, note)
            if (alsoBlock) {
                settingsDataStore.blockNumber(cleaned, category.labelEn)
            }
            emitMessage("Reported $cleaned (${category.labelEn})")
        }
    }

    fun removeSpamReport(canonicalKey: String) {
        viewModelScope.launch {
            settingsDataStore.removeSpamReport(canonicalKey)
            emitMessage("Report removed")
        }
    }

    fun addOrUpdatePrivateContact(
        name: String,
        number: String,
        note: String = "",
        sensitiveMask: Boolean = true
    ) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number).ifBlank { number.trim() }
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            privacyVaultDataStore.addOrUpdatePrivateContact(name, cleaned, note, sensitiveMask)
            emitMessage("Added to Private Contacts")
        }
    }

    fun togglePrivateContactMask(id: String, masked: Boolean) {
        viewModelScope.launch { privacyVaultDataStore.togglePrivateContactMask(id, masked) }
    }

    fun removePrivateContact(id: String) {
        viewModelScope.launch {
            privacyVaultDataStore.removePrivateContact(id)
            emitMessage("Removed from Private Contacts")
        }
    }

    fun deleteHiddenCall(id: String) {
        viewModelScope.launch { privacyVaultDataStore.deleteHiddenCall(id) }
    }

    fun clearHiddenHistory() {
        viewModelScope.launch {
            privacyVaultDataStore.clearHiddenHistory()
            emitMessage("Hidden call history cleared")
        }
    }

    fun exportEncryptedBackupToUri(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            val result = runCatching {
                val payload = privacyVaultDataStore.exportEncryptedBackup(
                    passphrase = passphrase,
                    blockedNumbers = blockedNumbers.value,
                    numberNotes = numberNotes.value,
                    communityReports = communityReports.value.values
                )
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open backup file for writing")
                }
            }
            if (result.isSuccess) {
                emitMessage("Encrypted backup exported securely")
            } else {
                emitMessage("Backup export failed: ${result.exceptionOrNull()?.message ?: "error"}")
            }
        }
    }

    fun restoreEncryptedBackupFromUri(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            val result = runCatching {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Unable to read backup file")
                }
                privacyVaultDataStore.restoreEncryptedBackup(content, passphrase, settingsDataStore)
                    .getOrThrow()
            }
            result.onSuccess { count ->
                emitMessage("Restored $count item(s) from encrypted backup")
            }.onFailure {
                emitMessage("Restore failed: incorrect passphrase or invalid file")
            }
        }
    }

    fun emitMessage(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }

    companion object {
        fun groupAndFilterCallLogs(
            logs: List<CallLogItem>,
            contacts: List<ContactItem>,
            filter: CallLogFilter,
            searchQuery: String
        ): List<GroupedCallLogItem> {
            val filteredLogs = when (filter) {
                CallLogFilter.ALL -> logs
                CallLogFilter.MISSED -> logs.filter {
                    it.direction == CallDirection.MISSED || it.direction == CallDirection.REJECTED
                }
                CallLogFilter.INCOMING -> logs.filter { it.direction == CallDirection.INCOMING }
                CallLogFilter.OUTGOING -> logs.filter { it.direction == CallDirection.OUTGOING }
                CallLogFilter.SIM1 -> logs.filter { it.simSlotIndex == 0 }
                CallLogFilter.SIM2 -> logs.filter { it.simSlotIndex == 1 }
            }

            val contactLookup = mutableMapOf<String, ContactItem>()
            contacts.forEach { contact ->
                contact.phoneNumbers.forEach { num ->
                    val key = PhoneNumberInfoHelper.canonicalKey(num)
                    if (key.isNotBlank() && key !in contactLookup) {
                        contactLookup[key] = contact
                    }
                }
            }

            val grouped = filteredLogs
                .sortedByDescending { it.dateMillis }
                .groupBy { item ->
                    val key = PhoneNumberInfoHelper.canonicalKey(item.number)
                    key.ifBlank { item.number.ifBlank { "unknown_${item.id}" } }
                }
                .map { (groupKey, entries) ->
                    val latest = entries.first()
                    val matchedContact = contactLookup[groupKey]
                    val resolvedTitle = matchedContact?.name
                        ?: latest.cachedName?.takeIf { it.isNotBlank() }
                        ?: PhoneNumberInfoHelper.formatDisplayNumber(latest.number).ifBlank { "Unknown" }

                    GroupedCallLogItem(
                        groupKey = groupKey,
                        number = latest.number,
                        displayTitle = resolvedTitle,
                        photoUri = matchedContact?.photoUri,
                        contactId = matchedContact?.id,
                        latestDirection = latest.direction,
                        latestDateMillis = latest.dateMillis,
                        latestDurationSeconds = latest.durationSeconds,
                        latestSimSlotIndex = latest.simSlotIndex,
                        entries = entries
                    )
                }
                .sortedByDescending { it.latestDateMillis }

            val trimmed = searchQuery.trim()
            if (trimmed.isEmpty()) return grouped

            val digitQuery = trimmed.filter { it.isDigit() }
            return grouped.filter { group ->
                group.displayTitle.contains(trimmed, ignoreCase = true) ||
                    group.number.contains(trimmed, ignoreCase = true) ||
                    (digitQuery.isNotEmpty() && group.number.filter { it.isDigit() }.contains(digitQuery))
            }
        }
    }
}
