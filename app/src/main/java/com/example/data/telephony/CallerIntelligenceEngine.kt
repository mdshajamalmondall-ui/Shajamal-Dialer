package com.example.data.telephony

import com.example.data.model.AppLanguage
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallerIdentityReport
import com.example.data.model.CallerTrustStatus
import com.example.data.model.CommunitySpamReport
import com.example.data.model.DialerSettings
import com.example.data.model.PrivateContactEntry
import com.example.data.model.SpamCategory

object CallerIntelligenceEngine {

    private data class VerifiedBusinessEntry(
        val name: String,
        val department: String
    )

    private val verifiedBusinessesByKey = mapOf(
        "999" to VerifiedBusinessEntry("National Emergency Service", "Government Official"),
        "16247" to VerifiedBusinessEntry("bKash Customer Care", "Verified Financial Service"),
        "16216" to VerifiedBusinessEntry("DBBL / Rocket Helpline", "Verified Banking Service"),
        "16167" to VerifiedBusinessEntry("Nagad Support Center", "Verified Financial Service"),
        "16263" to VerifiedBusinessEntry("Shastho Batayon (Health)", "Verified Health Hotline"),
        "121" to VerifiedBusinessEntry("Mobile Operator Care", "Verified Telecom Support"),
        "16256" to VerifiedBusinessEntry("City Bank Contact Center", "Verified Banking Service"),
        "16221" to VerifiedBusinessEntry("Islami Bank Care", "Verified Banking Service"),
        "16236" to VerifiedBusinessEntry("BRAC Bank Care", "Verified Banking Service"),
        "106" to VerifiedBusinessEntry("ACC Anti-Corruption Hotline", "Government Official"),
        "109" to VerifiedBusinessEntry("Women & Children Helpline", "Government Official")
    )

    private val highRiskFraudPrefixes = listOf("+224", "+252", "+257", "+263", "+881", "+882", "+375", "+371")

    private val businessKeywords = listOf(
        "ltd", "limited", "inc", "corp", "bank", "delivery", "courier",
        "hospital", "clinic", "pharmacy", "office", "agency", "enterprise",
        "store", "shop", "care", "support", "service", "official", "company"
    )

    private val fraudKeywords = listOf(
        "scam", "fraud", "phish", "fake", "otp", "lottery", "prize", "won", "hack", "প্রতার", "লটারি"
    )

    private val telemarketingKeywords = listOf(
        "promo", "offer", "sales", "marketing", "agent", "telemarket", "sim offer", "অফার", "বিজ্ঞাপন"
    )

    private val loanKeywords = listOf(
        "loan", "emi", "credit", "debt", "collection", "finance", "installment", "লোন", "কিস্তি"
    )

    private val robocallKeywords = listOf(
        "robo", "auto", "ivr", "recorded", "dialer", "spam"
    )

    fun classifyAutomaticCategory(
        rawNumber: String,
        hintText: String?,
        existingReport: CommunitySpamReport?
    ): SpamCategory {
        if (existingReport != null && existingReport.category != SpamCategory.NONE) {
            return existingReport.category
        }
        val combined = listOfNotNull(hintText).joinToString(" ").lowercase()
        return when {
            fraudKeywords.any { combined.contains(it) } ||
                highRiskFraudPrefixes.any { rawNumber.trim().startsWith(it) } -> SpamCategory.SCAM_FRAUD
            loanKeywords.any { combined.contains(it) } -> SpamCategory.LOAN_DEBT
            telemarketingKeywords.any { combined.contains(it) } -> SpamCategory.TELEMARKETING
            robocallKeywords.any { combined.contains(it) } -> SpamCategory.ROBOCALL
            businessKeywords.any { combined.contains(it) } -> SpamCategory.VERIFIED_BUSINESS
            else -> SpamCategory.NONE
        }
    }

