package com.example.data.telephony

object PhoneNumberInfoHelper {

    fun canonicalKey(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun formatDisplayNumber(rawNumber: String): String {
        val cleaned = DualSimCallManager.sanitizeDialNumber(rawNumber)
        if (cleaned.startsWith("01") && cleaned.length == 11) {
            return "${cleaned.substring(0, 5)}-${cleaned.substring(5)}"
        }
        return cleaned
    }

    fun detectCarrierAndRegion(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        // Normalize Bangladesh prefix (+8801... or 8801... or 01...)
        val bdLocal = when {
            digits.startsWith("8801") && digits.length >= 6 -> "0" + digits.substring(3)
            digits.startsWith("01") -> digits
            else -> null
        }

        if (bdLocal != null && bdLocal.length >= 3) {
            val carrier = when (bdLocal.substring(0, 3)) {
                "017", "013" -> "Grameenphone"
                "018" -> "Robi"
                "016" -> "Airtel"
                "019", "014" -> "Banglalink"
                "015" -> "Teletalk"
                else -> "Mobile"
            }
            return "$carrier · Bangladesh"
        }

        return when {
            rawNumber.startsWith("+880") -> "Bangladesh"
            rawNumber.startsWith("+91") -> "India"
            rawNumber.startsWith("+1") -> "USA / Canada"
            rawNumber.startsWith("+44") -> "United Kingdom"
            rawNumber.startsWith("+966") -> "Saudi Arabia"
            rawNumber.startsWith("+971") -> "UAE"
            rawNumber.startsWith("+60") -> "Malaysia"
            rawNumber.startsWith("+65") -> "Singapore"
            rawNumber.startsWith("+974") -> "Qatar"
            rawNumber.startsWith("+965") -> "Kuwait"
            rawNumber.startsWith("+968") -> "Oman"
            else -> "Mobile Network"
        }
    }

    fun buildWhatsAppUrl(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        val international = when {
            digits.startsWith("01") && digits.length == 11 -> "88$digits"
            else -> digits
        }
        return "https://wa.me/$international"
    }
}
