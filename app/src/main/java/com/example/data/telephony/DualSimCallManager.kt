package com.example.data.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.model.CallPlacementResult
import com.example.data.model.SimSlotInfo

class DualSimCallManager(private val context: Context) {

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private val subscriptionManager: SubscriptionManager? =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    fun isDefaultDialer(): Boolean {
        return telecomManager?.defaultDialerPackage == context.packageName
    }

    fun createSetDefaultDialerIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    @SuppressLint("MissingPermission")
    fun loadSimSlots(): List<SimSlotInfo> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneState) {
            return listOf(
                SimSlotInfo(
                    slotIndex = 0,
                    subscriptionId = -1,
                    displayName = "SIM 1",
                    carrierName = "Permission needed",
                    phoneNumber = null,
                    phoneAccountHandle = null,
                    isActive = false
                ),
                SimSlotInfo(
                    slotIndex = 1,
                    subscriptionId = -1,
                    displayName = "SIM 2",
                    carrierName = "Permission needed",
                    phoneNumber = null,
                    phoneAccountHandle = null,
                    isActive = false
                )
            )
        }

        val activeSubs: List<SubscriptionInfo> = runCatching {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
                .sortedBy { it.simSlotIndex }
        }.getOrDefault(emptyList())

        val callAccounts: List<PhoneAccountHandle> = runCatching {
            telecomManager?.callCapablePhoneAccounts.orEmpty()
        }.getOrDefault(emptyList())

        val handleBySlotIndex = mapAccountsToSlots(callAccounts, activeSubs)

        val slots = mutableListOf<SimSlotInfo>()
        for (slotIndex in 0..1) {
            val subInfo = activeSubs.firstOrNull { it.simSlotIndex == slotIndex }
            val handle = handleBySlotIndex[slotIndex]
            val phoneAccount = handle?.let { runCatching { telecomManager?.getPhoneAccount(it) }.getOrNull() }

            val carrier = subInfo?.carrierName?.toString()?.takeIf { it.isNotBlank() }
                ?: subInfo?.displayName?.toString()?.takeIf { it.isNotBlank() }
                ?: phoneAccount?.label?.toString()?.takeIf { it.isNotBlank() }
                ?: ""

            val display = subInfo?.displayName?.toString()?.takeIf { it.isNotBlank() }
                ?: "SIM ${slotIndex + 1}"

            val number = getSubscriptionPhoneNumber(subInfo)

            val isActive = subInfo != null || handle != null

            slots.add(
                SimSlotInfo(
                    slotIndex = slotIndex,
                    subscriptionId = subInfo?.subscriptionId ?: -1,
                    displayName = display,
                    carrierName = carrier.ifBlank { if (isActive) "Active" else "No SIM" },
                    phoneNumber = number,
                    phoneAccountHandle = handle,
                    isActive = isActive
                )
            )
        }
        return slots
    }

    @SuppressLint("MissingPermission")
    private fun getSubscriptionPhoneNumber(subInfo: SubscriptionInfo?): String? {
        if (subInfo == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPhoneNumbers = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPhoneNumbers) {
                val num = runCatching { subscriptionManager?.getPhoneNumber(subInfo.subscriptionId) }.getOrNull()
                if (!num.isNullOrBlank()) return num
            }
        }
        @Suppress("DEPRECATION")
        return subInfo.number?.takeIf { it.isNotBlank() }
    }

    /**
     * Deterministically maps each Telecom PhoneAccountHandle to its real hardware SIM slot index (0 or 1).
     * This prevents the common bug where SIM 2 calls accidentally fall back to SIM 1.
     */
    @SuppressLint("MissingPermission")
    private fun mapAccountsToSlots(
        accounts: List<PhoneAccountHandle>,
        subscriptions: List<SubscriptionInfo>
    ): Map<Int, PhoneAccountHandle> {
        val result = mutableMapOf<Int, PhoneAccountHandle>()
        val usedHandles = mutableSetOf<PhoneAccountHandle>()

        // Tier 1 (API 26+): TelephonyManager.createForPhoneAccountHandle -> subscriptionId (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && telephonyManager != null) {
            for (handle in accounts) {
                if (handle in usedHandles) continue
                val scopedTm = runCatching {
                    telephonyManager?.createForPhoneAccountHandle(handle)
                }.getOrNull() ?: continue

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val subId = runCatching { scopedTm.subscriptionId }
                        .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                    if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        val matchedSub = subscriptions.firstOrNull { it.subscriptionId == subId }
                        if (matchedSub != null && matchedSub.simSlotIndex in 0..1 && !result.containsKey(matchedSub.simSlotIndex)) {
                            result[matchedSub.simSlotIndex] = handle
                            usedHandles.add(handle)
                            continue
                        }
                    }
                }

                // API 26-29 fallback via scoped TelephonyManager carrier/operator name
                val opName = runCatching { scopedTm.simOperatorName }.getOrNull().orEmpty()
                if (opName.isNotBlank()) {
                    val matchedSub = subscriptions.firstOrNull { sub ->
                        sub.simSlotIndex in 0..1 &&
                            !result.containsKey(sub.simSlotIndex) &&
                            sub.carrierName?.toString().orEmpty().equals(opName, ignoreCase = true)
                    }
                    if (matchedSub != null) {
                        result[matchedSub.simSlotIndex] = handle
                        usedHandles.add(handle)
                    }
                }
            }
        }

        // Tier 3: Match handle.id with SubscriptionInfo.iccId or subscriptionId string
        for (sub in subscriptions) {
            val slot = sub.simSlotIndex
            if (slot !in 0..1 || result.containsKey(slot)) continue

            val subIdStr = sub.subscriptionId.toString()
            @Suppress("DEPRECATION")
            val iccId = sub.iccId.orEmpty()

            val match = accounts.firstOrNull { handle ->
                if (handle in usedHandles) return@firstOrNull false
                val id = handle.id
                id == subIdStr ||
                    (iccId.isNotBlank() && (id.contains(iccId, ignoreCase = true) || iccId.contains(id, ignoreCase = true)))
            }
            if (match != null) {
                result[slot] = match
                usedHandles.add(match)
            }
        }

        // Tier 4: Match PhoneAccount label with subscription displayName/carrierName
        for (sub in subscriptions) {
            val slot = sub.simSlotIndex
            if (slot !in 0..1 || result.containsKey(slot)) continue
            val carrier = sub.carrierName?.toString().orEmpty()
            val display = sub.displayName?.toString().orEmpty()

            val match = accounts.firstOrNull { handle ->
                if (handle in usedHandles) return@firstOrNull false
                val label = runCatching { telecomManager?.getPhoneAccount(handle)?.label?.toString() }.getOrNull().orEmpty()
                label.isNotBlank() && (
                    (carrier.isNotBlank() && label.contains(carrier, ignoreCase = true)) ||
                        (display.isNotBlank() && label.contains(display, ignoreCase = true))
                    )
            }
            if (match != null) {
                result[slot] = match
                usedHandles.add(match)
            }
        }

        // Tier 5: Assign remaining unmatched handles to remaining active slots in deterministic order
        val remainingHandles = accounts.filter { it !in usedHandles }
        if (subscriptions.isNotEmpty()) {
            val remainingSlots = subscriptions.map { it.simSlotIndex }.filter { it in 0..1 && !result.containsKey(it) }
            remainingSlots.zip(remainingHandles).forEach { (slot, handle) ->
                result[slot] = handle
                usedHandles.add(handle)
            }
        } else {
            // No SubscriptionInfo returned (e.g., some emulator/SIP configs) -> map by index
            remainingHandles.forEachIndexed { idx, handle ->
                if (idx in 0..1 && !result.containsKey(idx)) {
                    result[idx] = handle
                }
            }
        }

        return result
    }

    /**
     * Resolves which SIM slot (0 or 1) a CallLog PHONE_ACCOUNT_ID belongs to.
     */
    fun resolveSimSlotForAccountId(accountId: String?, slots: List<SimSlotInfo>): Int? {
        if (accountId.isNullOrBlank()) return null
        slots.forEach { slot ->
            val handleId = slot.phoneAccountHandle?.id
            if (!handleId.isNullOrBlank() && (handleId == accountId || handleId.contains(accountId) || accountId.contains(handleId))) {
                return slot.slotIndex
            }
            if (slot.subscriptionId > 0 && accountId == slot.subscriptionId.toString()) {
                return slot.slotIndex
            }
        }
        return null
    }

    /**
     * Places an outgoing call strictly through the requested SIM slot (0 = SIM 1, 1 = SIM 2).
     * Never silently falls back from SIM 2 to SIM 1.
     */
    @SuppressLint("MissingPermission")
    fun placeCall(rawNumber: String, targetSlotIndex: Int): CallPlacementResult {
        val sanitized = sanitizeDialNumber(rawNumber)
        if (sanitized.isBlank()) {
            return CallPlacementResult.InvalidNumber
        }

        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPhone || !hasPhoneState) {
            return CallPlacementResult.PermissionRequired
        }

        val slots = loadSimSlots()
        val targetSlot = slots.firstOrNull { it.slotIndex == targetSlotIndex }

        if (targetSlot == null || !targetSlot.isActive || targetSlot.phoneAccountHandle == null) {
            // Do NOT silently fall back to SIM 1 if SIM 2 was requested!
            val reason = if (targetSlotIndex == 1) {
                "SIM 2 is not active or has no call-capable SIM card."
            } else {
                "SIM 1 is not active or has no call-capable SIM card."
            }
            return CallPlacementResult.SimUnavailable(targetSlotIndex, reason)
        }

        val encodedNumber = Uri.encode(sanitized, "+*#,;")
        val uri = Uri.parse("tel:$encodedNumber")
        val handle = targetSlot.phoneAccountHandle

        return runCatching {
            if (isDefaultDialer() && telecomManager != null) {
                val extras = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
                telecomManager.placeCall(uri, extras)
            } else {
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    putExtra("com.android.phone.extra.slot", targetSlotIndex)
                    putExtra("simSlot", targetSlotIndex)
                    putExtra("slot", targetSlotIndex)
                    if (targetSlot.subscriptionId > 0) {
                        putExtra("subscription", targetSlot.subscriptionId)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
            }
            CallPlacementResult.Success(targetSlot.summaryLabel, sanitized)
        }.getOrElse { error ->
            CallPlacementResult.Error(error.localizedMessage ?: "Unable to start call.")
        }
    }

    companion object {
        fun sanitizeDialNumber(input: String): String {
            val cleaned = buildString {
                input.forEachIndexed { index, ch ->
                    when {
                        ch.isDigit() -> append(ch)
                        ch == '+' && isEmpty() -> append(ch)
                        ch == '*' || ch == '#' || ch == ',' || ch == ';' -> append(ch)
                    }
                }
            }
            return cleaned
        }
    }
}
