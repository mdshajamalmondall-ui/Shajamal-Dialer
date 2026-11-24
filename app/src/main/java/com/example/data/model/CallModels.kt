package com.example.data.model

import android.telecom.PhoneAccountHandle

data class SimSlotInfo(
    val slotIndex: Int, // 0 = SIM 1, 1 = SIM 2
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?,
    val phoneAccountHandle: PhoneAccountHandle?,
    val isActive: Boolean
) {
    val label: String
        get() = "SIM ${slotIndex + 1}"

    val summaryLabel: String
        get() = if (carrierName.isNotBlank()) "$label · $carrierName" else label
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    VOICEMAIL,
    BLOCKED
}

data class CallLogItem(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val direction: CallDirection,
    val dateMillis: Long,
    val durationSeconds: Long,
    val phoneAccountId: String?,
    val simSlotIndex: Int?
) {
    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
}

data class GroupedCallLogItem(
    val groupKey: String,
    val number: String,
    val displayTitle: String,
    val photoUri: String?,
    val contactId: Long?,
    val latestDirection: CallDirection,
    val latestDateMillis: Long,
    val latestDurationSeconds: Long,
    val latestSimSlotIndex: Int?,
    val entries: List<CallLogItem>
) {
    val callCount: Int
        get() = entries.size

    val allIds: Set<Long>
        get() = entries.map { it.id }.toSet()
}

data class ContactItem(
    val id: Long,
    val name: String,
    val primaryNumber: String,
    val phoneNumbers: List<String>,
    val isStarred: Boolean,
    val photoUri: String?
)

enum class DualSimMode {
    ASK_ON_CALL,
    DUAL_BUTTONS
}

enum class AppLanguage {
    ENGLISH,
    BENGALI
}

enum class DoodleDesign(
    val id: String,
    val titleEn: String,
    val titleBn: String
) {
    NONE("none", "None", "কোনো ডুডল নেই"),
    PHONE_DOODLE("phone_doodle", "Phone Doodle", "ফোন ডুডল"),
    COSMIC_STAR("cosmic_star", "Cosmic Star", "কসমিক স্টার"),
    FLORAL_LEAVES("floral_leaves", "Floral Leaves", "ফ্লোরাল লিভস"),
    GEOMETRIC_HEARTS("geometric_hearts", "Geometric Hearts", "জিওমেট্রিক হার্টস"),
    ROYAL_MANDALA("royal_mandala", "Royal Mandala", "রয়্যাল ম্যান্ডেলা"),
    OCEAN_RIPPLES("ocean_ripples", "Ocean Ripples", "ওশেন রিফলস"),
    CYBER_CIRCUIT("cyber_circuit", "Cyber Circuit", "সাইবার সার্কিট"),
    MUSIC_BEAT("music_beat", "Music Beat", "মিউজিক বিট"),
    BUTTERFLY("butterfly", "Butterfly", "বাটারফ্লাই");

    companion object {
        fun fromId(id: String?): DoodleDesign =
            entries.firstOrNull { it.id == id } ?: COSMIC_STAR
    }
}

data class BlockedNumberEntry(
    val number: String,
    val label: String = "",
    val addedAtMillis: Long = System.currentTimeMillis()
)

data class DialerSettings(
    val selectedThemeId: String = "pure_white",
    val selectedDoodle: DoodleDesign = DoodleDesign.COSMIC_STAR,
    val customThemeImagePath: String? = null,
    val overlayDimAlpha: Float = 0.52f,
    val dtmfTonesEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val dualSimMode: DualSimMode = DualSimMode.ASK_ON_CALL,
    val autoCleanPastedNumber: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val displayScale: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val sim1RingtoneUri: String? = null,
    val sim1RingtoneName: String? = null,
    val sim2RingtoneUri: String? = null,
    val sim2RingtoneName: String? = null,
    val callerIdEnabled: Boolean = true,
    val spamDetectionEnabled: Boolean = true,
    val fraudWarningEnabled: Boolean = true,
    val autoSpamCategoryEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val pinProtectedSettingsEnabled: Boolean = false,
    val sensitiveContactProtectionEnabled: Boolean = false,
    val hasSecurityPin: Boolean = false
)

data class OutgoingAnimationState(
    val number: String,
    val displayName: String?,
    val simLabel: String,
    val carrierName: String,
    val slotIndex: Int,
    val photoUri: String? = null
)

enum class FavoriteGroup(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    FAMILY("family", "Family", "পরিবার"),
    FRIENDS("friends", "Friends", "বন্ধু"),
    OFFICE("office", "Office", "অফিস"),
    NONE("none", "None", "কোনোটি নয়");

    companion object {
        fun fromId(id: String?): FavoriteGroup =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class CallerProfileTarget(
    val number: String,
    val displayTitle: String,
    val photoUri: String?,
    val contactId: Long?,
    val isStarred: Boolean = false
)

data class CallAnalyticsSummary(
    val totalCalls: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val sim1Count: Int,
    val sim2Count: Int,
    val totalDurationSeconds: Long,
    val topContacts: List<Pair<String, Int>>
)

sealed interface CallPlacementResult {
    data class Success(val simLabel: String, val number: String) : CallPlacementResult
    data class SimUnavailable(val slotIndex: Int, val reason: String) : CallPlacementResult
    data object PermissionRequired : CallPlacementResult
    data object InvalidNumber : CallPlacementResult
    data class Error(val message: String) : CallPlacementResult
}

data class PermissionSnapshot(
    val callPhone: Boolean = false,
    val readPhoneState: Boolean = false,
    val readCallLog: Boolean = false,
    val writeCallLog: Boolean = false,
    val readContacts: Boolean = false,
    val writeContacts: Boolean = false,
    val postNotifications: Boolean = true
) {
    val hasCoreDialerPermissions: Boolean
        get() = callPhone && readPhoneState
}
