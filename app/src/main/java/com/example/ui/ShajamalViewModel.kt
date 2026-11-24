package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallAnalyticsSummary
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.CallPlacementResult
import com.example.data.model.ContactItem
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.data.model.DualSimMode
import com.example.data.model.GroupedCallLogItem
import com.example.data.model.OutgoingAnimationState
import com.example.data.model.PermissionSnapshot
import com.example.data.model.SimSlotInfo
import com.example.data.preferences.SettingsDataStore
import com.example.data.telephony.DeviceDirectoryRepository
import com.example.data.telephony.DualSimCallManager
import com.example.data.telephony.PhoneNumberInfoHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val dualSimCallManager = DualSimCallManager(context)
    private val directoryRepository = DeviceDirectoryRepository(context, dualSimCallManager)

    val settings: StateFlow<DialerSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DialerSettings()
    )

    val blockedNumbers: StateFlow<List<BlockedNumberEntry>> = settingsDataStore.blockedNumbersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val numberNotes: StateFlow<Map<String, String>> = settingsDataStore.numberNotesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val _dialField = MutableStateFlow(TextFieldValue(""))
    val dialField: StateFlow<TextFieldValue> = _dialField.asStateFlow()

    private val _simSlots = MutableStateFlow<List<SimSlotInfo>>(emptyList())
    val simSlots: StateFlow<List<SimSlotInfo>> = _simSlots.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _permissions = MutableStateFlow(PermissionSnapshot())
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    private val _allCallLogs = MutableStateFlow<List<CallLogItem>>(emptyList())
    private val _callLogFilter = MutableStateFlow(CallLogFilter.ALL)
    val callLogFilter: StateFlow<CallLogFilter> = _callLogFilter.asStateFlow()

    private val _recentsSearchQuery = MutableStateFlow("")
    val recentsSearchQuery: StateFlow<String> = _recentsSearchQuery.asStateFlow()

    private val _allContacts = MutableStateFlow<List<ContactItem>>(emptyList())
    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    val groupedCallLogs: StateFlow<List<GroupedCallLogItem>> = combine(
        _allCallLogs,
        _allContacts,
        _callLogFilter,
        _recentsSearchQuery
    ) { logs, contacts, filter, query ->
        groupAndFilterCallLogs(logs, contacts, filter, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCallCount: StateFlow<Int> = _allCallLogs.map { logs ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        logs.count { it.dateMillis >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CallAnalyticsSummary(0, 0, 0, 0, 0, 0, 0L, emptyList())
    )

    val filteredContacts: StateFlow<List<ContactItem>> = combine(
        _allContacts,
        _contactSearchQuery
    ) { contacts, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(trimmed, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.contains(trimmed, ignoreCase = true) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<ContactItem>> = _allContacts.map { contacts ->
        contacts.filter { it.isStarred }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _simPickerNumber = MutableStateFlow<String?>(null)
    val simPickerNumber: StateFlow<String?> = _simPickerNumber.asStateFlow()

    private val _outgoingAnimation = MutableStateFlow<OutgoingAnimationState?>(null)
    val outgoingAnimation: StateFlow<OutgoingAnimationState?> = _outgoingAnimation.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _permissionPromptTrigger = MutableSharedFlow<Unit>()
    val permissionPromptTrigger: SharedFlow<Unit> = _permissionPromptTrigger.asSharedFlow()

    init {
        refreshDeviceState()
    }

    fun refreshDeviceState() {
        val snapshot = PermissionSnapshot(
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
            }
        )
        _permissions.value = snapshot
        _isDefaultDialer.value = dualSimCallManager.isDefaultDialer()

        val slots = dualSimCallManager.loadSimSlots()
        _simSlots.value = slots

        viewModelScope.launch {
            _allCallLogs.value = directoryRepository.loadCallLogs(slots, limit = 400)
            _allContacts.value = directoryRepository.loadContacts()
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
        _simPickerNumber.value = number
    }

    fun openSimPickerForNumber(number: String) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(number)
        if (cleaned.isNotBlank()) {
            _simPickerNumber.value = cleaned
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

        viewModelScope.launch {
            _outgoingAnimation.value = OutgoingAnimationState(
                number = sanitized,
                displayName = matchedContact?.name,
                simLabel = slot.label,
                carrierName = slot.carrierName,
                slotIndex = slotIndex
            )
            val result = dualSimCallManager.placeCall(sanitized, slotIndex)
            delay(500L)
            _outgoingAnimation.value = null

            when (result) {
                is CallPlacementResult.Success -> Unit
                is CallPlacementResult.SimUnavailable -> emitMessage(result.reason)
                is CallPlacementResult.PermissionRequired -> _permissionPromptTrigger.emit(Unit)
                is CallPlacementResult.InvalidNumber -> emitMessage("Enter or paste a valid phone number.")
                is CallPlacementResult.Error -> emitMessage(result.message)
            }
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
