package com.example.data.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.model.SimSlotInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class StoredSimBalance(
    val ussdCode: String = "",
    val balanceShort: String = "",
    val rawResponse: String = "",
    val updatedAtMillis: Long = 0L
)

class SimBalanceManager(private val context: Context) {

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    @SuppressLint("MissingPermission")
    suspend fun fetchSimBalance(
        slot: SimSlotInfo,
        ussdCode: String
    ): Result<Pair<String, String>> {
        val cleanCode = ussdCode.trim().ifBlank { defaultUssdCodeForCarrier(slot.carrierName) }
        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCallPhone) {
            return Result.failure(IllegalStateException("CALL_PHONE permission required"))
        }

        val baseTm = telephonyManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || baseTm == null) {
            return Result.failure(UnsupportedOperationException("Use Dial USSD on Android 7 or below"))
        }

        val scopedTm = if (slot.subscriptionId > 0) {
            runCatching { baseTm.createForSubscriptionId(slot.subscriptionId) }.getOrNull()
                ?: baseTm
        } else {
            baseTm
        }

        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager,
                    request: String,
                    response: CharSequence
                ) {
                    if (!cont.isActive) return
                    val raw = response.toString().trim()
                    val short = extractBalanceShort(raw)
                    cont.resume(Result.success(short to raw))
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager,
                    request: String,
                    failureCode: Int
                ) {
                    if (!cont.isActive) return
                    cont.resume(
                        Result.failure(
                            IllegalStateException("USSD request failed (code $failureCode)")
                        )
                    )
                }
            }

            runCatching {
                scopedTm.sendUssdRequest(cleanCode, callback, handler)
            }.onFailure { err ->
                if (cont.isActive) {
                    cont.resume(Result.failure(err))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun dialUssdFallback(slot: SimSlotInfo?, ussdCode: String): Boolean {
        val cleanCode = ussdCode.trim().ifBlank {
            defaultUssdCodeForCarrier(slot?.carrierName.orEmpty())
        }
        val uri = Uri.parse("tel:${Uri.encode(cleanCode)}")
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            if (slot?.phoneAccountHandle != null) {
                putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, slot.phoneAccountHandle)
            }
            if (slot != null && slot.subscriptionId > 0) {
                putExtra("subscription", slot.subscriptionId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    companion object {
        private val currencyPrefixRegex = Regex(
            """(?i)(?:৳|tk\.?|bdt|\$|usd|eur|€|₹|rs\.?|inr)\s*([0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        private val currencySuffixRegex = Regex(
            """(?i)([0-9]+(?:[.,][0-9]{1,2})?)\s*(?:tk\.?|taka|bdt|৳|টাকা|usd|\$|inr|rs\.?)"""
        )
        private val balanceKeywordRegex = Regex(
            """(?i)(?:balance|bal|main\s*account|ব্যালেন্স)\s*(?:is|:|=)?\s*(?:৳|tk\.?|bdt|\$)?\s*([0-9]+(?:[.,][0-9]{1,2})?)"""
        )

        fun defaultUssdCodeForCarrier(carrierName: String): String {
            val lower = carrierName.lowercase(Locale.ROOT)
            return when {
                lower.contains("grameen") || lower.contains("gp") -> "*566#"
                lower.contains("banglalink") || lower.contains("ryze") || lower.contains("bl") -> "*124#"
                lower.contains("robi") -> "*222#"
                lower.contains("airtel") -> "*778#"
                lower.contains("teletalk") -> "*152#"
                else -> "*124#"
            }
        }

        fun extractBalanceShort(rawResponse: String): String {
            val trimmed = rawResponse.trim()
            if (trimmed.isBlank()) return ""

            currencyPrefixRegex.find(trimmed)?.let { match ->
                val rawToken = match.value.lowercase(Locale.ROOT)
                val amount = match.groupValues[1].replace(",", ".")
                val symbol = when {
                    rawToken.contains("$") || rawToken.contains("usd") -> "$"
                    rawToken.contains("€") || rawToken.contains("eur") -> "€"
                    rawToken.contains("₹") || rawToken.contains("rs") || rawToken.contains("inr") -> "₹"
                    else -> "৳"
                }
                return "$symbol$amount"
            }

            currencySuffixRegex.find(trimmed)?.let { match ->
                val rawToken = match.value.lowercase(Locale.ROOT)
                val amount = match.groupValues[1].replace(",", ".")
                val symbol = when {
                    rawToken.contains("$") || rawToken.contains("usd") -> "$"
                    rawToken.contains("rs") || rawToken.contains("inr") -> "₹"
                    else -> "৳"
                }
                return "$symbol$amount"
            }

            balanceKeywordRegex.find(trimmed)?.let { match ->
                val amount = match.groupValues[1].replace(",", ".")
                return "৳$amount"
            }

            return trimmed.take(12)
        }
    }
}
