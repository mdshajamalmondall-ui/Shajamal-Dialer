package com.example.data.telephony

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.CallDirection
import com.example.data.model.CallLogItem
import com.example.data.model.ContactItem
import com.example.data.model.SimSlotInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceDirectoryRepository(
    private val context: Context,
    private val dualSimCallManager: DualSimCallManager
) {

    suspend fun loadCallLogs(slots: List<SimSlotInfo>, limit: Int = 150): List<CallLogItem> =
        withContext(Dispatchers.IO) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return@withContext emptyList()

            val items = mutableListOf<CallLogItem>()
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )

            val limitedUri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, limit.coerceAtLeast(1).toString())
                .build()

            runCatching {
                val cursor = runCatching {
                    context.contentResolver.query(
                        limitedUri,
                        projection,
                        null,
                        null,
                        "${CallLog.Calls.DATE} DESC"
                    )
                }.getOrNull() ?: context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use { c ->
                    val idIdx = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val numIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val accIdx = c.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

                    while (c.moveToNext() && items.size < limit) {
                        val typeInt = c.getInt(typeIdx)
                        val direction = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                            CallLog.Calls.VOICEMAIL_TYPE -> CallDirection.VOICEMAIL
                            CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
                            else -> CallDirection.INCOMING
                        }
                        val accountId = c.getString(accIdx)
                        items.add(
                            CallLogItem(
                                id = c.getLong(idIdx),
                                number = c.getString(numIdx).orEmpty(),
                                cachedName = c.getString(nameIdx),
                                direction = direction,
                                dateMillis = c.getLong(dateIdx),
                                durationSeconds = c.getLong(durIdx),
                                phoneAccountId = accountId,
                                simSlotIndex = dualSimCallManager.resolveSimSlotForAccountId(accountId, slots)
                            )
                        )
                    }
                }
            }
            items
        }

    suspend fun deleteCallLogEntry(id: Long): Boolean = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext false

        runCatching {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext emptyList()

        val contactMap = linkedMapOf<Long, MutableContactBuilder>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
                val photoUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx).orEmpty().ifBlank { "Unnamed Contact" }
                    val rawNumber = cursor.getString(numIdx).orEmpty().trim()
                    if (rawNumber.isBlank()) continue
                    val starred = cursor.getInt(starIdx) == 1
                    val photo = (if (photoUriIdx >= 0) cursor.getString(photoUriIdx) else null)
                        ?.takeIf { it.isNotBlank() }
                        ?: cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() }

                    val builder = contactMap.getOrPut(contactId) {
                        MutableContactBuilder(contactId, name, starred, photo)
                    }
                    val normalized = rawNumber.replace("\\s+".toRegex(), "")
                    if (builder.numbers.none { it.replace("\\s+".toRegex(), "") == normalized }) {
                        builder.numbers.add(rawNumber)
                    }
                }
            }
        }

        contactMap.values.map {
            ContactItem(
                id = it.id,
                name = it.name,
                primaryNumber = it.numbers.first(),
                phoneNumbers = it.numbers.toList(),
                isStarred = it.isStarred,
                photoUri = it.photoUri
            )
        }
    }

    suspend fun resolveCallerIdentity(number: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val cleaned = number.trim()
        if (cleaned.isBlank()) return@withContext null to null
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext null to null

        runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleaned)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )
            context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } else null
                    val photo = (if (photoIdx >= 0) cursor.getString(photoIdx) else null)?.takeIf { it.isNotBlank() }
                        ?: (if (thumbIdx >= 0) cursor.getString(thumbIdx) else null)?.takeIf { it.isNotBlank() }
                    return@withContext name to photo
                }
            }
        }

        val targetCanonical = PhoneNumberInfoHelper.canonicalKey(cleaned)
        if (targetCanonical.length >= 6) {
            val matched = loadContacts().firstOrNull { contact ->
                contact.phoneNumbers.any { PhoneNumberInfoHelper.canonicalKey(it) == targetCanonical }
            }
            if (matched != null) {
                return@withContext matched.name to matched.photoUri
            }
        }

        null to null
    }

    suspend fun setContactStarred(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext false

        runCatching {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
            context.contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString())
            ) > 0
        }.getOrDefault(false)
    }

    private data class MutableContactBuilder(
        val id: Long,
        val name: String,
        val isStarred: Boolean,
        val photoUri: String?,
        val numbers: MutableList<String> = mutableListOf()
    )

    companion object {
        fun nameToT9Digits(name: String): String {
            return buildString {
                for (ch in name.lowercase()) {
                    when (ch) {
                        in 'a'..'c' -> append('2')
                        in 'd'..'f' -> append('3')
                        in 'g'..'i' -> append('4')
                        in 'j'..'l' -> append('5')
                        in 'm'..'o' -> append('6')
                        in 'p'..'s' -> append('7')
                        in 't'..'v' -> append('8')
                        in 'w'..'z' -> append('9')
                        in '0'..'9' -> append(ch)
                    }
                }
            }
        }

        fun matchesDialQuery(contact: ContactItem, queryDigits: String): Boolean {
            if (queryDigits.isBlank()) return false
            val cleanQuery = queryDigits.filter { it.isDigit() || it == '+' }
            if (cleanQuery.isBlank()) return false

            val numberMatch = contact.phoneNumbers.any { num ->
                num.filter { it.isDigit() || it == '+' }.contains(cleanQuery)
            }
            if (numberMatch) return true

            val digitsOnly = cleanQuery.filter { it.isDigit() }
            if (digitsOnly.isEmpty()) return false
            return nameToT9Digits(contact.name).contains(digitsOnly)
        }
    }
}