    fun evaluate(
        rawNumber: String,
        contactName: String?,
        photoUri: String?,
        note: String?,
        blockedNumbers: List<BlockedNumberEntry>,
        communityReports: Map<String, CommunitySpamReport>,
        privateContacts: List<PrivateContactEntry>,
        settings: DialerSettings,
        unlockedPrivateSession: Boolean = false
    ): CallerIdentityReport {
        val isBn = settings.language == AppLanguage.BENGALI
        val sanitized = DualSimCallManager.sanitizeDialNumber(rawNumber).ifBlank { rawNumber.trim() }
        val canonical = PhoneNumberInfoHelper.canonicalKey(sanitized)
        val formatted = PhoneNumberInfoHelper.formatDisplayNumber(sanitized).ifBlank { sanitized }
        val carrierRegion = PhoneNumberInfoHelper.detectCarrierAndRegion(sanitized)

        val privateEntry = privateContacts.firstOrNull {
            val pk = PhoneNumberInfoHelper.canonicalKey(it.number)
            (pk.isNotBlank() && pk == canonical) || it.number.trim() == sanitized
        }

        val isSensitiveMasked = privateEntry != null &&
            (privateEntry.sensitiveMask || settings.sensitiveContactProtectionEnabled) &&
            !unlockedPrivateSession

        val blockedEntry = blockedNumbers.firstOrNull {
            val bk = PhoneNumberInfoHelper.canonicalKey(it.number)
            (bk.isNotBlank() && bk == canonical) || it.number.trim() == sanitized
        }
        val report = communityReports[canonical]
            ?: communityReports[sanitized]
        val verifiedBusiness = verifiedBusinessesByKey[sanitized]
            ?: verifiedBusinessesByKey[canonical]

        val resolvedRawName = privateEntry?.name?.takeIf { it.isNotBlank() }
            ?: contactName?.takeIf { it.isNotBlank() }
            ?: verifiedBusiness?.name

        val autoCategory = if (settings.autoSpamCategoryEnabled) {
            classifyAutomaticCategory(sanitized, listOfNotNull(resolvedRawName, note, blockedEntry?.label, report?.note).joinToString(" "), report)
        } else {
            report?.category ?: SpamCategory.NONE
        }

        // Sensitive protection masking takes top priority over exposing private names/numbers publicly
        if (isSensitiveMasked) {
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = maskPhoneNumber(formatted),
                displayName = if (isBn) "গোপন কন্টাক্ট" else "Protected Contact",
                status = CallerTrustStatus.SENSITIVE_PROTECTED,
                category = SpamCategory.NONE,
                statusBadge = if (isBn) "প্রাইভেট শিল্ড" else "Protected",
                subStatusLine = if (isBn) "সেন্সিটিভ কন্টাক্ট প্রোটেকশন চালু আছে" else "Sensitive Contact Protection active",
                reputationLine = if (isBn) "ভল্টে নিরাপদ সংরক্ষিত" else "Secured in Private Vault",
                carrierRegion = carrierRegion,
                isPrivate = true,
                isSensitiveMasked = true,
                photoUri = null
            )
        }

