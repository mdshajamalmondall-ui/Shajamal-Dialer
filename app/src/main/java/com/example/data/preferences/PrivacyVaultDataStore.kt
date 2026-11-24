package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallDirection
import com.example.data.model.CommunitySpamReport
import com.example.data.model.HiddenCallEntry
import com.example.data.model.PrivateContactEntry
import com.example.data.model.SpamCategory
import com.example.data.security.SecurityCryptoManager
import com.example.data.telephony.DualSimCallManager
import com.example.data.telephony.PhoneNumberInfoHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.privacyVaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "shajamal_privacy_vault")

class PrivacyVaultDataStore(private val context: Context) {

    private object Keys {
        val PRIVATE_CONTACTS_JSON = stringPreferencesKey("private_contacts_json")
        val HIDDEN_CALL_HISTORY_JSON = stringPreferencesKey("hidden_call_history_json")
    }

    val privateContactsFlow: Flow<List<PrivateContactEntry>> = context.privacyVaultDataStore.data.map { prefs ->
        parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty())
            .sortedByDescending { it.addedAtMillis }
    }

    val hiddenCallsFlow: Flow<List<HiddenCallEntry>> = context.privacyVaultDataStore.data.map { prefs ->
        parseHiddenCalls(prefs[Keys.HIDDEN_CALL_HISTORY_JSON].orEmpty())
            .sortedByDescending { it.timestampMillis }
    }

    suspend fun addOrUpdatePrivateContact(
        name: String,
        rawNumber: String,
        note: String = "",
        sensitiveMask: Boolean = true
    ) {
        val cleanedNumber = DualSimCallManager.sanitizeDialNumber(rawNumber).ifBlank { rawNumber.trim() }
        if (cleanedNumber.isBlank()) return
        val canonical = PhoneNumberInfoHelper.canonicalKey(cleanedNumber)
        val cleanName = name.trim().ifBlank { cleanedNumber }

        context.privacyVaultDataStore.edit { prefs ->
            val current = parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty()).toMutableList()
            val existingIdx = current.indexOfFirst {
                val ek = PhoneNumberInfoHelper.canonicalKey(it.number)
                (canonical.isNotBlank() && ek == canonical) || it.number == cleanedNumber
            }
            val updated = PrivateContactEntry(
                id = if (existingIdx >= 0) current[existingIdx].id else UUID.randomUUID().toString(),
                name = cleanName,
                number = cleanedNumber,
                note = note.trim(),
                sensitiveMask = sensitiveMask,
                addedAtMillis = System.currentTimeMillis()
            )
            if (existingIdx >= 0) {
                current[existingIdx] = updated
            } else {
                current.add(0, updated)
            }
            prefs[Keys.PRIVATE_CONTACTS_JSON] = serializePrivateContacts(current)
        }
    }

    suspend fun togglePrivateContactMask(id: String, masked: Boolean) {
        context.privacyVaultDataStore.edit { prefs ->
            val current = parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty()).map {
                if (it.id == id) it.copy(sensitiveMask = masked) else it
            }
            prefs[Keys.PRIVATE_CONTACTS_JSON] = serializePrivateContacts(current)
        }
    }

    suspend fun removePrivateContact(id: String) {
        context.privacyVaultDataStore.edit { prefs ->
            val current = parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty())
                .filterNot { it.id == id }
            prefs[Keys.PRIVATE_CONTACTS_JSON] = serializePrivateContacts(current)
        }
    }

    suspend fun isPrivateNumber(rawNumber: String): Boolean {
        val canonical = PhoneNumberInfoHelper.canonicalKey(rawNumber)
        val sanitized = DualSimCallManager.sanitizeDialNumber(rawNumber).ifBlank { rawNumber.trim() }
        if (sanitized.isBlank()) return false
        val list = parsePrivateContacts(context.privacyVaultDataStore.data.first()[Keys.PRIVATE_CONTACTS_JSON].orEmpty())
        return list.any {
            val pk = PhoneNumberInfoHelper.canonicalKey(it.number)
            (canonical.isNotBlank() && pk == canonical) || it.number == sanitized
        }
    }

    suspend fun recordHiddenCall(
        rawNumber: String,
        contactName: String?,
        direction: CallDirection,
        timestampMillis: Long = System.currentTimeMillis(),
        durationSeconds: Long = 0L,
        simSlotIndex: Int? = null
    ) {
        val cleaned = DualSimCallManager.sanitizeDialNumber(rawNumber).ifBlank { rawNumber.trim() }
        if (cleaned.isBlank()) return
        context.privacyVaultDataStore.edit { prefs ->
            val current = parseHiddenCalls(prefs[Keys.HIDDEN_CALL_HISTORY_JSON].orEmpty()).toMutableList()
            current.add(
                0,
                HiddenCallEntry(
                    id = UUID.randomUUID().toString(),
                    number = cleaned,
                    contactName = contactName?.takeIf { it.isNotBlank() },
                    direction = direction,
                    timestampMillis = timestampMillis,
                    durationSeconds = durationSeconds,
                    simSlotIndex = simSlotIndex
                )
            )
            prefs[Keys.HIDDEN_CALL_HISTORY_JSON] = serializeHiddenCalls(current.take(250))
        }
    }

    suspend fun deleteHiddenCall(id: String) {
        context.privacyVaultDataStore.edit { prefs ->
            val current = parseHiddenCalls(prefs[Keys.HIDDEN_CALL_HISTORY_JSON].orEmpty())
                .filterNot { it.id == id }
            prefs[Keys.HIDDEN_CALL_HISTORY_JSON] = serializeHiddenCalls(current)
        }
    }

    suspend fun clearHiddenHistory() {
        context.privacyVaultDataStore.edit { prefs ->
            prefs.remove(Keys.HIDDEN_CALL_HISTORY_JSON)
        }
    }

    suspend fun exportEncryptedBackup(
        passphrase: String,
        blockedNumbers: List<BlockedNumberEntry>,
        numberNotes: Map<String, String>,
        communityReports: Collection<CommunitySpamReport>
    ): String {
        val prefs = context.privacyVaultDataStore.data.first()
        val privContacts = parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty())
        val hiddenCalls = parseHiddenCalls(prefs[Keys.HIDDEN_CALL_HISTORY_JSON].orEmpty())

        val bundle = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("privateContacts", JSONArray(serializePrivateContacts(privContacts)))
            put("hiddenCalls", JSONArray(serializeHiddenCalls(hiddenCalls)))
            put(
                "blockedNumbers",
                JSONArray().apply {
                    blockedNumbers.forEach { b ->
                        put(JSONObject().apply {
                            put("number", b.number)
                            put("label", b.label)
                            put("addedAt", b.addedAtMillis)
                        })
                    }
                }
            )
            put(
                "notes",
                JSONObject().apply {
                    numberNotes.forEach { (k, v) -> put(k, v) }
                }
            )
            put(
                "communityReports",
                JSONArray().apply {
                    communityReports.forEach { r ->
                        put(JSONObject().apply {
                            put("key", r.canonicalKey)
                            put("number", r.number)
                            put("category", r.category.id)
                            put("note", r.note)
                            put("count", r.reportCount)
                            put("ts", r.reportedAtMillis)
                        })
                    }
                }
            )
        }
        return SecurityCryptoManager.encryptBackupJson(bundle.toString(), passphrase)
    }

    suspend fun restoreEncryptedBackup(
        envelopeJson: String,
        passphrase: String,
        settingsDataStore: SettingsDataStore
    ): Result<Int> {
        val decrypted = SecurityCryptoManager.decryptBackupJson(envelopeJson, passphrase)
        val plain = decrypted.getOrElse { return Result.failure(it) }

        return runCatching {
            val bundle = JSONObject(plain)
            var restoredCount = 0

            val privArr = bundle.optJSONArray("privateContacts")
            if (privArr != null) {
                val restoredContacts = parsePrivateContacts(privArr.toString())
                context.privacyVaultDataStore.edit { prefs ->
                    val existing = parsePrivateContacts(prefs[Keys.PRIVATE_CONTACTS_JSON].orEmpty()).toMutableList()
                    restoredContacts.forEach { entry ->
                        val canonical = PhoneNumberInfoHelper.canonicalKey(entry.number)
                        if (existing.none { PhoneNumberInfoHelper.canonicalKey(it.number) == canonical }) {
                            existing.add(entry)
                            restoredCount++
                        }
                    }
                    prefs[Keys.PRIVATE_CONTACTS_JSON] = serializePrivateContacts(existing)
                }
            }

            val hiddenArr = bundle.optJSONArray("hiddenCalls")
            if (hiddenArr != null) {
                val restoredCalls = parseHiddenCalls(hiddenArr.toString())
                context.privacyVaultDataStore.edit { prefs ->
                    val existing = parseHiddenCalls(prefs[Keys.HIDDEN_CALL_HISTORY_JSON].orEmpty()).toMutableList()
                    val existingIds = existing.map { it.id }.toSet()
                    restoredCalls.forEach { c ->
                        if (c.id !in existingIds) {
                            existing.add(c)
                            restoredCount++
                        }
                    }
                    prefs[Keys.HIDDEN_CALL_HISTORY_JSON] = serializeHiddenCalls(existing.take(250))
                }
            }

            val blockedArr = bundle.optJSONArray("blockedNumbers")
            if (blockedArr != null) {
                for (i in 0 until blockedArr.length()) {
                    val obj = blockedArr.getJSONObject(i)
                    val num = obj.optString("number")
                    if (num.isNotBlank()) {
                        settingsDataStore.blockNumber(num, obj.optString("label", ""))
                        restoredCount++
                    }
                }
            }

            val notesObj = bundle.optJSONObject("notes")
            if (notesObj != null) {
                val keys = notesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = notesObj.optString(k)
                    if (k.isNotBlank() && v.isNotBlank()) {
                        settingsDataStore.saveNumberNote(k, v)
                    }
                }
            }

            val reportsArr = bundle.optJSONArray("communityReports")
            if (reportsArr != null) {
                val list = mutableListOf<CommunitySpamReport>()
                for (i in 0 until reportsArr.length()) {
                    val obj = reportsArr.getJSONObject(i)
                    val key = obj.optString("key")
                    if (key.isNotBlank()) {
                        list.add(
                            CommunitySpamReport(
                                canonicalKey = key,
                                number = obj.optString("number", key),
                                category = SpamCategory.fromId(obj.optString("category")),
                                note = obj.optString("note", ""),
                                reportCount = obj.optInt("count", 1),
                                reportedAtMillis = obj.optLong("ts", System.currentTimeMillis())
                            )
                        )
                        restoredCount++
                    }
                }
                settingsDataStore.mergeRestoredReports(list)
            }

            restoredCount
        }
    }

    private fun parsePrivateContacts(raw: String): List<PrivateContactEntry> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val number = obj.optString("number").takeIf { it.isNotBlank() } ?: continue
                    add(
                        PrivateContactEntry(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = obj.optString("name", number),
                            number = number,
                            note = obj.optString("note", ""),
                            sensitiveMask = obj.optBoolean("sensitiveMask", true),
                            addedAtMillis = obj.optLong("addedAt", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializePrivateContacts(list: List<PrivateContactEntry>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("number", c.number)
                    put("note", c.note)
                    put("sensitiveMask", c.sensitiveMask)
                    put("addedAt", c.addedAtMillis)
                }
            )
        }
        return arr.toString()
    }

    private fun parseHiddenCalls(raw: String): List<HiddenCallEntry> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val number = obj.optString("number").takeIf { it.isNotBlank() } ?: continue
                    val dir = runCatching {
                        CallDirection.valueOf(obj.optString("direction", CallDirection.INCOMING.name))
                    }.getOrDefault(CallDirection.INCOMING)
                    add(
                        HiddenCallEntry(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            number = number,
                            contactName = obj.optString("contactName").takeIf { it.isNotBlank() },
                            direction = dir,
                            timestampMillis = obj.optLong("ts", 0L),
                            durationSeconds = obj.optLong("duration", 0L),
                            simSlotIndex = if (obj.has("sim") && !obj.isNull("sim")) obj.getInt("sim") else null
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeHiddenCalls(list: List<HiddenCallEntry>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("number", c.number)
                    put("contactName", c.contactName.orEmpty())
                    put("direction", c.direction.name)
                    put("ts", c.timestampMillis)
                    put("duration", c.durationSeconds)
                    put("sim", c.simSlotIndex)
                }
            )
        }
        return arr.toString()
    }
}
