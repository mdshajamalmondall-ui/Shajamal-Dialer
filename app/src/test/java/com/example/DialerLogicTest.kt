package com.example

import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.CallerTrustStatus
import com.example.data.model.CommunitySpamReport
import com.example.data.model.ContactItem
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.data.model.PrivateContactEntry
import com.example.data.model.SpamCategory
import com.example.data.telephony.CallerIntelligenceEngine
import com.example.data.telephony.DeviceDirectoryRepository
import com.example.data.telephony.DualSimCallManager
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.CallLogFilter
import com.example.ui.ShajamalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialerLogicTest {

    @Test
    fun `sanitizeDialNumber strips spaces dashes and letters while preserving plus and USSD symbols`() {
        assertEquals("+8801712345678", DualSimCallManager.sanitizeDialNumber("+880 1712-345678"))
        assertEquals("*121#", DualSimCallManager.sanitizeDialNumber("Call *121# now"))
        assertEquals("01811223344", DualSimCallManager.sanitizeDialNumber("(018) 11-22-33-44"))
    }

    @Test
    fun `nameToT9Digits converts contact names accurately`() {
        assertEquals("74252625", DeviceDirectoryRepository.nameToT9Digits("Shajamal"))
    }

    @Test
    fun `matchesDialQuery matches both T9 name and phone digits`() {
        val contact = ContactItem(
            id = 1L,
            name = "Shajamal Islam",
            primaryNumber = "+8801711223344",
            phoneNumbers = listOf("+8801711223344"),
            isStarred = true,
            photoUri = null
        )

        assertTrue(DeviceDirectoryRepository.matchesDialQuery(contact, "7425"))
        assertTrue(DeviceDirectoryRepository.matchesDialQuery(contact, "01711"))
        assertFalse(DeviceDirectoryRepository.matchesDialQuery(contact, "99999"))
    }

    @Test
    fun `phoneNumberInfoHelper detects Bangladeshi carriers and canonical keys`() {
        assertEquals("Grameenphone · Bangladesh", PhoneNumberInfoHelper.detectCarrierAndRegion("01712345678"))
        assertEquals("Robi · Bangladesh", PhoneNumberInfoHelper.detectCarrierAndRegion("+8801812345678"))
        assertEquals("Banglalink · Bangladesh", PhoneNumberInfoHelper.detectCarrierAndRegion("01912345678"))
        assertEquals("1712345678", PhoneNumberInfoHelper.canonicalKey("+880 1712-345678"))
        assertEquals("01712-345678", PhoneNumberInfoHelper.formatDisplayNumber("01712345678"))
        assertEquals("https://wa.me/8801712345678", PhoneNumberInfoHelper.buildWhatsAppUrl("01712345678"))
    }

    @Test
    fun `doodleDesign resolves all 9 requested designs and defaults safely`() {
        assertEquals(10, DoodleDesign.entries.size)
        assertEquals(DoodleDesign.PHONE_DOODLE, DoodleDesign.fromId("phone_doodle"))
        assertEquals(DoodleDesign.ROYAL_MANDALA, DoodleDesign.fromId("royal_mandala"))
        assertEquals(DoodleDesign.BUTTERFLY, DoodleDesign.fromId("butterfly"))
        assertEquals(DoodleDesign.COSMIC_STAR, DoodleDesign.fromId("unknown_id"))
    }

    @Test
    fun `groupAndFilterCallLogs merges repeated calls to the same number and counts them`() {
        val contacts = listOf(
            ContactItem(
                id = 10L,
                name = "Mohtarma",
                primaryNumber = "+8801988604043",
                phoneNumbers = listOf("+8801988604043"),
                isStarred = true,
                photoUri = null
            )
        )
        val logs = listOf(
            CallLogItem(1L, "01988604043", null, CallDirection.OUTGOING, 3000L, 12L, "acc1", 0),
            CallLogItem(2L, "+8801988604043", "Mohtarma", CallDirection.OUTGOING, 2000L, 0L, "acc1", 0),
            CallLogItem(3L, "01711000000", null, CallDirection.MISSED, 2500L, 0L, "acc2", 1),
            CallLogItem(4L, "01988-604043", null, CallDirection.INCOMING, 1000L, 45L, "acc2", 1)
        )

        val allGroups = ShajamalViewModel.groupAndFilterCallLogs(logs, contacts, CallLogFilter.ALL, "")
        assertEquals(2, allGroups.size)
        assertEquals("Mohtarma", allGroups[0].displayTitle)
        assertEquals(3, allGroups[0].callCount)
        assertEquals(setOf(1L, 2L, 4L), allGroups[0].allIds)

        val missedGroups = ShajamalViewModel.groupAndFilterCallLogs(logs, contacts, CallLogFilter.MISSED, "")
        assertEquals(1, missedGroups.size)
        assertEquals("01711-000000", missedGroups[0].displayTitle)

        val searchedGroups = ShajamalViewModel.groupAndFilterCallLogs(logs, contacts, CallLogFilter.ALL, "mohtar")
        assertEquals(1, searchedGroups.size)
        assertEquals(3, searchedGroups[0].callCount)
    }

    @Test
    fun `dialerSettings defaults to 100 percent display and font scale`() {
        val defaults = DialerSettings()
        assertEquals(1.0f, defaults.displayScale, 0.001f)
        assertEquals(1.0f, defaults.fontScale, 0.001f)
    }

    @Test
    fun `callerIntelligenceEngine identifies verified business and unknown callers`() {
        val settings = DialerSettings()
        val biz = CallerIntelligenceEngine.evaluate(
            rawNumber = "16247",
            contactName = null,
            photoUri = null,
            note = null,
            blockedNumbers = emptyList(),
            communityReports = emptyMap(),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.VERIFIED_BUSINESS, biz.status)
        assertEquals("bKash Customer Care", biz.displayName)
        assertTrue(biz.isBusinessVerified)

        val unknown = CallerIntelligenceEngine.evaluate(
            rawNumber = "01712345678",
            contactName = null,
            photoUri = null,
            note = null,
            blockedNumbers = emptyList(),
            communityReports = emptyMap(),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.UNKNOWN_CLEAN, unknown.status)
        assertEquals("Unknown Caller", unknown.displayName)
        assertTrue(unknown.carrierRegion.contains("Grameenphone"))
    }

    @Test
    fun `callerIntelligenceEngine flags fraud warnings, auto spam categories, and blocked database`() {
        val settings = DialerSettings()

        val fraud = CallerIntelligenceEngine.evaluate(
            rawNumber = "+22499887766",
            contactName = null,
            photoUri = null,
            note = null,
            blockedNumbers = emptyList(),
            communityReports = emptyMap(),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.FRAUD_WARNING, fraud.status)
        assertEquals(SpamCategory.SCAM_FRAUD, fraud.category)

        val autoLoan = CallerIntelligenceEngine.evaluate(
            rawNumber = "01811223344",
            contactName = "Quick Loan Desk",
            photoUri = null,
            note = null,
            blockedNumbers = emptyList(),
            communityReports = emptyMap(),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.SPAM_SUSPECTED, autoLoan.status)
        assertEquals(SpamCategory.LOAN_DEBT, autoLoan.category)

        val communityReported = CallerIntelligenceEngine.evaluate(
            rawNumber = "01955443322",
            contactName = null,
            photoUri = null,
            note = null,
            blockedNumbers = emptyList(),
            communityReports = mapOf(
                "1955443322" to CommunitySpamReport(
                    canonicalKey = "1955443322",
                    number = "01955443322",
                    category = SpamCategory.ROBOCALL,
                    note = "Automated voice",
                    reportCount = 4
                )
            ),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.SPAM_SUSPECTED, communityReported.status)
        assertEquals(4, communityReported.reportCount)

        val blocked = CallerIntelligenceEngine.evaluate(
            rawNumber = "01611002233",
            contactName = null,
            photoUri = null,
            note = null,
            blockedNumbers = listOf(BlockedNumberEntry("01611002233", "Spammer", 100L)),
            communityReports = emptyMap(),
            privateContacts = emptyList(),
            settings = settings
        )
        assertEquals(CallerTrustStatus.BLOCKED, blocked.status)
    }

    @Test
    fun `callerIntelligenceEngine masks sensitive private contacts when protected`() {
        val settings = DialerSettings(sensitiveContactProtectionEnabled = true)
        val privates = listOf(
            PrivateContactEntry(
                id = "p1",
                name = "Secret Advisor",
                number = "01712345678",
                sensitiveMask = true
            )
        )
        val masked = CallerIntelligenceEngine.evaluate(
            rawNumber = "+8801712345678",
            contactName = "Secret Advisor",
            photoUri = "content://photo/1",
            note = "Private note",
            blockedNumbers = emptyList(),
            communityReports = emptyMap(),
            privateContacts = privates,
            settings = settings,
            unlockedPrivateSession = false
        )
        assertEquals(CallerTrustStatus.SENSITIVE_PROTECTED, masked.status)
        assertTrue(masked.isSensitiveMasked)
        assertTrue(masked.formattedNumber.contains("••••"))
        assertNull(masked.photoUri)
        assertFalse(masked.displayName.contains("Secret"))
    }
}