        // Blocked Database Check
        if (blockedEntry != null) {
            val cat = if (autoCategory != SpamCategory.NONE) autoCategory else SpamCategory.HARASSMENT
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = formatted,
                displayName = resolvedRawName ?: if (isBn) "ব্লকড নাম্বার" else "Blocked Number",
                status = CallerTrustStatus.BLOCKED,
                category = cat,
                statusBadge = if (isBn) "ব্লকড ডাটাবেস" else "Blocked",
                subStatusLine = if (isBn) "আপনার ব্লক লিস্টে আছে" else "In Blocked Number Database",
                reputationLine = blockedEntry.label.ifBlank {
                    if (isBn) "স্বয়ংক্রিয়ভাবে কল প্রত্যাখ্যান করা হবে" else "Blocked from reaching you"
                },
                carrierRegion = carrierRegion,
                reportCount = report?.reportCount ?: 1,
                isPrivate = privateEntry != null,
                photoUri = photoUri
            )
        }

        // Fraud Warning Check (#3)
        val isFraudPrefix = settings.fraudWarningEnabled &&
            highRiskFraudPrefixes.any { sanitized.startsWith(it) }
        val isFraudReported = settings.fraudWarningEnabled &&
            (report?.category == SpamCategory.SCAM_FRAUD || autoCategory == SpamCategory.SCAM_FRAUD)
        if (isFraudPrefix || isFraudReported) {
            val reportsCount = (report?.reportCount ?: 1).coerceAtLeast(1)
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = formatted,
                displayName = resolvedRawName ?: if (isBn) "সন্দেহজনক ফ্রড কল" else "Suspected Fraud Caller",
                status = CallerTrustStatus.FRAUD_WARNING,
                category = SpamCategory.SCAM_FRAUD,
                statusBadge = if (isBn) "ফ্রড সতর্কতা" else "Fraud Warning",
                subStatusLine = if (isBn) "সতর্ক থাকুন • আর্থিক প্রতারণার ঝুঁকি" else "High Risk • Do not share OTP or PIN",
                reputationLine = if (isBn) {
                    "কমিউনিটি রিপোর্ট: $reportsCount টি স্ক্যাম/ফ্রড সতর্কতা"
                } else {
                    "Fraud warning • $reportsCount community report(s)"
                },
                carrierRegion = carrierRegion,
                reportCount = reportsCount,
                isPrivate = privateEntry != null,
                photoUri = photoUri
            )
        }

        // Spam Call Detection (#2)
        val isSpamReported = settings.spamDetectionEnabled && report != null &&
            report.category != SpamCategory.NONE &&
            report.category != SpamCategory.VERIFIED_BUSINESS
        val isAutoSpam = settings.spamDetectionEnabled &&
            settings.autoSpamCategoryEnabled &&
            autoCategory in setOf(SpamCategory.TELEMARKETING, SpamCategory.LOAN_DEBT, SpamCategory.ROBOCALL, SpamCategory.HARASSMENT)
        if (isSpamReported || isAutoSpam) {
            val cat = report?.category ?: autoCategory
            val catLabel = if (isBn) cat.labelBn else cat.labelEn
            val count = report?.reportCount ?: 1
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = formatted,
                displayName = resolvedRawName ?: formatted,
                status = CallerTrustStatus.SPAM_SUSPECTED,
                category = cat,
                statusBadge = if (isBn) "স্প্যাম: $catLabel" else "Spam • $catLabel",
                subStatusLine = if (isBn) "স্বয়ংক্রিয় স্প্যাম ক্যাটাগরি: $catLabel" else "Category: $catLabel",
                reputationLine = if (isBn) {
                    "$count জন ব্যবহারকারী স্প্যাম হিসেবে রিপোর্ট করেছেন"
                } else {
                    "$count community spam report(s)"
                },
                carrierRegion = carrierRegion,
                reportCount = count,
                isPrivate = privateEntry != null,
                photoUri = photoUri
            )
        }

        // Business Caller Identification (#4)
        val isCommunityBusiness = report?.category == SpamCategory.VERIFIED_BUSINESS
        val isKeywordBusiness = resolvedRawName != null &&
            businessKeywords.any { resolvedRawName.lowercase().contains(it) }
        if (verifiedBusiness != null || isCommunityBusiness || isKeywordBusiness) {
            val bizName = resolvedRawName ?: report?.note?.takeIf { it.isNotBlank() } ?: formatted
            val subText = when {
                verifiedBusiness != null -> if (isBn) "বিজনেস পরিচয় ভেরিফায়েড" else "Business identity verified"
                isCommunityBusiness -> if (isBn) "কমিউনিটি ভেরিফায়েড বিজনেস" else "Business identity verified"
                else -> if (isBn) "বিজনেস কন্টাক্ট শনাক্ত হয়েছে" else "Business caller identified"
            }
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = formatted,
                displayName = bizName,
                status = CallerTrustStatus.VERIFIED_BUSINESS,
                category = SpamCategory.VERIFIED_BUSINESS,
                statusBadge = if (isBn) "ভেরিফায়েড বিজনেস" else "Verified Business",
                subStatusLine = subText,
                reputationLine = if (isBn) "কোনো স্প্যাম রিপোর্ট নেই" else "No known spam reports",
                carrierRegion = carrierRegion,
                isBusinessVerified = true,
                isPrivate = privateEntry != null,
                photoUri = photoUri
            )
        }

        // Saved / Personal Verified Contact (#1)
        if (!resolvedRawName.isNullOrBlank()) {
            return CallerIdentityReport(
                rawNumber = sanitized,
                formattedNumber = formatted,
                displayName = resolvedRawName,
                status = CallerTrustStatus.VERIFIED_CONTACT,
                category = SpamCategory.NONE,
                statusBadge = if (privateEntry != null) {
                    if (isBn) "প্রাইভেট কন্টাক্ট" else "Private Contact"
                } else {
                    if (isBn) "সেভড কন্টাক্ট" else "Saved Contact"
                },
                subStatusLine = carrierRegion,
                reputationLine = if (isBn) "কোনো স্প্যাম রিপোর্ট নেই" else "No known spam reports",
                carrierRegion = carrierRegion,
                isPrivate = privateEntry != null,
                photoUri = photoUri
            )
        }

        // Unknown Caller Information (#5)
        return CallerIdentityReport(
            rawNumber = sanitized,
            formattedNumber = formatted,
            displayName = if (isBn) "অজানা কলার" else "Unknown Caller",
            status = CallerTrustStatus.UNKNOWN_CLEAN,
            category = SpamCategory.NONE,
            statusBadge = if (isBn) "অজানা নাম্বার" else "Unknown Caller",
            subStatusLine = carrierRegion,
            reputationLine = if (isBn) "কোনো স্প্যাম রিপোর্ট পাওয়া যায়নি" else "No known spam reports",
            carrierRegion = carrierRegion,
            isPrivate = false,
            photoUri = photoUri
        )
    }

    fun maskPhoneNumber(formatted: String): String {
        val digits = formatted.filter { it.isDigit() }
        if (digits.length <= 4) return "••••"
        val prefix = formatted.take(5)
        val suffix = digits.takeLast(2)
        return "$prefix ••••$suffix"
    }
}
