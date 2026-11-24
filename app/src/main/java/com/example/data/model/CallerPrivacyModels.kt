package com.example.data.model

enum class SpamCategory(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    NONE("none", "General", "সাধারণ"),
    TELEMARKETING("telemarketing", "Telemarketing", "টেলিমার্কেটিং"),
    SCAM_FRAUD("scam_fraud", "Fraud / Scam", "প্রতারণা / স্ক্যাম"),
    ROBOCALL("robocall", "Robocall / Promo", "রোবোকল / প্রোমো"),
    LOAN_DEBT("loan_debt", "Loan / Finance", "লোন / ফিন্যান্স"),
    HARASSMENT("harassment", "Harassment", "বিরক্তিকর কল"),
    VERIFIED_BUSINESS("verified_business", "Verified Business", "ভেরিফায়েড বিজনেস");

    companion object {
        fun fromId(id: String?): SpamCategory =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class CallerTrustStatus {
    VERIFIED_CONTACT,
    VERIFIED_BUSINESS,
    UNKNOWN_CLEAN,
    SPAM_SUSPECTED,
    FRAUD_WARNING,
    BLOCKED,
    SENSITIVE_PROTECTED
}

data class CommunitySpamReport(
    val canonicalKey: String,
    val number: String,
    val category: SpamCategory,
    val note: String = "",
    val reportCount: Int = 1,
    val reportedAtMillis: Long = System.currentTimeMillis()
)

data class CallerIdentityReport(
    val rawNumber: String,
    val formattedNumber: String,
    val displayName: String,
    val status: CallerTrustStatus,
    val category: SpamCategory,
    val statusBadge: String,
    val subStatusLine: String,
    val reputationLine: String,
    val carrierRegion: String,
    val reportCount: Int = 0,
    val isBusinessVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val isSensitiveMasked: Boolean = false,
    val photoUri: String? = null
)

data class PrivateContactEntry(
    val id: String,
    val name: String,
    val number: String,
    val note: String = "",
    val sensitiveMask: Boolean = true,
    val addedAtMillis: Long = System.currentTimeMillis()
)

data class HiddenCallEntry(
    val id: String,
    val number: String,
    val contactName: String?,
    val direction: CallDirection,
    val timestampMillis: Long,
    val durationSeconds: Long,
    val simSlotIndex: Int?
)
